package com.forge.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeTheme

enum class ForgeButtonVariant {
    PRIMARY,
    ACCENT,
    GLASS,
    SUBTLE,
    DANGER
}

enum class ForgeButtonSize {
    SM,
    MD,
    LG,
    XL
}

/**
 * High-performance ForgeButton with glow animations, tactile press physics, and responsive sizing.
 * Variants: Primary (Titanium White), Accent (Radiant Orange), Glass (Glass Strong), Subtle (Glass Subtle), Danger (Crimson Red)
 */
@Composable
fun ForgeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ForgeButtonVariant = ForgeButtonVariant.PRIMARY,
    size: ForgeButtonSize = ForgeButtonSize.MD,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    text: String
) {
    val height = when (size) {
        ForgeButtonSize.SM -> 40.dp
        ForgeButtonSize.MD -> 48.dp
        ForgeButtonSize.LG -> 56.dp
        ForgeButtonSize.XL -> 64.dp
    }
    val fontSize = when (size) {
        ForgeButtonSize.SM -> 13.sp
        ForgeButtonSize.MD -> 14.sp
        ForgeButtonSize.LG -> 15.sp
        ForgeButtonSize.XL -> 16.sp
    }

    val (bg, textColor) = when (variant) {
        ForgeButtonVariant.PRIMARY -> Pair(ForgeTheme.colors.primary, Color(0xFF09090C))
        ForgeButtonVariant.ACCENT -> Pair(ForgeTheme.colors.accent, Color(0xFFFFFFFF))
        ForgeButtonVariant.GLASS -> Pair(Color(0x17FFFFFF), ForgeTheme.colors.primary)
        ForgeButtonVariant.SUBTLE -> Pair(Color(0x09FFFFFF), ForgeTheme.colors.primary)
        ForgeButtonVariant.DANGER -> Pair(Color(0xFFE53935), Color(0xFFFFFFFF))
    }

    val borderModifier = when (variant) {
        ForgeButtonVariant.GLASS -> Modifier.border(1.dp, Color(0x1AFFFFFF), CircleShape)
        ForgeButtonVariant.SUBTLE -> Modifier.border(1.dp, Color(0x0DFFFFFF), CircleShape)
        else -> Modifier
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(if (enabled) bg else bg.copy(alpha = 0.4f), CircleShape)
            .then(borderModifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Circular glass icon button matching proto IconButton:
 */
@Composable
fun ForgeIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = ForgeTheme.colors.textPrimary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x0EFFFFFF), CircleShape)
            .border(1.dp, Color(0x14FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

/**
 * Filter chip matching proto Chip:
 * Active: White fill + Dark text
 * Inactive: Glass subtle fill + Muted text
 */
@Composable
fun ForgeChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(
                if (selected) ForgeTheme.colors.primary else Color(0x09FFFFFF),
                CircleShape
            )
            .then(
                if (!selected) Modifier.border(1.dp, Color(0x0DFFFFFF), CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF09090C) else ForgeTheme.colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * Segmented control matching proto Segmented:
 * Container with glass-subtle background and sliding pill indicator.
 */
@Composable
fun <T> ForgeSegmentedControl(
    items: List<Pair<T, String>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(Color(0x09FFFFFF), CircleShape)
            .border(1.dp, Color(0x0DFFFFFF), CircleShape)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { (key, label) ->
                val isSelected = key == selectedItem
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) ForgeTheme.colors.primary else Color.Transparent,
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(key) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF09090C) else ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ForgeSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = options.mapIndexed { idx, title -> idx to title }
    ForgeSegmentedControl(
        items = items,
        selectedItem = selectedIndex,
        onItemSelected = onSelectIndex,
        modifier = modifier
    )
}

