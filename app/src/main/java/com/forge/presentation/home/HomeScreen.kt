package com.forge.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.liquidGlass
import com.forge.core.designsystem.theme.ForgeTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    hasActiveWorkout: Boolean = false,
    activeWorkoutTitle: String? = null,
    onStartWorkoutClick: () -> Unit = {},
    onResumeWorkoutClick: () -> Unit = {},
    onExploreExercisesClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Brand Creed Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FORGE",
                    color = ForgeTheme.colors.primary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.5.sp
                )
                Text(
                    text = "Train. Adapt. Transform.",
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Consistency Streak Pill
            Box(
                modifier = Modifier
                    .liquidGlass(cornerRadius = 20.dp, backgroundColor = Color(0x1AFFFFFF))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ForgeTheme.colors.success)
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "4 DAY STREAK",
                        color = ForgeTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Hero: Today's Workout Command Center Card
        if (hasActiveWorkout) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        cornerRadius = 20.dp,
                        backgroundColor = Color(0x2210B981),
                        borderColor = ForgeTheme.colors.success
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WORKOUT IN PROGRESS",
                            color = ForgeTheme.colors.success,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ForgeTheme.colors.success)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color(0xFF090A0D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = activeWorkoutTitle ?: "Current Session",
                        color = ForgeTheme.colors.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Session logging active in background",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onResumeWorkoutClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForgeTheme.colors.primary,
                            contentColor = Color(0xFF090A0D)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "RESUME WORKOUT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 20.dp, backgroundColor = Color(0x16FFFFFF))
                    .padding(22.dp)
            ) {
                Column {
                    Text(
                        text = "TODAY'S SESSION",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Upper Body — Hypertrophy",
                        color = ForgeTheme.colors.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Bench Press • Incline DB Press • Lat Pulldowns • Rows",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onStartWorkoutClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForgeTheme.colors.primary,
                            contentColor = Color(0xFF090A0D)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "START WORKOUT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Training Status & Consistency Section
        Text(
            text = "TRAINING STATUS",
            color = ForgeTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Weekly Sessions Glass Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(cornerRadius = 16.dp)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "THIS WEEK",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "3 / 4",
                        color = ForgeTheme.colors.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "sessions done",
                        color = ForgeTheme.colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Volume Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(cornerRadius = 16.dp)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "WEEKLY VOLUME",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "14,820",
                        color = ForgeTheme.colors.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "kg total load",
                        color = ForgeTheme.colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Recent PR Progress
        Text(
            text = "RECENT PROGRESS",
            color = ForgeTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(cornerRadius = 16.dp)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Barbell Bench Press",
                        color = ForgeTheme.colors.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Estimated 1RM: 104 kg (+2.5 kg)",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2210B981))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+ PR",
                        color = ForgeTheme.colors.success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Actions
        Text(
            text = "QUICK ACTIONS",
            color = ForgeTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(cornerRadius = 14.dp)
                    .clickable(onClick = onStartWorkoutClick)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Launch Workout →",
                    color = ForgeTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(cornerRadius = 14.dp)
                    .clickable(onClick = onExploreExercisesClick)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Browse Exercises →",
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
