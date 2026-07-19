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

package com.mardous.projectmusic.data.local.database.intel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mardous.projectmusic.data.local.database.core.SongEntity

@Entity(
    tableName = "song_ai",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["song_key"],
            childColumns = ["song_key"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongAIEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_key")
    val songKey: Long,
    val energy: Double? = null,
    val valence: Double? = null,
    val danceability: Double? = null,
    val speechiness: Double? = null,
    val tempo: Double? = null,
    val summary: String? = null,
    @ColumnInfo(name = "model_name") val modelName: String? = null,
    @ColumnInfo(name = "model_version") val modelVersion: String? = null,
    @ColumnInfo(name = "generated_at") val generatedAt: Long = System.currentTimeMillis(),
    val confidence: Double? = null
)
