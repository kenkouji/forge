package com.forge.presentation.exercise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.liquidGlass
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.entity.ExerciseEntity

enum class FilterSheetType {
    NONE, MUSCLE, EQUIPMENT, PATTERN, LEVEL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    viewModel: ExerciseLibraryViewModel,
    onExerciseClick: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val filterState by viewModel.filterState.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val isSeeding by viewModel.isSeeding.collectAsState()
    val muscles by viewModel.allMuscles.collectAsState()
    val equipment by viewModel.allEquipment.collectAsState()

    var showCustomDialog by remember { mutableStateOf(false) }
    var activeFilterSheet by remember { mutableStateOf(FilterSheetType.NONE) }

    val hasActiveFilters = filterState.selectedMuscleId != null ||
        filterState.selectedEquipmentId != null ||
        filterState.selectedMovementPattern != null ||
        filterState.selectedExperienceLevel != null

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCustomDialog = true },
                containerColor = ForgeTheme.colors.primary,
                contentColor = Color(0xFF090A0D),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CUSTOM", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        },
        containerColor = ForgeTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "EXERCISE LIBRARY",
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = when {
                            isSeeding -> "Seeding exercise ecosystem..."
                            filterState.searchQuery.isNotBlank() -> "Searching all 876+ exercises"
                            filterState.activeTab == ExerciseLibraryTab.POPULAR -> "Popular Core Movements (~${exercises.size})"
                            filterState.activeTab == ExerciseLibraryTab.RECENT -> "Recently Logged Exercises"
                            filterState.activeTab == ExerciseLibraryTab.FAVORITES -> "Starred Favorites"
                            else -> "All Canonical & Custom Movements (${exercises.size})"
                        },
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                if (onNavigateBack != null) {
                    TextButton(onClick = onNavigateBack) {
                        Text("BACK", color = ForgeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar (searches entire 876 database)
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text("Search 876+ movements, aliases, muscles...", color = ForgeTheme.colors.textMuted, fontSize = 13.sp)
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕",
                            color = ForgeTheme.colors.textSecondary,
                            modifier = Modifier
                                .clickable { viewModel.onSearchQueryChange("") }
                                .padding(8.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ForgeTheme.colors.textPrimary,
                    unfocusedTextColor = ForgeTheme.colors.textPrimary,
                    focusedContainerColor = ForgeTheme.colors.glassSurface,
                    unfocusedContainerColor = ForgeTheme.colors.glassSurface,
                    focusedBorderColor = ForgeTheme.colors.primary,
                    unfocusedBorderColor = ForgeTheme.colors.divider,
                    cursorColor = ForgeTheme.colors.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Section / Tab Selector (when not searching)
            if (filterState.searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ExerciseLibraryTab.values().forEach { tab ->
                        val isSelected = filterState.activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ForgeTheme.colors.primary else Color.Transparent)
                                .clickable { viewModel.selectTab(tab) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color(0xFF090A0D) else ForgeTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Filter Chips Row (Visible in Browse All mode)
            AnimatedVisibility(visible = filterState.searchQuery.isBlank() && filterState.activeTab == ExerciseLibraryTab.BROWSE_ALL) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChipPill(
                            label = filterState.selectedMuscleId?.let { id ->
                                muscles.find { it.id == id }?.name ?: id.replace("_", " ")
                            } ?: "All Muscles",
                            isActive = filterState.selectedMuscleId != null,
                            onClick = { activeFilterSheet = FilterSheetType.MUSCLE }
                        )

                        FilterChipPill(
                            label = filterState.selectedEquipmentId?.let { id ->
                                equipment.find { it.id == id }?.name ?: id.replace("_", " ")
                            } ?: "Equipment",
                            isActive = filterState.selectedEquipmentId != null,
                            onClick = { activeFilterSheet = FilterSheetType.EQUIPMENT }
                        )

                        FilterChipPill(
                            label = filterState.selectedMovementPattern?.replace("_", " ") ?: "Pattern",
                            isActive = filterState.selectedMovementPattern != null,
                            onClick = { activeFilterSheet = FilterSheetType.PATTERN }
                        )

                        FilterChipPill(
                            label = filterState.selectedExperienceLevel?.replaceFirstChar { it.uppercase() } ?: "Level",
                            isActive = filterState.selectedExperienceLevel != null,
                            onClick = { activeFilterSheet = FilterSheetType.LEVEL }
                        )

                        if (hasActiveFilters) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ForgeTheme.colors.error.copy(alpha = 0.15f))
                                    .clickable { viewModel.clearFilters() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Reset Filters ✕",
                                    color = ForgeTheme.colors.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Exercise List
            if (isSeeding && exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ForgeTheme.colors.primary, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Seeding Exercise Ecosystem...",
                            color = ForgeTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Populating 876 movements into local Room database",
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (filterState.activeTab == ExerciseLibraryTab.FAVORITES) {
                                "No Starred Favorites Yet"
                            } else if (filterState.activeTab == ExerciseLibraryTab.RECENT) {
                                "No Recently Logged Exercises"
                            } else {
                                "No matching exercises found"
                            },
                            color = ForgeTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (filterState.activeTab == ExerciseLibraryTab.FAVORITES) {
                                "Tap the star on any exercise card to add it to your Favorites."
                            } else if (filterState.activeTab == ExerciseLibraryTab.RECENT) {
                                "Completed exercises will appear here automatically."
                            } else {
                                "Try searching for a different term or clearing filters."
                            },
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        ExerciseListItemCard(
                            exercise = exercise,
                            onClick = { onExerciseClick(exercise.id) },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(exercise.id, exercise.isFavorite)
                            }
                        )
                    }
                }
            }
        }
    }

    // Filter Bottom Sheets
    if (activeFilterSheet != FilterSheetType.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeFilterSheet = FilterSheetType.NONE },
            sheetState = sheetState,
            containerColor = ForgeTheme.colors.surfaceElevated
        ) {
            when (activeFilterSheet) {
                FilterSheetType.MUSCLE -> {
                    FilterSelectionContent(
                        title = "Select Primary Muscle",
                        items = muscles.map { it.id to it.name },
                        selectedId = filterState.selectedMuscleId,
                        onSelect = {
                            viewModel.selectMuscle(it)
                            activeFilterSheet = FilterSheetType.NONE
                        }
                    )
                }
                FilterSheetType.EQUIPMENT -> {
                    FilterSelectionContent(
                        title = "Select Equipment",
                        items = equipment.map { it.id to it.name },
                        selectedId = filterState.selectedEquipmentId,
                        onSelect = {
                            viewModel.selectEquipment(it)
                            activeFilterSheet = FilterSheetType.NONE
                        }
                    )
                }
                FilterSheetType.PATTERN -> {
                    val patterns = listOf(
                        "HORIZONTAL_PUSH" to "Horizontal Push",
                        "VERTICAL_PUSH" to "Vertical Push",
                        "HORIZONTAL_PULL" to "Horizontal Pull",
                        "VERTICAL_PULL" to "Vertical Pull",
                        "SQUAT" to "Squat",
                        "HINGE" to "Hinge",
                        "LUNGE" to "Lunge",
                        "ISOLATION" to "Isolation",
                        "CORE" to "Core",
                        "CARDIO" to "Cardio"
                    )
                    FilterSelectionContent(
                        title = "Select Movement Pattern",
                        items = patterns,
                        selectedId = filterState.selectedMovementPattern,
                        onSelect = {
                            viewModel.selectMovementPattern(it)
                            activeFilterSheet = FilterSheetType.NONE
                        }
                    )
                }
                FilterSheetType.LEVEL -> {
                    val levels = listOf(
                        "beginner" to "Beginner",
                        "intermediate" to "Intermediate",
                        "expert" to "Expert"
                    )
                    FilterSelectionContent(
                        title = "Select Experience Level",
                        items = levels,
                        selectedId = filterState.selectedExperienceLevel,
                        onSelect = {
                            viewModel.selectExperienceLevel(it)
                            activeFilterSheet = FilterSheetType.NONE
                        }
                    )
                }
                FilterSheetType.NONE -> {}
            }
        }
    }

    if (showCustomDialog) {
        CustomExerciseDialog(
            muscles = muscles,
            equipment = equipment,
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, muscleId, eqId, pattern ->
                viewModel.createCustomExercise(name, muscleId, eqId, pattern)
                showCustomDialog = false
            }
        )
    }
}

@Composable
fun FilterChipPill(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .liquidGlass(
                cornerRadius = 16.dp,
                backgroundColor = if (isActive) ForgeTheme.colors.primary.copy(alpha = 0.2f) else Color(0x12FFFFFF),
                borderColor = if (isActive) ForgeTheme.colors.primary else Color(0x1FFFFFFF)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) ForgeTheme.colors.primary else ForgeTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSelectionContent(
    title: String,
    items: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = title,
            color = ForgeTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedId == null) ForgeTheme.colors.primary else Color(0x14FFFFFF))
                    .clickable { onSelect(null) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "All / Any",
                    color = if (selectedId == null) Color(0xFF090A0D) else ForgeTheme.colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = if (selectedId == null) FontWeight.Bold else FontWeight.Normal
                )
            }

            items.forEach { (id, name) ->
                val isSelected = selectedId == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ForgeTheme.colors.primary else Color(0x14FFFFFF))
                        .clickable { onSelect(id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) Color(0xFF090A0D) else ForgeTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseListItemCard(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 14.dp)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = exercise.name,
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (exercise.youtubeVideoId != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x26EF4444))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "▶ VIDEO",
                                color = ForgeTheme.colors.error,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (onToggleFavorite != null) {
                        Text(
                            text = if (exercise.isFavorite) "★" else "☆",
                            color = if (exercise.isFavorite) Color(0xFFFFB020) else ForgeTheme.colors.textMuted,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable { onToggleFavorite() }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pattern = exercise.forgeMovementPattern.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() }
                Text(
                    text = pattern,
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                if (exercise.sourceEquipment != null) {
                    Text("•", color = ForgeTheme.colors.textMuted, fontSize = 10.sp)
                    Text(
                        text = exercise.sourceEquipment.replaceFirstChar { it.uppercase() },
                        color = ForgeTheme.colors.textMuted,
                        fontSize = 12.sp
                    )
                }

                if (exercise.isPopular) {
                    Text("•", color = ForgeTheme.colors.textMuted, fontSize = 10.sp)
                    Text(
                        text = "Popular #${exercise.popularityRank}",
                        color = ForgeTheme.colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
