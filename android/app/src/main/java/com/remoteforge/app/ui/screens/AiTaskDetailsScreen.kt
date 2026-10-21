package com.remoteforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.TaskItem
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTaskDetailsScreen(
    taskId: String,
    repository: RemoteForgeRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tasks by repository.tasks.collectAsState()
    val task = tasks.firstOrNull { it.id == taskId }

    var actionFeedback by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Task Execution Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Task ID: ${taskId.take(16)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = RoyalViolet
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            repository.refreshTasks()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Task",
                            tint = RoyalViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (task == null) {
                EmptyState(
                    title = "Task Not Found",
                    description = "Task $taskId could not be found in active memory.",
                    icon = Icons.Rounded.SearchOff,
                    actionLabel = "Back to AI",
                    onAction = onBack
                )
            } else {
                val isRunning = task.status == "RUNNING" || task.status == "PENDING"
                val statusAccent = when (task.status) {
                    "COMPLETED" -> EmeraldOnline
                    "RUNNING" -> RoyalViolet
                    "FAILED", "REJECTED" -> RubyOffline
                    else -> AmberWarning
                }

                // Task Status Hero Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = statusAccent.copy(alpha = 0.4f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(statusAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Rounded.Sync else Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = statusAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Tool: ${task.tool}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Permission: ${task.permissionLevel ?: "LOW"} • Target: ${task.deviceId?.take(10) ?: "---"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMutedDark
                                )
                            }
                        }

                        StatusIndicator(
                            text = task.status,
                            accentColor = statusAccent,
                            isPulsing = isRunning
                        )
                    }

                    if (isRunning) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = RoyalViolet,
                            trackColor = GlassSurfaceElevated
                        )
                    }
                }

                // Arguments JSON Card
                Text(
                    text = "TOOL CALL ARGUMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = task.argumentsJson?.ifEmpty { "{}" } ?: "{}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = RoyalViolet
                        )
                    }
                }

                // Execution Result / Output Card
                Text(
                    text = "EXECUTION RESULT & TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (task.errorMessage != null) RubyOffline.copy(alpha = 0.4f) else GlassBorderSubtle,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    if (task.errorMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Error, contentDescription = null, tint = RubyOffline, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Error: ${task.errorMessage}",
                                color = RubyOffline,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (task.resultJson != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = task.resultJson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimaryDark
                            )
                        }
                    } else {
                        Text(
                            text = if (isRunning) "Execution in progress on agent..." else "No output payload returned.",
                            color = TextMutedDark,
                            fontSize = 12.sp
                        )
                    }
                }

                // Timeline Timestamps Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CREATED AT", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                            Text(
                                text = task.createdAt?.take(19)?.replace("T", " ") ?: "Recently",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryDark
                            )
                        }

                        task.completedAt?.let { comp ->
                            Column {
                                Text("COMPLETED AT", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                                Text(
                                    text = comp.take(19).replace("T", " "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                    }
                }

                // Action Notification
                actionFeedback?.let { msg ->
                    Text(text = msg, color = RoyalViolet, fontSize = 12.sp)
                }

                // Action Buttons
                if (isRunning) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                actionFeedback = "Task termination requested"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RubyOffline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Task Execution", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
