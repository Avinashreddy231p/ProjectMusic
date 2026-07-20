package com.mardous.projectmusic.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.local.database.dao.InclExclDao
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.ui.dialogs.MultiCheckDialog
import com.mardous.projectmusic.ui.dialogs.library.BlacklistWhitelistDialog
import com.mardous.projectmusic.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibrarySettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onReloadSuggestions: () -> Unit,
    onStartDirectoryClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    var showHistoryIntervalDialog by remember { mutableStateOf(false) }
    var showLastAddedIntervalDialog by remember { mutableStateOf(false) }
    var showRecursiveActionsDialog by remember { mutableStateOf(false) }

    if (showRecursiveActionsDialog) {
        val entries = context.resources.getStringArray(R.array.pref_recursive_folder_action_entries).toList()
        val values = context.resources.getStringArray(R.array.pref_recursive_folder_action_values).toList()
        (context as? FragmentActivity)?.let { activity ->
            MultiCheckDialog.Builder(activity)
                .title(R.string.recursive_folder_actions_title)
                .items(entries)
                .createDialog { _, whichPos, _ ->
                    val selectedValues = whichPos.map { values[it] }.toSet()
                    viewModel.setRecursiveFolderActions(selectedValues)
                    true
                }
                .show(activity.supportFragmentManager, "RECURSIVE_DIALOG")
        }
        showRecursiveActionsDialog = false
    }

    if (showHistoryIntervalDialog) {
        val entries = context.resources.getStringArray(R.array.pref_playlists_cutoff_entries)
        val values = context.resources.getStringArray(R.array.pref_playlists_cutoff_values)
        SingleChoiceDialog(
            title = stringResource(R.string.history_interval_title),
            options = values.toList(),
            selectedOption = uiState.historyInterval,
            onOptionSelected = { viewModel.setHistoryInterval(it) },
            onDismissRequest = { showHistoryIntervalDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    if (showLastAddedIntervalDialog) {
        val entries = context.resources.getStringArray(R.array.pref_playlists_cutoff_entries)
        val values = context.resources.getStringArray(R.array.pref_playlists_cutoff_values)
        SingleChoiceDialog(
            title = stringResource(R.string.last_added_interval_title),
            options = values.toList(),
            selectedOption = uiState.lastAddedInterval,
            onOptionSelected = { viewModel.setLastAddedInterval(it) },
            onDismissRequest = { showLastAddedIntervalDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painter = painterResource(R.drawable.ic_back_24dp), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- FOLDER MANAGEMENT ---
            item { DashboardCategoryHeader("Folder Management") }
            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.recursive_folder_actions_title),
                        summary = stringResource(R.string.recursive_folder_actions_summary),
                        icon = R.drawable.ic_folder_24dp,
                        onClick = { showRecursiveActionsDialog = true }
                    )
                    ExpressivePreferenceItem(
                        title = "Folder Whitelist",
                        summary = "Scan only these directories",
                        icon = R.drawable.ic_folder_24dp,
                        trailingContent = {
                            Switch(checked = uiState.whitelistEnabled, onCheckedChange = { viewModel.toggleWhitelist(it) })
                        },
                        onClick = {
                            (context as? FragmentActivity)?.let {
                                BlacklistWhitelistDialog.newInstance(InclExclDao.WHITELIST).show(it.supportFragmentManager, "WHITELIST_DIALOG")
                            }
                        }
                    )
                    ExpressivePreferenceItem(
                        title = "Blacklist",
                        summary = "Hide unwanted folders",
                        icon = R.drawable.ic_blacklist_24dp,
                        trailingContent = {
                            Switch(checked = uiState.blacklistEnabled, onCheckedChange = { viewModel.toggleBlacklist(it) })
                        },
                        onClick = {
                            (context as? FragmentActivity)?.let {
                                BlacklistWhitelistDialog.newInstance(InclExclDao.BLACKLIST).show(it.supportFragmentManager, "BLACKLIST_DIALOG")
                            }
                        }
                    )

                    ExpressivePreferenceItem(
                        title = "Start Directory",
                        summary = Preferences.startDirectory.path,
                        icon = R.drawable.ic_folder_24dp,
                        onClick = onStartDirectoryClick
                    )
                }
            }

            // --- SMART PLAYLISTS ---
            item { DashboardCategoryHeader("Smart Playlists") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.enable_history_title),
                        checked = uiState.enableHistoryPlaylist,
                        onCheckedChange = { viewModel.setEnableHistoryPlaylist(it) },
                        icon = R.drawable.ic_history_24dp
                    )
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.history_interval_title),
                        summary = uiState.historyInterval.replace("_", " ").replaceFirstChar { it.uppercase() },
                        enabled = uiState.enableHistoryPlaylist,
                        onClick = { showHistoryIntervalDialog = true }
                    )
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.last_added_interval_title),
                        summary = uiState.lastAddedInterval.replace("_", " ").replaceFirstChar { it.uppercase() },
                        onClick = { showLastAddedIntervalDialog = true }
                    )
                    ExpressiveSwitchItem(
                        title = "Locked Playlists",
                        summary = "Prevent accidental changes",
                        checked = uiState.lockedPlaylists,
                        onCheckedChange = { viewModel.setLockedPlaylists(it) },
                        icon = R.drawable.ic_lock_24dp
                    )
                }
            }

            // --- FILTERS & LIMITS ---
            item { DashboardCategoryHeader("Filtering & Limits") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "Only Album Artists",
                        checked = uiState.onlyAlbumArtists,
                        onCheckedChange = { viewModel.setOnlyAlbumArtists(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Ignore Singles",
                        checked = uiState.ignoreSingles,
                        onCheckedChange = { viewModel.setIgnoreSingles(it) }
                    )
                    ExpressiveSliderItem(
                        title = stringResource(R.string.min_song_duration_title),
                        value = uiState.minimumSongDuration.toFloat(),
                        onValueChange = { viewModel.setMinimumSongDuration(it.toInt()) },
                        valueRange = 0f..120f,
                        valueDisplay = { "${it.toInt()}s" }
                    )
                    ExpressiveSliderItem(
                        title = "Min Artist Songs",
                        value = uiState.artistMinimumSongs.toFloat(),
                        onValueChange = { viewModel.setArtistMinimumSongs(it.toInt()) },
                        valueRange = 1f..10f
                    )
                    ExpressiveSliderItem(
                        title = "Min Album Songs",
                        value = uiState.albumMinimumSongs.toFloat(),
                        onValueChange = { viewModel.setAlbumMinimumSongs(it.toInt()) },
                        valueRange = 1f..10f
                    )
                }
            }

            // --- METADATA ---
            item { DashboardCategoryHeader("Metadata & Artwork") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "Show Album Duration",
                        checked = uiState.showAlbumDuration,
                        onCheckedChange = { viewModel.setShowAlbumDuration(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Hierarchy Folder View",
                        checked = uiState.hierarchyFolderView,
                        onCheckedChange = { viewModel.setHierarchyFolderView(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Use Folder Art",
                        summary = "Prefer cover.jpg/folder.jpg files",
                        checked = uiState.useFolderArt,
                        onCheckedChange = { viewModel.setUseFolderArt(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Ignore Media Store",
                        summary = "Scan files directly for better metadata",
                        checked = uiState.ignoreMediaStore,
                        onCheckedChange = { viewModel.setIgnoreMediaStore(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.ignore_articles_when_sorting_title),
                        checked = uiState.ignoreArticlesWhenSorting,
                        onCheckedChange = { viewModel.setIgnoreArticlesWhenSorting(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.trash_music_title),
                        checked = uiState.trashMusicFiles,
                        onCheckedChange = { viewModel.setTrashMusicFiles(it) },
                        icon = R.drawable.ic_delete_24dp
                    )
                }
            }

            // --- MAINTENANCE ---
            item { DashboardCategoryHeader("Maintenance") }
            item {
                ExpressivePreferenceItem(
                    title = "Reload Suggestions",
                    summary = "Refresh Recently Added and Top Tracks",
                    icon = R.drawable.ic_restart_alt_24dp,
                    onClick = onReloadSuggestions
                )
            }
        }
    }
}
