package com.mardous.booming.ui.screen.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.ui.component.compose.preferences.*
import com.mardous.booming.data.model.network.ScrobblingService
import com.mardous.booming.data.model.network.LoginState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.booming.ui.component.compose.InputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSettingsComposeScreen(
    viewModel: SettingsViewModel,
    libraryViewModel: com.mardous.booming.ui.screen.library.LibraryViewModel,
    onBackClick: () -> Unit,
    onLastFmProfileClick: () -> Unit,
    onPendingScrobblesClick: () -> Unit,
    onLoginClick: (ScrobblingService) -> Unit,
    onClearLyricsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastfmLoginState by libraryViewModel.getLoginState(ScrobblingService.Lastfm).collectAsStateWithLifecycle(LoginState.Empty)
    val lbLoginState by libraryViewModel.getLoginState(ScrobblingService.ListenBrainz).collectAsStateWithLifecycle(LoginState.Empty)

    var showGeniusKeyDialog by remember { mutableStateOf(false) }
    var showLyricallyKeyDialog by remember { mutableStateOf(false) }

    if (showGeniusKeyDialog) {
        InputDialog(
            onConfirm = { 
                viewModel.setGeniusApiKey(it)
                showGeniusKeyDialog = false 
            },
            onDismiss = { showGeniusKeyDialog = false },
            message = "Genius requires a Client Access Token for searching. You can generate one in your Genius API dashboard.",
            confirmButton = stringResource(android.R.string.ok),
            title = stringResource(R.string.lyrics_client_access_token_title),
            inputHint = stringResource(R.string.lyrics_api_key_hint),
            inputPrefill = uiState.geniusApiKey
        )
    }

    if (showLyricallyKeyDialog) {
        InputDialog(
            onConfirm = { 
                viewModel.setLyricallyApiKey(it)
                showLyricallyKeyDialog = false 
            },
            onDismiss = { showLyricallyKeyDialog = false },
            message = "Enter your Lyrically (Paxsenix) API key. This is optional but recommended for better rate limits.",
            confirmButton = stringResource(android.R.string.ok),
            title = stringResource(R.string.lyrics_api_key_title),
            inputHint = stringResource(R.string.lyrics_api_key_hint),
            inputPrefill = uiState.lyricallyApiKey
        )
    }

    CollapsibleAppBarScaffold(
        title = stringResource(R.string.network_title),
        onBackClick = onBackClick,
        forceSmallAppBar = true
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- CONNECTIVITY ---
            item { PreferenceCategoryHeader(title = "Connectivity") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.network_features_title),
                        summary = "Global toggle for online features",
                        icon = R.drawable.ic_language_24dp,
                        trailingContent = {
                            Switch(checked = uiState.networkFeatures, onCheckedChange = { viewModel.setNetworkFeatures(it) })
                        }
                    )
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.wifi_only_network_features_title),
                        summary = stringResource(R.string.wifi_only_network_features_summary),
                        icon = R.drawable.ic_network_wifi_24dp,
                        trailingContent = {
                            Switch(checked = uiState.wifiOnlyNetwork, onCheckedChange = { viewModel.setWifiOnlyNetwork(it) })
                        },
                        showDivider = false
                    )
                }
            }

            // --- SERVICES ---
            if (uiState.networkFeatures) {
                item { PreferenceCategoryHeader(title = "Cloud Integrations") }
                item {
                    ScrobblingServiceHub(
                        service = ScrobblingService.Lastfm,
                        loginState = lastfmLoginState,
                        enabled = uiState.lastfmScrobbling,
                        nowPlaying = uiState.lastfmNowPlaying,
                        onEnabledChange = { viewModel.setLastfmScrobbling(it) },
                        onNowPlayingChange = { viewModel.setLastfmNowPlaying(it) },
                        onPrimaryClick = { 
                            if (lastfmLoginState is LoginState.LoggedIn) onLastFmProfileClick() 
                            else onLoginClick(ScrobblingService.Lastfm)
                        },
                        onPendingScrobblesClick = onPendingScrobblesClick
                    )
                }
                item {
                    ScrobblingServiceHub(
                        service = ScrobblingService.ListenBrainz,
                        loginState = lbLoginState,
                        enabled = uiState.listenbrainzScrobbling,
                        nowPlaying = uiState.listenbrainzNowPlaying,
                        onEnabledChange = { viewModel.setListenbrainzScrobbling(it) },
                        onNowPlayingChange = { viewModel.setListenbrainzNowPlaying(it) },
                        onPrimaryClick = { onLoginClick(ScrobblingService.ListenBrainz) }
                    )
                }

                // --- LYRICS ---
                item { PreferenceCategoryHeader(title = "Lyrics Providers") }
                item {
                    SegmentedPreferenceGroup {
                        SegmentedPreferenceItem(
                            title = "LRCLib",
                            summary = stringResource(R.string.use_lrclib_provider_summary),
                            trailingContent = {
                                Switch(checked = uiState.lrclibEnabled, onCheckedChange = { viewModel.setLrclibEnabled(it) })
                            }
                        )
                        SegmentedPreferenceItem(
                            title = "BetterLyrics",
                            summary = stringResource(R.string.use_betterlyrics_provider_summary),
                            trailingContent = {
                                Switch(checked = uiState.betterLyricsEnabled, onCheckedChange = { viewModel.setBetterLyricsEnabled(it) })
                            }
                        )
                        
                        var lyricallyExpanded by remember { mutableStateOf(false) }
                        SplitButtonPreference(
                            title = "Lyrically",
                            summary = stringResource(R.string.use_lyrically_provider_summary),
                            checked = uiState.lyricallyEnabled,
                            onCheckedChange = { viewModel.setLyricallyEnabled(it) },
                            expanded = lyricallyExpanded,
                            onExpandClick = { lyricallyExpanded = !lyricallyExpanded }
                        ) {
                            SegmentedPreferenceItem(
                                title = stringResource(R.string.lyrics_api_key_title),
                                summary = if (uiState.lyricallyApiKey.isNotEmpty()) 
                                    stringResource(R.string.lyrics_api_key_summary, uiState.lyricallyApiKey.take(8) + "...") 
                                    else stringResource(R.string.lyrics_api_key_not_set),
                                onClick = { showLyricallyKeyDialog = true },
                                showDivider = false
                            )
                        }

                        var geniusExpanded by remember { mutableStateOf(false) }
                        SplitButtonPreference(
                            title = "Genius",
                            summary = stringResource(R.string.use_genius_provider_summary),
                            checked = uiState.geniusEnabled,
                            onCheckedChange = { viewModel.setGeniusEnabled(it) },
                            expanded = geniusExpanded,
                            onExpandClick = { geniusExpanded = !geniusExpanded }
                        ) {
                            SegmentedPreferenceItem(
                                title = stringResource(R.string.lyrics_client_access_token_title),
                                summary = if (uiState.geniusApiKey.isNotEmpty()) 
                                    stringResource(R.string.lyrics_api_key_summary, uiState.geniusApiKey.take(8) + "...") 
                                    else stringResource(R.string.lyrics_api_key_not_set),
                                onClick = { showGeniusKeyDialog = true },
                                showDivider = false
                            )
                        }

                        SegmentedPreferenceItem(
                            title = "LyricsPlus",
                            summary = stringResource(R.string.use_lyricsplus_provider_summary),
                            trailingContent = {
                                Switch(checked = uiState.lyricsPlusEnabled, onCheckedChange = { viewModel.setLyricsPlusEnabled(it) })
                            }
                        )
                        SegmentedPreferenceItem(
                            title = stringResource(R.string.delete_downloaded_lyrics_title),
                            summary = stringResource(R.string.delete_downloaded_lyrics_summary),
                            icon = R.drawable.ic_delete_24dp,
                            onClick = onClearLyricsClick,
                            showDivider = false
                        )
                    }
                }

                // --- MEDIA PROVIDERS ---
                item { PreferenceCategoryHeader(title = "Sources & Metadata") }
                item {
                    SegmentedPreferenceGroup {
                        val sizes = listOf("small", "medium", "large")
                        val sizeNames = listOf("Small", "Medium", "Large")
                        SegmentedPreferenceItem(
                            title = stringResource(R.string.preferred_image_size_title),
                            options = sizeNames,
                            selectedIndex = sizes.indexOf(uiState.preferredImageSize).coerceAtLeast(0),
                            onOptionSelected = { viewModel.setPreferredImageSize(sizes[it]) }
                        )
                        
                        SegmentedPreferenceItem(
                            title = stringResource(R.string.online_artist_images_title),
                            summary = "Fetch artist artwork online",
                            icon = R.drawable.ic_artist_24dp,
                            trailingContent = {
                                Switch(checked = uiState.allowOnlineArtistImages, onCheckedChange = { viewModel.setAllowOnlineArtistImages(it) })
                            }
                        )
                        SegmentedPreferenceItem(
                            title = stringResource(R.string.online_album_covers_title),
                            summary = stringResource(R.string.online_album_covers_summary),
                            icon = R.drawable.ic_album_24dp,
                            trailingContent = {
                                Switch(checked = uiState.allowOnlineAlbumCovers, onCheckedChange = { viewModel.setAllowOnlineAlbumCovers(it) })
                            },
                            showDivider = false
                        )
                    }
                }
            }
        }
    }
}
