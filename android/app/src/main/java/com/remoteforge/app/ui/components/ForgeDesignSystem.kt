package com.remoteforge.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.data.model.FileItem
import com.remoteforge.app.ui.theme.*

/**
 * RemoteForge Apple Liquid Glass × Obsidian Design System
 * Deep obsidian surfaces, subtle reflections, restrained glowing accents.
 */

val ObsidianBackground = Color(0xFF080B12)
val ObsidianSurface = Color(0xCC0E131F)
val ObsidianSurfaceElevated = Color(0xE6151C2A)
val ObsidianBorder = Color(0x2BFFFFFF)
val ObsidianBorderSubtle = Color(0x18FFFFFF)
val ObsidianBorderAccent = Color(0x3838BDF8)

val AccentCyan = Color(0xFF38BDF8)
val AccentSky = Color(0xFF00E5FF)
val AccentEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentRuby = Color(0xFFEF4444)
val AccentIndigo = Color(0xFF818CF8)
val AccentViolet = Color(0xFF8B5CF6)
val AccentBlue = Color(0xFF3B82F6)

val TextPureWhite = Color(0xFFF8FAFC)
val TextSoftWhite = Color(0xFFE2E8F0)
val TextRestrainedGray = Color(0xFF94A3B8)
val TextDarkMuted = Color(0xFF64748B)

val AiGradientBrush = Brush.linearGradient(
    listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
)

val GlassHighlightBorderBrush = Brush.verticalGradient(
    listOf(
        Color(0x4DFFFFFF),
        Color(0x14FFFFFF),
        Color(0x05FFFFFF)
    )
)

/**
 * Core Obsidian Liquid Glass Card
 */
@Composable
fun ForgeGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = ObsidianSurface,
    borderColor: Color = ObsidianBorder,
    borderBrush: Brush? = null,
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
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color(0x60000000),
                spotColor = Color(0x90000000)
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = borderBrush ?: Brush.verticalGradient(
                        listOf(borderColor.copy(alpha = 0.5f), borderColor.copy(alpha = 0.15f))
                    )
                ),
                shape = shape
            )
            .clip(shape)
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

/**
 * Screen Section Header
 */
@Composable
fun ForgeSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPureWhite,
                fontSize = 15.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextRestrainedGray,
                    fontSize = 11.sp
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Status Dot Badge (e.g. Green dot "Online", Amber dot "Locked")
 */
@Composable
fun ForgeStatusBadge(
    text: String,
    accentColor: Color = AccentEmerald,
    isPulsing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = alphaAnim))
        )
        Text(
            text = text,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Circular Telemetry Gauge (CPU, RAM, Disk)
 */
@Composable
fun ForgeTelemetryGauge(
    percent: Float,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 5.dp
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "gaugeProgress"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    val strokePx = strokeWidth.toPx()
                    val arcSize = Size(size.toPx() - strokePx, size.toPx() - strokePx)
                    val arcTopLeft = Offset(strokePx / 2f, strokePx / 2f)

                    // Background Track
                    drawArc(
                        color = Color(0x25FFFFFF),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // Active Sweep Arc
                    val sweep = (animatedPercent / 100f) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${animatedPercent.toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = TextRestrainedGray,
            fontSize = 11.sp
        )
    }
}

/**
 * 2-Column Quick Action Card
 */
@Composable
fun ForgeQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    ForgeGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(12.dp),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(0.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextRestrainedGray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Registered Computer Card (Compact, with device thumbnail graphic and status)
 */
@Composable
fun ForgeDeviceCard(
    device: Device,
    isSelected: Boolean,
    lastActiveText: String = "Last active 2 min ago",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMac = device.osName.contains("Mac", ignoreCase = true) || device.osName.contains("Darwin", ignoreCase = true)
    val isWin = device.osName.contains("Windows", ignoreCase = true)

    ForgeGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        borderColor = if (isSelected) AccentCyan.copy(alpha = 0.6f) else ObsidianBorder,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Device Thumbnail Graphic
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) AccentCyan.copy(alpha = 0.4f) else Color(0x25FFFFFF)
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isMac -> Icons.Rounded.LaptopMac
                            isWin -> Icons.Rounded.LaptopWindows
                            else -> Icons.Rounded.Computer
                        },
                        contentDescription = device.name,
                        tint = if (isSelected) AccentCyan else TextSoftWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (device.isOnline) AccentEmerald else AccentRuby)
                        )
                        Text(
                            text = if (device.isOnline) "Online • $lastActiveText" else "Offline",
                            color = TextRestrainedGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Inspect",
                tint = TextDarkMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Activity Row for Dashboard
 */
@Composable
fun ForgeActivityRow(
    title: String,
    timestamp: String,
    icon: ImageVector,
    isSuccess: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clickableModifier = if (onClick != null) {
        Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(clickableModifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextRestrainedGray,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSoftWhite,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = TextDarkMuted,
                fontSize = 11.sp
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) AccentEmerald else AccentRuby)
            )
        }
    }
}

/**
 * Floating Bottom Navigation Bar (Obsidian Liquid Glass Capsule)
 */
@Composable
fun ForgeFloatingNavigationBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .fillMaxWidth(0.92f)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = Color(0x70000000),
                    spotColor = Color(0x95000000)
                )
                .border(
                    BorderStroke(1.dp, GlassHighlightBorderBrush),
                    shape = RoundedCornerShape(34.dp)
                ),
            shape = RoundedCornerShape(34.dp),
            color = Color(0xE80D1322) // Translucent Obsidian Graphite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                ForgeNavIconButton(
                    icon = Icons.Rounded.Home,
                    contentDescription = "Home",
                    isSelected = selectedTab == NavigationTab.HOME,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(NavigationTab.HOME)
                    }
                )

                // 2. Devices (Computer Screen Icon)
                ForgeNavIconButton(
                    icon = Icons.Rounded.DesktopWindows,
                    contentDescription = "Devices",
                    isSelected = selectedTab == NavigationTab.DEVICES,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(NavigationTab.DEVICES)
                    }
                )

                // 3. Central AI Orb (Glowing Violet/Blue Glass Button)
                val isAiSelected = selectedTab == NavigationTab.AI_ASSISTANT
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = AccentViolet.copy(alpha = 0.5f),
                            spotColor = AccentBlue.copy(alpha = 0.6f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isAiSelected) Color(0xFF9333EA) else Color(0xFF7C3AED),
                                    if (isAiSelected) Color(0xFF2563EB) else Color(0xFF1D4ED8)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                width = if (isAiSelected) 2.dp else 1.dp,
                                color = if (isAiSelected) Color.White.copy(alpha = 0.9f) else Color(0x60A78BFA)
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSelected(NavigationTab.AI_ASSISTANT)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 4. Files
                ForgeNavIconButton(
                    icon = Icons.Rounded.Folder,
                    contentDescription = "Files",
                    isSelected = selectedTab == NavigationTab.FILES,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(NavigationTab.FILES)
                    }
                )

                // 5. Settings
                ForgeNavIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    isSelected = selectedTab == NavigationTab.SETTINGS,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(NavigationTab.SETTINGS)
                    }
                )
            }
        }
    }
}

@Composable
private fun ForgeNavIconButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) AccentCyan else TextDarkMuted,
        animationSpec = tween(200),
        label = "navTint"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) AccentCyan.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
