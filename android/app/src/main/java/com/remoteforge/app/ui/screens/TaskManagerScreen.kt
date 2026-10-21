package com.remoteforge.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.TaskItem
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.GlassCard
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen(
    deviceId: String?,
    repository: RemoteForgeRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tasks by repository.tasks.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.refreshTasks()
    }

    val filteredTasks = remember(tasks, selectedFilter, deviceId) {
        tasks.filter { task ->
            val matchesDevice = deviceId == null || task.deviceId == null || task.deviceId == deviceId
            val matchesStatus = when (selectedFilter) {
                "RUNNING" -> task.status in listOf("RUNNING", "PENDING")
                "COMPLETED" -> task.status == "COMPLETED"
                "FAILED" -> task.status == "FAILED"
                "CANCELLED" -> task.status in listOf("CANCELLED", "INTERRUPTED")
                else -> true
            }
            matchesDevice && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Persistent Tasks",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${tasks.size} durable operations tracked",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.cleanupTasks(keepCount = 50).onSuccess {
                                    Toast.makeText(context, "Purged old tasks", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Cleanup",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            coroutineScope.launch {
                                repository.refreshTasks()
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = RoyalViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GlassBackgroundDark
                )
            )
        },
        containerColor = GlassBackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("ALL", "RUNNING", "COMPLETED", "FAILED", "CANCELLED")) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) RoyalViolet.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (isSelected) RoyalViolet else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Task List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks found for filter '$selectedFilter'.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            onCancel = {
                                coroutineScope.launch {
                                    repository.cancelTask(task.id).onSuccess {
                                        Toast.makeText(context, "Task cancelled", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Cancel failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onRetry = {
                                coroutineScope.launch {
                                    repository.retryTask(task.id).onSuccess {
                                        Toast.makeText(context, "Task restarted", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Retry failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    task: TaskItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val statusColor = when (task.status.uppercase()) {
        "COMPLETED" -> EmeraldOnline
        "RUNNING", "PENDING" -> RoyalViolet
        "FAILED" -> RubyOffline
        "CANCELLED" -> AmberWarning
        "INTERRUPTED" -> Color(0xFFB388FF)
        else -> Color.White.copy(alpha = 0.5f)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.tool ?: "task",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${task.id.take(8)}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                task.deviceId?.let { dev ->
                    Text(
                        text = "Device: $dev",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f))
                    .border(1.dp, statusColor, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = task.status,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        task.errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = err,
                color = RubyOffline.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        task.createdAt?.let { created ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Started: $created",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 10.sp
            )
        }

        // Action Buttons
        val isRunning = task.status in listOf("RUNNING", "PENDING")
        val isTerminal = task.status in listOf("FAILED", "CANCELLED", "INTERRUPTED")

        if (isRunning || isTerminal) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning)
                    ) {
                        Text(text = "Cancel Task", fontSize = 11.sp)
                    }
                } else if (isTerminal) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet.copy(alpha = 0.3f))
                    ) {
                        Text(text = "Retry Task", color = RoyalViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
