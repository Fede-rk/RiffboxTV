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
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import java.io.File
import com.riffbox.tv.analytics.AnalyticsManager
import com.riffbox.tv.data.AppUpdateInfo
import com.riffbox.tv.data.ChannelsRepository
import com.riffbox.tv.databinding.ActivityMainBinding
import com.riffbox.tv.model.Channel
import com.riffbox.tv.music.data.DownloadedTracksManager
import com.riffbox.tv.music.data.MusicCatalogRepository
import com.riffbox.tv.music.model.ArtistProfile
import com.riffbox.tv.music.model.MusicAlbum
import com.riffbox.tv.music.model.MusicArtist
import com.riffbox.tv.music.model.MusicCategory
import com.riffbox.tv.music.model.MusicPlaylist
import com.riffbox.tv.music.model.MusicTrack
import com.riffbox.tv.music.player.MusicPlayerManager
import com.riffbox.tv.music.ui.MusicAlbumAdapter
import com.riffbox.tv.music.ui.MusicArtistAdapter
import com.riffbox.tv.music.ui.MusicCategoryAdapter
import com.riffbox.tv.music.ui.MusicPlaylistAdapter
import com.riffbox.tv.music.ui.MusicTrackHorizontalAdapter
import com.riffbox.tv.music.ui.MusicTrackVerticalAdapter
import com.riffbox.tv.player.PlaybackManager
import com.riffbox.tv.ui.CategoryAdapter
import com.riffbox.tv.ui.ChannelAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    private lateinit var artistAlbumsAdapter: MusicAlbumAdapter
    private lateinit var artistSinglesAdapter: MusicAlbumAdapter
    private var currentArtistProfile: ArtistProfile? = null
    private var isViewingAlbum = false
    private var isPlayerModalOpen = false


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
        checkDirectUpdate()
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
        musicPlayerManager = MusicPlayerManager(this, binding.musicYouTubePlayerView, this)

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
        } else {
            val downloadedTracks = DownloadedTracksManager.getDownloadedTracks(this)
            if (downloadedTracks.isNotEmpty()) {
                binding.downloadsSectionLayout.visibility = View.VISIBLE
            }
            binding.downloadsHeaderLayout.setOnClickListener {
                openDownloadsTracks()
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
                confirmTrackDownload(track)
            },
            onDeleteClick = { track ->
                confirmTrackDelete(track)
            }
        )
        binding.tracksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tracksRecyclerView.adapter = tracksOverlayAdapter

        // Artist Albums and Singles Horizontal Carousels
        artistAlbumsAdapter = MusicAlbumAdapter(emptyList()) { album ->
            openAlbumTracks(album)
        }
        binding.rvArtistAlbums.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = artistAlbumsAdapter
        }

        artistSinglesAdapter = MusicAlbumAdapter(emptyList()) { single ->
            openAlbumTracks(single)
        }
        binding.rvArtistSingles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = artistSinglesAdapter
        }

        // Back from Tracks Overlay
        binding.btnBackFromTracks.setOnClickListener {
            handleTracksOverlayBack()
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
        binding.miniTrackArtist.setOnClickListener {
            musicPlayerManager.currentTrack?.let { track ->
                val cleanArtist = track.artist
                    .replace(Regex("""-\s*Topic""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*feat\..*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (cleanArtist.isNotEmpty()) {
                    openArtistProfile(cleanArtist)
                }
            }
        }
        binding.btnMiniPlayPause.setOnClickListener {
            musicPlayerManager.togglePlayPause()
        }

        // Fullscreen Player Modal Controls
        binding.btnMinimizePlayer.setOnClickListener {
            closeMusicPlayerModal()
        }
        binding.modalTrackArtist.setOnClickListener {
            musicPlayerManager.currentTrack?.let { track ->
                val cleanArtist = track.artist
                    .replace(Regex("""-\s*Topic""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*feat\..*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (cleanArtist.isNotEmpty()) {
                    closeMusicPlayerModal()
                    openArtistProfile(cleanArtist)
                }
            }
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

        // Player Artwork Touch Overlay: toggles play/pause and absorbs all touches so WebView never opens YouTube
        binding.playerTouchOverlay.setOnClickListener {
            musicPlayerManager.togglePlayPause()
        }
        binding.playerTouchOverlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                musicPlayerManager.togglePlayPause()
            }
            true
        }
    }

    private var tracksOverlayAdapterCurrentList: List<MusicTrack> = emptyList()

    private fun handleTracksOverlayBack(): Boolean {
        if (binding.musicTracksOverlay.visibility == View.VISIBLE) {
            if (isViewingAlbum && currentArtistProfile != null) {
                // Return from Album to Artist Profile
                displayArtistProfile(currentArtistProfile!!)
                return true
            } else {
                // Close Overlay and return to Home / Search
                binding.musicTracksOverlay.visibility = View.GONE
                binding.musicScrollView.visibility = View.VISIBLE
                currentArtistProfile = null
                isViewingAlbum = false
                return true
            }
        }
        return false
    }

    private fun openCategoryTracks(category: MusicCategory) {
        currentArtistProfile = null
        isViewingAlbum = false
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = "${category.icon} ${category.title}"
        binding.searchArtistCard.visibility = View.GONE
        binding.artistAlbumsSection.visibility = View.GONE
        binding.artistSinglesSection.visibility = View.GONE
        binding.tvTracksSectionTitle.visibility = View.GONE
        binding.musicSearchLoading.visibility = View.VISIBLE

        binding.overlayProfileHeader.visibility = View.VISIBLE
        binding.ivOverlayHeaderThumb.setImageResource(R.drawable.ic_riffbox_logo)
        binding.tvOverlayHeaderName.text = "${category.icon} ${category.title}"
        binding.tvOverlayHeaderSubtitle.text = "Cargando lo mejor del género..."
        binding.btnOverlaySimilar.visibility = View.GONE

        lifecycleScope.launch {
            val results = MusicCatalogRepository.searchMusic(category.query)
            val fullList = MusicCatalogRepository.getTracksForCategory(category, results)
            binding.musicSearchLoading.visibility = View.GONE
            binding.tvOverlayHeaderSubtitle.text = "${fullList.size} canciones seleccionadas"

            binding.btnOverlayPlayAll.setOnClickListener {
                if (fullList.isNotEmpty()) {
                    playMusicTrack(fullList.first(), fullList)
                }
            }
            binding.btnOverlayShuffle.setOnClickListener {
                if (fullList.isNotEmpty()) {
                    val shuffled = fullList.shuffled()
                    playMusicTrack(shuffled.first(), shuffled)
                }
            }

            tracksOverlayAdapterCurrentList = fullList
            tracksOverlayAdapter.updateList(fullList)
        }
    }

    private fun openPlaylistTracks(playlist: MusicPlaylist) {
        currentArtistProfile = null
        isViewingAlbum = false
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = playlist.title
        binding.searchArtistCard.visibility = View.GONE
        binding.artistAlbumsSection.visibility = View.GONE
        binding.artistSinglesSection.visibility = View.GONE
        binding.tvTracksSectionTitle.visibility = View.GONE
        binding.musicSearchLoading.visibility = View.GONE

        // Setup Playlist Header Banner
        binding.overlayProfileHeader.visibility = View.VISIBLE
        binding.btnOverlaySimilar.visibility = View.GONE
        binding.ivOverlayHeaderThumb.load(playlist.coverUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(RoundedCornersTransformation(14f))
        }
        binding.tvOverlayHeaderName.text = playlist.title
        binding.tvOverlayHeaderSubtitle.text = "${playlist.tracks.size} canciones • ${playlist.badge}"

        binding.btnOverlayPlayAll.setOnClickListener {
            if (playlist.tracks.isNotEmpty()) {
                playMusicTrack(playlist.tracks.first(), playlist.tracks)
            }
        }
        binding.btnOverlayShuffle.setOnClickListener {
            if (playlist.tracks.isNotEmpty()) {
                val shuffled = playlist.tracks.shuffled()
                playMusicTrack(shuffled.first(), shuffled)
            }
        }

        tracksOverlayAdapterCurrentList = playlist.tracks
        tracksOverlayAdapter.updateList(playlist.tracks)
    }

    private fun openArtistTracks(artist: MusicArtist) {
        openArtistProfile(artist.name, "", artist.imageUrl)
    }

    private fun openArtistProfile(name: String, browseId: String = "", imageUrl: String = "") {
        isViewingAlbum = false
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = name
        binding.searchArtistCard.visibility = View.GONE
        binding.artistAlbumsSection.visibility = View.GONE
        binding.artistSinglesSection.visibility = View.GONE
        binding.tvTracksSectionTitle.visibility = View.GONE
        binding.musicSearchLoading.visibility = View.VISIBLE
        binding.btnOverlaySimilar.visibility = View.GONE

        binding.overlayProfileHeader.visibility = View.VISIBLE
        binding.ivOverlayHeaderThumb.load(imageUrl.ifEmpty { null }) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(CircleCropTransformation())
        }
        binding.tvOverlayHeaderName.text = name
        binding.tvOverlayHeaderSubtitle.text = "Cargando discografía, álbumes y éxitos..."

        lifecycleScope.launch {
            val profile = if (browseId.isNotEmpty()) {
                MusicCatalogRepository.getArtistProfile(browseId, name, imageUrl)
            } else {
                MusicCatalogRepository.getArtistProfileByName(name)
            }

            binding.musicSearchLoading.visibility = View.GONE

            if (profile != null) {
                currentArtistProfile = profile
                displayArtistProfile(profile)
            } else {
                binding.tvOverlayHeaderSubtitle.text = "Canciones"
                val tracks = MusicCatalogRepository.searchMusic(name)
                tracksOverlayAdapterCurrentList = tracks
                tracksOverlayAdapter.updateList(tracks)
            }
        }
    }

    private fun displayArtistProfile(profile: ArtistProfile) {
        isViewingAlbum = false
        binding.tvTracksOverlayTitle.text = profile.name
        binding.overlayProfileHeader.visibility = View.VISIBLE
        binding.searchArtistCard.visibility = View.GONE

        binding.ivOverlayHeaderThumb.load(profile.imageUrl.ifEmpty { null }) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(CircleCropTransformation())
        }
        binding.tvOverlayHeaderName.text = profile.name
        binding.tvOverlayHeaderSubtitle.text = if (profile.subtitle.isNotEmpty()) {
            profile.subtitle
        } else {
            "Discografía completa • ${profile.topTracks.size} canciones populares"
        }

        binding.btnOverlayPlayAll.setOnClickListener {
            if (profile.topTracks.isNotEmpty()) {
                playMusicTrack(profile.topTracks.first(), profile.topTracks)
            }
        }
        binding.btnOverlayShuffle.setOnClickListener {
            if (profile.topTracks.isNotEmpty()) {
                val shuffled = profile.topTracks.shuffled()
                playMusicTrack(shuffled.first(), shuffled)
            }
        }

        // Button: Artistas Similares (Automatic, dynamic, on-the-fly random mix)
        binding.btnOverlaySimilar.visibility = View.VISIBLE
        binding.btnOverlaySimilar.setOnClickListener {
            binding.musicSearchLoading.visibility = View.VISIBLE
            binding.tvOverlayHeaderSubtitle.text = "🎲 Armando lista de ${profile.name} y artistas similares..."
            Toast.makeText(this, "Creando lista aleatoria con artistas similares...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                val radioQueue = MusicCatalogRepository.buildSimilarArtistsRadioQueue(profile)
                binding.musicSearchLoading.visibility = View.GONE

                if (radioQueue.isNotEmpty()) {
                    tracksOverlayAdapterCurrentList = radioQueue
                    tracksOverlayAdapter.updateList(radioQueue)

                    binding.tvTracksOverlayTitle.text = "📻 ${profile.name} + Similares"
                    binding.tvTracksSectionTitle.visibility = View.VISIBLE
                    binding.tvTracksSectionTitle.text = "Mezcla Aleatoria (${radioQueue.size} canciones)"
                    binding.tvOverlayHeaderSubtitle.text = "Mezcla dinámica generada en el momento"

                    playMusicTrack(radioQueue.first(), radioQueue)
                    Toast.makeText(this@MainActivity, "▶ Reproduciendo ${radioQueue.size} canciones de ${profile.name} y similares", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvOverlayHeaderSubtitle.text = profile.subtitle.ifEmpty { "Canciones" }
                    Toast.makeText(this@MainActivity, "No se pudieron obtener artistas similares", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Albums Section
        if (profile.albums.isNotEmpty()) {
            binding.artistAlbumsSection.visibility = View.VISIBLE
            artistAlbumsAdapter.updateList(profile.albums)
        } else {
            binding.artistAlbumsSection.visibility = View.GONE
        }

        // Singles Section
        if (profile.singles.isNotEmpty()) {
            binding.artistSinglesSection.visibility = View.VISIBLE
            artistSinglesAdapter.updateList(profile.singles)
        } else {
            binding.artistSinglesSection.visibility = View.GONE
        }

        // Top Songs Section
        if (profile.topTracks.isNotEmpty()) {
            binding.tvTracksSectionTitle.visibility = View.VISIBLE
            binding.tvTracksSectionTitle.text = "Canciones Populares"
            tracksOverlayAdapterCurrentList = profile.topTracks
            tracksOverlayAdapter.updateList(profile.topTracks)
        } else {
            binding.tvTracksSectionTitle.visibility = View.GONE
            tracksOverlayAdapterCurrentList = emptyList()
            tracksOverlayAdapter.updateList(emptyList())
        }
    }

    private fun openAlbumTracks(album: MusicAlbum) {
        isViewingAlbum = true
        binding.tvTracksOverlayTitle.text = album.title
        binding.searchArtistCard.visibility = View.GONE
        binding.artistAlbumsSection.visibility = View.GONE
        binding.artistSinglesSection.visibility = View.GONE
        binding.musicSearchLoading.visibility = View.VISIBLE

        binding.overlayProfileHeader.visibility = View.VISIBLE
        binding.btnOverlaySimilar.visibility = View.GONE
        binding.ivOverlayHeaderThumb.load(album.coverUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(RoundedCornersTransformation(14f))
        }
        binding.tvOverlayHeaderName.text = album.title
        binding.tvOverlayHeaderSubtitle.text = "${album.artist} • ${if (album.year.isNotEmpty()) album.year else "Álbum completo"}"

        binding.tvTracksSectionTitle.visibility = View.VISIBLE
        binding.tvTracksSectionTitle.text = "Canciones del Álbum"

        lifecycleScope.launch {
            val albumTracks = MusicCatalogRepository.getAlbumTracks(album)
            binding.musicSearchLoading.visibility = View.GONE

            binding.tvOverlayHeaderSubtitle.text = "${album.artist} • ${if (album.year.isNotEmpty()) "${album.year} • " else ""}${albumTracks.size} canciones"

            binding.btnOverlayPlayAll.setOnClickListener {
                if (albumTracks.isNotEmpty()) {
                    playMusicTrack(albumTracks.first(), albumTracks)
                }
            }
            binding.btnOverlayShuffle.setOnClickListener {
                if (albumTracks.isNotEmpty()) {
                    val shuffled = albumTracks.shuffled()
                    playMusicTrack(shuffled.first(), shuffled)
                }
            }

            tracksOverlayAdapterCurrentList = albumTracks
            tracksOverlayAdapter.updateList(albumTracks)
        }
    }

    private fun performMusicSearch(query: String) {
        currentArtistProfile = null
        isViewingAlbum = false
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = "Búsqueda: $query"
        binding.overlayProfileHeader.visibility = View.GONE
        binding.artistAlbumsSection.visibility = View.GONE
        binding.artistSinglesSection.visibility = View.GONE
        binding.tvTracksSectionTitle.visibility = View.GONE
        binding.searchArtistCard.visibility = View.GONE
        binding.musicSearchLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = MusicCatalogRepository.searchMusicWithArtist(query)
            binding.musicSearchLoading.visibility = View.GONE

            if (result.artist != null && result.artist.name.isNotEmpty()) {
                binding.searchArtistCard.visibility = View.VISIBLE
                binding.tvSearchArtistName.text = result.artist.name
                binding.tvSearchArtistSubtitle.text = if (result.artist.subtitle.isNotEmpty()) {
                    "${result.artist.subtitle} • Ver perfil completo, álbumes y singles ▶"
                } else {
                    "Artista • Toca para ver discografía, álbumes y singles ▶"
                }
                binding.ivSearchArtistThumb.load(result.artist.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_riffbox_logo)
                    error(R.drawable.ic_riffbox_logo)
                    transformations(CircleCropTransformation())
                }
                binding.searchArtistCard.setOnClickListener {
                    openArtistProfile(result.artist.name, result.artist.browseId, result.artist.imageUrl)
                }
            } else {
                binding.searchArtistCard.visibility = View.GONE
            }

            tracksOverlayAdapterCurrentList = result.tracks
            tracksOverlayAdapter.updateList(result.tracks)
            if (result.tracks.isEmpty() && result.artist == null) {
                Toast.makeText(this@MainActivity, "No se encontraron canciones", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun playMusicTrack(track: MusicTrack, queue: List<MusicTrack>) {
        // Stop TV playback if playing
        playbackManager.pause()

        // If playing from outside or a single track, generate a smart Radio queue of similar songs
        val effectiveQueue = if (queue.size <= 1) {
            MusicCatalogRepository.buildRadioQueue(track)
        } else {
            queue
        }

        musicPlayerManager.playTrack(track, effectiveQueue)
        openMusicPlayerModal()
    }

    private fun openMusicPlayerModal() {
        isPlayerModalOpen = true
        binding.musicPlayerModal.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(250)
            .start()
        musicPlayerManager.currentTrack?.let { updateModalUI(it) }
    }

    private fun closeMusicPlayerModal() {
        isPlayerModalOpen = false
        val screenHeight = resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(1920f)
        binding.musicPlayerModal.animate()
            .translationY(screenHeight + 500f)
            .alpha(0f)
            .setDuration(250)
            .start()
    }

    private fun updateModalUI(track: MusicTrack) {
        binding.modalTrackTitle.text = track.title
        binding.modalTrackArtist.text = if (track.album.isNotEmpty()) "${track.artist} • ${track.album}" else track.artist

        // Album Artwork: Always 100% visible, clean and elegant (Zero YouTube video, zero YouTube logos)
        binding.modalArtworkImage.visibility = View.VISIBLE
        binding.modalArtworkImage.alpha = 1f
        binding.modalArtworkImage.load(track.thumbnailUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(RoundedCornersTransformation(18f))
        }

        binding.miniTrackTitle.text = track.title
        binding.miniTrackArtist.text = track.artist
        binding.miniTrackThumb.load(track.thumbnailUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(RoundedCornersTransformation(10f))
        }
        binding.nowPlayingMiniBar.visibility = View.VISIBLE

        // Load Lyrics (Spotify / Apple Music Style)
        binding.cardModalLyrics.visibility = View.VISIBLE
        binding.lyricsLoading.visibility = View.VISIBLE
        binding.tvModalLyrics.text = "Buscando letra de la canción..."

        lifecycleScope.launch {
            val lyrics = MusicCatalogRepository.getTrackLyrics(track.artist, track.title)
            binding.lyricsLoading.visibility = View.GONE
            if (!lyrics.isNullOrEmpty()) {
                binding.tvModalLyrics.text = lyrics
            } else {
                binding.tvModalLyrics.text = "Letra instrumental o no disponible para esta canción."
            }
        }
    }


    private fun confirmTrackDownload(track: MusicTrack) {
        if (isTvDevice) return
        if (DownloadedTracksManager.isTrackDownloaded(this, track.id)) {
            Toast.makeText(this, "Esta canción ya está descargada en tu dispositivo", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Descargar canción")
            .setMessage("¿Deseas descargar '${track.title} - ${track.artist}' para escuchar sin conexión?")
            .setPositiveButton("Descargar") { _, _ ->
                handleTrackDownload(track)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmTrackDelete(track: MusicTrack) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar descarga")
            .setMessage("¿Deseas eliminar '${track.title}' de tu dispositivo?")
            .setPositiveButton("Eliminar") { _, _ ->
                DownloadedTracksManager.removeTrack(this, track.id)
                Toast.makeText(this, "Canción eliminada", Toast.LENGTH_SHORT).show()
                refreshDownloadsCarousel()
                val isDownloadsView = binding.tvTracksOverlayTitle.text.contains("Guardadas")
                val updated = tracksOverlayAdapterCurrentList.map {
                    if (it.id == track.id) it.copy(isDownloaded = false, localFilePath = null) else it
                }.filter {
                    if (isDownloadsView) it.id != track.id else true
                }
                tracksOverlayAdapterCurrentList = updated
                tracksOverlayAdapter.updateList(updated)
                if (isDownloadsView) {
                    binding.tvOverlayHeaderSubtitle.text = "${updated.size} canciones en tu dispositivo"
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openDownloadsTracks() {
        val downloaded = DownloadedTracksManager.getDownloadedTracks(this)
        binding.musicScrollView.visibility = View.GONE
        binding.musicTracksOverlay.visibility = View.VISIBLE
        binding.tvTracksOverlayTitle.text = "💾 Canciones Guardadas"
        binding.musicSearchLoading.visibility = View.GONE

        binding.overlayProfileHeader.visibility = View.VISIBLE
        binding.btnOverlaySimilar.visibility = View.GONE
        binding.ivOverlayHeaderThumb.setImageResource(R.drawable.ic_riffbox_logo)
        binding.tvOverlayHeaderName.text = "Canciones Guardadas"
        binding.tvOverlayHeaderSubtitle.text = "${downloaded.size} canciones en tu dispositivo"

        binding.btnOverlayPlayAll.setOnClickListener {
            if (downloaded.isNotEmpty()) {
                playMusicTrack(downloaded.first(), downloaded)
            }
        }
        binding.btnOverlayShuffle.setOnClickListener {
            if (downloaded.isNotEmpty()) {
                val shuffled = downloaded.shuffled()
                playMusicTrack(shuffled.first(), shuffled)
            }
        }

        tracksOverlayAdapterCurrentList = downloaded
        tracksOverlayAdapter.updateList(downloaded)
    }

    private fun handleTrackDownload(track: MusicTrack) {
        if (isTvDevice) return // Disabled on TV

        if (DownloadedTracksManager.isTrackDownloaded(this, track.id)) {
            Toast.makeText(this, "Esta canción ya está descargada en tu dispositivo", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "La descarga offline para este tema estará disponible próximamente", Toast.LENGTH_SHORT).show()
    }

    private fun refreshDownloadsCarousel() {
        if (isTvDevice) return
        val downloaded = DownloadedTracksManager.getDownloadedTracks(this)
        if (downloaded.isNotEmpty()) {
            binding.downloadsSectionLayout.visibility = View.VISIBLE
            musicDownloadsAdapter.updateList(downloaded)
        } else {
            binding.downloadsSectionLayout.visibility = View.GONE
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

        // Cover artwork remains 100% visible - zero video, zero YouTube logos/branding!
        binding.modalArtworkImage.alpha = 1f
    }


    // ==========================================
    // NAVIGATION: MUSIC <-> LIVE TV
    // ==========================================
    private fun switchToLiveTV() {
        currentMode = Mode.LIVE_TV
        musicPlayerManager.pause()
        binding.musicHomeContainer.visibility = View.GONE
        closeMusicPlayerModal()
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
                .setTitle("🚀 Actualización Disponible (${info.latestVersionName})")
                .setMessage("Hay una nueva versión de Riffbox TV (${info.latestVersionName}) lista para instalar.\n\n${info.releaseNotes}\n\n💡 También puedes actualizar desde la app Downloader con el código 2852396.")
                .setPositiveButton("Actualizar e Instalar") { _, _ ->
                    startApkDownloadAndInstall(info.downloadUrl)
                }
                .setNeutralButton("Abrir Enlace Externo") { _, _ ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                    } catch (e: Exception) {
                        Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Más tarde", null)
                .show()
        }
    }

    private fun checkDirectUpdate() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Bust cache using timestamp to guarantee instant update detection
                val url = URL("https://raw.githubusercontent.com/Fede-rk/RiffboxTV/main/channels.json?t=${System.currentTimeMillis()}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.setRequestProperty("User-Agent", "RiffboxTV-App")
                conn.setRequestProperty("Cache-Control", "no-cache")
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val vCode = root.optInt("latest_app_version_code", 0)
                    val vName = root.optString("latest_app_version_name", "")
                    val notes = root.optString("release_notes", "")
                    val dlUrl = root.optString("download_url", "")
                    if (vCode > BuildConfig.VERSION_CODE && dlUrl.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (!isFinishing && !isDestroyed) {
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle("🚀 Actualización Disponible (v$vName)")
                                    .setMessage("$notes\n\n¿Deseas descargar e instalar la actualización ahora?\n\n💡 O actualiza desde Downloader con el código 2852396.")
                                    .setPositiveButton("Descargar e Instalar") { _, _ ->
                                        startApkDownloadAndInstall(dlUrl)
                                    }
                                    .setNeutralButton("Abrir Enlace Externo") { _, _ ->
                                        try {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dlUrl)))
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .setNegativeButton("Más tarde", null)
                                    .show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startApkDownloadAndInstall(downloadUrl: String) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Descargando Riffbox TV...")
            .setMessage("Iniciando descarga de la nueva versión, por favor espera un momento...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Follow HTTP/HTTPS 301/302 redirects manually (GitHub Releases -> Azure Blob / S3)
                var currentUrl = downloadUrl
                var connection: HttpURLConnection? = null
                var redirects = 0
                val maxRedirects = 6

                while (redirects < maxRedirects) {
                    val url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "RiffboxTV-App")
                    }

                    val code = connection.responseCode
                    if (code in 300..399) {
                        val loc = connection.getHeaderField("Location") ?: break
                        currentUrl = if (loc.startsWith("http")) loc else URL(url, loc).toString()
                        connection.disconnect()
                        redirects++
                    } else {
                        break
                    }
                }

                val conn = connection ?: throw IllegalStateException("No se pudo conectar al servidor de descarga")
                if (conn.responseCode !in 200..299) {
                    throw IllegalStateException("HTTP ${conn.responseCode}")
                }

                val apkFile = File(cacheDir, "riffbox_update.apk")
                if (apkFile.exists()) apkFile.delete()

                val totalLength = conn.contentLength
                conn.inputStream.use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var downloaded = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (totalLength > 0) {
                                val progressPercent = ((downloaded * 100) / totalLength).toInt()
                                withContext(Dispatchers.Main) {
                                    val mbDownloaded = String.format("%.1f", downloaded / (1024.0 * 1024.0))
                                    val mbTotal = String.format("%.1f", totalLength / (1024.0 * 1024.0))
                                    progressDialog.setMessage("Descargando: $progressPercent% ($mbDownloaded MB de $mbTotal MB)...")
                                }
                            }
                        }
                        output.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    promptInstallApk(apkFile)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error downloading APK: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Fallo descarga directa: ${e.message}. Abriendo navegador...", Toast.LENGTH_LONG).show()
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
                    } catch (ex: Exception) {
                        Toast.makeText(this@MainActivity, "No se pudo abrir el instalador", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun promptInstallApk(apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // On Android 8.0+ check unknown sources permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    AlertDialog.Builder(this)
                        .setTitle("Permiso de Instalación")
                        .setMessage("Para actualizar Riffbox TV directamente, debes permitir instalar aplicaciones de fuentes desconocidas para esta app.\n\nAl presionar Continuar se abrirán los ajustes de tu TV o dispositivo.")
                        .setPositiveButton("Continuar") { _, _ ->
                            try {
                                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                            } catch (e: Exception) {
                                startActivity(installIntent)
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    return
                }
            }

            startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error launching installer: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar instalador: ${e.message}", Toast.LENGTH_LONG).show()
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
        if (isPlayerModalOpen) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                closeMusicPlayerModal()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        // 2. If Tracks Overlay is open:
        if (binding.musicTracksOverlay.visibility == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (handleTracksOverlayBack()) {
                    return true
                }
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