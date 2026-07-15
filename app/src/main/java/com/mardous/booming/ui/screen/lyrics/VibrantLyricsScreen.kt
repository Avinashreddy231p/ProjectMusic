package com.mardous.booming.ui.screen.lyrics

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Velocity
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mardous.booming.R
import com.mardous.booming.core.model.LibraryMargin
import com.mardous.booming.data.model.Song
import com.mardous.booming.ui.component.compose.decoration.FadingEdges
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.player.PlayerViewModel
import org.koin.compose.viewmodel.koinActivityViewModel
import androidx.compose.runtime.livedata.observeAsState

import com.mardous.booming.extensions.isPowerSaveMode

import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import android.content.res.ColorStateList
import android.widget.ImageButton

@Composable
fun VibrantTopBar(
    albumName: String,
    onCloseClick: () -> Unit,
    onMenuClick: (View) -> Unit,
    contentColor: Color,
    onDrag: ((Float, Boolean, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var dragStartRootY by remember { mutableStateOf(0f) }
    var layoutCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates = it }
            .pointerInput(onDrag) {
                if (onDrag == null) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = layoutCoordinates?.localToRoot(down.position) ?: Offset.Zero
                    dragStartRootY = startPos.y
                    
                    var totalDelta = 0f
                    
                    drag(down.id) { change ->
                        val currentPos = layoutCoordinates?.localToRoot(change.position) ?: Offset.Zero
                        totalDelta = currentPos.y - dragStartRootY
                        onDrag(totalDelta, false, 0f)
                        if (totalDelta > 10f) { // Only consume if we are actually dragging down
                            change.consume()
                        }
                    }
                    // Handle end
                    onDrag(totalDelta, true, 0f)
                }
            }
            .statusBarsPadding()
    ) {
        // Visual Handle Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(contentColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(48.dp)
            ) {
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
                    text = "PLAYING FROM ALBUM",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    letterSpacing = 0.1.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AndroidView(
                factory = { ctx ->
                    ImageButton(ctx).apply {
                        setImageResource(R.drawable.ic_more_vert_24dp)
                        setBackgroundResource(android.R.color.transparent)
                        imageTintList = ColorStateList.valueOf(contentColor.toArgb())
                        setOnClickListener { onMenuClick(this) }
                    }
                },
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun VibrantLyricsScreen(
    dominantColor: Color,
    libraryViewModel: LibraryViewModel = koinActivityViewModel(),
    lyricsViewModel: LyricsViewModel = koinActivityViewModel(),
    playerViewModel: PlayerViewModel = koinActivityViewModel(),
    onEditClick: (Song) -> Unit,
    onCloseClick: () -> Unit,
    onMoreClick: (View) -> Unit,
    onDrag: ((Float, Boolean, Float) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isPowerSaveMode = context.isPowerSaveMode()

    val miniPlayerMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))
    val lyricsViewSettings by lyricsViewModel.fullLyricsViewSettings.collectAsState()
    val uiState by lyricsViewModel.lyricsUiState.collectAsState()

    val song by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()

    val backgroundColor by animateColorAsState(targetValue = dominantColor, label = "vibrant-lyrics-bg")
    val isLight = backgroundColor.luminance() > 0.5f
    val contentColor = if (isLight) Color.Black else Color.White
    val secondaryContentColor = contentColor.copy(alpha = 0.6f)

    var accumulatedNestedDrag by remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember(onDrag) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                // If we are currently dragging down, we should consume the scroll
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
                    // Pulling down at the top
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
                // If nested drag was active but ended without velocity
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
        containerColor = Color.Transparent, // Transparent to show background layer
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .keepScreenOn()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            VibrantTopBar(
                albumName = song.albumName,
                onCloseClick = onCloseClick,
                onMenuClick = onMoreClick,
                contentColor = contentColor,
                onDrag = onDrag,
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.weight(1f)) {
                LyricsSurface(
                    playerViewModel = playerViewModel,
                    uiState = uiState,
                    settings = lyricsViewSettings.copy(
                        isCenterCurrentLine = true,
                        syncedStyle = lyricsViewSettings.syncedStyle.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 36.sp,
                            color = contentColor
                        ),
                        unsyncedStyle = lyricsViewSettings.unsyncedStyle.copy(
                            color = contentColor
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
                accentColor = contentColor, // In vibrant mode, accent matches content since bg is solid
                onEditClick = { onEditClick(song) },
                onMoreClick = onMoreClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
