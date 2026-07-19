package com.mardous.projectmusic.ui.screen.library.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mardous.projectmusic.data.model.Artist
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.screen.library.home.SpotifyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(
    viewModel: LibraryViewModel,
    onArtistClick: (Artist) -> Unit,
    onSettingsClick: () -> Unit
) {
    val artists by viewModel.getArtists().observeAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredArtists = remember(artists, searchQuery) {
        if (searchQuery.isEmpty()) artists
        else artists.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

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
                        Text("Good morning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpotifyColors.Background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SpotifyColors.OnSurface
            )
            Text(
                text = "${artists.size} Artists collected",
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyColors.OnSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape),
                placeholder = { Text("Search your artists...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SpotifyColors.SurfaceVariant,
                    unfocusedContainerColor = SpotifyColors.SurfaceVariant,
                    disabledContainerColor = SpotifyColors.SurfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(filteredArtists, key = { it.id }) { artist ->
                    ArtistGridItem(artist, onArtistClick)
                }
            }
        }
    }
}

@Composable
fun ArtistGridItem(artist: Artist, onClick: (Artist) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(artist) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist,
            contentDescription = artist.name,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(CircleShape),
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
