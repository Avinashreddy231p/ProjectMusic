package com.mardous.projectmusic.ui.screen.library.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.model.Album
import com.mardous.projectmusic.data.model.Artist
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.ui.screen.library.home.SpotifyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artist: Artist,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        containerColor = SpotifyColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(SpotifyColors.SurfaceVariant))
                        Text("SoundScape", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SpotifyColors.Primary)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = SpotifyColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyColors.Background.copy(alpha = 0.8f))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Artist Hero Section
            item {
                ArtistHeroSection(artist)
            }

            // Statistics Row
            item {
                ArtistStatsRow(artist)
            }

            // Popular Tracks
            item {
                Text(
                    text = "Popular Tracks",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
            itemsIndexed(artist.sortedSongs.take(5)) { index, song ->
                PopularTrackItem(index + 1, song, onSongClick)
            }

            // Featured Albums
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Featured Albums",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium,
                        color = SpotifyColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(artist.sortedAlbums) { album ->
                        ArtistAlbumItem(album, onAlbumClick)
                    }
                }
            }

            // About Section
            item {
                Text(
                    text = "About ${artist.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "${artist.name} is a pioneer in ambient electronic music, blending deep atmospheric textures with intricate melodic layers. Based in Berlin, his work explores the intersection of nature and technology.",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpotifyColors.OnSurface,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistHeroSection(artist: Artist) {
    Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
        AsyncImage(
            model = artist,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Black),
                        startY = 500f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = artist.name,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyColors.SurfaceContainerHigh),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Favorite", fontWeight = FontWeight.Bold)
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp).clickable { },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painter = painterResource(R.drawable.ic_shuffle_24dp), contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistStatsRow(artist: Artist) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { StatChip("Total Listen Time", "48h 20m") }
        item { StatChip("Plays", "8.5M") }
        item { StatChip("Songs", artist.songCount.toString()) }
        item { StatChip("Albums", artist.albumCount.toString()) }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SpotifyColors.OnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun PopularTrackItem(index: Int, song: Song, onClick: (Song) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(song) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = SpotifyColors.OnSurfaceVariant,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )
        AsyncImage(
            model = song,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("3,452,102 plays", style = MaterialTheme.typography.bodySmall, color = SpotifyColors.OnSurfaceVariant)
        }
        Icon(Icons.Default.MoreVert, contentDescription = null, tint = SpotifyColors.OnSurfaceVariant)
    }
}

@Composable
fun ArtistAlbumItem(album: Album, onClick: (Album) -> Unit) {
    Column(modifier = Modifier.width(160.dp).clickable { onClick(album) }) {
        AsyncImage(
            model = album,
            contentDescription = null,
            modifier = Modifier.size(160.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(album.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${album.year} • Album", style = MaterialTheme.typography.labelSmall, color = SpotifyColors.OnSurfaceVariant)
    }
}
