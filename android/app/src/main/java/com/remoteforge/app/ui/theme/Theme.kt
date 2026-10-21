package com.remoteforge.app.ui.theme

import android.app.Activity
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class ForgeColors(
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val navBackground: Color,
    val navBorder: Color,
    val shadowColor: Color,
    val isDark: Boolean
)

val DarkForgeColors = ForgeColors(
    background = ObsidianBase,
    surface = GlassSurfaceDark,
    surfaceSecondary = GlassSurfaceElevated,
    surfaceElevated = Color(0xF02A1842),
    border = GlassBorderSubtle,
    borderSubtle = Color(0x18FFFFFF),
    borderHighlight = GlassBorderHighlight,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
    accent = RoyalViolet,
    accentSoft = LavenderAccent,
    navBackground = GlassPillBackground,
    navBorder = GlassPillBorder,
    shadowColor = Color(0x90110B1C),
    isDark = true
)

val LightForgeColors = ForgeColors(
    background = LightBackground, // #F7F3FC
    surface = LightSurface,       // #FFFCFF / milky-white translucent
    surfaceSecondary = LightSurfaceSecondary, // #EEE6F8
    surfaceElevated = LightSurfaceElevated,   // #FFFFFF
    border = LightBorder,         // #DCD0EA
    borderSubtle = LightBorderSubtle,
    borderHighlight = LightAccentSoft, // #DCCBFA
    textPrimary = LightTextPrimary,    // #241630
    textSecondary = LightTextSecondary,// #65566F
    textMuted = LightTextMuted,        // #8C7B99
    accent = LightAccent,              // #7041B8
    accentSoft = LightAccentSoft,      // #DCCBFA
    navBackground = LightGlassPillBackground,
    navBorder = LightGlassPillBorder,
    shadowColor = Color(0x257041B8),   // soft diffuse lilac shadows
    isDark = false
)

val LocalForgeColors = staticCompositionLocalOf { DarkForgeColors }

@Immutable
data class ForgeMotionTokens(
    val shortMs: Int = 150,
    val standardMs: Int = 240,
    val screenMs: Int = 320,
    val reducedMotion: Boolean = false
) {
    val durationShort: Int get() = if (reducedMotion) 0 else shortMs
    val durationStandard: Int get() = if (reducedMotion) 0 else standardMs
    val durationScreen: Int get() = if (reducedMotion) 0 else screenMs
    val easeStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val easeEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val easeSmooth = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
}

val LocalForgeMotion = staticCompositionLocalOf { ForgeMotionTokens() }

object ForgeTheme {
    val colors: ForgeColors
        @Composable
        get() = LocalForgeColors.current

    val motion: ForgeMotionTokens
        @Composable
        get() = LocalForgeMotion.current
}

private val DarkColorScheme = darkColorScheme(
    primary = RoyalViolet,
    secondary = LavenderAccent,
    tertiary = EmeraldOnline,
    background = ObsidianBase,
    surface = DeepPlum,
    onPrimary = PearlWhite,
    onSecondary = ObsidianBase,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = RubyOffline
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    secondary = LightAccentSoft,
    tertiary = EmeraldOnline,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = LightTextPrimary,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    error = RubyOffline
)

@Composable
fun RemoteForgeTheme(
    themePreference: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themePreference.uppercase()) {
        "LIGHT" -> false
        "SYSTEM" -> systemDark
        else -> true // default DARK
    }

    val forgeColors = if (isDark) DarkForgeColors else LightForgeColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val motionTokens = ForgeMotionTokens(reducedMotion = reducedMotion)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = forgeColors.background
            window.statusBarColor = bg.toArgb()
            window.navigationBarColor = bg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalForgeColors provides forgeColors,
        LocalForgeMotion provides motionTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RemoteForgeTypography,
            content = content
        )
    }
}
