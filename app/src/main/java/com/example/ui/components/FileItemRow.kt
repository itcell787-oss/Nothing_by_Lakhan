package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.core.FileCategory
import com.example.core.FileItem
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
fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPreviewText: () -> Unit,
    onPreviewHex: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onBookmark: () -> Unit,
    onProperties: () -> Unit = {},
    onShare: (() -> Unit)? = null,
    onOpenWith: (() -> Unit)? = null,
    onToggleSelect: () -> Unit = {},
    onMoveToTrash: (() -> Unit)? = null,
    onMoveToSafeFolder: (() -> Unit)? = null,
    onCompressZip: (() -> Unit)? = null,
    onExtractZip: (() -> Unit)? = null,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val icon: ImageVector = when (item.category) {
        FileCategory.FOLDER -> Icons.Default.Folder
        FileCategory.DOCUMENT -> Icons.Default.Description
        FileCategory.CODE -> Icons.Default.Code
        FileCategory.IMAGE -> Icons.Default.Image
        FileCategory.AUDIO -> Icons.Default.AudioFile
        FileCategory.VIDEO -> Icons.Default.Movie
        FileCategory.ARCHIVE -> Icons.Default.Archive
        FileCategory.BINARY -> Icons.Default.Memory
        FileCategory.SYSTEM_CONFIG -> Icons.Default.Settings
        FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    val iconBg = if (isDarkTheme) {
        when {
            item.isDirectory -> NothingWhite
            item.category == FileCategory.BINARY -> NothingRed
            else -> NothingDark
        }
    } else {
        when {
            item.isDirectory -> Color(0xFFE5F0FF) // iOS Folder Blue background
            item.category == FileCategory.IMAGE -> Color(0xFFFFECE5)
            item.category == FileCategory.VIDEO -> Color(0xFFE8F5E9)
            item.category == FileCategory.AUDIO -> Color(0xFFFDE8E8)
            item.category == FileCategory.ARCHIVE -> Color(0xFFFFF4E5)
            item.category == FileCategory.BINARY -> Color(0xFFFFEBEE)
            else -> Color(0xFFF0F0F5)
        }
    }

    val iconTint = if (isDarkTheme) {
        when {
            item.isDirectory -> NothingBlack
            item.category == FileCategory.BINARY -> NothingWhite
            else -> NothingLightGray
        }
    } else {
        when {
            item.isDirectory -> Color(0xFF007AFF) // iOS Blue
            item.category == FileCategory.IMAGE -> Color(0xFFFF9500) // iOS Orange
            item.category == FileCategory.VIDEO -> Color(0xFF34C759) // iOS Green
            item.category == FileCategory.AUDIO -> Color(0xFFFF2D55) // iOS Pink/Red
            item.category == FileCategory.ARCHIVE -> Color(0xFFFF9500)
            item.category == FileCategory.BINARY -> Color(0xFFD32F2F)
            else -> Color(0xFF5856D6) // iOS Purple
        }
    }

    val rowBg = if (isDarkTheme) {
        if (isSelected) NothingDarkGray else NothingSurface
    } else {
        if (isSelected) Color(0xFFE5F0FF) else Color(0xFFFFFFFF) // iOS White grouped card
    }

    val rowBorder = if (isDarkTheme) {
        BorderStroke(1.dp, if (isSelected) NothingWhite else NothingBorder)
    } else {
        BorderStroke(1.dp, if (isSelected) Color(0xFF007AFF) else Color(0xFFE5E5EA))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .border(rowBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Checkbox Indicator
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        if (isDarkTheme) NothingRed else Color(0xFF007AFF)
                    } else {
                        if (isDarkTheme) NothingBlack else Color(0xFFF2F2F7)
                    }
                )
                .border(
                    1.dp,
                    if (isSelected) {
                        if (isDarkTheme) NothingRed else Color(0xFF007AFF)
                    } else {
                        if (isDarkTheme) NothingBorder else Color(0xFFD1D1D6)
                    },
                    CircleShape
                )
                .clickable { onToggleSelect() }
                .testTag("file_select_checkbox_${item.name}"),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // File category glyph
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg)
                .border(1.dp, if (isDarkTheme) NothingBorder else Color(0x223C3C43), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.category.name,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // File Details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    color = if (isDarkTheme) NothingWhite else Color(0xFF000000), // iOS Primary Label
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (item.isSymlink) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "->",
                        color = if (isDarkTheme) NothingRed else Color(0xFF007AFF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (item.isRemote) {
                    Spacer(modifier = Modifier.width(6.dp))
                    NothingBadge(
                        text = item.remoteDriveType ?: "REMOTE",
                        dotColor = if (isDarkTheme) NothingWhite else Color(0xFF007AFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.formattedSize,
                    color = if (isDarkTheme) NothingLightGray else Color(0xFF3C3C43),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )

                Text(
                    text = "  //  ",
                    color = if (isDarkTheme) NothingGray else Color(0xFFC7C7CC),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = item.permissions,
                    color = if (isDarkTheme) NothingGray else Color(0xFF8E8E93), // iOS Secondary Label
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )

                Text(
                    text = "  //  ",
                    color = if (isDarkTheme) NothingGray else Color(0xFFC7C7CC),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = item.formattedDate,
                    color = if (isDarkTheme) NothingGray else Color(0xFF8E8E93), // iOS Secondary Label
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // More options dropdown
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(36.dp).testTag("file_options_${item.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = if (isDarkTheme) NothingGray else Color(0xFF8E8E93),
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(NothingBlack)
                    .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
            ) {
                DropdownMenuItem(
                    text = { Text(if (isSelected) "Deselect Item" else "Select Item", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = {
                        menuExpanded = false
                        onToggleSelect()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Properties & Size", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = {
                        menuExpanded = false
                        onProperties()
                    }
                )

                if (onShare != null) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = NothingWhite, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share via...", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                }

                if (onOpenWith != null) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open With", tint = NothingWhite, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (item.isDirectory) "Open Folder with..." else "Open with...", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenWith()
                        }
                    )
                }

                if (!item.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("View Text Content", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onPreviewText()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Inspect Hex / Binary", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onPreviewHex()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Copy", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = {
                        menuExpanded = false
                        onCopy()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Cut", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = {
                        menuExpanded = false
                        onCut()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Rename", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )

                if (onMoveToTrash != null) {
                    DropdownMenuItem(
                        text = { Text("Move to Trash", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onMoveToTrash()
                        }
                    )
                }

                if (onMoveToSafeFolder != null) {
                    DropdownMenuItem(
                        text = { Text("Move to Safe Folder", color = NothingGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onMoveToSafeFolder()
                        }
                    )
                }

                if (onCompressZip != null) {
                    DropdownMenuItem(
                        text = { Text("Compress to ZIP", color = NothingLightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onCompressZip()
                        }
                    )
                }

                if (item.category == FileCategory.ARCHIVE && onExtractZip != null) {
                    DropdownMenuItem(
                        text = { Text("Extract ZIP Archive...", color = NothingLightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            menuExpanded = false
                            onExtractZip()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Bookmark Path", color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = {
                        menuExpanded = false
                        onBookmark()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Permanently Delete", color = NothingRed, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
