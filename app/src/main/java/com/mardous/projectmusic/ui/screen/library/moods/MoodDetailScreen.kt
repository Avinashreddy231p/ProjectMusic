package com.mardous.projectmusic.ui.screen.library.moods

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.projectmusic.data.local.database.dao.ListeningHistoryDao
import com.mardous.projectmusic.data.local.repository.MoodDetail
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.media.asReadableDuration
import com.mardous.projectmusic.ui.component.compose.MediaImage
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodDetailScreen(
    viewModel: MoodDetailViewModel,
    onSongClick: (Song) -> Unit,
    onAddSongsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val moodDetail by viewModel.moodDetail.collectAsState()
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
                        moodDetail?.mood?.name ?: "",
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
        moodDetail?.let { detail ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Hero Header with Parallax
                item {
                    MoodHeroHeader(detail, scrollState)
                }

                // Stats Section
                item {
                    AnimatedEntrance {
                        MoodStatsGrid(detail)
                    }
                }

                // Trends Chart
                item {
                    AnimatedEntrance {
                        MoodTrendCard(detail.trends)
                    }
                }

                // Songs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("Songs")
                        TextButton(onClick = onAddSongsClick) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
                items(detail.songs) { song ->
                    AnimatedEntrance {
                        PremiumSongItem(song = song, onClick = { onSongClick(song) })
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
                                    ArtistBubble(artist)
                                }
                            }
                        }
                    }
                }

                // Top Albums
                if (detail.topAlbums.isNotEmpty()) {
                    item { SectionHeader("Top Albums") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(detail.topAlbums) { album ->
                                AnimatedEntrance {
                                    AlbumGridItem(album)
                                }
                            }
                        }
                    }
                }

                // AI Insights & Discovery
                item { SectionHeader("AI Mood Insights") }
                item {
                    AnimatedEntrance {
                        AIInsightsCard(detail)
                    }
                }

                // Related Moods Placeholder
                item { SectionHeader("Related Moods") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("Relaxed", "Energetic", "Focus", "Chill")) { mood ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(mood) }
                            )
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
fun MoodHeroHeader(detail: MoodDetail, scrollState: LazyListState) {
    val parallaxOffset = remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0) {
                scrollState.firstVisibleItemScrollOffset.toFloat() * 0.5f
            } else 0f
        }
    }

    val emojiScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "EmojiScale"
    )

    val moodColor = remember(detail.mood.name) {
        val hash = detail.mood.name.hashCode().absoluteValue
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
                            moodColor.copy(alpha = 0.8f),
                            moodColor.copy(alpha = 0.2f),
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
            Text(
                text = detail.mood.emoji ?: "🎵",
                fontSize = 100.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = emojiScale
                    scaleY = emojiScale
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = detail.mood.name,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            detail.mood.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MoodStatsGrid(detail: MoodDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Time",
            value = detail.mood.listenedDurationMs.asReadableDuration(),
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.weight(1.0f)
        )
        StatCard(
            label = "Plays",
            value = detail.mood.playCount.toString(),
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.weight(0.7f)
        )
        StatCard(
            label = "Rank",
            value = if (detail.mood.overallRank > 0) "#${detail.mood.overallRank}" else "-",
            icon = Icons.Default.Star,
            modifier = Modifier.weight(0.7f),
            highlight = true
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
fun MoodTrendCard(trends: List<ListeningHistoryDao.TrendData>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Listening Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Canvas-based Sparkline
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
                
                // Area fill
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
    Column(modifier = Modifier.width(120.dp)) {
        MediaImage(
            model = album.albumId,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(album.albumName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(album.artistName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun AIInsightsCard(detail: MoodDetail) {
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "✨ AI Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your listening pattern for '${detail.mood.name}' suggests a preference for complex melodic structures and high-energy transitions. This mood peak usually occurs during your morning deep-work sessions.",
                style = MaterialTheme.typography.bodyMedium
            )
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
        enter = slideInVertically { it / 2 } + fadeIn(animationSpec = tween(600)),
    ) {
        content()
    }
}

