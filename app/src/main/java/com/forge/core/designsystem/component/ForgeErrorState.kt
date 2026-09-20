package com.forge.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeSpacing
import com.forge.core.designsystem.theme.ForgeTheme

/**
 * Error / permission / unavailable card. Visually consistent across the app:
 * subtle surface, restrained warning tint, single retry action.
 */
@Composable
fun ForgeErrorState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Filled.WarningAmber,
    actionLabel: String = "Retry",
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ForgeSpacing.screenHorizontal, vertical = ForgeSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ForgeTheme.colors.error.copy(alpha = 0.12f))
                    .border(1.dp, ForgeTheme.colors.error.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ForgeTheme.colors.error,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(ForgeSpacing.lg))
        }
        Text(
            text = title,
            color = ForgeTheme.colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ForgeSpacing.xs))
        Text(
            text = description,
            color = ForgeTheme.colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.82f)
        )
        if (onAction != null) {
            Spacer(modifier = Modifier.height(ForgeSpacing.xl))
            ForgeButton(
                onClick = onAction,
                variant = ForgeButtonVariant.SUBTLE,
                size = ForgeButtonSize.MD,
                icon = Icons.Filled.Refresh,
                text = actionLabel
            )
        }
    }
}
