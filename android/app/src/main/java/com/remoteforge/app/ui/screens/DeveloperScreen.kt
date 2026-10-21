package com.remoteforge.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import com.remoteforge.app.data.model.*
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.GlassCard
import com.remoteforge.app.ui.theme.*

import kotlinx.coroutines.launch

enum class DeveloperTab {
    GIT, BUILD, CLIPBOARD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    deviceId: String,
    repository: RemoteForgeRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(DeveloperTab.GIT) }
    var projectPath by remember { mutableStateOf("/Users/g.aasish/mainproject") }

    // Git state
    var gitStatus by remember { mutableStateOf<GitStatusInfo?>(null) }
    var gitDiff by remember { mutableStateOf<GitDiffInfo?>(null) }
    var isLoadingGit by remember { mutableStateOf(false) }
    var gitError by remember { mutableStateOf<String?>(null) }

    // Build state
    var selectedTaskType by remember { mutableStateOf("build") }
    var customCommand by remember { mutableStateOf("") }
    var buildResult by remember { mutableStateOf<ProjectBuildResult?>(null) }
    var isBuilding by remember { mutableStateOf(false) }
    var buildError by remember { mutableStateOf<String?>(null) }

    // Clipboard state
    var macClipboardText by remember { mutableStateOf("") }
    var androidSendText by remember { mutableStateOf("") }
    var isClipboardLoading by remember { mutableStateOf(false) }
    var clipboardStatus by remember { mutableStateOf<String?>(null) }

    // Initial git status load
    fun loadGit() {
        isLoadingGit = true
        gitError = null
        coroutineScope.launch {
            val statusRes = repository.getGitStatus(deviceId, projectPath)
            statusRes.onSuccess {
                gitStatus = it
                // Also fetch diff
                val diffRes = repository.getGitDiff(deviceId, projectPath)
                diffRes.onSuccess { d -> gitDiff = d }
                diffRes.onFailure { dErr -> gitError = dErr.message }
            }.onFailure {
                gitError = it.message
            }
            isLoadingGit = false
        }
    }

    LaunchedEffect(deviceId, projectPath) {
        loadGit()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Developer Workstation",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = deviceId,
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
                            when (selectedTab) {
                                DeveloperTab.GIT -> loadGit()
                                DeveloperTab.CLIPBOARD -> {
                                    isClipboardLoading = true
                                    coroutineScope.launch {
                                        repository.getMacClipboard(deviceId).onSuccess {
                                            macClipboardText = it.text
                                            clipboardStatus = "Clipboard fetched (${it.length} chars)"
                                        }.onFailure {
                                            clipboardStatus = "Error: ${it.message}"
                                        }
                                        isClipboardLoading = false
                                    }
                                }
                                else -> {}
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
            // Project Path Input Bar
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(
                    text = "TARGET PROJECT PATH",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = projectPath,
                    onValueChange = { projectPath = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalViolet,
                        unfocusedBorderColor = GlassBorderHighlight,
                        cursorColor = RoyalViolet
                    )
                )
            }

            // Segment Tabs Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    text = "Git & Diff",
                    selected = selectedTab == DeveloperTab.GIT,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = DeveloperTab.GIT }

                TabButton(
                    text = "Build & Run",
                    selected = selectedTab == DeveloperTab.BUILD,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = DeveloperTab.BUILD }

                TabButton(
                    text = "Clipboard",
                    selected = selectedTab == DeveloperTab.CLIPBOARD,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = DeveloperTab.CLIPBOARD }
            }

            // Tab Content
            when (selectedTab) {
                DeveloperTab.GIT -> GitTabContent(
                    gitStatus = gitStatus,
                    gitDiff = gitDiff,
                    isLoading = isLoadingGit,
                    errorMessage = gitError,
                    onRefresh = { loadGit() }
                )

                DeveloperTab.BUILD -> BuildTabContent(
                    selectedType = selectedTaskType,
                    onTypeChange = { selectedTaskType = it },
                    customCmd = customCommand,
                    onCustomCmdChange = { customCommand = it },
                    buildResult = buildResult,
                    isBuilding = isBuilding,
                    error = buildError,
                    onRunBuild = {
                        isBuilding = true
                        buildError = null
                        coroutineScope.launch {
                            val res = repository.buildProject(
                                deviceId = deviceId,
                                projectPath = projectPath,
                                taskType = selectedTaskType,
                                customCmd = if (selectedTaskType == "custom") customCommand else null
                            )
                            res.onSuccess {
                                buildResult = it
                            }.onFailure {
                                buildError = it.message
                            }
                            isBuilding = false
                        }
                    }
                )

                DeveloperTab.CLIPBOARD -> ClipboardTabContent(
                    macClipboardText = macClipboardText,
                    androidSendText = androidSendText,
                    onAndroidSendTextChange = { androidSendText = it },
                    isLoading = isClipboardLoading,
                    statusMessage = clipboardStatus,
                    onPullClipboard = {
                        isClipboardLoading = true
                        coroutineScope.launch {
                            repository.getMacClipboard(deviceId).onSuccess {
                                macClipboardText = it.text
                                clipboardStatus = "Fetched ${it.length} characters."
                            }.onFailure {
                                clipboardStatus = "Error: ${it.message}"
                            }
                            isClipboardLoading = false
                        }
                    },
                    onPushClipboard = {
                        if (androidSendText.isNotBlank()) {
                            isClipboardLoading = true
                            coroutineScope.launch {
                                repository.setMacClipboard(deviceId, androidSendText).onSuccess {
                                    clipboardStatus = "Mac clipboard updated!"
                                    Toast.makeText(context, "Mac clipboard updated", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    clipboardStatus = "Push failed: ${it.message}"
                                }
                                isClipboardLoading = false
                            }
                        }
                    },
                    onCopyToAndroid = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("RemoteForge Mac Clipboard", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to Android clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) RoyalViolet.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) RoyalViolet.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun GitTabContent(
    gitStatus: GitStatusInfo?,
    gitDiff: GitDiffInfo?,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    if (isLoading && gitStatus == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RoyalViolet)
        }
        return
    }

    if (errorMessage != null && gitStatus == null) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(text = "Git Error", color = RubyOffline, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = errorMessage, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Overview Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌿 ", fontSize = 16.sp)
                            Text(
                                text = gitStatus?.branch ?: "main",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (gitStatus?.tracking != null) {
                            Text(
                                text = "Tracking: ${gitStatus.tracking}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Clean / Dirty Badge
                    val isClean = gitStatus?.clean ?: true
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isClean) EmeraldOnline.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f))
                            .border(1.dp, if (isClean) EmeraldOnline else AmberWarning, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isClean) "Clean" else "Dirty (${(gitStatus?.unstagedCount ?: 0) + (gitStatus?.stagedCount ?: 0)} modified)",
                            color = if (isClean) EmeraldOnline else AmberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Latest Commit
                gitStatus?.latestCommit?.let { commit ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = GlassBorderHighlight.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LATEST COMMIT",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = commit.hash,
                            color = RoyalViolet,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = commit.relativeTime,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = commit.message,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Staged & Unstaged Changes Summary
        if ((gitStatus?.unstaged?.isNotEmpty() == true) || (gitStatus?.staged?.isNotEmpty() == true)) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = "WORKING TREE CHANGES",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    gitStatus.staged.forEach { file ->
                        Text(
                            text = "● $file",
                            color = EmeraldOnline,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    gitStatus.unstaged.forEach { file ->
                        Text(
                            text = "○ $file",
                            color = AmberWarning,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Unified Diff Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UNIFIED DIFF",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    gitDiff?.let { diff ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "+${diff.insertions}",
                                color = EmeraldOnline,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "-${diff.deletions}",
                                color = RubyOffline,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${diff.filesChanged} files",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Diff Window
                val diffText = gitDiff?.diff ?: "No differences recorded."
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 340.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, GlassBorderHighlight.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    val scrollState = rememberScrollState()
                    val horizScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .horizontalScroll(horizScrollState)
                    ) {
                        diffText.lines().forEach { line ->
                            val color = when {
                                line.startsWith("+") && !line.startsWith("+++") -> EmeraldOnline
                                line.startsWith("-") && !line.startsWith("---") -> RubyOffline
                                line.startsWith("@@") -> RoyalViolet
                                line.startsWith("diff --git") -> Color.White.copy(alpha = 0.9f)
                                else -> Color.White.copy(alpha = 0.5f)
                            }
                            Text(
                                text = line,
                                color = color,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildTabContent(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    customCmd: String,
    onCustomCmdChange: (String) -> Unit,
    buildResult: ProjectBuildResult?,
    isBuilding: Boolean,
    error: String?,
    onRunBuild: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Preset selector chips
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(14.dp)
        ) {
            Text(
                text = "TASK PRESET",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("build", "test", "clean", "custom").forEach { preset ->
                    val isSelected = selectedType == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RoyalViolet.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (isSelected) RoyalViolet else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onTypeChange(preset) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.uppercase(),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (selectedType == "custom") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customCmd,
                    onValueChange = onCustomCmdChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. ./gradlew assembleDebug", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f)) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalViolet,
                        unfocusedBorderColor = GlassBorderHighlight
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trigger Button
            Button(
                onClick = onRunBuild,
                enabled = !isBuilding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalViolet.copy(alpha = 0.85f),
                    disabledContainerColor = RoyalViolet.copy(alpha = 0.3f)
                )
            ) {
                if (isBuilding) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Executing Task...", color = Color.White, fontSize = 13.sp)
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Run ${selectedType.replaceFirstChar { it.uppercase() }}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Terminal Console Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TERMINAL LOG STREAM (CIRCULAR 200L)",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                buildResult?.let { res ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (res.success) EmeraldOnline.copy(alpha = 0.2f) else RubyOffline.copy(alpha = 0.2f))
                            .border(1.dp, if (res.success) EmeraldOnline else RubyOffline, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Exit Code ${res.exitCode} (${res.durationMs.toInt()}ms)",
                            color = if (res.success) EmeraldOnline else RubyOffline,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val terminalOutput = buildResult?.let {
                if (it.stdout.isNotBlank() || it.stderr.isNotBlank()) {
                    "${it.command}\n${it.stdout}\n${it.stderr}".trim()
                } else {
                    "Process finished with code ${it.exitCode}"
                }
            } ?: (error ?: "Ready. Tap 'Run' to trigger bounded project execution.")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 360.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, GlassBorderHighlight.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                val scrollState = rememberScrollState()
                val horizScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .horizontalScroll(horizScroll)
                ) {
                    Text(
                        text = terminalOutput,
                        color = if (buildResult?.success == false || error != null) RubyOffline.copy(alpha = 0.9f) else EmeraldOnline.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipboardTabContent(
    macClipboardText: String,
    androidSendText: String,
    onAndroidSendTextChange: (String) -> Unit,
    isLoading: Boolean,
    statusMessage: String?,
    onPullClipboard: () -> Unit,
    onPushClipboard: () -> Unit,
    onCopyToAndroid: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pull Mac Clipboard Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MAC CLIPBOARD",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Fetch live clipboard from Mac",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onPullClipboard,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet.copy(alpha = 0.2f))
                ) {
                    Text(text = "Pull", color = RoyalViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, GlassBorderHighlight.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = if (macClipboardText.isNotBlank()) macClipboardText else "No clipboard content fetched yet.",
                    color = if (macClipboardText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (macClipboardText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onCopyToAndroid(macClipboardText) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldOnline)
                ) {
                    Text(text = "Copy to Android Clipboard", fontSize = 12.sp)
                }
            }
        }

        // Push to Mac Clipboard Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "SEND TO MAC CLIPBOARD",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = androidSendText,
                onValueChange = onAndroidSendTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 180.dp),
                placeholder = { Text("Enter text to copy to Mac...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalViolet,
                    unfocusedBorderColor = GlassBorderHighlight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onPushClipboard,
                enabled = !isLoading && androidSendText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet)
            ) {
                Text(text = "Push to Mac (pbcopy)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            statusMessage?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        // Security Notice
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(12.dp)
        ) {
            Text(
                text = "🛡️ Security: Clipboard contents are ephemeral and transferred only on explicit user action. Clipboard text is never stored in persistent databases or sent to external AI services.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
