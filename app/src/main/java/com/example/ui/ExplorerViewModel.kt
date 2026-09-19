package com.example.ui

import android.app.Application
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AudioNotificationManager
import com.example.core.FileCategory
import com.example.core.FileItem
import com.example.core.FilesystemManager
import com.example.core.NetworkStorageManager
import com.example.core.PartitionInfo
import com.example.core.SampleMediaGenerator
import com.example.core.excel.ExcelParser
import com.example.core.excel.ExcelSheetData
import com.example.core.ArchiveManager
import com.example.core.StorageCleanerManager
import com.example.core.SafeFolderManager
import com.example.core.TrashManager
import com.example.data.AppDatabase
import com.example.data.ExplorerRepository
import com.example.data.model.BookmarkEntity
import com.example.data.model.DriveEntity
import com.example.data.model.DriveType
import com.example.ui.viewer.AudioEqPreset
import com.example.ui.viewer.AudioRepeatMode
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class ClipboardState(
    val paths: List<String>,
    val isCut: Boolean,
    val summaryText: String
) {
    val path: String get() = paths.firstOrNull() ?: ""
    val fileName: String get() = summaryText
}

enum class PreviewType {
    PHOTO,
    TEXT,
    VIDEO,
    AUDIO,
    PDF,
    EXCEL,
    DOCX,
    HEX
}

data class PreviewDialogState(
    val file: FileItem,
    val type: PreviewType,
    val content: String = "",
    val excelData: ExcelSheetData? = null,
    val isHex: Boolean = false,
    val md5Checksum: String? = null,
    val currentIndex: Int = 1,
    val totalCount: Int = 1,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

enum class SortMode {
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    DATE_DESC,
    TYPE
}

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ExplorerRepository(db.driveDao(), db.bookmarkDao(), db.transferDao())
    val filesystemManager = FilesystemManager()
    val networkStorageManager = NetworkStorageManager()
    val trashManager = TrashManager(application)
    val safeFolderManager = SafeFolderManager(application)
    val storageCleanerManager = StorageCleanerManager()
    val archiveManager = ArchiveManager

    val allDrives: StateFlow<List<DriveEntity>> = repository.allDrives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    private val _isHomeScreen = MutableStateFlow(true)
    val isHomeScreen: StateFlow<Boolean> = _isHomeScreen.asStateFlow()

    private val _currentPath = MutableStateFlow(
        if (checkStoragePermission()) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            application.filesDir.resolve("sample_media").absolutePath
        }
    )
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _partitions = MutableStateFlow<List<PartitionInfo>>(emptyList())
    val partitions: StateFlow<List<PartitionInfo>> = _partitions.asStateFlow()

    private val _activeDrive = MutableStateFlow<DriveEntity?>(null)
    val activeDrive: StateFlow<DriveEntity?> = _activeDrive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val searchResults: StateFlow<List<FileItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    private val _sortMode = MutableStateFlow(SortMode.NAME_ASC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _showHidden = MutableStateFlow(true)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard: StateFlow<ClipboardState?> = _clipboard.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    private val _previewState = MutableStateFlow<PreviewDialogState?>(null)
    val previewState: StateFlow<PreviewDialogState?> = _previewState.asStateFlow()

    // Background Audio Playback Engine
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var audioProgressJob: Job? = null
    private var audioEqualizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private val _audioEqPreset = MutableStateFlow(AudioEqPreset.BASS_BOOST)
    val audioEqPreset: StateFlow<AudioEqPreset> = _audioEqPreset.asStateFlow()

    private val _currentAudio = MutableStateFlow<FileItem?>(null)
    val currentAudio: StateFlow<FileItem?> = _currentAudio.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _audioPositionMs = MutableStateFlow(0)
    val audioPositionMs: StateFlow<Int> = _audioPositionMs.asStateFlow()

    private val _audioDurationMs = MutableStateFlow(0)
    val audioDurationMs: StateFlow<Int> = _audioDurationMs.asStateFlow()

    private val _isBackgroundPlayEnabled = MutableStateFlow(false)
    val isBackgroundPlayEnabled: StateFlow<Boolean> = _isBackgroundPlayEnabled.asStateFlow()

    private val _audioRepeatMode = MutableStateFlow(AudioRepeatMode.ALL)
    val audioRepeatMode: StateFlow<AudioRepeatMode> = _audioRepeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _audioPlaylist = MutableStateFlow<List<FileItem>>(emptyList())
    val audioPlaylist: StateFlow<List<FileItem>> = _audioPlaylist.asStateFlow()

    private val audioNotificationManager by lazy {
        AudioNotificationManager(
            context = getApplication(),
            onPlay = { toggleAudioPlayback() },
            onPause = { pauseAudio() },
            onNext = { playNextAudioTrack(wrapAround = true) },
            onPrevious = { playPreviousAudioTrack() },
            onStop = { stopAudio() }
        )
    }

    private fun syncAudioNotification() {
        audioNotificationManager.updateNotification(
            file = _currentAudio.value,
            isPlaying = _isAudioPlaying.value,
            positionMs = _audioPositionMs.value,
            durationMs = _audioDurationMs.value,
            isBackgroundPlayEnabled = _isBackgroundPlayEnabled.value
        )
    }

    private val _audioSpeed = MutableStateFlow(1.0f)
    val audioSpeed: StateFlow<Float> = _audioSpeed.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    private val _notification = MutableStateFlow<String?>(null)
    val notification: StateFlow<String?> = _notification.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isDeviceRooted = filesystemManager.isDeviceRooted()

    private val _hasStoragePermission = MutableStateFlow(checkStoragePermission())
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    val hasManageStoragePermission: Boolean
        get() = _hasStoragePermission.value

    fun updateStoragePermissionState() {
        val newState = checkStoragePermission()
        if (_hasStoragePermission.value != newState) {
            _hasStoragePermission.value = newState
            if (newState) {
                _currentPath.value = Environment.getExternalStorageDirectory().absolutePath
                loadCurrentDirectory()
                loadPartitions()
                startRealtimeFileWatcher(_currentPath.value)
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private var currentFileObserver: FileObserver? = null

    init {
        viewModelScope.launch {
            val sampleDir = SampleMediaGenerator.ensureSampleFiles(application)
            val initial = repository.allDrives.first()
            if (initial.isEmpty()) {
                repository.initializeDefaultDrivesIfEmpty(sampleDir.absolutePath)
            }
            loadPartitions()
            loadCurrentDirectory()
            startRealtimeFileWatcher(_currentPath.value)
        }
    }

    private fun startRealtimeFileWatcher(path: String) {
        try {
            currentFileObserver?.stopWatching()
            currentFileObserver = null

            val dir = File(path)
            // Guard against SELinux denials by only watching accessible user/app directories
            if (!dir.exists() || !dir.canRead() || !dir.isDirectory) {
                return
            }
            val abs = dir.absolutePath
            // Avoid inotify on root, sys, or system mountpoints where unprivileged apps are denied by SELinux
            if (abs == "/" || abs.startsWith("/system") || abs.startsWith("/vendor") ||
                abs.startsWith("/apex") || abs.startsWith("/proc") || abs.startsWith("/sys") || abs.startsWith("/dev")) {
                return
            }

            val mask = FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or
                    FileObserver.MOVED_FROM or FileObserver.MOVED_TO
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                currentFileObserver = object : FileObserver(dir, mask) {
                    override fun onEvent(event: Int, file: String?) {
                        viewModelScope.launch {
                            refreshCurrentDirectorySilently()
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                currentFileObserver = object : FileObserver(abs, mask) {
                    override fun onEvent(event: Int, file: String?) {
                        viewModelScope.launch {
                            refreshCurrentDirectorySilently()
                        }
                    }
                }
            }
            currentFileObserver?.startWatching()
        } catch (_: Throwable) {
            currentFileObserver = null
        }
    }

    suspend fun refreshCurrentDirectorySilently() {
        try {
            val path = _currentPath.value
            val dir = File(path)
            if (!dir.exists() || !dir.canRead()) return

            val result = filesystemManager.listDirectory(path, _showHidden.value)
            result.onSuccess { list ->
                if (_files.value != list) {
                    _files.value = list
                    sortCurrentFiles()
                }
            }
        } catch (_: Throwable) {}
    }

    fun loadPartitions() {
        viewModelScope.launch {
            _partitions.value = filesystemManager.getPartitions()
        }
    }

    fun setNotification(msg: String?) {
        _notification.value = msg
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
        } else {
            searchJob = viewModelScope.launch {
                _isSearching.value = true
                val results = filesystemManager.searchRecursive(
                    rootPath = _currentPath.value,
                    query = cleanQuery,
                    includeHidden = _showHidden.value
                )
                results.onSuccess { list ->
                    _searchResults.value = list
                }.onFailure {
                    _searchResults.value = emptyList()
                }
                _isSearching.value = false
            }
        }
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        sortCurrentFiles()
    }

    fun toggleShowHidden() {
        _showHidden.value = !_showHidden.value
        loadCurrentDirectory()
    }

    fun navigateTo(path: String) {
        _isHomeScreen.value = false
        _currentPath.value = path
        _selectedPaths.value = emptySet()
        loadCurrentDirectory()
        startRealtimeFileWatcher(path)
        if (_searchQuery.value.isNotBlank()) {
            setSearchQuery(_searchQuery.value)
        }
    }

    fun canNavigateUp(): Boolean {
        if (_isHomeScreen.value) return false
        return true
    }

    fun navigateUp() {
        if (_isHomeScreen.value) return
        val current = File(_currentPath.value)
        val parent = current.parentFile
        val activePath = _activeDrive.value?.path
        val isAtDriveRoot = (activePath != null && current.absolutePath == activePath) ||
                current.absolutePath == Environment.getExternalStorageDirectory().absolutePath ||
                current.absolutePath == getApplication<Application>().filesDir.resolve("sample_media").absolutePath ||
                current.absolutePath == "/"

        if (isAtDriveRoot || parent == null || !parent.exists() || !parent.canRead()) {
            navigateHome()
        } else {
            navigateTo(parent.absolutePath)
        }
    }

    fun navigateHome() {
        _isHomeScreen.value = true
        _activeDrive.value = null
        _selectedPaths.value = emptySet()
        if (!_isBackgroundPlayEnabled.value) {
            stopAudio()
        }
        setNotification("Home: All Connected Drives")
    }

    fun selectDrive(drive: DriveEntity) {
        _isHomeScreen.value = false
        _activeDrive.value = drive
        if (!_isBackgroundPlayEnabled.value) {
            stopAudio()
        }
        viewModelScope.launch {
            if (drive.type in listOf(
                    DriveType.CLOUD_GDRIVE,
                    DriveType.CLOUD_DROPBOX,
                    DriveType.CLOUD_ONEDRIVE,
                    DriveType.NETWORK_SMB,
                    DriveType.NETWORK_FTP
                )
            ) {
                _isLoading.value = true
                val mappedDir = networkStorageManager.getOrCreateMappedDirectory(getApplication(), drive)
                _isLoading.value = false
                navigateTo(mappedDir.absolutePath)
            } else {
                navigateTo(drive.path)
            }
        }
    }

    fun loadCurrentDirectory() {
        viewModelScope.launch {
            _isLoading.value = true
            val path = _currentPath.value

            val drive = _activeDrive.value
            if (drive != null && drive.type == DriveType.NETWORK_SMB && path.startsWith(drive.path)) {
                try {
                    networkStorageManager.syncSmbDirectory(File(path), drive)
                } catch (_: Exception) {}
            }

            val result = filesystemManager.listDirectory(path, _showHidden.value)
            result.onSuccess { list ->
                _files.value = list
                sortCurrentFiles()
            }.onFailure { err ->
                _files.value = emptyList()
                setNotification("Cannot read directory: ${err.message}")
            }
            _isLoading.value = false
        }
    }

    fun retryActiveDriveSync() {
        val drive = _activeDrive.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            setNotification("Syncing ${drive.type.displayName} [${drive.host.ifEmpty { drive.name }}]...")
            if (drive.type == DriveType.NETWORK_SMB) {
                val target = File(_currentPath.value)
                val result = networkStorageManager.syncSmbDirectory(target, drive)
                result.onSuccess { count ->
                    setNotification("SMB Synchronized: $count items updated from ${drive.host}")
                    loadCurrentDirectory()
                }.onFailure { err ->
                    setNotification("SMB Sync: ${err.message ?: "Connection timed out"}")
                }
            } else if (drive.type == DriveType.NETWORK_FTP) {
                setNotification("FTP Synchronized: Directory refreshed")
                loadCurrentDirectory()
            } else {
                networkStorageManager.getOrCreateMappedDirectory(getApplication(), drive)
                loadCurrentDirectory()
                setNotification("${drive.name} refreshed")
            }
            _isLoading.value = false
        }
    }

    private fun sortCurrentFiles() {
        val current = _files.value
        val sorted = when (_sortMode.value) {
            SortMode.NAME_ASC -> current.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
            SortMode.NAME_DESC -> current.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenByDescending { it.name.lowercase(Locale.getDefault()) }
            )
            SortMode.SIZE_DESC -> current.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenByDescending { it.size }
            )
            SortMode.DATE_DESC -> current.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenByDescending { it.lastModified }
            )
            SortMode.TYPE -> current.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenBy { it.category.name }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
        }
        _files.value = sorted
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val res = filesystemManager.createFolder(_currentPath.value, name)
            res.onSuccess {
                val drive = _activeDrive.value
                if (drive != null && drive.type == DriveType.NETWORK_SMB && _currentPath.value.startsWith(drive.path)) {
                    val folder = File(_currentPath.value, name)
                    networkStorageManager.createSmbDirectory(folder, drive)
                }
                setNotification("Folder '$name' created")
                loadCurrentDirectory()
            }.onFailure {
                setNotification("Failed to create folder: ${it.message}")
            }
        }
    }

    fun createFile(name: String, content: String = "") {
        viewModelScope.launch {
            val res = filesystemManager.createFile(_currentPath.value, name, content)
            res.onSuccess {
                val drive = _activeDrive.value
                if (drive != null && drive.type == DriveType.NETWORK_SMB && _currentPath.value.startsWith(drive.path)) {
                    val f = File(_currentPath.value, name)
                    networkStorageManager.uploadSmbFile(f, drive)
                }
                setNotification("File '$name' created")
                loadCurrentDirectory()
            }.onFailure {
                setNotification("Failed to create file: ${it.message}")
            }
        }
    }

    fun renameItem(oldPath: String, newName: String) {
        viewModelScope.launch {
            val res = filesystemManager.rename(oldPath, newName)
            res.onSuccess {
                val drive = _activeDrive.value
                if (drive != null && drive.type == DriveType.NETWORK_SMB && oldPath.startsWith(drive.path)) {
                    val oldF = File(oldPath)
                    val newF = File(oldF.parentFile, newName)
                    networkStorageManager.deleteSmbItem(oldF, drive)
                    if (newF.isDirectory) {
                        networkStorageManager.createSmbDirectory(newF, drive)
                    } else {
                        networkStorageManager.uploadSmbFile(newF, drive)
                    }
                }
                setNotification("Renamed to '$newName'")
                loadCurrentDirectory()
            }.onFailure {
                setNotification("Rename failed: ${it.message}")
            }
        }
    }

    fun deleteItem(path: String) {
        viewModelScope.launch {
            val drive = _activeDrive.value
            if (drive != null && drive.type == DriveType.NETWORK_SMB && path.startsWith(drive.path)) {
                networkStorageManager.deleteSmbItem(File(path), drive)
            }
            val trashRes = trashManager.moveToTrash(path)
            if (trashRes.isSuccess) {
                setNotification("Moved to Trash")
                loadCurrentDirectory()
            } else {
                val res = filesystemManager.delete(path)
                res.onSuccess {
                    setNotification("Deleted successfully")
                    loadCurrentDirectory()
                }.onFailure {
                    setNotification("Delete failed: ${it.message}")
                }
            }
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = _selectedPaths.value.toList()
            val drive = _activeDrive.value
            var failCount = 0
            var trashCount = 0
            for (p in toDelete) {
                if (drive != null && drive.type == DriveType.NETWORK_SMB && p.startsWith(drive.path)) {
                    networkStorageManager.deleteSmbItem(File(p), drive)
                }
                val trashRes = trashManager.moveToTrash(p)
                if (trashRes.isSuccess) {
                    trashCount++
                } else {
                    val r = filesystemManager.delete(p)
                    if (r.isFailure) failCount++
                }
            }
            _selectedPaths.value = emptySet()
            if (failCount > 0) {
                setNotification("Failed to delete $failCount item(s)")
            } else {
                setNotification("Moved ${toDelete.size} items to Trash")
            }
            loadCurrentDirectory()
        }
    }

    fun setClipboard(path: String, isCut: Boolean) {
        copyToClipboard(path, isCut)
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    fun selectAll(paths: List<String> = emptyList()) {
        val targetPaths = if (paths.isNotEmpty()) paths else _files.value.map { it.path }
        _selectedPaths.value = targetPaths.toSet()
        setNotification("Selected ${targetPaths.size} item(s)")
    }

    fun copySelected(isCut: Boolean = false) {
        val selected = _selectedPaths.value.toList()
        if (selected.isEmpty()) return
        val summary = if (selected.size == 1) {
            File(selected.first()).name
        } else {
            "${selected.size} items"
        }
        _clipboard.value = ClipboardState(
            paths = selected,
            isCut = isCut,
            summaryText = summary
        )
        _selectedPaths.value = emptySet()
        // Sync to system ClipboardManager so paste directly works in every app
        try {
            val app = getApplication<Application>()
            val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cm != null && selected.isNotEmpty()) {
                val clipData = ClipData.newPlainText(summary, selected.joinToString("\n"))
                val firstFile = File(selected.first())
                if (firstFile.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", firstFile)
                        clipData.addItem(ClipData.Item(uri))
                    } catch (_: Exception) {}
                }
                cm.setPrimaryClip(clipData)
            }
        } catch (e: Exception) {
            Log.e("ExplorerViewModel", "Sync to system clipboard failed", e)
        }
        setNotification(if (isCut) "Cut $summary to clipboard" else "Copied $summary to clipboard")
    }

    fun cutSelected() = copySelected(isCut = true)

    fun copyToClipboard(path: String, isCut: Boolean) {
        val f = File(path)
        _clipboard.value = ClipboardState(
            paths = listOf(path),
            isCut = isCut,
            summaryText = f.name
        )
        // Sync to system ClipboardManager
        try {
            val app = getApplication<Application>()
            val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cm != null) {
                val clipData = ClipData.newPlainText(f.name, path)
                if (f.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", f)
                        clipData.addItem(ClipData.Item(uri))
                    } catch (_: Exception) {}
                }
                cm.setPrimaryClip(clipData)
            }
        } catch (e: Exception) {
            Log.e("ExplorerViewModel", "Sync to system clipboard failed", e)
        }
        setNotification(if (isCut) "Cut '${f.name}'" else "Copied '${f.name}'")
    }

    fun pasteClipboard() {
        val internalClip = _clipboard.value
        if (internalClip != null) {
            viewModelScope.launch {
                val drive = _activeDrive.value
                var successCount = 0
                var failCount = 0
                for (p in internalClip.paths) {
                    val res = if (internalClip.isCut) {
                        filesystemManager.move(p, _currentPath.value)
                    } else {
                        filesystemManager.copy(p, _currentPath.value)
                    }
                    if (res.isSuccess) {
                        successCount++
                        if (drive != null && drive.type == DriveType.NETWORK_SMB && _currentPath.value.startsWith(drive.path)) {
                            val destFile = File(_currentPath.value, File(p).name)
                            if (destFile.exists()) {
                                if (destFile.isDirectory) {
                                    networkStorageManager.createSmbDirectory(destFile, drive)
                                } else {
                                    networkStorageManager.uploadSmbFile(destFile, drive)
                                }
                            }
                        }
                    } else failCount++
                }
                if (failCount > 0) {
                    setNotification("Pasted $successCount item(s), $failCount failed")
                } else {
                    setNotification("Pasted ${internalClip.summaryText} into current folder")
                }
                if (internalClip.isCut) {
                    _clipboard.value = null
                }
                loadCurrentDirectory()
            }
        } else {
            // Read from system clipboard directly (from external apps or any source)
            viewModelScope.launch {
                try {
                    val app = getApplication<Application>()
                    val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val pClip = cm?.primaryClip
                    if (pClip == null || pClip.itemCount == 0) {
                        setNotification("Clipboard is empty")
                        return@launch
                    }

                    var pastedCount = 0
                    for (i in 0 until pClip.itemCount) {
                        val item = pClip.getItemAt(i)
                        val uri = item.uri
                        if (uri != null) {
                            try {
                                val name = uri.lastPathSegment?.substringAfterLast('/')?.ifEmpty { null }
                                    ?: "pasted_file_${System.currentTimeMillis()}"
                                val destFile = File(_currentPath.value, name)
                                withContext(Dispatchers.IO) {
                                    app.contentResolver.openInputStream(uri)?.use { input ->
                                        destFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                }
                                pastedCount++
                            } catch (e: Exception) {
                                Log.e("ExplorerViewModel", "Failed to paste URI from system clipboard", e)
                            }
                        } else {
                            val text = item.text?.toString()
                            if (!text.isNullOrBlank()) {
                                val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                val validFiles = lines.filter { File(it).exists() }
                                if (validFiles.isNotEmpty()) {
                                    for (vp in validFiles) {
                                        val res = filesystemManager.copy(vp, _currentPath.value)
                                        if (res.isSuccess) pastedCount++
                                    }
                                } else {
                                    // Save plain text from clipboard into a text note in current folder
                                    withContext(Dispatchers.IO) {
                                        val destFile = File(_currentPath.value, "clipboard_${System.currentTimeMillis() % 100000}.txt")
                                        destFile.writeText(text)
                                    }
                                    pastedCount++
                                }
                            }
                        }
                    }

                    if (pastedCount > 0) {
                        setNotification("Pasted $pastedCount item(s) from system clipboard")
                        loadCurrentDirectory()
                    } else {
                        setNotification("No compatible content in clipboard")
                    }
                } catch (e: Exception) {
                    Log.e("ExplorerViewModel", "Failed to paste from system clipboard", e)
                    setNotification("Failed to paste: ${e.message}")
                }
            }
        }
    }

    fun openFilePreview(file: FileItem, asHex: Boolean = false) {
        viewModelScope.launch {
            val drive = _activeDrive.value
            if (drive != null && drive.type == DriveType.NETWORK_SMB && file.path.startsWith(drive.path)) {
                val f = File(file.path)
                if (!f.exists() || f.length() == 0L) {
                    networkStorageManager.downloadSmbFile(f, drive)
                }
            }

            // Skip heavy checksum calculations on media or large files to prevent UI freeze/hangs
            val md5 = if (file.size in 1..(2 * 1024 * 1024L) && !file.isVideo && !file.isAudio) {
                filesystemManager.calculateChecksum(file.path, "MD5").getOrNull()
            } else null
            val ext = file.extension.lowercase()

            if (asHex) {
                val hexDump = filesystemManager.readHexDump(file.path, 1024).getOrDefault("Failed to read binary")
                _previewState.value = PreviewDialogState(
                    file = file,
                    type = PreviewType.HEX,
                    content = hexDump,
                    isHex = true,
                    md5Checksum = md5
                )
                return@launch
            }

            val targetType = when {
                ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "heif") -> PreviewType.PHOTO
                ext in listOf("mp4", "mkv", "webm", "3gp", "avi", "mov", "m4v") -> PreviewType.VIDEO
                ext in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus", "mid") -> PreviewType.AUDIO
                ext == "pdf" -> PreviewType.PDF
                ext in listOf("docx", "doc") -> PreviewType.DOCX
                ext in listOf("xlsx", "xls", "csv", "tsv") -> PreviewType.EXCEL
                ext in listOf(
                    "txt", "log", "xml", "json", "md", "kt", "java", "py", "html",
                    "css", "js", "ts", "sh", "ini", "conf", "rc", "prop", "properties",
                    "yaml", "yml", "sql", "gradle", "c", "cpp", "h"
                ) || file.category == FileCategory.DOCUMENT || file.category == FileCategory.CODE -> PreviewType.TEXT
                else -> PreviewType.HEX
            }

            // Calculate pagination indices for swipeable media in current directory
            val siblingMedia = when (targetType) {
                PreviewType.PHOTO -> _files.value.filter { it.isPhoto }
                PreviewType.VIDEO -> _files.value.filter { it.isVideo }
                PreviewType.AUDIO -> _files.value.filter { it.isAudio }
                else -> emptyList()
            }
            val currentIndexInFolder = siblingMedia.indexOfFirst { it.path == file.path }
            val totalCount = siblingMedia.size.coerceAtLeast(1)
            val currentNumber = if (currentIndexInFolder >= 0) currentIndexInFolder + 1 else 1
            val hasNext = currentIndexInFolder in 0 until (siblingMedia.size - 1)
            val hasPrev = currentIndexInFolder > 0

            when (targetType) {
                PreviewType.PHOTO -> {
                    _previewState.value = PreviewDialogState(
                        file = file,
                        type = PreviewType.PHOTO,
                        md5Checksum = md5,
                        currentIndex = currentNumber,
                        totalCount = totalCount,
                        hasNext = hasNext,
                        hasPrevious = hasPrev
                    )
                }

                PreviewType.VIDEO -> {
                    _previewState.value = PreviewDialogState(
                        file = file,
                        type = PreviewType.VIDEO,
                        md5Checksum = md5,
                        currentIndex = currentNumber,
                        totalCount = totalCount,
                        hasNext = hasNext,
                        hasPrevious = hasPrev
                    )
                }

                PreviewType.AUDIO -> {
                    playAudioTrack(file)
                    _previewState.value = PreviewDialogState(
                        file = file,
                        type = PreviewType.AUDIO,
                        md5Checksum = md5,
                        currentIndex = currentNumber,
                        totalCount = totalCount,
                        hasNext = hasNext,
                        hasPrevious = hasPrev
                    )
                }

                PreviewType.PDF -> {
                    _previewState.value = PreviewDialogState(
                        file = file,
                        type = PreviewType.PDF,
                        md5Checksum = md5
                    )
                }

                PreviewType.DOCX -> {
                    _previewState.value = PreviewDialogState(
                        file = file,
                        type = PreviewType.DOCX,
                        md5Checksum = md5
                    )
                }

                PreviewType.EXCEL -> {
                    val excelData = withContext(Dispatchers.IO) {
                        ExcelParser.parse(File(file.path))
                    }
                    _previewState.value = PreviewDialogState(
                        file = file,
                        type = PreviewType.EXCEL,
                        excelData = excelData,
                        md5Checksum = md5
                    )
                }

                PreviewType.TEXT -> {
                    val text = filesystemManager.readFileContent(file.path, 16000).getOrDefault("Could not read text content")
                    _previewState.value = PreviewDialogState(
                        file = file,
                        type = PreviewType.TEXT,
                        content = text,
                        isHex = false,
                        md5Checksum = md5
                    )
                }

                else -> {
                    val hexDump = filesystemManager.readHexDump(file.path, 1024).getOrDefault("Failed to read binary")
                    _previewState.value = PreviewDialogState(
                        file = file,
                        type = PreviewType.HEX,
                        content = hexDump,
                        isHex = true,
                        md5Checksum = md5
                    )
                }
            }
        }
    }

    fun navigateNextMedia() {
        val current = _previewState.value ?: return
        val siblingMedia = when (current.type) {
            PreviewType.PHOTO -> _files.value.filter { it.isPhoto }
            PreviewType.VIDEO -> _files.value.filter { it.isVideo }
            PreviewType.AUDIO -> _audioPlaylist.value.ifEmpty { _files.value.filter { it.isAudio } }
            else -> emptyList()
        }
        if (siblingMedia.isEmpty()) return
        val idx = siblingMedia.indexOfFirst { it.path == current.file.path }
        val nextFile = if (idx in 0 until (siblingMedia.size - 1)) {
            siblingMedia[idx + 1]
        } else {
            siblingMedia.first()
        }
        openFilePreview(nextFile)
    }

    fun navigatePreviousMedia() {
        val current = _previewState.value ?: return
        val siblingMedia = when (current.type) {
            PreviewType.PHOTO -> _files.value.filter { it.isPhoto }
            PreviewType.VIDEO -> _files.value.filter { it.isVideo }
            PreviewType.AUDIO -> _audioPlaylist.value.ifEmpty { _files.value.filter { it.isAudio } }
            else -> emptyList()
        }
        if (siblingMedia.isEmpty()) return
        val idx = siblingMedia.indexOfFirst { it.path == current.file.path }
        val prevFile = if (idx > 0) {
            siblingMedia[idx - 1]
        } else {
            siblingMedia.last()
        }
        openFilePreview(prevFile)
    }

    // Audio Playback Controls
    fun toggleBackgroundPlay() {
        val newState = !_isBackgroundPlayEnabled.value
        _isBackgroundPlayEnabled.value = newState
        if (!newState) {
            // If background play turned off and preview dialog not open, immediately stop audio!
            if (_previewState.value?.type != PreviewType.AUDIO) {
                stopAudio()
            }
        }
        setNotification(
            if (newState) "Background Audio Playback: ON" else "Background Audio Playback: OFF (Audio stops when closed)"
        )
        syncAudioNotification()
    }

    private fun releaseMediaPlayerOnly() {
        audioProgressJob?.cancel()
        audioProgressJob = null
        try {
            audioEqualizer?.release()
            bassBoost?.release()
        } catch (_: Exception) {}
        audioEqualizer = null
        bassBoost = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun playAudioTrack(file: FileItem) {
        try {
            // Populate playlist so skipping works even when navigating elsewhere
            val currentFolderAudio = _files.value.filter { it.isAudio }
            if (currentFolderAudio.any { it.path == file.path }) {
                _audioPlaylist.value = currentFolderAudio
            } else if (_audioPlaylist.value.none { it.path == file.path }) {
                _audioPlaylist.value = listOf(file)
            }

            if (_currentAudio.value?.path == file.path && mediaPlayer != null) {
                if (!_isAudioPlaying.value) {
                    resumeAudio()
                }
                return
            }

            releaseMediaPlayerOnly()

            val mp = android.media.MediaPlayer().apply {
                setDataSource(getApplication(), android.net.Uri.fromFile(File(file.path)))
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        playbackParams = playbackParams.setSpeed(_audioSpeed.value)
                    } catch (_: Exception) {}
                }
                start()
                _audioDurationMs.value = duration
                _audioPositionMs.value = 0
                _isAudioPlaying.value = true
                _currentAudio.value = file

                setOnCompletionListener {
                    _isAudioPlaying.value = false
                    _audioPositionMs.value = 0
                    syncAudioNotification()
                    when (_audioRepeatMode.value) {
                        AudioRepeatMode.ONE -> {
                            restartAudio()
                        }
                        AudioRepeatMode.ALL -> {
                            playNextAudioTrack(wrapAround = true)
                        }
                        AudioRepeatMode.OFF -> {
                            playNextAudioTrack(wrapAround = false)
                        }
                    }
                }
            }
            mediaPlayer = mp

            try {
                val sessionId = mp.audioSessionId
                if (sessionId != 0) {
                    val eq = Equalizer(0, sessionId).apply { enabled = true }
                    audioEqualizer = eq
                    val bb = BassBoost(0, sessionId).apply { enabled = true }
                    bassBoost = bb
                    applyCurrentEqPreset()
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Failed to init Equalizer: ${e.message}")
            }

            startAudioProgressTracking()
            syncAudioNotification()
        } catch (e: Exception) {
            setNotification("Audio error: ${e.message}")
        }
    }

    fun setAudioEqPreset(preset: AudioEqPreset) {
        _audioEqPreset.value = preset
        applyCurrentEqPreset()
    }

    private fun applyCurrentEqPreset() {
        val eq = audioEqualizer ?: return
        val bb = bassBoost
        val preset = _audioEqPreset.value
        try {
            when (preset) {
                AudioEqPreset.FLAT -> {
                    bb?.setStrength(0.toShort())
                    val numBands = eq.numberOfBands
                    for (b in 0 until numBands) {
                        eq.setBandLevel(b.toShort(), 0)
                    }
                }
                AudioEqPreset.BASS_BOOST -> {
                    bb?.setStrength(800.toShort())
                    val numBands = eq.numberOfBands
                    if (numBands > 0) eq.setBandLevel(0, 500)
                    if (numBands > 1) eq.setBandLevel(1, 300)
                }
                AudioEqPreset.ROCK -> {
                    bb?.setStrength(400.toShort())
                    val numBands = eq.numberOfBands
                    if (numBands >= 5) {
                        eq.setBandLevel(0, 400)
                        eq.setBandLevel(1, 200)
                        eq.setBandLevel(2, (-100).toShort())
                        eq.setBandLevel(3, 200)
                        eq.setBandLevel(4, 500)
                    }
                }
                AudioEqPreset.POP -> {
                    bb?.setStrength(200.toShort())
                    val numBands = eq.numberOfBands
                    if (numBands >= 5) {
                        eq.setBandLevel(0, (-100).toShort())
                        eq.setBandLevel(1, 200)
                        eq.setBandLevel(2, 400)
                        eq.setBandLevel(3, 100)
                        eq.setBandLevel(4, (-100).toShort())
                    }
                }
                AudioEqPreset.JAZZ -> {
                    bb?.setStrength(200.toShort())
                    val numBands = eq.numberOfBands
                    if (numBands >= 5) {
                        eq.setBandLevel(0, 300)
                        eq.setBandLevel(1, 100)
                        eq.setBandLevel(2, (-100).toShort())
                        eq.setBandLevel(3, 100)
                        eq.setBandLevel(4, 300)
                    }
                }
                AudioEqPreset.VOCAL -> {
                    bb?.setStrength(0.toShort())
                    val numBands = eq.numberOfBands
                    if (numBands >= 5) {
                        eq.setBandLevel(0, (-300).toShort())
                        eq.setBandLevel(1, 100)
                        eq.setBandLevel(2, 500)
                        eq.setBandLevel(3, 300)
                        eq.setBandLevel(4, (-200).toShort())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error applying EQ preset: ${e.message}")
        }
    }

    fun toggleAudioPlayback() {
        if (_isAudioPlaying.value) {
            pauseAudio()
        } else {
            resumeAudio()
        }
    }

    fun pauseAudio() {
        try {
            mediaPlayer?.pause()
            _isAudioPlaying.value = false
            syncAudioNotification()
        } catch (_: Exception) {}
    }

    fun resumeAudio() {
        try {
            mediaPlayer?.start()
            _isAudioPlaying.value = true
            startAudioProgressTracking()
            syncAudioNotification()
        } catch (_: Exception) {}
    }

    fun seekAudio(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _audioPositionMs.value = positionMs
            syncAudioNotification()
        } catch (_: Exception) {}
    }

    fun restartAudio() {
        seekAudio(0)
        resumeAudio()
    }

    fun stopAudio() {
        releaseMediaPlayerOnly()
        _isAudioPlaying.value = false
        _audioPositionMs.value = 0
        _currentAudio.value = null
        audioNotificationManager.dismissNotification()
    }

    private fun startAudioProgressTracking() {
        audioProgressJob?.cancel()
        audioProgressJob = viewModelScope.launch {
            while (_isAudioPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _audioPositionMs.value = mp.currentPosition
                    }
                }
                delay(250)
            }
        }
    }

    fun playNextAudioTrack(wrapAround: Boolean = true) {
        val current = _currentAudio.value ?: _previewState.value?.file ?: return
        val audioList = _audioPlaylist.value.ifEmpty { _files.value.filter { it.isAudio } }
        if (audioList.isEmpty()) return

        val nextFile = if (_isShuffleEnabled.value) {
            val remaining = audioList.filter { it.path != current.path }
            if (remaining.isNotEmpty()) remaining.random() else audioList.first()
        } else {
            val idx = audioList.indexOfFirst { it.path == current.path }
            if (idx in 0 until audioList.size - 1) {
                audioList[idx + 1]
            } else if (wrapAround) {
                audioList.first()
            } else null
        }

        nextFile?.let { next ->
            playAudioTrack(next)
            if (_previewState.value?.type == PreviewType.AUDIO) {
                openFilePreview(next)
            }
        }
    }

    fun playPreviousAudioTrack() {
        val current = _currentAudio.value ?: _previewState.value?.file ?: return
        val audioList = _audioPlaylist.value.ifEmpty { _files.value.filter { it.isAudio } }
        if (audioList.isEmpty()) return
        val idx = audioList.indexOfFirst { it.path == current.path }
        val prev = if (idx > 0) audioList[idx - 1] else audioList.last()
        playAudioTrack(prev)
        if (_previewState.value?.type == PreviewType.AUDIO) {
            openFilePreview(prev)
        }
    }

    fun toggleAudioRepeatMode() {
        _audioRepeatMode.value = when (_audioRepeatMode.value) {
            AudioRepeatMode.OFF -> AudioRepeatMode.ALL
            AudioRepeatMode.ALL -> AudioRepeatMode.ONE
            AudioRepeatMode.ONE -> AudioRepeatMode.OFF
        }
        setNotification("Repeat Mode: ${_audioRepeatMode.value.name}")
    }

    fun toggleAudioShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        setNotification(if (_isShuffleEnabled.value) "Audio Shuffle: ON" else "Audio Shuffle: OFF")
    }

    fun setAudioSpeed(speed: Float) {
        _audioSpeed.value = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                }
            } catch (_: Exception) {}
        }
    }

    fun moveToTrash(file: FileItem) {
        viewModelScope.launch {
            val res = trashManager.moveToTrash(file.path)
            if (res.isSuccess) {
                setNotification("Moved to Trash: ${file.name}")
                loadCurrentDirectory()
            } else {
                setNotification("Trash error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun moveSelectedToTrash() {
        viewModelScope.launch {
            var count = 0
            _selectedPaths.value.forEach { p ->
                val f = File(p)
                if (f.exists() && trashManager.moveToTrash(p).isSuccess) {
                    count++
                }
            }
            _selectedPaths.value = emptySet()
            setNotification("Moved $count items to Trash")
            loadCurrentDirectory()
        }
    }

    fun moveToSafeFolder(file: FileItem) {
        viewModelScope.launch {
            val res = safeFolderManager.moveToSafeFolder(file.path)
            if (res.isSuccess) {
                setNotification("Secured in Safe Folder: ${file.name}")
                loadCurrentDirectory()
            } else {
                setNotification("Safe Folder error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun moveSelectedToSafeFolder() {
        viewModelScope.launch {
            val selected = _selectedPaths.value.toList()
            var count = 0
            for (p in selected) {
                val res = safeFolderManager.moveToSafeFolder(p)
                if (res.isSuccess) count++
            }
            _selectedPaths.value = emptySet()
            setNotification("Secured $count items in Safe Folder")
            loadCurrentDirectory()
        }
    }

    fun compressFilesToZip(files: List<FileItem>, zipName: String) {
        viewModelScope.launch {
            val target = File(_currentPath.value, if (zipName.endsWith(".zip")) zipName else "$zipName.zip")
            val sources = files.map { it.path }
            val res = archiveManager.createZip(sources, target)
            if (res.isSuccess) {
                setNotification("ZIP created: ${target.name}")
                loadCurrentDirectory()
            } else {
                setNotification("Compression failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun extractArchive(file: FileItem, targetDirectory: File? = null) {
        viewModelScope.launch {
            val dest = targetDirectory ?: File(_currentPath.value, file.name.substringBeforeLast("."))
            val res = archiveManager.extractZip(File(file.path), dest)
            if (res.isSuccess) {
                setNotification("Extracted to: ${dest.name}")
                loadCurrentDirectory()
            } else {
                setNotification("Extraction failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun togglePreviewHex() {
        val current = _previewState.value ?: return
        if (current.isHex) {
            openFilePreview(current.file, asHex = false)
        } else {
            openFilePreview(current.file, asHex = true)
        }
    }

    fun closePreview() {
        if (!_isBackgroundPlayEnabled.value) {
            stopAudio()
        }
        _previewState.value = null
    }

    fun toggleSelection(path: String) {
        val current = _selectedPaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedPaths.value = current
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun toggleBookmark(name: String, path: String, isSystem: Boolean) {
        viewModelScope.launch {
            val bookmarks = repository.allBookmarks.first()
            val existing = bookmarks.find { it.path == path }
            if (existing != null) {
                repository.removeBookmark(path)
                setNotification("Removed bookmark for $name")
            } else {
                repository.addBookmark(name, path, isSystem)
                setNotification("Bookmarked $name")
            }
        }
    }

    fun mountNewDrive(
        name: String,
        type: DriveType,
        host: String = "",
        port: Int = 0,
        shareName: String = "",
        username: String = "",
        pass: String = ""
    ) {
        viewModelScope.launch {
            val tempDrive = DriveEntity(
                name = name,
                type = type,
                path = "",
                host = host,
                port = port,
                shareName = shareName,
                username = username,
                passwordEncrypted = pass,
                isMounted = true,
                fsType = when (type) {
                    DriveType.NETWORK_SMB -> "cifs/smb3"
                    DriveType.NETWORK_FTP -> "ftpfs"
                    DriveType.CLOUD_GDRIVE -> "gdrive-rest"
                    DriveType.CLOUD_DROPBOX -> "dropbox-v2"
                    DriveType.CLOUD_ONEDRIVE -> "onedrive-graph"
                    else -> "ext4"
                }
            )

            val mappedDir = networkStorageManager.getOrCreateMappedDirectory(getApplication(), tempDrive)
            val finalDrive = tempDrive.copy(path = mappedDir.absolutePath)

            repository.addDrive(finalDrive)
            setNotification("Drive '$name' verified and mapped to explorer")
            selectDrive(finalDrive)
        }
    }

    fun unmountDrive(drive: DriveEntity) {
        viewModelScope.launch {
            repository.deleteDrive(drive)
            setNotification("Unmounted '${drive.name}'")
            if (_activeDrive.value?.id == drive.id) {
                navigateTo(Environment.getExternalStorageDirectory().absolutePath)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            currentFileObserver?.stopWatching()
        } catch (_: Exception) {}
        stopAudio()
        audioNotificationManager.release()
    }
}
