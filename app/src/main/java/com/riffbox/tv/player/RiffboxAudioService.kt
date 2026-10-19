package com.riffbox.tv.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.riffbox.tv.MainActivity
import com.riffbox.tv.R
import com.riffbox.tv.model.Channel
import com.riffbox.tv.music.model.MusicTrack
import com.riffbox.tv.music.player.MusicPlayerManager

class RiffboxAudioService : Service() {

    companion object {
        const val CHANNEL_ID = "riffbox_media_playback"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.riffbox.tv.ACTION_START"
        const val ACTION_START_MUSIC = "com.riffbox.tv.ACTION_START_MUSIC"
        const val ACTION_PLAY = "com.riffbox.tv.ACTION_PLAY"
        const val ACTION_PAUSE = "com.riffbox.tv.ACTION_PAUSE"
        const val ACTION_NEXT = "com.riffbox.tv.ACTION_NEXT"
        const val ACTION_PREV = "com.riffbox.tv.ACTION_PREV"
        const val ACTION_STOP = "com.riffbox.tv.ACTION_STOP"

        var isServiceRunning = false
            private set

        var playbackManagerInstance: PlaybackManager? = null
        var musicPlayerManagerInstance: MusicPlayerManager? = null
        var isMusicMode: Boolean = false

        fun updateService(context: Context, channel: Channel, isPlaying: Boolean) {
            isMusicMode = false
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

        fun updateServiceForMusic(context: Context, track: MusicTrack, isPlaying: Boolean) {
            isMusicMode = true
            val intent = Intent(context, RiffboxAudioService::class.java).apply {
                action = ACTION_START_MUSIC
                putExtra("track_title", track.title)
                putExtra("track_artist", track.artist)
                putExtra("track_album", track.album)
                putExtra("track_thumb", track.thumbnailUrl)
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
    private var currentTitle = "Riffbox"
    private var currentCategory = "Música"
    private var currentAlbum = "Riffbox"
    private var isPlayingState = true

    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume or transient duck
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                resumeInternal()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "RiffboxMediaSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumeInternal()
                }

                override fun onPause() {
                    pauseInternal()
                }

                override fun onSkipToNext() {
                    skipNextInternal()
                }

                override fun onSkipToPrevious() {
                    skipPrevInternal()
                }

                override fun onStop() {
                    stopInternal()
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                isMusicMode = false
                currentTitle = intent.getStringExtra("channel_name") ?: currentTitle
                currentCategory = intent.getStringExtra("channel_category") ?: currentCategory
                currentAlbum = "Riffbox TV"
                isPlayingState = intent.getBooleanExtra("is_playing", true)

                if (isPlayingState) {
                    requestAudioFocus()
                    acquireWakeLock()
                } else {
                    releaseWakeLock()
                }
                updateMediaMetadata()
                startForeground(NOTIFICATION_ID, buildMediaNotification())
            }
            ACTION_START_MUSIC -> {
                isMusicMode = true
                currentTitle = intent.getStringExtra("track_title") ?: currentTitle
                currentCategory = intent.getStringExtra("track_artist") ?: currentCategory
                currentAlbum = intent.getStringExtra("track_album")?.takeIf { it.isNotEmpty() } ?: "Riffbox"
                isPlayingState = intent.getBooleanExtra("is_playing", true)

                if (isPlayingState) {
                    requestAudioFocus()
                    acquireWakeLock()
                } else {
                    releaseWakeLock()
                }
                updateMediaMetadata()
                startForeground(NOTIFICATION_ID, buildMediaNotification())
            }
            ACTION_PLAY -> {
                resumeInternal()
            }
            ACTION_PAUSE -> {
                pauseInternal()
            }
            ACTION_NEXT -> {
                skipNextInternal()
            }
            ACTION_PREV -> {
                skipPrevInternal()
            }
            ACTION_STOP -> {
                stopInternal()
            }
        }
        return START_STICKY
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager).also { audioManager = it }
            ?: return false

        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Riffbox:AudioWakeLock")
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(2 * 60 * 60 * 1000L) // 2 hours
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMediaMetadata() {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentCategory)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentAlbum)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentCategory)
            .build()
        mediaSession.setMetadata(metadata)
        mediaSession.isActive = true
    }

    private fun resumeInternal() {
        requestAudioFocus()
        acquireWakeLock()
        isPlayingState = true
        if (isMusicMode) {
            musicPlayerManagerInstance?.resume()
        } else {
            playbackManagerInstance?.resume()
        }
        startForeground(NOTIFICATION_ID, buildMediaNotification())
    }

    private fun pauseInternal() {
        releaseWakeLock()
        isPlayingState = false
        if (isMusicMode) {
            musicPlayerManagerInstance?.pause()
        } else {
            playbackManagerInstance?.pause()
        }
        startForeground(NOTIFICATION_ID, buildMediaNotification())
    }

    private fun skipNextInternal() {
        if (isMusicMode) {
            musicPlayerManagerInstance?.playNext()
        } else {
            playbackManagerInstance?.playNext()
        }
    }

    private fun skipPrevInternal() {
        if (isMusicMode) {
            musicPlayerManagerInstance?.playPrevious()
        } else {
            playbackManagerInstance?.playPrevious()
        }
    }

    private fun stopInternal() {
        releaseWakeLock()
        abandonAudioFocus()
        isPlayingState = false
        if (isMusicMode) {
            musicPlayerManagerInstance?.stop()
        } else {
            playbackManagerInstance?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
        releaseWakeLock()
        abandonAudioFocus()
        mediaSession.isActive = false
        mediaSession.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
