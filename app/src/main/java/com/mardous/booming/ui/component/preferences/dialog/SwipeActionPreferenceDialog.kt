package com.mardous.booming.ui.component.preferences.dialog

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.mardous.booming.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.core.model.swipe.SwipeAction
import com.mardous.booming.ui.component.compose.DialogListItemWithRadio
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.android.ext.android.get

class SwipeActionPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val prefKey = requireArguments().getString(EXTRA_KEY)
        checkNotNull(prefKey)

        val allActions = SwipeAction.entries.toList().filter { action ->
            when (action) {
                SwipeAction.REMOVE_FROM_QUEUE -> prefKey.contains("queue", ignoreCase = true)
                SwipeAction.REMOVE_FROM_PLAYLIST -> prefKey.contains("playlist", ignoreCase = true)
                else -> true
            }
        }
        
        val sharedPrefs = get<SharedPreferences>()
        val currentValueStr = sharedPrefs.getString(prefKey, SwipeAction.NONE.name)
        val currentAction = SwipeAction.fromString(currentValueStr)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        BoomingMusicTheme {
                            DialogScreen(
                                actions = allActions,
                                selected = currentAction,
                                onActionClick = { action ->
                                    sharedPrefs.edit {
                                        putString(prefKey, action.name)
                                    }
                                    dialog?.dismiss()
                                }
                            )
                        }
                    }
                }
            )
            .create()
    }

    @Composable
    private fun DialogScreen(
        actions: List<SwipeAction>,
        selected: SwipeAction,
        onActionClick: (SwipeAction) -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.wrapContentHeight()
        ) {
            val firstVisibleIndex = actions.indexOfFirst { it.ordinal == selected.ordinal }
                .coerceAtLeast(0)

            LazyColumn(
                state = rememberLazyListState(firstVisibleIndex),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 16.dp)
            ) {
                items(actions) { action ->
                    val isSelected = action == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActionClick(action) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                        val iconRes = getActionIcon(action)
                        if (iconRes != 0) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.width(24.dp))
                        }
                        Text(
                            text = stringResource(action.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    private fun getActionIcon(action: SwipeAction): Int {
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

    companion object {
        private const val EXTRA_KEY = "extra_key"

        fun newInstance(preference: String): SwipeActionPreferenceDialog {
            return SwipeActionPreferenceDialog().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_KEY, preference)
                }
            }
        }
    }
}