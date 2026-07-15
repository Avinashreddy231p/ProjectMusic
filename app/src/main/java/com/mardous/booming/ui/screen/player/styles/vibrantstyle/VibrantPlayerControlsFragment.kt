package com.mardous.booming.ui.screen.player.styles.vibrantstyle

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.core.model.player.PlayerTintTarget
import com.mardous.booming.core.model.player.iconButtonTintTarget
import com.mardous.booming.core.model.player.tintTarget
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentVibrantPlayerPlaybackControlsBinding
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.booming.ui.component.compose.decoration.FadingEdges
import com.mardous.booming.ui.component.views.MusicSlider
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.lyrics.LyricsSurface
import com.mardous.booming.ui.screen.lyrics.LyricsViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

import com.mardous.booming.ui.component.base.AbsPlayerFragment
import com.mardous.booming.ui.screen.player.PlayerGesturesController

import com.google.android.material.shape.CornerFamily
import com.mardous.booming.util.Preferences

class VibrantPlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_vibrant_player_playback_controls) {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private var _binding: FragmentVibrantPlayerPlaybackControlsBinding? = null
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVibrantPlayerPlaybackControlsBinding.bind(view)
        
        val gesturesListener = (parentFragment as? AbsPlayerFragment)
        if (gesturesListener != null) {
            val gesturesController = PlayerGesturesController(
                context = requireContext(),
                acceptedGestures = setOf(
                    PlayerGesturesController.GestureType.Fling(PlayerGesturesController.GestureType.Fling.DIRECTION_UP)
                ),
                listener = gesturesListener
            )
            binding.root.setOnTouchListener { v, event ->
                // Check if touch is on an interactive view first? 
                // GestureDetector will only consume confirmed gestures.
                gesturesController.onTouch(v, event)
            }
        }

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

        binding.lyricsPreviewCard.setOnClickListener {
            (playerFragment as? VibrantPlayerFragment)?.showFullLyrics()
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
                            (playerFragment as? VibrantPlayerFragment)?.onLyricsDragProgress(deltaY, false)
                            return true
                        }
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        velocityTracker?.computeCurrentVelocity(1000)
                        val velocityY = velocityTracker?.yVelocity ?: 0f
                        
                        if (isDragging) {
                            val deltaY = event.rawY - startRawY
                            (playerFragment as? VibrantPlayerFragment)?.onLyricsDragProgress(deltaY, true, velocityY)
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
        binding.lyricsHandle.setOnTouchListener(cardTouchListener)

        setupMetadataView()
        setupLyricsPreview()

        updateLyricsCardCornerRadius()
    }

    private fun updateLyricsCardCornerRadius() {
        binding.lyricsPreviewCard.post {
            val radiusDp = Preferences.getNowPlayingLyricsCornerRadius(requireContext())
            val radius = radiusDp * resources.displayMetrics.density
            binding.lyricsPreviewCard.shapeAppearanceModel = binding.lyricsPreviewCard.shapeAppearanceModel.toBuilder()
                .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                .setTopRightCorner(CornerFamily.ROUNDED, radius)
                .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
                .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
                .build()
        }
    }

    private var metadataContentColorState by mutableStateOf(androidx.compose.ui.graphics.Color.White)
    private var metadataSecondaryColorState by mutableStateOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f))
    private var lyricsContentColorState by mutableStateOf(androidx.compose.ui.graphics.Color.White)

    private fun setupMetadataView() {
        binding.metadataComposeView.setContent {
            BoomingMusicTheme {
                val song by playerViewModel.currentSongFlow.collectAsState(initial = Song.emptySong)
                var isTitleMarqueeEnabled by remember { mutableStateOf(false) }
                var isArtistMarqueeEnabled by remember { mutableStateOf(false) }

                Column {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = metadataContentColorState,
                        maxLines = 1,
                        overflow = if (isTitleMarqueeEnabled) TextOverflow.Visible else TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isTitleMarqueeEnabled) Modifier.basicMarquee() else Modifier)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isTitleMarqueeEnabled = !isTitleMarqueeEnabled
                            }
                    )
                    Text(
                        text = "${song.artistName} • ${song.albumName}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = metadataSecondaryColorState,
                        maxLines = 1,
                        overflow = if (isArtistMarqueeEnabled) TextOverflow.Visible else TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isArtistMarqueeEnabled) Modifier.basicMarquee() else Modifier)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isArtistMarqueeEnabled = !isArtistMarqueeEnabled
                            }
                    )
                }
            }
        }
    }

    private fun setupLyricsPreview() {
        binding.lyricsComposeView.setContent {
            BoomingMusicTheme {
                val uiState by lyricsViewModel.lyricsUiState.collectAsState()
                val settings by lyricsViewModel.playerLyricsViewSettings.collectAsState()
                val isPlaying by playerViewModel.isPlayingFlow.collectAsState(initial = false)

                LyricsSurface(
                    playerViewModel = playerViewModel,
                    uiState = uiState,
                    settings = settings.copy(
                        isCenterCurrentLine = false,
                        isCenterHorizontally = true
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    fadingEdges = FadingEdges(top = 0.dp, bottom = 12.dp),
                    textAlign = TextAlign.Center,
                    isPlaying = isPlaying,
                    isPowerSaveMode = false,
                    hasBackgroundEffects = false,
                    contentColor = lyricsContentColorState,
                    userScrollEnabled = false,
                    onDrag = { deltaY, finished, velY ->
                        (playerFragment as? VibrantPlayerFragment)?.onLyricsDragProgress(deltaY, finished, velY)
                    },
                    onSeekToLine = {
                        playerViewModel.seekTo(it.start)
                        if (settings.resumeOnSeek) {
                            playerViewModel.play()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }
    }

    private var lastFavoriteState: Boolean? = null

    internal fun updateFavoriteButton(isFavorite: Boolean, withAnimation: Boolean) {
        if (lastFavoriteState == isFavorite) return
        val isInitial = lastFavoriteState == null
        lastFavoriteState = isFavorite

        val iconRes = if (isFavorite) {
            if (withAnimation) R.drawable.avd_vibrant_favorite_fill else R.drawable.ic_favorite_24dp
        } else {
            if (withAnimation) R.drawable.avd_vibrant_favorite_empty else R.drawable.ic_favorite_outline_24dp
        }
        
        val drawable = ContextCompat.getDrawable(requireContext(), iconRes)
        binding.minusButton.icon = drawable
        
        if (withAnimation && drawable is AnimatedVectorDrawable) {
            drawable.start()
            
            // Polished Micro-Bounce with Elastic Feel
            binding.minusButton.animate()
                .scaleX(if (isFavorite) 1.5f else 0.8f)
                .scaleY(if (isFavorite) 1.5f else 0.8f)
                .setDuration(150)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
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
                binding.favoriteBurstView.startAnimation()
                binding.minusButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    binding.minusButton.performHapticFeedback(HapticFeedbackConstants.REJECT)
                } else {
                    binding.minusButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }
        }
    }

    override fun onSongInfoChanged(currentSong: Song, nextSong: Song) {
        // Handled by Compose
    }

    override fun onExtraInfoChanged(extraInfo: String?) {
        // Not used
    }

    override fun onUpdatePlayPause(isPlaying: Boolean) {
        _binding?.let { nonNullBinding ->
            val avdRes = if (isPlaying) R.drawable.avd_play else R.drawable.avd_pause
            val drawable = ContextCompat.getDrawable(requireContext(), avdRes)
            if (drawable is AnimatedVectorDrawable) {
                nonNullBinding.playPauseButton.setImageDrawable(drawable)
                drawable.start()
            } else {
                nonNullBinding.playPauseButton.setImageResource(if (isPlaying) R.drawable.ic_pause_48dp else R.drawable.ic_play_48dp)
            }
            
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

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        if (key == com.mardous.booming.util.LYRICS_CARD_CORNER_RADIUS) {
            updateLyricsCardCornerRadius()
        }
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldControlColor = binding.nextButton.iconTint.defaultColor
        val oldSliderColor = binding.progressSlider.currentColor
        val oldPlayPauseColor = binding.playPauseButton.backgroundTintList?.defaultColor ?: Color.WHITE

        val accentColor = scheme.primaryColor
        
        binding.lyricsPreviewCard.setCardBackgroundColor(accentColor)
        binding.favoriteBurstView.setBurstColor(accentColor)
        val isAccentLight = ColorUtils.calculateLuminance(accentColor) > 0.5
        val contentOnAccentColor = if (isAccentLight) Color.BLACK else Color.WHITE
        binding.lyricsHandle.setBackgroundColor(ColorUtils.setAlphaComponent(contentOnAccentColor, 128))
        lyricsContentColorState = androidx.compose.ui.graphics.Color(contentOnAccentColor)
        
        val isPlayButtonLight = ColorUtils.calculateLuminance(accentColor) > 0.5
        val playButtonIconColor = if (isPlayButtonLight) Color.BLACK else Color.WHITE
        binding.playPauseButton.imageTintList = ColorStateList.valueOf(playButtonIconColor)

        // Bottom area is mostly black due to gradient
        val contentColor = Color.WHITE
        val secondaryColor = ColorUtils.setAlphaComponent(contentColor, 150)
        
        metadataContentColorState = androidx.compose.ui.graphics.Color(contentColor)
        metadataSecondaryColorState = androidx.compose.ui.graphics.Color(secondaryColor)

        val shuffleColor = if (isShuffleModeOn) Color.WHITE else secondaryColor
        val repeatColor = if (isRepeatModeOn) Color.WHITE else secondaryColor

        return listOfNotNull(
            binding.playPauseButton.tintTarget(oldPlayPauseColor, accentColor),
            binding.progressSlider.progressView?.tintTarget(oldSliderColor, accentColor),
            binding.nextButton.iconButtonTintTarget(oldControlColor, Color.WHITE),
            binding.previousButton.iconButtonTintTarget(oldControlColor, Color.WHITE),
            binding.shuffleButton.iconButtonTintTarget(oldControlColor, shuffleColor),
            binding.repeatButton.iconButtonTintTarget(oldControlColor, repeatColor),
            binding.minusButton.iconButtonTintTarget(oldControlColor, Color.WHITE),
            binding.plusButton.iconButtonTintTarget(oldControlColor, Color.WHITE),
            binding.deviceButton.iconButtonTintTarget(oldControlColor, Color.WHITE),
            binding.shareButton.iconButtonTintTarget(oldControlColor, Color.WHITE),
            binding.queueButton.iconButtonTintTarget(oldControlColor, Color.WHITE),
            binding.songCurrentProgress.tintTarget(oldControlColor, secondaryColor),
            binding.songTotalTime.tintTarget(oldControlColor, secondaryColor)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
