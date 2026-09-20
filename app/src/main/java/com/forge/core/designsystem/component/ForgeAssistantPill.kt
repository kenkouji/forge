package com.forge.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeMotion
import com.forge.core.designsystem.theme.ForgeSpacing
import com.forge.core.designsystem.theme.ForgeTheme

/**
 * FORGE's intelligent layer. A subtle glass pill that sits between Today and
 * Training Snapshot. Interactive without becoming visually dominant — a soft
 * animated cyan glow communicates it is alive.
 */
@Composable
fun ForgeAssistantPill(
    prompt: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "forge_assistant")
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ForgeMotion.DurationExtended, easing = ForgeMotion.StandardEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "assistant_glow"
    )

    ForgeGlassSurface(
        modifier = modifier.fillMaxWidth(),
        variant = GlassVariant.STRONG,
        borderColor = ForgeColors.CyanPrimary.copy(alpha = glow * 0.5f),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ForgeSpacing.lg, vertical = ForgeSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ForgeColors.CyanPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = ForgeColors.CyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.size(ForgeSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Personalized Assistant",
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = prompt,
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.size(ForgeSpacing.sm))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ForgeColors.CyanPrimary.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ASK",
                    color = ForgeColors.CyanPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
