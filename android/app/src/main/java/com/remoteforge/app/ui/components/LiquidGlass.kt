package com.remoteforge.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.ui.theme.*

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = GlassSurfaceDark,
    borderColor: Color = GlassBorderHighlight,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    } else Modifier

    Surface(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color(0x40110B1C),
                spotColor = Color(0x6029163D)
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            borderColor,
                            borderColor.copy(alpha = 0.25f),
                            Color(0x05FFFFFF)
                        )
                    )
                ),
                shape = shape
            )
            .then(clickableModifier),
        shape = shape,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun LiquidGlassBadge(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .border(
                BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = text,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector, val showInNavBar: Boolean = true) {
    HOME("Home", Icons.Rounded.Home),
    DEVICES("Devices", Icons.Rounded.DesktopWindows),
    AI_ASSISTANT("AI", Icons.Rounded.AutoAwesome),
    FILES("Files", Icons.Rounded.Folder),
    SETTINGS("Settings", Icons.Rounded.Settings),
    ACTIVITY("Activity", Icons.Rounded.History, showInNavBar = false),
    TRANSFERS("Transfers", Icons.Rounded.SwapVert, showInNavBar = false)
}

@Composable
fun LiquidGlassFloatingNavBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = Color(0x80110B1C),
                    spotColor = Color(0x9029163D)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                GlassPillBorder,
                                GlassBorderSubtle
                            )
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                ),
            shape = RoundedCornerShape(34.dp),
            color = GlassPillBackground
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.values().filter { it.showInNavBar }.forEach { tab ->
                    val isSelected = currentTab == tab
                    val isAi = tab == NavigationTab.AI_ASSISTANT

                    if (isAi) {
                        // Center prominent AI capsule button
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape,
                                    ambientColor = RoyalViolet.copy(alpha = 0.5f),
                                    spotColor = LavenderAccent.copy(alpha = 0.5f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(RoyalViolet, RoyalVioletDark)
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) LavenderAccent else Color(0x60BDA0FF)
                                    ),
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTabSelected(tab)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = PearlWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        val tabColor by animateColorAsState(
                            targetValue = if (isSelected) LavenderAccent else TextMutedDark,
                            animationSpec = tween(220),
                            label = "tabColor"
                        )

                        val bgModifier = if (isSelected) {
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(RoyalViolet.copy(alpha = 0.22f))
                        } else {
                            Modifier
                        }

                        Column(
                            modifier = Modifier
                                .then(bgModifier)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTabSelected(tab)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = tabColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
