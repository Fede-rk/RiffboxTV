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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.riffbox.tv.data.AppUpdateInfo
import com.riffbox.tv.data.ChannelsRepository
import com.riffbox.tv.databinding.ActivityMainBinding
import com.riffbox.tv.model.Channel
import com.riffbox.tv.player.PlaybackManager
import com.riffbox.tv.ui.CategoryAdapter
import com.riffbox.tv.ui.ChannelAdapter
import com.riffbox.tv.analytics.AnalyticsManager

class MainActivity : AppCompatActivity(), PlaybackManager.PlaybackListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var playbackManager: PlaybackManager
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var gestureDetector: GestureDetector

    private var isTvDevice = false
    private val handler = Handler(Looper.getMainLooper())
    private var isSidebarVisible = false
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detectDeviceMode()
        AnalyticsManager.init(this, isTvDevice)
        requestNotificationPermission()
        setupUI()
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

    private fun setupUI() {
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
                toggleSidebar()
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                // Vertical swipe: change channel
                if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffY) > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    if (diffY < 0) {
                        playbackManager.playNext()
                    } else {
                        playbackManager.playPrevious()
                    }
                    return true
                }

                // Horizontal swipe: hide/show sidebar
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

        val initialChannel = playbackManager.getLastSavedChannel()
        playbackManager.playChannel(initialChannel)

        if (isTvDevice) {
            showSidebar()
        } else {
            hideSidebarImmediate()
        }
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

            val audioSession = playbackManager.getPlayer()?.audioSessionId ?: 0
            binding.visualizerView.linkAudioSession(audioSession)
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
        showSidebar()
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

    // Helper: Checks if view is descendant of another view
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
    // REMOTE CONTROL KEYS (D-PAD, CH+, CH-, VOL)
    // ==========================================
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // When sidebar is OPEN on TV:
        if (isSidebarVisible) {
            when (keyCode) {
                // Back button closes sidebar
                KeyEvent.KEYCODE_BACK -> {
                    hideSidebar()
                    return true
                }

                // If currently focused on a Channel item, LEFT and RIGHT switch category tab!
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
            // For all other keys (including navigating inside categoriesRecyclerView),
            // let standard Android Focus Manager move focus naturally!
            return super.onKeyDown(keyCode, event)
        }

        // When sidebar is CLOSED:
        when (keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                playbackManager.playNext()
                return true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                playbackManager.playPrevious()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                playbackManager.playPrevious()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                playbackManager.playNext()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                showSidebar()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (backPressedOnce) {
                    finish()
                } else {
                    backPressedOnce = true
                    Toast.makeText(this, getString(R.string.press_back_again), Toast.LENGTH_SHORT).show()
                    handler.postDelayed({ backPressedOnce = false }, 2000)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        binding.visualizerView.release()
        playbackManager.release()
    }
}