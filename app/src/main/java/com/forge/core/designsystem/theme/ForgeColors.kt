package com.forge.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic design tokens for FORGE.
 * Matches the dark-first Liquid Glass design system from kenkouji/proto.
 */
@Immutable
data class ForgeColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val glassSubtle: Color,
    val glassSubtleBorder: Color,
    val glassStrong: Color,
    val glassStrongBorder: Color,
    val glassElevated: Color,
    val glassElevatedBorder: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val divider: Color,
    val textTertiary: Color = Color(0xFF636370),
    val onPrimary: Color = Color.Black,
    // Muscle category colors
    val muscleChest: Color = Color(0xFFFB7185),
    val muscleBack: Color = Color(0xFF38BDF8),
    val muscleShoulders: Color = Color(0xFFA78BFA),
    val muscleArms: Color = Color(0xFF34D399),
    val muscleQuads: Color = Color(0xFFF59E0B),
    val muscleGlutes: Color = Color(0xFFFB923C),
    val muscleCore: Color = Color(0xFFFBBF24)
) {
    companion object {
        val CyanPrimary = Color(0xFF38BDF8)
        val AmberGlow = Color(0xFFF97316)
        val NeonPurple = Color(0xFFA78BFA)
        val GlassStroke = Color(0x1AFFFFFF)
        val GlassFillSubtle = Color(0x09FFFFFF)
        val GlassFillStrong = Color(0x17FFFFFF)
        val CrimsonAlert = Color(0xFFEF4444)
    }

    fun getMuscleColor(muscle: String): Color {
        val m = muscle.lowercase()
        return when {
            m.contains("chest") || m.contains("pectoral") -> muscleChest
            m.contains("back") || m.contains("lat") || m.contains("trapezius") -> muscleBack
            m.contains("shoulder") || m.contains("delt") -> muscleShoulders
            m.contains("bicep") || m.contains("tricep") || m.contains("arm") || m.contains("forearm") -> muscleArms
            m.contains("quad") || m.contains("hamstring") -> muscleQuads
            m.contains("glute") || m.contains("calf") || m.contains("calves") -> muscleGlutes
            m.contains("core") || m.contains("ab") -> muscleCore
            else -> Color(0xFF94A3B8)
        }
    }
}

val DarkForgeColors = ForgeColors(
    background = Color(0xFF09090C),        // Deep Obsidian Void (hsl(240, 14%, 4%))
    surface = Color(0xFF131318),           // Card baseline
    surfaceElevated = Color(0xFF1B1B22),   // Elevated card baseline
    glassSurface = Color(0x0EFFFFFF),      // ~5.5% white glass layer
    glassBorder = Color(0x14FFFFFF),       // ~8% white border
    glassSubtle = Color(0x09FFFFFF),       // ~3.5% white subtle glass
    glassSubtleBorder = Color(0x0DFFFFFF), // ~5% white subtle border
    glassStrong = Color(0x17FFFFFF),       // ~9% white strong glass
    glassStrongBorder = Color(0x1AFFFFFF), // ~10% white strong border
    glassElevated = Color(0x13FFFFFF),     // ~7.5% white elevated glass
    glassElevatedBorder = Color(0x17FFFFFF),// ~9% white elevated border
    primary = Color(0xFFFAF7F2),           // Warm Titanium White (hsl(40, 30%, 96%))
    secondary = Color(0xFF9696A2),         // Muted Slate Neutral (hsl(240, 6%, 60%))
    accent = Color(0xFFF97316),            // Radiant Forge Amber/Orange (hsl(20, 92%, 60%))
    textPrimary = Color(0xFFFAF7F2),       // High-contrast readable typography
    textSecondary = Color(0xFF9696A2),     // Secondary units & captions
    textMuted = Color(0xFF636370),         // Subtle metadata & tags
    success = Color(0xFF10B981),           // Clean Emerald for PRs & completed sets
    warning = Color(0xFFF59E0B),           // Warm Amber
    error = Color(0xFFEF4444),             // Clean Crimson
    divider = Color(0x14FFFFFF)            // Subtle translucent divider
)

val LocalForgeColors = staticCompositionLocalOf<ForgeColors> {
    error("No ForgeColors provided. Did you wrap your content in ForgeTheme?")
}
