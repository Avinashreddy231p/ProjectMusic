package com.mardous.projectmusic.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.action.QueueClearingBehavior
import com.mardous.projectmusic.core.model.action.SongClickBehavior
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaybackSettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onEqualizerClick: () -> Unit = {},
    onSoundSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    // Dialog States
    var showSongClickDialog by remember { mutableStateOf(false) }
    var showClearQueueDialog by remember { mutableStateOf(false) }
    var showAlbumShuffleDialog by remember { mutableStateOf(false) }
    var showArtistShuffleDialog by remember { mutableStateOf(false) }
    var showStartupDialog by remember { mutableStateOf(false) }
    var showQueueNextDialog by remember { mutableStateOf(false) }

    if (showSongClickDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.on_song_click_title),
            options = SongClickBehavior.entries,
            selectedOption = uiState.songClickAction,
            onOptionSelected = { viewModel.setSongClickAction(it) },
            onDismissRequest = { showSongClickDialog = false },
            optionTitle = { stringResource(it.titleRes) }
        )
    }

    if (showClearQueueDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.on_clear_queue_title),
            options = QueueClearingBehavior.entries,
            selectedOption = uiState.clearQueueAction,
            onOptionSelected = { viewModel.setClearQueueAction(it) },
            onDismissRequest = { showClearQueueDialog = false },
            optionTitle = { stringResource(it.titleRes) }
        )
    }

    if (showAlbumShuffleDialog) {
        val entries = context.resources.getStringArray(R.array.pref_album_shuffle_mode_entries)
        val values = context.resources.getStringArray(R.array.pref_album_shuffle_mode_values)
        SingleChoiceDialog(
            title = stringResource(R.string.album_shuffle_mode_title),
            options = values.toList(),
            selectedOption = uiState.albumShuffleMode,
            onOptionSelected = { viewModel.setAlbumShuffleMode(it) },
            onDismissRequest = { showAlbumShuffleDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    if (showArtistShuffleDialog) {
        val entries = context.resources.getStringArray(R.array.pref_artist_shuffle_mode_entries)
        val values = context.resources.getStringArray(R.array.pref_artist_shuffle_mode_values)
        SingleChoiceDialog(
            title = stringResource(R.string.artist_shuffle_mode_title),
            options = values.toList(),
            selectedOption = uiState.artistShuffleMode,
            onOptionSelected = { viewModel.setArtistShuffleMode(it) },
            onDismissRequest = { showArtistShuffleDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    if (showStartupDialog) {
        val entries = context.resources.getStringArray(R.array.pref_play_on_startup_entries)
        val values = context.resources.getStringArray(R.array.pref_play_on_startup_values)
        SingleChoiceDialog(
            title = stringResource(R.string.play_on_startup_title),
            options = values.toList(),
            selectedOption = uiState.playOnStartupMode,
            onOptionSelected = { viewModel.setPlayOnStartupMode(it) },
            onDismissRequest = { showStartupDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    if (showQueueNextDialog) {
        val entries = context.resources.getStringArray(R.array.queue_next_mode_entries)
        val values = context.resources.getStringArray(R.array.queue_next_mode_values)
        SingleChoiceDialog(
            title = stringResource(R.string.queue_next_mode_title),
            options = values.toList(),
            selectedOption = uiState.queueNextMode,
            onOptionSelected = { viewModel.setQueueNextMode(it) },
            onDismissRequest = { showQueueNextDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.playback_title)) },
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
            // --- AUDIO ENGINE ---
            item { DashboardCategoryHeader("Audio Engine") }
            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = "System Equalizer",
                        summary = "Refine your sound profile",
                        icon = Icons.Default.Equalizer,
                        onClick = onEqualizerClick
                    )
                    ExpressivePreferenceItem(
                        title = "Sound Settings",
                        summary = "Advanced audio effects and processing",
                        icon = R.drawable.ic_tune_24dp,
                        onClick = onSoundSettingsClick
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.enable_audio_offload_title),
                        summary = stringResource(R.string.enable_audio_offload_description),
                        checked = uiState.audioOffload,
                        onCheckedChange = { viewModel.setAudioOffload(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.enable_audio_float_output_title),
                        summary = stringResource(R.string.enable_audio_float_output_description),
                        checked = uiState.audioFloatOutput,
                        onCheckedChange = { viewModel.setAudioFloatOutput(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.skip_silence_title),
                        summary = stringResource(R.string.skip_silence_description),
                        checked = uiState.skipSilence,
                        onCheckedChange = { viewModel.setSkipSilence(it) }
                    )
                }
            }

            // --- STARTUP ---
            item { DashboardCategoryHeader("Startup & Focus") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.play_on_startup_title),
                        summary = uiState.playOnStartupMode.replace("_", " ").replaceFirstChar { it.uppercase() },
                        icon = R.drawable.ic_play_24dp,
                        onClick = { showStartupDialog = true }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.ignore_audio_focus_title),
                        checked = uiState.ignoreAudioFocus,
                        onCheckedChange = { viewModel.setIgnoreAudioFocus(it) },
                        icon = R.drawable.ic_pause_24dp
                    )
                }
            }

            // --- QUEUE MANAGEMENT ---
            item { DashboardCategoryHeader("Queue Management") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.on_song_click_title),
                        summary = stringResource(uiState.songClickAction.titleRes),
                        icon = R.drawable.ic_touch_app_24dp,
                        onClick = { showSongClickDialog = true }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.on_clear_queue_title),
                        summary = stringResource(uiState.clearQueueAction.titleRes),
                        icon = R.drawable.ic_clear_all_24dp,
                        onClick = { showClearQueueDialog = true }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.queue_next_mode_title),
                        summary = uiState.queueNextMode,
                        icon = R.drawable.ic_queue_play_next_24dp,
                        onClick = { showQueueNextDialog = true }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.clear_queue_on_completion_title),
                        checked = uiState.clearQueueOnCompletion,
                        onCheckedChange = { viewModel.setClearQueueOnCompletion(it) }
                    )
                }
            }

            // --- DISCOVERY ---
            item { DashboardCategoryHeader("Discovery & Search") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.play_option_always_visible_title),
                        checked = uiState.playOptionAlwaysVisible,
                        onCheckedChange = { viewModel.setPlayOptionAlwaysVisible(it) },
                        icon = R.drawable.ic_play_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.play_option_whole_list_title),
                        checked = uiState.playOptionWholeList,
                        onCheckedChange = { viewModel.setPlayOptionWholeList(it) },
                        icon = R.drawable.ic_playlist_play_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.play_all_songs_when_searching_title),
                        checked = uiState.playAllSongsWhenSearching,
                        onCheckedChange = { viewModel.setPlayAllSongsWhenSearching(it) },
                        icon = R.drawable.ic_search_24dp
                    )
                }
            }

            // --- SHUFFLE ---
            item { DashboardCategoryHeader("Shuffle Experience") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.remember_shuffle_mode_title),
                        checked = uiState.rememberShuffleMode,
                        onCheckedChange = { viewModel.setRememberShuffleMode(it) },
                        icon = R.drawable.ic_shuffle_24dp
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.album_shuffle_mode_title),
                        summary = uiState.albumShuffleMode.replace("_", " ").replaceFirstChar { it.uppercase() },
                        icon = R.drawable.ic_album_24dp,
                        onClick = { showAlbumShuffleDialog = true }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.artist_shuffle_mode_title),
                        summary = uiState.artistShuffleMode.replace("_", " ").replaceFirstChar { it.uppercase() },
                        icon = R.drawable.ic_artist_24dp,
                        onClick = { showArtistShuffleDialog = true }
                    )
                }
            }

            // --- BEHAVIOR ---
            item { DashboardCategoryHeader("Connectivity Behavior") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.resume_on_connect_title),
                        checked = uiState.resumeOnConnect,
                        onCheckedChange = { viewModel.setResumeOnConnect(it) },
                        icon = R.drawable.ic_headphones_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.pause_on_disconnect_title),
                        checked = uiState.pauseOnDisconnect,
                        onCheckedChange = { viewModel.setPauseOnDisconnect(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.resume_on_bluetooth_connect_title),
                        checked = uiState.resumeOnBluetoothConnect,
                        onCheckedChange = { viewModel.setResumeOnBluetoothConnect(it) },
                        icon = R.drawable.ic_media_bluetooth_on_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.pause_on_bluetooth_disconnect_title),
                        checked = uiState.pauseOnBluetoothDisconnect,
                        onCheckedChange = { viewModel.setPauseOnBluetoothDisconnect(it) }
                    )
                }
            }

            // --- CONTROLS ---
            item { DashboardCategoryHeader("Controls") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSliderItem(
                        title = "Seek Interval",
                        value = uiState.seekInterval.toFloat(),
                        onValueChange = { viewModel.setSeekInterval(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 10,
                        icon = R.drawable.ic_fast_forward_24dp,
                        valueDisplay = { "${it.toInt()}s" }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.rewind_with_back_title),
                        checked = uiState.rewindWithBack,
                        onCheckedChange = { viewModel.setRewindWithBack(it) },
                        icon = R.drawable.ic_previous_24dp
                    )
                }
            }
        }
    }
}
