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
    tableName = "song_fingerprints",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["song_key"],
            childColumns = ["song_key"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongFingerprintEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_key")
    val songKey: Long,
    val acoustid: String? = null,
    val sha256: String? = null,
    val isrc: String? = null,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: Int = 1,
    @ColumnInfo(name = "scan_date") val scanDate: Long = System.currentTimeMillis(),
    val confidence: Double? = null
)
