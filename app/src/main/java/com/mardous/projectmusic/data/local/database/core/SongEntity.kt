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

package com.mardous.projectmusic.data.local.database.core

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "songs",
    indices = [Index(value = ["media_store_id"], unique = true)]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "song_key")
    val songKey: Long = 0L,
    @ColumnInfo(name = "media_store_id")
    val mediaStoreId: Long,
    val data: String,
    val title: String,
    @ColumnInfo(name = "duration")
    val duration: Long,
    @ColumnInfo(name = "album_id")
    val albumId: Long,
    @ColumnInfo(name = "album_name")
    val albumName: String,
    @ColumnInfo(name = "artist_id")
    val artistId: Long,
    @ColumnInfo(name = "artist_name")
    val artistName: String,
    @ColumnInfo(name = "album_artist")
    val albumArtist: String?,
    @ColumnInfo(name = "genre_name")
    val genreName: String?,
    @ColumnInfo(name = "volume_name")
    val volumeName: String? = null,
    val bitrate: Int = 0
) : Parcelable
