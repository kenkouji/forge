package com.forge.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeSpacing
import com.forge.core.designsystem.theme.ForgeTheme

/**
 * Floating rest timer. Visible without occupying the entire screen. Large timer
 * in the centre of an animated progress ring, with Pause/Skip/Adjust controls.
 */
@Composable
fun ForgeRestTimerSheet(
    remainingSeconds: Int,
    totalSeconds: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    onPauseToggle: () -> Unit = {},
    onSkip: () -> Unit = {},
    onAdjust: () -> Unit = {}
) {
    val safeTotal = if (totalSeconds > 0) totalSeconds else 1
    val progress = (remainingSeconds.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    val mm = (remainingSeconds / 60).coerceAtLeast(0)
    val ss = (remainingSeconds % 60).coerceAtLeast(0)
    val timeText = String.format(java.util.Locale.US, "%02d:%02d", mm, ss)

    ForgeGlassSurface(
        modifier = modifier.fillMaxWidth(),
        variant = GlassVariant.STRONG,
        cornerRadius = ForgeSpacing.radiusLg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ForgeSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rest",
                color = ForgeTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(ForgeSpacing.sm))
            ForgeRing(
                progress = progress,
                size = 160.dp,
                strokeWidth = 12.dp,
                progressColor = if (isPaused) ForgeTheme.colors.textMuted else ForgeColors.CyanPrimary
            ) {
                Text(
                    text = timeText,
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(ForgeSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = ForgeSpacing.xl),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RestTimerControl(
                    icon = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    label = if (isPaused) "Resume" else "Pause",
                    onClick = onPauseToggle
                )
                RestTimerControl(icon = Icons.Filled.Tune, label = "Adjust", onClick = onAdjust)
                RestTimerControl(icon = Icons.Filled.SkipNext, label = "Skip", onClick = onSkip)
            }
        }
    }
}

@Composable
private fun RestTimerControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ForgeIconButton(
            icon = icon,
            onClick = onClick,
            size = 52.dp
        )
        Spacer(modifier = Modifier.height(ForgeSpacing.xs))
        Text(
            text = label,
            color = ForgeTheme.colors.textSecondary,
            fontSize = 11.sp
        )
    }
}
