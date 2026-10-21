package com.remoteforge.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * RemoteForge Apple Liquid Glass × Obsidian Design System
 * Deep obsidian surfaces, subtle reflections, restrained royal violet and lavender accents.
 * ZERO CYAN POLICY ENFORCED.
 */

val ObsidianBackground = Color(0xFF110B1C)
val ObsidianSurface = Color(0xCC29163D)
val ObsidianSurfaceElevated = Color(0xE6381E54)
val ObsidianBorder = Color(0x2BFFFFFF)
val ObsidianBorderSubtle = Color(0x18FFFFFF)
val ObsidianBorderAccent = Color(0x506136A6)

val TextPureWhite = Color(0xFFF2EDFA)
val TextSoftWhite = Color(0xFFD4C8E6)
val TextRestrainedGray = Color(0xFFA693BD)
val TextDarkMuted = Color(0xFF7E6699)

val AiGradientBrush = Brush.linearGradient(
    listOf(RoyalViolet, LavenderAccent)
)

val GlassHighlightBorderBrush = Brush.verticalGradient(
    listOf(
        Color(0x60BDA0FF),
        Color(0x206136A6),
        Color(0x05FFFFFF)
    )
)

/**
 * Slow breathing purple ambient background with multi-layer depth.
 * Pauses or reduces motion when reduced motion is on or during demanding tasks.
 */
@Composable
fun ForgeAmbientBackground(
    intensity: String = "FULL", // "FULL", "SUBTLE", "OFF"
    isLiveMirroring: Boolean = false,
    modifier: Modifier = Modifier
) {
    val forgeColors = ForgeTheme.colors
    val reducedMotion = ForgeTheme.motion.reducedMotion

    if (intensity.equals("OFF", ignoreCase = true) || isLiveMirroring) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(forgeColors.background)
        )
        return
    }

    val isDark = forgeColors.isDark
    val isSubtle = intensity.equals("SUBTLE", ignoreCase = true)

    // Slow-mo ASMR continuous orbital durations (never rush, never replay abruptly)
    val duration1 = if (isSubtle) 78000 else 52000
    val duration2 = if (isSubtle) 98000 else 68000
    val duration3 = if (isSubtle) 124000 else 86000
    val pulseDuration = if (isSubtle) 54000 else 38000

    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")

    // Slow continuous 360° orbital motion without any seam or turnaround replay
    val angle1 by if (reducedMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration1, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orb1Angle"
        )
    }

    val angle2 by if (reducedMotion) {
        remember { mutableFloatStateOf(180f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration2, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orb2Angle"
        )
    }

    val angle3 by if (reducedMotion) {
        remember { mutableFloatStateOf(90f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration3, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orb3Angle"
        )
    }

    // Gentle slow breathing scale
    val glowScale by if (reducedMotion) {
        remember { mutableFloatStateOf(1.0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(pulseDuration, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowScale"
        )
    }

    // Alpha breathing for Dark vs Light, and Full vs Subtle
    val alpha1Target = when {
        isDark && isSubtle -> 0.22f
        isDark -> 0.44f
        isSubtle -> 0.32f
        else -> 0.52f
    }
    val alpha1Base = when {
        isDark && isSubtle -> 0.12f
        isDark -> 0.28f
        isSubtle -> 0.20f
        else -> 0.36f
    }

    val alpha2Target = when {
        isDark && isSubtle -> 0.16f
        isDark -> 0.32f
        isSubtle -> 0.24f
        else -> 0.42f
    }
    val alpha2Base = when {
        isDark && isSubtle -> 0.08f
        isDark -> 0.18f
        isSubtle -> 0.14f
        else -> 0.26f
    }

    val glowAlpha1 by if (reducedMotion) {
        remember { mutableFloatStateOf(alpha1Base) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = alpha1Base,
            targetValue = alpha1Target,
            animationSpec = infiniteRepeatable(
                animation = tween((pulseDuration * 0.9).toInt(), easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha1"
        )
    }

    val glowAlpha2 by if (reducedMotion) {
        remember { mutableFloatStateOf(alpha2Base) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = alpha2Base,
            targetValue = alpha2Target,
            animationSpec = infiniteRepeatable(
                animation = tween((pulseDuration * 1.15).toInt(), easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha2"
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(forgeColors.background)
    ) {
        val w = size.width
        val h = size.height

        val rad1 = Math.toRadians(angle1.toDouble())
        val rad2 = Math.toRadians(angle2.toDouble())
        val rad3 = Math.toRadians(angle3.toDouble())

        // Continuous seamless trajectories across the display
        val center1 = Offset(
            x = w * 0.48f + (w * 0.36f) * kotlin.math.cos(rad1).toFloat(),
            y = h * 0.36f + (h * 0.24f) * kotlin.math.sin(rad1).toFloat()
        )

        val center2 = Offset(
            x = w * 0.52f + (w * 0.38f) * kotlin.math.cos(-rad2).toFloat(),
            y = h * 0.64f + (h * 0.26f) * kotlin.math.sin(-rad2).toFloat()
        )

        val center3 = Offset(
            x = w * 0.50f + (w * 0.35f) * kotlin.math.sin(rad3).toFloat(),
            y = h * 0.50f + (h * 0.30f) * kotlin.math.sin(rad3 * 2.0).toFloat()
        )

        if (isDark) {
            // Dark Mode: Deep Obsidian with Royal Violet, Lavender Accent, and Plum
            val radius1 = (w * 0.90f) * glowScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RoyalViolet.copy(alpha = glowAlpha1),
                        DeepPlum.copy(alpha = glowAlpha1 * 0.45f),
                        Color.Transparent
                    ),
                    center = center1,
                    radius = radius1
                ),
                radius = radius1,
                center = center1
            )

            val radius2 = (w * 0.82f) * (2f - glowScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LavenderAccent.copy(alpha = glowAlpha2),
                        RoyalViolet.copy(alpha = glowAlpha2 * 0.35f),
                        Color.Transparent
                    ),
                    center = center2,
                    radius = radius2
                ),
                radius = radius2,
                center = center2
            )

            val radius3 = w * 0.70f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DeepPlum.copy(alpha = glowAlpha1 * 0.35f),
                        Color.Transparent
                    ),
                    center = center3,
                    radius = radius3
                ),
                radius = radius3,
                center = center3
            )
        } else {
            // Light Mode: Pearl & Lilac iridescent glow on clean background
            val radius1 = (w * 0.92f) * glowScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LightAccentSoft.copy(alpha = glowAlpha1 * 0.55f),
                        LightSurfaceSecondary.copy(alpha = glowAlpha1 * 0.25f),
                        Color.Transparent
                    ),
                    center = center1,
                    radius = radius1
                ),
                radius = radius1,
                center = center1
            )

            val radius2 = (w * 0.85f) * (2f - glowScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE9DEF8).copy(alpha = glowAlpha2 * 0.65f),
                        LightAccentSoft.copy(alpha = glowAlpha2 * 0.25f),
                        Color.Transparent
                    ),
                    center = center2,
                    radius = radius2
                ),
                radius = radius2,
                center = center2
            )

            // Luminous pearl satin highlight
            val radius3 = w * 0.70f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = glowAlpha1 * 0.65f),
                        LightSurfaceSecondary.copy(alpha = glowAlpha1 * 0.20f),
                        Color.Transparent
                    ),
                    center = center3,
                    radius = radius3
                ),
                radius = radius3,
                center = center3
            )
        }
    }
}

/**
 * Custom RemoteForge abstract forge & signal emblem
 */
@Composable
fun RemoteForgeEmblem(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = RoyalViolet.copy(alpha = 0.6f))
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(RoyalViolet, DeepPlum)
                )
            )
            .border(1.dp, Brush.linearGradient(listOf(LavenderAccent.copy(alpha = 0.7f), RoyalViolet)), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height

            // Anvil Forge base silhouette
            val anvilPath = Path().apply {
                moveTo(w * 0.2f, h * 0.35f)
                lineTo(w * 0.8f, h * 0.35f)
                lineTo(w * 0.7f, h * 0.55f)
                lineTo(w * 0.85f, h * 0.85f)
                lineTo(w * 0.15f, h * 0.85f)
                lineTo(w * 0.3f, h * 0.55f)
                close()
            }
            drawPath(
                path = anvilPath,
                brush = Brush.verticalGradient(
                    listOf(PearlWhite, LavenderAccent)
                )
            )

            // Radiating signal pulse spark
            drawCircle(
                color = PearlWhite,
                radius = w * 0.12f,
                center = Offset(w * 0.5f, h * 0.32f)
            )
        }
    }
}

/**
 * Connected-Device Island: Dynamic capsule-shaped pill (never rectangular).
 * Expands on state changes, shrinks smoothly on Home scroll.
 */
@Composable
fun ConnectedDeviceIsland(
    device: Device?,
    connectionState: String,
    latencyMs: Long? = null,
    scrollOffset: Float = 0f, // 0 = fully visible at top, > 200 = shrunken/faded
    isExpandedOverride: Boolean = false,
    onTap: () -> Unit,
    onSwitchDevice: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isTemporarilyExpanded by remember { mutableStateOf(false) }

    // Trigger 2-second expansion when connection state changes
    LaunchedEffect(connectionState, device?.id) {
        if (device != null) {
            isTemporarilyExpanded = true
            delay(2200)
            isTemporarilyExpanded = false
        }
    }

    val isExpanded = isExpandedOverride || isTemporarilyExpanded

    // Scroll shrink calculation (fades and shrinks subtly as user scrolls down Home)
    val scrollShrink = (1f - (scrollOffset / 350f)).coerceIn(0f, 1f)
    if (scrollShrink <= 0.05f) return // Completely hidden when deeply scrolled

    val isOnline = device?.isOnline == true
    val statusColor = if (isOnline) AccentEmerald else AmberWarning
    val isMac = device?.osName?.contains("Mac", ignoreCase = true) == true || device?.osName?.contains("Darwin", ignoreCase = true) == true
    val forgeColors = ForgeTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 160.dp, max = if (isExpanded) 340.dp else 220.dp)
                .shadow(
                    elevation = (12 * scrollShrink).dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = RoyalViolet.copy(alpha = 0.4f),
                    spotColor = Color(0x80000000)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                LavenderAccent.copy(alpha = 0.6f * scrollShrink),
                                RoyalViolet.copy(alpha = 0.3f * scrollShrink)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .clip(RoundedCornerShape(26.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTap()
                },
            shape = RoundedCornerShape(26.dp),
            color = if (forgeColors.isDark) Color(0xF0180F2A) else Color(0xF4FFFFFF)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // Device glyph with glowing status ring
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(RoyalViolet.copy(alpha = 0.35f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMac) Icons.Rounded.LaptopMac else Icons.Rounded.Computer,
                        contentDescription = "Device",
                        tint = PearlWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = device?.name ?: "No Device Selected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = forgeColors.textPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = if (isOnline) {
                                if (latencyMs != null) "$connectionState • ${latencyMs}ms" else connectionState
                            } else {
                                "Offline"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = forgeColors.textMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                if (onSwitchDevice != null && isExpanded) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(RoyalViolet.copy(alpha = 0.4f))
                            .clickable { onSwitchDevice() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Switch Device",
                            tint = LavenderAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Core Obsidian Liquid Glass Card
 */
@Composable
fun ForgeGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderBrush: Brush? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val forgeColors = ForgeTheme.colors
    val effectiveBg = if (backgroundColor != Color.Unspecified) backgroundColor else forgeColors.surface
    val effectiveBorder = if (borderColor != Color.Unspecified) borderColor else forgeColors.border
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
                ambientColor = forgeColors.shadowColor,
                spotColor = forgeColors.shadowColor
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = borderBrush ?: Brush.verticalGradient(
                        listOf(effectiveBorder.copy(alpha = 0.5f), effectiveBorder.copy(alpha = 0.15f))
                    )
                ),
                shape = shape
            )
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = effectiveBg
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
    val forgeColors = ForgeTheme.colors

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
                color = forgeColors.textPrimary,
                fontSize = 15.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = forgeColors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = if (forgeColors.isDark) LavenderAccent else LightAccent,
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
 * Status Dot Badge
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
            .border(0.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
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
 * Circular Telemetry Gauge (Zero cyan - uses Royal Violet / Lavender / Emerald)
 */
@Composable
fun ForgeTelemetryGauge(
    percent: Float,
    label: String,
    accentColor: Color = RoyalViolet,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 5.dp
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "gaugeProgress"
    )

    val forgeColors = ForgeTheme.colors

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

                    // Track
                    drawArc(
                        color = forgeColors.borderSubtle,
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
                    color = forgeColors.textPrimary,
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = forgeColors.textMuted,
            fontSize = 11.sp
        )
    }
}

/**
 * Quick Action Card
 */
@Composable
fun ForgeQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = RoyalViolet,
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
            val forgeColors = ForgeTheme.colors

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(0.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (forgeColors.isDark) LavenderAccent else LightAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = forgeColors.textPrimary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = forgeColors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Registered Computer Card
 */
@Composable
fun ForgeDeviceCard(
    device: Device,
    isSelected: Boolean,
    lastActiveText: String = "Last active 2 min ago",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forgeColors = ForgeTheme.colors
    val isMac = device.osName.contains("Mac", ignoreCase = true) || device.osName.contains("Darwin", ignoreCase = true)
    val isWin = device.osName.contains("Windows", ignoreCase = true)

    ForgeGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        borderColor = if (isSelected) (if (forgeColors.isDark) LavenderAccent.copy(alpha = 0.7f) else LightAccent.copy(alpha = 0.8f)) else forgeColors.border,
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
                                listOf(RoyalViolet.copy(alpha = 0.3f), DeepPlum)
                            )
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) (if (forgeColors.isDark) LavenderAccent.copy(alpha = 0.6f) else LightAccent.copy(alpha = 0.8f)) else forgeColors.borderSubtle
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
                        tint = if (isSelected) (if (forgeColors.isDark) LavenderAccent else LightAccent) else forgeColors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = forgeColors.textPrimary,
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
                            color = forgeColors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Inspect",
                tint = forgeColors.textMuted,
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
    val forgeColors = ForgeTheme.colors
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
                tint = if (forgeColors.isDark) LavenderAccent.copy(alpha = 0.8f) else LightAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = forgeColors.textPrimary,
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
                color = forgeColors.textMuted,
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
 * EXACTLY FIVE DESTINATIONS in order: Home, Devices, AI, Files, Settings.
 * AI is center and visually prominent in a purple glass capsule/circle.
 * Supports normal taps and drag-to-preview gesture.
 */
@Composable
fun ForgeFloatingNavigationBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val forgeColors = ForgeTheme.colors
    val haptic = LocalHapticFeedback.current
    var previewTab by remember { mutableStateOf<NavigationTab?>(null) }

    val activeTab = previewTab ?: selectedTab

    val tabs = remember {
        listOf(
            NavigationTab.HOME,
            NavigationTab.DEVICES,
            NavigationTab.AI_ASSISTANT,
            NavigationTab.FILES,
            NavigationTab.SETTINGS
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.92f)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = forgeColors.shadowColor,
                    spotColor = forgeColors.shadowColor
                )
                .border(
                    BorderStroke(1.dp, if (forgeColors.isDark) GlassHighlightBorderBrush else LightGlassCardBorderBrush),
                    shape = RoundedCornerShape(34.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val tabWidth = size.width / 5f
                            val index = (offset.x / tabWidth).toInt().coerceIn(0, 4)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSelected(tabs[index])
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val tabWidth = size.width / 5f
                            val index = (offset.x / tabWidth).toInt().coerceIn(0, 4)
                            previewTab = tabs[index]
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val tabWidth = size.width / 5f
                            val index = (change.position.x / tabWidth).toInt().coerceIn(0, 4)
                            val target = tabs[index]
                            if (target != previewTab) {
                                previewTab = target
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            previewTab?.let { onTabSelected(it) }
                            previewTab = null
                        },
                        onDragCancel = {
                            previewTab = null
                        }
                    )
                },
            shape = RoundedCornerShape(34.dp),
            color = forgeColors.navBackground
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                ForgeNavIconButton(
                    icon = Icons.Rounded.Home,
                    contentDescription = "Home",
                    isSelected = activeTab == NavigationTab.HOME,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(NavigationTab.HOME)
                    }
                )

                // 2. Devices
                ForgeNavIconButton(
                    icon = Icons.Rounded.DesktopWindows,
                    contentDescription = "Devices",
                    isSelected = activeTab == NavigationTab.DEVICES,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(NavigationTab.DEVICES)
                    }
                )

                // 3. Central AI Capsule (Visually Prominent in glowing purple glass)
                val isAiSelected = activeTab == NavigationTab.AI_ASSISTANT
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            ambientColor = (if (forgeColors.isDark) RoyalViolet else LightAccent).copy(alpha = 0.6f),
                            spotColor = (if (forgeColors.isDark) LavenderAccent else LightAccentSoft).copy(alpha = 0.6f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isAiSelected) (if (forgeColors.isDark) RoyalViolet else LightAccent) else (if (forgeColors.isDark) RoyalVioletDark else Color(0xFF582C96)),
                                    if (isAiSelected) (if (forgeColors.isDark) LavenderAccent else LightAccentSoft) else (if (forgeColors.isDark) RoyalViolet else LightAccent)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                width = if (isAiSelected) 2.dp else 1.dp,
                                color = if (isAiSelected) PearlWhite else (if (forgeColors.isDark) Color(0x80BDA0FF) else LightAccentSoft)
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
                        contentDescription = "AI Cockpit",
                        tint = PearlWhite,
                        modifier = Modifier.size(23.dp)
                    )
                }

                // 4. Files
                ForgeNavIconButton(
                    icon = Icons.Rounded.Folder,
                    contentDescription = "Files",
                    isSelected = activeTab == NavigationTab.FILES,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(NavigationTab.FILES)
                    }
                )

                // 5. Settings
                ForgeNavIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    isSelected = activeTab == NavigationTab.SETTINGS,
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
    val forgeColors = ForgeTheme.colors
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) (if (forgeColors.isDark) LavenderAccent else LightAccent) else forgeColors.textSecondary,
        animationSpec = tween(200),
        label = "navTint"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    if (forgeColors.isDark) RoyalViolet.copy(alpha = 0.28f) else LightAccentSoft.copy(alpha = 0.35f)
                } else Color.Transparent
            )
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
