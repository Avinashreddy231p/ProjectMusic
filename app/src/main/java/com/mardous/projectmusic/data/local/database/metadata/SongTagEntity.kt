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
 */

package com.mardous.projectmusic.data.local.database.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "song_tag_relationship",
    primaryKeys = ["song_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("song_id"),
        Index("tag_id")
    ]
)
data class SongTagEntity(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Double,
    @ColumnInfo(name = "source") val source: Int,
    @ColumnInfo(name = "date_assigned") val dateAssigned: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)
