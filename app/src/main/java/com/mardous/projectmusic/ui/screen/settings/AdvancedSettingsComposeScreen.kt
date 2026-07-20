package com.mardous.projectmusic.ui.screen.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.mardous.projectmusic.R
import com.mardous.projectmusic.BuildConfig
import com.mardous.projectmusic.extensions.files.getFormattedFileName
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.ui.dialogs.MultiCheckDialog
import androidx.compose.runtime.livedata.observeAsState
import com.mardous.projectmusic.ui.screen.settings.search.SettingsSearchHelper
import com.mardous.projectmusic.core.model.task.Event
import com.mardous.projectmusic.ui.screen.update.UpdateViewModel
import com.mardous.projectmusic.ui.screen.update.UpdateSearchResult
import com.mardous.projectmusic.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdvancedSettingsComposeScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onBackClick: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onClearCache: () -> Unit,
    highlightKey: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateEvent by updateViewModel.updateEventObservable.observeAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val resources = context.resources
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    // --- SCROLL TO HIGHLIGHTED ---
    LaunchedEffect(highlightedKey) {
        if (highlightedKey != null) {
            delay(300)
            val index = when (highlightedKey) {
                "language_name" -> 1
                "enable_rotation_lock" -> 1
                "backup_data", "restore_data" -> 3
                "mp3_index_seeking", "pause_on_zero_volume", "stop_when_closed_from_recents" -> 5
                "search_for_update", "update_search_mode", "experimental_updates" -> 7
                "clear_cache", "diagnostics" -> 9
                else -> -1
            }
            if (index != -1) {
                listState.animateScrollToItem(index)
                delay(2000)
                highlightedKey = null
            }
        }
    }

    // --- UPDATE ERROR FEEDBACK ---
    LaunchedEffect(updateEvent) {
        val result = updateEvent?.getContentIfNotConsumed()
        if (result != null && result.state == UpdateSearchResult.State.Failed && result.wasFromUser) {
            val error = result.error
            val errorMessage = when (error) {
                is java.io.IOException -> resources.getString(R.string.update_error_network)
                is IllegalStateException -> {
                    val msg = error.message ?: ""
                    if (msg.contains("rate limit", true)) {
                        resources.getString(R.string.update_error_api_limit)
                    } else if (msg.contains("not configured", true)) {
                        msg
                    } else if (msg.contains("No suitable", true)) {
                        resources.getString(R.string.update_error_not_found)
                    } else if (msg.contains("GitHub", true)) {
                        msg // Use detailed GitHub error message directly
                    } else {
                        resources.getString(R.string.could_not_check_for_updates_detailed, msg.ifBlank { "Unknown error" })
                    }
                }
                else -> {
                    val msg = error?.message
                    if (msg.isNullOrBlank()) {
                        resources.getString(R.string.could_not_check_for_updates_detailed, error?.javaClass?.simpleName ?: "Unknown error")
                    } else {
                        resources.getString(R.string.could_not_check_for_updates_detailed, msg)
                    }
                }
            }
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Long
            )
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUpdateModeDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        val entries = context.resources.getStringArray(R.array.pref_language_names)
        val values = context.resources.getStringArray(R.array.pref_language_codes)
        SingleChoiceDialog(
            title = stringResource(R.string.app_language_title),
            options = values.toList(),
            selectedOption = uiState.languageName,
            onOptionSelected = { viewModel.setLanguageName(it) },
            onDismissRequest = { showLanguageDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    if (showUpdateModeDialog) {
        val entries = context.resources.getStringArray(R.array.pref_update_mode_titles)
        val values = context.resources.getStringArray(R.array.pref_update_mode_values)
        SingleChoiceDialog(
            title = stringResource(R.string.auto_update_title),
            options = values.toList(),
            selectedOption = uiState.updateSearchMode,
            onOptionSelected = { viewModel.setUpdateSearchMode(it) },
            onDismissRequest = { showUpdateModeDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

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
            MediumTopAppBar(
                title = { Text(stringResource(R.string.advanced_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painter = painterResource(R.drawable.ic_back_24dp), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- GENERAL ---
            item { DashboardCategoryHeader("General") }
            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.app_language_title),
                        summary = uiState.languageName.replaceFirstChar { it.uppercase() },
                        icon = R.drawable.ic_language_24dp,
                        onClick = { showLanguageDialog = true }
                    )
                    ExpressiveSwitchItem(
                        title = "Rotation Lock",
                        checked = uiState.rotationLock,
                        onCheckedChange = { viewModel.setRotationLock(it) },
                        icon = R.drawable.ic_phone_android_24dp
                    )
                }
            }

            // --- BACKUP & RESTORE ---
            item { DashboardCategoryHeader("Data Management") }
            item {
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { 
                            createBackupLauncher.launch(getFormattedFileName("Backup", BackupHelper.BACKUP_EXTENSION))
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(painterResource(R.drawable.ic_file_export_24dp), null)
                        Spacer(Modifier.width(8.dp))
                        Text("Backup")
                    }
                    FilledTonalButton(
                        onClick = { 
                            selectBackupLauncher.launch(arrayOf("application/*"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(painterResource(R.drawable.ic_restart_alt_24dp), null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restore")
                    }
                }
            }

            // --- PLAYBACK ENGINE ---
            item { DashboardCategoryHeader("Engine Internals") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.mp3_index_seeking_title),
                        summary = "Direct byte-offset seeking",
                        checked = uiState.mp3IndexSeeking,
                        onCheckedChange = { viewModel.setMp3IndexSeeking(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.pause_on_zero_volume_title),
                        checked = uiState.pauseOnZeroVolume,
                        onCheckedChange = { viewModel.setPauseOnZeroVolume(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.stop_when_closed_from_recents_title),
                        checked = uiState.stopWhenClosedFromRecents,
                        onCheckedChange = { viewModel.setStopWhenClosedFromRecents(it) }
                    )
                }
            }

            // --- UPDATES ---
            if (uiState.isUpdaterEnabled) {
                item { DashboardCategoryHeader("System Updates") }
                item {
                    SegmentedPreferenceGroup {
                        ExpressivePreferenceItem(
                            title = "Search for Updates",
                            summary = "Stay on the latest version",
                            icon = R.drawable.ic_update_24dp,
                            onClick = onCheckForUpdates,
                            modifier = Modifier.background(
                                if (highlightedKey == "search_for_update") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                        )
                        SegmentedPreferenceItem(
                            title = stringResource(R.string.auto_update_title),
                            summary = uiState.updateSearchMode.replaceFirstChar { it.uppercase() },
                            onClick = { showUpdateModeDialog = true }
                        )
                        ExpressiveSwitchItem(
                            title = "Experimental Updates",
                            summary = "Receive beta and alpha builds",
                            checked = uiState.experimentalUpdates,
                            onCheckedChange = { viewModel.setExperimentalUpdates(it) }
                        )
                    }
                }
            }

            // --- TROUBLESHOOTING ---
            item { DashboardCategoryHeader("Troubleshooting") }
            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = "Clear Image Cache",
                        summary = "Free up storage used by covers",
                        icon = R.drawable.ic_delete_24dp,
                        onClick = onClearCache
                    )
                    ExpressivePreferenceItem(
                        title = "System Diagnostics",
                        summary = "Index settings and view device info",
                        icon = R.drawable.ic_bug_report_24dp,
                        onClick = {
                            SettingsSearchHelper.indexSettings(context)
                        }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Device Information", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        DiagnosticInfoRow("App Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        DiagnosticInfoRow("Android Version", "${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                        DiagnosticInfoRow("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                        DiagnosticInfoRow("Build", android.os.Build.DISPLAY)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
