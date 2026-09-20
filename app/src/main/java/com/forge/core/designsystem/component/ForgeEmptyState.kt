package com.forge.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
 * Premium empty state. Centered icon in a subtle glass disc, title, description
 * and an optional action. Use across every screen when there is no data — never
 * fill a screen with fake content.
 */
@Composable
fun ForgeEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Filled.AutoAwesome,
    actionLabel: String? = null,
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
                    .background(ForgeTheme.colors.glassSubtle)
                    .border(1.dp, ForgeTheme.colors.glassSubtleBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ForgeTheme.colors.textMuted,
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
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(ForgeSpacing.xl))
            ForgeButton(
                onClick = onAction,
                variant = ForgeButtonVariant.GLASS,
                size = ForgeButtonSize.MD,
                text = actionLabel
            )
        }
    }
}
