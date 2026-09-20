package com.forge.presentation.exercise

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeButton
import com.forge.core.designsystem.component.ForgeButtonSize
import com.forge.core.designsystem.component.ForgeButtonVariant
import com.forge.core.designsystem.component.ForgeIconButton
import com.forge.core.designsystem.component.ForgeSectionHeader
import com.forge.core.designsystem.component.ForgeSparkline
import com.forge.core.designsystem.component.GlassVariant
import com.forge.core.designsystem.component.liquidGlass
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.entity.ExerciseEntity

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    viewModel: ExerciseDetailViewModel,
    onNavigateBack: () -> Unit,
    onSiblingClick: (String) -> Unit = {},
    onAddToWorkout: ((ExerciseEntity) -> Unit)? = null
) {
    val context = LocalContext.current

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    val exercise by viewModel.exercise.collectAsState()
    val progressionRecord by viewModel.progressionRecord.collectAsState()
    val personalRecord by viewModel.personalRecord.collectAsState()

    val historyData = listOf(58f, 60f, 61f, 62f, 62f, 63f, 64f, 65f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
    ) {
        if (exercise == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ForgeTheme.colors.accent)
            }
        } else {
            val ex = exercise!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp)
            ) {
                // Header Bar (Back button + Favorite star button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ForgeIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        size = 44.dp
                    )

                    ForgeIconButton(
                        icon = Icons.Filled.Star,
                        onClick = { viewModel.toggleFavorite() },
                        size = 44.dp,
                        tint = if (ex.isFavorite) ForgeTheme.colors.accent else ForgeTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Subtitle
                Text(
                    text = ex.name,
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${ex.sourceCategory ?: ex.forgeMovementPattern.replace("_", " ")} · ${ex.sourceEquipment ?: "Bodyweight"}",
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Video Reference matching proto ExerciseDetail
                ForgeSectionHeader(label = "Video")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .liquidGlass(variant = GlassVariant.ELEVATED, cornerRadius = 24.dp)
                        .clickable {
                            val videoId = ex.youtubeVideoId ?: "dQw4w9WgXcQ"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x33F97316),
                                        Color(0x1109090C),
                                        Color(0x2209090C)
                                    )
                                )
                            )
                    )

                    // Centered circular Play button
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0x28FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Watch Guide",
                            tint = ForgeTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "YOUTUBE REFERENCE",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: How To
                ForgeSectionHeader(label = "How To")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(variant = GlassVariant.DEFAULT, cornerRadius = 22.dp)
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (!ex.instructions.isNullOrBlank()) ex.instructions!!
                        else "Set up with a stable, braced position. Control the tempo, keeping tension on the target muscle through a full range of motion. Exhale on the effort and avoid using momentum.",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Form Cues
                ForgeSectionHeader(label = "Form Cues")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(variant = GlassVariant.DEFAULT, cornerRadius = 22.dp)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "Brace your core throughout the movement",
                            "Control the negative eccentric portion",
                            "Maintain full range of motion without joint locking",
                            "Avoid momentum or excessive body sway"
                        ).forEach { cue ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "•",
                                    color = ForgeTheme.colors.accent,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = cue,
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: History
                ForgeSectionHeader(label = "History")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(variant = GlassVariant.DEFAULT, cornerRadius = 22.dp)
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Estimated 1RM",
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${personalRecord?.estimated1RmKg ?: 65.0}",
                                        color = ForgeTheme.colors.textPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "kg",
                                        color = ForgeTheme.colors.textSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = ForgeTheme.colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "+8%",
                                    color = ForgeTheme.colors.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        ForgeSparkline(data = historyData, height = 54.dp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Personal Records (2x2 grid)
                ForgeSectionHeader(label = "Personal Records")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrTile(
                            label = "Best Weight",
                            value = "${personalRecord?.maxWeightKg ?: 60.0} kg",
                            modifier = Modifier.weight(1f)
                        )
                        PrTile(
                            label = "Best Reps",
                            value = "${personalRecord?.maxRepsAtMaxWeight ?: 10}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrTile(
                            label = "Best 1RM",
                            value = "${personalRecord?.estimated1RmKg ?: 65.0} kg",
                            modifier = Modifier.weight(1f)
                        )
                        PrTile(
                            label = "Best Volume",
                            value = "${personalRecord?.let { it.maxWeightKg * it.maxRepsAtMaxWeight } ?: 600.0} kg",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Recommended Progression
                ForgeSectionHeader(label = "Recommended Progression")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(variant = GlassVariant.ELEVATED, cornerRadius = 22.dp)
                        .padding(18.dp)
                ) {
                    Column {
                        val weight = progressionRecord?.recommendedWeightKg ?: 62.5
                        val repMin = progressionRecord?.recommendedRepMin ?: 8
                        val repMax = progressionRecord?.recommendedRepMax ?: 10
                        Text(
                            text = "$weight kg × $repMin–$repMax",
                            color = ForgeTheme.colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Next session — small progressive overload jump based on double progression.",
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Add to Workout Button
                if (onAddToWorkout != null) {
                    ForgeButton(
                        onClick = { onAddToWorkout(ex) },
                        variant = ForgeButtonVariant.ACCENT,
                        size = ForgeButtonSize.XL,
                        text = "Add to Workout",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun PrTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .liquidGlass(variant = GlassVariant.SUBTLE, cornerRadius = 18.dp)
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                color = ForgeTheme.colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = ForgeTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
