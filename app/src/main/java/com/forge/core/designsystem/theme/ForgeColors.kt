package com.forge.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic design tokens for FORGE.
 * Prevents hard-coding hex colors across screens and allows theme evolution.
 */
@Immutable
data class ForgeColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val divider: Color
)

val DarkForgeColors = ForgeColors(
    background = Color(0xFF090A0D),        // Deep Obsidian / Onyx
    surface = Color(0xFF13151B),           // Card baseline
    surfaceElevated = Color(0xFF1B1E26),   // Elevated card baseline
    glassSurface = Color(0x14FFFFFF),      // 8% white glass layer
    glassBorder = Color(0x1FFFFFFF),       // 12% translucent light highlight rim
    primary = Color(0xFFF8FAFC),           // Crisp Titanium White (Apple-grade high contrast)
    secondary = Color(0xFF94A3B8),         // Slate neutral
    accent = Color(0xFF38BDF8),            // Subtle athletic titanium blue
    textPrimary = Color(0xFFF8FAFC),       // High-contrast readable typography
    textSecondary = Color(0xFF94A3B8),     // Secondary units & captions
    textMuted = Color(0xFF64748B),         // Subtle metadata & tags
    success = Color(0xFF10B981),           // Clean Emerald for PRs & completed sets
    warning = Color(0xFFF59E0B),           // Warm Amber fatigue warning
    error = Color(0xFFEF4444),             // Clean Crimson for discard/errors
    divider = Color(0x1FFFFFFF)            // 12% translucent divider
)

val LocalForgeColors = staticCompositionLocalOf<ForgeColors> {
    error("No ForgeColors provided. Did you wrap your content in ForgeTheme?")
}
