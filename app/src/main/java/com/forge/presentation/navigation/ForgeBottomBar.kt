package com.forge.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.forge.core.designsystem.component.GlassVariant
import com.forge.core.designsystem.component.liquidGlass
import com.forge.core.designsystem.theme.ForgeTheme

/**
 * Floating Pill Navigation Bar matching proto/src/components/forge/Layout.jsx:
 * - Floating glass-strong pill container centered above the navigation bar
 * - 5 items: Home, Workouts, Progress, Journey, Profile
 * - Animated pill indicator on active item (bg-white/12 border border-white/12)
 */
@Composable
fun ForgeBottomBar(
    currentRoute: String?,
    onNavigateToDestination: (ForgeNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .liquidGlass(variant = GlassVariant.STRONG, cornerRadius = 32.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ForgeNavDestination.rootDestinations.forEach { destination ->
                val isSelected = currentRoute == destination.route ||
                    (destination == ForgeNavDestination.Workouts &&
                        (currentRoute == ForgeNavDestination.ExerciseLibrary.route ||
                         currentRoute?.startsWith("exercise_detail") == true))

                val icon: ImageVector = when (destination) {
                    ForgeNavDestination.Home -> Icons.Filled.Home
                    ForgeNavDestination.Workouts -> Icons.Filled.FitnessCenter
                    ForgeNavDestination.Progress -> Icons.AutoMirrored.Filled.ShowChart
                    ForgeNavDestination.Journey -> Icons.Filled.Explore
                    ForgeNavDestination.Profile -> Icons.Filled.Person
                    else -> Icons.Filled.Home
                }

                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) ForgeTheme.colors.primary else Color(0x759696A2),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navIconTint"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0x1FFFFFFF) else Color.Transparent,
                            CircleShape
                        )
                        .then(
                            if (isSelected) Modifier.border(1.dp, Color(0x1FFFFFFF), CircleShape)
                            else Modifier
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onNavigateToDestination(destination) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = destination.title,
                        tint = tintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
