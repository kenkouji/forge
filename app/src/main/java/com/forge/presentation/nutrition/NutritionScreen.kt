package com.forge.presentation.nutrition

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeAmbientParticles
import com.forge.core.designsystem.component.ForgeGlassSurface
import com.forge.core.designsystem.component.ForgeGlassVariant
import com.forge.core.designsystem.component.ForgeSectionHeader
import com.forge.core.designsystem.component.ForgeStatTile
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme

@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        ForgeAmbientParticles(enabled = true, reduceMotion = false, particleCount = 16)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ForgeColors.GlassFillSubtle)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ForgeTheme.colors.textPrimary
                    )
                }
                Text(
                    text = "Nutrition Targets",
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calorie Target Card
            ForgeGlassSurface(
                variant = ForgeGlassVariant.ELEVATED,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DAILY ENERGY GOAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForgeColors.CyanPrimary,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${uiState.targetCalories}",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "kcal target",
                        fontSize = 13.sp,
                        color = ForgeTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Macronutrient Targets
            ForgeSectionHeader(label = "Macronutrient Prescription")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ForgeStatTile(
                    icon = Icons.Filled.FitnessCenter,
                    label = "Protein Target",
                    value = "${uiState.targetProteinGrams}",
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
                ForgeStatTile(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Active Burned",
                    value = if (uiState.activeCalories > 0) "${uiState.activeCalories}" else "—",
                    unit = "kcal",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Energy Expenditure from Health Connect
            ForgeSectionHeader(label = "Daily Metabolic Output")
            ForgeGlassSurface(
                variant = ForgeGlassVariant.SUBTLE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsWalk, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Health Connect Steps", color = Color.White, fontSize = 13.sp)
                        }
                        Text(
                            text = if (uiState.steps > 0) "${uiState.steps}" else "No sync",
                            color = if (uiState.steps > 0) ForgeColors.CyanPrimary else ForgeTheme.colors.textTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FitnessCenter, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Latest Training Session", color = Color.White, fontSize = 13.sp)
                        }
                        Text(
                            text = uiState.latestWorkoutName?.let { "$it (${uiState.latestWorkoutMinutes}m)" } ?: "None today",
                            color = if (uiState.latestWorkoutName != null) Color.White else ForgeTheme.colors.textTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
