/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.ui.dialogs.metadata

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.model.Artist
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.EXTRA_SONGS
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.extensions.withArgs
import com.mardous.projectmusic.ui.component.compose.BottomSheetDialogSurface
import com.mardous.projectmusic.ui.component.compose.MediaImage
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MusicBrainzSelectionDialog : BottomSheetDialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    
    private val songs by lazy {
        arguments?.getParcelableArrayList<Song>(EXTRA_SONGS) ?: emptyList<Song>()
    }
    
    private val artists by lazy {
        arguments?.getParcelableArrayList<com.mardous.projectmusic.data.local.database.metadata.ArtistEntity>(EXTRA_ARTISTS) ?: emptyList()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                ProjectMusicTheme {
                    MusicBrainzSelectionScreen(
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun MusicBrainzSelectionScreen(
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        var searchQuery by rememberSaveable { mutableStateOf("") }
        
        val isArtistMode = artists.isNotEmpty()
        
        val allIds = if (isArtistMode) artists.map { it.id } else songs.map { it.id }
        val checkedIds = rememberSaveable { mutableStateListOf<Long>().apply { addAll(allIds) } }
        
        val filteredSongs = remember(searchQuery) {
            if (searchQuery.isBlank()) songs else songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.artistName.contains(searchQuery, ignoreCase = true)
            }
        }
        
        val filteredArtists = remember(searchQuery) {
            if (searchQuery.isBlank()) artists else artists.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        BottomSheetDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.musicbrainz_selection_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        checkedIds.clear()
                        checkedIds.addAll(allIds)
                    }) {
                        Text(stringResource(R.string.select_all))
                    }
                    TextButton(onClick = {
                        checkedIds.clear()
                    }) {
                        Text(stringResource(R.string.deselect_all))
                    }
                }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    placeholder = { Text(if (isArtistMode) "Search artists..." else "Search songs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (isArtistMode) {
                            itemsIndexed(filteredArtists, key = { _, it -> it.id }) { index, artist ->
                                SelectionItem(
                                    title = artist.name,
                                    subtitle = artist.country,
                                    isSelected = checkedIds.contains(artist.id),
                                    onToggle = {
                                        if (checkedIds.contains(artist.id)) checkedIds.remove(artist.id)
                                        else checkedIds.add(artist.id)
                                    }
                                )
                            }
                        } else {
                            itemsIndexed(filteredSongs, key = { _, it -> it.id }) { index, song ->
                                SelectionItem(
                                    title = song.title,
                                    subtitle = song.artistName,
                                    model = song,
                                    isSelected = checkedIds.contains(song.id),
                                    onToggle = {
                                        if (checkedIds.contains(song.id)) checkedIds.remove(song.id)
                                        else checkedIds.add(song.id)
                                    }
                                )
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = checkedIds.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isArtistMode) {
                                    libraryViewModel.lookupArtistsMusicBrainz(checkedIds.toList())
                                } else {
                                    libraryViewModel.lookupMusicBrainz(checkedIds.toList())
                                }
                                MusicBrainzProgressDialog().show(childFragmentManager, "MB_PROGRESS")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_cloud_24dp), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${stringResource(R.string.start_lookup)} (${checkedIds.size})")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SelectionItem(
        title: String,
        subtitle: String?,
        isSelected: Boolean,
        onToggle: () -> Unit,
        model: Any? = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (model != null) {
                MediaImage(
                    model = model,
                    modifier = Modifier.size(56.dp).padding(4.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        }
    }

    companion object {
        private const val EXTRA_ARTISTS = "extra_artists"

        fun create(songs: List<Song>): MusicBrainzSelectionDialog {
            return MusicBrainzSelectionDialog().withArgs {
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
            }
        }
        
        fun createArtists(artists: List<com.mardous.projectmusic.data.local.database.metadata.ArtistEntity>): MusicBrainzSelectionDialog {
            return MusicBrainzSelectionDialog().withArgs {
                putParcelableArrayList(EXTRA_ARTISTS, ArrayList(artists))
            }
        }
    }
}
