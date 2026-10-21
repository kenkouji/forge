package com.remoteforge.app.ui.navigation

sealed class AppScreen {
    object Home : AppScreen()
    object Devices : AppScreen()
    data class DeviceDetails(val deviceId: String) : AppScreen()
    object Files : AppScreen()
    data class FilePreview(
        val deviceId: String,
        val filePath: String,
        val filename: String
    ) : AppScreen()
    object AiAssistant : AppScreen()
    data class AiTaskDetails(val taskId: String) : AppScreen()
    object Transfers : AppScreen()
    data class UniversalTransfer(
        val preselectedSourceDevice: String? = null,
        val preselectedFile: String? = null
    ) : AppScreen()
    object Automations : AppScreen()
    object ActivityLogs : AppScreen()
    object Settings : AppScreen()
    data class ScreenshotViewer(
        val screenshotBase64: String,
        val deviceName: String = "MacBook Pro"
    ) : AppScreen()
    data class RemoteControl(val deviceId: String) : AppScreen()
    data class Developer(val deviceId: String) : AppScreen()
    data class ClipboardHistory(val deviceId: String? = null) : AppScreen()
    data class TaskManager(val deviceId: String? = null) : AppScreen()
    data class LiveScreen(val deviceId: String) : AppScreen()
}
