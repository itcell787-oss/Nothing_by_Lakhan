package com.example.ui.dialogs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DriveEntity
import com.example.data.model.DriveType
import com.example.ui.components.NothingBadge
import com.example.ui.components.NothingButton
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite

@Composable
fun MountedDrivesDialog(
    mountedDrives: List<DriveEntity>,
    onDismiss: () -> Unit,
    onOpenLocalHome: () -> Unit,
    onOpenSystemRoot: () -> Unit,
    onOpenDrive: (DriveEntity) -> Unit,
    onUnmountDrive: (DriveEntity) -> Unit,
    onOpenMountNewDialog: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("mounted_drives_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NothingDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NothingGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MOUNTED VOLUMES",
                            color = NothingWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_mounted_drives_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NothingLightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "ACTIVE LOCAL & NETWORK STORAGE MOUNT POINTS",
                    color = NothingGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Item 1: Internal Storage
                    item {
                        MountedVolumeCard(
                            title = "INTERNAL USER STORAGE",
                            subtitle = "/storage/emulated/0",
                            badgeText = "LOCAL FLASH",
                            badgeColor = NothingGreen,
                            statusText = "MOUNTED // R/W",
                            icon = Icons.Default.Storage,
                            onOpen = onOpenLocalHome,
                            onUnmount = null
                        )
                    }

                    // Item 2: System Root
                    item {
                        MountedVolumeCard(
                            title = "SYSTEM PARTITION",
                            subtitle = "/system",
                            badgeText = "ROOT EXT4",
                            badgeColor = NothingLightGray,
                            statusText = "MOUNTED // READ ONLY",
                            icon = Icons.Default.SdCard,
                            onOpen = onOpenSystemRoot,
                            onUnmount = null
                        )
                    }

                    // Additional Network & Cloud Drives (only displayed when actively mounted)
                    val activeMountedDrives = mountedDrives.filter { 
                        it.isMounted && 
                        it.type != DriveType.LOCAL_INTERNAL && 
                        it.type != DriveType.LOCAL_PARTITION && 
                        it.type != DriveType.LOCAL_SYSTEM_ROOT 
                    }
                    if (activeMountedDrives.isNotEmpty()) {
                        items(activeMountedDrives, key = { it.id }) { drive ->
                            MountedVolumeCard(
                                title = drive.name,
                                subtitle = if (drive.username.isNotEmpty()) drive.username else drive.host,
                                badgeText = drive.type.name.replace("CLOUD_", "").replace("NETWORK_", ""),
                                badgeColor = NothingGreen,
                                statusText = "ONLINE // ACTIVE",
                                icon = Icons.Default.CloudQueue,
                                onOpen = { onOpenDrive(drive) },
                                onUnmount = { onUnmountDrive(drive) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NothingButton(
                        text = "+ MOUNT NEW DRIVE",
                        onClick = {
                            onDismiss()
                            onOpenMountNewDialog()
                        },
                        isPrimary = true,
                        modifier = Modifier.testTag("open_mount_new_drive_btn")
                    )

                    NothingButton(
                        text = "CLOSE",
                        onClick = onDismiss,
                        isPrimary = false,
                        modifier = Modifier.testTag("close_dialog_btn")
                    )
                }
            }
        }
    }
}

@Composable
private fun MountedVolumeCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    statusText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onOpen: () -> Unit,
    onUnmount: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
            .clickable { onOpen() },
        color = NothingBlack
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NothingWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = NothingWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        NothingBadge(text = badgeText, dotColor = badgeColor)
                    }
                    Text(
                        text = subtitle,
                        color = NothingLightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Text(
                        text = statusText,
                        color = NothingGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                NothingButton(
                    text = "OPEN",
                    onClick = onOpen,
                    isPrimary = false,
                    modifier = Modifier.padding(end = 4.dp)
                )

                if (onUnmount != null) {
                    IconButton(
                        onClick = onUnmount,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Unmount",
                            tint = NothingRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
