package com.forge.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forge.core.designsystem.theme.ForgeTheme

enum class GlassVariant {
    DEFAULT,
    SUBTLE,
    STRONG,
    ELEVATED
}

/**
 * Liquid Glass visual language token modifier.
 * Matches the glass hierarchy from kenkouji/proto:
 * - DEFAULT: 5.5% white, 8% border
 * - SUBTLE: 3.5% white, 5% border
 * - STRONG: 9% white, 10% border
 * - ELEVATED: 7.5% white, 9% border
 */
fun Modifier.liquidGlass(
    variant: GlassVariant = GlassVariant.DEFAULT,
    cornerRadius: Dp = 24.dp
): Modifier = this.then(
    Modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            when (variant) {
                GlassVariant.DEFAULT -> Color(0x0EFFFFFF)
                GlassVariant.SUBTLE -> Color(0x09FFFFFF)
                GlassVariant.STRONG -> Color(0x17FFFFFF)
                GlassVariant.ELEVATED -> Color(0x13FFFFFF)
            },
            RoundedCornerShape(cornerRadius)
        )
        .border(
            1.dp,
            when (variant) {
                GlassVariant.DEFAULT -> Color(0x14FFFFFF)
                GlassVariant.SUBTLE -> Color(0x0DFFFFFF)
                GlassVariant.STRONG -> Color(0x1AFFFFFF)
                GlassVariant.ELEVATED -> Color(0x17FFFFFF)
            },
            RoundedCornerShape(cornerRadius)
        )
)

/**
 * Ambient background with soft radial glow orbs matching proto/src/components/forge/Layout.jsx:
 * - Top-center warm radiant orange glow (orange-500/10)
 * - Upper-left subtle sky blue glow (sky-500/5)
 * - Bottom-right subtle violet glow (violet-500/5)
 */
@Composable
fun ForgeAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Top-center warm orange glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x1EF97316), // Orange-500 12%
                        Color(0x08F97316),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, -height * 0.05f),
                    radius = width * 0.85f
                ),
                radius = width * 0.85f,
                center = Offset(width * 0.5f, -height * 0.05f)
            )

            // 2. Middle-left cool sky blue glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x0E38BDF8), // Sky-500 5.5%
                        Color(0x0438BDF8),
                        Color.Transparent
                    ),
                    center = Offset(-width * 0.1f, height * 0.35f),
                    radius = width * 0.65f
                ),
                radius = width * 0.65f,
                center = Offset(-width * 0.1f, height * 0.35f)
            )

            // 3. Bottom-right subtle violet glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x0EA78BFA), // Violet-500 5.5%
                        Color(0x03A78BFA),
                        Color.Transparent
                    ),
                    center = Offset(width * 1.1f, height * 0.95f),
                    radius = width * 0.75f
                ),
                radius = width * 0.75f,
                center = Offset(width * 1.1f, height * 0.95f)
            )
        }

        content()
    }
}
