package com.remoteforge.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.ChatMessage
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiAssistantScreen(
    repository: RemoteForgeRepository,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val chatMessages by repository.chatMessages.collectAsState()
    val isAILoading by repository.isAILoading.collectAsState()
    val selectedDevice by repository.selectedDevice.collectAsState()
    val telemetry by repository.telemetry.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to newest message
    LaunchedEffect(chatMessages.size, isAILoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val suggestionChips = listOf(
        "Show me CPU and memory usage on PC",
        "Check RAM usage",
        "List files in the Projects folder",
        "Find my DBMS assignment",
        "Take a screenshot of PC",
        "Lock workstation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        // ==========================================
        // 1. Header (RemoteForge, AI Command, Ask, Automate, Achieve)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "RemoteForge",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextRestrainedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "AI Command",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    fontSize = 28.sp
                )
                Text(
                    text = "Ask, Automate, Achieve.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextRestrainedGray,
                    fontSize = 12.sp
                )
            }

            // Target computer pill
            selectedDevice?.let { dev ->
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurfaceElevated)
                        .border(0.5.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (dev.isOnline) AccentEmerald else AccentRuby)
                    )
                    Text(
                        text = dev.name.take(16),
                        color = TextSoftWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ==========================================
        // 2. Suggestion Chips Bar
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestionChips.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(0.5.dp, ObsidianBorderSubtle),
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            repository.sendAIMessage(suggestion)
                        }
                    }
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 11.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ==========================================
        // 3. Vertically Scrolling Conversation Stream
        // ==========================================
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    ForgeGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AiGradientBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(
                                    text = "Ready to control your workstation",
                                    color = TextPureWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Ask to inspect telemetry, search files, capture screenshots, or run tasks.",
                                    color = TextRestrainedGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            items(chatMessages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    // User Message Bubble (Violet Gradient, Aligned Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = 18.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(AiGradientBrush)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Just now",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                } else {
                    // Assistant Response Card (Obsidian Glass, Aligned Left)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        ForgeGlassCard(
                            modifier = Modifier.fillMaxWidth(0.92f),
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 18.dp
                            ),
                            backgroundColor = ObsidianSurfaceElevated,
                            borderColor = ObsidianBorder,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = msg.text,
                                    color = TextSoftWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                // Check if this message was a telemetry check
                                if (msg.toolCalled == "get_system_status" || msg.text.contains("status for", ignoreCase = true) || msg.text.contains("RAM usage", ignoreCase = true)) {
                                    val cpuP = (telemetry?.cpu?.percent ?: 12.0).toInt()
                                    val ramP = (telemetry?.ram?.percent ?: 48.0).toInt()
                                    val diskP = (telemetry?.disk?.percent ?: 67.0).toInt()

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = ObsidianBackground.copy(alpha = 0.7f),
                                        border = BorderStroke(0.5.dp, ObsidianBorderSubtle)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentCyan))
                                                Text("$cpuP% CPU", color = TextPureWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentEmerald))
                                                Text("$ramP% RAM", color = TextPureWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentSky))
                                                Text("$diskP% Disk", color = TextPureWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }

                                // Render matched files with interactive cards & format disambiguation
                                if (msg.matchedFiles.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Suggested Files (${msg.matchedFiles.size}):",
                                            color = AccentCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        msg.matchedFiles.forEach { file ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = ObsidianBackground.copy(alpha = 0.8f),
                                                border = BorderStroke(0.5.dp, ObsidianBorderSubtle)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        FileTypeBadge(filename = file.name, isDir = file.isDir)
                                                        Column {
                                                            Text(
                                                                text = file.name,
                                                                color = TextPureWhite,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = "${(file.sizeBytes / 1024).coerceAtLeast(1)} KB • ${file.path.takeLast(28)}",
                                                                color = TextRestrainedGray,
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        // Preview button
                                                        IconButton(
                                                            onClick = {
                                                                val devId = selectedDevice?.id ?: ""
                                                                onNavigate(AppScreen.FilePreview(devId, file.path, file.name))
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Rounded.Visibility, contentDescription = "Preview", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                                        }

                                                        // Download button
                                                        IconButton(
                                                            onClick = {
                                                                val devId = selectedDevice?.id ?: ""
                                                                coroutineScope.launch {
                                                                    repository.downloadFile(devId, file.path, file.name)
                                                                    onNavigate(AppScreen.Transfers)
                                                                }
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Rounded.Download, contentDescription = "Download", tint = AccentEmerald, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Disambiguation quick choice chips if multiple formats exist
                                        val hasPdf = msg.matchedFiles.any { it.name.endsWith(".pdf", ignoreCase = true) }
                                        val hasWord = msg.matchedFiles.any { it.name.endsWith(".docx", ignoreCase = true) || it.name.endsWith(".doc", ignoreCase = true) }
                                        if (hasPdf && hasWord) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = AccentCyan.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, AccentCyan.copy(alpha = 0.4f)),
                                                    modifier = Modifier.clickable {
                                                        val pdfFile = msg.matchedFiles.firstOrNull { it.name.endsWith(".pdf", ignoreCase = true) }
                                                        if (pdfFile != null) {
                                                            onNavigate(AppScreen.FilePreview(selectedDevice?.id ?: "", pdfFile.path, pdfFile.name))
                                                        }
                                                    }
                                                ) {
                                                    Text("Open PDF Version", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = AccentViolet.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, AccentViolet.copy(alpha = 0.4f)),
                                                    modifier = Modifier.clickable {
                                                        val wordFile = msg.matchedFiles.firstOrNull { it.name.endsWith(".docx", ignoreCase = true) || it.name.endsWith(".doc", ignoreCase = true) }
                                                        if (wordFile != null) {
                                                            onNavigate(AppScreen.FilePreview(selectedDevice?.id ?: "", wordFile.path, wordFile.name))
                                                        }
                                                    }
                                                ) {
                                                    Text("Open Word Version", color = AccentViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                                }
                                            }
                                        }
                                    }
                                } else if (msg.toolCalled == "search_files" || msg.toolCalled == "list_files" || msg.text.contains("items in", ignoreCase = true) || msg.text.contains("file", ignoreCase = true)) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onNavigate(AppScreen.Files) },
                                        color = AccentCyan.copy(alpha = 0.12f),
                                        border = BorderStroke(0.5.dp, AccentCyan.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                                Text("View in File Explorer", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Confirmation Required Action
                                if (msg.requiresConfirmation) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = AccentAmber.copy(alpha = 0.1f),
                                        border = BorderStroke(0.5.dp, AccentAmber.copy(alpha = 0.3f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Confirmation required: run ${msg.pendingTool ?: "action"}?",
                                                    color = AccentAmber,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            repository.sendAIMessage(
                                                                text = "Approved: execute ${msg.pendingTool}",
                                                                confirmed = true,
                                                                pendingTool = msg.pendingTool,
                                                                pendingArgs = msg.pendingArguments
                                                            )
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Approve & Run", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            repository.sendAIMessage("Cancel operation", confirmed = false)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Cancel", color = TextRestrainedGray, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isAILoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        ForgeGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = ObsidianSurfaceElevated,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = AccentCyan,
                                    strokeWidth = 2.dp
                                )
                                Text("Autonomous assistant is reasoning...", color = TextRestrainedGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. Command Composer (Pinned Above Floating Dock)
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 76.dp),
            shape = RoundedCornerShape(26.dp),
            color = ObsidianSurfaceElevated,
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = {
                        Text(
                            text = "Ask anything...",
                            color = TextDarkMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPureWhite,
                        unfocusedTextColor = TextPureWhite
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val text = inputPrompt.trim()
                            if (text.isNotBlank() && !isAILoading) {
                                inputPrompt = ""
                                focusManager.clearFocus()
                                coroutineScope.launch {
                                    repository.sendAIMessage(text)
                                }
                            }
                        }
                    )
                )

                val canSend = inputPrompt.isNotBlank() && !isAILoading
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (canSend) AiGradientBrush else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))))
                        .clickable(enabled = canSend) {
                            val text = inputPrompt.trim()
                            if (text.isNotBlank()) {
                                inputPrompt = ""
                                focusManager.clearFocus()
                                coroutineScope.launch {
                                    repository.sendAIMessage(text)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isAILoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else TextDarkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
