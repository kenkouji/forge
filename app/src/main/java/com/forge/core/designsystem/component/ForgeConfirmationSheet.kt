package com.forge.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeSpacing
import com.forge.core.designsystem.theme.ForgeTheme

/**
 * Premium confirmation surface for AI-proposed actions. Place inside any bottom
 * sheet. Single change: title + subtitle. Multiple changes: a counted header
 * followed by the list. Cancel / Confirm always visible.
 *
 * Visual design only — does not execute any mutation.
 */
@Composable
fun ForgeConfirmationSheet(
    title: String,
    subtitle: String? = null,
    changes: List<String> = emptyList(),
    confirmLabel: String = "Confirm",
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    val multiple = changes.isNotEmpty()
    val header = if (multiple) "${changes.size} CHANGES" else title

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ForgeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ForgeSpacing.md)
    ) {
        Text(
            text = if (multiple) header else title,
            color = ForgeTheme.colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (multiple) {
            changes.forEach { change ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .padding(0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = ForgeTheme.colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(ForgeSpacing.sm))
                    Text(
                        text = change,
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (subtitle != null) {
            Text(
                text = subtitle,
                color = ForgeTheme.colors.textSecondary,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(ForgeSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.sm)
        ) {
            ForgeButton(
                onClick = onCancel,
                variant = ForgeButtonVariant.SUBTLE,
                size = ForgeButtonSize.LG,
                modifier = Modifier.weight(1f),
                text = "Cancel"
            )
            ForgeButton(
                onClick = onConfirm,
                variant = ForgeButtonVariant.PRIMARY,
                size = ForgeButtonSize.LG,
                modifier = Modifier.weight(1f),
                text = confirmLabel
            )
        }
    }
}
