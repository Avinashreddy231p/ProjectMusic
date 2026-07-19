package com.mardous.projectmusic.data

import com.mardous.projectmusic.data.model.Song

interface SongProvider {
    val songs: List<Song>
}