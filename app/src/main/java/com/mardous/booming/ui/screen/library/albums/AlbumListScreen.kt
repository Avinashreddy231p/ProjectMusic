package com.mardous.booming.ui.screen.library.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mardous.booming.R
import com.mardous.booming.data.model.Album
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.library.home.SpotifyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    viewModel: LibraryViewModel,
    onAlbumClick: (Album) -> Unit,
    onSettingsClick: () -> Unit
) {
    val albums by viewModel.getAlbums().observeAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isEmpty()) albums
        else albums.filter { it.name.contains(searchQuery, ignoreCase = true) || it.artistName.contains(searchQuery, ignoreCase = true) }
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
                text = "${albums.size} Albums collected",
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
                placeholder = { Text("Search your albums...", fontSize = 14.sp) },
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

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryFilterChip("Recently Added", isSelected = true)
                LibraryFilterChip("A-Z")
                LibraryFilterChip("Artist")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Layout Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(SpotifyColors.SurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LayoutIcon(Icons.Default.GridView, isSelected = true)
                    LayoutIcon(Icons.Default.List)
                }
                Text(
                    text = "LAYOUT VIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyColors.OnSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(filteredAlbums, key = { it.id }) { album ->
                    AlbumGridItem(album, onAlbumClick)
                }
            }
        }
    }
}

@Composable
fun LibraryFilterChip(text: String, isSelected: Boolean = false) {
    Surface(
        color = if (isSelected) SpotifyColors.Primary else SpotifyColors.SurfaceVariant,
        shape = CircleShape,
        modifier = Modifier.clickable { }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else SpotifyColors.OnSurface
        )
    }
}

@Composable
fun LayoutIcon(icon: ImageVector, isSelected: Boolean = false) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) SpotifyColors.SurfaceContainerHigh else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) SpotifyColors.Primary else SpotifyColors.OnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AlbumGridItem(album: Album, onClick: (Album) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(album) }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
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
                    .background(Color.Black.copy(alpha = 0.2f))
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = SpotifyColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.artistName} • ${album.year}",
            style = MaterialTheme.typography.bodySmall,
            color = SpotifyColors.OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
