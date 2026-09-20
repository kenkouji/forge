package com.forge.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Centralized FORGE spatial design tokens.
 *
 * Semantic spacing, corner radii, touch targets, elevation and blur tiers
 * for the dark-first Liquid Glass design system. Use these instead of
 * scattered literal dp values so the visual rhythm stays coherent and
 * compact across every screen.
 */
@Immutable
object ForgeSpacing {
    // Inline / hairline spacing
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    // Section rhythm
    val sectionGap = 14.dp
    val cardPadding = 16.dp
    val cardPaddingCompact = 12.dp
    val screenHorizontal = 20.dp
    val screenVerticalTop = 20.dp
    val bottomNavOffset = 110.dp

    // Corner radii
    val radiusSm = 12.dp
    val radiusMd = 16.dp
    val radiusLg = 20.dp
    val radiusXl = 28.dp
    val radiusPill = 28.dp

    // Touch targets (>= 48dp for accessibility)
    val touchMin = 48.dp
    val touchControl = 56.dp
    val touchLarge = 64.dp

    // Elevation (restrained for the dark glass aesthetic)
    val elevationNone = 0.dp
    val elevationLow = 2.dp
    val elevationMedium = 6.dp
    val elevationHigh = 12.dp

    // Blur tiers (translucency levels)
    const val blurSubtle = 16f
    const val blurDefault = 24f
    const val blurStrong = 30f
    const val blurElevated = 28f

    // Glass alpha tiers (white overlays)
    const val glassAlphaSubtle = 0.035f
    const val glassAlphaDefault = 0.055f
    const val glassAlphaStrong = 0.09f
    const val glassAlphaElevated = 0.075f
    const val glassBorderAlpha = 0.08f
}
