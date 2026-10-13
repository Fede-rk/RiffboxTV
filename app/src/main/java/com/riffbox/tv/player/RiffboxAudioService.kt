package com.riffbox.tv.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.riffbox.tv.MainActivity
import com.riffbox.tv.R
import com.riffbox.tv.model.Channel

class RiffboxAudioService : Service() {

    companion object {
        const val CHANNEL_ID = "riffbox_media_playback"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.riffbox.tv.ACTION_START"
        const val ACTION_PLAY = "com.riffbox.tv.ACTION_PLAY"
        const val ACTION_PAUSE = "com.riffbox.tv.ACTION_PAUSE"
        const val ACTION_NEXT = "com.riffbox.tv.ACTION_NEXT"
        const val ACTION_PREV = "com.riffbox.tv.ACTION_PREV"
        const val ACTION_STOP = "com.riffbox.tv.ACTION_STOP"

        var isServiceRunning = false
            private set

        var playbackManagerInstance: PlaybackManager? = null

        fun updateService(context: Context, channel: Channel, isPlaying: Boolean) {
            val intent = Intent(context, RiffboxAudioService::class.java).apply {
                action = ACTION_START
                putExtra("channel_name", channel.name)
                putExtra("channel_category", channel.category)
                putExtra("channel_badge", channel.badge)
                putExtra("is_playing", isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RiffboxAudioService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var mediaSession: MediaSessionCompat
    private var currentTitle = "Riffbox TV"
    private var currentCategory = "Música en Vivo"
    private var isPlayingState = true

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "RiffboxMediaSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playbackManagerInstance?.resume()
                }

                override fun onPause() {
                    playbackManagerInstance?.pause()
                }

                override fun onSkipToNext() {
                    playbackManagerInstance?.playNext()
                }

                override fun onSkipToPrevious() {
                    playbackManagerInstance?.playPrevious()
                }

                override fun onStop() {
                    stopSelf()
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentTitle = intent.getStringExtra("channel_name") ?: currentTitle
                currentCategory = intent.getStringExtra("channel_category") ?: currentCategory
                isPlayingState = intent.getBooleanExtra("is_playing", true)
                startForeground(NOTIFICATION_ID, buildMediaNotification())
            }
            ACTION_PLAY -> {
                playbackManagerInstance?.resume()
                isPlayingState = true
                startForeground(NOTIFICATION_ID, buildMediaNotification())
            }
            ACTION_PAUSE -> {
                playbackManagerInstance?.pause()
                isPlayingState = false
                startForeground(NOTIFICATION_ID, buildMediaNotification())
            }
            ACTION_NEXT -> {
                playbackManagerInstance?.playNext()
            }
            ACTION_PREV -> {
                playbackManagerInstance?.playPrevious()
            }
            ACTION_STOP -> {
                playbackManagerInstance?.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Riffbox Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles en pantalla de bloqueo y reproducción en segundo plano"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildMediaNotification(): android.app.Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Tap notification opens MainActivity
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val contentPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, flags)

        // Remote action buttons
        val prevIntent = Intent(this, RiffboxAudioService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(this, 1, prevIntent, flags)

        val playPauseIntent = Intent(this, RiffboxAudioService::class.java).apply {
            action = if (isPlayingState) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePending = PendingIntent.getService(this, 2, playPauseIntent, flags)

        val nextIntent = Intent(this, RiffboxAudioService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 3, nextIntent, flags)

        // Large icon for artwork
        val artwork = BitmapFactory.decodeResource(resources, R.drawable.ic_riffbox_logo)

        // Update MediaSession PlaybackState
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(
                if (isPlayingState) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1.0f
            )
        mediaSession.setPlaybackState(stateBuilder.build())

        val playPauseIcon = if (isPlayingState) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val playPauseText = if (isPlayingState) "Pausa" else "Reproducir"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(artwork)
            .setContentTitle(currentTitle)
            .setContentText("$currentCategory • Riffbox")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on Lockscreen!
            .setOngoing(isPlayingState)
            .addAction(R.drawable.ic_notif_prev, "Anterior", prevPending)
            .addAction(playPauseIcon, playPauseText, playPausePending)
            .addAction(R.drawable.ic_notif_next, "Siguiente", nextPending)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        mediaSession.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
