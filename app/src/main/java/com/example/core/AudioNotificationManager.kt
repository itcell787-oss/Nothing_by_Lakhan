package com.example.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.MainActivity

class AudioNotificationManager(
    private val context: Context,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onStop: () -> Unit
) {
    companion object {
        const val CHANNEL_ID = "audio_playback_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_PLAY_PAUSE = "com.example.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_STOP = "com.example.ACTION_STOP"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var mediaSession: MediaSession? = null
    private var isReceiverRegistered = false

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> {
                    onPlay()
                }
                ACTION_NEXT -> onNext()
                ACTION_PREV -> onPrevious()
                ACTION_STOP -> onStop()
            }
        }
    }

    init {
        createNotificationChannel()
        setupMediaSession()
        registerReceiver()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music player controls on lock screen and notification pane"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        try {
            mediaSession = MediaSession(context, "NothingMusicPlayerSession").apply {
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        this@AudioNotificationManager.onPlay()
                    }

                    override fun onPause() {
                        this@AudioNotificationManager.onPause()
                    }

                    override fun onSkipToNext() {
                        this@AudioNotificationManager.onNext()
                    }

                    override fun onSkipToPrevious() {
                        this@AudioNotificationManager.onPrevious()
                    }

                    override fun onStop() {
                        this@AudioNotificationManager.onStop()
                    }

                    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                        val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                        }
                        if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                            when (event.keyCode) {
                                KeyEvent.KEYCODE_MEDIA_NEXT,
                                KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                                    this@AudioNotificationManager.onNext()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                                KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                                    this@AudioNotificationManager.onPrevious()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                    this@AudioNotificationManager.onPlay()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    this@AudioNotificationManager.onPause()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                                KeyEvent.KEYCODE_HEADSETHOOK -> {
                                    this@AudioNotificationManager.onPlay()
                                    return true
                                }
                            }
                        }
                        return super.onMediaButtonEvent(mediaButtonIntent)
                    }
                })
            }
        } catch (_: Exception) {}
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PLAY_PAUSE)
                addAction(ACTION_NEXT)
                addAction(ACTION_PREV)
                addAction(ACTION_STOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(controlReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    fun updateNotification(
        file: FileItem?,
        isPlaying: Boolean,
        positionMs: Int,
        durationMs: Int,
        isBackgroundPlayEnabled: Boolean
    ) {
        // Notification is kept active as long as a track is active/playing
        if (file == null) {
            dismissNotification()
            return
        }

        try {
            // Update media session state & metadata for lockscreen and system UI
            mediaSession?.let { session ->
                session.isActive = true
                val actions = PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_SEEK_TO

                val playbackSpeed = if (isPlaying) 1.0f else 0f
                val state = PlaybackState.Builder()
                    .setActions(actions)
                    .setState(
                        if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                        positionMs.toLong(),
                        playbackSpeed
                    )
                    .build()
                session.setPlaybackState(state)

                val metadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, file.name)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "Nothing Audio Player")
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, "Local Drive")
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs.toLong())
                    .build()
                session.setMetadata(metadata)
            }

            // Build Intents
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val prevIntent = Intent(ACTION_PREV).setPackage(context.packageName)
            val prevPending = PendingIntent.getBroadcast(
                context, 1, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val playPauseIntent = Intent(ACTION_PLAY_PAUSE).setPackage(context.packageName)
            val playPausePending = PendingIntent.getBroadcast(
                context, 2, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextIntent = Intent(ACTION_NEXT).setPackage(context.packageName)
            val nextPending = PendingIntent.getBroadcast(
                context, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(ACTION_STOP).setPackage(context.packageName)
            val stopPending = PendingIntent.getBroadcast(
                context, 4, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Setup Android MediaStyle for interactive notification pane & lockscreen widget
            val mediaStyle = MediaStyle()
                .setShowActionsInCompactView(0, 1, 2) // Previous (0), Play/Pause (1), Next (2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopPending)

            mediaSession?.sessionToken?.let { token ->
                try {
                    val compatToken = MediaSessionCompat.Token.fromToken(token)
                    mediaStyle.setMediaSession(compatToken)
                } catch (_: Exception) {}
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(file.name)
                .setContentText(if (isPlaying) "Playing • Background Audio" else "Paused • Background Audio")
                .setSubText("Explorer Audio")
                .setContentIntent(openPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(isPlaying)
                .setAutoCancel(false)
                .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
                .addAction(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) "Pause" else "Play",
                    playPausePending
                )
                .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPending)
                .setStyle(mediaStyle)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)

            // Keep alive via AudioPlaybackService foreground service when playing
            if (isPlaying) {
                AudioPlaybackService.start(context, notification)
            }
        } catch (_: Exception) {}
    }

    fun dismissNotification() {
        try {
            AudioPlaybackService.stop(context)
            notificationManager.cancel(NOTIFICATION_ID)
            mediaSession?.isActive = false
        } catch (_: Exception) {}
    }

    fun release() {
        dismissNotification()
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(controlReceiver)
                isReceiverRegistered = false
            }
            mediaSession?.release()
            mediaSession = null
        } catch (_: Exception) {}
    }
}
