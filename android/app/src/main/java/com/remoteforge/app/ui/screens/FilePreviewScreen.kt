package com.remoteforge.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.remoteforge.app.data.model.FilePreviewResponse
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

private fun findDownloadedLocalFile(context: Context, filename: String): File? {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (downloadsDir != null) {
        val f = File(downloadsDir, filename)
        if (f.exists() && f.length() > 0) return f
    }
    val appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    if (appDownloads != null) {
        val f = File(appDownloads, filename)
        if (f.exists() && f.length() > 0) return f
    }
    val internalFile = File(context.filesDir, filename)
    if (internalFile.exists() && internalFile.length() > 0) return internalFile
    return null
}

private fun launchLocalFileViewer(context: Context, file: File): Boolean {
    return try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open '${file.name}' with...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        true
    } catch (_: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    deviceId: String,
    filePath: String,
    filename: String,
    repository: RemoteForgeRepository,
    onBack: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var previewData by remember { mutableStateOf<FilePreviewResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionNotification by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(findDownloadedLocalFile(context, filename)) }

    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        downloadedFile = findDownloadedLocalFile(context, filename)
        val res = repository.previewFile(deviceId, filePath)
        if (res != null) {
            previewData = res
        } else {
            errorMessage = "Unable to preview file. You can download and open it in your phone's viewer."
        }
        isLoading = false
    }

    // Image Bitmap decoding
    val imageBitmap = remember(previewData?.imageDataBase64) {
        previewData?.imageDataBase64?.let { b64 ->
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    // PDF Page 1 Preview rendering via native Android PdfRenderer
    val pdfPageBitmap = remember(previewData?.pdfDataBase64) {
        previewData?.pdfDataBase64?.let { b64 ->
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val tempFile = File(context.cacheDir, "pdf_preview_render.pdf")
                tempFile.writeBytes(bytes)
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    pfd.close()
                    bmp.asImageBitmap()
                } else {
                    renderer.close()
                    pfd.close()
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = filename,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite,
                            maxLines = 1
                        )
                        Text(
                            text = filePath.takeLast(35),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextRestrainedGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = AccentCyan
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(filePath))
                        actionNotification = "Copied path to clipboard"
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy Path",
                            tint = TextRestrainedGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Action Notification Banner
            actionNotification?.let { msg ->
                ForgeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = AccentCyan.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Text(text = msg, color = AccentCyan, fontSize = 12.sp)
                    }
                }
            }

            // File Attributes Header Card
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FileTypeBadge(filename = filename, isDir = false)

                        Column {
                            Text(
                                text = filename,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPureWhite
                            )
                            Text(
                                text = previewData?.let { "${(it.fileSize / 1024).coerceAtLeast(1)} KB • ${it.previewType?.uppercase() ?: "DOCUMENT"}" } ?: "Inspecting metadata...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextRestrainedGray
                            )
                        }
                    }

                    val badgeLabel = when {
                        downloadedFile != null -> "SAVED"
                        previewData?.content != null -> "TEXT"
                        imageBitmap != null -> "IMAGE"
                        pdfPageBitmap != null -> "PDF"
                        else -> "REMOTE"
                    }
                    StatusIndicator(
                        text = badgeLabel,
                        accentColor = if (downloadedFile != null) AccentEmerald else AccentCyan,
                        isPulsing = false
                    )
                }
            }

            // Preview Content Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LoadingSkeleton(height = 120.dp)
                        LoadingSkeleton(height = 180.dp)
                        LoadingSkeleton(height = 80.dp)
                    }
                } else if (errorMessage != null) {
                    ErrorState(
                        message = errorMessage!!,
                        onRetry = {
                            coroutineScope.launch {
                                isLoading = true
                                val res = repository.previewFile(deviceId, filePath)
                                previewData = res
                                isLoading = false
                            }
                        }
                    )
                } else if (imageBitmap != null) {
                    // 1. Image Viewer (High Fidelity)
                    ForgeGlassCard(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = filename,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        previewData?.imageDataBase64?.let { b64 ->
                                            onNavigate(AppScreen.ScreenshotViewer(b64, filename))
                                        }
                                    },
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap image to open in Fullscreen Viewer with zoom & crop",
                                color = AccentCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (pdfPageBitmap != null) {
                    // 2. PDF First Page Renderer
                    ForgeGlassCard(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PDF Document (Page 1)", color = TextPureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (downloadedFile != null) {
                                    Text("Downloaded to phone", color = AccentEmerald, fontSize = 11.sp)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = Color.White
                            ) {
                                Image(
                                    bitmap = pdfPageBitmap,
                                    contentDescription = "PDF Page 1",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                } else if (previewData?.content != null) {
                    // 3. Syntax / Text Viewer
                    val content = previewData!!.content!!
                    val lines = content.lines()

                    ForgeGlassCard(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        ) {
                            lines.forEachIndexed { idx, line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${idx + 1}".padStart(3, ' '),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TextRestrainedGray,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(
                                        text = line,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = TextPureWhite
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 4. Binary / Document with Direct Open Option
                    ForgeGlassCard(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            FileTypeBadge(filename = filename, isDir = false, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = filename,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPureWhite,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (downloadedFile != null) {
                                    "Saved locally on phone. Tap 'Open with App' to view in your preferred application."
                                } else {
                                    "Document file (${(previewData?.fileSize ?: 0) / 1024} KB). Tap Download below to stream and open it directly."
                                },
                                color = TextRestrainedGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Actions Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // If downloaded or local file exists, display primary "Open with App" button
                if (downloadedFile != null) {
                    Button(
                        onClick = {
                            val opened = launchLocalFileViewer(context, downloadedFile!!)
                            if (!opened) {
                                actionNotification = "No app found to open this file type"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open with Default App", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                actionNotification = "Downloading to phone..."
                                val success = repository.downloadFile(deviceId, filePath, filename)
                                if (success) {
                                    downloadedFile = findDownloadedLocalFile(context, filename)
                                    actionNotification = "Downloaded! Tap 'Open with Default App' above."
                                } else {
                                    actionNotification = "Download failed."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (downloadedFile != null) "Re-download" else "Download", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onNavigate(
                                AppScreen.UniversalTransfer(
                                    preselectedSourceDevice = deviceId,
                                    preselectedFile = filePath
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
