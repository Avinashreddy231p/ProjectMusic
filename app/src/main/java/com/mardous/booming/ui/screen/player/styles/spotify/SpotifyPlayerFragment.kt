/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  Is not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.screen.player.styles.spotify

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.mardous.booming.R
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.core.model.player.PlayerColorSchemeMode
import com.mardous.booming.core.model.player.PlayerTintTarget
import com.mardous.booming.core.model.player.tintTarget
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.databinding.FragmentSpotifyPlayerBinding
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.AbsPlayerFragment
import com.mardous.booming.ui.component.compose.decoration.FadingEdges
import com.mardous.booming.ui.screen.lyrics.LyricsSurface
import com.mardous.booming.ui.screen.lyrics.LyricsViewModel
import com.mardous.booming.ui.screen.lyrics.SpotifyLyricsScreen
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.util.Preferences
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import androidx.core.view.doOnPreDraw
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

class SpotifyPlayerFragment : AbsPlayerFragment(R.layout.fragment_spotify_player) {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private var _binding: FragmentSpotifyPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: SpotifyPlayerControlsFragment

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.Spotify)

    override val playerToolbar: Toolbar
        get() = binding.toolbar

    override val blurView: ImageView?
        get() = null

    private var primaryControlColor: Int = 0
    private var currentAmbientColor: Int = Color.BLACK
    private var colorAnimator: ValueAnimator? = null

    private enum class GestureState { IDLE, DRAGGING_UP, DRAGGING_DOWN, ANIMATING }
    private var gestureState = GestureState.IDLE

    private var transitionProgress = 0f
    private var dragStartY = 0f
    private var startScale = 0.9f
    private var hasCrossedThreshold = false

    private val activeAnimations = mutableMapOf<String, SpringAnimation>()

    private val TRANSITION_PROPERTY = object : androidx.dynamicanimation.animation.FloatPropertyCompat<SpotifyPlayerFragment>("lyricsTransition") {
        override fun getValue(fragment: SpotifyPlayerFragment): Float = fragment.transitionProgress
        override fun setValue(fragment: SpotifyPlayerFragment, value: Float) {
            fragment.updateLyricsTransition(value)
        }
    }

    private fun updateLyricsTransition(progress: Float) {
        transitionProgress = progress
        val fullLyricsView = binding.fullLyricsComposeView
        val controlsView = controlsFragment.view
        val coverView = whichFragment<androidx.fragment.app.Fragment>(R.id.playerAlbumCoverFragment)?.view
        val toolbarView = binding.toolbar

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
        val backgroundScale = 1f - (progress * 0.1f)

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
        _binding = FragmentSpotifyPlayerBinding.bind(view)
        setupToolbar()
        inflateMenuInView(playerToolbar)

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect { song ->
                playerToolbar.title = song.albumName
                resetGestureState()
            }
        }

        setupFullLyricsView()
    }

    private fun setupToolbar() {
        playerToolbar.setNavigationOnClickListener {
            if (binding.fullLyricsComposeView.isVisible) {
                hideFullLyrics()
            } else {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupFullLyricsView() {
        binding.fullLyricsComposeView.setContent {
            BoomingMusicTheme {
                SpotifyLyricsScreen(
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
            val previewCard = controlsFragment.view?.findViewById<View>(R.id.lyricsPreviewCard)
            val parentRect = Rect()
            binding.root.getGlobalVisibleRect(parentRect)
            val cardRect = Rect()
            previewCard?.getGlobalVisibleRect(cardRect)
            
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
            val previewCard = controlsFragment.view?.findViewById<View>(R.id.lyricsPreviewCard)
            val parentRect = Rect()
            binding.root.getGlobalVisibleRect(parentRect)
            val cardRect = Rect()
            previewCard?.getGlobalVisibleRect(cardRect)
            dragStartY = if (cardRect.top > 0) (cardRect.top - parentRect.top).toFloat() else binding.root.height * 0.8f
            startScale = if (cardRect.width() > 0) cardRect.width().toFloat() / binding.root.width.toFloat() else 0.9f
        }
        
        // Disable swipe down to dismiss player while lyrics are shown
        (activity as? com.mardous.booming.ui.component.base.AbsSlidingMusicPanelActivity)
            ?.getBottomSheetBehavior()?.isDraggable = false

        animateToProgress(1f)

        playerToolbar.setNavigationIcon(R.drawable.ic_close_24dp)
    }

    fun hideFullLyrics(fromDrag: Boolean = false) {
        // Re-enable swipe down
        (activity as? com.mardous.booming.ui.component.base.AbsSlidingMusicPanelActivity)
            ?.getBottomSheetBehavior()?.isDraggable = true

        animateToProgress(0f, stiffness = SpringForce.STIFFNESS_MEDIUM)

        playerToolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_down_24dp)
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
        val oldPrimaryControlColor = primaryControlColor
        primaryControlColor = scheme.onSurfaceColor
        
        // Use a more vibrant blend for the ambient top color
        val targetAmbientColor = ColorUtils.blendARGB(scheme.primaryColor, scheme.surfaceColor, 0.4f)
        
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentAmbientColor, targetAmbientColor).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                currentAmbientColor = color
                val backgroundGradient = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(currentAmbientColor, Color.BLACK)
                )
                binding.coordinatorLayout.background = backgroundGradient
                if (binding.fullLyricsComposeView.isVisible) {
                    binding.fullLyricsComposeView.setBackgroundColor(currentAmbientColor)
                }
            }
            start()
        }

        return mutableListOf(
            binding.toolbar.tintTarget(oldPrimaryControlColor, scheme.onSurfaceColor)
        ).also {
            it.addAll(playerControlsFragment.getTintTargets(scheme))
        }
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.removeItem(R.id.action_favorite)
        menu.removeItem(R.id.action_show_lyrics)
        menu.removeItem(R.id.action_playing_queue)
        
        menu.add(Menu.NONE, R.id.action_show_lyrics, Menu.NONE, R.string.lyrics)
            .setIcon(R.drawable.ic_lyrics_24dp)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_show_lyrics) {
            showFullLyrics()
            return true
        }
        return super.onMenuItemClick(menuItem)
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment<SpotifyPlayerControlsFragment>(R.id.playbackControlsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
