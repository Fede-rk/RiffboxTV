package com.riffbox.tv.music.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.riffbox.tv.music.model.MusicTrack

class MusicPlayerManager(
    private val context: Context,
    private val webView: WebView,
    private val listener: MusicPlayerListener
) {

    interface MusicPlayerListener {
        fun onTrackChanged(track: MusicTrack)
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }

    private var localExoPlayer: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var currentTrack: MusicTrack? = null
        private set

    var currentQueue: List<MusicTrack> = emptyList()
        private set

    private var currentIndex: Int = -1
    var isPlaying: Boolean = false
        private set

    private var isYtPlayerReady = false
    private var pendingTrackToPlay: MusicTrack? = null
    private var lastErrorTime = 0L

    init {
        initLocalPlayer()
        initWebView()
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onPlayerReady() {
                mainHandler.post {
                    Log.d("MusicPlayer", "YouTube IFrame Engine initialized and ready!")
                    isYtPlayerReady = true
                    pendingTrackToPlay?.let { track ->
                        val t = track
                        pendingTrackToPlay = null
                        playTrack(t, currentQueue)
                    }
                }
            }

            @JavascriptInterface
            fun onState(playing: Boolean) {
                mainHandler.post {
                    this@MusicPlayerManager.isPlaying = playing
                    listener.onPlaybackStateChanged(playing)
                }
            }

            @JavascriptInterface
            fun onEnded() {
                mainHandler.post {
                    playNext()
                }
            }

            @JavascriptInterface
            fun onError(code: Int) {
                mainHandler.post {
                    Log.e("MusicPlayer", "YouTube IFrame player error: $code")
                    val now = System.currentTimeMillis()
                    if (now - lastErrorTime > 1500) {
                        lastErrorTime = now
                        playNext()
                    }
                }
            }
        }, "RiffboxBridge")

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                    #player { width: 100%; height: 100%; }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script src="https://www.youtube.com/iframe_api"></script>
                <script>
                    var ytPlayer = null;
                    var isReady = false;
                    var queuedVid = null;

                    function onYouTubeIframeAPIReady() {
                        ytPlayer = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            playerVars: {
                                'autoplay': 1,
                                'controls': 0,
                                'playsinline': 1,
                                'origin': 'https://www.youtube.com',
                                'enablejsapi': 1,
                                'rel': 0,
                                'fs': 0,
                                'iv_load_policy': 3
                            },
                            events: {
                                'onReady': function(e) {
                                    isReady = true;
                                    try {
                                        ytPlayer.unMute();
                                        ytPlayer.setVolume(100);
                                    } catch(err) {}
                                    if (window.RiffboxBridge) window.RiffboxBridge.onPlayerReady();
                                    if (queuedVid) {
                                        var v = queuedVid;
                                        queuedVid = null;
                                        loadVideo(v);
                                    }
                                },
                                'onStateChange': function(e) {
                                    if (window.RiffboxBridge) {
                                        if (e.data === 1) window.RiffboxBridge.onState(true);
                                        else if (e.data === 2) window.RiffboxBridge.onState(false);
                                        else if (e.data === 0) window.RiffboxBridge.onEnded();
                                    }
                                },
                                'onError': function(e) {
                                    if (window.RiffboxBridge) window.RiffboxBridge.onError(e.data);
                                }
                            }
                        });
                    }

                    function loadVideo(vid) {
                        if (isReady && ytPlayer && ytPlayer.loadVideoById) {
                            try {
                                ytPlayer.loadVideoById(vid);
                                ytPlayer.unMute();
                                ytPlayer.setVolume(100);
                                ytPlayer.playVideo();
                            } catch(err) {
                                console.log('loadVideo error: ' + err);
                            }
                        } else {
                            queuedVid = vid;
                        }
                    }

                    function pauseVideo() {
                        if (isReady && ytPlayer && ytPlayer.pauseVideo) {
                            ytPlayer.pauseVideo();
                        }
                    }

                    function resumeVideo() {
                        if (isReady && ytPlayer && ytPlayer.playVideo) {
                            ytPlayer.playVideo();
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MusicPlayer", "WebView player shell loaded")
            }
        }

        webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
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
            webView.evaluateJavascript("pauseVideo();", null)
            localExoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                val mediaItem = MediaItem.fromUri(track.localFilePath!!)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        } else {
            // Play online track with official YouTube IFrame engine
            localExoPlayer?.pause()
            if (isYtPlayerReady) {
                webView.evaluateJavascript("loadVideo('${track.id}');", null)
            } else {
                pendingTrackToPlay = track
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
            webView.evaluateJavascript("resumeVideo();", null)
        }
    }

    fun pause() {
        isPlaying = false
        listener.onPlaybackStateChanged(false)
        if (currentTrack?.isDownloaded == true) {
            localExoPlayer?.pause()
        } else {
            webView.evaluateJavascript("pauseVideo();", null)
        }
    }

    fun stop() {
        isPlaying = false
        listener.onPlaybackStateChanged(false)
        localExoPlayer?.stop()
        webView.evaluateJavascript("pauseVideo();", null)
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
        webView.evaluateJavascript("pauseVideo();", null)
        webView.destroy()
    }
}
