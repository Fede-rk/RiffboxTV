package com.riffbox.tv

import android.app.AlertDialog
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.RoundedCornersTransformation
import com.riffbox.tv.analytics.AnalyticsManager
import com.riffbox.tv.data.AppUpdateInfo
import com.riffbox.tv.data.ChannelsRepository
import com.riffbox.tv.databinding.ActivityMainBinding
import com.riffbox.tv.model.Channel
import com.riffbox.tv.music.data.DownloadedTracksManager
import com.riffbox.tv.music.data.MusicCatalogRepository
import com.riffbox.tv.music.model.MusicArtist
import com.riffbox.tv.music.model.MusicCategory
import com.riffbox.tv.music.model.MusicPlaylist
import com.riffbox.tv.music.model.MusicTrack
import com.riffbox.tv.music.player.MusicPlayerManager
import com.riffbox.tv.music.ui.MusicArtistAdapter
import com.riffbox.tv.music.ui.MusicCategoryAdapter
import com.riffbox.tv.music.ui.MusicPlaylistAdapter
import com.riffbox.tv.music.ui.MusicTrackHorizontalAdapter
import com.riffbox.tv.music.ui.MusicTrackVerticalAdapter
import com.riffbox.tv.player.PlaybackManager
import com.riffbox.tv.ui.CategoryAdapter
import com.riffbox.tv.ui.ChannelAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), PlaybackManager.PlaybackListener, MusicPlayerManager.MusicPlayerListener {

    private lateinit var binding: ActivityMainBinding

    // Live TV / Radio components
    private lateinit var playbackManager: PlaybackManager
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var gestureDetector: GestureDetector

    // Music on-demand components
    private lateinit var musicPlayerManager: MusicPlayerManager
    private lateinit var musicCategoryAdapter: MusicCategoryAdapter
    private lateinit var musicPlaylistAdapter: MusicPlaylistAdapter
    private lateinit var musicArtistAdapter: MusicArtistAdapter
    private lateinit var musicDownloadsAdapter: MusicTrackHorizontalAdapter
    private lateinit var tracksOverlayAdapter: MusicTrackVerticalAdapter

    private var isTvDevice = false
    private val handler = Handler(Looper.getMainLooper())
    private var isSidebarVisible = false
    private var backPressedOnce = false

    // App Navigation State: MUSIC vs LIVE_TV
    private var currentMode = Mode.MUSIC

    private enum class Mode {
        MUSIC,
        LIVE_TV
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detectDeviceMode()
        AnalyticsManager.init(this, isTvDevice)
        requestNotificationPermission()

        setupMusicUI()
        setupLiveTVUI()
        setupGestures()
        setupPlayback()
        syncChannelsAndCheckUpdates()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun detectDeviceMode() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        isTvDevice = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                !packageManager.hasSystemFeature("android.hardware.touchscreen")

        if (isTvDevice) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    // ==========================================
    // RIFFBOX MUSIC ON-DEMAND SETUP
    // ==========================================
    private fun setupMusicUI() {
        // Initialize Music Player Engine
        musicPlayerManager = MusicPlayerManager(this, binding.youtubePlayerView, this)
        lifecycle.addObserver(binding.youtubePlayerView)

        // 1. Categories Adapter
        musicCategoryAdapter = MusicCategoryAdapter(MusicCatalogRepository.categories) { category ->
            openCategoryTracks(category)
        }
        binding.musicCategoriesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.musicCategoriesRecyclerView.adapter = musicCategoryAdapter

        // 2. Playlists Adapter
        musicPlaylistAdapter = MusicPlaylistAdapter(MusicCatalogRepository.playlists) { playlist ->
            openPlaylistTracks(playlist)
        }
        binding.musicPlaylistsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.musicPlaylistsRecyclerView.adapter = musicPlaylistAdapter

        // 3. Artists Adapter
        musicArtistAdapter = MusicArtistAdapter(MusicCatalogRepository.artists) { artist ->
            openArtistTracks(artist)
        }
        binding.musicArtistsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.musicArtistsRecyclerView.adapter = musicArtistAdapter

        // 4. Offline Downloads (Smartphones only)
        if (isTvDevice) {
            binding.downloadsSectionLayout.visibility = View.GONE
            binding.btnModalDownload.visibility = View.GONE
        } else {
            val downloadedTracks = DownloadedTracksManager.getDownloadedTracks(this)
            if (downloadedTracks.isNotEmpty()) {
                binding.downloadsSectionLayout.visibility = View.VISIBLE
            }
            musicDownloadsAdapter = MusicTrackHorizontalAdapter(downloadedTracks) { track ->
                playMusicTrack(track, downloadedTracks)
            }
            binding.musicDownloadsRecyclerView.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.musicDownloadsRecyclerView.adapter = musicDownloadsAdapter
        }

        // 5. Tracks Overlay Adapter
        tracksOverlayAdapter = MusicTrackVerticalAdapter(
            emptyList(),
            isTvDevice,
            onTrackClick = { track ->
                playMusicTrack(track, tracksOverlayAdapterCurrentList)
            },
            onDownloadClick = { track ->
                handleTrackDownload(track)
            }
        )
        binding.tracksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tracksRecyclerView.adapter = tracksOverlayAdapter

        // Back from Tracks Overlay
        binding.btnBackFromTracks.setOnClickListener {
            binding.musicTracksOverlay.visibility = View.GONE
            binding.musicScrollView.visibility = View.VISIBLE
        }

        // Search Input Handling
        binding.etSearchMusic.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    performMusicSearch(query)
                }
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        // Switch to Live TV Button
        binding.btnGoLive.setOnClickListener {
            switchToLiveTV()
        }

        // Return from Live TV Button
        binding.btnBackToMusic.setOnClickListener {
            switchToMusic()
        }

        // Mini Player Bar Controls
        binding.nowPlayingMiniBar.setOnClickListener {
            openMusicPlayerModal()
        }
        binding.btnMiniPlayPause.setOnClickListener {
            musicPlayerManager.togglePlayPause()
        }

        // Fullscreen Player Modal Controls
        binding.btnMinimizePlayer.setOnClickListener {
            binding.musicPlayerModal.visibility = View.GONE
        }
        binding.btnModalPlayPause.setOnClickListener {
            musicPlayerManager.togglePlayPause()
        }
        binding.btnModalNext.setOnClickListener {
            musicPlayerManager.playNext()
        }
        binding.btnModalPrev.setOnClickListener {
            musicPlayerManager.playPrevious()
        }
        binding.btnModalDownload.setOnClickListener {
            musicPlayerManager.currentTrack?.let { track ->
                handleTrackDownload(track)
            }
        }
    }

    private var tracksOverlayAdapterCurrentList: List<MusicTrack> = emptyList()

    private fun openCategoryTracks(category: MusicCategory) {
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = "${category.icon} ${category.title}"
        binding.musicSearchLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val results = MusicCatalogRepository.searchMusic(category.query)
            binding.musicSearchLoading.visibility = View.GONE
            tracksOverlayAdapterCurrentList = results
            tracksOverlayAdapter.updateList(results)
        }
    }

    private fun openPlaylistTracks(playlist: MusicPlaylist) {
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = playlist.title
        binding.musicSearchLoading.visibility = View.GONE

        tracksOverlayAdapterCurrentList = playlist.tracks
        tracksOverlayAdapter.updateList(playlist.tracks)
    }

    private fun openArtistTracks(artist: MusicArtist) {
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = "Canciones de ${artist.name}"
        binding.musicSearchLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val results = MusicCatalogRepository.searchMusic(artist.query)
            binding.musicSearchLoading.visibility = View.GONE
            tracksOverlayAdapterCurrentList = results
            tracksOverlayAdapter.updateList(results)
        }
    }

    private fun performMusicSearch(query: String) {
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = "Resultados para: $query"
        binding.musicSearchLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val results = MusicCatalogRepository.searchMusic(query)
            binding.musicSearchLoading.visibility = View.GONE
            tracksOverlayAdapterCurrentList = results
            tracksOverlayAdapter.updateList(results)
            if (results.isEmpty()) {
                Toast.makeText(this@MainActivity, "No se encontraron canciones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playMusicTrack(track: MusicTrack, queue: List<MusicTrack>) {
        // Stop TV playback if playing
        playbackManager.pause()

        musicPlayerManager.playTrack(track, queue)
        openMusicPlayerModal()
    }

    private fun openMusicPlayerModal() {
        binding.musicPlayerModal.visibility = View.VISIBLE
        musicPlayerManager.currentTrack?.let { updateModalUI(it) }
    }

    private fun updateModalUI(track: MusicTrack) {
        binding.modalTrackTitle.text = track.title
        binding.modalTrackArtist.text = if (track.album.isNotEmpty()) "${track.artist} • ${track.album}" else track.artist

        binding.miniTrackTitle.text = track.title
        binding.miniTrackArtist.text = track.artist
        binding.miniTrackThumb.load(track.thumbnailUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(RoundedCornersTransformation(10f))
        }
        binding.nowPlayingMiniBar.visibility = View.VISIBLE
    }

    private fun handleTrackDownload(track: MusicTrack) {
        if (isTvDevice) return // Disabled on TV

        if (DownloadedTracksManager.isTrackDownloaded(this, track.id)) {
            Toast.makeText(this, "Esta canción ya está descargada en tu dispositivo", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Iniciando descarga de: ${track.title}...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            // For YouTube tracks, resolve stream and save
            val streamUrl = "https://i.ytimg.com/vi/${track.id}/hqdefault.jpg" // placeholder audio stream target
            val result = DownloadedTracksManager.downloadAudioStream(this@MainActivity, track, streamUrl) { _ ->
                // Progress update if needed
            }

            if (result.isSuccess) {
                Toast.makeText(this@MainActivity, "¡Descarga completa! Guardada en Música/Riffbox", Toast.LENGTH_LONG).show()
                refreshDownloadsCarousel()
            } else {
                Toast.makeText(this@MainActivity, "No se pudo descargar la pista", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshDownloadsCarousel() {
        if (isTvDevice) return
        val downloaded = DownloadedTracksManager.getDownloadedTracks(this)
        if (downloaded.isNotEmpty()) {
            binding.downloadsSectionLayout.visibility = View.VISIBLE
            musicDownloadsAdapter.updateList(downloaded)
        }
    }

    // Music Player Listener
    override fun onTrackChanged(track: MusicTrack) {
        updateModalUI(track)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        val playPauseRes = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        binding.btnMiniPlayPause.setImageResource(playPauseRes)
        binding.btnModalPlayPause.setImageResource(playPauseRes)
    }

    // ==========================================
    // NAVIGATION: MUSIC <-> LIVE TV
    // ==========================================
    private fun switchToLiveTV() {
        currentMode = Mode.LIVE_TV
        musicPlayerManager.pause()
        binding.musicHomeContainer.visibility = View.GONE
        binding.musicPlayerModal.visibility = View.GONE
        binding.livePlayerContainer.visibility = View.VISIBLE

        val initialChannel = playbackManager.getLastSavedChannel()
        playbackManager.playChannel(initialChannel)

        if (isTvDevice) {
            showSidebar()
        }
    }

    private fun switchToMusic() {
        currentMode = Mode.MUSIC
        playbackManager.pause()
        binding.livePlayerContainer.visibility = View.GONE
        hideSidebarImmediate()
        binding.musicHomeContainer.visibility = View.VISIBLE
    }

    // ==========================================
    // LIVE TV / RADIO LOGIC
    // ==========================================
    private fun setupLiveTVUI() {
        // Category adapter
        categoryAdapter = CategoryAdapter(ChannelsRepository.categories) { selectedCategory ->
            AnalyticsManager.logCategorySelected(selectedCategory)
            val filtered = ChannelsRepository.getChannelsByCategory(selectedCategory)
            channelAdapter.updateList(filtered, playbackManager.currentChannel?.id ?: -1)
        }
        binding.categoriesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecyclerView.adapter = categoryAdapter

        // Channel adapter
        channelAdapter = ChannelAdapter(ChannelsRepository.channels) { channel ->
            playbackManager.playChannel(channel)
            hideSidebar()
        }
        binding.channelsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.channelsRecyclerView.adapter = channelAdapter

        // Dim overlay click closes sidebar
        binding.dimOverlay.setOnClickListener {
            hideSidebar()
        }
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_MIN_DISTANCE = 60
            private val SWIPE_THRESHOLD_VELOCITY = 100

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (currentMode == Mode.LIVE_TV) {
                    toggleSidebar()
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (currentMode != Mode.LIVE_TV || e1 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffY) > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    if (diffY < 0) {
                        playbackManager.playNext()
                    } else {
                        playbackManager.playPrevious()
                    }
                    return true
                }

                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    if (diffX < 0 && isSidebarVisible) {
                        hideSidebar()
                        return true
                    } else if (diffX > 0 && !isSidebarVisible && e1.x < 150) {
                        showSidebar()
                        return true
                    }
                }
                return false
            }
        })

        val touchListener = View.OnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.playerView.setOnTouchListener(touchListener)
        binding.radioContainer.setOnTouchListener(touchListener)
    }

    private fun setupPlayback() {
        playbackManager = PlaybackManager(this, this)
        binding.playerView.player = playbackManager.getPlayer()
    }

    private fun syncChannelsAndCheckUpdates() {
        ChannelsRepository.init(this) { success, updateInfo ->
            if (success) {
                categoryAdapter.updateCategories(ChannelsRepository.categories)
                val currentCat = ChannelsRepository.categories.getOrElse(categoryAdapter.selectedIndex) { "Todas" }
                val filtered = ChannelsRepository.getChannelsByCategory(currentCat)
                channelAdapter.updateList(filtered, playbackManager.currentChannel?.id ?: -1)
            }

            updateInfo?.let { info ->
                checkAppUpdate(info)
            }
        }
    }

    private fun checkAppUpdate(info: AppUpdateInfo) {
        if (info.latestVersionCode > BuildConfig.VERSION_CODE && info.downloadUrl.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Actualización Disponible")
                .setMessage("Hay una nueva versión de Riffbox TV (${info.latestVersionName}) disponible.\n\n${info.releaseNotes}")
                .setPositiveButton("Actualizar") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    startActivity(intent)
                }
                .setNegativeButton("Más tarde", null)
                .show()
        }
    }

    override fun onChannelChanged(channel: Channel) {
        channelAdapter.setSelectedChannel(channel.id)

        if (channel.isRadio) {
            binding.playerView.visibility = View.GONE
            binding.radioContainer.visibility = View.VISIBLE
            binding.radioTitle.text = channel.name
            binding.radioSubtitle.text = "${channel.category} • Audio Alta Fidelidad"
        } else {
            binding.radioContainer.visibility = View.GONE
            binding.playerView.visibility = View.VISIBLE
        }

        showOsd(channel)
        AnalyticsManager.logChannelPlayed(channel)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean, isLoading: Boolean) {
        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onError(error: String) {
        binding.loadingIndicator.visibility = View.GONE
        if (currentMode == Mode.LIVE_TV) {
            showSidebar()
        }
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    private fun showOsd(channel: Channel) {
        binding.osdChannelNumber.text = "CH %02d".format(channel.id)
        binding.osdChannelName.text = channel.name
        binding.osdBadge.text = channel.badge
        binding.osdCategory.text = channel.category

        binding.osdContainer.visibility = View.VISIBLE
        binding.osdContainer.alpha = 1f

        handler.removeCallbacks(hideOsdRunnable)
        handler.postDelayed(hideOsdRunnable, 3500)
    }

    private val hideOsdRunnable = Runnable {
        binding.osdContainer.animate().alpha(0f).setDuration(400).withEndAction {
            binding.osdContainer.visibility = View.GONE
        }.start()
    }

    private fun toggleSidebar() {
        if (isSidebarVisible) hideSidebar() else showSidebar()
    }

    private fun showSidebar() {
        isSidebarVisible = true
        binding.dimOverlay.visibility = View.VISIBLE
        binding.dimOverlay.alpha = 0f
        binding.dimOverlay.animate().alpha(1f).setDuration(200).start()

        binding.sidebarContainer.visibility = View.VISIBLE
        binding.sidebarContainer.animate().translationX(0f).setDuration(250).start()

        binding.channelsRecyclerView.post {
            val playingPos = ChannelsRepository.channels.indexOfFirst { it.id == playbackManager.currentChannel?.id }
            if (playingPos >= 0) {
                binding.channelsRecyclerView.scrollToPosition(playingPos)
                binding.channelsRecyclerView.findViewHolderForAdapterPosition(playingPos)?.itemView?.requestFocus()
                    ?: binding.channelsRecyclerView.requestFocus()
            } else {
                binding.channelsRecyclerView.requestFocus()
            }
        }
    }

    private fun hideSidebar() {
        isSidebarVisible = false
        binding.dimOverlay.animate().alpha(0f).setDuration(200).withEndAction {
            binding.dimOverlay.visibility = View.GONE
        }.start()

        val width = if (binding.sidebarContainer.width > 0) binding.sidebarContainer.width.toFloat() else 800f
        binding.sidebarContainer.animate()
            .translationX(-width)
            .setDuration(250)
            .withEndAction { binding.sidebarContainer.visibility = View.GONE }
            .start()
    }

    private fun hideSidebarImmediate() {
        isSidebarVisible = false
        binding.dimOverlay.visibility = View.GONE
        binding.dimOverlay.alpha = 0f
        binding.sidebarContainer.visibility = View.GONE
        binding.sidebarContainer.translationX = -1200f
    }

    private fun isChildOf(view: View?, parent: View): Boolean {
        var p = view?.parent
        while (p != null) {
            if (p == parent) return true
            p = p.parent
        }
        return false
    }

    private fun switchCategoryRelative(delta: Int) {
        val total = ChannelsRepository.categories.size
        if (total == 0) return
        val newIndex = (categoryAdapter.selectedIndex + delta + total) % total
        categoryAdapter.selectCategory(newIndex)
        binding.categoriesRecyclerView.smoothScrollToPosition(newIndex)
    }

    // ==========================================
    // REMOTE CONTROL KEYS (D-PAD & BACK)
    // ==========================================
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 1. If Fullscreen Music Player Modal is open:
        if (binding.musicPlayerModal.visibility == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                binding.musicPlayerModal.visibility = View.GONE
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        // 2. If Tracks Overlay is open:
        if (binding.musicTracksOverlay.visibility == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                binding.musicTracksOverlay.visibility = View.GONE
                binding.musicScrollView.visibility = View.VISIBLE
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        // 3. If in LIVE TV Mode:
        if (currentMode == Mode.LIVE_TV) {
            if (isSidebarVisible) {
                when (keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        hideSidebar()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        val focused = currentFocus
                        if (focused != null && isChildOf(focused, binding.channelsRecyclerView)) {
                            switchCategoryRelative(-1)
                            return true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        val focused = currentFocus
                        if (focused != null && isChildOf(focused, binding.channelsRecyclerView)) {
                            switchCategoryRelative(1)
                            return true
                        }
                    }
                }
                return super.onKeyDown(keyCode, event)
            }

            // When sidebar is closed in Live TV:
            when (keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    playbackManager.playNext()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_UP -> {
                    playbackManager.playPrevious()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    showSidebar()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    // Back from Live TV returns to Riffbox Music!
                    switchToMusic()
                    return true
                }
            }
            return super.onKeyDown(keyCode, event)
        }

        // 4. In MUSIC Mode (Home Screen):
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backPressedOnce) {
                finish()
            } else {
                backPressedOnce = true
                Toast.makeText(this, getString(R.string.press_back_again), Toast.LENGTH_SHORT).show()
                handler.postDelayed({ backPressedOnce = false }, 2000)
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        playbackManager.release()
        musicPlayerManager.release()
    }
}