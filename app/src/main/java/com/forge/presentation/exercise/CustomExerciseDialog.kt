package com.forge.presentation.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.entity.EquipmentEntity
import com.forge.data.local.entity.MuscleEntity

val MOVEMENT_PATTERNS = listOf(
    "HORIZONTAL_PUSH",
    "VERTICAL_PUSH",
    "HORIZONTAL_PULL",
    "VERTICAL_PULL",
    "SQUAT",
    "HINGE",
    "LUNGE",
    "CARRY",
    "ISOLATION",
    "CORE"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomExerciseDialog(
    muscles: List<MuscleEntity>,
    equipment: List<EquipmentEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, primaryMuscleId: String?, equipmentId: String?, movementPattern: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf(muscles.firstOrNull()?.id ?: "chest") }
    var selectedEquipment by remember { mutableStateOf(equipment.firstOrNull()?.id ?: "barbell") }
    var selectedPattern by remember { mutableStateOf("HORIZONTAL_PUSH") }

    var muscleExpanded by remember { mutableStateOf(false) }
    var equipmentExpanded by remember { mutableStateOf(false) }
    var patternExpanded by remember { mutableStateOf(false) }

    val isValid = name.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "CREATE CUSTOM EXERCISE",
                color = ForgeTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exercise Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name", color = ForgeTheme.colors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ForgeTheme.colors.textPrimary,
                        unfocusedTextColor = ForgeTheme.colors.textPrimary,
                        focusedBorderColor = ForgeTheme.colors.primary,
                        unfocusedBorderColor = ForgeTheme.colors.divider,
                        cursorColor = ForgeTheme.colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Movement Pattern Selector
                ExposedDropdownMenuBox(
                    expanded = patternExpanded,
                    onExpandedChange = { patternExpanded = !patternExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPattern.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Movement Pattern", color = ForgeTheme.colors.textSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patternExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ForgeTheme.colors.textPrimary,
                            unfocusedTextColor = ForgeTheme.colors.textPrimary,
                            focusedBorderColor = ForgeTheme.colors.primary,
                            unfocusedBorderColor = ForgeTheme.colors.divider
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = patternExpanded,
                        onDismissRequest = { patternExpanded = false },
                        modifier = Modifier.background(ForgeTheme.colors.surfaceElevated)
                    ) {
                        MOVEMENT_PATTERNS.forEach { pattern ->
                            DropdownMenuItem(
                                text = { Text(pattern.replace("_", " "), color = ForgeTheme.colors.textPrimary) },
                                onClick = {
                                    selectedPattern = pattern
                                    patternExpanded = false
                                }
                            )
                        }
                    }
                }

                // Primary Muscle Selector
                ExposedDropdownMenuBox(
                    expanded = muscleExpanded,
                    onExpandedChange = { muscleExpanded = !muscleExpanded }
                ) {
                    val muscleName = muscles.find { it.id == selectedMuscle }?.name ?: selectedMuscle
                    OutlinedTextField(
                        value = muscleName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Muscle", color = ForgeTheme.colors.textSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = muscleExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ForgeTheme.colors.textPrimary,
                            unfocusedTextColor = ForgeTheme.colors.textPrimary,
                            focusedBorderColor = ForgeTheme.colors.primary,
                            unfocusedBorderColor = ForgeTheme.colors.divider
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = muscleExpanded,
                        onDismissRequest = { muscleExpanded = false },
                        modifier = Modifier.background(ForgeTheme.colors.surfaceElevated)
                    ) {
                        muscles.forEach { muscle ->
                            DropdownMenuItem(
                                text = { Text(muscle.name, color = ForgeTheme.colors.textPrimary) },
                                onClick = {
                                    selectedMuscle = muscle.id
                                    muscleExpanded = false
                                }
                            )
                        }
                    }
                }

                // Equipment Selector
                ExposedDropdownMenuBox(
                    expanded = equipmentExpanded,
                    onExpandedChange = { equipmentExpanded = !equipmentExpanded }
                ) {
                    val eqName = equipment.find { it.id == selectedEquipment }?.name ?: selectedEquipment
                    OutlinedTextField(
                        value = eqName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipment", color = ForgeTheme.colors.textSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ForgeTheme.colors.textPrimary,
                            unfocusedTextColor = ForgeTheme.colors.textPrimary,
                            focusedBorderColor = ForgeTheme.colors.primary,
                            unfocusedBorderColor = ForgeTheme.colors.divider
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = equipmentExpanded,
                        onDismissRequest = { equipmentExpanded = false },
                        modifier = Modifier.background(ForgeTheme.colors.surfaceElevated)
                    ) {
                        equipment.forEach { eq ->
                            DropdownMenuItem(
                                text = { Text(eq.name, color = ForgeTheme.colors.textPrimary) },
                                onClick = {
                                    selectedEquipment = eq.id
                                    equipmentExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(name, selectedMuscle, selectedEquipment, selectedPattern)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForgeTheme.colors.primary,
                    disabledContainerColor = ForgeTheme.colors.primary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Exercise", color = ForgeTheme.colors.background, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ForgeTheme.colors.textSecondary)
            }
        },
        containerColor = ForgeTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(12.dp)
    )
}
