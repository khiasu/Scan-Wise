package com.khiasu.docscanai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.khiasu.docscanai.data.ScanRepository
import com.khiasu.docscanai.export.ExportFormat
import com.khiasu.docscanai.export.ExportManager
import com.khiasu.docscanai.export.ExportScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(docId: Long) {
    val context = LocalContext.current
    val repo = remember { ScanRepository(context) }
    val scope = rememberCoroutineScope()
    val pages by repo.observePages(docId).collectAsState(initial = emptyList())
    var doc by remember { mutableStateOf<com.khiasu.docscanai.data.DocumentEntity?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedUris by remember { mutableStateOf<List<android.net.Uri>?>(null) }
    var exportedFormat by remember { mutableStateOf<com.khiasu.docscanai.export.ExportFormat?>(null) }

    LaunchedEffect(docId) { 
        doc = repo.getDocument(docId) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Centered Header with Safe Top Padding to prevent notch/status-bar overlap
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 28.dp, bottom = 12.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = doc?.title ?: "Document",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            val doneCount = pages.count { it.status == "DONE" }
            Text(
                text = "$doneCount of ${pages.size} pages digitized",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }

        // Page list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(pages) { page ->
                val statusText = when (page.status) {
                    "DONE" -> "Completed"
                    "PROCESSING" -> "Extracting Text"
                    "ERROR" -> "Failed"
                    else -> "Pending Sync"
                }

                val statusColor = when (page.status) {
                    "DONE" -> Color(0xFF10B981) // Green
                    "PROCESSING" -> MaterialTheme.colorScheme.primary
                    "ERROR" -> Color(0xFFEF4444) // Red
                    else -> Color(0xFFF59E0B) // Amber
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 1.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page ${page.pageIndex + 1}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            // Status Pill
                            Surface(
                                color = statusColor.copy(alpha = 0.15f),
                                contentColor = statusColor,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (page.status) {
                            "DONE" -> {
                                Text(
                                    text = page.rawText?.take(180)?.plus(if ((page.rawText?.length ?: 0) > 180) "…" else "") ?: "(Empty Page)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            "ERROR" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = page.errorMessage ?: "Unknown extraction error",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { scope.launch { repo.retryPage(page.id) } },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retry")
                                    }
                                }
                            }
                            else -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Decoding page text...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export panel
        val doneCount = pages.count { it.status == "DONE" }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Button(
                onClick = { showExportDialog = true },
                enabled = doneCount > 0,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Document", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { exportScope, format ->
                scope.launch {
                    val document = doc ?: return@launch
                    val allPages = repo.getPages(docId)
                    val uris = ExportManager.export(context, document, allPages, exportScope, format)
                    exportedUris = uris
                    exportedFormat = format
                    showExportDialog = false
                }
            }
        )
    }

    val uris = exportedUris
    val format = exportedFormat
    if (uris != null && format != null) {
        AlertDialog(
            onDismissRequest = { 
                exportedUris = null
                exportedFormat = null
            },
            title = {
                Text(
                    text = "Export Successful",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Successfully saved your document files to the Downloads folder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val firstUri = uris.firstOrNull()
                        if (firstUri != null) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(firstUri, format.mimeType)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "No app found to open this format.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open File", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val firstUri = uris.firstOrNull()
                            if (firstUri != null) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = format.mimeType
                                        putExtra(android.content.Intent.EXTRA_STREAM, firstUri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Document"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Sharing failed.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            exportedUris = null
                            exportedFormat = null
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportScope, ExportFormat) -> Unit
) {
    var scope by remember { mutableStateOf(ExportScope.MERGED) }
    var format by remember { mutableStateOf(ExportFormat.PDF) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Export Document",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                // Scope selection
                Text(
                    text = "Structure",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Merged Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope = ExportScope.MERGED },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (scope == ExportScope.MERGED) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (scope == ExportScope.MERGED) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = scope == ExportScope.MERGED,
                                onClick = { scope = ExportScope.MERGED },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Single Document", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Merge all pages into one single file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Per page Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope = ExportScope.PER_PAGE },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (scope == ExportScope.PER_PAGE) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (scope == ExportScope.PER_PAGE) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = scope == ExportScope.PER_PAGE,
                                onClick = { scope = ExportScope.PER_PAGE },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Individual Pages", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Save each page as its own separate file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Format selection
                Text(
                    text = "File Format",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { f ->
                        val (title, subtitle) = when (f) {
                            ExportFormat.PDF -> "PDF Document" to "Best for sharing, viewing and printing"
                            ExportFormat.DOCX -> "Microsoft Word (DOCX)" to "Best for text editing and layout"
                            ExportFormat.CSV -> "CSV Spreadsheet" to "Best for tabular key-value data"
                            ExportFormat.JSON -> "Structured JSON" to "Best for developer data imports"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { format = f },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (format == f) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (format == f) 
                                    MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = format == f,
                                    onClick = { format = f },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onExport(scope, format) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
