package com.remoteforge.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.remoteforge.app.RemoteForgeApplication
import com.remoteforge.app.ui.components.ForgeFloatingNavigationBar
import com.remoteforge.app.ui.components.NavigationTab
import com.remoteforge.app.ui.components.ObsidianBackground
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.screens.*
import com.remoteforge.app.ui.theme.RemoteForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RemoteForgeApplication
        val repository = app.repository

        setContent {
            RemoteForgeTheme {
                var screenStack by remember { mutableStateOf(listOf<AppScreen>(AppScreen.Home)) }
                val currentScreen = screenStack.last()

                // Intercept Android hardware / gesture back button
                BackHandler(enabled = screenStack.size > 1) {
                    screenStack = screenStack.dropLast(1)
                }

                // Automatically connect on launch: restore session, fetch devices, connect WebSocket
                LaunchedEffect(Unit) {
                    repository.autoConnect()
                }

                // Determine active navigation tab in floating dock
                val activeTab = when (currentScreen) {
                    is AppScreen.Home, is AppScreen.Automations, is AppScreen.ActivityLogs -> NavigationTab.HOME
                    is AppScreen.Devices, is AppScreen.DeviceDetails, is AppScreen.ScreenshotViewer, is AppScreen.RemoteControl -> NavigationTab.DEVICES
                    is AppScreen.AiAssistant, is AppScreen.AiTaskDetails -> NavigationTab.AI_ASSISTANT
                    is AppScreen.Files, is AppScreen.FilePreview, is AppScreen.Transfers, is AppScreen.UniversalTransfer -> NavigationTab.FILES
                    is AppScreen.Settings -> NavigationTab.SETTINGS
                }

                // Root full-screen container with deep obsidian background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBackground)
                ) {
                    // Screen transitions
                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(220),
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
                        }
                    }

                    // Floating Bottom Navigation Capsule (hidden during full-screen screenshot viewing and remote trackpad)
                    if (currentScreen !is AppScreen.ScreenshotViewer && currentScreen !is AppScreen.RemoteControl) {
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
                }
            }
        }
    }
}
