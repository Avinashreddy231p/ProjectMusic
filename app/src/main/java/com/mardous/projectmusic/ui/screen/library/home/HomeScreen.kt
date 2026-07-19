package com.mardous.projectmusic.ui.screen.library.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.local.repository.StatsSummary
import com.mardous.projectmusic.data.model.Album
import com.mardous.projectmusic.data.model.Artist
import com.mardous.projectmusic.data.model.ContentType
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.Suggestion
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import java.util.*
import java.util.concurrent.TimeUnit

object SpotifyColors {
    val Primary = Color(0xFF1DB954)
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF121212)
    val SurfaceVariant = Color(0xFF282828)
    val SurfaceContainerLow = Color(0xFF1E2020)
    val SurfaceContainerHigh = Color(0xFF292A2A)
    val OnSurface = Color(0xFFE3E2E2)
    val OnSurfaceVariant = Color(0xFFBCCBB9)
    val Tertiary = Color(0xFFFFB3B3)
    val Secondary = Color(0xFFC8C6C5)
}

@Composable
fun HomeScreen(
    viewModel: LibraryViewModel,
    statsSummary: StatsSummary?,
    onSettingsClick: () -> Unit,
    onSuggestionClick: (Suggestion) -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onSeeAllClick: (ContentType) -> Unit,
    onShuffleClick: () -> Unit
) {
    val suggestionsResult by viewModel.getSuggestions().observeAsState(SuggestedResult.Idle)
    val allSuggestions = suggestionsResult.data
    
    var selectedFilter by remember { mutableStateOf("Top") }

    val filteredSuggestions = remember(allSuggestions, selectedFilter) {
        allSuggestions.filter { suggestion ->
            val match = when (selectedFilter) {
                "Top" -> suggestion.type == ContentType.TopArtists || suggestion.type == ContentType.TopAlbums || suggestion.type == ContentType.TopTracks
                "Recent" -> suggestion.type == ContentType.HistoryArtists || suggestion.type == ContentType.HistoryAlbums || suggestion.type == ContentType.History
                "New" -> suggestion.type == ContentType.RecentArtists || suggestion.type == ContentType.RecentAlbums || suggestion.type == ContentType.RecentSongs
                else -> true
            }
            match && suggestion.items.isNotEmpty()
        }
    }
    
    val ambientColor = SpotifyColors.Primary.copy(alpha = 0.15f)

    Scaffold(
        containerColor = SpotifyColors.Background,
        topBar = {
            HomeTopBar(onSettingsClick)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Ambient Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ambientColor, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(0f, 0f),
                            radius = 2000f
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Quick Actions Grid
                item {
                    QuickActionsGrid(
                        onTopTracksClick = { onSeeAllClick(ContentType.TopTracks) },
                        onLastAddedClick = { onSeeAllClick(ContentType.RecentSongs) },
                        onHistoryClick = { onSeeAllClick(ContentType.History) },
                        onShuffleClick = onShuffleClick
                    )
                }

                // Filter Chips
                item {
                    FilterChips(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it }
                    )
                }

                // Suggestions
                items(filteredSuggestions, key = { it.type.name }) { suggestion ->
                    when (suggestion.type) {
                        ContentType.TopArtists, ContentType.RecentArtists, ContentType.HistoryArtists -> {
                            ArtistsSection(
                                title = stringResource(suggestion.type.titleRes),
                                artists = suggestion.items.filterIsInstance<Artist>(),
                                onArtistClick = onArtistClick,
                                onSeeAllClick = { onSeeAllClick(suggestion.type) }
                            )
                        }

                        ContentType.TopAlbums, ContentType.RecentAlbums, ContentType.HistoryAlbums -> {
                            AlbumsSection(
                                title = stringResource(suggestion.type.titleRes),
                                albums = suggestion.items.filterIsInstance<Album>(),
                                onAlbumClick = onAlbumClick,
                                onSeeAllClick = { onSeeAllClick(suggestion.type) }
                            )
                        }

                        ContentType.TopTracks, ContentType.RecentSongs, ContentType.History -> {
                            QuickPicksSection(
                                title = stringResource(suggestion.type.titleRes),
                                songs = suggestion.items.filterIsInstance<Song>(),
                                onSongClick = onSongClick,
                                onSeeAllClick = { onSeeAllClick(suggestion.type) }
                            )
                        }

                        else -> {}
                    }
                }

                // Listening Summary
                if (statsSummary != null) {
                    item {
                        ListeningSummaryCard(statsSummary)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Extra space for mini player
                }
            }
        }
    }
}

@Composable
fun HomeTopBar(onSettingsClick: () -> Unit) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = SpotifyColors.OnSurface,
            letterSpacing = (-1).sp
        )
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = SpotifyColors.OnSurface
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    onTopTracksClick: () -> Unit,
    onLastAddedClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onShuffleClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickActionItem(
                title = "Top Tracks",
                iconRes = R.drawable.ic_trending_up_24dp,
                iconColor = SpotifyColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onTopTracksClick
            )
            QuickActionItem(
                title = "Last added",
                iconRes = R.drawable.ic_library_add_24dp,
                iconColor = SpotifyColors.Tertiary,
                modifier = Modifier.weight(1f),
                onClick = onLastAddedClick
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickActionItem(
                title = "History",
                iconRes = R.drawable.ic_history_24dp,
                iconColor = SpotifyColors.Secondary,
                modifier = Modifier.weight(1f),
                onClick = onHistoryClick
            )
            QuickActionItem(
                title = "Shuffle",
                iconRes = R.drawable.ic_shuffle_24dp,
                iconColor = SpotifyColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onShuffleClick
            )
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    iconRes: Int,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SpotifyColors.OnSurface,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("Top", "Recent", "New")

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                color = if (isSelected) SpotifyColors.Primary else SpotifyColors.SurfaceContainerHigh,
                shape = CircleShape,
                modifier = Modifier.clickable { onFilterSelected(filter) }
            ) {
                Text(
                    text = filter,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Black else SpotifyColors.OnSurface
                )
            }
        }
    }
}

@Composable
fun QuickPicksSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column {
        SectionHeader("Quick picks", onSeeAllClick)
        val chunkedSongs = songs.take(6).chunked(3)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            items(chunkedSongs) { columnSongs ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    columnSongs.forEach { song ->
                        QuickPickItem(song, onSongClick)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPickItem(song: Song, onClick: (Song) -> Unit) {
    Row(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick(song) }
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = song,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SpotifyColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.labelSmall,
                color = SpotifyColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArtistsSection(
    title: String,
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column {
        SectionHeader("Top artists", onSeeAllClick)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            items(artists.take(10), key = { it.id }) { artist ->
                ArtistItem(artist, onArtistClick)
            }
        }
    }
}

@Composable
fun ArtistItem(artist: Artist, onClick: (Artist) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick(artist) }
    ) {
        AsyncImage(
            model = artist,
            contentDescription = artist.name,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(2.dp, SpotifyColors.Primary.copy(alpha = 0.2f), CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = SpotifyColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AlbumsSection(
    title: String,
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column {
        SectionHeader("Top albums", onSeeAllClick)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            items(albums.take(10), key = { it.id }) { album ->
                AlbumItem(album, onAlbumClick)
            }
        }
    }
}

@Composable
fun AlbumItem(album: Album, onClick: (Album) -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick(album) }
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = album,
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_circle_24dp),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.0f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SpotifyColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.year} • ${album.artistName}",
            style = MaterialTheme.typography.bodySmall,
            color = SpotifyColors.OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SpotifyColors.OnSurface
        )
        Text(
            text = "See all",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SpotifyColors.Primary,
            modifier = Modifier.clickable { onSeeAllClick() }
        )
    }
}

@Composable
fun ListeningSummaryCard(summary: StatsSummary) {
    val totalTime = remember(summary.totalListeningMs) {
        val hours = TimeUnit.MILLISECONDS.toHours(summary.totalListeningMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(summary.totalListeningMs) % 60
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E2020),
                        Color(0xFF0D0E0F)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        // Decorative blurs
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .size(160.dp)
                .background(SpotifyColors.Primary.copy(alpha = 0.1f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 40.dp)
                .size(160.dp)
                .background(SpotifyColors.Tertiary.copy(alpha = 0.1f), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_graphic_eq_24dp),
                    contentDescription = null,
                    tint = SpotifyColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Listening Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = totalTime,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp
            )

            Text(
                text = "TOTAL TIME THIS MONTH",
                style = MaterialTheme.typography.labelSmall,
                color = SpotifyColors.OnSurfaceVariant,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 32.dp),
                color = Color.White.copy(alpha = 0.05f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryStat(iconRes = R.drawable.ic_music_note_24dp, value = summary.uniqueSongs.toString(), label = "Songs")
                SummaryStat(iconRes = R.drawable.ic_mic_24dp, value = summary.uniqueArtists.toString(), label = "Artists")
                SummaryStat(iconRes = R.drawable.ic_album_24dp, value = summary.uniqueAlbums.toString(), label = "Albums")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    GridStat(value = "${summary.currentStreak} Days", label = "Streak 🔥", color = SpotifyColors.Primary)
                    GridStat(value = "${(summary.repeatRate * 100).toInt()}%", label = "Repeat Rate")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    GridStat(value = "${(summary.discoveryRate * 100).toInt()}%", label = "Discovery")
                    GridStat(value = "${(summary.shuffleRate * 100).toInt()}%", label = "Shuffle")
                }
                GridStat(value = "61%", label = "Night Owl 🌙", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun SummaryStat(iconRes: Int, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = SpotifyColors.Primary.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SpotifyColors.OnSurfaceVariant
        )
    }
}

@Composable
fun GridStat(value: String, label: String, color: Color = Color.White, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SpotifyColors.OnSurfaceVariant
        )
    }
}
