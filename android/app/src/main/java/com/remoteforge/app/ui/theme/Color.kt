package com.remoteforge.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// RemoteForge Liquid Glass Palette (Obsidian / Deep Plum / Royal Violet / Lavender / Pearl)
val ObsidianBase = Color(0xFF110B1C)
val DeepPlum = Color(0xFF29163D)
val RoyalViolet = Color(0xFF6136A6)
val RoyalVioletDark = Color(0xFF432375)
val LavenderAccent = Color(0xFFBDA0FF)
val LavenderMuted = Color(0x80BDA0FF)
val PearlWhite = Color(0xFFF2EDFA)
val PearlMuted = Color(0xFFD4C8E6)

// Surface and Elevation
val GlassBackgroundDark = Color(0xFF110B1C)
val GlassSurfaceDark = Color(0xCC29163D)
val GlassSurfaceElevated = Color(0xE6381E54)
val GlassBorderHighlight = Color(0x40BDA0FF)
val GlassBorderSubtle = Color(0x1AFFFFFF)
val GlassBorderViolet = Color(0x556136A6)

// Semantic Accent Colors (Zero Cyan)
val EmeraldOnline = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val RubyOffline = Color(0xFFEF4444)

// Text Colors
val TextPrimaryDark = Color(0xFFF2EDFA)
val TextSecondaryDark = Color(0xFFC4B5D6)
val TextMutedDark = Color(0xFF8B779E)

// Light Theme Palette (Pearl Lavender Liquid Glass System)
val LightBackground = Color(0xFFF7F3FC)
val LightSurface = Color(0xEBFFFCFF)
val LightSurfaceSecondary = Color(0xECEEE6F8)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightAccent = Color(0xFF7041B8)
val LightAccentSoft = Color(0xFFDCCBFA)
val LightTextPrimary = Color(0xFF241630)
val LightTextSecondary = Color(0xFF65566F)
val LightTextMuted = Color(0xFF8C7B99)
val LightBorder = Color(0xFFDCD0EA)
val LightBorderSubtle = Color(0x80DCD0EA)

// Pill Navigation Bar
val GlassPillBackground = Color(0xEE1E1130)
val GlassPillBorder = Color(0x4DBDA0FF)
val LightGlassPillBackground = Color(0xF2FFFCFF)
val LightGlassPillBorder = Color(0x80DCD0EA)

// Specular Glass Highlight Gradients
val GlassCardBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x60BDA0FF),
        Color(0x206136A6),
        Color(0x05FFFFFF)
    )
)

val LightGlassCardBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x807041B8),
        Color(0x40DCCBFA),
        Color(0x15DCD0EA)
    )
)

val VioletLavenderGradient = Brush.horizontalGradient(
    colors = listOf(RoyalViolet, LavenderAccent)
)

val LightVioletLavenderGradient = Brush.horizontalGradient(
    colors = listOf(LightAccent, LightAccentSoft)
)

val GlassAccentGlow = Brush.radialGradient(
    colors = listOf(Color(0x406136A6), Color(0x00000000))
)

// Zero-Cyan Compatibility Aliases
val ElectricCyan = RoyalViolet
val AccentCyan = RoyalViolet
val AccentSky = LavenderAccent
val AccentIndigo = LavenderAccent
val NeonIndigo = LavenderAccent
val AccentViolet = RoyalViolet
val AccentBlue = RoyalViolet
val AccentEmerald = EmeraldOnline
val AccentAmber = AmberWarning
val AccentRuby = RubyOffline
val CyanIndigoGradient = VioletLavenderGradient
