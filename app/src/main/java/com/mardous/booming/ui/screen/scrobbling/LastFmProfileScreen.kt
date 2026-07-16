package com.mardous.booming.ui.screen.scrobbling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mardous.booming.ui.component.compose.preferences.PreferenceCategoryHeader
import com.mardous.booming.ui.component.compose.preferences.SegmentedPreferenceGroup

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
        onBackClick = onBackClick,
        forceSmallAppBar = true
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
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
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Stats Period",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
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
                    item {
                        SegmentedPreferenceGroup {
                            uiState.topArtists.take(5).forEachIndexed { index, artist ->
                                ArtistItem(artist, showDivider = index < 4)
                            }
                        }
                    }
                }

                // Top Tracks
                if (uiState.topTracks.isNotEmpty()) {
                    item { SectionHeader("Top Tracks") }
                    item {
                        SegmentedPreferenceGroup {
                            uiState.topTracks.take(5).forEachIndexed { index, track ->
                                TrackItem(track, showDivider = index < 4)
                            }
                        }
                    }
                }

                // Recent Tracks
                if (uiState.recentTracks.isNotEmpty()) {
                    item { SectionHeader("Recent Scrobbles") }
                    item {
                        SegmentedPreferenceGroup {
                            uiState.recentTracks.take(10).forEachIndexed { index, track ->
                                RecentTrackItem(track, showDivider = index < 9)
                            }
                        }
                    }
                }
            }

            // Loading Overlay
            AnimatedVisibility(
                visible = uiState.isLoading,
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator()
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
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
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
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = realName?.takeIf { it.isNotBlank() } ?: username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = playcount ?: "0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
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
    Column(modifier = Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TipView(
            text = if (isSyncing) "Syncing scrobbles..." else "You have $count offline scrobbles pending.",
            icon = painterResource(R.drawable.ic_cloud_24dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        )
        if (isSyncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            Button(
                onClick = onSyncClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(painterResource(R.drawable.ic_update_24dp), null)
                Spacer(Modifier.width(8.dp))
                Text("Sync Now")
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun ArtistItem(artist: LastFmTopArtist, showDivider: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.playcount} plays",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun TrackItem(track: LastFmTopTrack, showDivider: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleMedium,
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
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun RecentTrackItem(track: LastFmRecentTrack, showDivider: Boolean) {
    val isNowPlaying = track.attr?.nowplaying == "true"
    
    Column(
        modifier = Modifier.fillMaxWidth().then(
            if (isNowPlaying) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)) else Modifier
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.titleMedium,
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
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}
