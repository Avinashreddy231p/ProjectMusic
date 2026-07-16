package com.mardous.booming.ui.screen.scrobbling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mardous.booming.R
import com.mardous.booming.data.remote.lastfm.model.*
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.booming.ui.component.compose.TipView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LastFmProfileScreen(
    viewModel: LastFmProfileViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val periods = listOf(
        "7day" to "7D",
        "1month" to "1M",
        "3month" to "3M",
        "6month" to "6M",
        "12month" to "1Y",
        "overall" to "All"
    )

    CollapsibleAppBarScaffold(
        title = stringResource(R.string.lastfm_title),
        onBackClick = onBackClick
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Info Header
                uiState.user?.let { user ->
                    val avatarUrl = user.image?.find { it.size == "extralarge" }?.url
                        ?: user.image?.find { it.size == "large" }?.url
                    
                    item {
                        UserProfileCard(
                            username = user.name,
                            realName = user.realName,
                            avatarUrl = avatarUrl,
                            playcount = user.playcount,
                            joinedDate = user.registered?.text
                        )
                    }
                }

                // Sync Banner
                if (uiState.pendingScrobblesCount > 0 || uiState.isSyncing) {
                    item {
                        SyncBanner(
                            count = uiState.pendingScrobblesCount,
                            isSyncing = uiState.isSyncing,
                            onSyncClick = { viewModel.syncNow() }
                        )
                    }
                }

                // Period Selector
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Stats Period",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            periods.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = uiState.selectedPeriod == value,
                                    onClick = { viewModel.setPeriod(value) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                                    icon = {}
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }

                // Top Artists
                if (uiState.topArtists.isNotEmpty()) {
                    item { SectionHeader("Top Artists") }
                    items(uiState.topArtists) { artist ->
                        ArtistItem(artist)
                    }
                }

                // Top Tracks
                if (uiState.topTracks.isNotEmpty()) {
                    item { SectionHeader("Top Tracks") }
                    items(uiState.topTracks) { track ->
                        TrackItem(track)
                    }
                }

                // Recent Tracks
                if (uiState.recentTracks.isNotEmpty()) {
                    item { SectionHeader("Recent Scrobbles") }
                    items(uiState.recentTracks) { track ->
                        RecentTrackItem(track)
                    }
                }

                // Settings Section
                item {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HorizontalDivider()
                        SectionHeader("Settings")
                        
                        PreferenceSwitch(
                            title = "Enable Scrobbling",
                            checked = uiState.scrobblingEnabled,
                            onCheckedChange = { viewModel.toggleScrobbling(it) }
                        )
                        
                        PreferenceSwitch(
                            title = "Offline Scrobbling",
                            summary = "Cache scrobbles when offline",
                            checked = uiState.offlineScrobbling,
                            onCheckedChange = { viewModel.toggleOfflineScrobbling(it) }
                        )
                        
                        PreferenceSwitch(
                            title = "Sync Favorites",
                            summary = "Sync loved tracks with Last.fm",
                            checked = uiState.syncFavorites,
                            onCheckedChange = { viewModel.toggleSyncFavorites(it) }
                        )

                        Column {
                            Text("Scrobble percentage: ${uiState.scrobblePercentage}%", style = MaterialTheme.typography.bodyMedium)
                            Slider(
                                value = uiState.scrobblePercentage.toFloat(),
                                onValueChange = { viewModel.setScrobblePercentage(it.toInt()) },
                                valueRange = 10f..100f,
                                steps = 18
                            )
                        }
                    }
                }
            }

            // Loading Overlay
            AnimatedVisibility(
                visible = uiState.isLoading,
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularWavyProgressIndicator()
            }
        }
    }
}

@Composable
fun UserProfileCard(
    username: String,
    realName: String?,
    avatarUrl: String?,
    playcount: String?,
    joinedDate: String?
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                placeholder = painterResource(R.drawable.ic_person_24dp),
                error = painterResource(R.drawable.ic_person_24dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = realName?.takeIf { it.isNotBlank() } ?: username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = playcount ?: "0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scrobbles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    joinedDate?.let {
                        Column {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Joined",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SyncBanner(count: Int, isSyncing: Boolean, onSyncClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TipView(
            text = if (isSyncing) "Syncing scrobbles..." else "You have $count offline scrobbles pending.",
            icon = painterResource(R.drawable.ic_cloud_24dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        )
        if (isSyncing) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            Button(
                onClick = onSyncClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync Now")
            }
        }
    }
}

@Composable
fun PreferenceSwitch(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            summary?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ArtistItem(artist: LastFmTopArtist) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.playcount} plays",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TrackItem(track: LastFmTopTrack) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${track.playcount} plays",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RecentTrackItem(track: LastFmRecentTrack) {
    val isNowPlaying = track.attr?.nowplaying == "true"
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = if (isNowPlaying) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)) else CardDefaults.outlinedCardColors()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isNowPlaying) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_play_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "${track.artist.text} • ${track.album.text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            track.date?.let { date ->
                Text(
                    text = date.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
