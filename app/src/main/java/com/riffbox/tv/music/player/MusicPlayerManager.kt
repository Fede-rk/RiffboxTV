package com.riffbox.tv.music.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.riffbox.tv.music.model.MusicTrack

class MusicPlayerManager(
    private val context: Context,
    private val youTubePlayerView: YouTubePlayerView,
    private val listener: MusicPlayerListener
) {

    interface MusicPlayerListener {
        fun onTrackChanged(track: MusicTrack)
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }

    private var activeYouTubePlayer: YouTubePlayer? = null
    private var isYtReady = false

    private var localExoPlayer: ExoPlayer? = null

    var currentTrack: MusicTrack? = null
        private set

    var currentQueue: List<MusicTrack> = emptyList()
        private set

    private var currentIndex: Int = -1
    var isPlaying: Boolean = false
        private set

    init {
        initLocalPlayer()
        initYouTubePlayer()
    }

    private fun initLocalPlayer() {
        localExoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    if (currentTrack?.isDownloaded == true) {
                        this@MusicPlayerManager.isPlaying = playing
                        listener.onPlaybackStateChanged(playing)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                        playNext()
                    }
                }
            })
        }
    }

    private fun initYouTubePlayer() {
        youTubePlayerView.enableAutomaticInitialization = false
        youTubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                activeYouTubePlayer = youTubePlayer
                isYtReady = true
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                if (currentTrack?.isDownloaded != true) {
                    when (state) {
                        PlayerConstants.PlayerState.PLAYING -> {
                            this@MusicPlayerManager.isPlaying = true
                            listener.onPlaybackStateChanged(true)
                        }
                        PlayerConstants.PlayerState.PAUSED -> {
                            this@MusicPlayerManager.isPlaying = false
                            listener.onPlaybackStateChanged(false)
                        }
                        PlayerConstants.PlayerState.ENDED -> {
                            this@MusicPlayerManager.isPlaying = false
                            playNext()
                        }
                        else -> {}
                    }
                }
            }
        })
    }

    fun playTrack(track: MusicTrack, queue: List<MusicTrack> = listOf(track)) {
        currentQueue = queue
        currentIndex = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        currentTrack = track
        isPlaying = true
        listener.onTrackChanged(track)
        listener.onPlaybackStateChanged(true)

        if (track.isDownloaded && track.localFilePath != null) {
            // Play local offline file with ExoPlayer
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
            // Play online track with YouTubePlayer
            localExoPlayer?.pause()
            if (isYtReady && activeYouTubePlayer != null) {
                activeYouTubePlayer?.loadVideo(track.id, 0f)
            } else {
                youTubePlayerView.postDelayed({
                    activeYouTubePlayer?.loadVideo(track.id, 0f)
                }, 800)
            }
        }
    }

    fun togglePlayPause() {
        if (isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun resume() {
        isPlaying = true
        listener.onPlaybackStateChanged(true)
        if (currentTrack?.isDownloaded == true) {
            localExoPlayer?.play()
        } else {
            activeYouTubePlayer?.play()
        }
    }

    fun pause() {
        isPlaying = false
        listener.onPlaybackStateChanged(false)
        localExoPlayer?.pause()
        activeYouTubePlayer?.pause()
    }

    fun stop() {
        isPlaying = false
        listener.onPlaybackStateChanged(false)
        localExoPlayer?.stop()
        activeYouTubePlayer?.pause()
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
        localExoPlayer?.release()
        localExoPlayer = null
        youTubePlayerView.release()
    }
}
