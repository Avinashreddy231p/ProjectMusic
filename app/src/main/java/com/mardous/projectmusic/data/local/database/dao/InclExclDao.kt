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

package com.mardous.projectmusic.data.local.database.dao

import androidx.room.*
import com.mardous.projectmusic.data.local.database.metadata.InclExclEntity

@Dao
interface InclExclDao {
    companion object {
        const val WHITELIST = 0
        const val BLACKLIST = 1
    }

    @Query("SELECT * FROM incl_excl WHERE type = :type")
    suspend fun getPaths(type: Int): List<InclExclEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPath(inclExclEntity: InclExclEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaths(inclExclEntities: List<InclExclEntity>)

    @Delete
    suspend fun deletePath(inclExclEntity: InclExclEntity)

    @Query("DELETE FROM incl_excl WHERE type = :type")
    suspend fun clearPaths(type: Int)

    @Query("SELECT * FROM incl_excl WHERE type = 1")
    fun blackListPaths(): List<InclExclEntity>

    @Query("SELECT * FROM incl_excl WHERE type = 0")
    fun whitelistPaths(): List<InclExclEntity>
}
