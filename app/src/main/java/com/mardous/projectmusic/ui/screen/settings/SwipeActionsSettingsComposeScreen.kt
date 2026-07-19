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
import com.mardous.projectmusic.core.model.swipe.SwipeAction
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.ui.component.preferences.dialog.SwipeActionPreferenceDialog
import com.mardous.projectmusic.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwipeActionsSettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.swipe_actions_title)) },
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
            // --- SONGS ---
            item { DashboardCategoryHeader(stringResource(R.string.pref_swipe_actions_songs)) }
            item {
                SwipeActionGroup(
                    leftKey = "swipe_action_left_songs",
                    rightKey = "swipe_action_right_songs",
                    viewModel = viewModel
                )
            }

            // --- ALBUMS ---
            item { DashboardCategoryHeader(stringResource(R.string.pref_swipe_actions_albums)) }
            item {
                SwipeActionGroup(
                    leftKey = "swipe_action_left_albums",
                    rightKey = "swipe_action_right_albums",
                    viewModel = viewModel
                )
            }

            // --- ARTISTS ---
            item { DashboardCategoryHeader(stringResource(R.string.pref_swipe_actions_artists)) }
            item {
                SwipeActionGroup(
                    leftKey = "swipe_action_left_artists",
                    rightKey = "swipe_action_right_artists",
                    viewModel = viewModel
                )
            }

            // --- PLAYLISTS ---
            item { DashboardCategoryHeader(stringResource(R.string.pref_swipe_actions_playlists)) }
            item {
                SwipeActionGroup(
                    leftKey = "swipe_action_left_playlists",
                    rightKey = "swipe_action_right_playlists",
                    viewModel = viewModel
                )
            }

            // --- QUEUE ---
            item { DashboardCategoryHeader(stringResource(R.string.pref_swipe_actions_queue)) }
            item {
                SwipeActionGroup(
                    leftKey = "swipe_action_left_queue",
                    rightKey = "swipe_action_right_queue",
                    viewModel = viewModel
                )
            }

            // --- STATS ---
            item { DashboardCategoryHeader(stringResource(R.string.pref_swipe_actions_stats)) }
            item {
                SwipeActionGroup(
                    leftKey = "swipe_action_left_stats",
                    rightKey = "swipe_action_right_stats",
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun SwipeActionGroup(
    leftKey: String,
    rightKey: String,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    
    // We need to read directly from SharedPreferences since these dynamic keys aren't in uiState
    val prefs = remember { (context as FragmentActivity).getSharedPreferences(context.packageName + "_preferences", 0) }
    
    // Trigger recomposition on preference change
    var leftAction by remember { mutableStateOf(SwipeAction.fromString(prefs.getString(leftKey, SwipeAction.NONE.name))) }
    var rightAction by remember { mutableStateOf(SwipeAction.fromString(prefs.getString(rightKey, SwipeAction.NONE.name))) }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == leftKey) leftAction = SwipeAction.fromString(p.getString(leftKey, SwipeAction.NONE.name))
            if (key == rightKey) rightAction = SwipeAction.fromString(p.getString(rightKey, SwipeAction.NONE.name))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    SegmentedPreferenceGroup {
        SegmentedPreferenceItem(
            title = stringResource(R.string.swipe_left_title),
            summary = stringResource(leftAction.titleRes),
            icon = getSwipeActionIcon(leftAction),
            onClick = {
                SwipeActionPreferenceDialog.newInstance(leftKey).show((context as FragmentActivity).supportFragmentManager, "SWIPE_DIALOG")
            }
        )
        SegmentedPreferenceItem(
            title = stringResource(R.string.swipe_right_title),
            summary = stringResource(rightAction.titleRes),
            icon = getSwipeActionIcon(rightAction),
            onClick = {
                SwipeActionPreferenceDialog.newInstance(rightKey).show((context as FragmentActivity).supportFragmentManager, "SWIPE_DIALOG")
            },
            showDivider = false
        )
    }
}

fun getSwipeActionIcon(action: SwipeAction): Int {
    return when (action) {
        SwipeAction.PLAY_NEXT -> R.drawable.ic_queue_play_next_24dp
        SwipeAction.ADD_TO_QUEUE -> R.drawable.ic_queue_music_24dp
        SwipeAction.ADD_TO_PLAYLIST -> R.drawable.ic_playlist_add_24dp
        SwipeAction.TOGGLE_FAVORITE -> R.drawable.ic_favorite_24dp
        SwipeAction.HIDE_SONG -> R.drawable.ic_visibility_off_24dp
        SwipeAction.REMOVE_FROM_PLAYLIST -> R.drawable.ic_delete_24dp
        SwipeAction.REMOVE_FROM_QUEUE -> R.drawable.ic_delete_24dp
        SwipeAction.DELETE_FILE -> R.drawable.ic_delete_24dp
        SwipeAction.SHARE -> R.drawable.ic_share_24dp
        SwipeAction.COPY_FILE_PATH -> R.drawable.ic_content_paste_24dp
        SwipeAction.SONG_INFO -> R.drawable.ic_info_24dp
        SwipeAction.EDIT_TAGS -> R.drawable.ic_edit_24dp
        SwipeAction.OPEN_ALBUM -> R.drawable.ic_album_24dp
        SwipeAction.OPEN_ARTIST -> R.drawable.ic_artist_24dp
        SwipeAction.GO_TO_FOLDER -> R.drawable.ic_folder_24dp
        SwipeAction.NONE -> R.drawable.ic_close_24dp
    }
}
