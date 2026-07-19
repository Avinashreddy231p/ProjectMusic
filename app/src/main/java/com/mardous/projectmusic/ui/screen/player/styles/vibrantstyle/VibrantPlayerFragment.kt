package com.mardous.projectmusic.ui.screen.player.styles.vibrantstyle

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.palette.graphics.Palette
import androidx.navigation.fragment.findNavController
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.action.NowPlayingAction
import com.mardous.projectmusic.core.model.player.PlayerColorScheme
import com.mardous.projectmusic.core.model.player.PlayerColorSchemeMode
import com.mardous.projectmusic.core.model.player.PlayerTintTarget
import com.mardous.projectmusic.core.model.player.tintTarget
import com.mardous.projectmusic.core.model.theme.NowPlayingScreen
import com.mardous.projectmusic.databinding.FragmentVibrantPlayerBinding
import com.mardous.projectmusic.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.projectmusic.extensions.whichFragment
import com.mardous.projectmusic.ui.component.base.AbsPlayerControlsFragment
import com.mardous.projectmusic.ui.component.base.AbsPlayerFragment
import com.mardous.projectmusic.ui.component.compose.decoration.VibrantBackground
import com.mardous.projectmusic.ui.screen.lyrics.LyricsViewModel
import com.mardous.projectmusic.ui.screen.lyrics.VibrantLyricsScreen
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import com.mardous.projectmusic.util.Preferences
import org.koin.androidx.viewmodel.ext.android.activityViewModel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.toArgb
import com.mardous.projectmusic.core.model.PaletteColor

class VibrantPlayerFragment : AbsPlayerFragment(R.layout.fragment_vibrant_player) {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private var _binding: FragmentVibrantPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: VibrantPlayerControlsFragment

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(
            Preferences.nowPlayingScreen.takeIf { it == NowPlayingScreen.Liquid } ?: NowPlayingScreen.Vibrant
        )

    override val playerToolbar: Toolbar?
        get() = null

    override val blurView: ImageView?
        get() = null

    private var dominantColorState by mutableStateOf(androidx.compose.ui.graphics.Color.Black)
    private var primaryControlColor: Int = 0

    private enum class GestureState { IDLE, DRAGGING_UP, DRAGGING_DOWN, ANIMATING }
    private var gestureState = GestureState.IDLE

    private var transitionProgress = 0f
    private var dragStartY = 0f
    private var startScale = 0.9f
    private var hasCrossedThreshold = false

    private val activeAnimations = mutableMapOf<String, SpringAnimation>()

    private val TRANSITION_PROPERTY = object : androidx.dynamicanimation.animation.FloatPropertyCompat<VibrantPlayerFragment>("lyricsTransition") {
        override fun getValue(fragment: VibrantPlayerFragment): Float = fragment.transitionProgress
        override fun setValue(fragment: VibrantPlayerFragment, value: Float) {
            fragment.updateLyricsTransition(value)
        }
    }

    private fun updateLyricsTransition(progress: Float) {
        transitionProgress = progress
        val fullLyricsView = binding.fullLyricsComposeView
        val controlsView = controlsFragment.view
        val coverView = whichFragment<androidx.fragment.app.Fragment>(R.id.playerAlbumCoverFragment)?.view
        val toolbarView = binding.toolbarComposeView

        if (progress > 0f) {
            fullLyricsView.isVisible = true
        } else if (gestureState == GestureState.IDLE) {
            fullLyricsView.isVisible = false
        }

        // 1. Full Lyrics Animation
        fullLyricsView.alpha = progress
        fullLyricsView.translationY = dragStartY * (1f - progress)
        val currentScale = startScale + (progress * (1f - startScale))
        fullLyricsView.scaleX = currentScale
        fullLyricsView.scaleY = currentScale

        // 2. Background Elements Animation (Controls, Cover, Toolbar)
        val backgroundAlpha = 1f - progress
        val backgroundScale = 1f - (progress * 0.1f) // shrink to 0.9

        controlsView?.alpha = backgroundAlpha
        controlsView?.scaleX = backgroundScale
        controlsView?.scaleY = backgroundScale

        coverView?.alpha = backgroundAlpha
        coverView?.scaleX = backgroundScale
        coverView?.scaleY = backgroundScale

        toolbarView.alpha = backgroundAlpha
    }

    private fun cancelActiveAnimations() {
        activeAnimations.values.forEach { it.cancel() }
        activeAnimations.clear()
        binding.fullLyricsComposeView.animate().cancel()
        controlsFragment.view?.animate()?.cancel()
    }

    private fun animateToProgress(
        targetProgress: Float,
        stiffness: Float = SpringForce.STIFFNESS_LOW,
        onEnd: (() -> Unit)? = null
    ) {
        cancelActiveAnimations()
        gestureState = GestureState.ANIMATING
        
        val animation = SpringAnimation(this, TRANSITION_PROPERTY, targetProgress).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
            spring.stiffness = stiffness
            addEndListener { _, _, _, _ ->
                if (targetProgress == 0f) {
                    binding.fullLyricsComposeView.isVisible = false
                }
                resetGestureState()
                onEnd?.invoke()
            }
        }
        activeAnimations["transition"] = animation
        animation.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVibrantPlayerBinding.bind(view)
        setupToolbar()

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect {
                resetGestureState()
            }
        }

        setupBackground()
        setupFullLyricsView()
    }

    private fun setupToolbar() {
        binding.toolbarComposeView.setContent {
            ProjectMusicTheme {
                val song by playerViewModel.currentSongFlow.collectAsState(initial = com.mardous.projectmusic.data.model.Song.emptySong)
                
                val isTopColorLight = ColorUtils.calculateLuminance(dominantColorState.toArgb()) > 0.5
                val toolbarContentColor = if (isTopColorLight) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                
                com.mardous.projectmusic.ui.screen.lyrics.VibrantTopBar(
                    albumName = song.albumName,
                    onCloseClick = {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    },
                    onMenuClick = { anchorView ->
                        showNowPlayingMenu(anchorView)
                    },
                    contentColor = toolbarContentColor,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
            }
        }
    }

    private fun setupBackground() {
        binding.backgroundComposeView.setContent {
            ProjectMusicTheme {
                val isPlaying by playerViewModel.isPlayingFlow.collectAsState(initial = false)
                val nps = Preferences.nowPlayingScreen
                val mode = when (nps) {
                    NowPlayingScreen.Liquid -> com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode.Liquid
                    NowPlayingScreen.Aurora -> com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode.Aurora
                    else -> Preferences.vibrantBackgroundMode
                }

                VibrantBackground(
                    dominantColor = dominantColorState,
                    mode = mode,
                    isPlaying = isPlaying
                )
            }
        }
    }

    private fun setupFullLyricsView() {
        binding.fullLyricsComposeView.setContent {
            ProjectMusicTheme {
                VibrantLyricsScreen(
                    dominantColor = dominantColorState,
                    onEditClick = {
                        onQuickActionEvent(NowPlayingAction.LyricsEditor)
                    },
                    onCloseClick = {
                        hideFullLyrics()
                    },
                    onMoreClick = { anchorView ->
                        showNowPlayingMenu(anchorView)
                    },
                    onDrag = { deltaY, isFinished, velocityY ->
                        onLyricsCollapseDragProgress(deltaY, isFinished, velocityY)
                    }
                )
            }
        }
    }

    private fun resetGestureState() {
        gestureState = GestureState.IDLE
        hasCrossedThreshold = false
    }

    fun onLyricsDragProgress(deltaY: Float, isFinished: Boolean, velocityY: Float = 0f) {
        if (gestureState == GestureState.IDLE && !isFinished) {
            cancelActiveAnimations()
            val previewCard = controlsFragment.requireView().findViewById<View>(R.id.lyricsPreviewCard)
            val parentRect = Rect()
            binding.root.getGlobalVisibleRect(parentRect)
            val cardRect = Rect()
            previewCard.getGlobalVisibleRect(cardRect)
            
            dragStartY = if (cardRect.top > 0) (cardRect.top - parentRect.top).toFloat() else binding.root.height * 0.8f
            startScale = if (cardRect.width() > 0) cardRect.width().toFloat() / binding.root.width.toFloat() else 0.9f
            
            if (dragStartY > 0f) {
                gestureState = GestureState.DRAGGING_UP
                hasCrossedThreshold = false
                updateLyricsTransition(0f)
            }
        }

        if (gestureState == GestureState.DRAGGING_UP) {
            val currentY = (dragStartY + deltaY).coerceIn(0f, dragStartY)
            val progress = (1f - (currentY / dragStartY)).coerceIn(0f, 1f)
            updateLyricsTransition(progress)

            if (!hasCrossedThreshold && progress > 0.001f) {
                hasCrossedThreshold = true
            } else if (hasCrossedThreshold && progress < 0.001f) {
                hasCrossedThreshold = false
            }

            if (isFinished) {
                val shouldShow = when {
                    velocityY < -1000f -> true
                    velocityY > 1000f -> false
                    else -> progress > 0.001f
                }
                
                if (shouldShow) {
                    showFullLyrics(fromDrag = true)
                } else {
                    hideFullLyrics(fromDrag = true)
                }
            }
        }
    }

    fun onLyricsCollapseDragProgress(deltaY: Float, isFinished: Boolean, velocityY: Float = 0f) {
        if (gestureState == GestureState.IDLE && !isFinished) {
            cancelActiveAnimations()
            gestureState = GestureState.DRAGGING_DOWN
            hasCrossedThreshold = false
            // dragStartY remains at its expansion value
        }

        if (gestureState == GestureState.DRAGGING_DOWN) {
            val currentY = deltaY.coerceIn(0f, dragStartY)
            val progress = (1f - (currentY / dragStartY)).coerceIn(0f, 1f)
            updateLyricsTransition(progress)

            if (!hasCrossedThreshold && progress < 0.98f) {
                hasCrossedThreshold = true
            } else if (hasCrossedThreshold && progress > 0.98f) {
                hasCrossedThreshold = false
            }

            if (isFinished) {
                val shouldHide = when {
                    velocityY > 1000f -> true
                    velocityY < -1000f -> false
                    else -> progress < 0.98f
                }
                
                if (shouldHide) {
                    hideFullLyrics(fromDrag = true)
                } else {
                    showFullLyrics(fromDrag = true)
                }
            }
        }
    }

    fun showFullLyrics(fromDrag: Boolean = false) {
        if (!fromDrag) {
            // Recalculate start points if not from drag
            val previewCard = controlsFragment.requireView().findViewById<View>(R.id.lyricsPreviewCard)
            val parentRect = Rect()
            binding.root.getGlobalVisibleRect(parentRect)
            val cardRect = Rect()
            previewCard.getGlobalVisibleRect(cardRect)
            dragStartY = if (cardRect.top > 0) (cardRect.top - parentRect.top).toFloat() else binding.root.height * 0.8f
            startScale = if (cardRect.width() > 0) cardRect.width().toFloat() / binding.root.width.toFloat() else 0.9f
        }
        
        (activity as? com.mardous.projectmusic.ui.component.base.AbsSlidingMusicPanelActivity)
            ?.getBottomSheetBehavior()?.isDraggable = false

        animateToProgress(1f)
    }

    fun hideFullLyrics(fromDrag: Boolean = false) {
        (activity as? com.mardous.projectmusic.ui.component.base.AbsSlidingMusicPanelActivity)
            ?.getBottomSheetBehavior()?.isDraggable = true

        animateToProgress(0f, stiffness = SpringForce.STIFFNESS_MEDIUM)
    }

    override fun onPrepareViewGestures(view: View) {
        super.onPrepareViewGestures(view)
        val gesturesController = com.mardous.projectmusic.ui.screen.player.PlayerGesturesController(
            context = view.context,
            acceptedGestures = setOf(
                com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType.Fling(com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType.Fling.DIRECTION_UP),
                com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType.Fling(com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType.Fling.DIRECTION_LEFT),
                com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType.Fling(com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType.Fling.DIRECTION_RIGHT)
            ),
            listener = this
        )
        view.setOnTouchListener(gesturesController)
    }

    override fun gestureDetected(gestureType: com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType): Boolean {
        return when (gestureType) {
            is com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType.Fling -> {
                if (gestureType.direction == com.mardous.projectmusic.ui.screen.player.PlayerGesturesController.GestureType.Fling.DIRECTION_UP) {
                    if (!binding.fullLyricsComposeView.isVisible) {
                        findNavController().navigate(R.id.nav_queue)
                        true
                    } else false
                } else super.gestureDetected(gestureType)
            }
            else -> super.gestureDetected(gestureType)
        }
    }
    override fun onShow() {
        super.onShow()
        resetGestureState()
    }

    override fun onHide() {
        super.onHide()
        resetGestureState()
    }

    override fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        controlsFragment.updateFavoriteButton(isFavorite, withAnimation)
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        dominantColorState = androidx.compose.ui.graphics.Color(scheme.primaryColor)
        return playerControlsFragment.getTintTargets(scheme)
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.removeItem(R.id.action_favorite)
        menu.removeItem(R.id.action_show_lyrics)
        menu.removeItem(R.id.action_playing_queue)
        
        if (binding.fullLyricsComposeView.isVisible) {
            menu.add(Menu.NONE, R.id.action_vibrant_lyrics_background, Menu.NONE, "Lyrics Background")
                .setIcon(R.drawable.ic_palette_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        menu.add(Menu.NONE, R.id.action_show_lyrics, Menu.NONE, R.string.lyrics)
            .setIcon(R.drawable.ic_lyrics_24dp)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_show_lyrics -> {
                showFullLyrics()
                true
            }
            R.id.action_vibrant_lyrics_background -> {
                showVibrantLyricsBackgroundDialog()
                true
            }
            else -> super.onMenuItemClick(menuItem)
        }
    }

    private fun showVibrantLyricsBackgroundDialog() {
        val modes = com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode.entries
        val items = modes.map { it.name }.toTypedArray()
        val checkedItem = modes.indexOf(Preferences.vibrantLyricsBackgroundMode)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Lyrics Background Mode")
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                Preferences.vibrantLyricsBackgroundMode = modes[which]
                dialog.dismiss()
            }
            .show()
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
