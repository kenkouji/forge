package com.forge.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forge.core.designsystem.theme.ForgeMotion
import com.forge.core.designsystem.theme.ForgeSpacing
import com.forge.core.designsystem.theme.ForgeTheme

private val ShimmerColors
    @Composable get() = listOf(
        ForgeTheme.colors.surface,
        ForgeTheme.colors.glassSubtle,
        ForgeTheme.colors.surface
    )

/**
 * Shimmering placeholder block. Use for loading states instead of generic
 * circular spinners. Keep geometry tight to the content it represents.
 */
@Composable
fun ForgeSkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = ForgeSpacing.radiusSm
) {
    val transition = rememberInfiniteTransition(label = "forge_skeleton")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ForgeMotion.DurationLong, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )
    val colors = ShimmerColors
    val sweep = 600f
    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(phase * sweep - sweep / 2f, 0f),
        end = Offset(phase * sweep + sweep / 2f, 0f)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(ForgeTheme.colors.surface)
            .background(brush)
    )
}

/** A single shimmering text line. */
@Composable
fun ForgeSkeletonLine(
    widthFraction: Float = 1f,
    height: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    ForgeSkeletonBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        cornerRadius = height / 2f
    )
}

/** A card-shaped skeleton block with a few stacked lines. */
@Composable
fun ForgeSkeletonCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ForgeSkeletonLine(widthFraction = 0.6f, height = 14.dp)
        Spacer(modifier = Modifier.height(ForgeSpacing.sm))
        ForgeSkeletonLine(widthFraction = 0.9f)
        Spacer(modifier = Modifier.height(ForgeSpacing.xs))
        ForgeSkeletonLine(widthFraction = 0.75f)
    }
}
