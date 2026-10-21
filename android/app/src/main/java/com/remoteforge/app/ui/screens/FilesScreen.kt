package com.remoteforge.app.ui.screens

import androidx.compose.foundation.background
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
import com.remoteforge.app.data.model.FileItem
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class FileSortMode(val label: String) {
    NAME("Name"),
    SIZE("Size"),
    DATE("Date")
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    repository: RemoteForgeRepository,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedDevice by repository.selectedDevice.collectAsState()

    var currentPath by remember { mutableStateOf<String?>(null) }
    var itemsList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<FileItem>?>(null) }
    var sortMode by remember { mutableStateOf(FileSortMode.NAME) }

    fun loadPath(path: String?) {
        val dev = selectedDevice
        if (dev == null) {
            errorMessage = "No computer selected. Please go to Devices tab."
            return
        }
        if (!dev.isOnline) {
            errorMessage = "Computer '${dev.name}' is offline."
            return
        }
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val res = repository.listFiles(dev.id, path)
            if (res != null && res.success && res.result != null) {
                currentPath = res.result.canonicalPath
                itemsList = res.result.items
            } else {
                errorMessage = res?.error ?: "Could not load folder contents."
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedDevice?.id) {
        if (selectedDevice != null) {
            loadPath(null)
        }
    }

    // Debounced remote search across entire PC
    LaunchedEffect(searchQuery, selectedDevice?.id) {
        val query = searchQuery.trim()
        val dev = selectedDevice
        if (query.isEmpty() || dev == null) {
            searchResults = null
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        kotlinx.coroutines.delay(350)
        val res = repository.searchFiles(dev.id, query)
        if (res != null && res.success && res.result != null) {
            searchResults = res.result.matches
        } else {
            searchResults = emptyList()
        }
        isSearching = false
    }

    val quickRoots = listOf("Downloads", "Documents", "Desktop", "Projects", "Pictures")

    val displayedItems = remember(itemsList, searchResults, searchQuery, sortMode) {
        val baseList = if (searchQuery.isNotBlank() && searchResults != null) {
            searchResults!!
        } else if (searchQuery.isNotBlank()) {
            itemsList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            itemsList
        }

        when (sortMode) {
            FileSortMode.NAME -> baseList.sortedWith(compareBy<FileItem>({ !it.isDir }, { it.name.lowercase() }))
            FileSortMode.SIZE -> baseList.sortedWith(compareBy<FileItem>({ !it.isDir }, { -it.sizeBytes }))
            FileSortMode.DATE -> baseList.sortedWith(compareBy<FileItem> { !it.isDir }.thenByDescending { it.modifiedAt ?: "" })
        }
    }

    val forgeColors = ForgeTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. Header (RemoteForge, File Explorer, breadcrumb path)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RemoteForge",
                    style = MaterialTheme.typography.labelMedium,
                    color = forgeColors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "File Explorer",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = forgeColors.textPrimary,
                    fontSize = 28.sp
                )
                val breadcrumb = if (!currentPath.isNullOrBlank()) {
                    "${selectedDevice?.name ?: "PC"} > ${currentPath!!.takeLast(32)}"
                } else {
                    "${selectedDevice?.name ?: "PC"} > /Users"
                }
                Text(
                    text = breadcrumb,
                    style = MaterialTheme.typography.bodySmall,
                    color = forgeColors.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { loadPath(currentPath) }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = if (forgeColors.isDark) LavenderAccent else LightAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ==========================================
        // 2. Search Field
        // ==========================================
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search files and folders...", color = forgeColors.textMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = forgeColors.textMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = if (forgeColors.isDark) RoyalViolet else LightAccent,
                        strokeWidth = 2.dp
                    )
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        searchResults = null
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = forgeColors.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = forgeColors.surfaceSecondary,
                unfocusedContainerColor = forgeColors.surfaceSecondary,
                unfocusedBorderColor = forgeColors.borderSubtle,
                focusedBorderColor = if (forgeColors.isDark) RoyalViolet else LightAccent,
                focusedTextColor = forgeColors.textPrimary,
                unfocusedTextColor = forgeColors.textPrimary
            ),
            singleLine = true
        )

        // ==========================================
        // 3. Quick Location Chips Row
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickRoots.forEach { root ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = forgeColors.surfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, forgeColors.borderSubtle),
                    modifier = Modifier.clickable {
                        loadPath(root)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = if (forgeColors.isDark) LavenderAccent else LightAccent, modifier = Modifier.size(14.dp))
                        Text(text = root, color = forgeColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ==========================================
        // 4. Sort and View Controls
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable {
                    sortMode = when (sortMode) {
                        FileSortMode.NAME -> FileSortMode.SIZE
                        FileSortMode.SIZE -> FileSortMode.DATE
                        FileSortMode.DATE -> FileSortMode.NAME
                    }
                }
            ) {
                Text(
                    text = when (sortMode) {
                        FileSortMode.NAME -> "Name >"
                        FileSortMode.SIZE -> "Size >"
                        FileSortMode.DATE -> "Date >"
                    },
                    color = TextRestrainedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Up Directory Action
            if (currentPath != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable {
                        val p = currentPath
                        if (!p.isNullOrBlank()) {
                            val parent = File(p).parent
                            if (parent != null && parent.contains("/Users/")) {
                                loadPath(parent)
                            } else {
                                loadPath(null)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Parent Directory", tint = RoyalViolet, modifier = Modifier.size(14.dp))
                    Text("Up", color = RoyalViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Error Banner
        errorMessage?.let { err ->
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AccentRuby.copy(alpha = 0.5f),
                contentPadding = PaddingValues(10.dp)
            ) {
                Text(text = err, color = AccentRuby, fontSize = 12.sp)
            }
        }

        // ==========================================
        // 5. File & Folder List
        // ==========================================
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RoyalViolet)
            }
        } else if (displayedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Folder is empty or no files match search", color = TextDarkMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(displayedItems, key = { it.path }) { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (item.isDir) {
                                    loadPath(item.path)
                                } else {
                                    val dev = selectedDevice
                                    if (dev != null) {
                                        onNavigate(AppScreen.FilePreview(dev.id, item.path, item.name))
                                    }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = forgeColors.surfaceSecondary,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, forgeColors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                FileTypeBadge(filename = item.name, isDir = item.isDir)

                                Column {
                                    Text(
                                        text = item.name,
                                        color = forgeColors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (item.isDir) "Folder" else "${formatFileSize(item.sizeBytes)} • ${item.modifiedAt?.take(10) ?: "Recent"}",
                                        color = forgeColors.textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (!item.isDir) {
                                IconButton(
                                    onClick = {
                                        val dev = selectedDevice ?: return@IconButton
                                        coroutineScope.launch {
                                            repository.downloadFile(dev.id, item.path, item.name)
                                            onNavigate(AppScreen.Transfers)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download",
                                        tint = if (forgeColors.isDark) LavenderAccent else LightAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = forgeColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
