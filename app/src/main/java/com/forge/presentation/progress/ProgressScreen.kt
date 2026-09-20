package com.forge.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.liquidGlass
import com.forge.core.designsystem.theme.ForgeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel? = null,
    modifier: Modifier = Modifier
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        ProgressContent(
            totalWorkouts = uiState.totalWorkouts,
            totalVolumeKg = uiState.totalVolumeKg,
            totalTrainingMinutes = uiState.totalTrainingMinutes,
            personalRecords = uiState.personalRecords,
            completedSessions = uiState.completedSessions,
            modifier = modifier
        )
    } else {
        ProgressContent(
            totalWorkouts = 0,
            totalVolumeKg = 0.0,
            totalTrainingMinutes = 0,
            personalRecords = emptyList(),
            completedSessions = emptyList(),
            modifier = modifier
        )
    }
}


@Composable
fun ProgressContent(
    totalWorkouts: Int,
    totalVolumeKg: Double,
    totalTrainingMinutes: Long,
    personalRecords: List<PrWithExercise>,
    completedSessions: List<com.forge.data.local.entity.WorkoutSessionEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PROGRESS",
                color = ForgeTheme.colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Volume, 1RM Trends & Personal Records",
                color = ForgeTheme.colors.textSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lifetime Summary Card (Liquid Glass)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 16.dp)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "LIFETIME OVERVIEW",
                        color = ForgeTheme.colors.secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = totalWorkouts.toString(),
                                color = ForgeTheme.colors.textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "WORKOUTS",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            val formattedVolume = if (totalVolumeKg >= 1000) {
                                String.format(Locale.US, "%.1f t", totalVolumeKg / 1000.0)
                            } else {
                                "${totalVolumeKg.toInt()} kg"
                            }
                            Text(
                                text = formattedVolume,
                                color = ForgeTheme.colors.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "TOTAL VOLUME",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            val hours = totalTrainingMinutes / 60
                            val mins = totalTrainingMinutes % 60
                            val formattedTime = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            Text(
                                text = formattedTime,
                                color = ForgeTheme.colors.accent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "TRAINING TIME",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Personal Records Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERSONAL RECORDS",
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${personalRecords.size} ESTABLISHED",
                    color = ForgeTheme.colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (personalRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 12.dp)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Complete your first workout session to record baseline personal records and 1RM benchmarks.",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(personalRecords, key = { it.pr.exerciseId }) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 12.dp)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.exerciseName,
                                color = ForgeTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Best: ${item.pr.maxWeightKg} kg × ${item.pr.maxRepsAtMaxWeight} reps",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${item.pr.estimated1RmKg} kg",
                                color = ForgeTheme.colors.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "EST. 1RM",
                                color = ForgeTheme.colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Workout History Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RECENT WORKOUTS",
                color = ForgeTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (completedSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 12.dp)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "No workouts completed yet. Start training from the Workouts tab to build momentum!",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(completedSessions, key = { it.id }) { session ->
                val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date(session.startTime))
                val durationMins = session.durationSeconds / 60

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 12.dp)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = session.name,
                                color = ForgeTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$dateStr • ${durationMins}m",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${session.totalVolumeKg.toInt()} kg",
                                color = ForgeTheme.colors.accent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "VOLUME",
                                color = ForgeTheme.colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(name = "Progress Screen Preview")
@Composable
fun ProgressScreenPreview() {
    ForgeTheme {
        ProgressContent(
            totalWorkouts = 14,
            totalVolumeKg = 24500.0,
            totalTrainingMinutes = 720,
            personalRecords = emptyList(),
            completedSessions = emptyList()
        )
    }
}
