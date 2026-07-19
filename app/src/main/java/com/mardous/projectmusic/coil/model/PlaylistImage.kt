package com.mardous.projectmusic.coil.model

import com.mardous.projectmusic.data.local.database.metadata.PlaylistEntity
import com.mardous.projectmusic.data.model.Song

class PlaylistImage(val playlistEntity: PlaylistEntity, val songs: List<Song>) {
    override fun toString(): String {
        return buildString {
            append("PlaylistImage{")
            append("playlistEntity=$playlistEntity,")
            append("songs=$songs")
            append("}")
        }
    }
}