package com.mardous.projectmusic.ui.screen.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.task.Event
import com.mardous.projectmusic.data.model.network.ScrobblingService
import com.mardous.projectmusic.data.model.network.LoginState
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.ui.screen.update.UpdateViewModel
import com.mardous.projectmusic.ui.screen.update.UpdateSearchResult
import com.mardous.projectmusic.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkSettingsComposeScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onBackClick: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onScrobblingLogin: (ScrobblingService) -> Unit,
    onPendingScrobblesClick: () -> Unit,
    onClearLyricsClick: () -> Unit,
    highlightKey: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateEvent by updateViewModel.updateEventObservable.observeAsState()
    val lastFmLoginState by viewModel.lastFmLoginState.collectAsState()
    val listenBrainzLoginState by viewModel.listenBrainzLoginState.collectAsState()
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
                "network_features", "wifi_only_network" -> 1
                "online_music_provider", "allow_online_artist_images", "allow_online_album_covers", "preferred_image_size" -> 3
                "lrclib_enabled", "betterlyrics_enabled", "lyrically_enabled", "genius_enabled", "clear_lyrics" -> 5
                "musicbrainz_enabled" -> 7
                "file_tag_scanner" -> 9
                "search_for_update", "experimental_updates" -> 11
                "lastfm_hub", "listenbrainz_hub" -> 13
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

    var showOnlineProviderDialog by remember { mutableStateOf(false) }
    var showImageSizeDialog by remember { mutableStateOf(false) }
    var showGeniusKeyDialog by remember { mutableStateOf(false) }
    var showLyricallyKeyDialog by remember { mutableStateOf(false) }

    if (showGeniusKeyDialog) {
        TextEditDialog(
            title = "Genius API Key",
            initialValue = uiState.geniusApiKey,
            onConfirm = { viewModel.setGeniusApiKey(it) },
            onDismissRequest = { showGeniusKeyDialog = false }
        )
    }

    if (showLyricallyKeyDialog) {
        TextEditDialog(
            title = "Lyrically API Key",
            initialValue = uiState.lyricallyApiKey,
            onConfirm = { viewModel.setLyricallyApiKey(it) },
            onDismissRequest = { showLyricallyKeyDialog = false }
        )
    }

    if (showOnlineProviderDialog) {
        val entries = context.resources.getStringArray(R.array.pref_online_provider_entries)
        val values = context.resources.getStringArray(R.array.pref_online_provider_values)
        SingleChoiceDialog(
            title = stringResource(R.string.online_music_provider_title),
            options = values.toList(),
            selectedOption = uiState.onlineMusicProvider,
            onOptionSelected = { viewModel.setOnlineMusicProvider(it) },
            onDismissRequest = { showOnlineProviderDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    if (showImageSizeDialog) {
        val entries = context.resources.getStringArray(R.array.pref_image_size_entries)
        val values = context.resources.getStringArray(R.array.pref_image_size_values)
        SingleChoiceDialog(
            title = stringResource(R.string.preferred_image_size_title),
            options = values.toList(),
            selectedOption = uiState.preferredImageSize,
            onOptionSelected = { viewModel.setPreferredImageSize(it) },
            onDismissRequest = { showImageSizeDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.network_title)) },
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
            // --- DATA USAGE ---
            item { DashboardCategoryHeader("Data Usage") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "Online Features",
                        summary = "Allow lyrics and artwork fetching",
                        checked = uiState.networkFeatures,
                        onCheckedChange = { viewModel.setNetworkFeatures(it) },
                        icon = R.drawable.ic_cloud_24dp
                    )
                    ExpressiveSwitchItem(
                        title = "WiFi Only",
                        summary = "Restrict online features to WiFi",
                        checked = uiState.wifiOnlyNetwork,
                        onCheckedChange = { viewModel.setWifiOnlyNetwork(it) },
                        enabled = uiState.networkFeatures
                    )
                    ExpressiveSwitchItem(
                        title = "Update only on WiFi",
                        checked = uiState.updateOnlyWifi,
                        onCheckedChange = { viewModel.setUpdateOnlyWifi(it) },
                        enabled = uiState.networkFeatures
                    )
                }
            }

            // --- IMAGES ---
            item { DashboardCategoryHeader("Online Images") }
            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.online_music_provider_title),
                        summary = uiState.onlineMusicProvider.replaceFirstChar { it.uppercase() },
                        enabled = uiState.networkFeatures,
                        onClick = { showOnlineProviderDialog = true }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.online_artist_images_title),
                        checked = uiState.allowOnlineArtistImages,
                        onCheckedChange = { viewModel.setAllowOnlineArtistImages(it) },
                        enabled = uiState.networkFeatures,
                        icon = R.drawable.ic_artist_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.online_album_covers_title),
                        checked = uiState.allowOnlineAlbumCovers,
                        onCheckedChange = { viewModel.setAllowOnlineAlbumCovers(it) },
                        enabled = uiState.networkFeatures,
                        icon = R.drawable.ic_album_24dp
                    )
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.preferred_image_size_title),
                        summary = uiState.preferredImageSize.replaceFirstChar { it.uppercase() },
                        enabled = uiState.networkFeatures,
                        onClick = { showImageSizeDialog = true }
                    )
                }
            }

            // --- LYRICS ---
            item { DashboardCategoryHeader("Lyrics Search") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "LRCLIB Provider",
                        checked = uiState.lrclibEnabled,
                        onCheckedChange = { viewModel.setLrclibEnabled(it) },
                        enabled = uiState.networkFeatures
                    )
                    ExpressiveSwitchItem(
                        title = "NetEase Provider",
                        checked = uiState.neteaseEnabled,
                        onCheckedChange = { viewModel.setNetEaseEnabled(it) },
                        enabled = uiState.networkFeatures
                    )
                    ExpressiveSwitchItem(
                        title = "Kugou Provider",
                        checked = uiState.kugouEnabled,
                        onCheckedChange = { viewModel.setKugouEnabled(it) },
                        enabled = uiState.networkFeatures
                    )
                    ExpressiveSwitchItem(
                        title = "BetterLyrics Provider",
                        checked = uiState.betterLyricsEnabled,
                        onCheckedChange = { viewModel.setBetterLyricsEnabled(it) },
                        enabled = uiState.networkFeatures
                    )
                    ExpressiveSwitchItem(
                        title = "Lyrically Provider",
                        checked = uiState.lyricallyEnabled,
                        onCheckedChange = { viewModel.setLyricallyEnabled(it) },
                        enabled = uiState.networkFeatures
                    )
                    ExpressiveSwitchItem(
                        title = "Genius Provider",
                        checked = uiState.geniusEnabled,
                        onCheckedChange = { viewModel.setGeniusEnabled(it) },
                        enabled = uiState.networkFeatures
                    )
                    ExpressiveSwitchItem(
                        title = "LyricsPlus Provider",
                        checked = uiState.lyricsPlusEnabled,
                        onCheckedChange = { viewModel.setLyricsPlusEnabled(it) },
                        enabled = uiState.networkFeatures
                    )

                    if (uiState.geniusEnabled) {
                        ExpressivePreferenceItem(
                            title = "Genius API Key",
                            summary = uiState.geniusApiKey.ifEmpty { "Set API key" },
                            onClick = { showGeniusKeyDialog = true }
                        )
                    }

                    if (uiState.lyricallyEnabled) {
                        ExpressivePreferenceItem(
                            title = "Lyrically API Key",
                            summary = uiState.lyricallyApiKey.ifEmpty { "Set API key" },
                            onClick = { showLyricallyKeyDialog = true }
                        )
                    }

                    ScanButtonWithProgress(
                        title = if (uiState.lyricsScanning) "Looking Up Lyrics..." else "Look Up Song Lyrics",
                        summary = uiState.lyricsScanResult ?: "Search all enabled providers for synced lyrics and write to database + file tags",
                        icon = R.drawable.ic_lyrics_24dp,
                        enabled = !uiState.lyricsScanning,
                        isScanning = uiState.lyricsScanning,
                        progress = uiState.lyricsScanProgress,
                        total = uiState.lyricsScanTotal,
                        label = uiState.lyricsScanLabel,
                        result = uiState.lyricsScanResult,
                        onScanClick = { viewModel.runLyricsScan() },
                        onDismissClick = { viewModel.clearLyricsScanResult() }
                    )

                    ExpressivePreferenceItem(
                        title = "Clear Downloaded Lyrics",
                        summary = "Delete cached .lrc and .ttml files",
                        icon = R.drawable.ic_delete_24dp,
                        onClick = onClearLyricsClick
                    )
                }
            }

            // --- MUSICBRAINZ ---
            item { DashboardCategoryHeader("MusicBrainz Metadata") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "MusicBrainz Lookup",
                        summary = "Auto-fetch metadata from MusicBrainz",
                        checked = uiState.musicbrainzEnabled,
                        onCheckedChange = { viewModel.setMusicbrainzEnabled(it) },
                        icon = R.drawable.ic_cloud_24dp
                    )
                    if (uiState.musicbrainzEnabled) {
                        ScanButtonWithProgress(
                            title = if (uiState.musicbrainzScanning) "Looking Up Song Tags..." else "Look Up Song Tags",
                            summary = uiState.musicbrainzScanResult ?: "Search MusicBrainz by artist->album->recording and write metadata + file tags",
                            icon = R.drawable.ic_update_24dp,
                            enabled = !uiState.musicbrainzScanning && !uiState.artistScanning,
                            isScanning = uiState.musicbrainzScanning,
                            progress = uiState.musicbrainzScanProgress,
                            total = uiState.musicbrainzScanTotal,
                            label = uiState.musicbrainzScanLabel,
                            result = uiState.musicbrainzScanResult,
                            onScanClick = { viewModel.runMusicbrainzScan() },
                            onDismissClick = { viewModel.clearMusicbrainzResult() }
                        )
                        ScanButtonWithProgress(
                            title = if (uiState.artistScanning) "Looking Up Artists..." else "Look Up Artists",
                            summary = uiState.artistScanResult ?: "Search MusicBrainz for artist details (type, gender, country) and write MBID to file tags",
                            icon = R.drawable.ic_artist_24dp,
                            enabled = !uiState.musicbrainzScanning && !uiState.artistScanning,
                            isScanning = uiState.artistScanning,
                            progress = uiState.artistScanProgress,
                            total = uiState.artistScanTotal,
                            label = uiState.artistScanLabel,
                            result = uiState.artistScanResult,
                            onScanClick = { viewModel.runArtistScan() },
                            onDismissClick = { viewModel.clearArtistResult() }
                        )
                    }
                }
            }

            // --- FILE TAGS ---
            item { DashboardCategoryHeader("File Tag Scanner") }
            item {
                SegmentedPreferenceGroup {
                    ScanButtonWithProgress(
                        title = if (uiState.fileTagScanning) "Scanning File Tags..." else "Scan File Tags",
                        summary = uiState.fileTagScanResult ?: "Read lyricist, arranger, producer, bpm, key, genre from audio file tags into database",
                        icon = R.drawable.ic_search_24dp,
                        enabled = !uiState.fileTagScanning,
                        isScanning = uiState.fileTagScanning,
                        progress = uiState.fileTagScanProgress,
                        total = uiState.fileTagScanTotal,
                        label = uiState.fileTagScanLabel,
                        result = uiState.fileTagScanResult,
                        onScanClick = { viewModel.runFileTagScan() },
                        onDismissClick = { viewModel.clearFileTagResult() }
                    )
                }
            }

            // --- UPDATES ---
            if (uiState.isUpdaterEnabled) {
                item { DashboardCategoryHeader("Application Updates") }
                item {
                    SegmentedPreferenceGroup {
                        ExpressivePreferenceItem(
                            title = "Search for Updates",
                            icon = R.drawable.ic_update_24dp,
                            onClick = onCheckForUpdates,
                            modifier = Modifier.background(
                                if (highlightedKey == "search_for_update") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                        )
                        ExpressiveSwitchItem(
                            title = "Experimental Updates",
                            checked = uiState.experimentalUpdates,
                            onCheckedChange = { viewModel.setExperimentalUpdates(it) }
                        )
                    }
                }
            }

            // --- SCROBBLING ---
            item { DashboardCategoryHeader("Scrobbling Services") }
            item {
                var showScrobbleSlider by remember { mutableStateOf(false) }
                if (showScrobbleSlider) {
                    TextEditDialog(
                        title = "Scrobble Percentage",
                        initialValue = uiState.lastfmScrobblePercentage.toString(),
                        onConfirm = { it.toIntOrNull()?.let { viewModel.setLastfmScrobblePercentage(it) } },
                        onDismissRequest = { showScrobbleSlider = false },
                        label = "Minimum % to count as play"
                    )
                }

                ScrobblingServiceHub(
                    service = ScrobblingService.Lastfm,
                    loginState = lastFmLoginState,
                    enabled = uiState.lastfmScrobbling,
                    nowPlaying = uiState.lastfmNowPlaying,
                    onEnabledChange = { viewModel.setLastfmScrobbling(it) },
                    onNowPlayingChange = { viewModel.setLastfmNowPlaying(it) },
                    onPrimaryClick = { onScrobblingLogin(ScrobblingService.Lastfm) },
                    onPendingScrobblesClick = onPendingScrobblesClick
                )
                
                if (uiState.lastfmScrobbling) {
                    SegmentedPreferenceGroup {
                        ExpressiveSwitchItem(
                            title = "Sync Favorites",
                            checked = uiState.lastfmSyncFavorites,
                            onCheckedChange = { viewModel.setLastfmSyncFavorites(it) }
                        )
                        ExpressiveSwitchItem(
                            title = "Offline Scrobbling",
                            checked = uiState.lastfmOfflineScrobbling,
                            onCheckedChange = { viewModel.setLastfmOfflineScrobbling(it) }
                        )
                        ExpressivePreferenceItem(
                            title = "Scrobble Threshold",
                            summary = "${uiState.lastfmScrobblePercentage}%",
                            onClick = { showScrobbleSlider = true }
                        )
                        ExpressiveSwitchItem(
                            title = "Biographies",
                            summary = "Fetch artist bios from Last.fm",
                            checked = uiState.lastfmInfoEnabled,
                            onCheckedChange = { viewModel.setLastfmInfoEnabled(it) }
                        )
                    }
                }
            }
            item {
                ScrobblingServiceHub(
                    service = ScrobblingService.ListenBrainz,
                    loginState = listenBrainzLoginState,
                    enabled = uiState.listenbrainzScrobbling,
                    nowPlaying = uiState.listenbrainzNowPlaying,
                    onEnabledChange = { viewModel.setListenbrainzScrobbling(it) },
                    onNowPlayingChange = { viewModel.setListenbrainzNowPlaying(it) },
                    onPrimaryClick = { onScrobblingLogin(ScrobblingService.ListenBrainz) }
                )
            }
        }
    }
}

@Composable
private fun ScanButtonWithProgress(
    title: String,
    summary: String,
    icon: Int,
    enabled: Boolean,
    isScanning: Boolean,
    progress: Int,
    total: Int,
    label: String?,
    result: String?,
    onScanClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    ExpressivePreferenceItem(
        title = title,
        summary = if (isScanning) {
            if (total > 0) "$progress / $total" else "Starting..."
        } else result ?: summary,
        icon = icon,
        enabled = enabled,
        onClick = onScanClick,
        trailingContent = {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
    if (isScanning && total > 0 && progress <= total) {
        val animatedProgress by animateFloatAsState(
            targetValue = if (total > 0) progress.toFloat() / total.toFloat() else 0f,
            label = "scanProgress"
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            if (label != null) {
                Text(
                    text = label.take(60),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
    if (result != null && !isScanning) {
        ExpressivePreferenceItem(
            title = "Dismiss",
            summary = result,
            onClick = onDismissClick
        )
    }
}
