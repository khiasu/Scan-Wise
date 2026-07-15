package com.khiasu.docscanai.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khiasu.docscanai.prefs.SecurePrefs
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.isSystemInDarkTheme
import com.khiasu.docscanai.prefs.ThemePrefs
import com.khiasu.docscanai.MainActivity
import androidx.core.content.FileProvider
import com.khiasu.docscanai.R
import com.khiasu.docscanai.data.ScanRepository
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ScanScreen(onDocumentCreated: (Long) -> Unit) {
    val context = LocalContext.current
    val repo = remember { ScanRepository(context) }
    val scope = rememberCoroutineScope()

    // Use rememberSaveable so the URI list survives Activity recreation (e.g. when camera kills our Activity)
    var capturedUriStrings by rememberSaveable { mutableStateOf(listOf<String>()) }
    val capturedUris = capturedUriStrings.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }

    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraUriString != null) {
            capturedUriStrings = capturedUriStrings + pendingCameraUriString!!
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCaptureUri(context)
            pendingCameraUriString = uri.toString()
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> 
        capturedUriStrings = capturedUriStrings + uris.map { it.toString() }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val docId = repo.createDocumentFromPdf("ScanWise PDF ${System.currentTimeMillis() / 1000}", uri)
                onDocumentCreated(docId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Premium Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "ScanWise Logo",
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                                append("Scan ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("Wise")
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Question Bank Digitizer",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            ThemeToggleButton()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            var isReady by remember { mutableStateOf(SecurePrefs.isReady(context)) }
            LaunchedEffect(Unit) {
                isReady = SecurePrefs.isReady(context)
            }

            if (!isReady) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Extraction Key Required",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Configure your API key in Settings to begin processing question banks.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "New Question Bank",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan exam papers and test sheets to instantly organize questions, compile keys, and practice with mock quizzes.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    val uri = createCaptureUri(context)
                                    pendingCameraUriString = uri.toString()
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Camera", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Gallery", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (!SecurePrefs.isReady(context)) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Please configure your API key in Settings first.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                pdfLauncher.launch(arrayOf("application/pdf"))
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import PDF File", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Queued pages
            if (capturedUris.isNotEmpty()) {
                Text(
                    text = "Document Queue (${capturedUris.size} pages)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(capturedUris.toList(), key = { it.toString() }) { uri ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            UriThumbnail(
                                uri = uri,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Close Button overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { capturedUriStrings = capturedUriStrings.filter { s -> Uri.parse(s) != uri } },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Page indicator overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Pg ${capturedUris.indexOf(uri) + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!SecurePrefs.hasApiKey(context)) {
                            android.widget.Toast.makeText(
                                context,
                                "Please configure your API key in Settings first.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        scope.launch {
                            val docId = repo.createDocumentFromImages(
                                title = "Scan ${System.currentTimeMillis() / 1000}",
                                imageUris = capturedUris,
                                sourceType = "CAMERA"
                            )
                            capturedUriStrings = emptyList()
                            onDocumentCreated(docId)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process & Save Document", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Empty state helper to fill height
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Captured pages will appear here in sequence.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun UriThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uri) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                val targetSize = 300
                val height = options.outHeight
                val width = options.outWidth
                var inSampleSize = 1
                if (height > targetSize || width > targetSize) {
                    val halfHeight = height / 2
                    val halfWidth = width / 2
                    while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = inSampleSize
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }.getOrNull()
        }.let { decoded ->
            bitmap = decoded
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun createCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
fun ThemeToggleButton() {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    
    val isDark = remember { 
        derivedStateOf {
            when (MainActivity.currentThemeMode) {
                ThemePrefs.ThemeMode.SYSTEM -> isSystemDark
                ThemePrefs.ThemeMode.LIGHT -> false
                ThemePrefs.ThemeMode.DARK -> true
            }
        }
    }

    IconButton(
        onClick = {
            val nextMode = if (isDark.value) ThemePrefs.ThemeMode.LIGHT else ThemePrefs.ThemeMode.DARK
            ThemePrefs.set(context, nextMode)
            MainActivity.themeUpdater(nextMode)
        }
    ) {
        androidx.compose.animation.Crossfade(
            targetState = isDark.value,
            animationSpec = tween(durationMillis = 250)
        ) { dark ->
            Icon(
                imageVector = if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
