package com.mardous.projectmusic.ui.screen.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.local.database.dao.ListeningHistoryDao
import com.mardous.projectmusic.data.local.database.analytics.ListeningSessionEntity
import com.mardous.projectmusic.data.model.Instrument
import com.mardous.projectmusic.data.model.Mood
import com.mardous.projectmusic.data.model.Tag
import com.mardous.projectmusic.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.projectmusic.ui.component.compose.menu.MenuItem
import com.mardous.projectmusic.ui.component.compose.menu.OverflowMenu
import com.mardous.projectmusic.ui.screen.librarystats.DistributionItem
import kotlinx.coroutines.launch
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBackClick: () -> Unit,
    onCategoryClick: (String, String) -> Unit = { _, _ -> },
    onExportCsv: () -> Unit = {},
    onExportJson: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    CollapsibleAppBarScaffold(
        title = stringResource(R.string.stats_listening_analytics),
        onBackClick = onBackClick,
        actions = {
            OverflowMenu(
                items = listOf(
                    MenuItem.Button.DropDown(
                        text = stringResource(R.string.stats_export_csv),
                        icon = painterResource(R.drawable.ic_description_24dp),
                        onClick = onExportCsv
                    ),
                    MenuItem.Button.DropDown(
                        text = stringResource(R.string.stats_export_json),
                        icon = painterResource(R.drawable.ic_audio_file_24dp),
                        onClick = onExportJson
                    )
                ),
                icon = painterResource(R.drawable.ic_file_export_24dp)
            )
        }
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
                    stringResource(R.string.stats_insights),
                    stringResource(R.string.stats_library_profile),
                    stringResource(R.string.stats_behavior),
                    stringResource(R.string.stats_context),
                    stringResource(R.string.stats_top_charts),
                    stringResource(R.string.stats_explorer),
                    stringResource(R.string.history_label),
                    stringResource(R.string.stats_wrapped)
                )
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val coroutineScope = rememberCoroutineScope()

                @Suppress("DEPRECATION")
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
                        0 -> InsightsTab(uiState.insights, onCategoryClick)
                        1 -> LibraryProfileTab(uiState.libraryProfile, onCategoryClick)
                        2 -> BehaviorTab(uiState.behavior, onCategoryClick)
                        3 -> ContextTab(uiState.context, onCategoryClick)
                        4 -> TopChartsTab(uiState.topCharts)
                        5 -> ExplorerTab(uiState.historySessions)
                        6 -> HistoryTab(uiState.historySessions, uiState.totalListeningTimeMs) { viewModel.deleteSession(it) }
                        7 -> WrappedTab(uiState.wrapped)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsTab(state: InsightsTabUi, onCategoryClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(title = stringResource(R.string.stats_total_plays), value = state.totalListens.toInt(), modifier = Modifier.weight(1f))
                    SummaryCard(title = stringResource(R.string.stats_total_songs), value = state.uniqueTracks.toInt(), modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(title = stringResource(R.string.stats_completion_rate), value = (state.completionRate * 100).toInt(), suffix = "%", modifier = Modifier.weight(1f))
                    SummaryCard(title = stringResource(R.string.stats_avg_pauses), value = state.avgPauses.toInt(), modifier = Modifier.weight(1f)) // using int for simplicity on counter
                }
            }
        }

        if (state.peakActivity.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_peak_activity))
                Spacer(modifier = Modifier.height(8.dp))
                DistributionListInline(category = stringResource(R.string.stats_time_period), distribution = state.peakActivity, onCategoryClick = onCategoryClick)
            }
        }

        if (state.audioProfile.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_audio_profile))
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveDonutChart(
                    category = stringResource(R.string.stats_audio_format),
                    distribution = state.audioProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

@Composable
private fun LibraryProfileTab(state: LibraryProfileTabUi, onCategoryClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.moodDistribution.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_mood_distribution))
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveDonutChart(
                    category = stringResource(R.string.stats_mood),
                    distribution = state.moodDistribution,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    onCategoryClick = onCategoryClick
                )
            }
        }
        if (state.genreDistribution.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_genre_distribution))
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveDonutChart(
                    category = stringResource(R.string.stats_genre),
                    distribution = state.genreDistribution,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    onCategoryClick = onCategoryClick
                )
            }
        }
        if (state.tagDistribution.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_top_tags))
                Spacer(modifier = Modifier.height(8.dp))
                DistributionListInline(category = stringResource(R.string.stats_tag), distribution = state.tagDistribution, onCategoryClick = onCategoryClick)
            }
        }
        if (state.instrumentDistribution.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.stats_top_instruments))
                Spacer(modifier = Modifier.height(8.dp))
                DistributionListInline(category = stringResource(R.string.stats_instrument), distribution = state.instrumentDistribution, onCategoryClick = onCategoryClick)
            }
        }
    }
}

@Composable
private fun BehaviorTab(state: BehaviorTabUi, onCategoryClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.endReasons.isNotEmpty()) {
            item {
                SectionHeader(title = "End Reasons")
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveDonutChart(
                    category = "End Reason",
                    distribution = state.endReasons,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    onCategoryClick = onCategoryClick
                )
            }
        }
        if (state.repeatModes.isNotEmpty()) {
            item {
                SectionHeader(title = "Repeat Modes")
                Spacer(modifier = Modifier.height(8.dp))
                DistributionListInline(category = "Repeat Mode", distribution = state.repeatModes, onCategoryClick = onCategoryClick)
            }
        }
        if (state.isWeekend.isNotEmpty()) {
            item {
                SectionHeader(title = "Weekend vs Weekday")
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveDonutChart(
                    category = "Day Type",
                    distribution = state.isWeekend,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

@Composable
private fun ContextTab(state: ContextTabUi, onCategoryClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.devices.isNotEmpty()) {
            item {
                SectionHeader(title = "Output Devices")
                Spacer(modifier = Modifier.height(8.dp))
                DistributionListInline(category = "Output Device", distribution = state.devices, onCategoryClick = onCategoryClick)
            }
        }
        if (state.queueSources.isNotEmpty()) {
            item {
                SectionHeader(title = "Queue Sources")
                Spacer(modifier = Modifier.height(8.dp))
                DistributionListInline(category = "Queue Source", distribution = state.queueSources, onCategoryClick = onCategoryClick)
            }
        }
        if (state.playbackOrigins.isNotEmpty()) {
            item {
                SectionHeader(title = "Playback Origins")
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveDonutChart(
                    category = "Playback Origin",
                    distribution = state.playbackOrigins,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    onCategoryClick = onCategoryClick
                )
            }
        }
        if (state.screenOn.isNotEmpty()) {
            item {
                SectionHeader(title = "Screen Status")
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveDonutChart(
                    category = "Screen Status",
                    distribution = state.screenOn,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

@Composable
private fun TopChartsTab(state: TopChartsTabUi) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val segments = listOf("Tracks", "Artists", "Albums", "Genres", "Moods", "Tags", "Instruments")

    Column(modifier = Modifier.fillMaxSize()) {
        @Suppress("DEPRECATION")
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            edgePadding = 16.dp,
            indicator = { },
            divider = { }
        ) {
            segments.forEachIndexed { index, title ->
                val selected = selectedIndex == index
                Tab(
                    selected = selected,
                    onClick = { selectedIndex = index },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedIndex) {
                0 -> {
                    itemsIndexed(state.topSongs) { index, stat ->
                        RankedRow(rank = index + 1, name = "${stat.songTitle} - ${stat.artistName}", count = stat.playCount.toLong())
                    }
                }
                1 -> {
                    itemsIndexed(state.topArtists) { index, stat ->
                        RankedRow(rank = index + 1, name = stat.artistName, count = stat.playCount.toLong())
                    }
                }
                2 -> {
                    itemsIndexed(state.topAlbums) { index, stat ->
                        RankedRow(rank = index + 1, name = "${stat.albumName} - ${stat.artistName}", count = stat.playCount.toLong())
                    }
                }
                3 -> {
                    itemsIndexed(state.topGenres) { index, stat ->
                        RankedRow(rank = index + 1, name = stat.genre, count = stat.playCount.toLong())
                    }
                }
                4 -> {
                    itemsIndexed(state.topMoods) { index, stat ->
                        val label = if (stat.emoji != null) "${stat.emoji} ${stat.name}" else stat.name
                        RankedRow(rank = index + 1, name = label, count = stat.playCount)
                    }
                }
                5 -> {
                    itemsIndexed(state.topTags) { index, stat ->
                        RankedRow(rank = index + 1, name = stat.name, count = stat.songCount.toLong(), countLabel = "songs")
                    }
                }
                6 -> {
                    itemsIndexed(state.topInstruments) { index, stat ->
                        RankedRow(rank = index + 1, name = stat.name, count = stat.songCount.toLong(), countLabel = "songs")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorerTab(sessions: List<ListeningSessionEntity>) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Library Explorer (Coming Soon)\n\n${sessions.size} Recent Sessions Loaded", textAlign = TextAlign.Center)
    }
}

@Composable
private fun HistoryTab(sessions: List<ListeningSessionEntity>, totalDurationMs: Long, onDelete: (Long) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Listening Duration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        StatsViewModel.formatDurationProgressive(totalDurationMs),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        items(sessions, key = { it.sessionId }) { session ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(session.songTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(session.artistName, style = MaterialTheme.typography.bodyMedium)
                        
                        val songMins = session.songDurationMs / 60000
                        val songSecs = (session.songDurationMs % 60000) / 1000
                        val pbMins = session.playbackDurationMs / 60000
                        val pbSecs = (session.playbackDurationMs % 60000) / 1000
                        val timeStr = String.format("Played %02d:%02d / %02d:%02d", pbMins, pbSecs, songMins, songSecs)
                        
                        Text("${session.startDate} ${session.startTimeOnly} • $timeStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(session.sessionId) }) {
                        Text("❌")
                    }
                }
            }
        }
    }
}

@Composable
private fun WrappedTab(state: WrappedTabUi) {
    val pagerState = rememberPagerState(pageCount = { state.thisMonth.size.coerceAtLeast(1) })
    
    if (state.thisMonth.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Not enough data for Wrapped yet!")
        }
        return
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 64.dp),
        pageSpacing = 16.dp
    ) { page ->
        val card = state.thisMonth[page]
        Card(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(0.7f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(card.iconEmoji, style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Text(card.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(16.dp))
                Text(card.primaryValue, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(card.subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// Reusable Components
// ══════════════════════════════════════════════════════════

@Composable
fun SummaryCard(title: String, value: Int, suffix: String = "", modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var targetValue by remember { mutableIntStateOf(0) }
            LaunchedEffect(value) {
                targetValue = value
            }
            val animatedValue by animateIntAsState(
                targetValue = targetValue,
                animationSpec = tween(durationMillis = 1500),
                label = "counter"
            )
            
            Text(text = "$animatedValue$suffix", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RankedRow(rank: Int, name: String, count: Long, subtitle: String? = null, countLabel: String = "plays") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(text = "$count $countLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DistributionListInline(category: String, distribution: List<DistributionItem>, onCategoryClick: (String, String) -> Unit) {
    val total = distribution.sumOf { it.count }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        distribution.forEach { item ->
            val fraction = item.count.toFloat() / total.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { onCategoryClick(category, item.name) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.count.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun InteractiveDonutChart(
    category: String,
    distribution: List<DistributionItem>,
    modifier: Modifier = Modifier,
    thickness: Float = 60f,
    onCategoryClick: (String, String) -> Unit = { _, _ -> }
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

                distribution.forEachIndexed { index, item ->
                    val sweepAngle = (item.count.toFloat() / total) * 360f
                    val actualSweep = maxOf(0f, sweepAngle - gapAngle)

                    newAngles.add(startAngle to sweepAngle)

                    val isSelected = selectedIndex == index
                    val strokeWidth = if (isSelected) thickness * 1.3f else thickness
                    val color = colors[index % colors.size].copy(alpha = if (selectedIndex == null || isSelected) 1f else 0.4f)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = actualSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    )
                    startAngle += sweepAngle
                }
                
                if (angles.isEmpty() || angles.size != newAngles.size) {
                    angles = newAngles
                }
            }

            if (selectedIndex != null) {
                val selectedItem = distribution[selectedIndex!!]
                val percent = ((selectedItem.count.toFloat() / total) * 100).toInt()
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable { onCategoryClick(category, selectedItem.name) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedItem.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = total.toInt().toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
