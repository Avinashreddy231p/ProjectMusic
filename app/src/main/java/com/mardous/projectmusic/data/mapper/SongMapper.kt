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

package com.mardous.projectmusic.data.mapper

import com.mardous.projectmusic.data.local.database.core.SongEntity
import com.mardous.projectmusic.data.local.database.metadata.PlaylistEntity
import com.mardous.projectmusic.data.local.database.analytics.HistoryEntity
import com.mardous.projectmusic.data.local.database.analytics.PlayCountEntity
import com.mardous.projectmusic.data.model.Song

fun List<SongEntity>.toSongs(): List<Song> {
    return map { it.toSong() }
}

fun List<Song>.toSongsEntity(playlistId: Long): List<SongEntity> {
    return map { it.toSongEntity() }
}

fun List<Song>.toSongsEntity(playlist: PlaylistEntity): List<SongEntity> {
    return map { it.toSongEntity() }
}

fun Song.toHistoryEntity(timePlayed: Long): HistoryEntity {
    return HistoryEntity(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        dateModified = rawDateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName,
        timePlayed = timePlayed
    )
}

fun Song.toSongEntity(): SongEntity {
    return SongEntity(
        mediaStoreId = id,
        data = data,
        title = title,
        duration = duration,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtist = albumArtistName,
        genreName = genreName,
        volumeName = volumeName,
        bitrate = bitrate
    )
}

fun SongEntity.toSong(): Song {
    return Song(
        id = mediaStoreId,
        data = data,
        title = title,
        trackNumber = 0, // MetadataEntity will provide this
        year = 0,        // MetadataEntity will provide this
        size = 0,        // MetadataEntity will provide this
        duration = duration,
        dateAdded = 0,   // MetadataEntity will provide this
        rawDateModified = 0, // MetadataEntity will provide this
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtist,
        genreName = genreName,
        volumeName = volumeName,
        bitrate = bitrate
    )
}

fun PlayCountEntity.toSong(): Song {
    return Song(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        rawDateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName
    )
}

fun HistoryEntity.toSong(): Song {
    return Song(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        rawDateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName
    )
}

fun Song.toPlayCount(timePlayed: Long = -1, playCount: Int = 0, skipCount: Int = 0): PlayCountEntity {
    return PlayCountEntity(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        dateModified = rawDateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName,
        timePlayed = timePlayed,
        playCount = playCount,
        skipCount = skipCount
    )
}
