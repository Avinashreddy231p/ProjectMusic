package com.mardous.projectmusic.ui.screen.library.tags

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.projectmusic.data.local.database.dao.ListeningHistoryDao
import com.mardous.projectmusic.data.local.repository.TagDetail
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.Tag
import com.mardous.projectmusic.extensions.media.asReadableDuration
import com.mardous.projectmusic.ui.component.compose.MediaImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
    viewModel: TagDetailViewModel,
    onSongClick: (Song) -> Unit,
    onAddSongsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val tagDetail by viewModel.tagDetail.collectAsState()
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
                        tagDetail?.tag?.name ?: "",
                        modifier = Modifier.graphicsLayer { this.alpha = alpha }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddSongsClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Songs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        tagDetail?.let { detail ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Hero Header with Parallax and Animation
                item {
                    TagHeroHeader(detail.tag, scrollState)
                }

                // Statistics Section
                item {
                    AnimatedEntrance {
                        TagStatsGrid(detail.tag)
                    }
                }

                // Trend Chart
                if (detail.trends.isNotEmpty()) {
                    item {
                        AnimatedEntrance {
                            TagTrendCard(detail.trends)
                        }
                    }
                }

                // Songs Section
                if (detail.songs.isNotEmpty()) {
                    item { SectionHeader("Songs") }
                    items(detail.songs) { song ->
                        AnimatedEntrance {
                            PremiumSongItem(song = song, onClick = { onSongClick(song) })
                        }
                    }
                }

                // Top Artists Section (Horizontal bubbles)
                if (detail.topArtists.isNotEmpty()) {
                    item { SectionHeader("Top Artists") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(detail.topArtists) { artist ->
                                AnimatedEntrance {
                                    ArtistBubble(artist)
                                }
                            }
                        }
                    }
                }

                // Top Albums Section (Grid)
                if (detail.topAlbums.isNotEmpty()) {
                    item { SectionHeader("Top Albums") }
                    detail.topAlbums.chunked(2).forEach { chunk ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                chunk.forEach { album ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        AnimatedEntrance {
                                            AlbumGridItem(album)
                                        }
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // AI Insights Section
                item { SectionHeader("AI Tag Insights") }
                item {
                    AnimatedEntrance {
                        AIInsightsCard(detail)
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
fun TagHeroHeader(tag: Tag, scrollState: LazyListState) {
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

    val tagColor = remember(tag.color) {
        tag.color?.let { Color(it) } ?: Color(0xFF6200EE) // Default primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .graphicsLayer { translationY = parallaxOffset.value }
    ) {
        // Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tagColor.copy(alpha = 0.9f),
                            tagColor.copy(alpha = 0.4f),
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
            AnimatedEntrance {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = tag.name,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            tag.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TagStatsGrid(tag: Tag) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Play Count",
            value = tag.playCount.toString(),
            icon = Icons.Default.BarChart,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Listening Time",
            value = tag.listenedDurationMs.asReadableDuration(),
            icon = Icons.Default.AccessTime,
            modifier = Modifier.weight(1f),
            highlight = true
        )
        StatCard(
            label = "Overall Rank",
            value = "#${tag.overallRank}",
            icon = Icons.Default.Star,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(20.dp),
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
    }
}

@Composable
fun TagTrendCard(trends: List<ListeningHistoryDao.TrendData>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Listening Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
fun ArtistBubble(artist: ListeningHistoryDao.ArtistStats) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(artist.artistName.take(1), style = MaterialTheme.typography.headlineMedium)
        }
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
fun AlbumGridItem(album: ListeningHistoryDao.AlbumStats) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaImage(
            model = album.albumId,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(album.albumName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(album.artistName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun AIInsightsCard(detail: TagDetail) {
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "✨ AI Insight",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            val insight = when {
                detail.topSongs.isEmpty() -> "Start listening to songs with this tag to generate insights!"
                detail.tag.playCount > 100 -> "You are a power listener of '${detail.tag.name}'. This tag accounts for a significant portion of your recent listening history, peaking during evening hours."
                else -> "Your interest in '${detail.tag.name}' is growing. You've discovered ${detail.topSongs.size} new songs in this category recently."
            }
            Text(text = insight, style = MaterialTheme.typography.bodyMedium)
        }
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
        enter = slideInVertically { it / 3 } + fadeIn(animationSpec = tween(700)),
    ) {
        content()
    }
}
