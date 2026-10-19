package com.riffbox.tv.music.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.riffbox.tv.music.model.MusicTrack
import com.riffbox.tv.player.RiffboxAudioService

/**
 * Robust Music Player Manager powered by:
 * 1. android-youtube-player v13.0.0 (official IFrame Player API with app package origin / referer)
 *    for all online YouTube music tracks.
 * 2. Google ExoPlayer for local offline downloaded files.
 *
 * Configured with background playback enabled, debounce on errors to prevent skip loops,
 * and seamless play/pause controls.
 */
class MusicPlayerManager(
    private val context: Context,
    private val youTubePlayerView: YouTubePlayerView,
    private val listener: MusicPlayerListener
) {

    interface MusicPlayerListener {
        fun onTrackChanged(track: MusicTrack)
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }

    companion object {
        private const val TAG = "MusicPlayerManager"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeYouTubePlayer: YouTubePlayer? = null
    private var isYtReady = false
    private var pendingTrack: MusicTrack? = null

    private var localExoPlayer: ExoPlayer? = null

    var currentTrack: MusicTrack? = null
        private set

    var currentQueue: List<MusicTrack> = emptyList()
        private set

    private var currentIndex: Int = -1

    private var _isPlaying: Boolean = false
    val isPlaying: Boolean get() = _isPlaying

    private var consecutiveErrorCount = 0
    private var lastErrorTime = 0L

    init {
        RiffboxAudioService.musicPlayerManagerInstance = this
        initLocalPlayer()
        initYouTubePlayer()
    }

    private fun initLocalPlayer() {
        localExoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    if (currentTrack?.isDownloaded == true) {
                        _isPlaying = playing
                        listener.onPlaybackStateChanged(playing)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (currentTrack?.isDownloaded == true && playbackState == Player.STATE_ENDED) {
                        playNext()
                    }
                }
            })
        }
    }

    private fun initYouTubePlayer() {
        try {
            // Enable background audio playback so WebView won't pause when minimized or screen-off
            youTubePlayerView.enableBackgroundPlayback(true)

            // Remove default library UI overlay to keep our clean Spotify design
            val emptyUi = View(context)
            youTubePlayerView.setCustomPlayerUi(emptyUi)

            val iFramePlayerOptions = IFramePlayerOptions.Builder(context)
                .controls(0)
                .autoplay(1)
                .rel(0)
                .ivLoadPolicy(3)
                .build()

            youTubePlayerView.initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        Log.i(TAG, "YouTube Player v13.0.0 engine is READY!")
                        activeYouTubePlayer = youTubePlayer
                        isYtReady = true

                        try {
                            youTubePlayer.unMute()
                            youTubePlayer.setVolume(100)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error unmuting: ${e.message}")
                        }

                        pendingTrack?.let { track ->
                            val t = track
                            pendingTrack = null
                            playTrack(t, currentQueue)
                        }
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        if (currentTrack?.isDownloaded != true) {
                            Log.d(TAG, "YouTube Player State: $state")
                            when (state) {
                                PlayerConstants.PlayerState.PLAYING -> {
                                    _isPlaying = true
                                    consecutiveErrorCount = 0
                                    listener.onPlaybackStateChanged(true)
                                }
                                PlayerConstants.PlayerState.PAUSED -> {
                                    _isPlaying = false
                                    listener.onPlaybackStateChanged(false)
                                }
                                PlayerConstants.PlayerState.ENDED -> {
                                    _isPlaying = false
                                    listener.onPlaybackStateChanged(false)
                                    playNext()
                                }
                                else -> {}
                            }
                        }
                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError
                    ) {
                        Log.e(TAG, "YouTube Player Error: $error on track ${currentTrack?.title}")
                        val now = System.currentTimeMillis()
                        if (now - lastErrorTime < 2000) {
                            // Debounce rapid error spam
                            return
                        }
                        lastErrorTime = now
                        consecutiveErrorCount++

                        if (consecutiveErrorCount >= 3) {
                            Log.w(TAG, "Too many errors ($consecutiveErrorCount). Halting auto-skip.")
                            _isPlaying = false
                            listener.onPlaybackStateChanged(false)
                            mainHandler.post {
                                Toast.makeText(
                                    context,
                                    "No se pudo reproducir este tema en este momento",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return
                        }

                        // Wait 2.5 seconds before attempting next track (avoid 1-second skipping loop)
                        mainHandler.postDelayed({
                            if (_isPlaying) {
                                playNext()
                            }
                        }, 2500)
                    }
                },
                true, // handleNetworkEvents
                iFramePlayerOptions
            )
        } catch (e: Exception) {
            Log.e(TAG, "initYouTubePlayer failed: ${e.message}", e)
        }
    }

    fun playTrack(track: MusicTrack, queue: List<MusicTrack> = listOf(track)) {
        currentQueue = queue
        currentIndex = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        currentTrack = track
        _isPlaying = true

        listener.onTrackChanged(track)
        listener.onPlaybackStateChanged(true)
        RiffboxAudioService.updateServiceForMusic(context, track, isPlaying = true)

        if (track.isDownloaded && track.localFilePath != null) {
            Log.d(TAG, "Playing offline track: ${track.localFilePath}")
            activeYouTubePlayer?.pause()
            localExoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                val mediaItem = MediaItem.fromUri(track.localFilePath!!)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        } else {
            Log.d(TAG, "Playing online track: ${track.id} (${track.title})")
            localExoPlayer?.pause()
            if (isYtReady && activeYouTubePlayer != null) {
                activeYouTubePlayer?.loadVideo(track.id, 0f)
                activeYouTubePlayer?.unMute()
                activeYouTubePlayer?.setVolume(100)
                activeYouTubePlayer?.play()
            } else {
                pendingTrack = track
            }
        }

        // Trigger auto-replenishment if nearing the end of queue
        if (currentQueue.size - (currentIndex + 1) <= 3) {
            onNeedMoreTracksListener?.invoke()
        }
    }

    var onNeedMoreTracksListener: (() -> Unit)? = null

    fun appendTracksToQueue(tracks: List<MusicTrack>) {
        if (tracks.isEmpty()) return
        val existingIds = currentQueue.map { it.id }.toSet()
        val uniqueNew = tracks.filterNot { it.id in existingIds }
        if (uniqueNew.isNotEmpty()) {
            currentQueue = currentQueue + uniqueNew
            Log.d(TAG, "Appended ${uniqueNew.size} new tracks to queue. Total: ${currentQueue.size}")
        }
    }

    fun togglePlayPause() {
        if (_isPlaying) pause() else resume()
    }

    fun resume() {
        _isPlaying = true
        listener.onPlaybackStateChanged(true)
        currentTrack?.let { RiffboxAudioService.updateServiceForMusic(context, it, isPlaying = true) }
        if (currentTrack?.isDownloaded == true) {
            localExoPlayer?.play()
        } else {
            activeYouTubePlayer?.play()
        }
    }

    fun pause() {
        _isPlaying = false
        listener.onPlaybackStateChanged(false)
        currentTrack?.let { RiffboxAudioService.updateServiceForMusic(context, it, isPlaying = false) }
        if (currentTrack?.isDownloaded == true) {
            localExoPlayer?.pause()
        } else {
            activeYouTubePlayer?.pause()
        }
    }

    fun stop() {
        _isPlaying = false
        listener.onPlaybackStateChanged(false)
        localExoPlayer?.stop()
        activeYouTubePlayer?.pause()
        RiffboxAudioService.stop(context)
    }

    fun playNext() {
        if (currentQueue.isEmpty()) return
        val nextIdx = currentIndex + 1
        if (nextIdx < currentQueue.size) {
            playTrack(currentQueue[nextIdx], currentQueue)
        } else {
            val loopIdx = 0
            playTrack(currentQueue[loopIdx], currentQueue)
        }

        // Trigger auto-replenishment when nearing queue end
        if (currentQueue.size - (currentIndex + 1) <= 3) {
            onNeedMoreTracksListener?.invoke()
        }
    }

    fun playPrevious() {
        if (currentQueue.isEmpty()) return
        val prevIdx = if (currentIndex - 1 < 0) currentQueue.size - 1 else currentIndex - 1
        playTrack(currentQueue[prevIdx], currentQueue)
    }

    fun release() {
        if (RiffboxAudioService.musicPlayerManagerInstance == this) {
            RiffboxAudioService.musicPlayerManagerInstance = null
        }
        RiffboxAudioService.stop(context)
        localExoPlayer?.release()
        localExoPlayer = null
        try {
            youTubePlayerView.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
