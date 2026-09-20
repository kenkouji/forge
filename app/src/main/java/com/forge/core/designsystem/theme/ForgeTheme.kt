package com.forge.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val MaterialDarkColors = darkColorScheme(
    primary = DarkForgeColors.primary,
    onPrimary = DarkForgeColors.background,
    secondary = DarkForgeColors.secondary,
    onSecondary = DarkForgeColors.background,
    background = DarkForgeColors.background,
    onBackground = DarkForgeColors.textPrimary,
    surface = DarkForgeColors.surface,
    onSurface = DarkForgeColors.textPrimary,
    surfaceVariant = DarkForgeColors.surfaceElevated,
    onSurfaceVariant = DarkForgeColors.textSecondary,
    error = DarkForgeColors.error,
    onError = DarkForgeColors.textPrimary
)

@Composable
fun ForgeTheme(
    colors: ForgeColors = DarkForgeColors,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalForgeColors provides colors
    ) {
        MaterialTheme(
            colorScheme = MaterialDarkColors,
            typography = ForgeTypography,
            content = content
        )
    }
}

object ForgeTheme {
    val colors: ForgeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalForgeColors.current
}
