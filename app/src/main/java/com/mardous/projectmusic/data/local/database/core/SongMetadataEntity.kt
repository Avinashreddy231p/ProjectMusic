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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "song_metadata",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["song_key"],
            childColumns = ["song_key"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class SongMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_key")
    val songKey: Long,
    val composer: String? = null,
    val lyricist: String? = null,
    val conductor: String? = null,
    val publisher: String? = null,
    val copyright: String? = null,
    val genre: String? = null,
    val year: Int = 0,
    @ColumnInfo(name = "track_number")
    val trackNumber: Int = 0,
    @ColumnInfo(name = "disc_number")
    val discNumber: Int = 0,
    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = 0,
    @ColumnInfo(name = "date_modified")
    val dateModified: Long = 0,
    @ColumnInfo(name = "audio_format")
    val audioFormat: String? = null,
    @ColumnInfo(name = "sample_rate")
    val sampleRate: Int = 0,
    @ColumnInfo(name = "channel_count")
    val channelCount: Int = 0,
    @ColumnInfo(name = "bit_depth")
    val bitDepth: Int = 0,
    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,
    @ColumnInfo(name = "replay_gain")
    val replayGain: Double? = null,
    val bpm: Int = 0,
    @ColumnInfo(name = "key_signature")
    val keySignature: String? = null,
    val isrc: String? = null,
    @ColumnInfo(name = "musicbrainz_track_id")
    val musicbrainzTrackId: String? = null,
    @ColumnInfo(name = "musicbrainz_album_id")
    val musicbrainzAlbumId: String? = null,
    @ColumnInfo(name = "musicbrainz_artist_id")
    val musicbrainzArtistId: String? = null,
    @ColumnInfo(name = "musicbrainz_work_id")
    val musicbrainzWorkId: String? = null,
    val iswc: String? = null,
    val label: String? = null,
    @ColumnInfo(name = "catalog_number")
    val catalogNumber: String? = null,
    val arranger: String? = null,
    val producer: String? = null,
    val engineer: String? = null,
    @ColumnInfo(name = "is_explicit")
    val isExplicit: Boolean = false,
    @ColumnInfo(name = "is_compilation")
    val isCompilation: Boolean = false,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    val rating: Int = 0,
    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String? = null,
    val checksum: String? = null,
    val language: String? = null,
    @ColumnInfo(name = "last_scanned_timestamp")
    val lastScannedTimestamp: Long = 0,
    @ColumnInfo(name = "track_total")
    val trackTotal: Int = 0,
    @ColumnInfo(name = "disc_total")
    val discTotal: Int = 0,
    val comment: String? = null
)
