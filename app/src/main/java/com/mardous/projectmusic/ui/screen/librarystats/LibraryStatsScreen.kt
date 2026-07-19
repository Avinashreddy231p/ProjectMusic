package com.mardous.projectmusic.ui.screen.librarystats

import android.text.format.Formatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mardous.projectmusic.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mardous.projectmusic.ui.ISongCallback
import com.mardous.projectmusic.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.projectmusic.ui.component.compose.TipView
import kotlinx.coroutines.launch
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryStatsScreen(
    viewModel: LibraryStatsViewModel,
    onBackClick: () -> Unit,
    onCategoryClick: (String, String) -> Unit,
    songCallback: ISongCallback,
    activity: FragmentActivity
) {
    val uiState by viewModel.uiState.collectAsState()

    CollapsibleAppBarScaffold(
        title = stringResource(R.string.stats_library_analytics),
        onBackClick = onBackClick,
        actions = {}
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val tabs = listOf(
                    stringResource(R.string.stats_core),
                    stringResource(R.string.stats_audiophile),
                    stringResource(R.string.stats_storage),
                    stringResource(R.string.stats_health),
                    stringResource(R.string.lastfm_title)
                )
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val coroutineScope = rememberCoroutineScope()

                // Pill Tabs
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    indicator = { },
                    divider = { },
                    containerColor = Color.Transparent
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        Tab(
                            selected = selected,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> CoreTab(uiState, onCategoryClick)
                        1 -> AudiophileTab(uiState, onCategoryClick)
                        2 -> StorageTab(uiState, onCategoryClick)
                        3 -> HealthTab(uiState, onCategoryClick)
                        4 -> LastFmTab(uiState, onCategoryClick)
                    }
                }
            }
        }
    }
}

@Composable
fun LastFmTab(uiState: LibraryStatsUiState, onCategoryClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.isLastFmLoggedIn) {
            item {
                TipView(
                    text = "Sign in to Last.fm in settings to see your global listening statistics here.",
                    icon = painterResource(com.mardous.projectmusic.R.drawable.ic_language_24dp)
                )
            }
        } else {
            if (uiState.lastFmTopArtists.isNotEmpty()) {
                item {
                    Text("Top Artists (Global)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    DistributionListInline("Global Artist", uiState.lastFmTopArtists, onCategoryClick)
                }
            }
            if (uiState.lastFmTopTracks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Top Tracks (Global)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    DistributionListInline("Global Track", uiState.lastFmTopTracks, onCategoryClick)
                }
            }
        }
    }
}

@Composable
fun CoreTab(uiState: LibraryStatsUiState, onCategoryClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.genreDistribution.isNotEmpty()) {
            item {
                Text("Genre Distribution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                InteractiveDonutChart(
                    category = "Genre",
                    distribution = uiState.genreDistribution,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Artist Concentration", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.artistConcentrationInsight, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (uiState.trackLengthDistribution.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Track Lengths", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DistributionListInline("Track Length", uiState.trackLengthDistribution, onCategoryClick)
            }
        }
        if (uiState.decadeDistribution.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Release Decades", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DistributionListInline("Decade", uiState.decadeDistribution, onCategoryClick)
            }
        }
        if (uiState.albumSizeDistribution.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Album Sizes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DistributionListInline("Album Size", uiState.albumSizeDistribution, onCategoryClick)
            }
        }
        if (uiState.alphabeticalDistribution.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Alphabetical", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DistributionListInline("Alphabetical", uiState.alphabeticalDistribution, onCategoryClick)
            }
        }
    }
}

@Composable
fun AudiophileTab(uiState: LibraryStatsUiState, onCategoryClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.formatDistribution.isNotEmpty()) {
            item {
                Text("Audio Formats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                InteractiveDonutChart(
                    category = "Format",
                    distribution = uiState.formatDistribution,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                )
            }
        }
        if (uiState.bitrateDistribution.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bitrate Quality", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DistributionListInline("Bitrate", uiState.bitrateDistribution, onCategoryClick)
            }
        }
        if (uiState.fileSizeDistribution.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("File Sizes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DistributionListInline("File Size", uiState.fileSizeDistribution, onCategoryClick)
            }
        }
    }
}

@Composable
fun StorageTab(uiState: LibraryStatsUiState, onCategoryClick: (String, String) -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Capacity", style = MaterialTheme.typography.titleMedium)
                    Text(Formatter.formatShortFileSize(context, uiState.totalSizeBytes), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val days = uiState.totalDurationMs / (1000 * 60 * 60 * 24)
                    val hours = (uiState.totalDurationMs / (1000 * 60 * 60)) % 24
                    Text("Total Playtime", style = MaterialTheme.typography.titleMedium)
                    Text("${days}d ${hours}h", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StorageStatCard(title = "Songs", value = uiState.totalSongs.toString(), modifier = Modifier.weight(1f))
                StorageStatCard(title = "Artists", value = uiState.uniqueArtists.toString(), modifier = Modifier.weight(1f))
                StorageStatCard(title = "Albums", value = uiState.uniqueAlbums.toString(), modifier = Modifier.weight(1f))
                StorageStatCard(title = "Genres", value = uiState.uniqueGenres.toString(), modifier = Modifier.weight(1f))
            }
        }
        
        if (uiState.largestFoldersDistribution.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Largest Folders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DistributionListInline("Folder", uiState.largestFoldersDistribution, onCategoryClick)
            }
        }
    }
}

@Composable
fun StorageStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HealthTab(uiState: LibraryStatsUiState, onCategoryClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.unplayedDistribution.isNotEmpty()) {
            item {
                Text("Play Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                InteractiveDonutChart(
                    category = "Play Activity",
                    distribution = uiState.unplayedDistribution,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onCategoryClick("Orphaned Tracks", "Orphaned Tracks") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Orphaned Tracks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("${uiState.orphanedTracksCount} artists have exactly 1 track", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Tap to view", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha=0.7f))
                }
            }
        }

        if (uiState.metadataHealthDistribution.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Metadata Issues", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DistributionListInline("Metadata Health", uiState.metadataHealthDistribution, onCategoryClick)
            }
        }
    }
}

@Composable
fun DistributionListInline(category: String, distribution: List<DistributionItem>, onCategoryClick: (String, String) -> Unit) {
    val total = distribution.sumOf { it.count }.coerceAtLeast(1)
    Column {
        distribution.forEach { item ->
            val fraction = item.count.toFloat() / total.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick(category, item.name) }
                    .padding(vertical = 4.dp)
            ) {
                // Background bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(item.count.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InteractiveDonutChart(
    category: String,
    distribution: List<DistributionItem>,
    onCategoryClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    thickness: Float = 60f
) {
    if (distribution.isEmpty()) return

    val total = distribution.sumOf { it.count }.toFloat()
    if (total <= 0f) return

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
    )

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var angles by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(distribution) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            val clickedIndex = angles.indexOfFirst { (start, sweep) ->
                                val end = start + sweep
                                if (end > 360f) {
                                    angle in start..360f || angle in 0f..(end - 360f)
                                } else {
                                    angle in start..end
                                }
                            }
                            selectedIndex = if (clickedIndex == selectedIndex) null else clickedIndex.takeIf { it >= 0 }
                        }
                    }
            ) {
                var startAngle = 0f
                val newAngles = mutableListOf<Pair<Float, Float>>()
                
                val gapAngle = if (distribution.size > 1) 2f else 0f
                val totalSweep = 360f - (distribution.size * gapAngle)

                distribution.forEachIndexed { index, item ->
                    val sweepAngle = (item.count / total) * totalSweep
                    newAngles.add(startAngle to sweepAngle)
                    
                    val isSelected = selectedIndex == index
                    val currentThickness = if (isSelected) thickness + 15f else thickness
                    val color = colors[index % colors.size].copy(alpha = if (selectedIndex == null || isSelected) 1f else 0.4f)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = currentThickness, cap = StrokeCap.Round),
                        size = Size(size.width, size.height),
                        topLeft = Offset(0f, 0f)
                    )
                    startAngle += sweepAngle + gapAngle
                }
                angles = newAngles
            }

            // Center text
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .clickable { 
                        selectedIndex?.let { onCategoryClick(category, distribution[it].name) }
                    }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val displayItem = selectedIndex?.let { distribution[it] }
                Text(
                    text = displayItem?.name ?: "Total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayItem?.count?.toString() ?: total.toInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (displayItem != null) {
                    Text("Tap to view", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            distribution.take(4).forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colors[index % colors.size])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedIndex == null || selectedIndex == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 60.dp)
                    )
                }
            }
        }
    }
}
