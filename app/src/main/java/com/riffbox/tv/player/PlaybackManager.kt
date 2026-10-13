package com.riffbox.tv.player

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.riffbox.tv.data.ChannelsRepository
import com.riffbox.tv.model.Channel

class PlaybackManager(
    private val context: Context,
    private val listener: PlaybackListener
) {

    interface PlaybackListener {
        fun onPlaybackStateChanged(isPlaying: Boolean, isLoading: Boolean)
        fun onChannelChanged(channel: Channel)
        fun onError(error: String)
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("riffbox_prefs", Context.MODE_PRIVATE)
    private var exoPlayer: ExoPlayer? = null
    var currentChannel: Channel? = null
        private set

    init {
        RiffboxAudioService.playbackManagerInstance = this
        setupPlayer()
    }

    private fun setupPlayer() {
        // High fidelity track selection: always pick highest bitrate stream
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredVideoMimeType("video/avc")
                    .setForceHighestSupportedBitrate(true)
            )
        }

        // Generous buffer control to eliminate micro-cuts
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs
                45000, // maxBufferMs
                2500,  // bufferForPlaybackMs
                5000   // bufferForPlaybackAfterRebufferMs
            )
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK) // Keeps CPU & network awake when screen is turned OFF!
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val isLoading = playbackState == Player.STATE_BUFFERING
                        val isPlaying = playbackState == Player.STATE_READY && playWhenReady
                        listener.onPlaybackStateChanged(isPlaying, isLoading)

                        // Update lockscreen notification
                        currentChannel?.let { ch ->
                            RiffboxAudioService.updateService(context, ch, isPlaying)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("PlaybackManager", "Player error on ${currentChannel?.name}: ${error.message}")
                        listener.onError("Error reproduciendo señal")
                    }
                })
            }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun playChannel(channel: Channel) {
        currentChannel = channel
        saveLastChannelId(channel.id)

        val mediaItem = MediaItem.fromUri(Uri.parse(channel.streamUrl))
        exoPlayer?.apply {
            stop()
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        listener.onChannelChanged(channel)
        RiffboxAudioService.updateService(context, channel, isPlaying = true)
    }

    fun pause() {
        exoPlayer?.pause()
        currentChannel?.let {
            RiffboxAudioService.updateService(context, it, isPlaying = false)
        }
    }

    fun resume() {
        exoPlayer?.play()
        currentChannel?.let {
            RiffboxAudioService.updateService(context, it, isPlaying = true)
        }
    }

    fun playNext() {
        val list = ChannelsRepository.channels
        val currentIndex = list.indexOfFirst { it.id == currentChannel?.id }
        val nextIndex = if (currentIndex >= 0 && currentIndex < list.size - 1) currentIndex + 1 else 0
        playChannel(list[nextIndex])
    }

    fun playPrevious() {
        val list = ChannelsRepository.channels
        val currentIndex = list.indexOfFirst { it.id == currentChannel?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playChannel(list[prevIndex])
    }

    fun getLastSavedChannel(): Channel {
        val lastId = prefs.getInt("last_channel_id", 1)
        return ChannelsRepository.channels.firstOrNull { it.id == lastId } ?: ChannelsRepository.channels.first()
    }

    private fun saveLastChannelId(channelId: Int) {
        prefs.edit().putInt("last_channel_id", channelId).apply()
    }

    fun release() {
        RiffboxAudioService.stop(context)
        RiffboxAudioService.playbackManagerInstance = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
