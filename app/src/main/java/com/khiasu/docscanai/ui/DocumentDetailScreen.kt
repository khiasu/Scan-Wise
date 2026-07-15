package com.khiasu.docscanai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.khiasu.docscanai.data.PageEntity
import com.khiasu.docscanai.data.ScanRepository
import com.khiasu.docscanai.data.ParsedQuestionData
import com.khiasu.docscanai.data.parseQuestionValue
import com.khiasu.docscanai.data.serializeQuestionValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import kotlinx.coroutines.launch
import com.khiasu.docscanai.export.ExportFormat
import com.khiasu.docscanai.export.ExportManager
import com.khiasu.docscanai.export.ExportScope

data class QuestionItem(
    val pageId: Long,
    val fieldIndex: Int,
    val key: String,
    val data: ParsedQuestionData
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(docId: Long) {
    val context = LocalContext.current
    val repo = remember { ScanRepository(context) }
    val scope = rememberCoroutineScope()
    val pages by repo.observePages(docId).collectAsState(initial = emptyList())
    var doc by remember { mutableStateOf<com.khiasu.docscanai.data.DocumentEntity?>(null) }
    
    var selectedTab by remember { mutableStateOf(0) }
    
    // Edit page raw text state
    var editingPageRawText by remember { mutableStateOf<PageEntity?>(null) }
    
    // Edit/Add question state
    var editingQuestionItem by remember { mutableStateOf<QuestionItem?>(null) }
    var isAddingQuestion by remember { mutableStateOf(false) }
    var isSolving by remember { mutableStateOf(false) }
    
    // Quiz state variables
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }
    var subjectiveAnswerVisible by remember { mutableStateOf(false) }
    var subjectiveTextResponse by remember { mutableStateOf("") }

    fun resetQuiz() {
        currentQuestionIndex = 0
        selectedOption = null
        showAnswer = false
        score = 0
        quizFinished = false
        subjectiveAnswerVisible = false
        subjectiveTextResponse = ""
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) {
            resetQuiz()
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedUris by remember { mutableStateOf<List<android.net.Uri>?>(null) }
    var exportedFormat by remember { mutableStateOf<com.khiasu.docscanai.export.ExportFormat?>(null) }

    LaunchedEffect(docId) { 
        doc = repo.getDocument(docId) 
    }

    // Build the sequential question items from the database
    val questionList = remember(pages) {
        val list = mutableListOf<QuestionItem>()
        pages.forEach { page ->
            if (page.status == "DONE" && !page.fieldsJson.isNullOrBlank()) {
                try {
                    val arr = org.json.JSONArray(page.fieldsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val key = obj.optString("key", "")
                        val value = obj.optString("value", "")
                        val parsed = parseQuestionValue(value)
                        list.add(QuestionItem(page.id, i, key, parsed))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 20.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = doc?.title ?: "Document Detail",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            val doneCount = pages.count { it.status == "DONE" }
            Text(
                text = "$doneCount of ${pages.size} pages processed",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }

        // Tab selection
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Page History", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Layers, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Question Bank", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Book, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Practice Quiz", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Assignment, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            // Pages List tab
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
                        "PROCESSING" -> "Extracting Questions"
                        "ERROR" -> "Failed"
                        else -> "Pending Sync"
                    }

                    val statusColor = when (page.status) {
                        "DONE" -> Color(0xFF10B981)
                        "PROCESSING" -> MaterialTheme.colorScheme.primary
                        "ERROR" -> Color(0xFFEF4444)
                        else -> Color(0xFFF59E0B)
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
                        val thumbnailBitmap = remember(page.imagePath) {
                            runCatching {
                                android.graphics.BitmapFactory.decodeFile(page.imagePath)?.let { bmp ->
                                    val width = bmp.width
                                    val height = bmp.height
                                    val scale = 120f / width
                                    val targetWidth = 120
                                    val targetHeight = (height * scale).toInt().coerceIn(120, 200)
                                    android.graphics.Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true).asImageBitmap()
                                }
                            }.getOrNull()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (thumbnailBitmap != null) {
                                Image(
                                    bitmap = thumbnailBitmap,
                                    contentDescription = "Page preview",
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Page ${page.pageIndex + 1}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    
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

                                Spacer(modifier = Modifier.height(10.dp))

                                when (page.status) {
                                    "DONE" -> {
                                        Text(
                                            text = page.rawText?.let {
                                                if (it.length > 220) it.take(220) + "…" else it
                                            } ?: "(Empty OCR Content)",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { editingPageRawText = page },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Edit Raw Text", fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        repo.deletePage(page.id)
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Page",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    "ERROR" -> {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = page.errorMessage ?: "Extraction error occurred",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFFEF4444),
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { scope.launch { repo.retryPage(page.id) } },
                                                    colors = ButtonDefaults.textButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Retry", fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            repo.deletePage(page.id)
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Page",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
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
                                                text = if (page.status == "PROCESSING") "Extracting Text..." else "Pending sync...",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        repo.deletePage(page.id)
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Page",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            // Question Bank Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (questionList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No questions extracted yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When your scanned pages finish processing, questions will appear here in clean structured formats.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isSolving = true
                                    val completedPages = pages.filter { it.status == "DONE" && !it.rawText.isNullOrBlank() }
                                    if (completedPages.isEmpty()) {
                                        android.widget.Toast.makeText(context, "No transcribed pages found. Scan first.", android.widget.Toast.LENGTH_SHORT).show()
                                        isSolving = false
                                        return@launch
                                    }
                                    var successCount = 0
                                    completedPages.forEach { p ->
                                        val res = repo.solvePage(p.id)
                                        if (res.isSuccess) successCount++
                                    }
                                    isSolving = false
                                    if (successCount > 0) {
                                        android.widget.Toast.makeText(context, "Successfully solved $successCount pages!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Solving failed. Check your API key.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Solve & Generate Answers", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { isAddingQuestion = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Question Manually")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Extracted Questions (${questionList.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                isSolving = true
                                                val completedPages = pages.filter { it.status == "DONE" && !it.rawText.isNullOrBlank() }
                                                if (completedPages.isEmpty()) {
                                                    android.widget.Toast.makeText(context, "No transcribed pages found. Scan first.", android.widget.Toast.LENGTH_SHORT).show()
                                                    isSolving = false
                                                    return@launch
                                                }
                                                var successCount = 0
                                                completedPages.forEach { p ->
                                                    val res = repo.solvePage(p.id)
                                                    if (res.isSuccess) successCount++
                                                }
                                                isSolving = false
                                                if (successCount > 0) {
                                                    android.widget.Toast.makeText(context, "Successfully solved $successCount pages!", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "Solving failed. Check your API key.", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Solve & Regenerate", fontSize = 12.sp, maxLines = 1)
                                    }
                                    Button(
                                        onClick = { isAddingQuestion = true },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Add Question", fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        items(questionList) { item ->
                            val q = item.data
                            val figureBitmap = remember(q.figureImagePath) {
                                if (q.figureImagePath.isNotEmpty()) {
                                    runCatching {
                                        BitmapFactory.decodeFile(q.figureImagePath)?.asImageBitmap()
                                    }.getOrNull()
                                } else null
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.key,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Marks badge
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = q.marks,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }

                                            // Type badge
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = q.type,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Question body
                                    Text(
                                        text = q.question,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (q.paraphrasedQuestion.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Paraphrased: ${q.paraphrasedQuestion}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (q.type.equals("MCQ", ignoreCase = true) && q.options.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            q.options.forEach { (key, optionText) ->
                                                val isCorrect = q.answerKey.trim().lowercase() == key.lowercase()
                                                val optionBg = if (isCorrect) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
                                                val optionBorder = if (isCorrect) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color.Transparent)
                                                
                                                Surface(
                                                    color = optionBg,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = optionBorder,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "$key)  ",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                 color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        )
                                                        Text(
                                                            text = optionText,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Answer key section
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Answer: ",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${q.answerKey.uppercase()}${if (q.answerText.isNotEmpty()) " (${q.answerText})" else ""}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (q.explanation.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Column {
                                            Text(
                                                text = "Explanation:",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = q.explanation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (q.hint.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Column {
                                            Text(
                                                text = "Hint:",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = q.hint,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (q.figureDescription.isNotEmpty() || figureBitmap != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = "Figure",
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Figure / Diagram",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                    if (q.figureDescription.isNotEmpty()) {
                                                        Text(
                                                            text = q.figureDescription,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    if (figureBitmap != null) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Image(
                                                            bitmap = figureBitmap,
                                                            contentDescription = "Diagram Image",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(max = 200.dp)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Buttons row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { editingQuestionItem = item }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                // Delete question
                                                scope.launch {
                                                    val page = pages.find { it.id == item.pageId } ?: return@launch
                                                    if (!page.fieldsJson.isNullOrBlank()) {
                                                        val arr = org.json.JSONArray(page.fieldsJson)
                                                        val newArr = org.json.JSONArray()
                                                        for (i in 0 until arr.length()) {
                                                            if (i != item.fieldIndex) {
                                                                newArr.put(arr.getJSONObject(i))
                                                            }
                                                        }
                                                        repo.updatePage(page.copy(fieldsJson = newArr.toString()))
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Practice Quiz Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (questionList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No questions available",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please scan pages and click 'Solve & Generate Answers' first to generate quiz questions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (quizFinished) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Quiz Completed!",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Great job practicing! Here is your performance score:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.width(260.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$score / ${questionList.size}",
                                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Questions Correct",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { resetQuiz() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(200.dp).height(50.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Restart Quiz", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val currentQuestionItem = questionList[currentQuestionIndex]
                    val q = currentQuestionItem.data
                    val quizFigureBitmap = remember(q.figureImagePath) {
                        if (q.figureImagePath.isNotEmpty()) {
                            runCatching {
                                BitmapFactory.decodeFile(q.figureImagePath)?.asImageBitmap()
                            }.getOrNull()
                        } else null
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Question ${currentQuestionIndex + 1} of ${questionList.size}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Score: $score",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF10B981)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = (currentQuestionIndex + 1).toFloat() / questionList.size,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                        
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = q.type,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Text(
                                        text = q.marks,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = q.question,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (q.figureDescription.isNotEmpty() || quizFigureBitmap != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "Figure",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Figure / Diagram",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                                if (q.figureDescription.isNotEmpty()) {
                                                    Text(
                                                        text = q.figureDescription,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                if (quizFigureBitmap != null) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Image(
                                                        bitmap = quizFigureBitmap,
                                                        contentDescription = "Diagram Image",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 200.dp)
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (q.type.equals("MCQ", ignoreCase = true)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                q.options.forEach { (letter, text) ->
                                    val isSelected = selectedOption == letter
                                    val isCorrect = letter == q.answerKey.trim().lowercase()
                                    
                                    val backgroundColor = when {
                                        showAnswer && isCorrect -> Color(0xFFE6F4EA)
                                        showAnswer && isSelected && !isCorrect -> Color(0xFFFCE8E6)
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                    
                                    val borderColor = when {
                                        showAnswer && isCorrect -> Color(0xFF10B981)
                                        showAnswer && isSelected && !isCorrect -> Color(0xFFEF4444)
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                    
                                    val textColor = when {
                                        showAnswer && isCorrect -> Color(0xFF137333)
                                        showAnswer && isSelected && !isCorrect -> Color(0xFFC5221F)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, borderColor),
                                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !showAnswer) {
                                                selectedOption = letter
                                                if (isCorrect) score++
                                                showAnswer = true
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = letter.uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = subjectiveTextResponse,
                                    onValueChange = { subjectiveTextResponse = it },
                                    label = { Text("Write your response here...") },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    enabled = !subjectiveAnswerVisible
                                )
                                
                                if (!subjectiveAnswerVisible) {
                                    Button(
                                        onClick = { subjectiveAnswerVisible = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text("Submit Answer & Reveal Solution", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Model Answer / Rubric:",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = q.answerKey,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Grade your response:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                score++
                                                showAnswer = true
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            enabled = !showAnswer
                                        ) {
                                            Text("Mark Correct (+1)", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                showAnswer = true
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            enabled = !showAnswer
                                        ) {
                                            Text("Mark Incorrect (+0)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (showAnswer) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (q.explanation.isNotEmpty()) {
                                        Text(
                                            text = "Explanation:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = q.explanation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (q.hint.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Hint:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = q.hint,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    if (currentQuestionIndex + 1 < questionList.size) {
                                        currentQuestionIndex++
                                        selectedOption = null
                                        showAnswer = false
                                        subjectiveAnswerVisible = false
                                        subjectiveTextResponse = ""
                                    } else {
                                        quizFinished = true
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text(
                                    text = if (currentQuestionIndex + 1 < questionList.size) "Next Question" else "Finish Quiz",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                        
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }

        // Export panel
        val doneCount = pages.count { it.status == "DONE" }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
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
                Text("Export Question Bank", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

        if (isSolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(24.dp).widthIn(max = 320.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Solving Question Paper...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "The selected AI engine is solving the questions and generating answers, explanations, and hints.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Dialog for Editing Raw Text
    if (editingPageRawText != null) {
        val targetPage = editingPageRawText!!
        var textValue by remember { mutableStateOf(targetPage.rawText.orEmpty()) }

        Dialog(onDismissRequest = { editingPageRawText = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Edit Page ${targetPage.pageIndex + 1} Raw Text",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("OCR Transcribed Text") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingPageRawText = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    repo.updatePage(targetPage.copy(rawText = textValue))
                                    editingPageRawText = null
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog for Editing / Adding a Question
    if (editingQuestionItem != null || isAddingQuestion) {
        val item = editingQuestionItem
        var keyInput by remember { mutableStateOf(item?.key ?: "Question ${questionList.size + 1}") }
        var typeInput by remember { mutableStateOf(item?.data?.type ?: "MCQ") }
        var marksInput by remember { mutableStateOf(item?.data?.marks ?: "5 marks") }
        var questionInput by remember { mutableStateOf(item?.data?.question ?: "") }
        
        var optionAInput by remember { mutableStateOf(item?.data?.options?.get("a") ?: "") }
        var optionBInput by remember { mutableStateOf(item?.data?.options?.get("b") ?: "") }
        var optionCInput by remember { mutableStateOf(item?.data?.options?.get("c") ?: "") }
        var optionDInput by remember { mutableStateOf(item?.data?.options?.get("d") ?: "") }
        
        var answerInput by remember { mutableStateOf(item?.data?.answerKey ?: "") }
        var explanationInput by remember { mutableStateOf(item?.data?.explanation ?: "") }

        var paraphrasedInput by remember { mutableStateOf(item?.data?.paraphrasedQuestion ?: "") }
        var answerTextInput by remember { mutableStateOf(item?.data?.answerText ?: "") }
        var hintInput by remember { mutableStateOf(item?.data?.hint ?: "") }

        Dialog(onDismissRequest = { 
            editingQuestionItem = null
            isAddingQuestion = false
        }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = if (item != null) "Edit Question Details" else "Add New Question",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            label = { Text("Question ID / Label") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = typeInput,
                                onValueChange = { typeInput = it },
                                label = { Text("Type (e.g. MCQ / Subjective)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = marksInput,
                                onValueChange = { marksInput = it },
                                label = { Text("Marks") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = questionInput,
                            onValueChange = { questionInput = it },
                            label = { Text("Question Text") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = paraphrasedInput,
                            onValueChange = { paraphrasedInput = it },
                            label = { Text("Paraphrased Question") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (typeInput.equals("MCQ", ignoreCase = true)) {
                            Text(
                                text = "Multiple Choice Options",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            OutlinedTextField(
                                value = optionAInput,
                                onValueChange = { optionAInput = it },
                                label = { Text("Option A") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = optionBInput,
                                onValueChange = { optionBInput = it },
                                label = { Text("Option B") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = optionCInput,
                                onValueChange = { optionCInput = it },
                                label = { Text("Option C") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = optionDInput,
                                onValueChange = { optionDInput = it },
                                label = { Text("Option D") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = answerInput,
                            onValueChange = { answerInput = it },
                            label = { Text("Correct Answer Key") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = answerTextInput,
                            onValueChange = { answerTextInput = it },
                            label = { Text("Answer Text / Value") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = explanationInput,
                            onValueChange = { explanationInput = it },
                            label = { Text("Explanation") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = hintInput,
                            onValueChange = { hintInput = it },
                            label = { Text("Hint / Clue") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { 
                            editingQuestionItem = null
                            isAddingQuestion = false
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (questionInput.isBlank()) {
                                    android.widget.Toast.makeText(context, "Question text cannot be blank.", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                
                                val optionsMap = mutableMapOf<String, String>()
                                if (typeInput.equals("MCQ", ignoreCase = true)) {
                                    optionsMap["a"] = optionAInput
                                    optionsMap["b"] = optionBInput
                                    optionsMap["c"] = optionCInput
                                    optionsMap["d"] = optionDInput
                                }

                                val parsedData = ParsedQuestionData(
                                    type = typeInput,
                                    marks = marksInput,
                                    question = questionInput,
                                    options = optionsMap,
                                    answerKey = answerInput,
                                    explanation = explanationInput,
                                    paraphrasedQuestion = paraphrasedInput,
                                    answerText = answerTextInput,
                                    hint = hintInput
                                )

                                scope.launch {
                                    if (item != null) {
                                        val page = pages.find { it.id == item.pageId }
                                        if (page != null && !page.fieldsJson.isNullOrBlank()) {
                                            val arr = org.json.JSONArray(page.fieldsJson)
                                            val obj = arr.getJSONObject(item.fieldIndex)
                                            obj.put("key", keyInput)
                                            obj.put("value", serializeQuestionValue(parsedData))
                                            repo.updatePage(page.copy(fieldsJson = arr.toString()))
                                        }
                                    } else {
                                        val targetPage = pages.findLast { it.status == "DONE" } ?: pages.firstOrNull()
                                        if (targetPage != null) {
                                            val currentJson = targetPage.fieldsJson ?: "[]"
                                            val arr = org.json.JSONArray(currentJson)
                                            arr.put(org.json.JSONObject().apply {
                                                put("key", keyInput)
                                                put("value", serializeQuestionValue(parsedData))
                                            })
                                            repo.updatePage(targetPage.copy(fieldsJson = arr.toString()))
                                        }
                                    }
                                    
                                    editingQuestionItem = null
                                    isAddingQuestion = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
