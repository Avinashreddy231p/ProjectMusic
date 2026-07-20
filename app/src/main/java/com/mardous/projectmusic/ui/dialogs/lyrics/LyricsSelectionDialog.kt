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

package com.mardous.projectmusic.ui.dialogs.lyrics

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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.withArgs
import com.mardous.projectmusic.ui.component.compose.BottomSheetDialogSurface
import com.mardous.projectmusic.data.model.SelectionItemData
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LyricsSelectionDialog : BottomSheetDialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    
    private val items by lazy {
        arguments?.getParcelableArrayList<SelectionItemData>(EXTRA_ITEMS) ?: emptyList()
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
                    LyricsSelectionScreen(
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun LyricsSelectionScreen(
        onDismiss: () -> Unit
    ) {
        var searchQuery by rememberSaveable { mutableStateOf("") }
        
        val allIds = items.map { it.id }
        val checkedIds = rememberSaveable { mutableStateListOf<Long>().apply { addAll(allIds) } }
        
        val filteredItems = remember(searchQuery) {
            if (searchQuery.isBlank()) items else items.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.subtitle?.contains(searchQuery, ignoreCase = true) == true
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
                        text = stringResource(R.string.lyrics_selection_title),
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
                    placeholder = { Text("Search...") },
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
                        itemsIndexed(filteredItems, key = { _, it -> it.id }) { _, item ->
                            SelectionItem(
                                title = item.title,
                                subtitle = item.subtitle,
                                isSelected = checkedIds.contains(item.id),
                                onToggle = {
                                    if (checkedIds.contains(item.id)) checkedIds.remove(item.id)
                                    else checkedIds.add(item.id)
                                }
                            )
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
                                libraryViewModel.lookupLyrics(checkedIds.toList())
                                val progressDialog = LyricsProgressDialog()
                                progressDialog.show(parentFragmentManager, "LYRICS_PROGRESS")
                                onDismiss()
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
        onToggle: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lyrics_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
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
        private const val EXTRA_ITEMS = "extra_items"

        fun create(songs: List<Song>): LyricsSelectionDialog {
            val list = songs.map { SelectionItemData(it.id, it.title, it.artistName, false) }
            return LyricsSelectionDialog().withArgs {
                putParcelableArrayList(EXTRA_ITEMS, ArrayList(list))
            }
        }
    }
}
