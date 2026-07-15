package com.mardous.booming.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListeningSessionGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ListeningSessionGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<ListeningSessionGroupEntity>)

    @Query("SELECT * FROM listening_session_groups WHERE group_id = :groupId")
    suspend fun getGroup(groupId: String): ListeningSessionGroupEntity?

    @Query("SELECT AVG(total_songs) FROM listening_session_groups")
    suspend fun avgSongsPerSession(): Float?

    @Query("SELECT AVG(total_duration_ms) FROM listening_session_groups")
    suspend fun avgSessionDurationMs(): Long?

    @Query("SELECT COUNT(*) FROM listening_session_groups")
    suspend fun totalSessionGroups(): Long

    @Query("SELECT MAX(total_songs) FROM listening_session_groups")
    suspend fun maxSongsInSession(): Int?

    @Query("SELECT MAX(total_duration_ms) FROM listening_session_groups")
    suspend fun maxSessionDurationMs(): Long?

    @Query("SELECT * FROM listening_session_groups ORDER BY start_time DESC LIMIT :limit")
    suspend fun getRecentGroups(limit: Int = 50): List<ListeningSessionGroupEntity>

    @Query("SELECT COUNT(*) FROM listening_session_groups WHERE total_songs = 1")
    suspend fun singleSongSessionCount(): Long

    @Query("SELECT COUNT(*) FROM listening_session_groups WHERE total_songs >= 10")
    suspend fun longSessionCount(): Long

    @Query("DELETE FROM listening_session_groups")
    suspend fun clearAll()
}
