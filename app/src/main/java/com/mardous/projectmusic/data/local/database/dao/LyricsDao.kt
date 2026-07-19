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

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mardous.projectmusic.data.local.database.intel.LyricsEntity

@Dao
interface LyricsDao {
    @Upsert
    suspend fun insertLyrics(lyrics: List<LyricsEntity>)

    @Upsert
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE id = :songId")
    suspend fun removeLyrics(songId: Long)

    @Query("DELETE FROM lyrics")
    suspend fun removeLyrics()

    @Query("SELECT * FROM lyrics")
    suspend fun getAllLyrics(): List<LyricsEntity>

    @Query("SELECT * FROM lyrics WHERE id = :songId")
    suspend fun getLyrics(songId: Long): LyricsEntity?
}
