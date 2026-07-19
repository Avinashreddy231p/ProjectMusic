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
import androidx.room.PrimaryKey

@Entity(tableName = "database_health")
data class DatabaseHealthEntity(
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo(name = "last_sync") val lastSync: Long = 0,
    @ColumnInfo(name = "last_ranking") val lastRanking: Long = 0,
    @ColumnInfo(name = "last_vacuum") val lastVacuum: Long = 0,
    @ColumnInfo(name = "last_integrity_check") val lastIntegrityCheck: Long = 0,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int = 0
)
