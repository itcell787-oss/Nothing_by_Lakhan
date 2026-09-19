package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.FileItem
import com.example.core.SafeFolderManager
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
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SafeFolderDialog(
    safeFolderManager: SafeFolderManager,
    currentFolderPath: String,
    onDismiss: () -> Unit,
    onOpenFile: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val isPinAlreadySet = remember { safeFolderManager.isPinSet() }
    var isSettingUpPin by remember { mutableStateOf(!isPinAlreadySet) }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    // PIN Change Flow States
    var isChangingPin by remember { mutableStateOf(false) }
    var changePinStep by remember { mutableStateOf(1) } // 1: Current PIN, 2: New PIN, 3: Confirm New PIN
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }

    var safeFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(false) }
    var isAddingItems by remember { mutableStateOf(false) }
    var itemPendingDelete by remember { mutableStateOf<FileItem?>(null) }

    fun refreshSafeFiles() {
        scope.launch {
            isLoadingFiles = true
            safeFiles = safeFolderManager.listSafeItems()
            isLoadingFiles = false
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            refreshSafeFiles()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NothingBlack.copy(alpha = if (isFullscreen) 1f else 0.85f))
                .padding(if (isFullscreen) 0.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            NothingCard(
                modifier = (if (isFullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 20.dp))
                    .border(if (isFullscreen) 0.dp else 1.dp, NothingBorder, RoundedCornerShape(if (isFullscreen) 0.dp else 20.dp))
                    .testTag("safe_folder_dialog"),
                backgroundColor = NothingDark
            ) {
                Column(
                    modifier = (if (isFullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                        .padding(if (isFullscreen) 16.dp else 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Safe Folder",
                                tint = if (isUnlocked) NothingGreen else NothingRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAFE FOLDER",
                                color = NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isFullscreen = !isFullscreen },
                                modifier = Modifier.size(28.dp).testTag("safe_folder_fullscreen_btn")
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                    tint = NothingGray
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = NothingGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isChangingPin) {
                        // Change PIN View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CHANGE VAULT PIN",
                                color = NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            TextButton(onClick = {
                                isChangingPin = false
                                pinError = null
                                oldPinInput = ""
                                newPinInput = ""
                                confirmNewPinInput = ""
                            }) {
                                Text("CANCEL", color = NothingRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (changePinStep) {
                                1 -> "STEP 1/3: ENTER CURRENT 4-DIGIT PIN"
                                2 -> "STEP 2/3: ENTER NEW 4-DIGIT PIN"
                                else -> "STEP 3/3: CONFIRM NEW 4-DIGIT PIN"
                            },
                            color = NothingLightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PIN Dots for Change PIN
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activePin = when (changePinStep) {
                                1 -> oldPinInput
                                2 -> newPinInput
                                else -> confirmNewPinInput
                            }
                            for (i in 0 until 4) {
                                val filled = i < activePin.length
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (filled) NothingRed else NothingDark)
                                        .border(1.5.dp, if (filled) NothingRed else NothingBorder, CircleShape)
                                )
                            }
                        }

                        if (pinError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = pinError ?: "",
                                color = NothingRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        NumericKeypad(
                            onDigit = { digit ->
                                pinError = null
                                when (changePinStep) {
                                    1 -> {
                                        if (oldPinInput.length < 4) {
                                            oldPinInput += digit
                                            if (oldPinInput.length == 4) {
                                                if (safeFolderManager.verifyPin(oldPinInput)) {
                                                    changePinStep = 2
                                                } else {
                                                    pinError = "Current PIN is incorrect"
                                                    oldPinInput = ""
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        if (newPinInput.length < 4) {
                                            newPinInput += digit
                                            if (newPinInput.length == 4) {
                                                if (newPinInput == oldPinInput) {
                                                    pinError = "New PIN must be different from current"
                                                    newPinInput = ""
                                                } else {
                                                    changePinStep = 3
                                                }
                                            }
                                        }
                                    }
                                    3 -> {
                                        if (confirmNewPinInput.length < 4) {
                                            confirmNewPinInput += digit
                                            if (confirmNewPinInput.length == 4) {
                                                if (confirmNewPinInput == newPinInput) {
                                                    safeFolderManager.setPin(newPinInput)
                                                    isChangingPin = false
                                                    isUnlocked = true
                                                    oldPinInput = ""
                                                    newPinInput = ""
                                                    confirmNewPinInput = ""
                                                    Toast.makeText(context, "PIN Changed Successfully!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    pinError = "New PINs do not match"
                                                    confirmNewPinInput = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onBackspace = {
                                pinError = null
                                when (changePinStep) {
                                    1 -> if (oldPinInput.isNotEmpty()) oldPinInput = oldPinInput.dropLast(1)
                                    2 -> {
                                        if (newPinInput.isNotEmpty()) newPinInput = newPinInput.dropLast(1)
                                        else changePinStep = 1
                                    }
                                    3 -> {
                                        if (confirmNewPinInput.isNotEmpty()) confirmNewPinInput = confirmNewPinInput.dropLast(1)
                                        else changePinStep = 2
                                    }
                                }
                            }
                        )
                    } else if (!isUnlocked) {
                        // PIN Entry View
                        Text(
                            text = if (isSettingUpPin) {
                                if (isConfirmingPin) "CONFIRM 4-DIGIT PIN" else "SET 4-DIGIT PIN"
                            } else {
                                "ENTER 4-DIGIT PIN TO ACCESS VAULT"
                            },
                            color = NothingLightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PIN Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activePin = if (isConfirmingPin) confirmPin else enteredPin
                            for (i in 0 until 4) {
                                val filled = i < activePin.length
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (filled) NothingRed else NothingDark)
                                        .border(1.5.dp, if (filled) NothingRed else NothingBorder, CircleShape)
                                )
                            }
                        }

                        if (pinError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = pinError ?: "",
                                color = NothingRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        NumericKeypad(
                            onDigit = { digit ->
                                pinError = null
                                if (isConfirmingPin) {
                                    if (confirmPin.length < 4) {
                                        confirmPin += digit
                                        if (confirmPin.length == 4) {
                                            if (confirmPin == enteredPin) {
                                                safeFolderManager.setPin(confirmPin)
                                                isUnlocked = true
                                                Toast.makeText(context, "PIN Set Successfully", Toast.LENGTH_SHORT).show()
                                            } else {
                                                pinError = "PINs do not match"
                                                confirmPin = ""
                                            }
                                        }
                                    }
                                } else {
                                    if (enteredPin.length < 4) {
                                        enteredPin += digit
                                        if (enteredPin.length == 4) {
                                            if (isSettingUpPin) {
                                                isConfirmingPin = true
                                            } else {
                                                if (safeFolderManager.verifyPin(enteredPin)) {
                                                    isUnlocked = true
                                                } else {
                                                    pinError = "Incorrect PIN"
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onBackspace = {
                                pinError = null
                                if (isConfirmingPin) {
                                    if (confirmPin.isNotEmpty()) {
                                        confirmPin = confirmPin.dropLast(1)
                                    } else {
                                        isConfirmingPin = false
                                    }
                                } else {
                                    if (enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                    }
                                }
                            }
                        )

                        if (!isSettingUpPin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    isChangingPin = true
                                    changePinStep = 1
                                    oldPinInput = ""
                                    newPinInput = ""
                                    confirmNewPinInput = ""
                                    pinError = null
                                }
                            ) {
                                Text(
                                    text = "CHANGE PIN",
                                    color = NothingLightGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else if (isAddingItems) {
                        // "Add Item to Safe Folder" file picker view
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT ITEM TO ENCRYPT",
                                color = NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = { isAddingItems = false }) {
                                Text("CANCEL", color = NothingRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = "Current folder: $currentFolderPath",
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val currentFolderFiles = remember(currentFolderPath) {
                            try {
                                val dir = File(currentFolderPath)
                                (dir.listFiles() ?: emptyArray()).map { FileItem.fromFile(it) }
                                    .sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }

                        if (currentFolderFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No files or folders in current directory to add.",
                                    color = NothingGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isFullscreen) Modifier.weight(1f) else Modifier.height(300.dp)),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(currentFolderFiles, key = { it.path }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NothingBlack)
                                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                scope.launch {
                                                    val res = safeFolderManager.moveToSafeFolder(item.path)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "Added '${item.name}' to Safe Folder", Toast.LENGTH_SHORT).show()
                                                        refreshSafeFiles()
                                                        isAddingItems = false
                                                    } else {
                                                        Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                                contentDescription = null,
                                                tint = if (item.isDirectory) NothingWhite else NothingLightGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    color = NothingWhite,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (item.isDirectory) "Directory" else item.formattedSize,
                                                    color = NothingGray,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "+ ADD",
                                            color = NothingGreen,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Unlocked Safe Folder Content View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${safeFiles.size} VAULT ITEMS",
                                color = NothingGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Add item button
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingBlack)
                                        .border(1.dp, NothingGreen, RoundedCornerShape(8.dp))
                                        .clickable { isAddingItems = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("safe_folder_add_items_button"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Item",
                                        tint = NothingGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ADD ITEM",
                                        color = NothingGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                NothingBadge(text = "ENCRYPTED", dotColor = NothingGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isLoadingFiles) {
                            Box(
                                modifier = Modifier.height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NothingRed, strokeWidth = 2.dp)
                            }
                        } else if (safeFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingBlack)
                                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = "Empty",
                                        tint = NothingGray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "SAFE FOLDER IS EMPTY",
                                        color = NothingWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap 'ADD ITEM' above or select any file/folder in Explorer",
                                        color = NothingGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isFullscreen) Modifier.weight(1f) else Modifier.height(320.dp)),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(safeFiles, key = { it.path }) { item ->
                                    val origPath = safeFolderManager.getOriginalLocation(item.name)
                                    SafeFileRow(
                                        item = item,
                                        originalLocation = origPath,
                                        onOpen = { onOpenFile(item) },
                                        onMoveToPreviousLocation = {
                                            scope.launch {
                                                val res = safeFolderManager.restoreToOriginalLocation(item.name, currentFolderPath)
                                                if (res.isSuccess) {
                                                    val target = origPath ?: currentFolderPath
                                                    Toast.makeText(context, "Restored to $target", Toast.LENGTH_SHORT).show()
                                                    refreshSafeFiles()
                                                } else {
                                                    Toast.makeText(context, "Restore failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onRemoveFromVault = {
                                            scope.launch {
                                                val res = safeFolderManager.moveOutOfSafeFolder(item.name, currentFolderPath)
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "Removed from vault to $currentFolderPath", Toast.LENGTH_SHORT).show()
                                                    refreshSafeFiles()
                                                } else {
                                                    Toast.makeText(context, "Remove failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onDelete = {
                                            itemPendingDelete = item
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NothingButton(
                                text = "CHANGE PIN",
                                onClick = {
                                    isChangingPin = true
                                    changePinStep = 1
                                    oldPinInput = ""
                                    newPinInput = ""
                                    confirmNewPinInput = ""
                                    pinError = null
                                },
                                isPrimary = false,
                                modifier = Modifier.weight(1f).testTag("safe_folder_change_pin_button")
                            )

                            NothingButton(
                                text = "LOCK VAULT",
                                onClick = {
                                    isUnlocked = false
                                    enteredPin = ""
                                    confirmPin = ""
                                },
                                isPrimary = true,
                                modifier = Modifier.weight(1f).testTag("safe_folder_lock_button")
                            )
                        }
                    }
                }
            }
        }
    }

    itemPendingDelete?.let { fileToDelete ->
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = {
                Text(
                    text = "PERMANENT DELETE",
                    color = NothingWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete '${fileToDelete.name}' from the Safe Folder? This cannot be undone.",
                    color = NothingLightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            safeFolderManager.deleteSafeItem(fileToDelete.name)
                            itemPendingDelete = null
                            refreshSafeFiles()
                            Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("DELETE", color = NothingRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDelete = null }) {
                    Text("CANCEL", color = NothingGray, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = NothingDark
        )
    }
}

@Composable
private fun SafeFileRow(
    item: FileItem,
    originalLocation: String?,
    onOpen: () -> Unit,
    onMoveToPreviousLocation: () -> Unit,
    onRemoveFromVault: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NothingBlack)
            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).clickable { onOpen() }) {
                    Text(
                        text = item.name,
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.isDirectory) "DIRECTORY // PROTECTED" else "${item.formattedSize}  //  ${item.extension.uppercase()}",
                        color = NothingGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    if (!originalLocation.isNullOrBlank()) {
                        Text(
                            text = "Previous: $originalLocation",
                            color = NothingGreen.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Open/Preview
                    IconButton(
                        onClick = onOpen,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Preview",
                            tint = NothingWhite,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Delete permanently
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete permanently",
                            tint = NothingRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dedicated Action Buttons for user intent:
            // "move to its previous location" & "remove from vault"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Move to Previous Location
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingDark)
                        .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { onMoveToPreviousLocation() }
                        .padding(vertical = 5.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Previous Location",
                        tint = NothingGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PREV LOCATION",
                        color = NothingGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Remove from Vault
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingDark)
                        .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { onRemoveFromVault() }
                        .padding(vertical = 5.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Remove From Vault",
                        tint = NothingLightGray,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "REMOVE VAULT",
                        color = NothingLightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in digits) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (item in row) {
                    when (item) {
                        "" -> Spacer(modifier = Modifier.size(54.dp))
                        "DEL" -> {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NothingDark)
                                    .border(1.dp, NothingBorder, CircleShape)
                                    .clickable { onBackspace() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = NothingWhite, modifier = Modifier.size(20.dp))
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NothingBlack)
                                    .border(1.dp, NothingBorder, CircleShape)
                                    .clickable { onDigit(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

