package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DuplicateFileGroup
import com.example.core.FileItem
import com.example.core.PartitionInfo
import com.example.core.StorageCategoryBreakdown
import com.example.core.StorageCleanerManager
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun StorageCleanerView(
    cleanerManager: StorageCleanerManager,
    internalPartition: PartitionInfo? = null,
    onOpenFile: (FileItem) -> Unit = {},
    onClose: (() -> Unit)? = null,
    onNavigateToPath: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(true) }
    var junkFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var duplicateGroups by remember { mutableStateOf<List<DuplicateFileGroup>>(emptyList()) }
    var largeFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var oldDownloads by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var categoryBreakdown by remember { mutableStateOf<StorageCategoryBreakdown?>(null) }
    var freedJunkBytes by remember { mutableLongStateOf(0L) }
    var hasCleanedJunk by remember { mutableStateOf(false) }

    fun refreshAllScans() {
        scope.launch {
            isScanning = true
            val junk = cleanerManager.scanJunkFiles()
            junkFiles = junk
            val dups = cleanerManager.scanDuplicateFiles()
            duplicateGroups = dups
            val large = cleanerManager.scanLargeFiles(15 * 1024 * 1024L)
            largeFiles = large
            val old = cleanerManager.scanOldDownloads(7)
            oldDownloads = old
            val cats = cleanerManager.getCategoryBreakdown()
            categoryBreakdown = cats
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAllScans()
    }

    val totalJunkBytes = remember(junkFiles) { junkFiles.sumOf { it.size } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("storage_cleaner_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CLEAN & OPTIMIZE",
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "GOOGLE FILES STORAGE CLEANER ASSISTANT",
                        color = NothingGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { refreshAllScans() },
                        modifier = Modifier.size(32.dp).testTag("cleaner_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NothingWhite, modifier = Modifier.size(18.dp))
                    }
                    if (onClose != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp).testTag("cleaner_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = NothingGray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // 1. Storage Usage Meter Card
        item {
            internalPartition?.let { partition ->
                NothingCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NothingDark,
                    borderColor = NothingBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STORAGE USAGE",
                                color = NothingLightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${partition.usedPercent}% USED",
                                color = if (partition.usedPercent > 85) NothingRed else NothingGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { partition.usedPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (partition.usedPercent > 85) NothingRed else NothingWhite,
                            trackColor = NothingBorder
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Free: ${partition.formattedFree}",
                                color = NothingLightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Total: ${partition.formattedTotal}",
                                color = NothingGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        if (isScanning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NothingRed, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ANALYZING STORAGE & JUNK FILES...",
                            color = NothingLightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            // 2. Junk Files Cleaner Assistant Card
            item {
                NothingCard(
                    modifier = Modifier.fillMaxWidth().testTag("clean_junk_card"),
                    backgroundColor = NothingDark,
                    borderColor = if (junkFiles.isNotEmpty()) NothingRed else NothingBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NothingBlack)
                                        .border(1.dp, NothingRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CleaningServices,
                                        contentDescription = "Junk Cleaner",
                                        tint = NothingRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "JUNK & TEMP FILES",
                                        color = NothingWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${junkFiles.size} temporary logs, cache & dumps",
                                        color = NothingGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            NothingBadge(
                                text = if (hasCleanedJunk) "CLEANED" else formatBytes(totalJunkBytes),
                                dotColor = if (hasCleanedJunk) NothingGreen else NothingRed
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (hasCleanedJunk) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingBlack)
                                    .border(1.dp, NothingGreen, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Cleaned", tint = NothingGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SUCCESSFULLY FREED ${formatBytes(freedJunkBytes)} OF STORAGE!",
                                    color = NothingGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        } else if (junkFiles.isEmpty()) {
                            Text(
                                text = "NO JUNK FILES FOUND. STORAGE IS CLEAN!",
                                color = NothingGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            NothingButton(
                                text = "CLEAN ${formatBytes(totalJunkBytes)} JUNK",
                                onClick = {
                                    scope.launch {
                                        val res = cleanerManager.cleanJunkFiles(junkFiles)
                                        if (res.isSuccess) {
                                            freedJunkBytes = res.getOrDefault(0L)
                                            hasCleanedJunk = true
                                            junkFiles = emptyList()
                                            Toast.makeText(context, "Storage cleaned: ${formatBytes(freedJunkBytes)} freed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                isPrimary = true,
                                modifier = Modifier.fillMaxWidth().testTag("clean_junk_button")
                            )
                        }
                    }
                }
            }

            // 3. Duplicate Files Section
            if (duplicateGroups.isNotEmpty()) {
                item {
                    Text(
                        text = "DUPLICATE FILES (${duplicateGroups.size} SETS)",
                        color = NothingLightGray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                items(duplicateGroups) { group ->
                    DuplicateGroupCard(
                        group = group,
                        onDeleteFile = { item ->
                            val f = File(item.path)
                            if (f.delete()) {
                                Toast.makeText(context, "Deleted duplicate: ${item.name}", Toast.LENGTH_SHORT).show()
                                refreshAllScans()
                            }
                        },
                        onOpenFile = onOpenFile
                    )
                }
            }

            // 4. Large Files Section (> 15MB)
            if (largeFiles.isNotEmpty()) {
                item {
                    Text(
                        text = "LARGE FILES TO REVIEW (${largeFiles.size})",
                        color = NothingLightGray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                items(largeFiles.take(15)) { file ->
                    LargeFileRow(
                        file = file,
                        onOpen = { onOpenFile(file) },
                        onDelete = {
                            val f = File(file.path)
                            if (f.delete()) {
                                Toast.makeText(context, "Deleted: ${file.name}", Toast.LENGTH_SHORT).show()
                                refreshAllScans()
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateFileGroup,
    onDeleteFile: (FileItem) -> Unit,
    onOpenFile: (FileItem) -> Unit
) {
    NothingCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NothingDark,
        borderColor = NothingBorder,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicates", tint = NothingWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = group.items.firstOrNull()?.name ?: "Duplicate Group",
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                NothingBadge(text = group.formattedSize, dotColor = NothingRed)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingBlack)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).clickable { onOpenFile(item) }) {
                            Text(
                                text = if (index == 0) "ORIGINAL [KEEP]" else "DUPLICATE COPY",
                                color = if (index == 0) NothingGreen else NothingRed,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                            Text(
                                text = item.path,
                                color = NothingLightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (index > 0) {
                            IconButton(
                                onClick = { onDeleteFile(item) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Copy", tint = NothingRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeFileRow(
    file: FileItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NothingDark)
            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).clickable { onOpen() }) {
            Text(
                text = file.name,
                color = NothingWhite,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${file.formattedSize}  //  ${file.extension.uppercase()}  //  ${file.parentPath}",
                color = NothingGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NothingRed, modifier = Modifier.size(16.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var d = bytes.toDouble()
    var idx = 0
    while (d >= 1024.0 && idx < units.size - 1) {
        d /= 1024.0
        idx++
    }
    return String.format(java.util.Locale.US, "%.1f %s", d, units[idx])
}
