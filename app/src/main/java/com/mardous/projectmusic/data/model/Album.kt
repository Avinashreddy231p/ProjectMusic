/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mardous.projectmusic.data.model

import android.net.Uri
import com.mardous.projectmusic.data.SongProvider
import com.mardous.projectmusic.extensions.media.asAlbumCoverUri
import java.util.Objects

data class Album(
    val id: Long,
    val artistName: String,
    val albumArtistName: String?,
    val year: Int,
    val firstSongIndex: Int,
    override val songs: List<Song>,
    val overallRank: Int = 0,
    val playCount: Long = 0L,
    val listeningDuration: Long = 0L,
    val overallScore: Double = 0.0,
    val playRank: Int = 0,
    val durationRank: Int = 0,
    val about: String? = null
) : SongProvider {

    val name: String
        get() = safeGetFirstSong().albumName

    val artistId: Long
        get() = safeGetFirstSong().artistId

    val songCount: Int
        get() = songs.size

    val duration: Long by lazy { songs.sumOf { it.duration } }

    val dateAdded: Long by lazy { songs.minOf { it.dateAdded } }

    val isSingle: Boolean
        get() = songCount == 1

    val albumCover: Uri
        get() = id.asAlbumCoverUri()

    fun safeGetFirstSong(): Song {
        return songs.getOrNull(firstSongIndex) ?: Song.emptySong
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val album = other as Album
        return id == album.id && songs == album.songs
    }

    override fun hashCode(): Int {
        return Objects.hash(id, songs)
    }

    override fun toString(): String {
        return "Album{id=$id, songs=$songs}"
    }

    companion object {
        const val UNKNOWN_ALBUM_DISPLAY_NAME = "Unknown Album"

        val empty = Album(-1, "", "", -1, -1, arrayListOf())
    }
}