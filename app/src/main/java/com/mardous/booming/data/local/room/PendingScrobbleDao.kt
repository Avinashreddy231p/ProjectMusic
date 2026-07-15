package com.mardous.booming.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingScrobbleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scrobble: PendingScrobbleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scrobbles: List<PendingScrobbleEntity>)

    @Update
    suspend fun update(scrobble: PendingScrobbleEntity)

    @Query("SELECT * FROM PendingScrobbleEntity ORDER BY timestamp ASC")
    fun getAllFlow(): Flow<List<PendingScrobbleEntity>>

    @Query("SELECT * FROM PendingScrobbleEntity ORDER BY timestamp ASC")
    suspend fun getAll(): List<PendingScrobbleEntity>

    @Query("DELETE FROM PendingScrobbleEntity WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM PendingScrobbleEntity WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    @Query("DELETE FROM PendingScrobbleEntity")
    suspend fun deleteAll()
}
