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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "song_instrument_relationship",
    primaryKeys = ["song_id", "instrument_id"],
    foreignKeys = [
        ForeignKey(
            entity = InstrumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["instrument_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("song_id"),
        Index("instrument_id")
    ]
)
data class SongInstrumentEntity(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "instrument_id") val instrumentId: Long,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Double,
    @ColumnInfo(name = "source") val source: Int,
    @ColumnInfo(name = "date_assigned") val dateAssigned: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)
