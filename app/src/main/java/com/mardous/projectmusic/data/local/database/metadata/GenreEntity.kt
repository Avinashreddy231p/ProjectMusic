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
    tableName = "genres",
    indices = [Index(value = ["name"], unique = true)]
)
data class GenreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: Int? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "song_count") val songCount: Int = 0,
    @ColumnInfo(name = "total_duration") val totalDuration: Long = 0,
    @ColumnInfo(name = "play_count") val playCount: Long = 0,
    @ColumnInfo(name = "listening_duration") val listeningDuration: Long = 0,
    @ColumnInfo(name = "overall_score") val overallScore: Double = 0.0,
    @ColumnInfo(name = "play_rank") val playRank: Int = 0,
    @ColumnInfo(name = "duration_rank") val durationRank: Int = 0,
    @ColumnInfo(name = "overall_rank") val overallRank: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable
