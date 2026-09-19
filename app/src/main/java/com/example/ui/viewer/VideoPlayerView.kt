package com.example.ui.viewer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.FileItem
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

enum class VideoRepeatMode {
    OFF, ONE, ALL
}

enum class VideoAspectRatio(val label: String) {
    FIT("FIT"),
    FILL("FILL"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

enum class VideoDecoder(val label: String) {
    HW("HW"),
    HW_PLUS("HW+"),
    SW("SW")
}

@Composable
fun VideoPlayerView(
    file: FileItem,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onSwipeNext: (() -> Unit)? = null,
    onSwipePrevious: (() -> Unit)? = null
) {
    // Intercept hardware/gesture Back key when in fullscreen to minimize fullscreen first
    BackHandler(enabled = isFullscreen) {
        onToggleFullscreen?.invoke()
    }

    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    // MX Player States
    var isLocked by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var repeatMode by remember { mutableStateOf(VideoRepeatMode.ALL) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var aspectRatioMode by remember { mutableStateOf(VideoAspectRatio.FIT) }
    var decoderMode by remember { mutableStateOf(VideoDecoder.HW) }
    var isMuted by remember { mutableStateOf(false) }
    var previousVolume by remember { mutableIntStateOf(maxVolume / 2) }

    // Orientation state
    var isLandscape by remember { mutableStateOf(false) }

    // On-screen MX Gesture HUD state
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    var hudPercent by remember { mutableFloatStateOf(0f) }
    var showHud by remember { mutableStateOf(false) }
    var hudDismissTrigger by remember { mutableIntStateOf(0) }

    // Auto-hide controls timer (3.5 seconds)
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(3500)
            showControls = false
        }
    }

    // Hide HUD after 1.2s of inactivity
    LaunchedEffect(hudDismissTrigger) {
        if (hudDismissTrigger > 0 && showHud) {
            delay(1200)
            showHud = false
        }
    }

    // Progress update loop
    LaunchedEffect(isPlaying, file.path) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPositionMs = vv.currentPosition
                    durationMs = vv.duration
                }
            }
            delay(250)
        }
    }

    // Reset orientation on dispose
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Keep screen on while playing video
    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun triggerSeek(offsetMs: Int) {
        videoViewRef?.let { vv ->
            val target = (vv.currentPosition + offsetMs).coerceIn(0, durationMs.coerceAtLeast(1))
            vv.seekTo(target)
            currentPositionMs = target
            hudText = "${if (offsetMs > 0) "+" else ""}${offsetMs / 1000}s [${formatDuration(target)} / ${formatDuration(durationMs)}]"
            hudIcon = if (offsetMs > 0) Icons.Default.FastForward else Icons.Default.FastRewind
            hudPercent = if (durationMs > 0) target.toFloat() / durationMs.toFloat() else 0f
            showHud = true
            hudDismissTrigger++
        }
    }

    fun toggleMute() {
        audioManager?.let { am ->
            if (isMuted) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
                isMuted = false
                hudText = "Volume ${((previousVolume.toFloat() / maxVolume) * 100).toInt()}%"
                hudIcon = Icons.Default.VolumeUp
                hudPercent = previousVolume.toFloat() / maxVolume.toFloat()
            } else {
                previousVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                isMuted = true
                hudText = "Muted"
                hudIcon = Icons.Default.VolumeMute
                hudPercent = 0f
            }
            showHud = true
            hudDismissTrigger++
        }
    }

    fun toggleOrientation() {
        activity?.let { act ->
            isLandscape = !isLandscape
            act.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    fun cycleSpeed() {
        val nextSpeed = when (playbackSpeed) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            2.0f -> 0.5f
            0.5f -> 0.75f
            else -> 1.0f
        }
        playbackSpeed = nextSpeed
        mediaPlayerRef?.let { mp ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(nextSpeed)
                } catch (_: Exception) {}
            }
        }
        hudText = "${nextSpeed}x Speed"
        hudIcon = Icons.Default.Speed
        hudPercent = nextSpeed / 2f
        showHud = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 12.dp))
            .background(NothingBlack)
    ) {
        // Video View Container with responsive aspect ratio
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            val videoModifier = when (aspectRatioMode) {
                VideoAspectRatio.FIT -> Modifier.fillMaxSize()
                VideoAspectRatio.FILL -> Modifier.fillMaxSize()
                VideoAspectRatio.RATIO_16_9 -> Modifier
                    .aspectRatio(16f / 9f)
                    .align(Alignment.Center)
                VideoAspectRatio.RATIO_4_3 -> Modifier
                    .aspectRatio(4f / 3f)
                    .align(Alignment.Center)
            }

            key(file.path) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.fromFile(File(file.path)))
                            setOnPreparedListener { mp ->
                                isBuffering = false
                                mediaPlayerRef = mp
                                durationMs = mp.duration
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                    } catch (_: Exception) {}
                                }
                                mp.isLooping = (repeatMode == VideoRepeatMode.ONE)
                                start()
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                currentPositionMs = durationMs
                                when (repeatMode) {
                                    VideoRepeatMode.ONE -> {
                                        seekTo(0)
                                        start()
                                        isPlaying = true
                                    }
                                    VideoRepeatMode.ALL -> {
                                        if (onSwipeNext != null) {
                                            onSwipeNext.invoke()
                                        } else {
                                            seekTo(0)
                                            start()
                                            isPlaying = true
                                        }
                                    }
                                    VideoRepeatMode.OFF -> {
                                        onSwipeNext?.invoke()
                                    }
                                }
                            }
                            setOnErrorListener { _, _, _ ->
                                isBuffering = false
                                isPlaying = false
                                true
                            }
                            videoViewRef = this
                        }
                    },
                    modifier = videoModifier.testTag("video_player_surface")
                )
            }
        }

        // Loading / Buffering Spinner
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                color = NothingRed,
                strokeWidth = 3.dp
            )
        }

        // Gesture Overlay Layer (Double taps & drags for Volume, Brightness, Seek)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(file.path, isLocked) {
                    if (isLocked) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                    } else {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = { offset ->
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.35f) {
                                    // Rewind 10s
                                    triggerSeek(-10000)
                                } else if (offset.x > screenWidth * 0.65f) {
                                    // Forward 10s
                                    triggerSeek(10000)
                                } else {
                                    // Center double tap toggles play/pause
                                    videoViewRef?.let { vv ->
                                        if (vv.isPlaying) {
                                            vv.pause()
                                            isPlaying = false
                                        } else {
                                            vv.start()
                                            isPlaying = true
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                .then(
                    if (!isLocked) {
                        Modifier.pointerInput(file.path) {
                            var isHorizontal = false
                            var isLeftVertical = false

                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    isHorizontal = false
                                    isLeftVertical = startOffset.x < (size.width / 2f)
                                },
                                onDragEnd = {
                                    hudDismissTrigger++
                                },
                                onDragCancel = {
                                    hudDismissTrigger++
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (!isHorizontal && kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y) * 1.5f && kotlin.math.abs(dragAmount.x) > 10f) {
                                        isHorizontal = true
                                    }

                                    if (isHorizontal) {
                                        val seekDeltaMs = (dragAmount.x * 200).toInt()
                                        triggerSeek(seekDeltaMs)
                                    } else {
                                        if (isLeftVertical) {
                                            // Brightness adjust
                                            activity?.window?.let { win ->
                                                val lp = win.attributes
                                                val currentBrightness = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                                val newBrightness = (currentBrightness - (dragAmount.y / 400f)).coerceIn(0.05f, 1.0f)
                                                lp.screenBrightness = newBrightness
                                                win.attributes = lp
                                                hudText = "Brightness ${(newBrightness * 100).toInt()}%"
                                                hudIcon = Icons.Default.BrightnessMedium
                                                hudPercent = newBrightness
                                                showHud = true
                                                hudDismissTrigger++
                                            }
                                        } else {
                                            // Volume adjust
                                            audioManager?.let { am ->
                                                val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val step = if (dragAmount.y < 0) 1 else -1
                                                val newVol = (currentVol + step).coerceIn(0, maxVolume)
                                                am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                isMuted = (newVol == 0)
                                                hudText = "Volume ${((newVol.toFloat() / maxVolume) * 100).toInt()}%"
                                                hudIcon = if (newVol == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp
                                                hudPercent = newVol.toFloat() / maxVolume.toFloat()
                                                showHud = true
                                                hudDismissTrigger++
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
        )

        // Center Gestures HUD (Volume / Brightness / Seek indicator)
        AnimatedVisibility(
            visible = showHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NothingBlack.copy(alpha = 0.85f))
                    .border(1.dp, NothingRed, RoundedCornerShape(16.dp))
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    hudIcon?.let { icon ->
                        Icon(icon, contentDescription = "HUD", tint = NothingWhite, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = hudText ?: "",
                        color = NothingWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (hudPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(NothingDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(hudPercent.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .background(NothingRed)
                            )
                        }
                    }
                }
            }
        }

        // Screen Lock Indicator (When locked, floating unlock button)
        if (isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(NothingDark.copy(alpha = 0.85f))
                    .border(1.dp, NothingRed, CircleShape)
                    .clickable { isLocked = false; showControls = true }
                    .padding(12.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Screen Locked - Tap to Unlock",
                    tint = NothingRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // MX Player Controls Overlay
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar with gradient scrim
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(NothingBlack.copy(alpha = 0.9f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimize Fullscreen / Close button
                        if (isFullscreen || onClose != null) {
                            IconButton(
                                onClick = {
                                    if (isFullscreen) {
                                        onToggleFullscreen?.invoke()
                                    } else {
                                        onClose?.invoke()
                                    }
                                },
                                modifier = Modifier.size(28.dp).padding(end = 4.dp).testTag("video_top_minimize_btn")
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Close,
                                    contentDescription = if (isFullscreen) "Minimize Fullscreen" else "Close Video",
                                    tint = if (isFullscreen) NothingRed else NothingWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = file.name,
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // MX Decoder badge (HW / HW+ / SW)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NothingDark)
                                .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    decoderMode = when (decoderMode) {
                                        VideoDecoder.HW -> VideoDecoder.HW_PLUS
                                        VideoDecoder.HW_PLUS -> VideoDecoder.SW
                                        VideoDecoder.SW -> VideoDecoder.HW
                                    }
                                    hudText = "Decoder: ${decoderMode.label}"
                                    hudIcon = Icons.Default.Speed
                                    hudPercent = 0f
                                    showHud = true
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = decoderMode.label,
                                color = NothingGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Aspect Ratio Mode Switcher
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NothingDark)
                                .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    aspectRatioMode = when (aspectRatioMode) {
                                        VideoAspectRatio.FIT -> VideoAspectRatio.FILL
                                        VideoAspectRatio.FILL -> VideoAspectRatio.RATIO_16_9
                                        VideoAspectRatio.RATIO_16_9 -> VideoAspectRatio.RATIO_4_3
                                        VideoAspectRatio.RATIO_4_3 -> VideoAspectRatio.FIT
                                    }
                                    hudText = "Aspect Ratio: ${aspectRatioMode.label}"
                                    hudIcon = Icons.Default.AspectRatio
                                    hudPercent = 0f
                                    showHud = true
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = aspectRatioMode.label,
                                color = NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Playback Speed Selector
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NothingDark)
                                .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                .clickable { cycleSpeed() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = NothingWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Screen Lock Button
                        IconButton(
                            onClick = {
                                isLocked = true
                                showControls = false
                                hudText = "Touch Controls Locked"
                                hudIcon = Icons.Default.Lock
                                hudPercent = 0f
                                showHud = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Controls",
                                tint = NothingLightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Center Playback Buttons (Previous, Rewind 10s, Play/Pause, Forward 10s, Next)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Video
                    IconButton(
                        onClick = { onSwipePrevious?.invoke() },
                        enabled = onSwipePrevious != null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NothingBlack.copy(alpha = 0.65f))
                            .border(1.dp, if (onSwipePrevious != null) NothingBorder else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Video",
                            tint = if (onSwipePrevious != null) NothingWhite else NothingGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = { triggerSeek(-10000) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NothingBlack.copy(alpha = 0.65f))
                            .border(1.dp, NothingBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s", tint = NothingWhite, modifier = Modifier.size(22.dp))
                    }

                    // Main Play / Pause Circle
                    IconButton(
                        onClick = {
                            videoViewRef?.let { vv ->
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(NothingWhite)
                            .testTag("video_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NothingBlack,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Fast Forward 10s
                    IconButton(
                        onClick = { triggerSeek(10000) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NothingBlack.copy(alpha = 0.65f))
                            .border(1.dp, NothingBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Forward 10s", tint = NothingWhite, modifier = Modifier.size(22.dp))
                    }

                    // Next Video
                    IconButton(
                        onClick = { onSwipeNext?.invoke() },
                        enabled = onSwipeNext != null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NothingBlack.copy(alpha = 0.65f))
                            .border(1.dp, if (onSwipeNext != null) NothingBorder else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Video",
                            tint = if (onSwipeNext != null) NothingWhite else NothingGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom Controls Bar with Scrubber and MX Actions
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, NothingBlack.copy(alpha = 0.95f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Scrubber line: Position • Slider • Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(currentPositionMs),
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )

                        Slider(
                            value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                            onValueChange = { frac ->
                                val targetMs = (frac * durationMs).toInt()
                                videoViewRef?.seekTo(targetMs)
                                currentPositionMs = targetMs
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

                    // Secondary Bottom Action Row (Repeat, Mute, Orientation, Fullscreen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Repeat Mode Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingDark.copy(alpha = 0.8f))
                                .clickable {
                                    repeatMode = when (repeatMode) {
                                        VideoRepeatMode.OFF -> VideoRepeatMode.ALL
                                        VideoRepeatMode.ALL -> VideoRepeatMode.ONE
                                        VideoRepeatMode.ONE -> VideoRepeatMode.OFF
                                    }
                                    mediaPlayerRef?.isLooping = (repeatMode == VideoRepeatMode.ONE)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (repeatMode == VideoRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeatMode != VideoRepeatMode.OFF) NothingRed else NothingLightGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(repeatMode.name, color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Right actions: Mute, Screen Rotation, Fullscreen
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute button
                            IconButton(
                                onClick = { toggleMute() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) NothingRed else NothingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Force Rotate (Portrait / Landscape)
                            IconButton(
                                onClick = { toggleOrientation() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate Screen",
                                    tint = if (isLandscape) NothingRed else NothingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Fullscreen / Minimize Toggle
                            if (onToggleFullscreen != null) {
                                IconButton(
                                    onClick = { onToggleFullscreen() },
                                    modifier = Modifier.size(28.dp).testTag("video_fullscreen_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isFullscreen) "Minimize Fullscreen" else "Enter Fullscreen",
                                        tint = if (isFullscreen) NothingRed else NothingWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
