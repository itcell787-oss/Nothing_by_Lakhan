package com.example.ui.viewer

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.FileItem
import com.example.ui.components.NothingBadge
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite

enum class AudioRepeatMode {
    OFF, ALL, ONE
}

enum class AudioEqPreset(val label: String) {
    FLAT("FLAT"),
    BASS_BOOST("BASS+"),
    ROCK("ROCK"),
    POP("POP"),
    JAZZ("JAZZ"),
    VOCAL("VOCAL")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioPlayerView(
    file: FileItem,
    playlist: List<FileItem> = emptyList(),
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    isBackgroundPlayEnabled: Boolean,
    repeatMode: AudioRepeatMode = AudioRepeatMode.ALL,
    isShuffleEnabled: Boolean = false,
    preset: AudioEqPreset = AudioEqPreset.BASS_BOOST,
    onTogglePlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onRestart: () -> Unit,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onSelectTrack: ((FileItem) -> Unit)? = null,
    onToggleBackgroundPlay: () -> Unit,
    onToggleRepeatMode: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onEqPresetChange: (AudioEqPreset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var showQueueDrawer by remember { mutableStateOf(false) }
    var showEqDrawer by remember { mutableStateOf(false) }
    var currentPreset by remember(preset) { mutableStateOf(preset) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (showQueueDrawer) {
            // Queue / Playlist View
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NothingDark)
                    .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYLIST QUEUE (${playlist.size})",
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = { showQueueDrawer = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Back to player", tint = NothingWhite, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(playlist, key = { it.path }) { item ->
                        val isCurrent = item.path == file.path
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) NothingBlack else NothingDark)
                                .border(1.dp, if (isCurrent) NothingRed else NothingBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelectTrack?.invoke(item) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = if (isCurrent) NothingRed else NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.formattedSize,
                                    color = NothingGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            }
                            if (isCurrent && isPlaying) {
                                Icon(Icons.Default.GraphicEq, contentDescription = "Playing", tint = NothingRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        } else if (showEqDrawer) {
            // Equalizer Presets Drawer
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NothingDark)
                    .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUDIO EQUALIZER (PRESETS)",
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = { showEqDrawer = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Back to player", tint = NothingWhite, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AudioVisualizerBars(isPlaying = isPlaying)

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AudioEqPreset.values().forEach { preset ->
                        val isSelected = currentPreset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NothingRed else NothingBlack)
                                .border(1.dp, if (isSelected) NothingRed else NothingBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    currentPreset = preset
                                    onEqPresetChange(preset)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.label,
                                color = if (isSelected) NothingWhite else NothingLightGray,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Track Visual Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NothingDark)
                    .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Top Mini Toolbar: Queue & Equalizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Queue Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingBlack)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .clickable { showQueueDrawer = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = NothingWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "QUEUE", color = NothingWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        // EQ Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingBlack)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .clickable { showEqDrawer = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Equalizer, contentDescription = "EQ", tint = NothingWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "EQ: ${currentPreset.label}", color = NothingWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Waveform Equalizer Display
                    AudioVisualizerBars(isPlaying = isPlaying)

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = file.name,
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${file.formattedSize}  //  ${file.extension.uppercase()} AUDIO",
                        color = NothingGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Prominent Background Playback Button (as explicitly requested!)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isBackgroundPlayEnabled) NothingBlack else NothingDark)
                            .border(
                                1.dp,
                                if (isBackgroundPlayEnabled) NothingGreen else NothingBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onToggleBackgroundPlay() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("toggle_background_play_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Background Play",
                            tint = if (isBackgroundPlayEnabled) NothingGreen else NothingLightGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BACKGROUND PLAY",
                            color = NothingWhite,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NothingBadge(
                            text = if (isBackgroundPlayEnabled) "ON" else "OFF",
                            dotColor = if (isBackgroundPlayEnabled) NothingGreen else NothingGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time and seek bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(currentPositionMs),
                color = NothingLightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            Slider(
                value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                onValueChange = { frac ->
                    val targetMs = (frac * durationMs).toInt()
                    onSeek(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = NothingRed,
                    activeTrackColor = NothingWhite,
                    inactiveTrackColor = NothingBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Text(
                text = formatDuration(durationMs),
                color = NothingGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Music Player Controls Row: Shuffle, Previous, Play/Pause, Next, Repeat (Repeat 1 / All / Off)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Toggle Button
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.size(38.dp).testTag("audio_shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) NothingRed else NothingGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Previous Track & Volume Down Button (Tap: Previous song, Long Press: Volume -)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NothingDark)
                    .border(1.dp, NothingBorder, CircleShape)
                    .combinedClickable(
                        onClick = {
                            if (onPrevious != null) {
                                onPrevious.invoke()
                            } else {
                                Toast.makeText(context, "No previous track", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLongClick = {
                            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                            Toast.makeText(context, "Volume -", Toast.LENGTH_SHORT).show()
                        }
                    )
                    .testTag("audio_prev_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "-",
                        color = NothingRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track (Tap) / Volume - (Hold)",
                        tint = if (onPrevious != null) NothingWhite else NothingGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Play / Pause Main Button
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(NothingWhite)
                    .testTag("audio_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NothingBlack,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Next Track & Volume Up Button (Tap: Next song, Long Press: Volume +)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NothingDark)
                    .border(1.dp, NothingBorder, CircleShape)
                    .combinedClickable(
                        onClick = {
                            if (onNext != null) {
                                onNext.invoke()
                            } else {
                                Toast.makeText(context, "No next track", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLongClick = {
                            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                            Toast.makeText(context, "Volume +", Toast.LENGTH_SHORT).show()
                        }
                    )
                    .testTag("audio_next_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track (Tap) / Volume + (Hold)",
                        tint = if (onNext != null) NothingWhite else NothingGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "+",
                        color = NothingRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }

            // Repeat Mode Button: OFF -> ALL -> ONE (repeat1 as explicitly requested!)
            IconButton(
                onClick = onToggleRepeatMode,
                modifier = Modifier.size(38.dp).testTag("audio_repeat_mode_button")
            ) {
                Icon(
                    imageVector = if (repeatMode == AudioRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat: ${repeatMode.name}",
                    tint = if (repeatMode != AudioRepeatMode.OFF) NothingRed else NothingGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "[-] TAP: PREV | HOLD: VOL-    [+] TAP: NEXT | HOLD: VOL+",
            color = NothingGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Secondary Utility Bar: Speed Selector & Restart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Switcher
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingDark)
                    .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                        playbackSpeed = speeds[nextIdx]
                        onSpeedChange(playbackSpeed)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SPEED: ${playbackSpeed}x",
                    color = NothingWhite,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Restart Button
            IconButton(
                onClick = onRestart,
                modifier = Modifier.size(32.dp).testTag("audio_replay_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart Track",
                    tint = NothingLightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioVisualizerBars(isPlaying: Boolean) {
    val barCount = 18
    val transition = rememberInfiniteTransition(label = "audio_bars")

    val pulse1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1"
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p2"
    )
    val pulse3 by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p3"
    )

    Row(
        modifier = Modifier
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val factor = when (index % 3) {
                0 -> pulse1
                1 -> pulse2
                else -> pulse3
            }
            val heightDp = if (isPlaying) (10 + 44 * factor).dp else 10.dp
            val isAccent = index == barCount / 2 || index == barCount / 2 - 1

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(heightDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isAccent) NothingRed else NothingWhite)
            )
        }
    }
}

private fun formatDuration(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
