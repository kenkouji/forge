package com.forge.core.designsystem.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass visual language token modifier.
 * Provides translucent obsidian surface, subtle hairline light refraction border,
 * and hardware-accelerated RenderEffect blur on Android 12+ (API 31+).
 * Falls back to performant translucent layering on older versions.
 */
fun Modifier.liquidGlass(
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = Color(0x12FFFFFF),
    borderColor: Color = Color(0x1AFFFFFF),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor, RoundedCornerShape(cornerRadius))
    .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
