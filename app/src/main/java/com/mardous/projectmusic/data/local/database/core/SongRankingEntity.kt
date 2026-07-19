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
 * along with this program.  See the GNU General Public License
 * for more details.
 *
 */

package com.mardous.projectmusic.data.local.database.core

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "song_rankings",
    primaryKeys = ["song_key", "ranking_type"],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["song_key"],
            childColumns = ["song_key"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongRankingEntity(
    @ColumnInfo(name = "song_key")
    val songKey: Long,
    @ColumnInfo(name = "ranking_type")
    val rankingType: String,
    @ColumnInfo(name = "play_rank") val playRank: Int = 0,
    @ColumnInfo(name = "duration_rank") val durationRank: Int = 0,
    @ColumnInfo(name = "overall_rank") val overallRank: Int = 0,
    @ColumnInfo(name = "overall_score") val overallScore: Double = 0.0,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "ranking_version") val rankingVersion: Int = 1
)
