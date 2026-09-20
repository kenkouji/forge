package com.forge.presentation.exercise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeButton
import com.forge.core.designsystem.component.ForgeButtonSize
import com.forge.core.designsystem.component.ForgeButtonVariant
import com.forge.core.designsystem.component.ForgeChip
import com.forge.core.designsystem.component.ForgeIconButton
import com.forge.core.designsystem.component.GlassVariant
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onNavigateBack != null) {
                        ForgeIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = onNavigateBack,
                            size = 42.dp
                        )
                    }
                    Text(
                        text = "Exercise Library",
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                ForgeIconButton(
                    icon = Icons.Filled.Add,
                    onClick = { showCustomDialog = true },
                    size = 42.dp
                )
            }

            // Search Bar matching proto ExercisePicker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(Color(0x0EFFFFFF), CircleShape)
                    .border(1.dp, Color(0x14FFFFFF), CircleShape)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = ForgeTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    BasicTextField(
                        value = filterState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = ForgeTheme.colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(ForgeTheme.colors.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (filterState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search 876 exercises",
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (filterState.searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = ForgeTheme.colors.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.onSearchQueryChange("") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips Row (Popular, Recent, Favorites, Browse All)
            if (filterState.searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(ExerciseLibraryTab.POPULAR, "Popular"),
                        Pair(ExerciseLibraryTab.RECENT, "Recent"),
                        Pair(ExerciseLibraryTab.FAVORITES, "Favorites"),
                        Pair(ExerciseLibraryTab.BROWSE_ALL, "Browse All")
                    ).forEach { (tab, label) ->
                        ForgeChip(
                            selected = filterState.activeTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            label = label
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Exercise List
            if (isSeeding) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ForgeTheme.colors.accent, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading canonical exercises...",
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                        .liquidGlass(variant = GlassVariant.SUBTLE, cornerRadius = 24.dp)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No movements found",
                            color = ForgeTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search query or filter",
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        val muscleLabel = exercise.sourceCategory ?: exercise.forgeMovementPattern.replace("_", " ")
                        val muscleColor = ForgeTheme.colors.getMuscleColor(muscleLabel)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(variant = GlassVariant.DEFAULT, cornerRadius = 20.dp)
                                .clickable { onExerciseClick(exercise.id) }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Colored initial square matching proto
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(muscleColor.copy(alpha = 0.15f))
                                        .border(1.dp, muscleColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = muscleLabel.take(1).uppercase(java.util.Locale.ROOT),
                                        color = muscleColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        color = ForgeTheme.colors.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$muscleLabel · ${exercise.sourceEquipment ?: "Bodyweight"}",
                                        color = ForgeTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                if (exercise.isFavorite) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = ForgeTheme.colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(110.dp))
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomExerciseDialog(
            muscles = muscles,
            equipment = equipment,
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, pattern, muscleId, equipmentId ->
                viewModel.createCustomExercise(name, pattern, muscleId, equipmentId)
                showCustomDialog = false
            }
        )
    }
}
