package com.mardous.booming.ui.screen.scrobbling

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.data.local.room.PendingScrobbleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingScrobblesScreen(
    viewModel: PendingScrobblesViewModel,
    onBackClick: () -> Unit
) {
    val scrobbles by viewModel.pendingScrobbles.collectAsState()
    var editingScrobble by remember { mutableStateOf<PendingScrobbleEntity?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Pending Scrobbles (${scrobbles.size})") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(R.drawable.ic_back_24dp), contentDescription = "Back")
                    }
                },
                actions = {
                    if (scrobbles.isNotEmpty()) {
                        IconButton(onClick = { viewModel.syncScrobbles() }) {
                            Icon(painterResource(R.drawable.ic_update_24dp), contentDescription = "Sync Now")
                        }
                        IconButton(onClick = { showDeleteAllConfirm = true }) {
                            Icon(painterResource(R.drawable.ic_delete_24dp), contentDescription = "Delete All")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (scrobbles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = "No pending scrobbles.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(scrobbles, key = { it.id }) { scrobble ->
                    ListItem(
                        modifier = Modifier.clickable { editingScrobble = scrobble },
                        headlineContent = { Text(text = scrobble.track, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(text = "${scrobble.artist} • ${scrobble.album}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteScrobble(scrobble.id) }) {
                                Icon(painterResource(R.drawable.ic_delete_24dp), contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("Delete All") },
            text = { Text("Are you sure you want to delete all pending scrobbles? They will not be sent to Last.fm.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showDeleteAllConfirm = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    editingScrobble?.let { scrobble ->
        var editedTrack by remember { mutableStateOf(scrobble.track) }
        var editedArtist by remember { mutableStateOf(scrobble.artist) }
        var editedAlbum by remember { mutableStateOf(scrobble.album) }

        AlertDialog(
            onDismissRequest = { editingScrobble = null },
            title = { Text("Edit Scrobble") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editedTrack, onValueChange = { editedTrack = it }, label = { Text("Track") })
                    OutlinedTextField(value = editedArtist, onValueChange = { editedArtist = it }, label = { Text("Artist") })
                    OutlinedTextField(value = editedAlbum, onValueChange = { editedAlbum = it }, label = { Text("Album") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateScrobble(scrobble.copy(track = editedTrack, artist = editedArtist, album = editedAlbum))
                    editingScrobble = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingScrobble = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
