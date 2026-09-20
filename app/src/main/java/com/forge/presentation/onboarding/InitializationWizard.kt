package com.forge.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeAmbientParticles
import com.forge.core.designsystem.component.ForgeButton
import com.forge.core.designsystem.component.ForgeButtonSize
import com.forge.core.designsystem.component.ForgeButtonVariant
import com.forge.core.designsystem.component.ForgeGlassSurface
import com.forge.core.designsystem.component.ForgeGlassVariant
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InitializationWizard(
    viewModel: InitializationViewModel,
    onInitializationComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
    ) {
        ForgeAmbientParticles(enabled = true, reduceMotion = false, particleCount = 20)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header Bar with Back and Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.currentStep > 1) {
                    IconButton(onClick = { viewModel.previousStep() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ForgeTheme.colors.textSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Step Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ForgeColors.GlassFillStrong)
                        .border(1.dp, ForgeColors.GlassStroke, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "STEP ${state.currentStep} OF 12",
                        color = ForgeColors.CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated step container
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "step_animation"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        1 -> Step1Name(state.name) { viewModel.updateName(it) }
                        2 -> Step2BodyTelemetry(
                            heightCm = state.heightCm,
                            weightKg = state.weightKg,
                            age = state.age,
                            sex = state.sex,
                            onUpdate = { h, w, a, s -> viewModel.updateBodyStats(h, w, a, s) }
                        )
                        3 -> Step3Goal(state.goal) { viewModel.updateGoal(it) }
                        4 -> Step4Experience(state.experienceLevel) { viewModel.updateExperience(it) }
                        5 -> Step5FrequencyAndDays(
                            selectedDays = state.selectedDays,
                            onToggleDay = { viewModel.toggleDay(it) }
                        )
                        6 -> Step6Duration(
                            selectedDuration = state.sessionDurationMin,
                            onSelect = { viewModel.updateDuration(it) }
                        )
                        7 -> Step7Equipment(
                            selectedEquipment = state.selectedEquipment,
                            onToggle = { viewModel.toggleEquipment(it) }
                        )
                        8 -> Step8ExistingProgram(
                            hasProgram = state.hasExistingProgram,
                            rawText = state.rawWorkoutImportText,
                            parsed = state.parsedImport,
                            onSetHasProgram = { viewModel.setHasExistingProgram(it) },
                            onParse = { viewModel.parseImportText(it) },
                            onClear = { viewModel.clearImport() }
                        )
                        9 -> Step9Split(state.splitPreference) { viewModel.updateSplit(it) }
                        10 -> Step10Nutrition(
                            strategy = state.nutritionStrategy,
                            maintenance = state.maintenanceCalories,
                            calories = state.targetCalories,
                            protein = state.targetProteinGrams,
                            carbs = state.targetCarbsGrams,
                            fat = state.targetFatGrams,
                            onUpdateStrategy = { viewModel.updateNutritionStrategy(it) },
                            onUpdateMacros = { c, p, cb, f -> viewModel.updateManualMacros(c, p, cb, f) }
                        )
                        11 -> Step11Vault(
                            password = state.vaultPassword,
                            confirm = state.vaultPasswordConfirm,
                            onUpdate = { p, c -> viewModel.updateVaultPassword(p, c) }
                        )
                        12 -> PermissionsSetupScreen(
                            healthConnectGranted = state.healthConnectGranted,
                            notificationsGranted = state.notificationsGranted,
                            cameraGranted = state.cameraGranted,
                            onRequestHealthConnect = { viewModel.updatePermissions(!state.healthConnectGranted, state.notificationsGranted, state.cameraGranted) },
                            onRequestNotifications = { viewModel.updatePermissions(state.healthConnectGranted, !state.notificationsGranted, state.cameraGranted) },
                            onRequestCamera = { viewModel.updatePermissions(state.healthConnectGranted, state.notificationsGranted, !state.cameraGranted) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            if (state.currentStep < 12) {
                ForgeButton(
                    onClick = { viewModel.nextStep() },
                    text = "CONTINUE",
                    variant = ForgeButtonVariant.PRIMARY,
                    size = ForgeButtonSize.LG,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                if (state.isCompleting) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ForgeColors.CyanPrimary)
                    }
                } else {
                    ForgeButton(
                        onClick = {
                            viewModel.finalizeInitialization {
                                onInitializationComplete()
                            }
                        },
                        text = "FORGE MY OPERATING SYSTEM",
                        variant = ForgeButtonVariant.ACCENT,
                        size = ForgeButtonSize.LG,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(
        text = title,
        color = ForgeTheme.colors.textPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = subtitle,
        color = ForgeTheme.colors.textSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
private fun Step1Name(name: String, onNameChange: (String) -> Unit) {
    StepHeader(title = "What is your moniker?", subtitle = "Your identity in the FORGE fitness operating system.")
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("e.g. Victor, Ares, Elena", color = ForgeTheme.colors.textTertiary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForgeColors.CyanPrimary,
            unfocusedBorderColor = ForgeColors.GlassStroke,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@Composable
private fun Step2BodyTelemetry(
    heightCm: Float,
    weightKg: Float,
    age: Int,
    sex: String,
    onUpdate: (Float, Float, Int, String) -> Unit
) {
    StepHeader(
        title = "Body Telemetry",
        subtitle = "Accurate physical metrics anchor your baseline metabolic rate, training volume tolerances, and target macros."
    )

    var hText by remember { mutableStateOf(heightCm.toInt().toString()) }
    var wText by remember { mutableStateOf(weightKg.toInt().toString()) }
    var aText by remember { mutableStateOf(age.toString()) }
    var sVal by remember { mutableStateOf(sex) }

    fun sync() {
        val h = hText.toFloatOrNull() ?: 175f
        val w = wText.toFloatOrNull() ?: 75f
        val a = aText.toIntOrNull() ?: 26
        onUpdate(h, w, a, sVal)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = hText,
                onValueChange = { hText = it; sync() },
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
                value = wText,
                onValueChange = { wText = it; sync() },
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
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = aText,
                onValueChange = { aText = it; sync() },
                label = { Text("Age", color = ForgeTheme.colors.textSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForgeColors.CyanPrimary,
                    unfocusedBorderColor = ForgeColors.GlassStroke,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Biological Sex", color = ForgeTheme.colors.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Male", "Female").forEach { sOpt ->
                        val isSelected = sVal.equals(sOpt, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ForgeColors.CyanPrimary else ForgeColors.GlassFillSubtle)
                                .clickable { sVal = sOpt; sync() }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sOpt,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step3Goal(selectedGoal: String, onSelect: (String) -> Unit) {
    StepHeader(title = "Primary Objective", subtitle = "Your training prescription and focal volume adapt to this goal.")
    val goals = listOf(
        "Aesthetic / V-Taper" to "Upper torso width, lateral delts, tapered waist focus.",
        "Hypertrophy" to "Maximal mechanical tension & metabolic stress for lean tissue growth.",
        "Strength" to "Heavy compound progressive overload in the 3–6 rep domain.",
        "Fat Loss" to "Caloric deficit preservation with high-intensity compound stimulus.",
        "Recomposition" to "Simultaneous adipose reduction and muscular adaptation.",
        "Athletic" to "Explosive triple extension, rotational force, and work capacity.",
        "Endurance" to "Aerobic base and muscular fatigue resistance.",
        "General Fitness" to "Balanced health, mobility, posture, and systemic longevity.",
        "Bodybuilding" to "Symmetrical muscular development across all muscle groups.",
        "Powerlifting" to "Specific 1RM mastery across Squat, Bench, and Deadlift."
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        goals.forEach { (title, desc) ->
            val isSelected = selectedGoal.equals(title, ignoreCase = true)
            ForgeGlassSurface(
                variant = if (isSelected) ForgeGlassVariant.STRONG else ForgeGlassVariant.SUBTLE,
                borderColor = if (isSelected) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                onClick = { onSelect(title) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else ForgeTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = desc,
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = ForgeColors.CyanPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step4Experience(selectedLevel: String, onSelect: (String) -> Unit) {
    StepHeader(title = "Training Experience", subtitle = "Determines systemic recovery demands and overload increments.")
    val levels = listOf(
        "Beginner" to "Under 1 year of consistent compound lifting. Fast progression.",
        "Intermediate" to "1 to 3 years of structured programming. Double progression.",
        "Advanced" to "3+ years of systematic periodization. Nuanced autoregulation."
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        levels.forEach { (title, desc) ->
            val isSelected = selectedLevel.equals(title, ignoreCase = true)
            ForgeGlassSurface(
                variant = if (isSelected) ForgeGlassVariant.STRONG else ForgeGlassVariant.SUBTLE,
                borderColor = if (isSelected) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                onClick = { onSelect(title) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = desc, color = ForgeTheme.colors.textSecondary, fontSize = 12.sp)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = ForgeColors.CyanPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Step5FrequencyAndDays(
    selectedDays: Set<Int>,
    onToggleDay: (Int) -> Unit
) {
    StepHeader(
        title = "Weekly Training Schedule",
        subtitle = "Select which days you will enter the gym. Explicit rest days prevent systemic fatigue accumulation."
    )

    val dayNames = listOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday",
        6 to "Saturday",
        7 to "Sunday"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        dayNames.forEach { (dayNum, name) ->
            val isTraining = selectedDays.contains(dayNum)
            ForgeGlassSurface(
                variant = if (isTraining) ForgeGlassVariant.STRONG else ForgeGlassVariant.SUBTLE,
                borderColor = if (isTraining) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                onClick = { onToggleDay(dayNum) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        color = if (isTraining) Color.White else ForgeTheme.colors.textSecondary,
                        fontWeight = if (isTraining) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isTraining) "TRAINING DAY" else "REST & RECOVERY",
                        color = if (isTraining) ForgeColors.CyanPrimary else ForgeTheme.colors.textTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Step6Duration(selectedDuration: Int, onSelect: (Int) -> Unit) {
    StepHeader(title = "Target Session Duration", subtitle = "Prescribed volume per session is tuned to fit your available window.")
    val durations = listOf(30, 45, 60, 75, 90)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        durations.forEach { mins ->
            val isSelected = selectedDuration == mins
            ForgeGlassSurface(
                variant = if (isSelected) ForgeGlassVariant.STRONG else ForgeGlassVariant.SUBTLE,
                borderColor = if (isSelected) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                onClick = { onSelect(mins) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (mins >= 90) "90+ Minutes" else "$mins Minutes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = ForgeColors.CyanPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step7Equipment(
    selectedEquipment: Set<String>,
    onToggle: (String) -> Unit
) {
    StepHeader(
        title = "Available Equipment",
        subtitle = "Select all tools available in your training environment to filter exercise substitutions."
    )

    val equipmentOptions = listOf(
        "Gym", "Home", "Barbell", "Dumbbells",
        "Machines", "Cables", "Bands", "Bodyweight", "Custom"
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        equipmentOptions.forEach { equip ->
            val isSelected = selectedEquipment.contains(equip)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) ForgeColors.CyanPrimary.copy(alpha = 0.2f) else ForgeColors.GlassFillSubtle)
                    .border(1.dp, if (isSelected) ForgeColors.CyanPrimary else ForgeColors.GlassStroke, RoundedCornerShape(12.dp))
                    .clickable { onToggle(equip) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = equip,
                        color = if (isSelected) Color.White else ForgeTheme.colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Check, null, tint = ForgeColors.CyanPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Step8ExistingProgram(
    hasProgram: Boolean,
    rawText: String,
    parsed: com.forge.domain.engine.ParsedWorkout?,
    onSetHasProgram: (Boolean) -> Unit,
    onParse: (String) -> Unit,
    onClear: () -> Unit
) {
    StepHeader(
        title = "Already Have a Program?",
        subtitle = "Paste your existing workout split. FORGE parses day, focus, sets, reps, and RIR deterministically."
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (!hasProgram) ForgeColors.CyanPrimary else ForgeColors.GlassFillSubtle)
                .clickable { onSetHasProgram(false) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("NO (Auto-Generate)", color = if (!hasProgram) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (hasProgram) ForgeColors.CyanPrimary else ForgeColors.GlassFillSubtle)
                .clickable { onSetHasProgram(true) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("YES (Import Routine)", color = if (hasProgram) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }

    if (hasProgram) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = rawText,
            onValueChange = onParse,
            placeholder = {
                Text(
                    text = "Monday: Chest + Biceps\nBench Press 3x8\nIncline DB Press 3x10-15\nCable Fly 3x12\nEZ Curl 3x10",
                    color = ForgeTheme.colors.textTertiary,
                    fontSize = 12.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ForgeColors.CyanPrimary,
                unfocusedBorderColor = ForgeColors.GlassStroke,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        parsed?.let { p ->
            Spacer(modifier = Modifier.height(12.dp))
            ForgeGlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PARSED ROUTINE (${p.exercises.size} movements)",
                        color = ForgeColors.CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    p.exercises.take(4).forEach { ex ->
                        Text(
                            text = "• ${ex.exerciseName} — ${ex.setsInfo.sets} sets × ${ex.setsInfo.repsMin}–${ex.setsInfo.repsMax} reps",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step9Split(selectedSplit: String, onSelect: (String) -> Unit) {
    StepHeader(title = "Prescribed Split Architecture", subtitle = "FORGE maps your available days across proven biomechanical groupings.")
    val splits = listOf(
        "Push / Pull / Legs" to "Optimal 3 to 6 day split separating pressing, pulling, and lower body.",
        "Upper / Lower" to "Antagonistic frequency hitting all major muscle groups twice weekly.",
        "Full Body Compound" to "High-frequency compound lifting ideal for 2 to 4 days weekly."
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        splits.forEach { (title, desc) ->
            val isSelected = selectedSplit.equals(title, ignoreCase = true)
            ForgeGlassSurface(
                variant = if (isSelected) ForgeGlassVariant.STRONG else ForgeGlassVariant.SUBTLE,
                borderColor = if (isSelected) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                onClick = { onSelect(title) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = desc, color = ForgeTheme.colors.textSecondary, fontSize = 12.sp)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = ForgeColors.CyanPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Step10Nutrition(
    strategy: String,
    maintenance: Int,
    calories: Int,
    protein: Int,
    carbs: Int,
    fat: Int,
    onUpdateStrategy: (String) -> Unit,
    onUpdateMacros: (Int, Int, Int, Int) -> Unit
) {
    StepHeader(
        title = "Metabolic & Macro Targets",
        subtitle = "Calculated from your body telemetry via Mifflin-St Jeor formula and adapted to your goal."
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Cut", "Maintain", "Recomp", "Lean Bulk").forEach { strat ->
            val isSelected = strategy.equals(strat, ignoreCase = true)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) ForgeColors.CyanPrimary else ForgeColors.GlassFillSubtle)
                    .clickable { onUpdateStrategy(strat) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strat,
                    color = if (isSelected) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ForgeGlassSurface(variant = ForgeGlassVariant.STRONG, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = "DAILY NUTRITIONAL TARGET", color = ForgeColors.CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Target Energy", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "$calories kcal", color = ForgeColors.CyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Text(text = "Maintenance: $maintenance kcal", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${protein}g", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Protein", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${carbs}g", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Carbs", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${fat}g", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Fat", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun Step11Vault(
    password: String,
    confirm: String,
    onUpdate: (String, String) -> Unit
) {
    StepHeader(
        title = "Transformation Vault Password",
        subtitle = "Protects your physique check-in chronicle. Raw password is never stored; verified via salted PBKDF2."
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = password,
            onValueChange = { onUpdate(it, confirm) },
            label = { Text("Vault Password", color = ForgeTheme.colors.textSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ForgeColors.CyanPrimary,
                unfocusedBorderColor = ForgeColors.GlassStroke,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        OutlinedTextField(
            value = confirm,
            onValueChange = { onUpdate(password, it) },
            label = { Text("Confirm Vault Password", color = ForgeTheme.colors.textSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (password.isNotEmpty() && password == confirm) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                unfocusedBorderColor = ForgeColors.GlassStroke,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}
