package com.example.core

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class AudioPlaybackService : Service() {

    companion object {
        const val ACTION_START_FOREGROUND = "com.example.action.START_AUDIO_FGS"
        const val ACTION_STOP_FOREGROUND = "com.example.action.STOP_AUDIO_FGS"

        private var activeNotification: Notification? = null

        fun start(context: Context, notification: Notification) {
            activeNotification = notification
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_START_FOREGROUND
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_STOP_FOREGROUND
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                val notif = activeNotification
                if (notif != null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(
                                AudioNotificationManager.NOTIFICATION_ID,
                                notif,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            )
                        } else {
                            startForeground(AudioNotificationManager.NOTIFICATION_ID, notif)
                        }
                    } catch (_: Exception) {}
                }
            }
            ACTION_STOP_FOREGROUND -> {
                stopForegroundInternal()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopForegroundInternal() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopForegroundInternal()
        super.onDestroy()
    }
}
