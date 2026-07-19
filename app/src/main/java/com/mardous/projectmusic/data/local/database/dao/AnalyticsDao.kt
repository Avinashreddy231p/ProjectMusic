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

package com.mardous.projectmusic.data.local.database.dao

import androidx.room.*
import com.mardous.projectmusic.data.local.database.analytics.DailySummaryEntity
import com.mardous.projectmusic.data.local.database.analytics.LibraryStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    @Upsert
    suspend fun upsertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun getDailySummariesFlow(): Flow<List<DailySummaryEntity>>

    @Upsert
    suspend fun upsertLibraryStats(stats: LibraryStatsEntity)

    @Query("SELECT * FROM library_stats WHERE id = 0")
    fun getLibraryStatsFlow(): Flow<LibraryStatsEntity?>
}
