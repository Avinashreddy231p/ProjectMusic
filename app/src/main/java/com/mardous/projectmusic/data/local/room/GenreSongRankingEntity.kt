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

package com.mardous.projectmusic.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "genre_song_ranking",
    primaryKeys = ["song_id", "genre_id"],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["song_key"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GenreEntity::class,
            parentColumns = ["id"],
            childColumns = ["genre_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("song_id"),
        Index("genre_id"),
        Index("combined_rank")
    ]
)
data class GenreSongRankingEntity(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "genre_id") val genreId: Long,
    @ColumnInfo(name = "play_count") val playCount: Long,
    @ColumnInfo(name = "listened_duration_ms") val listenedDurationMs: Long,
    @ColumnInfo(name = "combined_score") val combinedScore: Double,
    @ColumnInfo(name = "play_rank") val playRank: Int,
    @ColumnInfo(name = "duration_rank") val durationRank: Int,
    @ColumnInfo(name = "combined_rank") val combinedRank: Int
)
