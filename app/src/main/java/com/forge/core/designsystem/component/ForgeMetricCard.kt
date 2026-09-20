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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeSpacing
import com.forge.core.designsystem.theme.ForgeTheme

/** Visual hierarchy tiers for metric cards. */
enum class ForgeMetricTier { PRIMARY, SECONDARY, ACTION, INFO }

/**
 * Tiered metric card. Primary/Secondary communicate numbers, ACTION carries an
 * affordance, INFO is compact metadata. Not every piece of data needs a giant
 * rounded rectangle — vary the tier to build hierarchy.
 */
@Composable
fun ForgeMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    icon: ImageVector? = null,
    tier: ForgeMetricTier = ForgeMetricTier.SECONDARY,
    onClick: (() -> Unit)? = null
) {
    val variant = when (tier) {
        ForgeMetricTier.PRIMARY -> GlassVariant.STRONG
        ForgeMetricTier.SECONDARY -> GlassVariant.DEFAULT
        ForgeMetricTier.ACTION -> GlassVariant.SUBTLE
        ForgeMetricTier.INFO -> GlassVariant.SUBTLE
    }
    val valueSize = if (tier == ForgeMetricTier.PRIMARY) 28.sp else 22.sp
    val valueWeight = if (tier == ForgeMetricTier.PRIMARY) FontWeight.Bold else FontWeight.SemiBold
    val accent = tier == ForgeMetricTier.PRIMARY || tier == ForgeMetricTier.ACTION

    ForgeGlassSurface(
        modifier = modifier.fillMaxWidth(),
        variant = variant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ForgeSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (accent) ForgeTheme.colors.accent else ForgeTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(ForgeSpacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = valueSize,
                        fontWeight = valueWeight
                    )
                    if (unit != null) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = unit,
                            color = ForgeTheme.colors.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
            if (tier == ForgeMetricTier.ACTION) {
                Spacer(modifier = Modifier.size(ForgeSpacing.sm))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ForgeTheme.colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
