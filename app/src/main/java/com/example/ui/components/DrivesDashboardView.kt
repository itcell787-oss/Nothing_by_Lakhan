package com.example.ui.components

import android.os.Environment
import android.os.StatFs
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.PartitionInfo
import com.example.data.model.DriveEntity
import com.example.data.model.DriveType
import com.example.vpn.VpnManager
import com.example.vpn.VpnStatus
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite

@Composable
fun DrivesDashboardView(
    partitions: List<PartitionInfo>,
    allDrives: List<DriveEntity>,
    onSelectDrive: (DriveEntity) -> Unit,
    onNavigateTo: (String) -> Unit,
    onOpenMountDialog: () -> Unit,
    onUnmountDrive: (DriveEntity) -> Unit,
    onOpenSafeFolder: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenStorageCleaner: () -> Unit,
    onOpenVpn: () -> Unit = {},
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NothingDark else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkTheme) NothingBorder else Color(0xFFE5E5EA)
    val textPrimary = if (isDarkTheme) NothingWhite else Color(0xFF000000)
    val textSecondary = if (isDarkTheme) NothingGray else Color(0xFF8E8E93)
    val textHighlight = if (isDarkTheme) NothingLightGray else Color(0xFF1C1C1E)

    val vpnStatus by VpnManager.status.collectAsState()
    val isVpnConnected = vpnStatus == VpnStatus.CONNECTED

    val extDir = Environment.getExternalStorageDirectory()
    val internalStorage = partitions.find { it.path.contains("emulated") || it.path.contains("storage") }
        ?: partitions.firstOrNull()
        ?: run {
            try {
                val stat = StatFs(extDir.path)
                val blockSize = stat.blockSizeLong
                val total = blockSize * stat.blockCountLong
                val free = blockSize * stat.freeBlocksLong
                val available = blockSize * stat.availableBlocksLong
                PartitionInfo(
                    name = "Internal Storage",
                    path = extDir.absolutePath,
                    totalBytes = total,
                    freeBytes = free,
                    availableBytes = available,
                    isReadOnly = false,
                    fsType = "f2fs/ext4"
                )
            } catch (_: Throwable) {
                null
            }
        }

    // Filter physical removable drives (SD Card, USB OTG)
    val physicalRemovableDrives = partitions.filter {
        it.isRemovable || (!it.path.contains("emulated") && it.path.startsWith("/storage/"))
    }

    // Filter mounted remote network/cloud drives
    val mountedRemoteDrives = allDrives.filter {
        it.type != DriveType.LOCAL_INTERNAL &&
        it.type != DriveType.LOCAL_SYSTEM_ROOT &&
        it.type != DriveType.LOCAL_PARTITION
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("drives_home_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STORAGE DASHBOARD",
                            color = textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "PHYSICAL DRIVES & GOOGLE FILES TOOLS",
                            color = textSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    NothingBadge(
                        text = "${1 + physicalRemovableDrives.size} PHYSICAL",
                        dotColor = if (isDarkTheme) NothingGreen else Color(0xFF34C759)
                    )
                }
            }
        }

        // 1. Google Files Quick Tools Bar (Safe Folder, Trash, Storage Cleaner)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Storage Cleaner
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                        .clickable { onOpenStorageCleaner() }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .testTag("dashboard_cleaner_tool"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = "Clean",
                            tint = if (isDarkTheme) NothingRed else Color(0xFF007AFF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "CLEAN", color = textPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "Storage", color = textSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Safe Folder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                        .clickable { onOpenSafeFolder() }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .testTag("dashboard_safe_folder_tool"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Safe Folder",
                            tint = if (isDarkTheme) NothingGreen else Color(0xFF34C759),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "SAFE FOLDER", color = textPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "PIN Vault", color = textSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Trash / Recycle Bin
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                        .clickable { onOpenTrash() }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .testTag("dashboard_trash_tool"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Trash",
                            tint = if (isDarkTheme) NothingLightGray else Color(0xFFFF3B30),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "TRASH", color = textPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "Recycle", color = textSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // VPN Tunnel (OpenVPN / Sophos)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(
                            1.dp,
                            if (isVpnConnected) NothingGreen else cardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onOpenVpn() }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .testTag("dashboard_vpn_tool"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "VPN Tunnel",
                                tint = if (isVpnConnected) NothingGreen else (if (isDarkTheme) NothingRed else Color(0xFF007AFF)),
                                modifier = Modifier.size(22.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isVpnConnected) NothingGreen else NothingRed)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVpnConnected) "VPN: ON" else "VPN",
                            color = if (isVpnConnected) NothingGreen else textPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isVpnConnected) "Connected" else "Tunnel",
                            color = if (isVpnConnected) NothingGreen else textSecondary,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 2. Primary Physical Internal Storage Card
        item {
            val extStoragePath = Environment.getExternalStorageDirectory().absolutePath
            NothingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTo(extStoragePath) }
                    .testTag("home_drive_internal"),
                backgroundColor = cardBg,
                borderColor = if (isDarkTheme) NothingWhite.copy(alpha = 0.4f) else cardBorder,
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkTheme) NothingBlack else Color(0xFFE5F0FF))
                                    .border(1.dp, if (isDarkTheme) NothingBorder else Color(0xFF007AFF).copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Internal Storage",
                                    tint = if (isDarkTheme) NothingWhite else Color(0xFF007AFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "INTERNAL STORAGE",
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "$extStoragePath  //  ${internalStorage?.fsType ?: "f2fs"}",
                                    color = textSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        NothingBadge(
                            text = "PRIMARY PHYSICAL",
                            dotColor = if (isDarkTheme) NothingGreen else Color(0xFF34C759)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    NothingSegmentedBar(
                        percentage = internalStorage?.usedPercentage ?: 40,
                        segments = 24,
                        activeColor = if (isDarkTheme) NothingWhite else Color(0xFF007AFF),
                        inactiveColor = cardBorder
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${internalStorage?.formattedUsed ?: "0 B"} USED",
                            color = textSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${internalStorage?.formattedFree ?: "0 B"} FREE OF ${internalStorage?.formattedTotal ?: "0 B"}",
                            color = textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick directories
                    Text(
                        text = "QUICK DIRECTORIES:",
                        color = textSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickFolderChip("DOWNLOADS", Icons.Default.Folder, "$extStoragePath/Download", isDarkTheme, onNavigateTo)
                        QuickFolderChip("DOCUMENTS", Icons.Default.FolderSpecial, "$extStoragePath/Documents", isDarkTheme, onNavigateTo)
                        QuickFolderChip("PICTURES", Icons.Default.Image, "$extStoragePath/Pictures", isDarkTheme, onNavigateTo)
                        QuickFolderChip("MUSIC", Icons.Default.Headphones, "$extStoragePath/Music", isDarkTheme, onNavigateTo)
                        QuickFolderChip("MOVIES", Icons.Default.Movie, "$extStoragePath/Movies", isDarkTheme, onNavigateTo)
                    }
                }
            }
        }

        // 3. Secondary Physically Available Removable Storage (SD Card / USB OTG)
        if (physicalRemovableDrives.isNotEmpty()) {
            item {
                Text(
                    text = "REMOVABLE PHYSICAL DRIVES (${physicalRemovableDrives.size})",
                    color = textHighlight,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            items(physicalRemovableDrives, key = { it.path }) { partition ->
                RemovableDriveCard(
                    partition = partition,
                    isDarkTheme = isDarkTheme,
                    onNavigate = onNavigateTo
                )
            }
        }

        // 4. Physical System Partitions (Root & System Core)
        item {
            Text(
                text = "PHYSICAL SYSTEM FLASH PARTITIONS",
                color = textHighlight,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SystemVolumeCard(
                    title = "ROOT (/)",
                    subtitle = "System Core",
                    path = "/",
                    icon = Icons.Default.Storage,
                    isDarkTheme = isDarkTheme,
                    onNavigate = onNavigateTo,
                    modifier = Modifier.weight(1f)
                )

                SystemVolumeCard(
                    title = "SYSTEM (/system)",
                    subtitle = "Android OS",
                    path = "/system",
                    icon = Icons.Default.Security,
                    isDarkTheme = isDarkTheme,
                    onNavigate = onNavigateTo,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Mounted Remote Drives (Visible ONLY if remote storage is mounted)
        if (mountedRemoteDrives.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MOUNTED REMOTE DRIVES (${mountedRemoteDrives.size})",
                        color = textHighlight,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    NothingButton(
                        text = "+ MOUNT DRIVE",
                        onClick = onOpenMountDialog,
                        isPrimary = true,
                        modifier = Modifier.testTag("home_add_mount_btn")
                    )
                }
            }

            items(mountedRemoteDrives, key = { it.id }) { drive ->
                MountedDriveCard(
                    drive = drive,
                    isDarkTheme = isDarkTheme,
                    onOpen = { onSelectDrive(drive) },
                    onUnmount = { onUnmountDrive(drive) }
                )
            }
        } else {
            // Unobtrusive "+ MOUNT REMOTE STORAGE" option
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable { onOpenMountDialog() }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = "Mount Remote Storage",
                            tint = if (isDarkTheme) NothingGray else Color(0xFF007AFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ MOUNT REMOTE STORAGE (SMB / FTP / CLOUD)",
                            color = textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RemovableDriveCard(
    partition: PartitionInfo,
    isDarkTheme: Boolean = true,
    onNavigate: (String) -> Unit
) {
    val isUsb = partition.name.contains("usb", ignoreCase = true)
    val cardBg = if (isDarkTheme) NothingDark else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkTheme) NothingGreen.copy(alpha = 0.6f) else Color(0xFFE5E5EA)
    val textPrimary = if (isDarkTheme) NothingWhite else Color(0xFF000000)
    val textSecondary = if (isDarkTheme) NothingGray else Color(0xFF8E8E93)

    NothingCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(partition.path) }
            .testTag("removable_drive_${partition.name.lowercase()}"),
        backgroundColor = cardBg,
        borderColor = cardBorder,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) NothingBlack else Color(0xFFE8F5E9))
                        .border(1.dp, if (isDarkTheme) NothingGreen else Color(0xFF34C759).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUsb) Icons.Default.Usb else Icons.Default.SdCard,
                        contentDescription = partition.name,
                        tint = if (isDarkTheme) NothingGreen else Color(0xFF34C759),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = partition.name.uppercase(),
                        color = textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${partition.path}  //  ${partition.formattedFree} free of ${partition.formattedTotal}",
                        color = textSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NothingBadge(text = "${partition.usedPercent}% USED", dotColor = if (isDarkTheme) NothingGreen else Color(0xFF34C759))
        }
    }
}

@Composable
private fun QuickFolderChip(
    name: String,
    icon: ImageVector,
    path: String,
    isDarkTheme: Boolean = true,
    onNavigate: (String) -> Unit
) {
    val chipBg = if (isDarkTheme) NothingBlack else Color(0xFFF2F2F7)
    val chipBorder = if (isDarkTheme) NothingBorder else Color(0xFFE5E5EA)
    val iconTint = if (isDarkTheme) NothingWhite else Color(0xFF007AFF)
    val textClr = if (isDarkTheme) NothingWhite else Color(0xFF1C1C1E)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(chipBg)
            .border(1.dp, chipBorder, RoundedCornerShape(10.dp))
            .clickable { onNavigate(path) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = name, tint = iconTint, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                color = textClr,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MountedDriveCard(
    drive: DriveEntity,
    isDarkTheme: Boolean = true,
    onOpen: () -> Unit,
    onUnmount: () -> Unit
) {
    val driveIcon = when (drive.type) {
        DriveType.CLOUD_GDRIVE, DriveType.CLOUD_DROPBOX, DriveType.CLOUD_ONEDRIVE -> Icons.Default.Cloud
        DriveType.NETWORK_SMB -> Icons.Default.FolderShared
        DriveType.NETWORK_FTP -> Icons.Default.Dns
        else -> Icons.Default.Storage
    }

    val typeLabel = when (drive.type) {
        DriveType.CLOUD_GDRIVE -> "GOOGLE DRIVE"
        DriveType.CLOUD_DROPBOX -> "DROPBOX"
        DriveType.CLOUD_ONEDRIVE -> "ONEDRIVE"
        DriveType.NETWORK_SMB -> "SMB / CIFS"
        DriveType.NETWORK_FTP -> "FTP"
        else -> drive.type.name
    }

    val cardBg = if (isDarkTheme) NothingDark else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkTheme) NothingBorder else Color(0xFFE5E5EA)
    val textPrimary = if (isDarkTheme) NothingWhite else Color(0xFF000000)
    val textSecondary = if (isDarkTheme) NothingGray else Color(0xFF8E8E93)

    NothingCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("mounted_drive_item_${drive.name.lowercase().replace(" ", "_")}"),
        backgroundColor = cardBg,
        borderColor = cardBorder,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) NothingBlack else Color(0xFFE5F0FF))
                        .border(1.dp, if (isDarkTheme) NothingBorder else Color(0xFF007AFF).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = driveIcon,
                        contentDescription = drive.name,
                        tint = if (isDarkTheme) NothingWhite else Color(0xFF007AFF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = drive.name,
                            color = textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        NothingBadge(text = typeLabel, dotColor = if (isDarkTheme) NothingRed else Color(0xFF007AFF))
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = drive.path,
                        color = textSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NothingButton(
                text = "UNMOUNT",
                onClick = onUnmount,
                isPrimary = false,
                modifier = Modifier.testTag("unmount_drive_${drive.id}")
            )
        }
    }
}

@Composable
private fun SystemVolumeCard(
    title: String,
    subtitle: String,
    path: String,
    icon: ImageVector,
    isDarkTheme: Boolean = true,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NothingDark else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkTheme) NothingBorder else Color(0xFFE5E5EA)
    val textPrimary = if (isDarkTheme) NothingWhite else Color(0xFF000000)
    val textSecondary = if (isDarkTheme) NothingGray else Color(0xFF8E8E93)

    NothingCard(
        modifier = modifier
            .clickable { onNavigate(path) }
            .testTag("system_volume_${title.lowercase().take(4)}"),
        backgroundColor = cardBg,
        borderColor = cardBorder,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDarkTheme) NothingBlack else Color(0xFFF2F2F7))
                    .border(1.dp, if (isDarkTheme) NothingBorder else Color(0xFFE5E5EA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isDarkTheme) NothingLightGray else Color(0xFF007AFF),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    color = textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = subtitle,
                    color = textSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
    }
}
