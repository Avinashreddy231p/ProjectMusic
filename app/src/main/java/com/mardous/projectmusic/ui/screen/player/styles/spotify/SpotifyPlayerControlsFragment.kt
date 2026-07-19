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

package com.mardous.projectmusic.ui.screen.player.styles.spotify

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.action.NowPlayingAction
import com.mardous.projectmusic.core.model.player.PlayerColorScheme
import com.mardous.projectmusic.core.model.player.PlayerColorSchemeMode
import com.mardous.projectmusic.core.model.player.PlayerTintTarget
import com.mardous.projectmusic.core.model.player.iconButtonTintTarget
import com.mardous.projectmusic.core.model.player.tintTarget
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.databinding.FragmentSpotifyPlayerPlaybackControlsBinding
import com.mardous.projectmusic.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.projectmusic.ui.component.base.AbsPlayerControlsFragment
import com.mardous.projectmusic.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.projectmusic.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.projectmusic.ui.component.compose.decoration.FadingEdges
import com.mardous.projectmusic.ui.component.views.MusicSlider
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.screen.lyrics.LyricsSurface
import com.mardous.projectmusic.ui.screen.lyrics.LyricsViewModel
import com.mardous.projectmusic.ui.component.base.AbsPlayerFragment
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlinx.coroutines.flow.combine
import java.util.LinkedList

class SpotifyPlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_spotify_player_playback_controls) {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private var _binding: FragmentSpotifyPlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val playPauseFab: FloatingActionButton
        get() = binding.playPauseButton

    override val repeatButton: MaterialButton?
        get() = binding.repeatButton

    override val shuffleButton: MaterialButton?
        get() = binding.shuffleButton

    override val musicSlider: MusicSlider?
        get() = binding.progressSlider

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songTitleView: TextView?
        get() = binding.title

    override val songArtistView: TextView?
        get() = binding.text

    override val songInfoView: TextView?
        get() = null

    private var isLyricsExpanded = false
    private var lyricsContentColorState by mutableStateOf(androidx.compose.ui.graphics.Color.White)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSpotifyPlayerPlaybackControlsBinding.bind(view)
        
        binding.playPauseButton.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_PREVIOUS))

        setViewAction(binding.minusButton, NowPlayingAction.ToggleFavoriteState)
        setViewAction(binding.plusButton, NowPlayingAction.AddToPlaylist)
        setViewAction(binding.deviceButton, NowPlayingAction.SoundSettings)
        setViewAction(binding.shareButton, NowPlayingAction.Share)
        setViewAction(binding.queueButton, NowPlayingAction.OpenPlayQueue)

        binding.expandLyricsButton.setOnClickListener {
            (parentFragment as? SpotifyPlayerFragment)?.showFullLyrics()
        }

        binding.lyricsPreviewCard.setOnClickListener {
            (parentFragment as? SpotifyPlayerFragment)?.showFullLyrics()
        }

        val cardTouchListener = object : View.OnTouchListener {
            private var startRawY = 0f
            private val swipeThreshold = 50
            private var isDragging = false
            private var velocityTracker: android.view.VelocityTracker? = null

            @android.annotation.SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: android.view.MotionEvent): Boolean {
                if (velocityTracker == null) {
                    velocityTracker = android.view.VelocityTracker.obtain()
                }
                velocityTracker?.addMovement(event)

                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        startRawY = event.rawY
                        isDragging = false
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        return true
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val deltaY = event.rawY - startRawY
                        if (deltaY < -swipeThreshold || isDragging) {
                            if (!isDragging) {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                isDragging = true
                            }
                            (playerFragment as? SpotifyPlayerFragment)?.onLyricsDragProgress(deltaY, false)
                            return true
                        }
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        velocityTracker?.computeCurrentVelocity(1000)
                        val velocityY = velocityTracker?.yVelocity ?: 0f
                        
                        if (isDragging) {
                            val deltaY = event.rawY - startRawY
                            (playerFragment as? SpotifyPlayerFragment)?.onLyricsDragProgress(deltaY, true, velocityY)
                            isDragging = false
                        } else if (event.action == android.view.MotionEvent.ACTION_UP) {
                            if (v.id == R.id.lyricsPreviewCard) {
                                v.performClick()
                            } else {
                                binding.lyricsPreviewCard.performClick()
                            }
                        }
                        
                        velocityTracker?.recycle()
                        velocityTracker = null
                        isDragging = false
                    }
                }
                return false
            }
        }

        binding.lyricsPreviewCard.setOnTouchListener(cardTouchListener)
        binding.lyricsLabel.setOnTouchListener(cardTouchListener)

        binding.lyricsComposeView.setContent {
            ProjectMusicTheme {
                val uiState by lyricsViewModel.lyricsUiState.collectAsState()
                val settings by lyricsViewModel.playerLyricsViewSettings.collectAsState()
                val isPlaying by playerViewModel.isPlayingFlow.collectAsState(initial = false)

                LyricsSurface(
                    playerViewModel = playerViewModel,
                    uiState = uiState,
                    settings = settings.copy(
                        isCenterCurrentLine = true,
                        isCenterHorizontally = true
                    ),
                    contentPadding = PaddingValues(16.dp),
                    fadingEdges = FadingEdges(top = 16.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center,
                    isPlaying = isPlaying,
                    isPowerSaveMode = false,
                    hasBackgroundEffects = false,
                    contentColor = lyricsContentColorState,
                    userScrollEnabled = false,
                    onDrag = { deltaY, finished, velY ->
                        (playerFragment as? SpotifyPlayerFragment)?.onLyricsDragProgress(deltaY, finished, velY)
                    },
                    onSeekToLine = {
                        playerViewModel.seekTo(it.start)
                        if (settings.resumeOnSeek) {
                            playerViewModel.play()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(if (isLyricsExpanded) 600.dp else 200.dp)
                )
            }
        }
    }

    private var lastFavoriteState: Boolean? = null

    internal fun updateFavoriteButton(isFavorite: Boolean, withAnimation: Boolean) {
        if (lastFavoriteState == isFavorite) return
        lastFavoriteState = isFavorite

        val context = context ?: return
        val iconRes = if (isFavorite) {
            if (withAnimation) R.drawable.avd_favorite else R.drawable.ic_favorite_24dp
        } else {
            if (withAnimation) R.drawable.avd_unfavorite else R.drawable.ic_favorite_outline_24dp
        }
        
        val drawable = ContextCompat.getDrawable(context, iconRes)
        binding.minusButton.icon = drawable
        
        if (withAnimation) {
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }

            // Polished Micro-Bounce
            binding.minusButton.animate()
                .scaleX(if (isFavorite) 1.5f else 0.8f)
                .scaleY(if (isFavorite) 1.5f else 0.8f)
                .setDuration(150)
                .withEndAction {
                    binding.minusButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(400)
                        .setInterpolator(android.view.animation.OvershootInterpolator(4f))
                        .start()
                }
                .start()

            if (isFavorite) {
                binding.minusButton.setIconTintResource(R.color.spotifyGreen)
                binding.minusButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                val onSurface = playerViewModel.colorSchemeFlow.value.onSurfaceColor
                binding.minusButton.iconTint = ColorStateList.valueOf(onSurface)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    binding.minusButton.performHapticFeedback(HapticFeedbackConstants.REJECT)
                }
            }
        } else {
            if (isFavorite) {
                binding.minusButton.setIconTintResource(R.color.spotifyGreen)
            } else {
                val onSurface = playerViewModel.colorSchemeFlow.value.onSurfaceColor
                binding.minusButton.iconTint = ColorStateList.valueOf(onSurface)
            }
        }
    }

    override fun onSongInfoChanged(currentSong: Song, nextSong: Song) {
        _binding?.let { nonNullBinding ->
            nonNullBinding.title.text = currentSong.title
            nonNullBinding.text.text = getSongArtist(currentSong)
        }
    }

    override fun onExtraInfoChanged(extraInfo: String?) {
        // Not used in this style
    }

    override fun onUpdatePlayPause(isPlaying: Boolean) {
        _binding?.let { nonNullBinding ->
            val context = context ?: return@let
            val avdRes = if (isPlaying) R.drawable.avd_play else R.drawable.avd_pause
            val drawable = ContextCompat.getDrawable(context, avdRes)
            if (drawable is AnimatedVectorDrawable) {
                nonNullBinding.playPauseButton.setImageDrawable(drawable)
                drawable.start()
            } else {
                nonNullBinding.playPauseButton.setImageResource(if (isPlaying) R.drawable.ic_pause_48dp else R.drawable.ic_play_48dp)
            }
            
            if (!isPlaying) {
                nonNullBinding.playPauseButton.setPadding(8, 0, 0, 0)
            } else {
                nonNullBinding.playPauseButton.setPadding(0, 0, 0, 0)
            }
            
            // Apply physics-based scale animation on state change
            nonNullBinding.playPauseButton.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(150)
                .withEndAction {
                    nonNullBinding.playPauseButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }

    override fun onClick(view: View) {
        super.onClick(view)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        when (view) {
            binding.repeatButton -> playerViewModel.cycleRepeatMode()
            binding.shuffleButton -> playerViewModel.toggleShuffleMode()
            binding.playPauseButton -> playerViewModel.togglePlayPause()
        }
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldControlColor = binding.nextButton.iconTint.defaultColor
        val oldSliderColor = binding.progressSlider.currentColor
        val oldPrimaryTextColor = binding.title.currentTextColor
        val oldSecondaryTextColor = binding.text.currentTextColor
        val oldPlayPauseColor = binding.playPauseButton.backgroundTintList?.defaultColor ?: Color.WHITE

        val activeControlColor = scheme.primaryColor
        
        val lyricsCardColor = ColorUtils.blendARGB(scheme.primaryColor, scheme.surfaceColor, 0.2f)
        binding.lyricsPreviewCard.setCardBackgroundColor(lyricsCardColor)
        
        val isColorLight = ColorUtils.calculateLuminance(lyricsCardColor) > 0.5
        val lyricsTextColor = if (isColorLight) Color.BLACK else Color.WHITE
        binding.lyricsLabel.setTextColor(lyricsTextColor)
        binding.expandLyricsButton.imageTintList = ColorStateList.valueOf(lyricsTextColor)
        lyricsContentColorState = androidx.compose.ui.graphics.Color(lyricsTextColor)
        
        val isPlayButtonLight = ColorUtils.calculateLuminance(activeControlColor) > 0.5
        val playButtonIconColor = if (isPlayButtonLight) Color.BLACK else Color.WHITE
        binding.playPauseButton.imageTintList = ColorStateList.valueOf(playButtonIconColor)

        val oldShuffleColor = getPlaybackControlsColor(isShuffleModeOn)
        val newShuffleColor = if (isShuffleModeOn) activeControlColor else scheme.onSurfaceVariantColor
        
        val oldRepeatColor = getPlaybackControlsColor(isRepeatModeOn)
        val newRepeatColor = if (isRepeatModeOn) activeControlColor else scheme.onSurfaceVariantColor

        return listOfNotNull(
            binding.playPauseButton.tintTarget(oldPlayPauseColor, activeControlColor),
            binding.progressSlider.tintTarget(oldSliderColor, activeControlColor),
            binding.nextButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.previousButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.shuffleButton.iconButtonTintTarget(oldShuffleColor, newShuffleColor),
            binding.repeatButton.iconButtonTintTarget(oldRepeatColor, newRepeatColor),
            binding.minusButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.plusButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.deviceButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.shareButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.queueButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.title.tintTarget(oldPrimaryTextColor, scheme.onSurfaceColor),
            binding.text.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songCurrentProgress.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songTotalTime.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Int.dp(resources: android.content.res.Resources): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    private fun View.updateLayoutParams(block: android.view.ViewGroup.LayoutParams.() -> Unit) {
        val params = layoutParams
        block(params)
        layoutParams = params
    }
}
