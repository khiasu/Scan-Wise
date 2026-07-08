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
    var lastExportMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(docId) { 
        doc = repo.getDocument(docId) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Premium Header with Back-like branding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc?.title ?: "Document",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val doneCount = pages.count { it.status == "DONE" }
                Text(
                    text = "$doneCount of ${pages.size} pages digitized",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
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

            lastExportMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
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
                    lastExportMessage = "Saved ${uris.size} file(s) to Downloads."
                    showExportDialog = false
                }
            }
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
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = "Export Document",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Combine Pages",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = scope == ExportScope.MERGED,
                        onClick = { scope = ExportScope.MERGED },
                        label = { Text("Single Merged File") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = scope == ExportScope.PER_PAGE,
                        onClick = { scope = ExportScope.PER_PAGE },
                        label = { Text("Individual Files") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = { format = f },
                            label = { Text(f.name) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { 
                        Text("Cancel") 
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onExport(scope, format) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}
