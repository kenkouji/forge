package com.remoteforge.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.RemoteForgeApplication
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.screens.*
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RemoteForgeApplication
        val repository = app.repository
        val networkConfig = repository.networkConfig

        setContent {
            val themePreference by networkConfig.appearanceThemeFlow.collectAsState()
            val animationIntensity by networkConfig.animationIntensityFlow.collectAsState()
            val reducedMotion by networkConfig.reducedMotionFlow.collectAsState()

            RemoteForgeTheme(themePreference = themePreference, reducedMotion = reducedMotion) {
                var screenStack by remember { mutableStateOf(listOf<AppScreen>(AppScreen.Home)) }
                val currentScreen = screenStack.last()

                // Boot Sequence State (Cinematic launch on first run, skippable)
                var isBooting by remember { mutableStateOf(!networkConfig.bootSequenceSeen && !reducedMotion) }

                // Intercept Android hardware / gesture back button
                BackHandler(enabled = screenStack.size > 1) {
                    screenStack = screenStack.dropLast(1)
                }

                // Automatically connect on launch: restore session, fetch devices, connect WebSocket
                LaunchedEffect(Unit) {
                    repository.autoConnect()
                }

                // Determine active navigation tab in floating dock (EXACTLY 5 TABS)
                val activeTab = when (currentScreen) {
                    is AppScreen.Home, is AppScreen.Automations, is AppScreen.ActivityLogs -> NavigationTab.HOME
                    is AppScreen.Devices, is AppScreen.DeviceDetails, is AppScreen.ScreenshotViewer, is AppScreen.RemoteControl, is AppScreen.Developer, is AppScreen.ClipboardHistory, is AppScreen.TaskManager, is AppScreen.LiveScreen -> NavigationTab.DEVICES
                    is AppScreen.AiAssistant, is AppScreen.AiTaskDetails -> NavigationTab.AI_ASSISTANT
                    is AppScreen.Files, is AppScreen.FilePreview, is AppScreen.Transfers, is AppScreen.UniversalTransfer -> NavigationTab.FILES
                    is AppScreen.Settings -> NavigationTab.SETTINGS
                }

                val isLiveMirroring = currentScreen is AppScreen.LiveScreen

                // Root full-screen container with breathing ambient glow
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Layer 1: Ambient purple breathing glow background
                    ForgeAmbientBackground(
                        intensity = animationIntensity,
                        isLiveMirroring = isLiveMirroring
                    )

                    // Layer 2: Main Application Screens
                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(if (reducedMotion) 0 else 240),
                        label = "ScreenTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { screen ->
                        when (screen) {
                            is AppScreen.Home -> HomeScreen(
                                repository = repository,
                                onNavigate = { newScreen ->
                                    screenStack = screenStack + newScreen
                                }
                            )

                            is AppScreen.Devices -> DevicesScreen(
                                repository = repository,
                                onNavigate = { newScreen ->
                                    screenStack = screenStack + newScreen
                                }
                            )

                            is AppScreen.DeviceDetails -> DeviceDetailsScreen(
                                deviceId = screen.deviceId,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                },
                                onNavigate = { newScreen ->
                                    screenStack = screenStack + newScreen
                                }
                            )

                            is AppScreen.Files -> FilesScreen(
                                repository = repository,
                                onNavigate = { newScreen ->
                                    screenStack = screenStack + newScreen
                                }
                            )

                            is AppScreen.FilePreview -> FilePreviewScreen(
                                deviceId = screen.deviceId,
                                filePath = screen.filePath,
                                filename = screen.filename,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                },
                                onNavigate = { newScreen ->
                                    screenStack = screenStack + newScreen
                                }
                            )

                            is AppScreen.AiAssistant -> AiAssistantScreen(
                                repository = repository,
                                onNavigate = { newScreen ->
                                    screenStack = screenStack + newScreen
                                }
                            )

                            is AppScreen.AiTaskDetails -> AiTaskDetailsScreen(
                                taskId = screen.taskId,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                }
                            )

                            is AppScreen.Transfers -> TransfersScreen(
                                repository = repository,
                                onNavigate = { newScreen ->
                                    screenStack = screenStack + newScreen
                                }
                            )

                            is AppScreen.UniversalTransfer -> UniversalTransferScreen(
                                preselectedSourceDevice = screen.preselectedSourceDevice,
                                preselectedFile = screen.preselectedFile,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                },
                                onNavigate = { newScreen ->
                                    screenStack = screenStack + newScreen
                                }
                            )

                            is AppScreen.Automations -> AutomationsScreen(
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                }
                            )

                            is AppScreen.ActivityLogs -> ActivityScreen(
                                repository = repository
                            )

                            is AppScreen.Settings -> SettingsScreen(
                                repository = repository
                            )

                            is AppScreen.ScreenshotViewer -> ScreenshotViewerScreen(
                                screenshotBase64 = screen.screenshotBase64,
                                deviceName = screen.deviceName,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                }
                            )

                            is AppScreen.RemoteControl -> RemoteControlScreen(
                                deviceId = screen.deviceId,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                }
                            )

                            is AppScreen.ClipboardHistory -> ClipboardScreen(
                                deviceId = screen.deviceId,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                }
                            )

                            is AppScreen.Developer -> DeveloperScreen(
                                deviceId = screen.deviceId,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                }
                            )

                            is AppScreen.TaskManager -> TaskManagerScreen(
                                deviceId = screen.deviceId,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                }
                            )

                            is AppScreen.LiveScreen -> LiveScreenViewerScreen(
                                deviceId = screen.deviceId,
                                repository = repository,
                                onBack = {
                                    if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
                                }
                            )
                        }
                    }

                    // Layer 3: Floating Bottom Navigation Capsule (Hidden during full-screen screenshot, live mirror, or trackpad)
                    if (currentScreen !is AppScreen.ScreenshotViewer && currentScreen !is AppScreen.RemoteControl && currentScreen !is AppScreen.LiveScreen && !isBooting) {
                        ForgeFloatingNavigationBar(
                            selectedTab = activeTab,
                            onTabSelected = { tab ->
                                val targetScreen: AppScreen = when (tab) {
                                    NavigationTab.HOME -> AppScreen.Home
                                    NavigationTab.DEVICES -> AppScreen.Devices
                                    NavigationTab.AI_ASSISTANT -> AppScreen.AiAssistant
                                    NavigationTab.FILES -> AppScreen.Files
                                    NavigationTab.SETTINGS -> AppScreen.Settings
                                    NavigationTab.TRANSFERS -> AppScreen.Transfers
                                    NavigationTab.ACTIVITY -> AppScreen.ActivityLogs
                                }
                                screenStack = listOf(targetScreen)
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    // Layer 4: Cinematic Boot Sequence Overlay
                    if (isBooting) {
                        CinematicBootSequence(
                            onComplete = {
                                networkConfig.bootSequenceSeen = true
                                isBooting = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cinematic First-Launch Boot Sequence
 * The RemoteForge identity emerges from purple ambient light into Home.
 * Tapping anywhere immediately skips it.
 */
@Composable
private fun CinematicBootSequence(onComplete: () -> Unit) {
    val scaleAnim = remember { Animatable(0.7f) }
    val alphaAnim = remember { Animatable(0f) }
    val glowAnim = remember { Animatable(0.2f) }

    LaunchedEffect(Unit) {
        // Phase 1: Glow & emblem rise
        alphaAnim.animateTo(1f, animationSpec = tween(700, easing = EaseOutCubic))
        scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
        glowAnim.animateTo(0.8f, animationSpec = tween(600))
        delay(1100)
        // Phase 2: Fade into Home
        alphaAnim.animateTo(0f, animationSpec = tween(450, easing = EaseInCubic))
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .clickable { onComplete() }, // Tap to skip
        contentAlignment = Alignment.Center
    ) {
        // Glowing radial ambient halo
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(scaleAnim.value)
                .background(
                    Brush.radialGradient(
                        listOf(
                            RoyalViolet.copy(alpha = glowAnim.value * 0.7f),
                            DeepPlum.copy(alpha = glowAnim.value * 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            RemoteForgeEmblem(size = 72.dp)

            Text(
                text = "REMOTEFORGE",
                color = PearlWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )

            Text(
                text = "Secure Autonomous Workstation Control",
                color = LavenderAccent.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}
