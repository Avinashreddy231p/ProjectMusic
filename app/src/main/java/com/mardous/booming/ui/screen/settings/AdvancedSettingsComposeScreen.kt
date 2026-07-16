package com.mardous.booming.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.booming.ui.component.compose.preferences.*
import com.mardous.booming.ui.dialogs.MultiCheckDialog
import com.mardous.booming.ui.screen.settings.search.SettingsSearchHelper
import com.mardous.booming.util.BackupContent
import com.mardous.booming.util.BackupHelper
import com.mardous.booming.extensions.files.getFormattedFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdvancedSettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onCheckForUpdates: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUpdateModeDialog by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/*")) { uri ->
        if (uri != null) {
            GlobalScope.launch(Dispatchers.IO) {
                BackupHelper.createBackup(context, uri)
            }
        }
    }

    val selectBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selection ->
        if (selection != null) {
            val contentEntries = BackupContent.entries
            val items = contentEntries.map { context.resources.getString(it.titleRes) }
            MultiCheckDialog.Builder(context)
                .title(R.string.select_content_to_restore)
                .items(items)
                .createDialog { _, whichPos, _ ->
                    val content = contentEntries.filterIndexed { i, _ -> whichPos.contains(i) }
                    GlobalScope.launch(Dispatchers.IO) {
                        BackupHelper.restoreBackup(context, selection, content)
                    }
                    true
                }
                .show((context as FragmentActivity).supportFragmentManager, "RESTORE_DIALOG")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isSearchActive) {
                MediumTopAppBar(
                    title = { Text(stringResource(R.string.advanced_title)) },
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
                                    // Handle navigation if needed, but here we stay in Advanced
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (showLanguageDialog) {
            val languageNames = stringArrayResource(R.array.pref_language_names)
            val languageCodes = stringArrayResource(R.array.pref_language_codes)
            SingleChoiceDialog(
                title = stringResource(R.string.app_language_title),
                options = languageNames.toList(),
                selectedOption = languageNames[languageCodes.indexOf(uiState.languageName).coerceAtLeast(0)],
                onOptionSelected = { name ->
                    val code = languageCodes[languageNames.indexOf(name)]
                    viewModel.setLanguageName(code)
                },
                onDismissRequest = { showLanguageDialog = false }
            )
        }

        if (showUpdateModeDialog) {
            val modeTitles = stringArrayResource(R.array.pref_update_mode_titles)
            val modeValues = stringArrayResource(R.array.pref_update_mode_values)
            SingleChoiceDialog(
                title = stringResource(R.string.auto_update_title),
                options = modeTitles.toList(),
                selectedOption = modeTitles[modeValues.indexOf(uiState.updateSearchMode).coerceAtLeast(0)],
                onOptionSelected = { title ->
                    val value = modeValues[modeTitles.indexOf(title)]
                    viewModel.setUpdateSearchMode(value)
                },
                onDismissRequest = { showUpdateModeDialog = false }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- LOCALIZATION & SYSTEM ---
            item { PreferenceCategoryHeader(title = "System & Localization") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.app_language_title),
                        summary = stringArrayResource(R.array.pref_language_names)[
                            stringArrayResource(R.array.pref_language_codes).indexOf(uiState.languageName).coerceAtLeast(0)
                        ],
                        icon = R.drawable.ic_translate_24dp,
                        onClick = { showLanguageDialog = true }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.rotation_lock_title),
                        summary = stringResource(R.string.rotation_lock_summary),
                        icon = R.drawable.ic_tune_24dp,
                        trailingContent = {
                            Switch(checked = uiState.rotationLock, onCheckedChange = { viewModel.setRotationLock(it) })
                        },
                        showDivider = false
                    )
                }
            }

            // --- DATA MANAGEMENT ---
            item { PreferenceCategoryHeader(title = "Data Management") }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            createBackupLauncher.launch(getFormattedFileName("Backup", BackupHelper.BACKUP_EXTENSION))
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_file_export_24dp), null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_data_title))
                    }
                    FilledTonalButton(
                        onClick = { selectBackupLauncher.launch(arrayOf("application/*")) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_restart_alt_24dp), null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.restore_backup_title))
                    }
                }
            }

            // --- PLAYBACK ENGINE ---
            item { PreferenceCategoryHeader(title = "Playback Engine") }
            item {
                var engineExpanded by remember { mutableStateOf(false) }
                SplitButtonPreference(
                    title = "MP3 Index Seeking",
                    summary = stringResource(R.string.mp3_index_seeking_summary),
                    checked = uiState.mp3IndexSeeking,
                    onCheckedChange = { viewModel.setMp3IndexSeeking(it) },
                    expanded = engineExpanded,
                    onExpandClick = { engineExpanded = !engineExpanded },
                    icon = R.drawable.ic_speed_24dp
                ) {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.pause_on_zero_volume_title),
                        summary = stringResource(R.string.pause_on_zero_volume_summary),
                        trailingContent = {
                            Switch(checked = uiState.pauseOnZeroVolume, onCheckedChange = { viewModel.setPauseOnZeroVolume(it) })
                        }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.stop_when_closed_from_recents_title),
                        summary = stringResource(R.string.stop_when_closed_from_recents_summary),
                        trailingContent = {
                            Switch(checked = uiState.stopWhenClosedFromRecents, onCheckedChange = { viewModel.setStopWhenClosedFromRecents(it) })
                        },
                        showDivider = false
                    )
                }
            }

            // --- LIBRARY ENGINE ---
            item { PreferenceCategoryHeader(title = "Library Engine") }
            item {
                var libraryExpanded by remember { mutableStateOf(false) }
                SplitButtonPreference(
                    title = "Ignore Media Store",
                    summary = stringResource(R.string.ignore_media_store_album_covers_summary),
                    checked = uiState.ignoreMediaStore,
                    onCheckedChange = { viewModel.setIgnoreMediaStore(it) },
                    expanded = libraryExpanded,
                    onExpandClick = { libraryExpanded = !libraryExpanded },
                    icon = R.drawable.ic_library_music_24dp
                ) {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.use_folder_album_cover_title),
                        summary = stringResource(R.string.use_folder_album_cover_summary),
                        trailingContent = {
                            Switch(checked = uiState.useFolderArt, onCheckedChange = { viewModel.setUseFolderArt(it) })
                        },
                        showDivider = false
                    )
                }
            }

            // --- UPDATES ---
            item { PreferenceCategoryHeader(title = "Software Updates") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.auto_update_title),
                        summary = stringArrayResource(R.array.pref_update_mode_titles)[
                            stringArrayResource(R.array.pref_update_mode_values).indexOf(uiState.updateSearchMode).coerceAtLeast(0)
                        ],
                        icon = R.drawable.ic_update_24dp,
                        onClick = { showUpdateModeDialog = true }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.receive_experimental_updates_title),
                        summary = stringResource(R.string.receive_experimental_updates_summary),
                        icon = R.drawable.ic_bug_report_24dp,
                        trailingContent = {
                            Switch(checked = uiState.experimentalUpdates, onCheckedChange = { viewModel.setExperimentalUpdates(it) })
                        },
                        showDivider = false
                    )
                }
            }
            item {
                ExpressivePreferenceItem(
                    title = stringResource(R.string.search_for_update_title),
                    summary = "Tap to manually check for new versions",
                    icon = R.drawable.ic_update_24dp,
                    onClick = onCheckForUpdates
                )
            }
        }
    }
}
