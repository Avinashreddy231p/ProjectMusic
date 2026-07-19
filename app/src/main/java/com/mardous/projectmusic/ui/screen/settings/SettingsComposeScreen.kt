package com.mardous.projectmusic.ui.screen.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Bundle
import androidx.navigation.NavController
import com.mardous.projectmusic.R
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.ui.screen.settings.search.SettingsSearchHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsComposeScreen(
    viewModel: SettingsViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onAboutClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isSearchActive) {
                LargeTopAppBar(
                    title = { 
                        Column {
                            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                            Text("Personalize your experience", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(painter = painterResource(R.drawable.ic_back_24dp), contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
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
                            Icon(painter = painterResource(R.drawable.ic_back_24dp), contentDescription = null)
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
                                    val args = Bundle().apply { putString("highlightKey", result.key) }
                                    navController.navigate(result.destinationId, args)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- PERSONALIZATION ---
            item { DashboardCategoryHeader("Personalization") }
            item {
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard(
                        title = "Appearance",
                        icon = R.drawable.ic_palette_24dp,
                        summary = "Design & Theme",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(R.id.action_to_appearancePreferences) }
                    )
                    DashboardCard(
                        title = "Now Playing",
                        icon = R.drawable.ic_play_circle_24dp,
                        summary = "Player Visuals",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(R.id.action_to_nowPlayingPreferences) }
                    )
                }
            }

            // --- AUDIO & PLAYBACK ---
            item { DashboardCategoryHeader("Audio & Playback") }
            item {
                ExpressivePreferenceItem(
                    title = "Playback Engine",
                    summary = "Equalizer, sound effects, and behavior",
                    icon = painterResource(R.drawable.ic_equalizer_24dp),
                    onClick = { navController.navigate(R.id.action_to_playbackPreferences) }
                )
            }

            // --- CONTENT & LYRICS ---
            item { DashboardCategoryHeader("Content & Experience") }
            item {
                ExpressivePreferenceItem(
                    title = "Lyrics Experience",
                    summary = "Syllable lyrics, visual effects, and typography",
                    icon = painterResource(R.drawable.ic_lyrics_outline_24dp),
                    onClick = { navController.navigate(R.id.action_to_lyricsPreferences) }
                )
            }
            item {
                ExpressivePreferenceItem(
                    title = "Library & Files",
                    summary = "Smart playlists, folders, and management",
                    icon = painterResource(R.drawable.ic_library_music_24dp),
                    onClick = { navController.navigate(R.id.action_to_libraryPreferences) }
                )
            }

            // --- CONNECTIVITY ---
            item { DashboardCategoryHeader("Connectivity & Services") }
            item {
                ExpressivePreferenceItem(
                    title = "Network & Cloud",
                    summary = "Last.fm, ListenBrainz, and updates",
                    icon = painterResource(R.drawable.ic_cloud_24dp),
                    onClick = { navController.navigate(R.id.action_to_networkPreferences) }
                )
            }

            // --- SYSTEM ---
            item { DashboardCategoryHeader("System") }
            item {
                ExpressivePreferenceItem(
                    title = "Advanced Settings",
                    summary = "Backup, language, and internals",
                    icon = painterResource(R.drawable.ic_settings_applications_24dp),
                    onClick = { navController.navigate(R.id.action_to_advancedPreferences) }
                )
            }
            
            item {
                ExpressivePreferenceItem(
                    title = "Listening Stats",
                    summary = "Your top tracks, artists, and habits",
                    icon = painterResource(R.drawable.ic_trending_up_24dp),
                    onClick = onStatsClick
                )
            }

            item {
                ExpressivePreferenceItem(
                    title = "About Project Music",
                    summary = "Version 1.3.1-beta.2 • Premium Edition",
                    icon = painterResource(R.drawable.ic_info_24dp),
                    onClick = onAboutClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardCard(
    title: String,
    icon: Int,
    summary: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = modifier.height(140.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
