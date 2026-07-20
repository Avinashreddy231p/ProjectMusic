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

@Entity(
    tableName = "song_stats",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["song_key"],
            childColumns = ["song_key"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_key")
    val songKey: Long,
    @ColumnInfo(name = "total_play_count") val totalPlayCount: Long = 0,
    @ColumnInfo(name = "completed_play_count") val completedPlayCount: Long = 0,
    @ColumnInfo(name = "total_skip_count") val totalSkipCount: Long = 0,
    @ColumnInfo(name = "total_listening_duration") val totalListeningDuration: Long = 0,
    @ColumnInfo(name = "effective_listening_duration") val effectiveListeningDuration: Long = 0,
    @ColumnInfo(name = "avg_completion_percentage") val avgCompletionPercentage: Double = 0.0,
    @ColumnInfo(name = "last_played_timestamp") val lastPlayedTimestamp: Long = 0,
    @ColumnInfo(name = "first_played_timestamp") val firstPlayedTimestamp: Long = 0,
    @ColumnInfo(name = "total_pause_count") val totalPauseCount: Int = 0,
    @ColumnInfo(name = "total_seek_count") val totalSeekCount: Int = 0,
    @ColumnInfo(name = "longest_listening_streak") val longestListeningStreak: Int = 0,
    @ColumnInfo(name = "monthly_play_count") val monthlyPlayCount: Int = 0,
    @ColumnInfo(name = "weekly_play_count") val weeklyPlayCount: Int = 0,
    @ColumnInfo(name = "daily_play_count") val dailyPlayCount: Int = 0,
    @ColumnInfo(name = "last_synced") val lastSynced: Long = 0
)
