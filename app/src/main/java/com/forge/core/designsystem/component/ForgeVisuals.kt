package com.forge.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeTheme

/**
 * Section Header matching proto SectionHeader:
 * uppercase tracking label + optional action text (e.g. "View all", "Add Photo")
 */
@Composable
fun ForgeSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            color = ForgeTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = ForgeTheme.colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

/**
 * Circular ring progress gauge matching proto Ring:
 * Displays animated progress arc with center composable content.
 */
@Composable
fun ForgeRing(
    progress: Float, // 0.0 to 1.0
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0x14FFFFFF),
    progressColor: Color = ForgeTheme.colors.accent,
    content: @Composable BoxScope.() -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "ring_progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokePx) / 2f
            val topLeft = Offset(
                (this.size.width - radius * 2) / 2f,
                (this.size.height - radius * 2) / 2f
            )
            val arcSize = Size(radius * 2, radius * 2)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        content()
    }
}

/**
 * Horizontal Progress Bar matching proto ProgressBar:
 * Smooth animated pill progress track.
 */
@Composable
fun ForgeProgressBar(
    progress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = ForgeTheme.colors.accent,
    trackColor: Color = Color(0x14FFFFFF)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "bar_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/**
 * Sparkline SVG-like canvas graph matching proto Sparkline:
 * Smooth cubic bezier curve with gradient fill below.
 */
@Composable
fun ForgeSparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    lineColor: Color = ForgeTheme.colors.accent
) {
    if (data.isEmpty()) return

    val min = data.minOrNull() ?: 0f
    val max = data.maxOrNull() ?: 1f
    val range = if (max - min > 0f) max - min else 1f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val stepX = if (data.size > 1) w / (data.size - 1) else w

        val points = data.mapIndexed { index, value ->
            val normalizedY = (value - min) / range
            val y = h - (normalizedY * (h - 14.dp.toPx())) - 7.dp.toPx()
            Offset(index * stepX, y)
        }

        if (points.isNotEmpty()) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX = (p0.x + p1.x) / 2f
                    cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }
            }

            val areaPath = Path().apply {
                addPath(path)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            // Fill area with gradient
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.28f),
                        lineColor.copy(alpha = 0.0f)
                    )
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Muscle Bar matching proto MuscleBar:
 * Horizontal muscle volume bar with label, track, and tonnage.
 */
@Composable
fun ForgeMuscleBar(
    label: String,
    value: Float,
    max: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pct = if (max > 0) (value / max).coerceIn(0f, 1f) else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ForgeTheme.colors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(80.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0x0EFFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${value.toInt()}",
            color = ForgeTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Stat Tile matching proto StatTile:
 * 2x2 grid card with icon, uppercase category tag, display number, and unit.
 */
@Composable
fun ForgeStatTile(
    icon: ImageVector? = null,
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .liquidGlass(variant = GlassVariant.SUBTLE, cornerRadius = 20.dp)
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ForgeTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label.uppercase(),
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

typealias ForgeGlassVariant = GlassVariant

@Composable
fun ForgeGlassSurface(
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.DEFAULT,
    borderColor: Color? = null,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .liquidGlass(variant = variant, cornerRadius = cornerRadius)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(cornerRadius)) else Modifier),
        content = content
    )
}

