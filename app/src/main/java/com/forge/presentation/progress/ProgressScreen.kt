package com.forge.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeAmbientParticles
import com.forge.core.designsystem.component.ForgeGlassSurface
import com.forge.core.designsystem.component.ForgeGlassVariant
import com.forge.core.designsystem.component.ForgeSectionHeader
import com.forge.core.designsystem.component.ForgeSegmentedControl
import com.forge.core.designsystem.component.ForgeStatTile
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutSetEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf("PRs") }
    val scrollState = rememberScrollState()

    var selectedSessionForDetail by remember { mutableStateOf<WorkoutSessionEntity?>(null) }
    var detailSets by remember { mutableStateOf<List<WorkoutSetEntity>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {
        ForgeAmbientParticles(enabled = true, reduceMotion = false, particleCount = 16)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Text(
                text = "Progress",
                color = ForgeTheme.colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Lifetime Summary Tiles
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ForgeStatTile(
                        icon = Icons.Filled.FitnessCenter,
                        label = "Workouts",
                        value = "${uiState.totalWorkouts}",
                        unit = "logged",
                        modifier = Modifier.weight(1f)
                    )
                    ForgeStatTile(
                        icon = Icons.Filled.Speed,
                        label = "Volume",
                        value = String.format(Locale.US, "%.1f", uiState.totalVolumeKg / 1000.0),
                        unit = "tons",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ForgeStatTile(
                        icon = Icons.Filled.EmojiEvents,
                        label = "Records",
                        value = "${uiState.personalRecords.size}",
                        unit = "PRs",
                        modifier = Modifier.weight(1f)
                    )
                    ForgeStatTile(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "Training Time",
                        value = "${uiState.totalTrainingMinutes}",
                        unit = "mins",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab Selector
            ForgeSegmentedControl(
                options = listOf("PRs", "History"),
                selectedIndex = if (selectedTab == "PRs") 0 else 1,
                onSelectIndex = { selectedTab = if (it == 0) "PRs" else "History" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (selectedTab == "PRs") {
                ForgeSectionHeader(label = "Personal Records (${uiState.personalRecords.size})")

                if (uiState.personalRecords.isEmpty()) {
                    ForgeGlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = ForgeColors.GlassStroke,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Personal Records Yet",
                                color = ForgeTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Complete workouts and log heavy sets to establish your real PR baseline.",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (prItem in uiState.personalRecords) {
                            ForgeGlassSurface(
                                variant = ForgeGlassVariant.SUBTLE,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prItem.exerciseName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(prItem.pr.achievedAt))
                                        Text(
                                            text = "Achieved $dateStr • Est. 1RM: ${prItem.pr.estimated1RmKg.toInt()} kg",
                                            color = ForgeTheme.colors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${prItem.pr.maxWeightKg} kg",
                                            color = ForgeColors.CyanPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "${prItem.pr.maxRepsAtMaxWeight} reps",
                                            color = ForgeTheme.colors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Workout History Tab
                ForgeSectionHeader(label = "Session Log (${uiState.completedSessions.size})")

                if (uiState.completedSessions.isEmpty()) {
                    ForgeGlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                            Text(
                                text = "No workouts yet.",
                                color = ForgeTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Completed workouts appear here with full set breakdown and tonnage metrics.",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (session in uiState.completedSessions) {
                            ForgeGlassSurface(
                                variant = ForgeGlassVariant.SUBTLE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadSessionSets(session.id) { sets ->
                                            detailSets = sets
                                            selectedSessionForDetail = session
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.name,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(session.startTime))
                                        Text(
                                            text = "$dateStr • Tap to inspect sets",
                                            color = ForgeTheme.colors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${session.totalVolumeKg.toInt()} kg",
                                            color = ForgeColors.CyanPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val durationMin = session.durationSeconds / 60
                                        Text(
                                            text = "$durationMin min",
                                            color = ForgeTheme.colors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Historical Session Set Breakdown Modal Sheet
    selectedSessionForDetail?.let { session ->
        ModalBottomSheet(
            onDismissRequest = { selectedSessionForDetail = null },
            containerColor = ForgeTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(session.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        val dateStr = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US).format(Date(session.startTime))
                        Text(dateStr, color = ForgeTheme.colors.textSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = { selectedSessionForDetail = null }) {
                        Icon(Icons.Default.Close, null, tint = ForgeTheme.colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${session.totalVolumeKg.toInt()} kg", color = ForgeColors.CyanPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Volume", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${session.durationSeconds / 60} mins", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Duration", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${detailSets.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Sets Logged", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                ForgeSectionHeader(label = "Logged Sets Breakdown")

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detailSets) { setItem ->
                        ForgeGlassSurface(
                            variant = ForgeGlassVariant.SUBTLE,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ForgeColors.GlassFillSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${setItem.setOrder + 1}", color = ForgeColors.CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.size(10.dp))
                                    Text(
                                        text = setItem.exerciseId.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) },
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = "${setItem.weightKg} kg × ${setItem.reps} reps",
                                    color = ForgeColors.CyanPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
