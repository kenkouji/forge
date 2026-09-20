package com.forge.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeButton
import com.forge.core.designsystem.component.ForgeButtonSize
import com.forge.core.designsystem.component.ForgeButtonVariant
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.entity.UserProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    userProfile: UserProfileEntity?,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSave: (name: String, heightCm: Float, weightKg: Float, age: Int?, sex: String?, goal: String, experience: String, daysPerWeek: Int, durationMin: Int, equipment: String) -> Unit
) {
    var name by remember { mutableStateOf(userProfile?.name.orEmpty()) }
    var heightText by remember { mutableStateOf(userProfile?.heightCm?.toInt()?.toString() ?: "175") }
    var weightText by remember { mutableStateOf(userProfile?.weightKg?.toInt()?.toString() ?: "75") }
    var ageText by remember { mutableStateOf(userProfile?.age?.toString() ?: "26") }
    var sex by remember { mutableStateOf(userProfile?.sex ?: "Male") }
    var goal by remember { mutableStateOf(userProfile?.goal.orEmpty().ifEmpty { "Hypertrophy" }) }
    var experience by remember { mutableStateOf(userProfile?.experience.orEmpty().ifEmpty { "Intermediate" }) }
    var daysPerWeek by remember { mutableStateOf(userProfile?.daysPerWeek ?: 4) }
    var durationMin by remember { mutableStateOf(userProfile?.sessionDurationMin ?: 60) }
    var equipment by remember { mutableStateOf(userProfile?.equipment.orEmpty().ifEmpty { "Commercial Gym" }) }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ForgeTheme.colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EDIT ATHLETIC PROFILE",
                    color = ForgeColors.CyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ForgeTheme.colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Moniker / Name", color = ForgeTheme.colors.textSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForgeColors.CyanPrimary,
                    unfocusedBorderColor = ForgeColors.GlassStroke,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Body Metrics: Height, Weight, Age
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("Height (cm)", color = ForgeTheme.colors.textSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeColors.CyanPrimary,
                        unfocusedBorderColor = ForgeColors.GlassStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)", color = ForgeTheme.colors.textSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeColors.CyanPrimary,
                        unfocusedBorderColor = ForgeColors.GlassStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    label = { Text("Age", color = ForgeTheme.colors.textSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeColors.CyanPrimary,
                        unfocusedBorderColor = ForgeColors.GlassStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Goal
            Text("Goal: $goal", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Aesthetic", "Hypertrophy", "Strength", "Fat Loss").forEach { g ->
                    val isSel = goal.contains(g, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) ForgeColors.CyanPrimary else ForgeColors.GlassFillSubtle)
                            .clickable { goal = g }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(g, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Experience
            Text("Experience: $experience", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Beginner", "Intermediate", "Advanced").forEach { exp ->
                    val isSel = experience.equals(exp, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) ForgeColors.CyanPrimary else ForgeColors.GlassFillSubtle)
                            .clickable { experience = exp }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(exp, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Frequency & Duration
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = daysPerWeek.toString(),
                    onValueChange = { daysPerWeek = it.toIntOrNull()?.coerceIn(1, 7) ?: daysPerWeek },
                    label = { Text("Days / Week", color = ForgeTheme.colors.textSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeColors.CyanPrimary,
                        unfocusedBorderColor = ForgeColors.GlassStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = durationMin.toString(),
                    onValueChange = { durationMin = it.toIntOrNull()?.coerceIn(20, 180) ?: durationMin },
                    label = { Text("Duration (min)", color = ForgeTheme.colors.textSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeColors.CyanPrimary,
                        unfocusedBorderColor = ForgeColors.GlassStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Action
            ForgeButton(
                onClick = {
                    val h = heightText.toFloatOrNull() ?: 175f
                    val w = weightText.toFloatOrNull() ?: 75f
                    val a = ageText.toIntOrNull()
                    onSave(name, h, w, a, sex, goal, experience, daysPerWeek, durationMin, equipment)
                    onDismiss()
                },
                text = "SAVE PROFILE CHANGES",
                variant = ForgeButtonVariant.ACCENT,
                size = ForgeButtonSize.LG,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
