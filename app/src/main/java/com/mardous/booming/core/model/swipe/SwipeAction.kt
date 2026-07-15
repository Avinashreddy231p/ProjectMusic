package com.mardous.booming.core.model.swipe

import androidx.annotation.StringRes
import com.mardous.booming.R

enum class SwipeAction(@StringRes val titleRes: Int) {
    NONE(R.string.action_none),
    PLAY_NEXT(R.string.action_queue_next),
    ADD_TO_QUEUE(R.string.action_add_to_playing_queue),
    ADD_TO_PLAYLIST(R.string.action_add_to_playlist),
    TOGGLE_FAVORITE(R.string.action_toggle_favorite),
    HIDE_SONG(R.string.action_hide_song),
    REMOVE_FROM_PLAYLIST(R.string.action_remove_from_playlist),
    REMOVE_FROM_QUEUE(R.string.action_remove_from_playing_queue),
    DELETE_FILE(R.string.action_delete),
    SHARE(R.string.action_share),
    COPY_FILE_PATH(R.string.action_copy_path),
    SONG_INFO(R.string.action_details),
    EDIT_TAGS(R.string.action_tag_editor),
    OPEN_ALBUM(R.string.action_go_to_album),
    OPEN_ARTIST(R.string.action_go_to_artist),
    GO_TO_FOLDER(R.string.folders_label);

    companion object {
        fun fromString(value: String?): SwipeAction {
            return entries.find { it.name == value } ?: NONE
        }
    }
}
