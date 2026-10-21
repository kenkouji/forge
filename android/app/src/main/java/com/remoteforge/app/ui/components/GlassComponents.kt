package com.remoteforge.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.*
import com.remoteforge.app.ui.theme.*

/**
 * Obsidian Liquid Glass Design System Core Components
 */

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = GlassSurfaceDark,
    borderColor: Color = GlassBorderHighlight,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    } else Modifier

    Surface(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color(0x35000000),
                spotColor = Color(0x45000000)
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            borderColor,
                            borderColor.copy(alpha = 0.2f),
                            Color(0x05FFFFFF)
                        )
                    )
                ),
                shape = shape
            )
            .then(clickableModifier),
        shape = shape,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun StatusIndicator(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val alphaAnim by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(
                BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = alphaAnim))
        )
        Text(
            text = text,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun DeviceCarouselCard(
    device: Device,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit = {},
    onClick: () -> Unit = onSelect,
    uptime: String? = null,
    cpuPercent: Double? = null,
    ramPercent: Double? = null
) {
    val haptic = LocalHapticFeedback.current
    val borderBrush = if (isSelected) {
        Brush.linearGradient(listOf(RoyalViolet, LavenderAccent.copy(alpha = 0.6f)))
    } else {
        Brush.verticalGradient(listOf(GlassBorderHighlight, GlassBorderSubtle))
    }

    Surface(
        modifier = modifier
            .width(290.dp)
            .shadow(
                elevation = if (isSelected) 10.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = if (isSelected) RoyalViolet.copy(alpha = 0.2f) else Color(0x30000000)
            )
            .border(
                BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderBrush),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) Color(0x381E293B) else GlassSurfaceDark
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Device Name + Online Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) RoyalViolet.copy(alpha = 0.2f) else GlassSurfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                device.osName.contains("mac", ignoreCase = true) -> Icons.Rounded.LaptopMac
                                device.osName.contains("win", ignoreCase = true) -> Icons.Rounded.Computer
                                else -> Icons.Rounded.Terminal
                            },
                            contentDescription = null,
                            tint = if (isSelected) RoyalViolet else TextSecondaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${device.osName} • Agent v${device.agentVersion}",
                            color = TextMutedDark,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                StatusIndicator(
                    text = if (device.isOnline) "ONLINE" else "OFFLINE",
                    accentColor = if (device.isOnline) EmeraldOnline else RubyOffline,
                    isPulsing = device.isOnline
                )
            }

            // Telemetry mini row if available
            if (device.isOnline && (cpuPercent != null || ramPercent != null)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (cpuPercent != null) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x2038BDF8)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CPU", fontSize = 10.sp, color = TextMutedDark)
                                Text("${cpuPercent.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalViolet)
                            }
                        }
                    }

                    if (ramPercent != null) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x20818CF8)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RAM", fontSize = 10.sp, color = TextMutedDark)
                                Text("${ramPercent.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LavenderAccent)
                            }
                        }
                    }
                }
            } else if (!uptime.isNullOrBlank()) {
                Text(
                    text = "Uptime: $uptime",
                    color = TextMutedDark,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "ID: ${device.id.take(18)}...",
                    color = TextMutedDark,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    label: String = "",
    title: String = label,
    description: String? = null,
    subtitle: String? = description,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val displayTitle = if (title.isNotBlank()) title else label
    val displayDesc = subtitle ?: description

    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x25000000)
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.35f),
                            Color(0x08FFFFFF)
                        )
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        color = GlassSurfaceDark
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = displayTitle,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = displayTitle,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (!displayDesc.isNullOrBlank()) {
                Text(
                    text = displayDesc,
                    color = TextMutedDark,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TaskProgressCard(
    task: TaskItem,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    onInspect: () -> Unit = {},
    onClick: () -> Unit = onInspect
) {
    val isRunning = task.status == "RUNNING" || task.status == "PENDING"
    val accent = if (isRunning) RoyalViolet else if (task.status == "COMPLETED") EmeraldOnline else RubyOffline

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = accent.copy(alpha = 0.6f),
        onClick = onClick
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Rounded.Autorenew else Icons.Rounded.TaskAlt,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Task: ${task.tool}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Device: ${task.deviceId?.take(16) ?: "Host"} • ${task.permissionLevel ?: "LOW"} Risk",
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                }
            }

            StatusIndicator(
                text = task.status,
                accentColor = accent,
                isPulsing = isRunning
            )
        }

        if (isRunning) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = RoyalViolet,
                trackColor = GlassSurfaceElevated
            )
        }

        if (task.errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.errorMessage,
                color = RubyOffline,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FileTypeBadge(
    filename: String,
    isDir: Boolean,
    modifier: Modifier = Modifier
) {
    if (isDir) {
        Box(
            modifier = modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF59E0B).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = "Folder",
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(22.dp)
            )
        }
        return
    }

    val ext = filename.substringAfterLast('.', "").lowercase()
    when (ext) {
        "pdf" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PDF",
                        color = Color(0xFFEF4444),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        "doc", "docx" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RoyalVioletDark.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = "Word",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DOC",
                        color = Color(0xFF3B82F6),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        "xls", "xlsx", "csv" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.TableChart,
                        contentDescription = "Excel",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (ext == "csv") "CSV" else "XLS",
                        color = Color(0xFF10B981),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        "ppt", "pptx" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF97316).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Slideshow,
                        contentDescription = "PowerPoint",
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PPT",
                        color = Color(0xFFF97316),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        "jpg", "jpeg", "png", "webp", "gif", "svg", "bmp" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RoyalViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = "Image",
                        tint = RoyalViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = ext.uppercase().take(3),
                        color = RoyalViolet,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        "kt", "java", "py", "js", "ts", "html", "css", "json", "xml", "sql", "sh", "yml", "yaml" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LavenderAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = "Code",
                        tint = LavenderAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = ext.uppercase().take(3),
                        color = LavenderAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        "zip", "rar", "tar", "gz", "7z" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFD97706).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Archive,
                        contentDescription = "Archive",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ZIP",
                        color = Color(0xFFF59E0B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        "mp3", "wav", "m4a", "flac", "aac" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEC4899).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.AudioFile,
                        contentDescription = "Audio",
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AUD",
                        color = Color(0xFFF472B6),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        "mp4", "mov", "mkv", "webm", "avi" -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.VideoFile,
                        contentDescription = "Video",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "VID",
                        color = Color(0xFFA78BFA),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        else -> {
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Article,
                        contentDescription = "File",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = ext.uppercase().take(3).ifEmpty { "DOC" },
                        color = TextSecondaryDark,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TransferCard(
    transfer: TransferRecord,
    onCancel: () -> Unit,
    onPause: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null,
    onOpenFile: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDone = transfer.status == TransferStatus.COMPLETED
    val isFailed = transfer.status == TransferStatus.FAILED
    val isPaused = transfer.status == TransferStatus.PAUSED
    val isRunning = transfer.status == TransferStatus.IN_PROGRESS || transfer.status == TransferStatus.QUEUED

    val progress = if (transfer.totalBytes > 0) {
        (transfer.transferredBytes.toFloat() / transfer.totalBytes).coerceIn(0f, 1f)
    } else 0f

    val accent = when {
        isDone -> EmeraldOnline
        isFailed -> RubyOffline
        isPaused -> AmberWarning
        else -> RoyalViolet
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = accent.copy(alpha = 0.4f),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                FileTypeBadge(filename = transfer.filename, isDir = false)

                Column {
                    Text(
                        text = transfer.filename,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isDone -> "Completed • ${(transfer.totalBytes / 1024)} KB" + if (transfer.sha256 != null) " • SHA: ${transfer.sha256.take(8)}" else ""
                            isFailed -> "Failed: ${transfer.errorMessage ?: "Interrupted"}"
                            isPaused -> "Paused at ${(transfer.transferredBytes / 1024)} / ${(transfer.totalBytes / 1024)} KB"
                            else -> "${(transfer.transferredBytes / 1024)} / ${(transfer.totalBytes / 1024)} KB • ${(transfer.speedBps / 1024)} KB/s"
                        },
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isRunning) {
                    if (onPause != null) {
                        IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = AmberWarning, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = RubyOffline, modifier = Modifier.size(16.dp))
                    }
                } else if (isPaused) {
                    if (onResume != null) {
                        IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume", tint = RoyalViolet, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = RubyOffline, modifier = Modifier.size(16.dp))
                    }
                } else if (isFailed) {
                    if (onResume != null) {
                        IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Retry", tint = RoyalViolet, modifier = Modifier.size(16.dp))
                        }
                    }
                } else if (isDone) {
                    if (onOpenFile != null && transfer.localPath != null) {
                        IconButton(onClick = onOpenFile, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = "Open File", tint = EmeraldOnline, modifier = Modifier.size(18.dp))
                        }
                    } else if (onClick != null) {
                        IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Visibility, contentDescription = "Preview", tint = RoyalViolet, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (isRunning || isPaused) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isPaused) AmberWarning else RoyalViolet,
                trackColor = GlassSurfaceElevated
            )
        }
    }
}

@Composable
fun AutomationCard(
    automation: AutomationItem,
    onToggle: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onEdit: () -> Unit,
    onDiscard: () -> Unit,
    onDryRun: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isActive = automation.status == "ACTIVE"
    val accent = if (isActive) RoyalViolet else TextMutedDark

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (isActive) RoyalViolet.copy(alpha = 0.4f) else GlassBorderSubtle,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Title & Active Switch
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) RoyalViolet.copy(alpha = 0.15f) else GlassSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = automation.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${automation.triggerType} • Tool: ${automation.actionTool}",
                            color = TextMutedDark,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RoyalViolet,
                        uncheckedTrackColor = GlassSurfaceElevated
                    )
                )
            }

            if (automation.description.isNotBlank()) {
                Text(
                    text = automation.description,
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (automation.isOneShot) {
                val nowSec = System.currentTimeMillis() / 1000.0
                val diffSec = (automation.scheduledEpoch?.minus(nowSec) ?: 0.0).toLong()
                val badgeColor = when {
                    automation.status == "COMPLETED" -> EmeraldOnline
                    diffSec > 0 -> RoyalViolet
                    else -> AmberWarning
                }
                val badgeText = when {
                    automation.status == "COMPLETED" -> "✓ Executed & Completed"
                    diffSec > 60 -> "⏱️ Scheduled in ${diffSec / 60}m ${diffSec % 60}s"
                    diffSec > 0 -> "⏱️ Scheduled in ${diffSec}s"
                    else -> "⏱️ Triggering now..."
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Schedule info & actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Runs: ${automation.executionCount} • ${automation.nextRunTime ?: "Standby"}",
                    color = TextMutedDark,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Dry Run Simulation Button
                    if (onDryRun != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LavenderAccent.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { onDryRun() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.Visibility, contentDescription = "Dry Run", tint = LavenderAccent, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Dry Run", color = LavenderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Run Now Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RoyalViolet.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onRunNow() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Run", tint = RoyalViolet, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Run", color = RoyalViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Edit
                    IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = TextSecondaryDark, modifier = Modifier.size(15.dp))
                    }

                    // Discard
                    IconButton(onClick = onDiscard, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Discard", tint = RubyOffline, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBottomNavigation(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.92f)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color(0x60000000),
                    spotColor = Color(0x80000000)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x40FFFFFF),
                                Color(0x10FFFFFF)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xE6151C28) // Translucent charcoal grey glass
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 5 destinations: Home, Devices, AI (Center Glow), Files, Settings
                val navItems = listOf(
                    NavigationTab.HOME,
                    NavigationTab.DEVICES,
                    NavigationTab.AI_ASSISTANT,
                    NavigationTab.FILES,
                    NavigationTab.SETTINGS
                )

                navItems.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val isAi = tab == NavigationTab.AI_ASSISTANT

                    if (isAi) {
                        // Prominent central glowing AI button
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = CircleShape,
                                    ambientColor = RoyalViolet.copy(alpha = 0.5f),
                                    spotColor = LavenderAccent.copy(alpha = 0.6f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            if (isSelected) RoyalViolet else RoyalVioletDark,
                                            if (isSelected) LavenderAccent else RoyalViolet
                                        )
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0x60BDA0FF)
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTabSelected(tab)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        // Standard destinations
                        val tabColor by animateColorAsState(
                            targetValue = if (isSelected) RoyalViolet else TextMutedDark,
                            animationSpec = tween(220),
                            label = "tabColor"
                        )

                        val bgModifier = if (isSelected) {
                            Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(RoyalViolet.copy(alpha = 0.12f))
                        } else {
                            Modifier
                        }

                        Column(
                            modifier = Modifier
                                .then(bgModifier)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTabSelected(tab)
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = tabColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                color = tabColor,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeletonShimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = shape,
        color = GlassSurfaceElevated.copy(alpha = alpha)
    ) {}
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = RubyOffline.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = RubyOffline,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Operation Error",
                fontWeight = FontWeight.Bold,
                color = RubyOffline,
                fontSize = 15.sp
            )
            Text(
                text = message,
                color = TextSecondaryDark,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = RubyOffline.copy(alpha = 0.25f))
            ) {
                Text("Retry", color = RubyOffline, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextMutedDark,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = description,
                color = TextMutedDark,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet.copy(alpha = 0.2f))
                ) {
                    Text(actionLabel, color = RoyalViolet, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConfirmationSheet(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Confirm & Execute",
    isDestructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDestructive) Icons.Rounded.Warning else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (isDestructive) RubyOffline else RoyalViolet,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Text(text = description, fontSize = 13.sp, color = TextSecondaryDark)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) RubyOffline else RoyalViolet
                )
            ) {
                Text(confirmButtonText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        },
        containerColor = Color(0xFF111827),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp
    )
}
