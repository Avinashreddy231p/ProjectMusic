package com.mardous.booming.ui.screen.lyrics

import android.app.Activity
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.mardous.booming.R
import com.mardous.booming.core.model.LibraryMargin
import com.mardous.booming.core.model.lyrics.LyricsViewSettings
import com.mardous.booming.core.model.lyrics.LyricsViewSettings.BackgroundEffect
import com.mardous.booming.core.model.lyrics.LyricsViewState
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.SyncedLyrics
import com.mardous.booming.extensions.isPowerSaveMode
import com.mardous.booming.extensions.resolveColor
import com.mardous.booming.extensions.resources.ensureContrastAgainst
import com.mardous.booming.ui.component.compose.AnimatedEqBars
import com.mardous.booming.ui.component.compose.color.extractGradientColors
import com.mardous.booming.ui.component.compose.decoration.FadingEdges
import com.mardous.booming.ui.component.compose.decoration.animatedGradient
import com.mardous.booming.ui.component.compose.decoration.fadingEdges
import com.mardous.booming.ui.component.compose.lyrics.LyricsView
import com.mardous.booming.ui.component.views.PlaceholderDrawable
import com.mardous.booming.ui.screen.library.LibraryViewModel

import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.PlayerTheme
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinActivityViewModel

import android.view.View
import com.mardous.booming.extensions.getShareSongIntent
import com.mardous.booming.extensions.toChooser
import kotlin.math.roundToInt

sealed class LyricsUiState(open val id: Long) {
    data class Loading(override val id: Long) : LyricsUiState(id)
    data class Empty(override val id: Long) : LyricsUiState(id)
    data class Instrumental(override val id: Long) : LyricsUiState(id)
    data class Plain(override val id: Long, val lyrics: String) : LyricsUiState(id)
    data class Synced(override val id: Long, val syncedLyrics: SyncedLyrics) : LyricsUiState(id)
}

@Composable
private fun rememberLyricsViewState(lyrics: SyncedLyrics): LyricsViewState {
    return remember(lyrics) { LyricsViewState(lyrics) }
}

@Composable
fun rememberSmoothPlaybackPosition(
    playerPosition: Long,
    playbackSpeed: Float,
    isPlaying: Boolean
): State<Long> {
    val position = remember { mutableLongStateOf(playerPosition) }
    LaunchedEffect(playerPosition, isPlaying) {
        val baseRealtime = SystemClock.elapsedRealtime()
        if (!isPlaying) {
            position.longValue = playerPosition
            return@LaunchedEffect
        }

        while (isActive) {
            withFrameNanos {
                val elapsed = SystemClock.elapsedRealtime() - baseRealtime
                position.longValue = playerPosition + (elapsed * playbackSpeed).toLong()
            }
        }
    }

    return position
}

@Composable
fun LyricsScreen(
    libraryViewModel: LibraryViewModel = koinActivityViewModel(),
    lyricsViewModel: LyricsViewModel = koinActivityViewModel(),
    playerViewModel: PlayerViewModel = koinActivityViewModel(),
    onEditClick: (Song) -> Unit
) {
    val context = LocalContext.current
    val isPowerSaveMode = context.isPowerSaveMode()

    val miniPlayerMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))

    val lyricsViewSettings by lyricsViewModel.fullLyricsViewSettings.collectAsState()
    val uiState by lyricsViewModel.lyricsUiState.collectAsState()

    val song by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    LaunchedEffect(song) {
        if (isPowerSaveMode)
            return@LaunchedEffect

        if (lyricsViewSettings.backgroundEffect == BackgroundEffect.Gradient) {
            withContext(Dispatchers.Default) {
                val result = SingletonImageLoader.get(context).execute(
                    ImageRequest.Builder(context)
                        .data(song)
                        .build()
                )
                gradientColors = if (result is SuccessResult) {
                    result.image.toBitmap().extractGradientColors(
                        context.resolveColor(PlaceholderDrawable.BACKGROUND_COLOR)
                    )
                } else {
                    emptyList()
                }
            }
        }
    }

    var hasBackgroundEffects by remember { mutableStateOf(false) }
    val contentColor = if (hasBackgroundEffects && gradientColors.isNotEmpty()) {
        if (gradientColors[0].luminance() > 0.5f) Color.Black else Color.White
    } else null

    Scaffold(
        contentWindowInsets = WindowInsets
            .navigationBars
            .add(WindowInsets(bottom = miniPlayerMargin.totalMargin)),
        containerColor = Color.Transparent, // Transparent to show background
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEditClick(song) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_note_24dp),
                    contentDescription = stringResource(R.string.action_lyrics_editor)
                )
            }
        },
        modifier = Modifier.keepScreenOn()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = Pair(lyricsViewSettings.backgroundEffect, gradientColors),
                transitionSpec = {
                    fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000)))
                }
            ) { (effect, gradientColors) ->
                when {
                    effect.isGradient && gradientColors.size >= 2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .animatedGradient(gradientColors, isPlaying)
                        )
                        hasBackgroundEffects = true
                    }

                    effect.isBlur -> {
                        val backgroundColor = Color(0xFF1A1A1A)

                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = song,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(90.dp)
                                    .drawWithContent {
                                        drawContent()

                                        drawRect(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    backgroundColor.copy(alpha = 0.8f),
                                                    backgroundColor
                                                ),
                                                radius = size.minDimension * 0.9f
                                            )
                                        )
                                    }
                            )
                        }
                        hasBackgroundEffects = true
                    }

                    else -> {
                        hasBackgroundEffects = false
                    }
                }
            }

            LyricsSurface(
                playerViewModel = playerViewModel,
                uiState = uiState,
                settings = lyricsViewSettings,
                PaddingValues(vertical = 96.dp, horizontal = 16.dp),
                fadingEdges = FadingEdges(top = 56.dp, bottom = 32.dp),
                textAlign = TextAlign.Start,
                isPlaying = isPlaying,
                isPowerSaveMode = isPowerSaveMode,
                hasBackgroundEffects = hasBackgroundEffects,
                contentColor = contentColor,
                onSeekToLine = {
                    playerViewModel.seekTo(it.start)
                    if (lyricsViewSettings.resumeOnSeek) {
                        playerViewModel.play()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
fun SpotifyLyricsScreen(
    libraryViewModel: LibraryViewModel = koinActivityViewModel(),
    lyricsViewModel: LyricsViewModel = koinActivityViewModel(),
    playerViewModel: PlayerViewModel = koinActivityViewModel(),
    onEditClick: (Song) -> Unit,
    onCloseClick: () -> Unit,
    onMoreClick: (View) -> Unit,
    onDrag: ((Float, Boolean, Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val isPowerSaveMode = context.isPowerSaveMode()
    val scope = rememberCoroutineScope()

    val miniPlayerMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))

    val lyricsViewSettings by lyricsViewModel.fullLyricsViewSettings.collectAsState()
    val uiState by lyricsViewModel.lyricsUiState.collectAsState()

    val song by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()

    val surfaceColor = MaterialTheme.colorScheme.surface
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    var dominantColor by remember { mutableStateOf(surfaceColor) }
    
    LaunchedEffect(song) {
        withContext(Dispatchers.Default) {
            val result = SingletonImageLoader.get(context).execute(
                ImageRequest.Builder(context)
                    .data(song)
                    .build()
            )
            if (result is SuccessResult) {
                val colors = result.image.toBitmap().extractGradientColors(
                    context.resolveColor(PlaceholderDrawable.BACKGROUND_COLOR)
                )
                if (colors.isNotEmpty()) {
                    dominantColor = colors[0]
                    gradientColors = colors
                }
            }
        }
    }

    var hasBackgroundEffects by remember { mutableStateOf(false) }
    
    val isLight = !isSystemInDarkTheme()
    
    // Match Spotify lyrics card: blend dominant color with surface
    val targetBackgroundColor = remember(dominantColor, isLight) {
        if (isLight) {
            Color(ColorUtils.blendARGB(dominantColor.toArgb(), Color.White.toArgb(), 0.7f))
        } else {
            // More vibrant color for ambient light effect
            Color(ColorUtils.blendARGB(dominantColor.toArgb(), Color.Black.toArgb(), 0.4f))
        }
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "lyrics-background-color-animation"
    )
    val contentColor = if (isLight) Color.Black else Color.White
    val accentColor = contentColor

    var accumulatedNestedDrag by remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember(onDrag) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (accumulatedNestedDrag > 0f && available.y < 0f) {
                    val consumed = available.y.coerceAtLeast(-accumulatedNestedDrag)
                    accumulatedNestedDrag += consumed
                    onDrag?.invoke(accumulatedNestedDrag, false, 0f)
                    return androidx.compose.ui.geometry.Offset(0f, consumed)
                }
                return super.onPreScroll(available, source)
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    accumulatedNestedDrag += available.y
                    onDrag?.invoke(accumulatedNestedDrag, false, 0f)
                    return available
                }
                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (accumulatedNestedDrag > 0f) {
                    onDrag?.invoke(accumulatedNestedDrag, true, available.y)
                    accumulatedNestedDrag = 0f
                    return available
                }
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0f && accumulatedNestedDrag > 0f) {
                    onDrag?.invoke(accumulatedNestedDrag, true, available.y)
                    accumulatedNestedDrag = 0f
                    return available
                }
                if (accumulatedNestedDrag > 0f) {
                    onDrag?.invoke(accumulatedNestedDrag, true, 0f)
                    accumulatedNestedDrag = 0f
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets
            .navigationBars
            .add(WindowInsets(bottom = miniPlayerMargin.totalMargin)),
        containerColor = Color.Transparent, // Show background layer
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .keepScreenOn()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(backgroundColor.copy(alpha = 0.6f), Color.Transparent),
                        startY = 0f,
                        endY = 1500f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SpotifyLyricsTopBar(
                    song = song,
                    onCloseClick = onCloseClick,
                    contentColor = contentColor,
                    onDrag = onDrag,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.weight(1f)) {
                    // Background logic (optional effects)
                    if (lyricsViewSettings.backgroundEffect == BackgroundEffect.Gradient && gradientColors.size >= 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .animatedGradient(gradientColors, isPlaying)
                        )
                    } else if (lyricsViewSettings.backgroundEffect == BackgroundEffect.Blur) {
                        val blurBackgroundColor = Color(0xFF1A1A1A)

                        AsyncImage(
                            model = song,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(90.dp)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                blurBackgroundColor.copy(alpha = 0.8f),
                                                blurBackgroundColor
                                            ),
                                            radius = size.minDimension * 0.9f
                                        )
                                    )
                                }
                        )
                    }

                    LyricsSurface(
                        playerViewModel = playerViewModel,
                        uiState = uiState,
                        settings = lyricsViewSettings.copy(
                            isCenterCurrentLine = true,
                            syncedStyle = lyricsViewSettings.syncedStyle.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 36.sp
                            )
                        ),
                        contentPadding = PaddingValues(vertical = 32.dp, horizontal = 24.dp),
                        fadingEdges = FadingEdges(top = 48.dp, bottom = 48.dp),
                        textAlign = TextAlign.Start,
                        isPlaying = isPlaying,
                        isPowerSaveMode = isPowerSaveMode,
                        hasBackgroundEffects = true,
                        contentColor = contentColor,
                        onSeekToLine = {
                            playerViewModel.seekTo(it.start)
                            if (lyricsViewSettings.resumeOnSeek) {
                                playerViewModel.play()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SpotifyLyricsBottomControls(
                    playerViewModel = playerViewModel,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onEditClick = { onEditClick(song) },
                    onMoreClick = onMoreClick,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SpotifyLyricsTopBar(
    song: Song,
    onCloseClick: () -> Unit,
    contentColor: Color,
    onDrag: ((Float, Boolean, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var startY by remember { mutableStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .pointerInput(Unit) {
                if (onDrag == null) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        startY = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        startY += dragAmount
                        onDrag(startY, false, 0f)
                    },
                    onDragEnd = {
                        onDrag(startY, true, 0f)
                    },
                    onDragCancel = {
                        onDrag(startY, true, 0f)
                    }
                )
            }
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCloseClick) {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                contentDescription = null,
                tint = contentColor
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
fun SpotifyLyricsBottomControls(
    playerViewModel: PlayerViewModel,
    contentColor: Color,
    accentColor: Color,
    onEditClick: () -> Unit,
    onMoreClick: (View) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val progress by playerViewModel.progressFlow.collectAsStateWithLifecycle()
    val duration by playerViewModel.durationFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var sliderValue by remember { mutableStateOf(progress.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            sliderValue = progress.toFloat()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Slider(
            value = sliderValue,
            onValueChange = {
                isDragging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                isDragging = false
                playerViewModel.seekTo(sliderValue.toLong())
            },
            valueRange = 0f..duration.coerceAtLeast(0L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = contentColor,
                activeTrackColor = contentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(sliderValue.toLong()),
                color = contentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTime(duration),
                color = contentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_note_24dp),
                    contentDescription = stringResource(R.string.action_lyrics_editor),
                    tint = contentColor
                )
            }

            IconButton(
                onClick = { playerViewModel.seekToPrevious() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_previous_24dp),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { playerViewModel.togglePlayPause() },
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawCircle(color = contentColor)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp),
                        contentDescription = null,
                        tint = if (contentColor == Color.White) Color.Black else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            IconButton(
                onClick = { playerViewModel.seekToNext() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_next_24dp),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.widget.ImageButton(ctx).apply {
                        setImageResource(R.drawable.ic_more_horiz_24dp)
                        setBackgroundResource(android.R.color.transparent)
                        imageTintList = android.content.res.ColorStateList.valueOf(contentColor.toArgb())
                        setOnClickListener {
                            onMoreClick(this)
                        }
                    }
                },
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun CoverLyricsScreen(
    lyricsViewModel: LyricsViewModel,
    playerViewModel: PlayerViewModel,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPowerSaveMode = context.isPowerSaveMode()

    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()

    val lyricsViewSettings by lyricsViewModel.playerLyricsViewSettings.collectAsState()
    val uiState by lyricsViewModel.lyricsUiState.collectAsState()

    val playerColorScheme by playerViewModel.colorSchemeFlow.collectAsState(
        initial = PlayerColorScheme.themeColorScheme(context)
    )

    PlayerTheme(playerColorScheme) {
        Box(modifier = modifier.fillMaxSize()) {
            LyricsSurface(
                uiState = uiState,
                playerViewModel = playerViewModel,
                settings = lyricsViewSettings,
                contentPadding = PaddingValues(vertical = 72.dp, horizontal = 12.dp),
                fadingEdges = FadingEdges(top = 72.dp, bottom = 64.dp),
                textAlign = TextAlign.Center,
                isPlaying = isPlaying,
                isPowerSaveMode = isPowerSaveMode,
                hasBackgroundEffects = false,
                contentColor = playerColorScheme.onSurface,
                onSeekToLine = {
                    playerViewModel.seekTo(it.start)
                    if (lyricsViewSettings.resumeOnSeek) {
                        playerViewModel.play()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            FilledIconButton(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                onClick = onExpandClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_full_24dp),
                    contentDescription = stringResource(R.string.action_lyrics_editor)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsSurface(
    playerViewModel: PlayerViewModel,
    uiState: LyricsUiState,
    settings: LyricsViewSettings,
    contentPadding: PaddingValues,
    fadingEdges: FadingEdges,
    textAlign: TextAlign?,
    isPlaying: Boolean,
    isPowerSaveMode: Boolean,
    hasBackgroundEffects: Boolean,
    onSeekToLine: (SyncedLyrics.Line) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color? = null,
    userScrollEnabled: Boolean = true,
    onDrag: ((Float, Boolean, Float) -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Determine the effective background color we are drawing on
    val surfaceBackground = when {
        hasBackgroundEffects -> {
            colorScheme.surface
        }
        else -> colorScheme.surface
    }.run {
        if (this == Color.Transparent) Color.Black else this
    }

    val baseContentColor = contentColor ?: when {
        hasBackgroundEffects -> Color.White
        else -> Preferences.lyricsAccentColor?.let { Color(it) } ?: colorScheme.primary
    }
    
    val finalContentColor = baseContentColor.ensureContrastAgainst(surfaceBackground)

    var accumulatedDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier.then(
            if (!userScrollEnabled && onDrag != null) {
                Modifier.pointerInput(onDrag) {
                    detectVerticalDragGestures(
                        onDragStart = { accumulatedDrag = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            accumulatedDrag += dragAmount
                            onDrag(accumulatedDrag, false, 0f)
                            if (Math.abs(accumulatedDrag) > 10f) {
                                change.consume()
                            }
                        },
                        onDragEnd = { onDrag(accumulatedDrag, true, 0f) },
                        onDragCancel = { onDrag(accumulatedDrag, true, 0f) }
                    )
                }
            } else Modifier
        )
    ) {
        key(uiState.id) {
            when (uiState) {
                is LyricsUiState.Empty -> {
                    Text(
                        text = stringResource(R.string.no_lyrics_found),
                        color = finalContentColor,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .align(Alignment.Center)
                    )
                }

                is LyricsUiState.Loading -> {
                    CircularWavyProgressIndicator(
                        color = finalContentColor,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is LyricsUiState.Instrumental -> {
                    AnimatedEqBars(
                        color = finalContentColor,
                        isPlaying = isPlaying,
                        barCount = 5,
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.Center)
                    )
                }

                is LyricsUiState.Plain -> {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(rememberNestedScrollInteropConnection())
                            .fadingEdges(fadingEdges)
                            .verticalScroll(scrollState, enabled = userScrollEnabled)
                            .padding(contentPadding)
                    ) {
                        Text(
                            text = uiState.lyrics,
                            color = finalContentColor,
                            textAlign = textAlign,
                            style = settings.unsyncedStyle,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                is LyricsUiState.Synced -> {
                    val lyricsViewState = rememberLyricsViewState(uiState.syncedLyrics)

                    val playerPosition by playerViewModel.progressFlow.collectAsStateWithLifecycle()
                    val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()

                    val smoothProgress by rememberSmoothPlaybackPosition(
                        playerPosition = playerPosition,
                        playbackSpeed = playbackSpeed,
                        isPlaying = isPlaying
                    )

                    LaunchedEffect(smoothProgress) {
                        lyricsViewState.updatePosition(smoothProgress)
                    }

                    LyricsView(
                        state = lyricsViewState,
                        settings = settings,
                        contentPadding = contentPadding,
                        fadingEdges = fadingEdges,
                        contentColor = finalContentColor,
                        isPowerSaveMode = isPowerSaveMode,
                        hasBackgroundEffects = hasBackgroundEffects,
                        userScrollEnabled = userScrollEnabled,
                        onLineClick = { onSeekToLine(it) }
                    )
                }
            }
        }
    }
}

fun Modifier.keepScreenOn(): Modifier = composed {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    this
}
