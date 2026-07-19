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
import com.mardous.projectmusic.data.model.network.ScrobblingService
import com.mardous.projectmusic.data.model.network.LoginState
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkSettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onScrobblingLogin: (ScrobblingService) -> Unit,
    onPendingScrobblesClick: () -> Unit,
    onClearLyricsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lastFmLoginState by viewModel.lastFmLoginState.collectAsState()
    val listenBrainzLoginState by viewModel.listenBrainzLoginState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

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
        }
    ) { padding ->
        LazyColumn(
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
                        ExpressivePreferenceItem(
                            title = if (uiState.musicbrainzScanning) "Scanning..." else "Look Up Song Tags",
                            summary = uiState.musicbrainzScanResult ?: "Search MusicBrainz by artist->album->recording and write metadata + file tags",
                            icon = R.drawable.ic_update_24dp,
                            enabled = !uiState.musicbrainzScanning && !uiState.artistScanning,
                            onClick = { viewModel.runMusicbrainzScan() }
                        )
                        if (uiState.musicbrainzScanResult != null && !uiState.musicbrainzScanning) {
                            ExpressivePreferenceItem(
                                title = "Dismiss Song Result",
                                summary = uiState.musicbrainzScanResult,
                                onClick = { viewModel.clearMusicbrainzResult() }
                            )
                        }
                    }
                    if (uiState.musicbrainzEnabled) {
                        ExpressivePreferenceItem(
                            title = if (uiState.artistScanning) "Looking Up Artists..." else "Look Up Artists",
                            summary = uiState.artistScanResult ?: "Search MusicBrainz for all artist details (type, gender, country) and write MBID to file tags",
                            icon = R.drawable.ic_artist_24dp,
                            enabled = !uiState.musicbrainzScanning && !uiState.artistScanning,
                            onClick = { viewModel.runArtistScan() }
                        )
                        if (uiState.artistScanResult != null && !uiState.artistScanning) {
                            ExpressivePreferenceItem(
                                title = "Dismiss Artist Result",
                                summary = uiState.artistScanResult,
                                onClick = { viewModel.clearArtistResult() }
                            )
                        }
                    }
                }
            }

            // --- FILE TAGS ---
            item { DashboardCategoryHeader("File Tag Scanner") }
            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = if (uiState.fileTagScanning) "Scanning..." else "Scan File Tags",
                        summary = uiState.fileTagScanResult ?: "Read lyricist, arranger, producer, bpm, key, genre from audio file tags into database",
                        icon = R.drawable.ic_search_24dp,
                        enabled = !uiState.fileTagScanning,
                        onClick = { viewModel.runFileTagScan() }
                    )
                    if (uiState.fileTagScanResult != null && !uiState.fileTagScanning) {
                        ExpressivePreferenceItem(
                            title = "Dismiss Result",
                            summary = uiState.fileTagScanResult,
                            onClick = { viewModel.clearFileTagResult() }
                        )
                    }
                }
            }

            // --- UPDATES ---
            item { DashboardCategoryHeader("Application Updates") }
            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = "Search for Updates",
                        icon = R.drawable.ic_update_24dp,
                        onClick = { /* ViewModel logic */ }
                    )
                    ExpressiveSwitchItem(
                        title = "Experimental Updates",
                        checked = uiState.experimentalUpdates,
                        onCheckedChange = { viewModel.setExperimentalUpdates(it) }
                    )
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
