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

package com.mardous.projectmusic.data.local.database.intel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey
    val id: Long,
    val lyrics: String? = null,
    val provider: String? = null,
    @ColumnInfo(name = "is_instrumental")
    val instrumental: Boolean = false,
    val checksum: String? = null,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long? = null,
    @ColumnInfo(name = "provider_id") val providerId: String? = null,
    @ColumnInfo(name = "embedded_lyrics") val embeddedLyrics: String? = null,
    @ColumnInfo(name = "synchronized_lyrics") val synchronizedLyrics: String? = null,
    @ColumnInfo(name = "unsynchronized_lyrics") val unsynchronizedLyrics: String? = null,
    val language: String? = null,
    val source: Int = 0,
    @ColumnInfo(name = "word_count") val wordCount: Int = 0,
    @ColumnInfo(name = "has_embedded_lyrics") val hasEmbeddedLyrics: Boolean = false,
    @ColumnInfo(name = "has_synchronized_lyrics") val hasSynchronizedLyrics: Boolean = false
)
