package com.remoteforge.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.remoteforge.app.data.model.FilePreviewResponse
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.delay
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
    val forgeColors = ForgeTheme.colors

    var previewData by remember { mutableStateOf<FilePreviewResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionNotification by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(findDownloadedLocalFile(context, filename)) }

    // PDF Multi-page state
    var pdfPageIndex by remember { mutableStateOf(0) }
    var pdfTotalPages by remember { mutableStateOf(1) }
    var pdfPageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Markdown view toggle
    var isMarkdownRaw by remember { mutableStateOf(false) }

    // Audio Player State
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioProgressSec by remember { mutableStateOf(0) }
    var audioDurationSec by remember { mutableStateOf(0) }
    var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Video local cached file
    var cachedVideoFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer?.release()
            audioPlayer = null
        }
    }

    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        downloadedFile = findDownloadedLocalFile(context, filename)
        val res = repository.previewFile(deviceId, filePath)
        if (res != null) {
            previewData = res
        } else {
            errorMessage = "Unable to load preview stream from target device. Ensure the workstation agent is connected."
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

    // PDF Multi-page rendering
    LaunchedEffect(previewData?.pdfDataBase64, pdfPageIndex) {
        previewData?.pdfDataBase64?.let { b64 ->
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val tempFile = File(context.cacheDir, "preview_${filename.hashCode()}.pdf")
                tempFile.writeBytes(bytes)
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pdfTotalPages = renderer.pageCount.coerceAtLeast(1)
                val safeIndex = pdfPageIndex.coerceIn(0, pdfTotalPages - 1)
                val page = renderer.openPage(safeIndex)
                val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                pdfPageBitmap = bmp
            } catch (_: Exception) {
                pdfPageBitmap = null
            }
        }
    }

    // Audio file handling
    LaunchedEffect(previewData?.mediaDataBase64, previewData?.previewType) {
        if (previewData?.previewType == "audio" && previewData?.mediaDataBase64 != null) {
            try {
                val bytes = Base64.decode(previewData!!.mediaDataBase64!!, Base64.DEFAULT)
                val tempAudio = File(context.cacheDir, "preview_audio_${filename.hashCode()}.mp3")
                tempAudio.writeBytes(bytes)

                val mp = MediaPlayer().apply {
                    setDataSource(tempAudio.absolutePath)
                    prepare()
                }
                audioDurationSec = mp.duration / 1000
                audioPlayer = mp

                mp.setOnCompletionListener {
                    isAudioPlaying = false
                    audioProgressSec = 0
                }
            } catch (_: Exception) {
            }
        }
    }

    // Audio progress polling
    LaunchedEffect(isAudioPlaying) {
        while (isAudioPlaying && audioPlayer != null) {
            audioProgressSec = (audioPlayer?.currentPosition ?: 0) / 1000
            delay(500)
        }
    }

    // Video file caching
    LaunchedEffect(previewData?.mediaDataBase64, previewData?.previewType) {
        if (previewData?.previewType == "video" && previewData?.mediaDataBase64 != null) {
            try {
                val bytes = Base64.decode(previewData!!.mediaDataBase64!!, Base64.DEFAULT)
                val tempVideo = File(context.cacheDir, "preview_vid_${filename.hashCode()}.mp4")
                tempVideo.writeBytes(bytes)
                cachedVideoFile = tempVideo
            } catch (_: Exception) {
                cachedVideoFile = null
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
                            color = forgeColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = filePath.takeLast(36),
                            style = MaterialTheme.typography.bodySmall,
                            color = forgeColors.textMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = if (forgeColors.isDark) LavenderAccent else LightAccent
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
                            tint = forgeColors.textMuted
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action Notification Banner
            actionNotification?.let { msg ->
                ForgeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (forgeColors.isDark) RoyalViolet.copy(alpha = 0.5f) else LightAccent.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (forgeColors.isDark) LavenderAccent else LightAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = msg,
                            color = forgeColors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // File Attributes Header Card
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        FileTypeBadge(filename = filename, isDir = false)

                        Column {
                            Text(
                                text = filename,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = forgeColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sizeText = previewData?.let {
                                val kb = (it.fileSize / 1024).coerceAtLeast(1)
                                "$kb KB • ${it.previewType?.uppercase() ?: "DOCUMENT"}"
                            } ?: "Inspecting stream..."
                            Text(
                                text = sizeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = forgeColors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    val badgeLabel = when {
                        previewData?.previewType == "markdown" -> "MARKDOWN"
                        previewData?.previewType == "audio" -> "AUDIO"
                        previewData?.previewType == "video" -> "VIDEO"
                        previewData?.previewType == "pdf" -> "PDF"
                        previewData?.previewType == "image" -> "IMAGE"
                        previewData?.previewType == "text" -> "CODE"
                        previewData?.previewSupported == true -> "PREVIEW"
                        else -> "METADATA"
                    }
                    StatusIndicator(
                        text = badgeLabel,
                        accentColor = if (previewData?.previewSupported == true) (if (forgeColors.isDark) LavenderAccent else LightAccent) else AccentAmber,
                        isPulsing = false
                    )
                }
            }

            // Preview Content Surface (Purely In-App)
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
                        LoadingSkeleton(height = 100.dp)
                        LoadingSkeleton(height = 160.dp)
                        LoadingSkeleton(height = 80.dp)
                    }
                } else if (errorMessage != null) {
                    ErrorState(
                        message = errorMessage!!,
                        onRetry = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                val res = repository.previewFile(deviceId, filePath)
                                if (res != null) {
                                    previewData = res
                                } else {
                                    errorMessage = "Unable to load preview stream from target device. Ensure the workstation agent is connected."
                                }
                                isLoading = false
                            }
                        }
                    )
                } else if (imageBitmap != null) {
                    // 1. Image Viewer (Fit with Fullscreen zoom option)
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
                                text = "Tap image to inspect in full-screen zoom viewer",
                                color = if (forgeColors.isDark) LavenderAccent else LightAccent,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (previewData?.previewType == "pdf" && pdfPageBitmap != null) {
                    // 2. Embedded Multi-Page PDF Viewer
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
                                Text(
                                    text = "PDF Document • Page ${pdfPageIndex + 1} of $pdfTotalPages",
                                    color = forgeColors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (pdfPageIndex > 0) pdfPageIndex--
                                        },
                                        enabled = pdfPageIndex > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.ChevronLeft,
                                            contentDescription = "Previous Page",
                                            tint = if (pdfPageIndex > 0) (if (forgeColors.isDark) LavenderAccent else LightAccent) else forgeColors.textMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (pdfPageIndex < pdfTotalPages - 1) pdfPageIndex++
                                        },
                                        enabled = pdfPageIndex < pdfTotalPages - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.ChevronRight,
                                            contentDescription = "Next Page",
                                            tint = if (pdfPageIndex < pdfTotalPages - 1) (if (forgeColors.isDark) LavenderAccent else LightAccent) else forgeColors.textMuted
                                        )
                                    }
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
                                    bitmap = pdfPageBitmap!!.asImageBitmap(),
                                    contentDescription = "PDF Page ${pdfPageIndex + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                } else if (previewData?.previewType == "markdown" && previewData?.content != null) {
                    // 3. Formatted Markdown Renderer with Raw toggle
                    val content = previewData!!.content!!
                    ForgeGlassCard(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isMarkdownRaw) "Raw Markdown" else "Formatted Document",
                                    color = forgeColors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                TextButton(
                                    onClick = { isMarkdownRaw = !isMarkdownRaw },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isMarkdownRaw) "Show Formatted" else "Show Raw",
                                        color = if (forgeColors.isDark) LavenderAccent else LightAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (isMarkdownRaw) {
                                val lines = content.lines()
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
                                                color = forgeColors.textMuted,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                            Text(
                                                text = line,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = forgeColors.textPrimary
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    content.lines().forEach { line ->
                                        when {
                                            line.startsWith("# ") -> {
                                                Text(
                                                    text = line.removePrefix("# "),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (forgeColors.isDark) LavenderAccent else LightAccent,
                                                    modifier = Modifier.padding(top = 8.dp)
                                                )
                                            }
                                            line.startsWith("## ") -> {
                                                Text(
                                                    text = line.removePrefix("## "),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = forgeColors.textPrimary,
                                                    modifier = Modifier.padding(top = 6.dp)
                                                )
                                            }
                                            line.startsWith("### ") -> {
                                                Text(
                                                    text = line.removePrefix("### "),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = forgeColors.textSecondary,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                            line.startsWith("- ") || line.startsWith("* ") -> {
                                                Row(
                                                    modifier = Modifier.padding(start = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 6.dp)
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (forgeColors.isDark) LavenderAccent else LightAccent)
                                                    )
                                                    Text(
                                                        text = line.drop(2),
                                                        fontSize = 12.sp,
                                                        color = forgeColors.textPrimary
                                                    )
                                                }
                                            }
                                            line.startsWith("> ") -> {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(BorderStroke(1.dp, if (forgeColors.isDark) LavenderAccent.copy(alpha = 0.3f) else LightAccent.copy(alpha = 0.3f)), RoundedCornerShape(8.dp)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = (if (forgeColors.isDark) RoyalViolet else LightAccent).copy(alpha = 0.08f)
                                                ) {
                                                    Text(
                                                        text = line.removePrefix("> "),
                                                        fontSize = 12.sp,
                                                        color = forgeColors.textSecondary,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }
                                            line.isNotBlank() -> {
                                                Text(
                                                    text = line,
                                                    fontSize = 12.sp,
                                                    color = forgeColors.textPrimary,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (previewData?.previewType == "audio") {
                    // 4. Embedded In-App Audio Player
                    ForgeGlassCard(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background((if (forgeColors.isDark) RoyalViolet else LightAccent).copy(alpha = 0.25f))
                                    .border(1.dp, if (forgeColors.isDark) LavenderAccent else LightAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Audiotrack,
                                    contentDescription = null,
                                    tint = if (forgeColors.isDark) LavenderAccent else LightAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = filename,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = forgeColors.textPrimary
                            )
                            Text(
                                text = "${audioProgressSec / 60}:${(audioProgressSec % 60).toString().padStart(2, '0')} / ${audioDurationSec / 60}:${(audioDurationSec % 60).toString().padStart(2, '0')}",
                                color = forgeColors.textMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            IconButton(
                                onClick = {
                                    audioPlayer?.let { mp ->
                                        if (mp.isPlaying) {
                                            mp.pause()
                                            isAudioPlaying = false
                                        } else {
                                            mp.start()
                                            isAudioPlaying = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (forgeColors.isDark) RoyalViolet else LightAccent)
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isAudioPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                } else if (previewData?.previewType == "video" && cachedVideoFile != null) {
                    // 5. Embedded In-App Video Player
                    ForgeGlassCard(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "In-App Video Playback",
                                color = forgeColors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoPath(cachedVideoFile!!.absolutePath)
                                        val mediaController = MediaController(ctx)
                                        mediaController.setAnchorView(this)
                                        setMediaController(mediaController)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                } else if (previewData?.content != null) {
                    // 6. Syntax / Code / Text Viewer
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
                                        color = forgeColors.textMuted,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(
                                        text = line,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = forgeColors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 7. Unsupported Format — Clean In-App Screen (NO EXTERNAL APP LAUNCH)
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
                                color = forgeColors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = previewData?.message ?: "Direct in-app rendering is not supported for this file format. You can download the file to your device or transfer it to another computer.",
                                color = forgeColors.textSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Actions Bar (Controlled In-App Actions)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                    actionNotification = "Downloaded to device storage."
                                } else {
                                    actionNotification = "Download failed."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (forgeColors.isDark) RoyalViolet else LightAccent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (downloadedFile != null) "Re-download" else "Download", color = Color.White, fontWeight = FontWeight.Bold)
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (forgeColors.isDark) RoyalVioletDark else Color(0xFF582C96)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Optional explicit external open ONLY if user explicitly taps it and file was downloaded
                if (downloadedFile != null) {
                    OutlinedButton(
                        onClick = {
                            val opened = launchLocalFileViewer(context, downloadedFile!!)
                            if (!opened) {
                                actionNotification = "No system viewer app found for this file type"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, (if (forgeColors.isDark) LavenderAccent else LightAccent).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = if (forgeColors.isDark) LavenderAccent else LightAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Open in External App (Optional)",
                            color = if (forgeColors.isDark) LavenderAccent else LightAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
