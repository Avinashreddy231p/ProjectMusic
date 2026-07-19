package com.mardous.projectmusic.ui.screen.library.instruments

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.projectmusic.data.local.database.metadata.InstrumentFamily
import com.mardous.projectmusic.data.local.database.dao.ListeningHistoryDao
import com.mardous.projectmusic.data.local.repository.InstrumentDetail
import com.mardous.projectmusic.data.model.Instrument
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.media.asReadableDuration
import com.mardous.projectmusic.ui.component.compose.MediaImage
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentDetailScreen(
    viewModel: InstrumentDetailViewModel,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onInstrumentClick: (Instrument) -> Unit,
    onBackClick: () -> Unit
) {
    val instrumentDetail by viewModel.instrumentDetail.collectAsState()
    val relatedInstruments by viewModel.relatedInstruments.collectAsState()
    val scrollState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val alpha by remember {
                derivedStateOf {
                    if (scrollState.firstVisibleItemIndex > 0) 1f
                    else (scrollState.firstVisibleItemScrollOffset / 500f).coerceIn(0f, 1f)
                }
            }
            TopAppBar(
                title = {
                    Text(
                        instrumentDetail?.instrument?.name ?: "",
                        modifier = Modifier.graphicsLayer { this.alpha = alpha }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        instrumentDetail?.let { detail ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Hero Header with Parallax
                item {
                    InstrumentHeroHeader(detail.instrument, scrollState)
                }

                // Stats Section
                item {
                    AnimatedEntrance {
                        InstrumentStatsGrid(detail)
                    }
                }

                // Trends Chart
                if (detail.trends.isNotEmpty()) {
                    item {
                        AnimatedEntrance {
                            InstrumentTrendCard(detail.trends)
                        }
                    }
                }

                // Songs
                if (detail.songs.isNotEmpty()) {
                    item { SectionHeader("Songs") }
                    items(detail.songs) { song ->
                        AnimatedEntrance {
                            PremiumSongItem(song = song, onClick = { onSongClick(song) })
                        }
                    }
                }

                // Top Artists
                if (detail.topArtists.isNotEmpty()) {
                    item { SectionHeader("Top Artists") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(detail.topArtists) { artist ->
                                AnimatedEntrance {
                                    ArtistBubble(artist, onClick = { onArtistClick(artist.artistName) })
                                }
                            }
                        }
                    }
                }

                // Top Albums
                if (detail.topAlbums.isNotEmpty()) {
                    item { SectionHeader("Top Albums") }
                    item {
                        AlbumGrid(detail.topAlbums, onAlbumClick)
                    }
                }

                // Related Instruments
                if (relatedInstruments.isNotEmpty()) {
                    item { SectionHeader("Related ${detail.instrument.family.name}") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(relatedInstruments) { related ->
                                AnimatedEntrance {
                                    SuggestionChip(
                                        onClick = { onInstrumentClick(related) },
                                        label = { Text(related.name) },
                                        icon = {
                                            Icon(
                                                related.family.toIcon(),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(padding.calculateBottomPadding())) }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun InstrumentHeroHeader(instrument: Instrument, scrollState: LazyListState) {
    val parallaxOffset = remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0) {
                scrollState.firstVisibleItemScrollOffset.toFloat() * 0.5f
            } else 0f
        }
    }

    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IconScale"
    )

    val instrumentColor = instrument.color?.let { Color(it) } ?: remember(instrument.name) {
        val hash = instrument.name.hashCode().absoluteValue
        Color(
            red = (hash % 200 + 55) / 255f,
            green = ((hash / 255) % 200 + 55) / 255f,
            blue = ((hash / 65025) % 200 + 55) / 255f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .graphicsLayer { translationY = parallaxOffset.value }
    ) {
        // Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            instrumentColor.copy(alpha = 0.8f),
                            instrumentColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = instrument.family.toIcon(),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = instrument.name,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = instrument.family.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InstrumentStatsGrid(detail: InstrumentDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Songs",
            value = detail.instrument.songCount.toString(),
            icon = Icons.Default.MusicNote,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Plays",
            value = detail.instrument.playCount.toString(),
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Confidence",
            value = "${(detail.instrument.avgConfidence * 100).toInt()}%",
            icon = Icons.Default.Star,
            modifier = Modifier.weight(1f),
            isGauge = true,
            progress = detail.instrument.avgConfidence
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    isGauge: Boolean = false,
    progress: Float = 0f
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp),
                    tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isGauge) {
                Canvas(modifier = Modifier.size(32.dp).align(Alignment.TopEnd)) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color.Cyan,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun InstrumentTrendCard(trends: List<ListeningHistoryDao.TrendData>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Popularity Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                if (trends.isEmpty()) return@Canvas
                
                val maxVal = trends.maxOf { it.totalTime }.toFloat().coerceAtLeast(1f)
                val width = size.width
                val height = size.height
                val stepX = width / (trends.size - 1).coerceAtLeast(1)
                
                val path = Path().apply {
                    trends.forEachIndexed { index, data ->
                        val x = index * stepX
                        val y = height - (data.totalTime.toFloat() / maxVal * height)
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color.Cyan,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Cyan.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
            }
        }
    }
}

@Composable
fun PremiumSongItem(song: Song, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaImage(
                model = song,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${song.artistName} • ${song.albumName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(song.duration.asReadableDuration(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ArtistBubble(artist: ListeningHistoryDao.ArtistStats, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable { onClick() }
    ) {
        MediaImage(
            model = artist.artistName,
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            artist.artistName,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumGrid(albums: List<ListeningHistoryDao.AlbumStats>, onAlbumClick: (Long) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        albums.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { album ->
                    AlbumGridItem(
                        album = album,
                        onClick = { onAlbumClick(album.albumId) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AlbumGridItem(
    album: ListeningHistoryDao.AlbumStats, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.clickable { onClick() }) {
        MediaImage(
            model = album.albumId,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(album.albumName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(album.artistName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun AnimatedEntrance(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 2 } + fadeIn(animationSpec = tween(600)),
    ) {
        content()
    }
}

private fun InstrumentFamily.toIcon(): ImageVector = when (this) {
    InstrumentFamily.Strings -> Icons.Default.MusicNote
    InstrumentFamily.Percussion -> Icons.Default.MusicNote
    InstrumentFamily.Brass -> Icons.Default.MusicNote
    InstrumentFamily.Woodwind -> Icons.Default.MusicNote
    InstrumentFamily.Keyboard -> Icons.Default.Piano
    InstrumentFamily.Electronic -> Icons.Default.Memory
}
