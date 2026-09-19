package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.core.FileItem
import com.example.core.FileShareHelper
import com.example.ui.components.NothingBadge
import com.example.ui.components.NothingButton
import com.example.ui.components.NothingCard
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilePropertiesDialog(
    file: FileItem,
    onDismiss: () -> Unit,
    onMoveToSafeFolder: ((FileItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val f = remember(file.path) { File(file.path) }

    var isCalculatingDirSize by remember { mutableStateOf(file.isDirectory) }
    var totalDirBytes by remember { mutableLongStateOf(file.size) }
    var totalFilesCount by remember { mutableStateOf(0) }
    var totalDirsCount by remember { mutableStateOf(0) }

    var md5Hash by remember { mutableStateOf<String?>(null) }
    var isCalculatingHash by remember { mutableStateOf(false) }

    // Recursive directory size calculation
    LaunchedEffect(file.path) {
        if (file.isDirectory) {
            isCalculatingDirSize = true
            withContext(Dispatchers.IO) {
                var bytes = 0L
                var files = 0
                var dirs = 0
                try {
                    f.walkTopDown().maxDepth(15).forEach { item ->
                        if (item.isFile) {
                            bytes += item.length()
                            files++
                        } else if (item.isDirectory && item != f) {
                            dirs++
                        }
                    }
                } catch (_: Exception) {}
                totalDirBytes = bytes
                totalFilesCount = files
                totalDirsCount = dirs
                isCalculatingDirSize = false
            }
        }
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val fileTypeDescription = remember(file) {
        if (file.isDirectory) "Directory / Folder"
        else "${file.category.name} (${file.extension.uppercase().ifEmpty { "BIN" }})"
    }

    val mimeType = remember(file) {
        FileShareHelper.getMimeType(file)
    }

    val lastModStr = remember(file.lastModified) {
        try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified))
        } catch (_: Exception) {
            file.formattedDate
        }
    }

    val accessStatus = remember(f) {
        buildString {
            if (f.canRead()) append("READ ")
            if (f.canWrite()) append("WRITE ")
            if (f.canExecute()) append("EXECUTE ")
            if (isEmpty()) append("RESTRICTED")
        }.trim()
    }

    fun copyAllProperties() {
        val sb = StringBuilder()
        sb.appendLine("=== FILE PROPERTIES ===")
        sb.appendLine("Name: ${file.name}")
        sb.appendLine("Type: $fileTypeDescription")
        if (!file.isDirectory) sb.appendLine("Extension: .${file.extension}")
        sb.appendLine("MIME Type: $mimeType")
        sb.appendLine("Absolute Path: ${file.path}")
        sb.appendLine("Parent: ${f.parent ?: "Root"}")
        if (file.isDirectory) {
            sb.appendLine("Total Size: ${formatFileSize(totalDirBytes)} ($totalDirBytes bytes)")
            sb.appendLine("Contents: $totalFilesCount files, $totalDirsCount folders")
        } else {
            sb.appendLine("Size: ${file.formattedSize} (${file.size} bytes)")
        }
        sb.appendLine("Permissions: ${file.permissions}")
        sb.appendLine("Access: $accessStatus")
        sb.appendLine("Last Modified: $lastModStr (${file.lastModified})")
        if (md5Hash != null) sb.appendLine("MD5 Checksum: $md5Hash")
        copyToClipboard(sb.toString(), "All Properties")
    }

    Dialog(onDismissRequest = onDismiss) {
        NothingCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("file_properties_dialog"),
            backgroundColor = NothingBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Properties",
                            tint = NothingRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (file.isDirectory) "DIRECTORY // PROPERTIES" else "FILE // PROPERTIES",
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    NothingBadge(
                        text = if (file.isDirectory) "FOLDER" else file.extension.uppercase().ifEmpty { "FILE" },
                        dotColor = if (file.isDirectory) NothingGreen else NothingWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name (Copyable)
                PropertyRow(
                    label = "NAME",
                    value = file.name,
                    onCopy = { copyToClipboard(file.name, "File Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // File Type / Category (Copyable)
                PropertyRow(
                    label = "FILE TYPE",
                    value = fileTypeDescription,
                    onCopy = { copyToClipboard(fileTypeDescription, "File Type") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!file.isDirectory && file.extension.isNotEmpty()) {
                    PropertyRow(
                        label = "EXTENSION",
                        value = ".${file.extension.lowercase()}",
                        onCopy = { copyToClipboard(".${file.extension.lowercase()}", "Extension") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // MIME Type (Copyable)
                PropertyRow(
                    label = "MIME TYPE",
                    value = mimeType,
                    onCopy = { copyToClipboard(mimeType, "MIME Type") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Absolute Path (Copyable)
                PropertyRow(
                    label = "ABSOLUTE PATH",
                    value = file.path,
                    onCopy = { copyToClipboard(file.path, "Absolute Path") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Parent Directory (Copyable)
                PropertyRow(
                    label = "PARENT DIRECTORY",
                    value = f.parent ?: "/",
                    onCopy = { copyToClipboard(f.parent ?: "/", "Parent Directory") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Size details
                if (file.isDirectory) {
                    val sizeFormatted = formatFileSize(totalDirBytes)
                    val sizeValue = if (isCalculatingDirSize) "Calculating..." else "$sizeFormatted ($totalDirBytes bytes)"
                    PropertyRow(
                        label = "TOTAL SIZE",
                        value = sizeValue,
                        onCopy = { copyToClipboard(sizeValue, "Total Size") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val contentsVal = if (isCalculatingDirSize) "Scanning..." else "$totalFilesCount files, $totalDirsCount folders"
                    PropertyRow(
                        label = "CONTENTS",
                        value = contentsVal,
                        onCopy = { copyToClipboard(contentsVal, "Contents") }
                    )
                } else {
                    val sizeVal = "${file.formattedSize} (${file.size} bytes)"
                    PropertyRow(
                        label = "FILE SIZE",
                        value = sizeVal,
                        onCopy = { copyToClipboard(sizeVal, "File Size") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Permissions & Status
                PropertyRow(
                    label = "PERMISSIONS",
                    value = "${file.permissions} (Unix)",
                    onCopy = { copyToClipboard(file.permissions, "Permissions") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                PropertyRow(
                    label = "ACCESS STATUS",
                    value = accessStatus,
                    onCopy = { copyToClipboard(accessStatus, "Access Status") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Date Modified
                PropertyRow(
                    label = "LAST MODIFIED",
                    value = "$lastModStr (${file.lastModified})",
                    onCopy = { copyToClipboard(lastModStr, "Last Modified") }
                )

                // Checksum Section for regular files
                if (!file.isDirectory && file.size < 50 * 1024 * 1024L) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CHECKSUM (MD5)",
                                    color = NothingGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                                if (md5Hash == null && !isCalculatingHash) {
                                    Text(
                                        text = "CALCULATE",
                                        color = NothingRed,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                isCalculatingHash = true
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        val md = MessageDigest.getInstance("MD5")
                                                        FileInputStream(f).use { fis ->
                                                            val buffer = ByteArray(8192)
                                                            var bytesRead: Int
                                                            while (fis.read(buffer).also { bytesRead = it } != -1) {
                                                                md.update(buffer, 0, bytesRead)
                                                            }
                                                        }
                                                        val digest = md.digest().joinToString("") { "%02x".format(it) }
                                                        withContext(Dispatchers.Main) {
                                                            md5Hash = digest
                                                            isCalculatingHash = false
                                                        }
                                                    } catch (_: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            md5Hash = "Failed to calculate"
                                                            isCalculatingHash = false
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(2.dp)
                                    )
                                }
                            }

                            if (isCalculatingHash) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = NothingWhite,
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Hashing stream...",
                                        color = NothingLightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else if (md5Hash != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = md5Hash ?: "",
                                        color = NothingWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { copyToClipboard(md5Hash ?: "", "MD5 Hash") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Hash",
                                            tint = NothingGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Copy All Details Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingDark)
                        .border(1.dp, NothingRed, RoundedCornerShape(8.dp))
                        .clickable { copyAllProperties() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy All", tint = NothingRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY ALL DETAILS", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Share & Open With Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                FileShareHelper.shareFile(context, file)
                            }
                            .padding(vertical = 9.dp)
                            .testTag("properties_share_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = NothingWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHARE", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingDark)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                FileShareHelper.openFileWithExternalApp(context, file)
                            }
                            .padding(vertical = 9.dp)
                            .testTag("properties_open_with_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open With", tint = NothingWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OPEN WITH", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMoveToSafeFolder != null) {
                        NothingButton(
                            text = "MOVE TO SAFE FOLDER",
                            onClick = {
                                onMoveToSafeFolder(file)
                                onDismiss()
                            },
                            isPrimary = false,
                            modifier = Modifier.testTag("properties_move_safe_folder")
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    NothingButton(
                        text = "CLOSE",
                        onClick = onDismiss,
                        isPrimary = true,
                        modifier = Modifier.testTag("close_properties_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NothingDark)
            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
            .then(
                if (onCopy != null) Modifier.clickable { onCopy() } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = NothingGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = value,
                    color = NothingWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (onCopy != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $label",
                        tint = NothingLightGray,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}
