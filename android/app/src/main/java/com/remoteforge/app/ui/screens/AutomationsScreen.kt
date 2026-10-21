package com.remoteforge.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.remoteforge.app.data.model.AutomationItem
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.data.model.WorkflowStepItem
import com.remoteforge.app.data.model.WorkflowDefinitionItem
import com.remoteforge.app.data.model.WorkflowDryRunResponse
import com.remoteforge.app.data.model.WorkflowDryRunStepItem
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

enum class AutomationViewTab(val label: String) {
    WORKFLOWS("Workflows"),
    HISTORY("Execution Logs")
}

data class AutomationTemplate(
    val title: String,
    val tool: String,
    val schedule: String,
    val description: String,
    val iconName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsScreen(
    repository: RemoteForgeRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val automations by repository.automations.collectAsState()
    val history by repository.automationHistory.collectAsState()
    val devices by repository.devices.collectAsState()
    val selectedDevice by repository.selectedDevice.collectAsState()

    var selectedTab by remember { mutableStateOf(AutomationViewTab.WORKFLOWS) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Modal Sheet states for recurring automation
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newTool by remember { mutableStateOf("lock_pc") }
    var newSchedule by remember { mutableStateOf("Everyday at 23:00") }
    var newDeviceId by remember { mutableStateOf(selectedDevice?.id ?: "") }

    // Modal Sheet states for delayed scheduled task
    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleTaskType by remember { mutableStateOf("youtube") } // youtube, gmail, app, url, lock
    var scheduleQueryOrTarget by remember { mutableStateOf("pewdiepie") }
    var scheduleDelayMinutes by remember { mutableStateOf(20) }
    var customDelayMinutesText by remember { mutableStateOf("20") }
    var scheduleTargetDeviceId by remember { mutableStateOf(selectedDevice?.id ?: "") }

    // Discard Confirmation state
    var automationToDiscard by remember { mutableStateOf<AutomationItem?>(null) }
    var actionNotification by remember { mutableStateOf<String?>(null) }

    // Visual Workflow Builder & Dry Run states
    var dryRunTargetAutomation by remember { mutableStateOf<AutomationItem?>(null) }
    var dryRunResponse by remember { mutableStateOf<WorkflowDryRunResponse?>(null) }
    var isDryRunning by remember { mutableStateOf(false) }
    var showWorkflowBuilderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dryRunTargetAutomation) {
        val target = dryRunTargetAutomation
        if (target != null) {
            isDryRunning = true
            dryRunResponse = repository.dryRunSavedAutomation(target.id)
            isDryRunning = false
        } else {
            dryRunResponse = null
        }
    }

    val quickTemplates = remember {
        listOf(
            AutomationTemplate(
                title = "Nightly Workstation Lock",
                tool = "lock_pc",
                schedule = "Everyday at 23:00",
                description = "Secures workstation screen at night",
                iconName = "lock"
            ),
            AutomationTemplate(
                title = "Hourly Health Telemetry",
                tool = "system_telemetry",
                schedule = "Every hour",
                description = "Captures CPU, RAM and disk metrics hourly",
                iconName = "telemetry"
            ),
            AutomationTemplate(
                title = "Periodic Screen Snapshot",
                tool = "take_screenshot",
                schedule = "Every 2 hours",
                description = "Takes diagnostic desktop screenshots periodically",
                iconName = "snapshot"
            )
        )
    }

    LaunchedEffect(Unit) {
        repository.refreshAutomations()
        repository.refreshAutomationHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Automations & Schedules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (automations.isEmpty()) "No custom automations" else "${automations.size} custom automation(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark
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
                            isRefreshing = true
                            repository.refreshAutomations()
                            repository.refreshAutomationHistory()
                            isRefreshing = false
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = RoyalViolet
                        )
                    }

                    Button(
                        onClick = {
                            scheduleTaskType = "youtube"
                            scheduleQueryOrTarget = "pewdiepie"
                            scheduleDelayMinutes = 20
                            customDelayMinutesText = "20"
                            scheduleTargetDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                            showScheduleDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Alarm, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Schedule", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = { showWorkflowBuilderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Workflow", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            newName = ""
                            newDescription = ""
                            newTool = "lock_pc"
                            newSchedule = "Everyday at 23:00"
                            newDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                            showCreateDialog = true
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Routine", tint = RoyalViolet)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tab Selector Pill Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = GlassSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AutomationViewTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedTab = tab },
                            color = if (isSelected) RoyalViolet.copy(alpha = 0.25f) else Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) RoyalViolet else TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            // Action Notification banner
            actionNotification?.let { msg ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = RoyalViolet.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(16.dp))
                        Text(text = msg, color = RoyalViolet, fontSize = 12.sp)
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                AutomationViewTab.WORKFLOWS -> {
                    if (automations.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Quick Delayed Task Hero Banner
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                onClick = {
                                    scheduleTaskType = "youtube"
                                    scheduleQueryOrTarget = "pewdiepie"
                                    scheduleDelayMinutes = 20
                                    customDelayMinutesText = "20"
                                    scheduleTargetDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                                    showScheduleDialog = true
                                }
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
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(RoyalViolet.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Alarm,
                                                contentDescription = null,
                                                tint = RoyalViolet,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Schedule Delayed Task",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                text = "Open YouTube, Chrome, Gmail, or Apps in 20 mins",
                                                color = TextMutedDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = RoyalViolet
                                    ) {
                                        Text(
                                            text = "+ Schedule",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "QUICK DELAYED TASKS (IN 20 MINS)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedDark
                            )

                            // Quick preset 1: YouTube Search
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                                onClick = {
                                    scheduleTaskType = "youtube"
                                    scheduleQueryOrTarget = "pewdiepie"
                                    scheduleDelayMinutes = 20
                                    customDelayMinutesText = "20"
                                    scheduleTargetDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                                    showScheduleDialog = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("📺", fontSize = 20.sp)
                                        Column {
                                            Text("YouTube: Search 'pewdiepie'", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                                            Text("In 20 mins • Open browser & play", color = TextMutedDark, fontSize = 11.sp)
                                        }
                                    }
                                    Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(20.dp))
                                }
                            }

                            // Quick preset 2: Open Gmail in Chrome
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                                onClick = {
                                    scheduleTaskType = "gmail"
                                    scheduleQueryOrTarget = "https://mail.google.com"
                                    scheduleDelayMinutes = 20
                                    customDelayMinutesText = "20"
                                    scheduleTargetDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                                    showScheduleDialog = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("✉️", fontSize = 20.sp)
                                        Column {
                                            Text("Open Google Chrome & Gmail", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                                            Text("In 20 mins • Ready when you reach home", color = TextMutedDark, fontSize = 11.sp)
                                        }
                                    }
                                    Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(20.dp))
                                }
                            }

                            // Quick preset 3: Lock PC
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                                onClick = {
                                    scheduleTaskType = "lock"
                                    scheduleQueryOrTarget = ""
                                    scheduleDelayMinutes = 20
                                    customDelayMinutesText = "20"
                                    scheduleTargetDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                                    showScheduleDialog = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("🔒", fontSize = 20.sp)
                                        Column {
                                            Text("Lock Workstation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                                            Text("In 20 mins • Secure desktop session", color = TextMutedDark, fontSize = 11.sp)
                                        }
                                    }
                                    Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(20.dp))
                                }
                            }

                            Text(
                                text = "RECURRING ROUTINES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedDark
                            )

                            quickTemplates.forEach { tpl ->
                                LiquidGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(14.dp),
                                    onClick = {
                                        newName = tpl.title
                                        newDescription = tpl.description
                                        newTool = tpl.tool
                                        newSchedule = tpl.schedule
                                        newDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                                        showCreateDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(RoyalViolet.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (tpl.tool) {
                                                        "lock_pc" -> Icons.Rounded.Lock
                                                        "take_screenshot" -> Icons.Rounded.CameraAlt
                                                        else -> Icons.Rounded.Analytics
                                                    },
                                                    contentDescription = null,
                                                    tint = RoyalViolet,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = tpl.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimaryDark
                                                )
                                                Text(
                                                    text = "${tpl.schedule} • ${tpl.tool}",
                                                    color = TextMutedDark,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Rounded.AddCircleOutline,
                                            contentDescription = "Use Template",
                                            tint = RoyalViolet,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            // Top banner for quick schedule
                            item {
                                LiquidGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(12.dp),
                                    onClick = {
                                        scheduleTaskType = "youtube"
                                        scheduleQueryOrTarget = "pewdiepie"
                                        scheduleDelayMinutes = 20
                                        customDelayMinutesText = "20"
                                        scheduleTargetDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                                        showScheduleDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(RoyalViolet.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Rounded.Alarm, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(18.dp))
                                            }
                                            Column {
                                                Text("Schedule a Delayed Task", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                                Text("Open YouTube, Gmail, Chrome in 20 mins", color = TextMutedDark, fontSize = 11.sp)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = RoyalViolet.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "+ Schedule",
                                                color = RoyalViolet,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            items(automations, key = { it.id }) { item ->
                                AutomationCard(
                                    automation = item,
                                    onToggle = { isActive ->
                                        coroutineScope.launch {
                                            repository.toggleAutomation(item.id, isActive)
                                            actionNotification = if (isActive) "Automation resumed" else "Automation paused"
                                        }
                                    },
                                    onRunNow = {
                                        coroutineScope.launch {
                                            val res = repository.runAutomationNow(item.id)
                                            actionNotification = res.second
                                        }
                                    },
                                    onEdit = {
                                        newName = item.name
                                        newDescription = item.description
                                        newTool = item.actionTool
                                        newSchedule = item.scheduleTime
                                        newDeviceId = item.targetDeviceId
                                        showCreateDialog = true
                                    },
                                    onDiscard = {
                                        automationToDiscard = item
                                    },
                                    onDryRun = {
                                        dryRunTargetAutomation = item
                                    }
                                )
                            }
                        }
                    }
                }

                AutomationViewTab.HISTORY -> {
                    if (history.isEmpty()) {
                        EmptyState(
                            title = "No Execution History",
                            description = "No automation execution logs recorded yet. Run a workflow to view its telemetry record.",
                            icon = Icons.Rounded.History
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(history, key = { it.id }) { h ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = h.automationName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                text = "${h.timestamp.take(19).replace("T", " ")} • ${h.details}",
                                                color = TextMutedDark,
                                                fontSize = 11.sp
                                            )
                                        }

                                        val statusColor = if (h.status == "SUCCESS") EmeraldOnline else RubyOffline
                                        StatusIndicator(text = h.status, accentColor = statusColor, isPulsing = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Discard Confirmation Dialog
    automationToDiscard?.let { auto ->
        ConfirmationSheet(
            title = "Delete Automation?",
            description = "Deleting '${auto.name}' will permanently remove its schedule from your system. This cannot be undone.",
            confirmButtonText = "Delete Schedule",
            isDestructive = true,
            onConfirm = {
                val targetId = auto.id
                automationToDiscard = null
                coroutineScope.launch {
                    val success = repository.discardAutomation(targetId)
                    actionNotification = if (success) "Automation removed" else "Failed to remove automation"
                }
            },
            onDismiss = {
                automationToDiscard = null
            }
        )
    }

    // New Automation Creation Modal
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = RoyalViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Scheduled Automation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryDark)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Automation Title", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("e.g. Nightly Auto-Lock", color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Description (Optional)", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        placeholder = { Text("e.g. Lock PC screen everyday at 23:00", color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Action Tool", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                    val tools = listOf(
                        Pair("lock_pc", "Lock PC"),
                        Pair("take_screenshot", "Screenshot"),
                        Pair("system_telemetry", "Telemetry"),
                        Pair("run_command", "Command")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tools.take(2).forEach { (toolKey, label) ->
                            FilterChip(
                                selected = newTool == toolKey,
                                onClick = { newTool = toolKey },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tools.drop(2).forEach { (toolKey, label) ->
                            FilterChip(
                                selected = newTool == toolKey,
                                onClick = { newTool = toolKey },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Schedule Rule", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                    val schedules = listOf("Everyday at 23:00", "Every hour", "Every 30 minutes", "Everyday at 09:00")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        schedules.take(2).forEach { s ->
                            FilterChip(
                                selected = newSchedule == s,
                                onClick = { newSchedule = s },
                                label = { Text(s, fontSize = 10.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        schedules.drop(2).forEach { s ->
                            FilterChip(
                                selected = newSchedule == s,
                                onClick = { newSchedule = s },
                                label = { Text(s, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newSchedule,
                        onValueChange = { newSchedule = it },
                        placeholder = { Text("Custom schedule string", color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (devices.isNotEmpty()) {
                        Text("Target Computer", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            devices.forEach { dev ->
                                val isSelected = newDeviceId == dev.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { newDeviceId = dev.id },
                                    color = if (isSelected) RoyalViolet.copy(alpha = 0.2f) else GlassSurfaceElevated
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Computer,
                                            contentDescription = null,
                                            tint = if (isSelected) RoyalViolet else TextSecondaryDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = dev.name,
                                            fontSize = 12.sp,
                                            color = if (isSelected) RoyalViolet else TextPrimaryDark,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            showCreateDialog = false
                            val targetId = if (newDeviceId.isNotBlank()) newDeviceId else selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                            coroutineScope.launch {
                                val success = repository.createAutomation(
                                    name = newName,
                                    tool = newTool,
                                    scheduleTime = newSchedule,
                                    description = newDescription,
                                    deviceId = targetId
                                )
                                actionNotification = if (success) "Created automation '$newName'" else "Failed to create automation"
                                newName = ""
                                newDescription = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet)
                ) {
                    Text("Create Automation", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = Color(0xFF111827),
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Scheduled Delayed Task Creation Modal
    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Alarm, contentDescription = null, tint = RoyalViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Delayed Task", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryDark)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Action Type", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                    val actionTypes = listOf(
                        Triple("youtube", "📺 YouTube Search", "pewdiepie"),
                        Triple("gmail", "✉️ Open Gmail (Chrome)", "https://mail.google.com"),
                        Triple("app", "💻 Launch App", "Google Chrome"),
                        Triple("url", "🌐 Open Website", "https://"),
                        Triple("lock", "🔒 Lock Workstation", "")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        actionTypes.forEach { (typeKey, label, defaultVal) ->
                            val isSelected = scheduleTaskType == typeKey
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        scheduleTaskType = typeKey
                                        if (defaultVal.isNotBlank()) {
                                            scheduleQueryOrTarget = defaultVal
                                        }
                                    },
                                color = if (isSelected) RoyalViolet.copy(alpha = 0.2f) else GlassSurfaceElevated,
                                border = if (isSelected) BorderStroke(1.dp, RoyalViolet) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        color = if (isSelected) RoyalViolet else TextPrimaryDark,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    if (scheduleTaskType != "lock" && scheduleTaskType != "gmail") {
                        val inputLabel = when (scheduleTaskType) {
                            "youtube" -> "YouTube Search Query"
                            "app" -> "Application Name"
                            "url" -> "Website URL"
                            else -> "Query or Target"
                        }
                        val placeholder = when (scheduleTaskType) {
                            "youtube" -> "e.g. pewdiepie or lofi beats"
                            "app" -> "e.g. Google Chrome, Visual Studio Code"
                            "url" -> "e.g. https://mail.google.com"
                            else -> ""
                        }
                        Text(inputLabel, style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                        OutlinedTextField(
                            value = scheduleQueryOrTarget,
                            onValueChange = { scheduleQueryOrTarget = it },
                            placeholder = { Text(placeholder, color = TextMutedDark, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Text("Execute In (Delay)", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                    val delayOptions = listOf(5, 15, 20, 30, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        delayOptions.take(3).forEach { mins ->
                            FilterChip(
                                selected = scheduleDelayMinutes == mins,
                                onClick = {
                                    scheduleDelayMinutes = mins
                                    customDelayMinutesText = mins.toString()
                                },
                                label = { Text("${mins}m", fontSize = 11.sp) }
                            )
                        }
                        delayOptions.drop(3).forEach { mins ->
                            val label = if (mins == 60) "1h" else "${mins}m"
                            FilterChip(
                                selected = scheduleDelayMinutes == mins,
                                onClick = {
                                    scheduleDelayMinutes = mins
                                    customDelayMinutesText = mins.toString()
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Custom minutes:", style = MaterialTheme.typography.bodySmall, color = TextMutedDark)
                        OutlinedTextField(
                            value = customDelayMinutesText,
                            onValueChange = {
                                customDelayMinutesText = it
                                it.toIntOrNull()?.let { num ->
                                    if (num > 0) scheduleDelayMinutes = num
                                }
                            },
                            modifier = Modifier.width(90.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    if (devices.isNotEmpty()) {
                        Text("Target Computer", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            devices.forEach { dev ->
                                val isSelected = scheduleTargetDeviceId == dev.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { scheduleTargetDeviceId = dev.id },
                                    color = if (isSelected) RoyalViolet.copy(alpha = 0.2f) else GlassSurfaceElevated
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Computer,
                                            contentDescription = null,
                                            tint = if (isSelected) RoyalViolet else TextSecondaryDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = dev.name,
                                            fontSize = 12.sp,
                                            color = if (isSelected) RoyalViolet else TextPrimaryDark,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (tool, args, title) = when (scheduleTaskType) {
                            "youtube" -> {
                                val q = scheduleQueryOrTarget.ifBlank { "pewdiepie" }
                                Triple("browser_search", mapOf("query" to q, "site" to "youtube"), "YouTube: $q")
                            }
                            "gmail" -> Triple("open_browser", mapOf("url" to "https://mail.google.com", "browser" to "chrome"), "Open Gmail")
                            "app" -> {
                                val app = scheduleQueryOrTarget.ifBlank { "Google Chrome" }
                                Triple("open_application", mapOf("app_name" to app), "Launch $app")
                            }
                            "url" -> {
                                var u = scheduleQueryOrTarget.ifBlank { "https://google.com" }
                                if (!u.startsWith("http")) u = "https://$u"
                                Triple("open_browser", mapOf("url" to u), "Open $u")
                            }
                            "lock" -> Triple("lock_pc", emptyMap<String, Any>(), "Lock Workstation")
                            else -> Triple("lock_pc", emptyMap<String, Any>(), "Scheduled Task")
                        }
                        showScheduleDialog = false
                        val targetDev = if (scheduleTargetDeviceId.isNotBlank()) scheduleTargetDeviceId else selectedDevice?.id ?: devices.firstOrNull()?.id ?: ""
                        coroutineScope.launch {
                            val ok = repository.scheduleTask(
                                name = title,
                                tool = tool,
                                actionArguments = args,
                                delayMinutes = scheduleDelayMinutes,
                                description = "Execute $title in $scheduleDelayMinutes mins",
                                deviceId = targetDev
                            )
                            actionNotification = if (ok) "Scheduled '$title' in $scheduleDelayMinutes mins!" else "Failed to schedule task"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet)
                ) {
                    Text("Schedule (in ${scheduleDelayMinutes}m)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = Color(0xFF111827),
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Dry Run Simulation Dialog
    dryRunTargetAutomation?.let { auto ->
        WorkflowDryRunDialog(
            automation = auto,
            response = dryRunResponse,
            isLoading = isDryRunning,
            onDismiss = { dryRunTargetAutomation = null }
        )
    }

    // Visual Workflow Builder Dialog
    if (showWorkflowBuilderDialog) {
        VisualWorkflowBuilderDialog(
            devices = devices,
            selectedDeviceId = selectedDevice?.id ?: devices.firstOrNull()?.id ?: "",
            repository = repository,
            onDismiss = { showWorkflowBuilderDialog = false },
            onCreated = { msg ->
                showWorkflowBuilderDialog = false
                actionNotification = msg
            }
        )
    }
}

@Composable
fun WorkflowDryRunDialog(
    automation: AutomationItem,
    response: WorkflowDryRunResponse?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Visibility, contentDescription = null, tint = NeonIndigo)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dry-Run Simulation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryDark)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Simulating '${automation.name}' execution plan against security policies without running commands on Mac:",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonIndigo, modifier = Modifier.size(32.dp))
                    }
                } else if (response != null) {
                    val riskColor = when (response.riskAssessment) {
                        "HIGH_RISK" -> RubyOffline
                        "MEDIUM_RISK" -> AmberWarning
                        else -> EmeraldOnline
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = riskColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Policy Risk: ${response.riskAssessment.replace("_", " ")}",
                                color = riskColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "${response.stepsCount} step(s)",
                            color = TextMutedDark,
                            fontSize = 11.sp
                        )
                    }

                    if (response.requiresHumanApproval) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberWarning.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Security, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Requires explicit human confirmation for sensitive operations.", color = AmberWarning, fontSize = 11.sp)
                            }
                        }
                    }

                    Text("SIMULATED PIPELINE GRAPH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedDark)
                    response.steps.forEachIndexed { index, step ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, GlassBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${step.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    val stepRiskCol = if (step.riskLevel == "HIGH") RubyOffline else (if (step.riskLevel == "MEDIUM") AmberWarning else EmeraldOnline)
                                    Text(step.riskLevel, color = stepRiskCol, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(step.simulationNote, color = TextMutedDark, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Text("Simulation returned no output or an error occurred.", color = RubyOffline, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = ObsidianBackground,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun VisualWorkflowBuilderDialog(
    devices: List<Device>,
    selectedDeviceId: String,
    repository: RemoteForgeRepository,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("Mac Health & Thermal Guard") }
    var description by remember { mutableStateOf("Automated watchdog that monitors workstation health") }
    var triggerType by remember { mutableStateOf("interval") }
    var scheduleTime by remember { mutableStateOf("Every 30 minutes") }
    var targetDeviceId by remember { mutableStateOf(selectedDeviceId) }
    var isSaving by remember { mutableStateOf(false) }

    // Multi-step list
    var steps by remember {
        mutableStateOf(
            listOf(
                WorkflowStepItem(
                    stepId = "s1",
                    name = "Harvest Telemetry",
                    type = "action",
                    tool = "get_system_status"
                ),
                WorkflowStepItem(
                    stepId = "s2",
                    name = "Verify CPU Threshold",
                    type = "condition",
                    condition = mapOf("field" to "result.cpu_percent", "operator" to ">", "value" to 85.0),
                    onTrueStep = "s3",
                    onFalseStep = "s4"
                ),
                WorkflowStepItem(
                    stepId = "s3",
                    name = "High CPU Notification",
                    type = "notification",
                    notificationMessage = "Mac CPU utilization exceeded 85% threshold."
                ),
                WorkflowStepItem(
                    stepId = "s4",
                    name = "Complete Pipeline",
                    type = "completion"
                )
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = NeonIndigo)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Visual Workflow Builder", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryDark)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Workflow Presets Selector
                Text("VERIFIED PRESETS (1-TAP LOAD)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Health Watchdog" to {
                            name = "Workstation Health Watchdog"
                            description = "Checks CPU & RAM and notifies if limits exceeded"
                            triggerType = "interval"
                            scheduleTime = "Every 30 minutes"
                            steps = listOf(
                                WorkflowStepItem("s1", "Harvest Telemetry", "action", "get_system_status"),
                                WorkflowStepItem("s2", "Check CPU Threshold", "condition", condition = mapOf("field" to "result.cpu_percent", "operator" to ">", "value" to 85.0), onTrueStep = "s3", onFalseStep = "s4"),
                                WorkflowStepItem("s3", "High CPU Alert", "notification", notificationMessage = "CPU load exceeded 85%"),
                                WorkflowStepItem("s4", "Complete", "completion")
                            )
                        },
                        "Build Guard" to {
                            name = "Project Build Guard"
                            description = "Compiles project and alerts on non-zero exit code"
                            triggerType = "manual"
                            scheduleTime = "On Demand"
                            steps = listOf(
                                WorkflowStepItem("b1", "Execute Build", "action", "build_project", arguments = mapOf("task_type" to "build", "max_lines" to 100)),
                                WorkflowStepItem("b2", "Check Exit Code", "condition", condition = mapOf("field" to "result.exit_code", "operator" to "!=", "value" to 0), onTrueStep = "b3", onFalseStep = "b4"),
                                WorkflowStepItem("b3", "Build Failure Alert", "notification", notificationMessage = "Project compilation failed with errors"),
                                WorkflowStepItem("b4", "Complete", "completion")
                            )
                        },
                        "Git Watcher" to {
                            name = "Git Working Tree Watcher"
                            description = "Notifies if working tree has pending uncommitted changes"
                            triggerType = "interval"
                            scheduleTime = "Every hour"
                            steps = listOf(
                                WorkflowStepItem("g1", "Query Git Status", "action", "git_status"),
                                WorkflowStepItem("g2", "Check If Dirty", "condition", condition = mapOf("field" to "result.is_dirty", "operator" to "==", "value" to true), onTrueStep = "g3", onFalseStep = "g4"),
                                WorkflowStepItem("g3", "Pending Changes Alert", "notification", notificationMessage = "Uncommitted modifications detected in repo"),
                                WorkflowStepItem("g4", "Complete", "completion")
                            )
                        }
                    ).forEach { (label, onClick) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, GlassBorderSubtle),
                            modifier = Modifier.clickable { onClick() }
                        ) {
                            Text(label, color = RoyalViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                        }
                    }
                }

                // Name & Description
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workflow Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Trigger Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("manual" to "Manual", "interval" to "Interval", "schedule" to "Daily").forEach { (type, lbl) ->
                        val isSel = triggerType == type
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) NeonIndigo.copy(alpha = 0.25f) else ObsidianSurface,
                            border = BorderStroke(1.dp, if (isSel) NeonIndigo else GlassBorderSubtle),
                            modifier = Modifier.weight(1f).clickable { triggerType = type }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(lbl, color = if (isSel) Color.White else TextMutedDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Steps List
                Text("PIPELINE STEPS (${steps.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedDark)
                steps.forEachIndexed { index, step ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, GlassBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val typeIcon = when (step.type) {
                                    "action" -> Icons.Rounded.Build
                                    "condition" -> Icons.Rounded.AltRoute
                                    "delay" -> Icons.Rounded.HourglassBottom
                                    "notification" -> Icons.Rounded.Notifications
                                    else -> Icons.Rounded.CheckCircle
                                }
                                Icon(typeIcon, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("${index + 1}. ${step.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                    Text(
                                        text = if (step.tool != null) "Tool: ${step.tool}" else "Type: ${step.type.uppercase()}",
                                        color = TextMutedDark,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (steps.size > 2) {
                                IconButton(
                                    onClick = { steps = steps.filterIndexed { i, _ -> i != index } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Delete", tint = RubyOffline, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        isSaving = true
                        coroutineScope.launch {
                            val ok = repository.createCustomWorkflow(
                                name = name,
                                description = description,
                                triggerType = triggerType,
                                scheduleTime = scheduleTime,
                                deviceId = targetDeviceId,
                                steps = steps
                            )
                            isSaving = false
                            onCreated(if (ok) "Workflow '$name' deployed!" else "Failed to deploy workflow")
                        }
                    }
                },
                enabled = !isSaving && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isSaving) "Deploying..." else "Deploy Workflow", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        },
        containerColor = ObsidianBackground,
        shape = RoundedCornerShape(20.dp)
    )
}

