package com.mardous.projectmusic.data.local.database.dao

import androidx.room.*
import com.mardous.projectmusic.data.local.database.analytics.QueueEntity

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueue(items: List<QueueEntity>)

    @Query("SELECT * FROM queue ORDER BY `order` ASC")
    suspend fun savedItems(): List<QueueEntity>

    @Query("DELETE FROM queue WHERE id NOT IN (:currentIds)")
    suspend fun removeNotIn(currentIds: List<String>)

    @Transaction
    suspend fun replaceQueue(newQueue: List<QueueEntity>) {
        removeNotIn(newQueue.map { it.id })
        saveQueue(newQueue)
    }
}
