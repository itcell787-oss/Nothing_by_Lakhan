package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.core.FileShareHelper
import com.example.ui.components.NothingBadge
import com.example.ui.components.NothingButton
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.PreviewDialogState
import com.example.ui.PreviewType
import com.example.ui.viewer.AudioEqPreset
import com.example.ui.viewer.AudioPlayerView
import com.example.ui.viewer.AudioRepeatMode
import com.example.ui.viewer.DocxViewer
import com.example.ui.viewer.ExcelViewer
import com.example.ui.viewer.PdfViewer
import com.example.ui.viewer.PhotoViewer
import com.example.ui.viewer.TextViewer
import com.example.ui.viewer.VideoPlayerView

@Composable
fun FilePreviewDialog(
    state: PreviewDialogState,
    onDismiss: () -> Unit,
    onToggleHex: (() -> Unit)? = null,
    onNextMedia: (() -> Unit)? = null,
    onPreviousMedia: (() -> Unit)? = null,
    isAudioPlaying: Boolean = false,
    audioPositionMs: Int = 0,
    audioDurationMs: Int = 0,
    isBackgroundPlayEnabled: Boolean = false,
    audioRepeatMode: AudioRepeatMode = AudioRepeatMode.ALL,
    isShuffleEnabled: Boolean = false,
    audioPlaylist: List<FileItem> = emptyList(),
    onToggleAudioPlayPause: () -> Unit = {},
    onSeekAudio: (Int) -> Unit = {},
    onRestartAudio: () -> Unit = {},
    onToggleBackgroundPlay: () -> Unit = {},
    onToggleAudioRepeatMode: () -> Unit = {},
    onToggleAudioShuffle: () -> Unit = {},
    onSelectAudioTrack: ((FileItem) -> Unit)? = null,
    onAudioSpeedChange: (Float) -> Unit = {},
    audioEqPreset: AudioEqPreset = AudioEqPreset.BASS_BOOST,
    onAudioEqPresetChange: (AudioEqPreset) -> Unit = {}
) {
    var isFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    var dragAccumulatorX by remember { mutableFloatStateOf(0f) }

    val isMedia = state.type == PreviewType.PHOTO || state.type == PreviewType.VIDEO || state.type == PreviewType.AUDIO

    val badgeLabel = when {
        state.isHex -> "HEX"
        state.type == PreviewType.PHOTO -> "PHOTO"
        state.type == PreviewType.TEXT -> "TEXT"
        state.type == PreviewType.VIDEO -> "VIDEO"
        state.type == PreviewType.AUDIO -> "AUDIO"
        state.type == PreviewType.PDF -> "PDF"
        state.type == PreviewType.DOCX -> "DOCX"
        state.type == PreviewType.EXCEL -> "XLS"
        else -> "INSPECT"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !isFullscreen,
            dismissOnBackPress = true
        )
    ) {
        val containerModifier = if (isFullscreen) {
            Modifier
                .fillMaxSize()
                .background(NothingBlack)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
        } else {
            Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 4.dp)
        }

        val isVideoFullscreen = isFullscreen && state.type == PreviewType.VIDEO

        Box(
            modifier = containerModifier
                .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 16.dp))
                .background(NothingBlack)
                .border(
                    if (isFullscreen) 0.dp else 1.dp,
                    if (isFullscreen) NothingBlack else NothingBorder,
                    RoundedCornerShape(if (isFullscreen) 0.dp else 16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isVideoFullscreen) 0.dp else if (isFullscreen) 10.dp else 14.dp)
            ) {
                // Header Row (Hidden in true video fullscreen since VideoPlayerView has its own full top-bar HUD)
                if (!isVideoFullscreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.file.name,
                                color = NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isFullscreen) 13.sp else 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            // Pagination indicator if folder contains multiple items
                            if (isMedia && state.totalCount > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                NothingBadge(
                                    text = "${state.currentIndex}/${state.totalCount}",
                                    dotColor = NothingWhite
                                )
                            }
                        }

                        Text(
                            text = "${state.file.formattedSize}  //  ${state.file.permissions}",
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action Controls Cluster: strictly bounded
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onToggleHex != null && !isMedia) {
                            NothingButton(
                                text = if (state.isHex) "RICH" else "HEX",
                                onClick = onToggleHex,
                                isPrimary = false,
                                modifier = Modifier.testTag("toggle_hex_button")
                            )
                        }

                        NothingBadge(
                            text = badgeLabel,
                            dotColor = if (state.isHex) NothingRed else NothingWhite
                        )

                        // Share File (WhatsApp, Nearby Share, etc.)
                        IconButton(
                            onClick = { FileShareHelper.shareFile(context, state.file) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingDark)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .testTag("preview_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share via",
                                tint = NothingWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Open with External App (Word, VLC, etc.)
                        IconButton(
                            onClick = { FileShareHelper.openFileWithExternalApp(context, state.file) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingDark)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .testTag("preview_open_with_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open with external app",
                                tint = NothingWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (state.type == PreviewType.HEX) {
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText(state.file.name, state.content))
                                    Toast.makeText(context, "Copied hex data", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingDark)
                                    .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                    .testTag("preview_hex_copy_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Hex Data",
                                    tint = NothingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Fullscreen Toggle Button - explicitly bounded so it stays inside
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingDark)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .testTag("toggle_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                tint = NothingWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close Dialog Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingDark)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .testTag("close_preview_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = NothingGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (!isVideoFullscreen) {
                    if (state.md5Checksum != null && !isFullscreen) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MD5: ${state.md5Checksum}",
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Content Viewport with Pure Swipe Gesture Navigation for Photos
                val isPhoto = state.type == PreviewType.PHOTO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (isPhoto) {
                                Modifier.pointerInput(state.file.path) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (dragAccumulatorX < -50f && state.hasNext) {
                                                onNextMedia?.invoke()
                                            } else if (dragAccumulatorX > 50f && state.hasPrevious) {
                                                onPreviousMedia?.invoke()
                                            }
                                            dragAccumulatorX = 0f
                                        },
                                        onDragCancel = { dragAccumulatorX = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            dragAccumulatorX += dragAmount
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    if (state.isHex) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NothingDark)
                                .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.content.ifEmpty { "[ Empty File Content ]" },
                                color = NothingLightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier
                                    .verticalScroll(verticalScroll)
                                    .horizontalScroll(horizontalScroll)
                                    .testTag("preview_content_text")
                            )
                        }
                    } else {
                        when (state.type) {
                            PreviewType.PHOTO -> {
                                PhotoViewer(
                                    file = state.file,
                                    onSwipeNext = if (state.totalCount > 1) onNextMedia else null,
                                    onSwipePrevious = if (state.totalCount > 1) onPreviousMedia else null
                                )
                            }
                            PreviewType.TEXT -> {
                                TextViewer(file = state.file, content = state.content)
                            }
                            PreviewType.VIDEO -> {
                                VideoPlayerView(
                                    file = state.file,
                                    isFullscreen = isFullscreen,
                                    onToggleFullscreen = { isFullscreen = !isFullscreen },
                                    onClose = onDismiss,
                                    onSwipeNext = if (state.totalCount > 1) onNextMedia else null,
                                    onSwipePrevious = if (state.totalCount > 1) onPreviousMedia else null
                                )
                            }
                            PreviewType.AUDIO -> {
                                AudioPlayerView(
                                    file = state.file,
                                    playlist = audioPlaylist,
                                    isPlaying = isAudioPlaying,
                                    currentPositionMs = audioPositionMs,
                                    durationMs = audioDurationMs,
                                    isBackgroundPlayEnabled = isBackgroundPlayEnabled,
                                    repeatMode = audioRepeatMode,
                                    isShuffleEnabled = isShuffleEnabled,
                                    onTogglePlayPause = onToggleAudioPlayPause,
                                    onSeek = onSeekAudio,
                                    onRestart = onRestartAudio,
                                    onNext = if (state.hasNext) onNextMedia else null,
                                    onPrevious = if (state.hasPrevious) onPreviousMedia else null,
                                    onSelectTrack = onSelectAudioTrack,
                                    onToggleBackgroundPlay = onToggleBackgroundPlay,
                                    onToggleRepeatMode = onToggleAudioRepeatMode,
                                    onToggleShuffle = onToggleAudioShuffle,
                                    onSpeedChange = onAudioSpeedChange,
                                    preset = audioEqPreset,
                                    onEqPresetChange = onAudioEqPresetChange
                                )
                            }
                            PreviewType.PDF -> {
                                PdfViewer(file = state.file)
                            }
                            PreviewType.DOCX -> {
                                DocxViewer(file = state.file)
                            }
                            PreviewType.EXCEL -> {
                                ExcelViewer(file = state.file, data = state.excelData)
                            }
                            PreviewType.HEX -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NothingDark)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    SelectionContainer {
                                        Text(
                                            text = state.content.ifEmpty { "[ Empty Binary File ]" },
                                            color = NothingLightGray,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            modifier = Modifier
                                                .verticalScroll(verticalScroll)
                                                .horizontalScroll(horizontalScroll)
                                                .testTag("preview_content_text")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer
                if (!isFullscreen) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMedia && state.totalCount > 1) {
                                "SWIPE LEFT / RIGHT TO NAVIGATE (${state.currentIndex}/${state.totalCount})"
                            } else {
                                "TOUCH FULLSCREEN ICON TOP-RIGHT TO EXPAND"
                            },
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                        NothingButton(
                            text = "CLOSE",
                            onClick = onDismiss,
                            isPrimary = true,
                            modifier = Modifier.testTag("close_preview_button")
                        )
                    }
                }
            }
        }
    }
}
