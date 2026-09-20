package com.forge.presentation.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.forge.core.designsystem.component.liquidGlass
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.dao.ExerciseMuscleDetail
import com.forge.data.local.entity.EquipmentEntity
import com.forge.data.local.entity.ExerciseEntity

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    viewModel: ExerciseDetailViewModel,
    onNavigateBack: () -> Unit,
    onSiblingClick: (String) -> Unit = {},
    onAddToWorkout: ((ExerciseEntity) -> Unit)? = null
) {
    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    val exercise by viewModel.exercise.collectAsState()
    val muscles by viewModel.muscles.collectAsState()
    val equipment by viewModel.equipment.collectAsState()
    val attributes by viewModel.attributes.collectAsState()
    val family by viewModel.family.collectAsState()
    val siblings by viewModel.siblings.collectAsState()

    Scaffold(
        containerColor = ForgeTheme.colors.background
    ) { paddingValues ->
        if (exercise == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ForgeTheme.colors.primary)
            }
        } else {
            val ex = exercise!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("← BACK", color = ForgeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }

                    if (onAddToWorkout != null) {
                        Button(
                            onClick = { onAddToWorkout(ex) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ ADD TO WORKOUT", color = ForgeTheme.colors.background, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise Title
                Text(
                    text = ex.name,
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Movement Pattern & Category Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgePill(
                        text = ex.forgeMovementPattern.replace("_", " "),
                        color = ForgeTheme.colors.primary
                    )
                    if (!ex.sourceLevel.isNullOrBlank()) {
                        BadgePill(
                            text = ex.sourceLevel.replaceFirstChar { it.uppercase() },
                            color = ForgeTheme.colors.textSecondary
                        )
                    }
                    if (!ex.sourceMechanic.isNullOrBlank()) {
                        BadgePill(
                            text = ex.sourceMechanic.replaceFirstChar { it.uppercase() },
                            color = ForgeTheme.colors.textSecondary
                        )
                    }
                }

                // WATCH FORM Reference Video Action (Part 1 - YouTube Integration)
                val context = LocalContext.current
                if (ex.youtubeVideoId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                cornerRadius = 14.dp,
                                backgroundColor = Color(0x22EF4444),
                                borderColor = ForgeTheme.colors.error
                            )
                            .clickable {
                                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${ex.youtubeVideoId}"))
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${ex.youtubeVideoId}"))
                                try {
                                    context.startActivity(appIntent)
                                } catch (e: Exception) {
                                    context.startActivity(webIntent)
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("▶", color = ForgeTheme.colors.error, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "WATCH FORM",
                                        color = ForgeTheme.colors.primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Verified reference video tutorial",
                                        color = ForgeTheme.colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text("OPEN →", color = ForgeTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // License & Provenance Card (Correction #4)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ForgeTheme.colors.surface)
                        .border(1.dp, ForgeTheme.colors.divider, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PROVENANCE & LICENSING",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ForgeTheme.colors.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (ex.isCustom) "User Created" else "The Unlicense",
                                    color = ForgeTheme.colors.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (ex.isCustom) {
                                "Custom exercise created locally. Stored on-device with zero cloud synchronization."
                            } else {
                                "Source: Free Exercise DB · The Unlicense\nBiomechanical classifications and search indexing derived by FORGE. Zero third-party raster images are bundled."
                            },
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Target Muscles Section
                SectionHeader(title = "TARGET MUSCLES")
                Spacer(modifier = Modifier.height(8.dp))

                if (muscles.isEmpty()) {
                    Text("No muscle data recorded", color = ForgeTheme.colors.textSecondary, fontSize = 13.sp)
                } else {
                    val primaryMuscles = muscles.filter { it.role == "PRIMARY" }
                    val secondaryMuscles = muscles.filter { it.role == "SECONDARY" }

                    if (primaryMuscles.isNotEmpty()) {
                        Text("Primary Muscles", color = ForgeTheme.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        MusclePillsRow(primaryMuscles, isPrimary = true)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (secondaryMuscles.isNotEmpty()) {
                        Text("Secondary Muscles", color = ForgeTheme.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        MusclePillsRow(secondaryMuscles, isPrimary = false)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Equipment Section
                SectionHeader(title = "EQUIPMENT REQUIRED")
                Spacer(modifier = Modifier.height(8.dp))
                if (equipment.isEmpty()) {
                    Text(
                        text = ex.sourceEquipment?.replaceFirstChar { it.uppercase() } ?: "Bodyweight / None",
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 14.sp
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        equipment.forEach { eq ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ForgeTheme.colors.surface)
                                    .border(1.dp, ForgeTheme.colors.divider, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(eq.name, color = ForgeTheme.colors.textPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Execution Instructions Section
                SectionHeader(title = "EXECUTION & TECHNIQUE")
                Spacer(modifier = Modifier.height(8.dp))

                val steps = ex.instructions.split("\n").filter { it.isNotBlank() }
                if (steps.isEmpty()) {
                    Text("Instructions not provided by source dataset.", color = ForgeTheme.colors.textSecondary, fontSize = 13.sp)
                } else {
                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ForgeTheme.colors.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = ForgeTheme.colors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step.trim(),
                                color = ForgeTheme.colors.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Form Cues & Common Mistakes (Correction #5: Only displayed if non-empty, NEVER fabricated)
                if (ex.formCues.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "FORM CUES")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(ex.formCues, color = ForgeTheme.colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }

                if (ex.commonMistakes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "COMMON MISTAKES")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(ex.commonMistakes, color = ForgeTheme.colors.error.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 18.sp)
                }

                // Exercise Family Variations (Siblings)
                if (family != null && siblings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(title = "${family!!.name.uppercase()} VARIATIONS (${siblings.size})")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = family!!.description,
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    siblings.take(6).forEach { sibling ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ForgeTheme.colors.surface)
                                .border(1.dp, ForgeTheme.colors.divider, RoundedCornerShape(8.dp))
                                .clickable { onSiblingClick(sibling.id) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sibling.name,
                                    color = ForgeTheme.colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text("VIEW →", color = ForgeTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = ForgeTheme.colors.textSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusclePillsRow(muscles: List<ExerciseMuscleDetail>, isPrimary: Boolean) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        muscles.forEach { m ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isPrimary) ForgeTheme.colors.primary.copy(alpha = 0.18f)
                        else ForgeTheme.colors.surface
                    )
                    .border(
                        1.dp,
                        if (isPrimary) ForgeTheme.colors.primary else ForgeTheme.colors.divider,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${m.muscleName} (${m.bodyPart})",
                    color = if (isPrimary) ForgeTheme.colors.primary else ForgeTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun BadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
