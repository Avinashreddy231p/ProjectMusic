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

package com.mardous.projectmusic.data.local.database.metadata

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = true)]
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "musicbrainz_id") val musicbrainzId: String? = null,
    val type: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val disambiguation: String? = null,
    @ColumnInfo(name = "begin_date") val beginDate: String? = null,
    @ColumnInfo(name = "end_date") val endDate: String? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "cover_uri") val coverUri: String? = null,
    @ColumnInfo(name = "song_count") val songCount: Int = 0,
    @ColumnInfo(name = "total_duration") val totalDuration: Long = 0,
    @ColumnInfo(name = "play_count") val playCount: Long = 0,
    @ColumnInfo(name = "listening_duration") val listeningDuration: Long = 0,
    @ColumnInfo(name = "overall_score") val overallScore: Double = 0.0,
    @ColumnInfo(name = "play_rank") val playRank: Int = 0,
    @ColumnInfo(name = "duration_rank") val durationRank: Int = 0,
    @ColumnInfo(name = "overall_rank") val overallRank: Int = 0,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable
