package com.mardous.projectmusic.ui.screen.scrobbling

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.local.database.analytics.PendingScrobbleEntity
import com.mardous.projectmusic.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.projectmusic.ui.component.compose.preferences.SegmentedPreferenceGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingScrobblesScreen(
    viewModel: PendingScrobblesViewModel,
    onBackClick: () -> Unit
) {
    val scrobbles by viewModel.pendingScrobbles.collectAsState()
    var editingScrobble by remember { mutableStateOf<PendingScrobbleEntity?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    CollapsibleAppBarScaffold(
        title = "Pending Scrobbles",
        onBackClick = onBackClick,
        forceSmallAppBar = true,
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
    ) { paddingValues ->
        if (scrobbles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = "No pending scrobbles.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                item {
                    Text(
                        text = "${scrobbles.size} tracks waiting to sync",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                
                item {
                    SegmentedPreferenceGroup {
                        scrobbles.forEachIndexed { index, scrobble ->
                            PendingScrobbleItem(
                                scrobble = scrobble,
                                showDivider = index < scrobbles.size - 1,
                                onEdit = { editingScrobble = scrobble },
                                onDelete = { viewModel.deleteScrobble(scrobble.id) }
                            )
                        }
                    }
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
                    OutlinedTextField(value = editedTrack, onValueChange = { editedTrack = it }, label = { Text("Track") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editedArtist, onValueChange = { editedArtist = it }, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editedAlbum, onValueChange = { editedAlbum = it }, label = { Text("Album") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
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

@Composable
fun PendingScrobbleItem(
    scrobble: PendingScrobbleEntity,
    showDivider: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scrobble.track,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${scrobble.artist} • ${scrobble.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete_24dp), contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}
