package com.remoteforge.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Liquid Glass Core Palette
val GlassBackgroundDark = Color(0xFF080C14)
val GlassSurfaceDark = Color(0x2E1E293B)
val GlassSurfaceElevated = Color(0x40334155)
val GlassBorderHighlight = Color(0x35FFFFFF)
val GlassBorderSubtle = Color(0x18FFFFFF)

// Accent Colors
val ElectricCyan = Color(0xFF38BDF8)
val NeonIndigo = Color(0xFF818CF8)
val EmeraldOnline = Color(0xFF34D399)
val AmberWarning = Color(0xFFFBBF24)
val RubyOffline = Color(0xFFF87171)

// Text Colors
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Pill Navigation Bar
val GlassPillBackground = Color(0xE60D1524)
val GlassPillBorder = Color(0x38FFFFFF)

// Specular Glass Highlight Gradients
val GlassCardBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x4DFFFFFF),
        Color(0x14FFFFFF),
        Color(0x05FFFFFF)
    )
)

val CyanIndigoGradient = Brush.horizontalGradient(
    colors = listOf(ElectricCyan, NeonIndigo)
)

val GlassAccentGlow = Brush.radialGradient(
    colors = listOf(Color(0x2638BDF8), Color(0x00000000))
)
