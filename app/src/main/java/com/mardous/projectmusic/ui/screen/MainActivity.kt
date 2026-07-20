package com.mardous.projectmusic.ui.screen

import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import android.view.ViewAnimationUtils
import android.view.animation.AnticipateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.CategoryInfo
import com.mardous.projectmusic.core.model.MediaEvent
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.extensions.currentFragment
import com.mardous.projectmusic.extensions.navigation.isValidCategory
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.extensions.whichFragment
import com.mardous.projectmusic.playback.Playback
import com.mardous.projectmusic.playback.library.MediaIDs
import com.mardous.projectmusic.core.model.shuffle.OpenShuffleMode
import com.mardous.projectmusic.ui.IScrollHelper
import com.mardous.projectmusic.ui.component.base.AbsSlidingMusicPanelActivity
import com.mardous.projectmusic.ui.screen.update.UpdateDialog
import com.mardous.projectmusic.ui.screen.update.UpdateSearchResult
import com.mardous.projectmusic.ui.screen.update.UpdateViewModel
import com.mardous.projectmusic.util.Preferences
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

/**
 * @author Christians M. A. (mardous)
 */
class MainActivity : AbsSlidingMusicPanelActivity(), MediaController.Listener {

    private lateinit var navGestureDetector: GestureDetector
    private val updateViewModel: UpdateViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val centerX = splashScreenView.view.width / 2
            val centerY = splashScreenView.view.height / 2
            val startRadius = Math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat()

            val revealAnim = ViewAnimationUtils.createCircularReveal(
                splashScreenView.view, centerX, centerY, startRadius, 0f
            ).apply {
                duration = 500
                interpolator = AnticipateInterpolator()
                doOnEnd { splashScreenView.remove() }
            }

            revealAnim.start()
        }

        title = null

        updateTabs()
        setupNavigationController()

        val shortcutManager = getSystemService<ShortcutManager>()
        shortcutManager?.removeDynamicShortcuts(OLD_SHORTCUT_IDS)

        prepareUpdateViewModel()
    }

    override fun onConnected(controller: MediaController) {
        super.onConnected(controller)
        intent?.let { handlePlaybackIntent(it, true) }
    }

    @OptIn(UnstableApi::class)
    override fun onCustomCommand(
        controller: MediaController,
        command: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        val sessionResult = when (command.customAction) {
            Playback.EVENT_MEDIA_CONTENT_CHANGED -> {
                playerViewModel.submitEvent(MediaEvent.MediaContentChanged)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            Playback.EVENT_FAVORITE_CONTENT_CHANGED -> {
                playerViewModel.submitEvent(MediaEvent.FavoriteContentChanged)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            Playback.EVENT_SHOW_LYRICS -> {
                playerViewModel.submitEvent(MediaEvent.ShowLyrics)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            Playback.EVENT_PLAYBACK_STARTED -> {
                playerViewModel.submitEvent(MediaEvent.PlaybackStarted)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            Playback.EVENT_PLAYBACK_RESTORED -> {
                playerViewModel.submitEvent(MediaEvent.PlaybackRestored)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            else -> SessionResult(SessionError.ERROR_NOT_SUPPORTED)
        }
        return Futures.immediateFuture(sessionResult)
    }

    fun scanAllPaths() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.scan_media)
            .setMessage(R.string.scan_media_message)
            .setPositiveButton(R.string.scan_media_positive) { _, _ ->
                libraryViewModel.scanAllPaths(this).observe(this) {
                    // TODO show detailed info about scanned songs
                    showToast(R.string.scan_finished)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupNavigationController() {
        val navController = whichFragment<NavHostFragment>(R.id.fragment_container).navController
        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.graph_main)

        val categoryInfo: CategoryInfo = Preferences.libraryCategories.first { it.visible }
        if (categoryInfo.visible) {
            val lastPage = Preferences.lastPage
            if (!navGraph.isValidCategory(lastPage)) {
                Preferences.lastPage = categoryInfo.category.id
                navGraph.setStartDestination(categoryInfo.category.id)
            } else {
                navGraph.setStartDestination(
                    if (Preferences.isRememberLastPage) {
                        lastPage.let {
                            if (it == 0) {
                                categoryInfo.category.id
                            } else {
                                it
                            }
                        }
                    } else categoryInfo.category.id
                )
            }
        }

        navController.graph = navGraph
        navigationView.setupWithNavController(navController)
        // Scroll Fragment to top
        navigationView.setOnItemReselectedListener {
            currentFragment(R.id.fragment_container).apply {
                if (this is IScrollHelper) {
                    scrollToTop()
                }
            }
        }

        navGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 50
            private val SWIPE_VELOCITY_THRESHOLD = 50

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            switchToNextTab()
                        } else {
                            switchToPreviousTab()
                        }
                        return true
                    }
                }
                return false
            }
        })

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == navGraph.startDestinationId) {
                currentFragment(R.id.fragment_container)?.enterTransition = null
            }
            if (destination.navigatorName == "dialog") {
                return@addOnDestinationChangedListener
            }
            when (destination.id) {
                R.id.nav_home,
                R.id.nav_songs,
                R.id.nav_albums,
                R.id.nav_artists,
                R.id.nav_folders,
                R.id.nav_playlists,
                R.id.nav_genres,
                R.id.nav_years -> {
                    // Save the last tab
                    if (Preferences.isRememberLastPage) {
                        saveTab(destination.id)
                    }
                    // Show Bottom Navigation Bar
                    setBottomNavVisibility(visible = true, animate = true)
                }

                R.id.nav_queue,
                R.id.nav_lyrics_editor,
                R.id.nav_play_info,
                R.id.nav_stats,
                R.id.nav_about -> {
                    setBottomNavVisibility(visible = false, hideBottomSheet = true)
                }

                else -> setBottomNavVisibility(visible = false, animate = true) // Hide Bottom Navigation Bar
            }
        }
    }

    private fun saveTab(id: Int) {
        if (Preferences.libraryCategories.firstOrNull { it.category.id == id }?.visible == true) {
            Preferences.lastPage = id
        }
    }

    private fun switchToNextTab() {
        val currentId = navigationView.selectedItemId
        val menu = navigationView.menu
        val visibleItems = mutableListOf<android.view.MenuItem>()
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.isVisible) visibleItems.add(item)
        }
        val currentIndex = visibleItems.indexOfFirst { it.itemId == currentId }
        if (currentIndex != -1 && currentIndex < visibleItems.size - 1) {
            navigationView.selectedItemId = visibleItems[currentIndex + 1].itemId
        }
    }

    private fun switchToPreviousTab() {
        val currentId = navigationView.selectedItemId
        val menu = navigationView.menu
        val visibleItems = mutableListOf<android.view.MenuItem>()
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.isVisible) visibleItems.add(item)
        }
        val currentIndex = visibleItems.indexOfFirst { it.itemId == currentId }
        if (currentIndex > 0) {
            navigationView.selectedItemId = visibleItems[currentIndex - 1].itemId
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePlaybackIntent(intent, false)
    }

    private fun handlePlaybackIntent(intent: Intent, canRestorePlayback: Boolean) {
        when (intent.action) {
            APP_SHORTCUT_LAST_ADDED -> {
                playerViewModel.playMediaId(MediaIDs.LAST_ADDED)
                setIntent(Intent())
            }
            APP_SHORTCUT_TOP_TRACKS -> {
                playerViewModel.playMediaId(MediaIDs.TOP_TRACKS)
                setIntent(Intent())
            }
            APP_SHORTCUT_SHUFFLE -> {
                playerViewModel.playMediaId(MediaIDs.SONGS, true)
                setIntent(Intent())
            }
            APP_SHORTCUT_FAVORITES -> {
                playerViewModel.playMediaId(MediaIDs.FAVORITES, true)
                setIntent(Intent())
            }
            else -> {
                libraryViewModel.handleIntent(intent).observe(this) { result ->
                    if (result.handled) {
                        if (result.songs.isNotEmpty()) {
                            playerViewModel.openQueue(
                                queue = result.songs,
                                position = result.position,
                                shuffleMode = OpenShuffleMode.Off
                            )
                        }
                        setIntent(Intent())
                    } else if (canRestorePlayback) {
                        playerViewModel.restorePlayback()
                    }
                    if (result.failed) {
                        showToast(R.string.unplayable_file)
                    }
                }
            }
        }
    }

    private fun prepareUpdateViewModel() {
        updateViewModel.run {
            updateEventObservable.observe(this@MainActivity) { event ->
                event.getContentIfNotConsumed()?.let { result ->
                    when (result.state) {
                        UpdateSearchResult.State.Completed -> {
                            val release = result.data ?: return@let
                            if (result.wasFromUser || release.isDownloadable(this@MainActivity)) {
                                val existingDialog = supportFragmentManager.findFragmentByTag("UPDATE_FOUND")
                                if (existingDialog == null) {
                                    UpdateDialog().show(supportFragmentManager, "UPDATE_FOUND")
                                }
                            }
                        }
                        UpdateSearchResult.State.Failed -> {
                            if (result.wasFromUser) {
                                showToast(R.string.could_not_check_for_updates)
                            }
                        }
                        else -> {}
                    }
                }
            }
            updateEvent?.peekContent().let { updateState ->
                if (updateState == null || updateState.state == UpdateSearchResult.State.Idle) {
                    if (NetworkFeature.Updater.isAvailable) {
                        searchForUpdate(false)
                    }
                }
            }
        }
    }

    companion object {
        private const val APP_SHORTCUT_LAST_ADDED = "com.mardous.projectmusic.shortcut.LAST_ADDED"
        private const val APP_SHORTCUT_TOP_TRACKS = "com.mardous.projectmusic.shortcut.TOP_TRACKS"
        private const val APP_SHORTCUT_SHUFFLE = "com.mardous.projectmusic.shortcut.SHUFFLE"
        private const val APP_SHORTCUT_FAVORITES = "com.mardous.projectmusic.shortcut.FAVORITES"

        private val OLD_SHORTCUT_IDS = listOf(
            "com.mardous.projectmusic.appshortcuts.id.last_added",
            "com.mardous.projectmusic.appshortcuts.id.top_tracks",
            "com.mardous.projectmusic.appshortcuts.id.shuffle_all",
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (::navGestureDetector.isInitialized && navigationView.visibility == android.view.View.VISIBLE) {
            val location = IntArray(2)
            navigationView.getLocationOnScreen(location)
            val rect = android.graphics.Rect(location[0], location[1], location[0] + navigationView.width, location[1] + navigationView.height)
            if (rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                if (navGestureDetector.onTouchEvent(ev)) {
                    // Send an ACTION_CANCEL to child views so they drop any pressed state
                    val cancelEvent = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
                    super.dispatchTouchEvent(cancelEvent)
                    cancelEvent.recycle()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}