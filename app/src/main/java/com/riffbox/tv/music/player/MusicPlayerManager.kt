package com.riffbox.tv.music.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.riffbox.tv.music.model.MusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pure ExoPlayer-based music player.
 *
 * Playback flow:
 *   1. playTrack() is called with a MusicTrack.
 *   2. For downloaded tracks → play local file directly.
 *   3. For online tracks → YoutubeStreamResolver.resolve(videoId) fetches the direct
 *      audio URL from YouTube InnerTube API (Android client), then ExoPlayer streams it.
 *
 * No WebView. No YouTube IFrame. No dependency on a browser engine.
 */
class MusicPlayerManager(
    private val context: Context,
    private val listener: MusicPlayerListener
) {

    interface MusicPlayerListener {
        fun onTrackChanged(track: MusicTrack)
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Backing field for isPlaying so inner anonymous classes can mutate it
    private var _isPlaying: Boolean = false
    val isPlaying: Boolean get() = _isPlaying

    var currentTrack: MusicTrack? = null
        private set

    var currentQueue: List<MusicTrack> = emptyList()
        private set

    private var currentIndex: Int = -1

    // Tracks being resolved so we don't double-resolve if user taps twice quickly
    private var resolvingVideoId: String? = null

    companion object {
        private const val TAG = "MusicPlayerManager"
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {

            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying = playing
                listener.onPlaybackStateChanged(playing)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "Track ended → advancing to next")
                        playNext()
                    }
                    Player.STATE_READY -> {
                        Log.d(TAG, "ExoPlayer STATE_READY — playing")
                    }
                    Player.STATE_BUFFERING -> {
                        Log.d(TAG, "Buffering…")
                    }
                    else -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.message} (cause: ${error.cause?.message})")
                // Wait a beat then try the next track
                mainHandler.postDelayed({ playNext() }, 1200)
            }
        })
    }

    fun playTrack(track: MusicTrack, queue: List<MusicTrack> = listOf(track)) {
        currentQueue = queue
        currentIndex = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        currentTrack = track
        _isPlaying = true

        listener.onTrackChanged(track)
        listener.onPlaybackStateChanged(true)

        if (track.isDownloaded && track.localFilePath != null) {
            Log.d(TAG, "Playing local file: ${track.localFilePath}")
            playUrl(track.localFilePath!!)
        } else {
            Log.d(TAG, "Resolving YouTube stream for: ${track.id} — ${track.title}")
            resolveAndPlay(track)
        }
    }

    private fun resolveAndPlay(track: MusicTrack) {
        if (resolvingVideoId == track.id) {
            Log.d(TAG, "Already resolving ${track.id}, skipping duplicate call")
            return
        }
        resolvingVideoId = track.id

        // Stop current playback while we resolve
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        scope.launch {
            val audioUrl = withContext(Dispatchers.IO) {
                YoutubeStreamResolver.resolve(track.id)
            }

            // Only apply if this track is still the current one
            if (currentTrack?.id != track.id) {
                Log.d(TAG, "Track changed during resolution, discarding result for ${track.id}")
                resolvingVideoId = null
                return@launch
            }

            resolvingVideoId = null

            if (audioUrl != null) {
                Log.i(TAG, "Resolved ${track.id} → playing stream")
                playUrl(audioUrl)
            } else {
                Log.e(TAG, "Could not resolve stream for ${track.id} — skipping to next")
                listener.onPlaybackStateChanged(false)
                mainHandler.postDelayed({ playNext() }, 800)
            }
        }
    }

    private fun playUrl(url: String) {
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            Log.e(TAG, "playUrl error: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_isPlaying) pause() else resume()
    }

    fun resume() {
        _isPlaying = true
        listener.onPlaybackStateChanged(true)
        exoPlayer.playWhenReady = true
        if (!exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_IDLE) {
            // Player was stopped completely, need to re-resolve
            currentTrack?.let { resolveAndPlay(it) }
        } else {
            exoPlayer.play()
        }
    }

    fun pause() {
        _isPlaying = false
        listener.onPlaybackStateChanged(false)
        exoPlayer.pause()
    }

    fun stop() {
        _isPlaying = false
        listener.onPlaybackStateChanged(false)
        exoPlayer.stop()
    }

    fun playNext() {
        if (currentQueue.isEmpty()) return
        val nextIdx = (currentIndex + 1) % currentQueue.size
        playTrack(currentQueue[nextIdx], currentQueue)
    }

    fun playPrevious() {
        if (currentQueue.isEmpty()) return
        val prevIdx = if (currentIndex - 1 < 0) currentQueue.size - 1 else currentIndex - 1
        playTrack(currentQueue[prevIdx], currentQueue)
    }

    fun release() {
        scope.cancel()
        exoPlayer.release()
    }
}
