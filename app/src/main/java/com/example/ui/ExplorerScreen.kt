package com.example.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.FileItem
import com.example.core.FileShareHelper
import com.example.core.PartitionInfo
import com.example.data.model.DriveEntity
import com.example.data.model.DriveType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.DrivesDashboardView
import com.example.ui.components.FileItemRow
import com.example.ui.components.NothingBadge
import com.example.ui.components.NothingButton
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingPathBreadcrumbs
import com.example.ui.components.NothingRedIndicator
import com.example.ui.components.NothingSegmentedBar
import com.example.ui.components.StorageCleanerView
import com.example.ui.dialogs.ConfirmDeleteDialog
import com.example.ui.dialogs.CreateFileDialog
import com.example.ui.dialogs.CreateFolderDialog
import com.example.ui.dialogs.CreateZipDialog
import com.example.ui.dialogs.ExtractZipDialog
import com.example.ui.dialogs.FilePreviewDialog
import com.example.ui.dialogs.FilePropertiesDialog
import com.example.ui.dialogs.MountDriveDialog
import com.example.ui.dialogs.MountedDrivesDialog
import com.example.ui.dialogs.RenameFileDialog
import com.example.ui.dialogs.SafeFolderDialog
import com.example.ui.dialogs.StorageInfoDialog
import com.example.ui.dialogs.TrashDialog
import com.example.ui.dialogs.VpnDialog
import com.example.vpn.VpnManager
import com.example.vpn.VpnStatus
import com.example.ui.viewer.AudioRepeatMode
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingDarkGray
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingWhite

@Composable
fun ExplorerScreen(viewModel: ExplorerViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val allDrives by viewModel.allDrives.collectAsStateWithLifecycle()
    val vpnStatus by VpnManager.status.collectAsStateWithLifecycle()
    val isVpnConnected = vpnStatus == VpnStatus.CONNECTED
    val partitions by viewModel.partitions.collectAsStateWithLifecycle()
    val activeDrive by viewModel.activeDrive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val showHidden by viewModel.showHidden.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    val selectedPaths by viewModel.selectedPaths.collectAsStateWithLifecycle()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsStateWithLifecycle()
    val isHomeScreen by viewModel.isHomeScreen.collectAsStateWithLifecycle()
    val currentAudio by viewModel.currentAudio.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val audioPositionMs by viewModel.audioPositionMs.collectAsStateWithLifecycle()
    val audioDurationMs by viewModel.audioDurationMs.collectAsStateWithLifecycle()
    val isBackgroundPlayEnabled by viewModel.isBackgroundPlayEnabled.collectAsStateWithLifecycle()
    val audioRepeatMode by viewModel.audioRepeatMode.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val audioSpeed by viewModel.audioSpeed.collectAsStateWithLifecycle()
    val audioEqPreset by viewModel.audioEqPreset.collectAsStateWithLifecycle()

    // Dynamic monochromatic Nothing theme palette
    val bgColor = if (isDarkTheme) NothingBlack else Color(0xFFF7F7F7)
    val cardBg = if (isDarkTheme) NothingDark else Color(0xFFFFFFFF)
    val surfaceBg = if (isDarkTheme) NothingSurface else Color(0xFFEFEFEF)
    val borderClr = if (isDarkTheme) NothingBorder else Color(0xFFD6D6D6)
    val textPrimary = if (isDarkTheme) NothingWhite else Color(0xFF111111)
    val textSecondary = if (isDarkTheme) NothingGray else Color(0xFF6B6B70)
    val textHighlight = if (isDarkTheme) NothingLightGray else Color(0xFF2C2C2E)

    // Re-check storage permission state on resume so permission banner auto-hides immediately
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateStoragePermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Stop audio when app enters background if background play toggle is disabled
    DisposableEffect(lifecycleOwner, isBackgroundPlayEnabled, isAudioPlaying) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                if (!isBackgroundPlayEnabled && isAudioPlaying) {
                    viewModel.pauseAudio()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var showMountDialog by remember { mutableStateOf(false) }
    var showVpnDialog by remember { mutableStateOf(false) }
    var showMountedDrivesDialog by remember { mutableStateOf(false) }
    var showStorageInfoDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showSafeFolderDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showStorageCleanerDialog by remember { mutableStateOf(false) }
    var showCreateZipDialog by remember { mutableStateOf(false) }
    var archiveToExtract by remember { mutableStateOf<FileItem?>(null) }
    var itemsToZip by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var fileToRename by remember { mutableStateOf<FileItem?>(null) }
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var fileForProperties by remember { mutableStateOf<FileItem?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept Back Press: navigate up folder tree, close preview or dismiss search instead of closing app
    BackHandler(
        enabled = previewState != null || isSearchActive || searchQuery.isNotBlank() || selectedPaths.isNotEmpty() || !isHomeScreen || viewModel.canNavigateUp()
    ) {
        when {
            previewState != null -> viewModel.closePreview()
            isSearchActive || searchQuery.isNotBlank() -> {
                isSearchActive = false
                viewModel.setSearchQuery("")
            }
            selectedPaths.isNotEmpty() -> viewModel.clearSelection()
            !isHomeScreen -> viewModel.navigateUp()
        }
    }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.setNotification(null)
        }
    }

    // Filter files or present recursive subfolder search results
    val displayFiles = remember(files, searchQuery, searchResults) {
        if (searchQuery.isBlank()) {
            files
        } else {
            if (searchResults.isNotEmpty()) {
                searchResults
            } else {
                files.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
            }
        }
    }

    // Current active partition info
    val activePartition = remember(partitions, currentPath) {
        partitions.find { currentPath.startsWith(it.path) } ?: partitions.firstOrNull()
    }

    Scaffold(
        containerColor = bgColor,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 16.dp, vertical = if (isLandscape) 4.dp else 8.dp)
            ) {
                // Main Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NOTHING",
                                color = textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isLandscape) 16.sp else 18.sp,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "// EXPLORER",
                                color = textSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (isLandscape) 12.sp else 14.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            NothingRedIndicator(size = 7.dp)
                        }

                        Text(
                            text = if (viewModel.isDeviceRooted) "ROOT ACCESS ENABLED" else "SYSTEM FILESYSTEM ACCESS",
                            color = textSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    // Header Action Buttons: Theme Toggle + Storage Info Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dark / Light Theme Toggle Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(surfaceBg)
                                .border(BorderStroke(1.dp, borderClr), RoundedCornerShape(999.dp))
                                .clickable { viewModel.toggleTheme() }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                                .testTag("theme_toggle_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Light Theme" else "Dark Theme",
                                tint = textPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDarkTheme) "LIGHT" else "DARK",
                                color = textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        // Quick Storage Info / Partitions Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(surfaceBg)
                                .border(BorderStroke(1.dp, borderClr), RoundedCornerShape(999.dp))
                                .clickable { showStorageInfoDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("storage_partitions_quick_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Partitions",
                                tint = textPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${activePartition?.usedPercentage ?: 0}% DISK",
                                color = textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        // If viewing a mapped SMB or cloud drive, show quick Re-Sync button
                        if (activeDrive != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(NothingRed.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, NothingRed.copy(alpha = 0.5f)), RoundedCornerShape(999.dp))
                                    .clickable { viewModel.retryActiveDriveSync() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("resync_drive_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Drive",
                                    tint = NothingRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "SYNC ${activeDrive?.type?.displayName?.take(3)?.uppercase() ?: "SMB"}",
                                    color = NothingRed,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

                // Action Bar Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("FILTER DIRECTORY...", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            colors = nothingTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("search_filter_input")
                        )
                    } else {
                        // Quick action controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.size(36.dp).testTag("open_search_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = NothingLightGray, modifier = Modifier.size(20.dp))
                            }

                            Box {
                                IconButton(
                                    onClick = { sortMenuOpen = true },
                                    modifier = Modifier.size(36.dp).testTag("open_sort_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = NothingLightGray, modifier = Modifier.size(20.dp))
                                }

                                DropdownMenu(
                                    expanded = sortMenuOpen,
                                    onDismissRequest = { sortMenuOpen = false },
                                    modifier = Modifier
                                        .background(NothingBlack)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Name (A-Z)", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setSortMode(SortMode.NAME_ASC)
                                            sortMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name (Z-A)", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setSortMode(SortMode.NAME_DESC)
                                            sortMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Size (Largest First)", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setSortMode(SortMode.SIZE_DESC)
                                            sortMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Date (Newest First)", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setSortMode(SortMode.DATE_DESC)
                                            sortMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Type / Extension", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setSortMode(SortMode.TYPE)
                                            sortMenuOpen = false
                                        }
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.toggleShowHidden() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Hidden Files",
                                    tint = if (showHidden) NothingWhite else NothingGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.loadCurrentDirectory() },
                                modifier = Modifier.size(36.dp).testTag("refresh_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NothingLightGray, modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = {
                                    if (selectedPaths.size == displayFiles.size && displayFiles.isNotEmpty()) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAll()
                                    }
                                },
                                modifier = Modifier.size(36.dp).testTag("select_all_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All",
                                    tint = if (selectedPaths.isNotEmpty()) NothingRed else NothingLightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Mount Drive quick button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(NothingDark)
                                .border(BorderStroke(1.dp, NothingBorder), RoundedCornerShape(999.dp))
                                .clickable { showMountDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("open_mount_dialog_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Mount",
                                tint = NothingRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MOUNT DRIVE",
                                color = NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // VPN quick button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(NothingDark)
                                .border(BorderStroke(1.dp, if (isVpnConnected) NothingGreen else NothingBorder), RoundedCornerShape(999.dp))
                                .clickable { showVpnDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("open_vpn_dialog_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isVpnConnected) NothingGreen else NothingRed)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isVpnConnected) "VPN: ON" else "VPN",
                                color = if (isVpnConnected) NothingGreen else NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mounted Drives & System Partition Quick Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allDrives.forEach { drive ->
                        val isSelected = currentPath == drive.path || (drive.path.length > 1 && currentPath.startsWith(drive.path))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NothingWhite else NothingSurface)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) NothingWhite else NothingBorder),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectDrive(drive) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    NothingRedIndicator(size = 5.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = drive.name.uppercase(),
                                    color = if (isSelected) NothingBlack else NothingLightGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Fast quick-access to root `/system`
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentPath.startsWith("/system")) NothingWhite else NothingDark)
                            .border(BorderStroke(1.dp, NothingBorder), RoundedCornerShape(12.dp))
                            .clickable { viewModel.navigateTo("/system") }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "/SYSTEM",
                            color = if (currentPath.startsWith("/system")) NothingBlack else NothingLightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (currentPath.startsWith("/system")) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Fast quick-access to root `/`
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentPath == "/") NothingWhite else NothingDark)
                            .border(BorderStroke(1.dp, NothingBorder), RoundedCornerShape(12.dp))
                            .clickable { viewModel.navigateTo("/") }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "ROOT (/)",
                            color = if (currentPath == "/") NothingBlack else NothingLightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (currentPath == "/") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Nothing OS Bottom Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Clipboard notification bar if item is copied/cut
                if (clipboard != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingRed, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (clipboard?.isCut == true) "CUT" else "COPIED"}: ${clipboard?.summaryText ?: clipboard?.fileName}",
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Ready to paste in ${currentPath.substringAfterLast('/')}",
                                    color = NothingGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NothingButton(
                                    text = "PASTE",
                                    onClick = { viewModel.pasteClipboard() },
                                    isPrimary = true,
                                    modifier = Modifier.testTag("clipboard_paste_button")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.clearClipboard() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = NothingGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Persistent Background Audio Player Bar if audio is active and preview not open
                if (currentAudio != null && previewState?.type != PreviewType.AUDIO) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NothingDark)
                            .border(
                                1.dp,
                                if (isBackgroundPlayEnabled) NothingGreen else NothingBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                currentAudio?.let { audioFile ->
                                    viewModel.openFilePreview(audioFile)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("mini_audio_player")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Audio",
                                    tint = if (isBackgroundPlayEnabled) NothingGreen else NothingRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentAudio?.name ?: "Audio Playing",
                                        color = NothingWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isBackgroundPlayEnabled) "BACKGROUND AUDIO [ACTIVE]" else "MUSIC PLAYBACK",
                                        color = if (isBackgroundPlayEnabled) NothingGreen else NothingGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.playPreviousAudioTrack() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        contentDescription = "Prev",
                                        tint = NothingWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleAudioPlayback() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = NothingWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.playNextAudioTrack() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = "Next",
                                        tint = NothingWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.stopAudio() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Stop",
                                        tint = NothingGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Dock Buttons / Action Bar
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NothingSurface,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    if (selectedPaths.isNotEmpty()) {
                        // Multi-selection Action Dock
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedPaths.size} SEL",
                                color = NothingRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NothingDark)
                                        .clickable {
                                            itemsToZip = files.filter { selectedPaths.contains(it.path) }
                                            showCreateZipDialog = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Archive, contentDescription = "ZIP", tint = NothingWhite, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("ZIP", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NothingDark)
                                        .clickable { viewModel.moveSelectedToTrash() }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Trash", tint = NothingWhite, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("TRASH", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NothingDark)
                                        .clickable { viewModel.copySelected() }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NothingWhite, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("COPY", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NothingDark)
                                        .clickable { viewModel.moveSelectedToSafeFolder() }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Safe Folder", tint = NothingGreen, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("VAULT", color = NothingGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NothingDark)
                                        .clickable { showBatchDeleteConfirm = true }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NothingRed, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.clearSelection() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Deselect", tint = NothingGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        // Standard Dock: Home, +Folder, +File, Clean, Safe, Mounts
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isHomeScreen) (if (isDarkTheme) NothingDarkGray else Color(0xFFE0E0E0)) else Color.Transparent)
                                    .clickable { viewModel.navigateHome() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_home"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = if (isHomeScreen) NothingRed else NothingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "HOME",
                                    color = if (isHomeScreen) NothingRed else NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCreateFolderDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_new_folder"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CreateNewFolder,
                                    contentDescription = "New Folder",
                                    tint = NothingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ FOLDER",
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCreateFileDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_new_file"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.NoteAdd,
                                    contentDescription = "New File",
                                    tint = NothingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ FILE",
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.pasteClipboard() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_paste"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = if (clipboard != null) NothingGreen else NothingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PASTE",
                                    color = if (clipboard != null) NothingGreen else NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showStorageCleanerDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_clean"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CleaningServices,
                                    contentDescription = "Clean",
                                    tint = NothingRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CLEAN",
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showSafeFolderDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_safe_folder"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Safe Folder",
                                    tint = NothingGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SAFE",
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showTrashDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_trash"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Trash",
                                    tint = NothingLightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TRASH",
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showMountedDrivesDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_mount"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Cloud,
                                    contentDescription = "Mounts",
                                    tint = NothingRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MOUNTS",
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showVpnDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("dock_vpn"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isVpnConnected) NothingGreen else NothingRed)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isVpnConnected) "VPN:ON" else "VPN",
                                    color = if (isVpnConnected) NothingGreen else NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Permission Banner if Android 11+ MANAGE_EXTERNAL_STORAGE not granted
            // Automatically disappears once permission is granted
            if (!hasStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, NothingRed, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NothingRedIndicator(size = 8.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYSTEM STORAGE ACCESS REQUIRED",
                                color = textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To browse Android root system partitions, private folders, and full internal storage, enable 'All files access' in Android Settings.",
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        NothingButton(
                            text = "OPEN SYSTEM SETTINGS",
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            },
                            isPrimary = true,
                            modifier = Modifier.testTag("grant_permission_button")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isHomeScreen) {
                // Home Screen: All Drives & Storage Listing
                DrivesDashboardView(
                    partitions = partitions,
                    allDrives = allDrives,
                    onSelectDrive = { viewModel.selectDrive(it) },
                    onNavigateTo = { viewModel.navigateTo(it) },
                    onOpenMountDialog = { showMountDialog = true },
                    onUnmountDrive = { viewModel.unmountDrive(it) },
                    onOpenSafeFolder = { showSafeFolderDialog = true },
                    onOpenTrash = { showTrashDialog = true },
                    onOpenStorageCleaner = { showStorageCleanerDialog = true },
                    onOpenVpn = { showVpnDialog = true },
                    isDarkTheme = isDarkTheme
                )
            } else {
                // Path Breadcrumbs and "Up" Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceBg)
                            .border(1.dp, borderClr, RoundedCornerShape(10.dp))
                            .clickable { viewModel.navigateUp() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("navigate_up_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Up",
                                tint = textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "UP",
                                color = textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    NothingPathBreadcrumbs(
                        currentPath = currentPath,
                        onNavigateToSegment = { viewModel.navigateTo(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

                // Partition / Drive Metrics Banner (collapses in landscape mode for optimal vertical file viewing)
                if (!isLandscape) {
                    activePartition?.let { part ->
                        NothingCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = cardBg,
                            borderColor = borderClr,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${part.name.uppercase()} [${part.fsType}]",
                                        color = textHighlight,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    NothingBadge(
                                        text = if (part.isReadOnly) "READ ONLY" else "READ / WRITE",
                                        dotColor = if (part.isReadOnly) textSecondary else NothingRed
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                NothingSegmentedBar(
                                    percentage = part.usedPercentage,
                                    segments = 20
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${part.formattedUsed} USED",
                                        color = textSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "${part.formattedFree} FREE OF ${part.formattedTotal}",
                                        color = textHighlight,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Recursive Subfolder Search Info Bar
                if (searchQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceBg)
                            .border(1.dp, borderClr, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = NothingRed,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SEARCHING RECURSIVELY IN SUBFOLDERS...",
                                    color = textSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            } else {
                                Text(
                                    text = "${displayFiles.size} MATCHES IN FOLDERS & SUBFOLDERS",
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = "CLEAR",
                            color = NothingRed,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.setSearchQuery("")
                                    isSearchActive = false
                                }
                                .padding(4.dp)
                                .testTag("clear_search_button")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Dedicated Multi-Selection Action Bar
                if (selectedPaths.isNotEmpty()) {
                    NothingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_selection_bar"),
                        backgroundColor = NothingDark,
                        borderColor = NothingRed,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NothingRedIndicator(size = 6.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedPaths.size} SELECTED",
                                        color = NothingWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (selectedPaths.size == displayFiles.size) "DESELECT" else "SELECT ALL",
                                        color = NothingLightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier
                                            .clickable {
                                                if (selectedPaths.size == displayFiles.size) viewModel.clearSelection()
                                                else viewModel.selectAll()
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )

                                    IconButton(
                                        onClick = { viewModel.clearSelection() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = NothingGray, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Operations: COPY, MOVE, DELETE, RENAME (if 1), PROPERTIES (if 1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // COPY
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingBlack)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.copySelected(isCut = false) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("batch_copy_btn"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NothingWhite, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("COPY", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }

                                // MOVE
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingBlack)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.copySelected(isCut = true) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("batch_move_btn"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DriveFileMove, contentDescription = "Move", tint = NothingWhite, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MOVE", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }

                                // DELETE
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingBlack)
                                        .border(1.dp, NothingRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .clickable { showBatchDeleteConfirm = true }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("batch_delete_btn"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NothingRed, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("DELETE", color = NothingRed, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }

                                // VAULT
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingBlack)
                                        .border(1.dp, NothingGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.moveSelectedToSafeFolder() }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("batch_vault_btn"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Safe Folder", tint = NothingGreen, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("VAULT", color = NothingGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }

                                // SHARE
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingBlack)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            val selectedItems = displayFiles.filter { selectedPaths.contains(it.path) }
                                            FileShareHelper.shareMultipleFiles(context, selectedItems)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("batch_share_btn"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = NothingWhite, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SHARE", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }

                                if (selectedPaths.size == 1) {
                                    val singleItem = displayFiles.firstOrNull { it.path == selectedPaths.first() }
                                    if (singleItem != null) {
                                        if (!singleItem.isDirectory) {
                                            // OPEN WITH
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NothingBlack)
                                                    .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        FileShareHelper.openFileWithExternalApp(context, singleItem)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                                    .testTag("batch_open_with_btn"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = "Open With", tint = NothingLightGray, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("OPEN WITH", color = NothingLightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                            }
                                        }

                                        // RENAME
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NothingBlack)
                                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                                .clickable { fileToRename = singleItem }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                                .testTag("batch_rename_btn"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.NoteAdd, contentDescription = "Rename", tint = NothingLightGray, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("RENAME", color = NothingLightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }

                                        // PROPERTIES
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NothingBlack)
                                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                                .clickable { fileForProperties = singleItem }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                                .testTag("batch_properties_btn"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = "Properties", tint = NothingLightGray, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PROPERTIES", color = NothingLightGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // File Listing Area
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NothingWhite, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "SCANNING DIRECTORY...",
                                color = NothingGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (displayFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(NothingSurface)
                                    .border(1.dp, NothingBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Empty",
                                    tint = NothingGray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "DIRECTORY EMPTY",
                                color = NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No files or accessible folders found in this path.",
                                color = NothingGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("files_lazy_column"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(displayFiles, key = { it.path }) { item ->
                            FileItemRow(
                                item = item,
                                isSelected = selectedPaths.contains(item.path),
                                onClick = {
                                    if (item.isDirectory) {
                                        viewModel.navigateTo(item.path)
                                    } else {
                                        viewModel.openFilePreview(item, asHex = false)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(item.path)
                                },
                                onPreviewText = {
                                    viewModel.openFilePreview(item, asHex = false)
                                },
                                onPreviewHex = {
                                    viewModel.openFilePreview(item, asHex = true)
                                },
                                onCopy = {
                                    viewModel.setClipboard(item.path, isCut = false)
                                },
                                onCut = {
                                    viewModel.setClipboard(item.path, isCut = true)
                                },
                                onRename = {
                                    fileToRename = item
                                },
                                onDelete = {
                                    fileToDelete = item
                                },
                                onBookmark = {
                                    viewModel.toggleBookmark(item.name, item.path, isSystem = item.path.startsWith("/system"))
                                },
                                onProperties = {
                                    fileForProperties = item
                                },
                                onShare = {
                                    FileShareHelper.shareFile(context, item)
                                },
                                onOpenWith = {
                                    FileShareHelper.openFileWithExternalApp(context, item)
                                },
                                onToggleSelect = {
                                    viewModel.toggleSelection(item.path)
                                },
                                onMoveToTrash = {
                                    viewModel.moveToTrash(item)
                                },
                                onMoveToSafeFolder = {
                                    viewModel.moveToSafeFolder(item)
                                },
                                onCompressZip = {
                                    itemsToZip = listOf(item)
                                    showCreateZipDialog = true
                                },
                                onExtractZip = {
                                    archiveToExtract = item
                                },
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showMountDialog) {
        MountDriveDialog(
            networkManager = viewModel.networkStorageManager,
            onDismiss = { showMountDialog = false },
            onOpenVpn = { showVpnDialog = true },
            onMount = { name, type, host, port, share, user, pass ->
                viewModel.mountNewDrive(name, type, host, port, share, user, pass)
            }
        )
    }

    if (showVpnDialog) {
        VpnDialog(
            onDismiss = { showVpnDialog = false }
        )
    }

    if (showMountedDrivesDialog) {
        MountedDrivesDialog(
            mountedDrives = allDrives.filter { it.isMounted },
            onDismiss = { showMountedDrivesDialog = false },
            onOpenLocalHome = {
                viewModel.navigateHome()
                showMountedDrivesDialog = false
            },
            onOpenSystemRoot = {
                viewModel.navigateTo("/system")
                showMountedDrivesDialog = false
            },
            onOpenDrive = { drive ->
                viewModel.selectDrive(drive)
                showMountedDrivesDialog = false
            },
            onUnmountDrive = { drive ->
                viewModel.unmountDrive(drive)
            },
            onOpenMountNewDialog = {
                showMountDialog = true
            }
        )
    }

    if (showStorageInfoDialog) {
        StorageInfoDialog(
            partitions = partitions,
            isRooted = viewModel.isDeviceRooted,
            onNavigateToPartition = { path ->
                viewModel.navigateTo(path)
            },
            onDismiss = { showStorageInfoDialog = false }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { viewModel.createFolder(it) }
        )
    }

    if (showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { showCreateFileDialog = false },
            onCreate = { name, content -> viewModel.createFile(name, content) }
        )
    }

    fileToRename?.let { file ->
        RenameFileDialog(
            file = file,
            onDismiss = { fileToRename = null },
            onRename = { newName -> viewModel.renameItem(file.path, newName) }
        )
    }

    fileToDelete?.let { file ->
        ConfirmDeleteDialog(
            file = file,
            onDismiss = { fileToDelete = null },
            onConfirm = { viewModel.deleteItem(file.path) }
        )
    }

    fileForProperties?.let { file ->
        FilePropertiesDialog(
            file = file,
            onDismiss = { fileForProperties = null },
            onMoveToSafeFolder = { item -> viewModel.moveToSafeFolder(item) }
        )
    }

    if (showBatchDeleteConfirm) {
        ConfirmDeleteDialog(
            file = FileItem(
                name = "${selectedPaths.size} selected items",
                path = "",
                size = 0L,
                permissions = "rw-r--r--",
                lastModified = System.currentTimeMillis(),
                isDirectory = false
            ),
            onDismiss = { showBatchDeleteConfirm = false },
            onConfirm = {
                val paths = selectedPaths.toList()
                paths.forEach { viewModel.deleteItem(it) }
                viewModel.clearSelection()
                showBatchDeleteConfirm = false
            }
        )
    }

    previewState?.let { state ->
        FilePreviewDialog(
            state = state,
            onDismiss = { viewModel.closePreview() },
            onToggleHex = { viewModel.togglePreviewHex() },
            onNextMedia = { viewModel.navigateNextMedia() },
            onPreviousMedia = { viewModel.navigatePreviousMedia() },
            isAudioPlaying = isAudioPlaying,
            audioPositionMs = audioPositionMs,
            audioDurationMs = audioDurationMs,
            isBackgroundPlayEnabled = isBackgroundPlayEnabled,
            audioRepeatMode = audioRepeatMode,
            isShuffleEnabled = isShuffleEnabled,
            audioPlaylist = files.filter { it.isAudio },
            onToggleAudioPlayPause = { viewModel.toggleAudioPlayback() },
            onSeekAudio = { viewModel.seekAudio(it) },
            onRestartAudio = { viewModel.restartAudio() },
            onToggleBackgroundPlay = { viewModel.toggleBackgroundPlay() },
            onToggleAudioRepeatMode = { viewModel.toggleAudioRepeatMode() },
            onToggleAudioShuffle = { viewModel.toggleAudioShuffle() },
            onSelectAudioTrack = { file ->
                viewModel.playAudioTrack(file)
                viewModel.openFilePreview(file)
            },
            onAudioSpeedChange = { viewModel.setAudioSpeed(it) },
            audioEqPreset = audioEqPreset,
            onAudioEqPresetChange = { viewModel.setAudioEqPreset(it) }
        )
    }

    if (showSafeFolderDialog) {
        SafeFolderDialog(
            safeFolderManager = viewModel.safeFolderManager,
            currentFolderPath = currentPath,
            onDismiss = {
                showSafeFolderDialog = false
                viewModel.loadCurrentDirectory()
            },
            onOpenFile = { file ->
                viewModel.openFilePreview(file)
            }
        )
    }

    if (showTrashDialog) {
        TrashDialog(
            trashManager = viewModel.trashManager,
            onDismiss = {
                showTrashDialog = false
                viewModel.loadCurrentDirectory()
            },
            onRestored = {
                viewModel.loadCurrentDirectory()
            }
        )
    }

    if (showStorageCleanerDialog) {
        Dialog(
            onDismissRequest = { showStorageCleanerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NothingBlack)
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(WindowInsets.navigationBars.asPaddingValues())
            ) {
                StorageCleanerView(
                    cleanerManager = viewModel.storageCleanerManager,
                    internalPartition = partitions.firstOrNull(),
                    onOpenFile = { file ->
                        viewModel.openFilePreview(file)
                    },
                    onClose = {
                        showStorageCleanerDialog = false
                        viewModel.loadPartitions()
                        viewModel.loadCurrentDirectory()
                    },
                    onNavigateToPath = { path ->
                        showStorageCleanerDialog = false
                        viewModel.navigateTo(path)
                    }
                )
            }
        }
    }

    if (showCreateZipDialog && itemsToZip.isNotEmpty()) {
        CreateZipDialog(
            selectedFiles = itemsToZip,
            destinationDirectory = currentPath,
            onDismiss = {
                showCreateZipDialog = false
                itemsToZip = emptyList()
            },
            onZipCreated = { createdFile ->
                showCreateZipDialog = false
                itemsToZip = emptyList()
                viewModel.setNotification("Archive created: ${createdFile.name}")
                viewModel.loadCurrentDirectory()
            }
        )
    }

    archiveToExtract?.let { archiveFile ->
        ExtractZipDialog(
            archiveFile = archiveFile,
            destinationDirectory = currentPath,
            onDismiss = { archiveToExtract = null },
            onExtracted = {
                archiveToExtract = null
                viewModel.setNotification("Archive extracted successfully")
                viewModel.loadCurrentDirectory()
            }
        )
    }
}
