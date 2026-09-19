package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.ArchiveManager
import com.example.core.FileItem
import com.example.ui.components.NothingButton
import com.example.ui.components.NothingCard
import com.example.ui.components.nothingTextFieldColors
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CreateZipDialog(
    selectedFiles: List<FileItem>,
    destinationDirectory: String,
    onDismiss: () -> Unit,
    onZipCreated: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultZipName = if (selectedFiles.size == 1) {
        "${selectedFiles.first().name.substringBeforeLast('.')}.zip"
    } else {
        "Archive_${System.currentTimeMillis() / 1000}.zip"
    }
    var zipFileName by remember { mutableStateOf(defaultZipName) }
    var isCompressing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isCompressing) onDismiss() }) {
        NothingCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .testTag("create_zip_dialog"),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Archive, contentDescription = "ZIP", tint = NothingRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CREATE ZIP ARCHIVE",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    if (!isCompressing) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Compressing ${selectedFiles.size} item(s) to:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = zipFileName,
                    onValueChange = { zipFileName = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = nothingTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (isCompressing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = NothingRed, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "COMPRESSING ARCHIVE...", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        NothingButton(text = "CANCEL", onClick = onDismiss, isPrimary = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        NothingButton(
                            text = "COMPRESS",
                            onClick = {
                                val name = if (zipFileName.endsWith(".zip", ignoreCase = true)) zipFileName else "$zipFileName.zip"
                                val targetZip = File(destinationDirectory, name)
                                isCompressing = true
                                scope.launch {
                                    val result = ArchiveManager.createZip(selectedFiles.map { it.path }, targetZip)
                                    isCompressing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Archive created: $name", Toast.LENGTH_SHORT).show()
                                        onZipCreated(targetZip)
                                    } else {
                                        Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            isPrimary = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExtractZipDialog(
    archiveFile: FileItem,
    destinationDirectory: String,
    onDismiss: () -> Unit,
    onExtracted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultFolderName = archiveFile.name.substringBeforeLast('.')
    var extractFolderName by remember { mutableStateOf(defaultFolderName) }
    var isExtracting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isExtracting) onDismiss() }) {
        NothingCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .testTag("extract_zip_dialog"),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Unarchive, contentDescription = "Extract", tint = NothingRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXTRACT ZIP ARCHIVE",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    if (!isExtracting) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Archive: ${archiveFile.name} (${archiveFile.formattedSize})",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Extract into folder name:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = extractFolderName,
                    onValueChange = { extractFolderName = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = nothingTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (isExtracting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = NothingRed, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "EXTRACTING ARCHIVE...", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        NothingButton(text = "CANCEL", onClick = onDismiss, isPrimary = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        NothingButton(
                            text = "EXTRACT",
                            onClick = {
                                val targetDir = File(destinationDirectory, extractFolderName)
                                isExtracting = true
                                scope.launch {
                                    val result = ArchiveManager.extractZip(File(archiveFile.path), targetDir)
                                    isExtracting = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Extracted ${result.getOrNull()} files", Toast.LENGTH_SHORT).show()
                                        onExtracted()
                                    } else {
                                        Toast.makeText(context, "Extract error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            isPrimary = true
                        )
                    }
                }
            }
        }
    }
}
