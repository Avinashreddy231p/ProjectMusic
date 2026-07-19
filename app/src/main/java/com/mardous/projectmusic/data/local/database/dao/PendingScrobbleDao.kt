package com.mardous.projectmusic.data.local.database.dao

import androidx.room.*
import com.mardous.projectmusic.data.local.database.analytics.PendingScrobbleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingScrobbleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scrobble: PendingScrobbleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scrobbles: List<PendingScrobbleEntity>)

    @Update
    suspend fun update(scrobble: PendingScrobbleEntity)

    @Query("SELECT * FROM pending_scrobbles ORDER BY timestamp ASC")
    fun getAllFlow(): Flow<List<PendingScrobbleEntity>>

    @Query("SELECT * FROM pending_scrobbles ORDER BY timestamp ASC")
    suspend fun getAll(): List<PendingScrobbleEntity>

    @Query("DELETE FROM pending_scrobbles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_scrobbles WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    @Query("DELETE FROM pending_scrobbles")
    suspend fun deleteAll()
}
