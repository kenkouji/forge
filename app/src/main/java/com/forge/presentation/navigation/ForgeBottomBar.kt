package com.forge.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeTheme

@Composable
fun ForgeBottomBar(
    currentRoute: String?,
    onNavigateToDestination: (ForgeNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = ForgeTheme.colors.divider

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(Color(0xEE090A0D))
            .navigationBarsPadding()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ForgeNavDestination.rootDestinations.forEach { destination ->
            val isSelected = currentRoute == destination.route ||
                (destination == ForgeNavDestination.Workouts && (currentRoute == ForgeNavDestination.ExerciseLibrary.route || currentRoute?.startsWith("exercise_detail") == true))

            val textColor by animateColorAsState(
                targetValue = if (isSelected) ForgeTheme.colors.primary else ForgeTheme.colors.textMuted,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "bottomBarTextColor"
            )

            val interactionSource = remember { MutableInteractionSource() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onNavigateToDestination(destination) }
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = destination.title,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    letterSpacing = 0.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtle Apple-like active pill indicator
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 16.dp else 0.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isSelected) ForgeTheme.colors.primary else Color.Transparent)
                )
            }
        }
    }
}
