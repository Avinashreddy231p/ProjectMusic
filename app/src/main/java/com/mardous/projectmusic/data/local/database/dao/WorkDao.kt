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
import com.mardous.projectmusic.data.local.database.sync.PendingWorkEntity

@Dao
interface WorkDao {
    @Upsert
    suspend fun upsertWork(work: PendingWorkEntity)

    @Query("SELECT * FROM pending_work WHERE status = 'PENDING' ORDER BY priority DESC, created_at ASC")
    suspend fun getPendingWork(): List<PendingWorkEntity>

    @Query("UPDATE pending_work SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM pending_work WHERE id = :id")
    suspend fun deleteWork(id: Long)

    @Query("DELETE FROM pending_work WHERE status = 'COMPLETED'")
    suspend fun clearCompletedWork()
}
