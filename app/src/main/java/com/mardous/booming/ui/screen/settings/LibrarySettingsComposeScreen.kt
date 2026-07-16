package com.mardous.booming.ui.screen.settings

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mardous.booming.R
import com.mardous.booming.ui.component.compose.preferences.*
import com.mardous.booming.ui.dialogs.library.BlacklistWhitelistDialog
import com.mardous.booming.data.local.room.InclExclDao
import com.mardous.booming.ui.screen.settings.search.SettingsSearchHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibrarySettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onReloadSuggestions: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showHistoryIntervalDialog by remember { mutableStateOf(false) }
    var showLastAddedIntervalDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isSearchActive) {
                MediumTopAppBar(
                    title = { Text(stringResource(R.string.library_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(painterResource(R.drawable.ic_back_24dp), null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(painterResource(R.drawable.ic_search_24dp), "Search")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            } else {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = true,
                    onActiveChange = { isSearchActive = it },
                    placeholder = { Text(stringResource(R.string.search_label)) },
                    leadingIcon = {
                        IconButton(onClick = { isSearchActive = false }) {
                            Icon(painterResource(R.drawable.ic_back_24dp), null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    val results = remember(searchQuery) { SettingsSearchHelper.search(searchQuery) }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results) { result ->
                            ListItem(
                                headlineContent = { Text(result.title) },
                                supportingContent = { Text(result.summary) },
                                overlineContent = { Text(result.parentTitle) },
                                modifier = Modifier.clickable {
                                    isSearchActive = false
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (showHistoryIntervalDialog) {
            val entries = stringArrayResource(R.array.pref_playlists_cutoff_entries)
            val values = stringArrayResource(R.array.pref_playlists_cutoff_values)
            SingleChoiceDialog(
                title = stringResource(R.string.history_interval_title),
                options = entries.toList(),
                selectedOption = entries[values.indexOf(uiState.historyInterval).coerceAtLeast(0)],
                onOptionSelected = { name ->
                    val value = values[entries.indexOf(name)]
                    viewModel.setHistoryInterval(value)
                },
                onDismissRequest = { showHistoryIntervalDialog = false }
            )
        }

        if (showLastAddedIntervalDialog) {
            val entries = stringArrayResource(R.array.pref_playlists_cutoff_entries)
            val values = stringArrayResource(R.array.pref_playlists_cutoff_values)
            SingleChoiceDialog(
                title = stringResource(R.string.last_added_interval_title),
                options = entries.toList(),
                selectedOption = entries[values.indexOf(uiState.lastAddedInterval).coerceAtLeast(0)],
                onOptionSelected = { name ->
                    val value = values[entries.indexOf(name)]
                    viewModel.setLastAddedInterval(value)
                    onReloadSuggestions()
                },
                onDismissRequest = { showLastAddedIntervalDialog = false }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- GENERAL ---
            item { PreferenceCategoryHeader(title = "General Management") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.trash_music_title),
                        summary = stringResource(R.string.trash_music_summary),
                        icon = R.drawable.ic_delete_24dp,
                        trailingContent = {
                            Switch(checked = uiState.trashMusicFiles, onCheckedChange = { viewModel.setTrashMusicFiles(it) })
                        }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.ignore_articles_when_sorting_title),
                        summary = stringResource(R.string.ignore_articles_when_sorting_summary),
                        icon = R.drawable.ic_sort_24dp,
                        trailingContent = {
                            Switch(checked = uiState.ignoreArticlesWhenSorting, onCheckedChange = { viewModel.setIgnoreArticlesWhenSorting(it) })
                        },
                        showDivider = false
                    )
                }
            }

            // --- PLAYLISTS ---
            item { PreferenceCategoryHeader(title = "Auto Playlists") }
            item {
                var playlistsExpanded by remember { mutableStateOf(false) }
                SplitButtonPreference(
                    title = stringResource(R.string.enable_history_title),
                    summary = stringResource(R.string.enable_history_summary),
                    checked = uiState.enableHistoryPlaylist,
                    onCheckedChange = { viewModel.setEnableHistoryPlaylist(it) },
                    expanded = playlistsExpanded,
                    onExpandClick = { playlistsExpanded = !playlistsExpanded },
                    icon = R.drawable.ic_history_24dp
                ) {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.history_interval_title),
                        summary = stringArrayResource(R.array.pref_playlists_cutoff_entries)[
                            stringArrayResource(R.array.pref_playlists_cutoff_values).indexOf(uiState.historyInterval).coerceAtLeast(0)
                        ],
                        onClick = { if (uiState.enableHistoryPlaylist) showHistoryIntervalDialog = true }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.last_added_interval_title),
                        summary = stringArrayResource(R.array.pref_playlists_cutoff_entries)[
                            stringArrayResource(R.array.pref_playlists_cutoff_values).indexOf(uiState.lastAddedInterval).coerceAtLeast(0)
                        ],
                        onClick = { showLastAddedIntervalDialog = true },
                        showDivider = false
                    )
                }
            }

            // --- FOLDERS ---
            item { PreferenceCategoryHeader(title = "Folder Management") }
            item {
                var foldersExpanded by remember { mutableStateOf(false) }
                SplitButtonPreference(
                    title = stringResource(R.string.recursive_folder_actions_title),
                    summary = stringResource(R.string.recursive_folder_actions_summary),
                    checked = uiState.recursiveFolderActions.isNotEmpty(),
                    onCheckedChange = { if (!it) viewModel.setRecursiveFolderActions(emptySet()) },
                    expanded = foldersExpanded,
                    onExpandClick = { foldersExpanded = !foldersExpanded },
                    icon = R.drawable.ic_folder_24dp
                ) {
                    val entries = stringArrayResource(R.array.pref_recursive_folder_action_entries)
                    val values = stringArrayResource(R.array.pref_recursive_folder_action_values)
                    
                    values.forEachIndexed { index, value ->
                        val isChecked = uiState.recursiveFolderActions.contains(value)
                        HubSwitch(
                            title = entries[index],
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val newActions = if (checked) {
                                    uiState.recursiveFolderActions + value
                                } else {
                                    uiState.recursiveFolderActions - value
                                }
                                viewModel.setRecursiveFolderActions(newActions)
                            }
                        )
                    }
                }
            }

            // --- EXCLUSION & FILTERING ---
            item { PreferenceCategoryHeader(title = "Exclusion & Filtering") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.whitelist_title),
                        summary = stringResource(R.string.whitelist_summary),
                        icon = R.drawable.ic_whitelist_24dp,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { 
                                    BlacklistWhitelistDialog.newInstance(InclExclDao.WHITELIST).show((context as FragmentActivity).supportFragmentManager, "WHITELIST_DIALOG")
                                }) {
                                    Icon(painterResource(R.drawable.ic_edit_24dp), "Edit")
                                }
                                Switch(checked = uiState.whitelistEnabled, onCheckedChange = { viewModel.toggleWhitelist(it) })
                            }
                        }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.blacklist_title),
                        summary = stringResource(R.string.blacklist_summary),
                        icon = R.drawable.ic_blacklist_24dp,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { 
                                    BlacklistWhitelistDialog.newInstance(InclExclDao.BLACKLIST).show((context as FragmentActivity).supportFragmentManager, "BLACKLIST_DIALOG")
                                }) {
                                    Icon(painterResource(R.drawable.ic_edit_24dp), "Edit")
                                }
                                Switch(checked = uiState.blacklistEnabled, onCheckedChange = { viewModel.toggleBlacklist(it) })
                            }
                        },
                        showDivider = false
                    )
                }
            }

            item {
                SliderPreferenceItem(
                    title = stringResource(R.string.min_song_duration_title),
                    value = uiState.minimumSongDuration.toFloat(),
                    onValueChange = { viewModel.setMinimumSongDuration(it.toInt()) },
                    valueRange = 0f..120f,
                    icon = R.drawable.ic_speed_24dp,
                    valueDisplay = { it.toInt().toString() + "s" }
                )
            }

            item {
                SliderPreferenceItem(
                    title = stringResource(R.string.artist_min_songs_title),
                    value = uiState.artistMinimumSongs.toFloat(),
                    onValueChange = { viewModel.setArtistMinimumSongs(it.toInt()) },
                    valueRange = 1f..8f,
                    icon = R.drawable.ic_person_24dp
                )
            }

            item {
                SliderPreferenceItem(
                    title = stringResource(R.string.album_min_songs_title),
                    value = uiState.albumMinimumSongs.toFloat(),
                    onValueChange = { viewModel.setAlbumMinimumSongs(it.toInt()) },
                    valueRange = 1f..8f,
                    icon = R.drawable.ic_album_24dp
                )
            }
        }
    }
}
