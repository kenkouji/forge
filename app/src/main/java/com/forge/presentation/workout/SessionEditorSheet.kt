package com.forge.presentation.workout

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.mutableStateListOf
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
import com.forge.core.designsystem.component.ForgeGlassSurface
import com.forge.core.designsystem.component.ForgeGlassVariant
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.entity.ExerciseEntity

data class EditableExerciseItem(
    val exerciseId: String,
    var exerciseName: String,
    var targetSets: Int = 3,
    var targetReps: String = "8-12",
    var targetRepsMin: Int = 8,
    var targetRepsMax: Int = 12,
    var targetWeightKg: Double? = null,
    var targetRir: Int = 2,
    var restSeconds: Int = 90,
    var isWarmup: Boolean = false,
    var isDropSet: Boolean = false,
    var notes: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorSheet(
    title: String,
    initialExercises: List<EditableExerciseItem>,
    availableExercises: List<ExerciseEntity>,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSave: (sessionTitle: String, exercises: List<EditableExerciseItem>) -> Unit
) {
    var sessionTitle by remember { mutableStateOf(title) }
    val exercises = remember { mutableStateListOf<EditableExerciseItem>().apply { addAll(initialExercises) } }
    var showAddExercisePicker by remember { mutableStateOf(false) }
    var swapExerciseIndex by remember { mutableStateOf<Int?>(null) }

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
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EDIT SESSION PREVIEW",
                    color = ForgeColors.CyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ForgeTheme.colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Session Title Input
            OutlinedTextField(
                value = sessionTitle,
                onValueChange = { sessionTitle = it },
                label = { Text("Session Name", color = ForgeTheme.colors.textTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForgeColors.CyanPrimary,
                    unfocusedBorderColor = ForgeColors.GlassStroke,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Exercise List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MOVEMENTS (${exercises.size})",
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                ForgeButton(
                    onClick = { showAddExercisePicker = true },
                    text = "+ Add Movement",
                    variant = ForgeButtonVariant.GLASS,
                    size = ForgeButtonSize.SM
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Exercises Reorderable List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(exercises) { index, item ->
                    ForgeGlassSurface(
                        variant = ForgeGlassVariant.SUBTLE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Row 1: Order, Name, Reorder, Swap, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(ForgeColors.CyanPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${index + 1}", color = ForgeColors.CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.exerciseName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Move Up
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val prev = exercises[index - 1]
                                                exercises[index - 1] = item
                                                exercises[index] = prev
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    // Move Down
                                    if (index < exercises.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val next = exercises[index + 1]
                                                exercises[index + 1] = item
                                                exercises[index] = next
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    // Swap
                                    IconButton(
                                        onClick = { swapExerciseIndex = index },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, "Swap", tint = ForgeColors.CyanPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    // Delete
                                    IconButton(
                                        onClick = { exercises.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = ForgeColors.CrimsonAlert, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 2: Sets, Reps, Weight, RIR inputs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sets
                                OutlinedTextField(
                                    value = item.targetSets.toString(),
                                    onValueChange = {
                                        val s = it.toIntOrNull()?.coerceIn(1, 10) ?: item.targetSets
                                        exercises[index] = item.copy(targetSets = s)
                                    },
                                    label = { Text("Sets", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ForgeColors.CyanPrimary,
                                        unfocusedBorderColor = ForgeColors.GlassStroke,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                // Reps
                                OutlinedTextField(
                                    value = item.targetReps,
                                    onValueChange = { repStr ->
                                        val parts = repStr.split("-").mapNotNull { it.trim().toIntOrNull() }
                                        val min = if (parts.isNotEmpty()) parts.first() else 8
                                        val max = if (parts.size > 1) parts[1] else min
                                        exercises[index] = item.copy(
                                            targetReps = repStr,
                                            targetRepsMin = min,
                                            targetRepsMax = max
                                        )
                                    },
                                    label = { Text("Reps", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.3f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ForgeColors.CyanPrimary,
                                        unfocusedBorderColor = ForgeColors.GlassStroke,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                // Weight kg
                                OutlinedTextField(
                                    value = item.targetWeightKg?.toString().orEmpty(),
                                    onValueChange = {
                                        val w = it.toDoubleOrNull()
                                        exercises[index] = item.copy(targetWeightKg = w)
                                    },
                                    placeholder = { Text("kg", fontSize = 10.sp) },
                                    label = { Text("Weight", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.2f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ForgeColors.CyanPrimary,
                                        unfocusedBorderColor = ForgeColors.GlassStroke,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                // RIR
                                OutlinedTextField(
                                    value = item.targetRir.toString(),
                                    onValueChange = {
                                        val r = it.toIntOrNull()?.coerceIn(0, 5) ?: 2
                                        exercises[index] = item.copy(targetRir = r)
                                    },
                                    label = { Text("RIR", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.9f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ForgeColors.CyanPrimary,
                                        unfocusedBorderColor = ForgeColors.GlassStroke,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Action
            ForgeButton(
                onClick = {
                    onSave(sessionTitle, exercises)
                    onDismiss()
                },
                text = "SAVE TEMPLATE CHANGES",
                variant = ForgeButtonVariant.PRIMARY,
                size = ForgeButtonSize.LG,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add Exercise Picker Modal
    if (showAddExercisePicker || swapExerciseIndex != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddExercisePicker = false
                swapExerciseIndex = null
            },
            containerColor = ForgeTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (swapExerciseIndex != null) "SWAP EXERCISE" else "ADD EXERCISE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(availableExercises.take(40)) { _, ex ->
                        ForgeGlassSurface(
                            variant = ForgeGlassVariant.SUBTLE,
                            onClick = {
                                val swapIdx = swapExerciseIndex
                                if (swapIdx != null) {
                                    exercises[swapIdx] = exercises[swapIdx].copy(
                                        exerciseId = ex.id,
                                        exerciseName = ex.name
                                    )
                                    swapExerciseIndex = null
                                } else {
                                    exercises.add(
                                        EditableExerciseItem(
                                            exerciseId = ex.id,
                                            exerciseName = ex.name,
                                            targetSets = 3,
                                            targetReps = "8-12",
                                            targetRepsMin = 8,
                                            targetRepsMax = 12
                                        )
                                    )
                                    showAddExercisePicker = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ex.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(ex.sourceCategory ?: ex.forgeMovementPattern, color = ForgeColors.CyanPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
