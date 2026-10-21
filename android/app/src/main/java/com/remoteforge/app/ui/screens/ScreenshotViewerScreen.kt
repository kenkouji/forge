package com.remoteforge.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    ORIGINAL("Original", null),
    SQUARE("1:1", 1.0f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

@Composable
fun ScreenshotViewerScreen(
    screenshotBase64: String,
    deviceName: String = "MacBook Pro",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Decode original bitmap safely in memory
    val originalBitmap = remember(screenshotBase64) {
        try {
            val bytes = Base64.decode(screenshotBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    var currentBitmap by remember(originalBitmap) { mutableStateOf(originalBitmap) }

    // Intercept Back button: if in crop mode, exit crop mode first; otherwise go back
    var isCropMode by remember { mutableStateOf(false) }
    BackHandler {
        if (isCropMode) {
            isCropMode = false
        } else {
            onBack()
        }
    }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Crop settings
    var selectedCropRatio by remember { mutableStateOf(CropAspectRatio.FREE) }
    var cropRectFraction by remember { mutableStateOf(Rect(0.1f, 0.1f, 0.9f, 0.9f)) }

    // Status feedback
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }

    val formattedTimestamp = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    val dimensionsText = remember(currentBitmap) {
        currentBitmap?.let { "${it.width} × ${it.height} px" } ?: "0 × 0"
    }

    val fileSizeText = remember(screenshotBase64) {
        val bytes = (screenshotBase64.length * 3) / 4
        if (bytes > 1024 * 1024) "${String.format("%.1f", bytes / (1024f * 1024f))} MB"
        else "${bytes / 1024} KB"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .onSizeChanged { containerSize = it }
    ) {
        if (currentBitmap == null) {
            // Error state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.BrokenImage, contentDescription = null, tint = AccentRuby, modifier = Modifier.size(54.dp))
                Text("Failed to render screenshot image.", color = TextPureWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet.copy(alpha = 0.2f))
                ) {
                    Text("Return", color = RoyalViolet)
                }
            }
        } else {
            val bmp = currentBitmap!!

            // ==========================================
            // Main Zoomable Image Canvas
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            val contW = containerSize.width.toFloat().coerceAtLeast(100f)
                            val contH = containerSize.height.toFloat().coerceAtLeast(100f)
                            val maxPanX = (contW * scale).coerceAtLeast(contW / 2f)
                            val maxPanY = (contH * scale).coerceAtLeast(contH / 2f)
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                y = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                            )
                        }
                    }
                    .pointerInput(isCropMode) {
                        if (!isCropMode) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Workstation Screenshot",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = ContentScale.Fit
                )

                // ==========================================
                // Crop Overlay (when in Crop mode)
                // ==========================================
                if (isCropMode) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val left = cropRectFraction.left * w
                        val top = cropRectFraction.top * h
                        val right = cropRectFraction.right * w
                        val bottom = cropRectFraction.bottom * h

                        val dimColor = Color.Black.copy(alpha = 0.62f)

                        // 1. Draw 4 dimming overlays around crop window (clean non-destructive)
                        if (top > 0f) {
                            drawRect(color = dimColor, topLeft = Offset.Zero, size = Size(w, top))
                        }
                        if (bottom < h) {
                            drawRect(color = dimColor, topLeft = Offset(0f, bottom), size = Size(w, h - bottom))
                        }
                        if (left > 0f) {
                            drawRect(color = dimColor, topLeft = Offset(0f, top), size = Size(left, bottom - top))
                        }
                        if (right < w) {
                            drawRect(color = dimColor, topLeft = Offset(right, top), size = Size(w - right, bottom - top))
                        }

                        // 2. Crop Window border
                        val cropW = right - left
                        val cropH = bottom - top
                        drawRect(
                            color = RoyalViolet,
                            topLeft = Offset(left, top),
                            size = Size(cropW, cropH),
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // 3. Rule-of-thirds subtle interior grid lines
                        val thirdW = cropW / 3f
                        val thirdH = cropH / 3f
                        val gridColor = Color.White.copy(alpha = 0.28f)
                        val gridStroke = 1.dp.toPx()

                        drawLine(gridColor, Offset(left + thirdW, top), Offset(left + thirdW, bottom), gridStroke)
                        drawLine(gridColor, Offset(left + 2 * thirdW, top), Offset(left + 2 * thirdW, bottom), gridStroke)
                        drawLine(gridColor, Offset(left, top + thirdH), Offset(right, top + thirdH), gridStroke)
                        drawLine(gridColor, Offset(left, top + 2 * thirdH), Offset(right, top + 2 * thirdH), gridStroke)

                        // 4. Corner bracket accents for tactile look
                        val bracketLen = 22.dp.toPx()
                        val bracketStroke = 4.dp.toPx()
                        val bracketColor = RoyalViolet

                        // Top-left
                        drawLine(bracketColor, Offset(left - 2, top), Offset(left + bracketLen, top), bracketStroke)
                        drawLine(bracketColor, Offset(left, top - 2), Offset(left, top + bracketLen), bracketStroke)
                        // Top-right
                        drawLine(bracketColor, Offset(right + 2, top), Offset(right - bracketLen, top), bracketStroke)
                        drawLine(bracketColor, Offset(right, top - 2), Offset(right, top + bracketLen), bracketStroke)
                        // Bottom-left
                        drawLine(bracketColor, Offset(left - 2, bottom), Offset(left + bracketLen, bottom), bracketStroke)
                        drawLine(bracketColor, Offset(left, bottom + 2), Offset(left, bottom - bracketLen), bracketStroke)
                        // Bottom-right
                        drawLine(bracketColor, Offset(right + 2, bottom), Offset(right - bracketLen, bottom), bracketStroke)
                        drawLine(bracketColor, Offset(right, bottom + 2), Offset(right, bottom - bracketLen), bracketStroke)
                    }
                }
            }

            // ==========================================
            // Top Bar: Back Button & Metadata Chips
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isCropMode) isCropMode = false else onBack()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceElevated)
                            .border(0.5.dp, ObsidianBorder, CircleShape)
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPureWhite, modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = if (isCropMode) "Crop Screenshot" else deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite
                        )
                        Text(
                            text = if (isCropMode) "Select aspect ratio and crop area" else "$dimensionsText • $fileSizeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalViolet
                        )
                    }
                }

                // Reset Zoom button (if zoomed)
                if (!isCropMode && scale > 1.05f) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianSurfaceElevated,
                        border = BorderStroke(0.5.dp, ObsidianBorder),
                        modifier = Modifier.clickable {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.ZoomOutMap, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(14.dp))
                            Text("Reset", color = RoyalViolet, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ==========================================
            // Bottom Action Toolbar (Apple Liquid Glass)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                if (isCropMode) {
                    // CROP CONTROLS TOOLBAR
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xEE161824))
                            .border(1.dp, Color(0x3300F5D4), RoundedCornerShape(24.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Aspect ratio selector chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CropAspectRatio.values().forEach { ratioPreset ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedCropRatio == ratioPreset) RoyalViolet.copy(alpha = 0.25f) else Color(0x22FFFFFF),
                                    border = BorderStroke(0.5.dp, if (selectedCropRatio == ratioPreset) RoyalViolet else Color(0x1AFFFFFF)),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable {
                                            selectedCropRatio = ratioPreset
                                            val contW = containerSize.width.toFloat().coerceAtLeast(100f)
                                            val contH = containerSize.height.toFloat().coerceAtLeast(100f)
                                            val targetAspect: Float = when {
                                                ratioPreset == CropAspectRatio.ORIGINAL -> bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                                                ratioPreset.ratio != null -> ratioPreset.ratio!!
                                                else -> bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                                            }
                                            var targetPixelW = contW * 0.86f
                                            var targetPixelH = targetPixelW / targetAspect
                                            val maxPixelH = contH * 0.52f
                                            if (targetPixelH > maxPixelH) {
                                                targetPixelH = maxPixelH
                                                targetPixelW = targetPixelH * targetAspect
                                            }
                                            val normW = (targetPixelW / contW).coerceIn(0.15f, 0.95f)
                                            val normH = (targetPixelH / contH).coerceIn(0.15f, 0.95f)
                                            val centerY = 0.44f
                                            cropRectFraction = Rect(
                                                0.5f - normW / 2f,
                                                centerY - normH / 2f,
                                                0.5f + normW / 2f,
                                                centerY + normH / 2f
                                            )
                                        }
                                ) {
                                    Text(
                                        text = ratioPreset.label,
                                        color = if (selectedCropRatio == ratioPreset) RoyalViolet else TextSoftWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Apply / Cancel Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isCropMode = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Cancel", color = TextPureWhite, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    // Apply Crop to Bitmap accounting for user zoom and pan
                                    try {
                                        val srcW = bmp.width
                                        val srcH = bmp.height
                                        val contW = containerSize.width.toFloat()
                                        val contH = containerSize.height.toFloat()

                                        if (contW > 0 && contH > 0 && srcW > 0 && srcH > 0) {
                                            val fitScale = minOf(contW / srcW.toFloat(), contH / srcH.toFloat())
                                            val baseDispW = srcW * fitScale
                                            val baseDispH = srcH * fitScale

                                            val curDispW = baseDispW * scale
                                            val curDispH = baseDispH * scale

                                            val imgCenterX = contW / 2f + offset.x
                                            val imgCenterY = contH / 2f + offset.y

                                            val imgLeft = imgCenterX - curDispW / 2f
                                            val imgTop = imgCenterY - curDispH / 2f

                                            val cropScreenLeft = cropRectFraction.left * contW
                                            val cropScreenTop = cropRectFraction.top * contH
                                            val cropScreenRight = cropRectFraction.right * contW
                                            val cropScreenBottom = cropRectFraction.bottom * contH

                                            val normX1 = ((cropScreenLeft - imgLeft) / curDispW).coerceIn(0f, 1f)
                                            val normY1 = ((cropScreenTop - imgTop) / curDispH).coerceIn(0f, 1f)
                                            val normX2 = ((cropScreenRight - imgLeft) / curDispW).coerceIn(0f, 1f)
                                            val normY2 = ((cropScreenBottom - imgTop) / curDispH).coerceIn(0f, 1f)

                                            val minX = minOf(normX1, normX2)
                                            val maxX = maxOf(normX1, normX2)
                                            val minY = minOf(normY1, normY2)
                                            val maxY = maxOf(normY1, normY2)

                                            val cropX = (minX * srcW).toInt().coerceIn(0, srcW - 1)
                                            val cropY = (minY * srcH).toInt().coerceIn(0, srcH - 1)
                                            val cropW = ((maxX - minX) * srcW).toInt().coerceIn(1, srcW - cropX)
                                            val cropH = ((maxY - minY) * srcH).toInt().coerceIn(1, srcH - cropY)

                                            val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropW, cropH)
                                            currentBitmap = cropped
                                            scale = 1f
                                            offset = Offset.Zero
                                            isCropMode = false
                                            feedbackMessage = "Crop applied (${cropW}×${cropH} px)"
                                        }
                                    } catch (e: Exception) {
                                        feedbackMessage = "Crop failed: ${e.localizedMessage}"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Apply Crop", color = RoyalViolet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // MAIN FULLSCREEN TOOLBAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xDD12131C))
                            .border(0.5.dp, ObsidianBorder, RoundedCornerShape(26.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action 1: Crop
                        ViewerToolbarButton(
                            icon = Icons.Rounded.Crop,
                            label = "Crop",
                            accentColor = RoyalViolet
                        ) {
                            isCropMode = true
                            selectedCropRatio = CropAspectRatio.FREE
                            val contW = containerSize.width.toFloat().coerceAtLeast(100f)
                            val contH = containerSize.height.toFloat().coerceAtLeast(100f)
                            val normW = 0.86f
                            val normH = 0.50f
                            cropRectFraction = Rect(0.5f - normW / 2f, 0.44f - normH / 2f, 0.5f + normW / 2f, 0.44f + normH / 2f)
                        }

                        // Action 2: Save to Phone Gallery
                        ViewerToolbarButton(
                            icon = Icons.Rounded.SaveAlt,
                            label = "Save",
                            accentColor = AccentEmerald
                        ) {
                            coroutineScope.launch {
                                val uri = saveBitmapToGallery(context, bmp, isCropped = (bmp != originalBitmap))
                                if (uri != null) {
                                    savedUri = uri
                                    feedbackMessage = "Saved to Gallery (Pictures/RemoteForge)"
                                } else {
                                    feedbackMessage = "Failed to save screenshot"
                                }
                            }
                        }

                        // Action 3: Share
                        ViewerToolbarButton(
                            icon = Icons.Rounded.Share,
                            label = "Share",
                            accentColor = AccentViolet
                        ) {
                            coroutineScope.launch {
                                shareScreenshot(context, bmp)
                            }
                        }

                        // Action 4: Copy to Clipboard
                        ViewerToolbarButton(
                            icon = Icons.Rounded.ContentCopy,
                            label = "Copy",
                            accentColor = AccentAmber
                        ) {
                            coroutineScope.launch {
                                copyScreenshotToClipboard(context, bmp)
                                feedbackMessage = "Copied screenshot to clipboard"
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Feedback Toast / SnackBar Overlay
            // ==========================================
            feedbackMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3500)
                    feedbackMessage = null
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF0181A24))
                        .border(1.dp, RoyalViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = msg, color = TextPureWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        savedUri?.let { uri ->
                            Text(
                                text = "OPEN",
                                color = RoyalViolet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "image/png")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(viewIntent)
                                    }
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerToolbarButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(18.dp))
        }
        Text(text = label, color = TextSoftWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Saves the given bitmap to Android MediaStore in the Pictures/RemoteForge album,
 * scans the media store, and returns the newly created content Uri.
 */
suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, isCropped: Boolean = false): Uri? = withContext(Dispatchers.IO) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val filename = if (isCropped) "RemoteForge_Screenshot_${timestamp}_cropped.png" else "RemoteForge_Screenshot_${timestamp}.png"

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RemoteForge")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null

    try {
        resolver.openOutputStream(imageUri)?.use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }

        // Notify Android Media Scanner so it appears immediately in Google Photos / Gallery
        MediaScannerConnection.scanFile(context, arrayOf(imageUri.toString()), arrayOf("image/png"), null)
        imageUri
    } catch (e: Exception) {
        resolver.delete(imageUri, null, null)
        null
    }
}

/**
 * Exports bitmap to temporary cache and launches standard Android share sheet.
 */
suspend fun shareScreenshot(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
    try {
        val cacheDir = File(context.cacheDir, "screenshots").apply { mkdirs() }
        val file = File(cacheDir, "shared_screenshot_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(shareIntent, "Share Workstation Screenshot").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        // Fallback
    }
}

/**
 * Copies the screenshot into the Android system clipboard.
 */
suspend fun copyScreenshotToClipboard(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
    try {
        val cacheDir = File(context.cacheDir, "screenshots").apply { mkdirs() }
        val file = File(cacheDir, "clipboard_screenshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(context.contentResolver, "Screenshot", uri)
        clipboard.setPrimaryClip(clip)
    } catch (e: Exception) {
        // Fallback
    }
}
