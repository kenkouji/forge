package com.remoteforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.AuditLogEntry
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

enum class LogCategory(val label: String, val apiValue: String) {
    ALL("All Logs", "all"),
    BUGS("Bugs & Issues", "bugs"),
    SUCCESS("Success Only", "success")
}

enum class LogTimeWindow(val label: String, val days: Int, val apiValue: String) {
    TODAY("Today (24h)", 1, "1d"),
    WEEK("7 Days (1W)", 7, "7d"),
    TWO_WEEKS("14 Days (2W Max)", 14, "14d")
}

@Composable
fun ActivityScreen(
    repository: RemoteForgeRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val auditLogs by repository.auditLogs.collectAsState()
    val auditStats by repository.auditStats.collectAsState()

    var selectedCategory by remember { mutableStateOf(LogCategory.ALL) }
    var selectedTimeWindow by remember { mutableStateOf(LogTimeWindow.TWO_WEEKS) }
    var selectedDayFilter by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showCleanDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refreshAll() {
        coroutineScope.launch {
            isRefreshing = true
            repository.refreshAuditLogs(
                category = selectedCategory.apiValue,
                timeframe = selectedTimeWindow.apiValue,
                days = selectedTimeWindow.days
            )
            repository.refreshAuditStats(days = selectedTimeWindow.days)
            isRefreshing = false
        }
    }

    LaunchedEffect(selectedCategory, selectedTimeWindow) {
        refreshAll()
    }

    // Filter by day if selected
    val displayLogs = remember(auditLogs, selectedDayFilter) {
        if (selectedDayFilter == null) {
            auditLogs
        } else {
            auditLogs.filter { it.timestamp.startsWith(selectedDayFilter!!) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Activity & Bug Tracker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "14-day auto-purged audit trail • zero clutter",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedDark
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Clean Logs Action
                IconButton(
                    onClick = { showCleanDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Clean Logs",
                        tint = RubyOffline
                    )
                }

                // Refresh Action
                IconButton(
                    onClick = { refreshAll() }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = ElectricCyan
                    )
                }
            }
        }

        // Notification / Status Banner
        val currentStatusMsg = statusMessage
        if (currentStatusMsg != null) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = EmeraldOnline.copy(alpha = 0.6f),
                contentPadding = PaddingValues(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = currentStatusMsg, fontSize = 12.sp, color = EmeraldOnline)
                    Text(
                        text = "✕",
                        fontSize = 12.sp,
                        color = TextMutedDark,
                        modifier = Modifier.clickable { statusMessage = null }
                    )
                }
            }
        }

        // Bugs & Stats Summary HUD
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Overview (${selectedTimeWindow.label})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Retention Cap: 14 Days",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }

                // Quick metric chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = GlassSurfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Total", fontSize = 10.sp, color = TextMutedDark)
                            Text(
                                text = "${auditStats?.totalLogs ?: auditLogs.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }

                    // Bugs
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = RubyOffline.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Bugs/Errors", fontSize = 10.sp, color = RubyOffline)
                            Text(
                                text = "${auditStats?.totalBugs ?: 0}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RubyOffline
                            )
                        }
                    }

                    // Success
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldOnline.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Success", fontSize = 10.sp, color = EmeraldOnline)
                            Text(
                                text = "${auditStats?.totalSuccess ?: 0}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldOnline
                            )
                        }
                    }
                }

                // Bugs Per Day Interactive Strip
                auditStats?.daysBreakdown?.let { days ->
                    if (days.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Bugs Per Day (Tap to filter):",
                                fontSize = 11.sp,
                                color = TextMutedDark
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                days.take(selectedTimeWindow.days).forEach { day ->
                                    val isSelectedDay = selectedDayFilter == day.date
                                    val hasBugs = day.bugs > 0
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isSelectedDay -> ElectricCyan.copy(alpha = 0.25f)
                                            hasBugs -> RubyOffline.copy(alpha = 0.15f)
                                            else -> GlassSurfaceElevated
                                        },
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = when {
                                                    isSelectedDay -> ElectricCyan
                                                    hasBugs -> RubyOffline.copy(alpha = 0.5f)
                                                    else -> Color.Transparent
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                selectedDayFilter = if (isSelectedDay) null else day.date
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = day.date.takeLast(5),
                                                fontSize = 10.sp,
                                                color = TextSecondaryDark,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (day.bugs > 0) "${day.bugs} bugs" else "0 bugs",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (day.bugs > 0) RubyOffline else EmeraldOnline
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

        // Timeframe Selector & Category Chips
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Time window pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LogTimeWindow.values().forEach { tw ->
                    val isSelected = selectedTimeWindow == tw
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) ElectricCyan.copy(alpha = 0.2f) else GlassSurfaceElevated,
                        modifier = Modifier.clickable {
                            selectedTimeWindow = tw
                            selectedDayFilter = null
                        }
                    ) {
                        Text(
                            text = tw.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ElectricCyan else TextSecondaryDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Category Chips (All vs Bugs vs Success)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LogCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val accent = when (cat) {
                        LogCategory.ALL -> ElectricCyan
                        LogCategory.BUGS -> RubyOffline
                        LogCategory.SUCCESS -> EmeraldOnline
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) accent.copy(alpha = 0.2f) else GlassSurfaceElevated,
                        modifier = Modifier.clickable {
                            selectedCategory = cat
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (cat == LogCategory.BUGS) {
                                Icon(
                                    imageVector = Icons.Rounded.BugReport,
                                    contentDescription = null,
                                    tint = if (isSelected) RubyOffline else TextMutedDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = cat.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accent else TextSecondaryDark
                            )
                        }
                    }
                }
            }
        }

        // Active filter indicator if single date filtered
        selectedDayFilter?.let { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing day: $day",
                    fontSize = 12.sp,
                    color = ElectricCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Clear day filter",
                    fontSize = 11.sp,
                    color = AmberWarning,
                    modifier = Modifier.clickable { selectedDayFilter = null }
                )
            }
        }

        // Log Items List
        if (displayLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldOnline,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedCategory == LogCategory.BUGS) "No bugs or errors recorded in this timeframe!" else "No audit records found.",
                        color = TextMutedDark,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(displayLogs, key = { it.id }) { log ->
                    val isBug = log.executionStatus != "SUCCESS" || "REJECTED" in log.action || "ERROR" in log.action

                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (isBug) RubyOffline.copy(alpha = 0.5f) else GlassBorderSubtle,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isBug) {
                                        Icon(
                                            imageVector = Icons.Rounded.Warning,
                                            contentDescription = "Issue",
                                            tint = RubyOffline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = log.tool ?: log.action,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                val statusColor = when (log.executionStatus) {
                                    "SUCCESS" -> EmeraldOnline
                                    "REJECTED" -> RubyOffline
                                    "FAILED", "ERROR" -> RubyOffline
                                    else -> AmberWarning
                                }

                                LiquidGlassBadge(
                                    text = log.executionStatus,
                                    accentColor = statusColor
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Actor: ${log.actor} • Perm: ${log.permissionLevel}",
                                    color = TextMutedDark,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = log.timestamp.take(19).replace("T", " "),
                                    color = TextMutedDark,
                                    fontSize = 11.sp
                                )
                            }

                            log.details?.let { details ->
                                if (details.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isBug) RubyOffline.copy(alpha = 0.08f) else GlassSurfaceElevated,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = details,
                                            color = if (isBug) RubyOffline else TextSecondaryDark,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(8.dp)
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

    // Clean Logs Modal Dialog
    if (showCleanDialog) {
        AlertDialog(
            onDismissRequest = { showCleanDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = RubyOffline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Clean Logs Trail", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Prevent clutter and free database storage. RemoteForge strictly caps logs at 14 days maximum. You can also purge old logs immediately.",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            showCleanDialog = false
                            val result = repository.cleanAuditLogs(days = 0)
                            result.onSuccess {
                                statusMessage = "Cleaned all audit records successfully!"
                                refreshAll()
                            }.onFailure {
                                statusMessage = "Failed to clean logs: ${it.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyOffline)
                ) {
                    Text("Clean All Logs", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            showCleanDialog = false
                            val result = repository.cleanAuditLogs(days = 2)
                            result.onSuccess {
                                statusMessage = "Purged records older than 2 days."
                                refreshAll()
                            }.onFailure {
                                statusMessage = "Failed to purge: ${it.message}"
                            }
                        }
                    }
                ) {
                    Text("Purge > 2 Days", color = ElectricCyan)
                }
            },
            containerColor = Color(0xFF0F172A),
            tonalElevation = 6.dp
        )
    }
}
