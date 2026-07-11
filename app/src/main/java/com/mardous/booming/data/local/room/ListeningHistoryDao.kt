package com.mardous.booming.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ListeningSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ListeningSessionEntity>): List<Long>

    @Query("SELECT * FROM listening_sessions ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    suspend fun getSessionsPaged(limit: Int = 50, offset: Int = 0): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions WHERE song_id = :songId ORDER BY start_time DESC LIMIT :limit")
    suspend fun getSessionsForSong(songId: Long, limit: Int = 100): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions WHERE year_month = :yearMonth ORDER BY start_time DESC LIMIT :limit")
    suspend fun getSessionsForMonth(yearMonth: String, limit: Int = 500): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions WHERE start_time >= :from AND start_time <= :to ORDER BY start_time DESC")
    suspend fun getSessionsInRange(from: Long, to: Long): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions ORDER BY start_time DESC")
    fun getAllSessionsFlow(): Flow<List<ListeningSessionEntity>>

    @Query("SELECT * FROM listening_sessions ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getSessionsPagedFlow(limit: Int = 50, offset: Int = 0): Flow<List<ListeningSessionEntity>>

    @Query("SELECT COUNT(*) FROM listening_sessions")
    suspend fun getTotalSessionCount(): Long

    @Query("SELECT SUM(duration_played_ms) FROM listening_sessions")
    suspend fun getTotalListeningTimeMs(): Long

    @Query("SELECT SUM(duration_played_ms) FROM listening_sessions WHERE year_month = :yearMonth")
    suspend fun getTotalListeningTimeMsForMonth(yearMonth: String): Long

    @Query("SELECT SUM(duration_played_ms) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getTotalListeningTimeMsInRange(from: Long, to: Long): Long

    @Query("SELECT song_id, song_title, artist_name, SUM(duration_played_ms) as total_played, COUNT(*) as play_count, AVG(duration_played_ms) as avg_played FROM listening_sessions GROUP BY song_id ORDER BY total_played DESC LIMIT :limit")
    suspend fun getTopSongs(limit: Int = 50): List<SongStats>

    @Query("SELECT artist_name, SUM(duration_played_ms) as total_played, COUNT(*) as play_count FROM listening_sessions GROUP BY artist_name ORDER BY total_played DESC LIMIT :limit")
    suspend fun getTopArtists(limit: Int = 50): List<ArtistStats>

    @Query("SELECT album_id, album_name, artist_name, SUM(duration_played_ms) as total_played, COUNT(*) as play_count FROM listening_sessions GROUP BY album_id ORDER BY total_played DESC LIMIT :limit")
    suspend fun getTopAlbums(limit: Int = 50): List<AlbumStats>

    @Query("SELECT year_month as period, SUM(duration_played_ms) as total_played, COUNT(*) as session_count FROM listening_sessions GROUP BY year_month ORDER BY year_month DESC LIMIT :limit")
    suspend fun getMonthlyStats(limit: Int = 12): List<PeriodStats>

    @Query("SELECT year, SUM(duration_played_ms) as total_played, COUNT(*) as session_count FROM listening_sessions GROUP BY year ORDER BY year DESC LIMIT :limit")
    suspend fun getYearlyStats(limit: Int = 10): List<PeriodStats>

    @Query("SELECT genre, SUM(duration_played_ms) as total_played, COUNT(*) as play_count FROM listening_sessions WHERE genre IS NOT NULL AND genre != '' GROUP BY genre ORDER BY total_played DESC LIMIT :limit")
    suspend fun getTopGenres(limit: Int = 20): List<GenreStats>

    @Query("SELECT COUNT(DISTINCT year_month) FROM listening_sessions")
    suspend fun getActiveMonthsCount(): Int

    @Query("SELECT MIN(DATE(start_time / 1000, 'unixepoch')) FROM listening_sessions")
    suspend fun getFirstListeningDate(): String?

    @Query("SELECT COUNT(DISTINCT song_id) FROM listening_sessions")
    suspend fun getUniqueSongsCount(): Long

    @Query("SELECT COUNT(DISTINCT artist_name) FROM listening_sessions")
    suspend fun getUniqueArtistsCount(): Long

    @Query("SELECT COUNT(DISTINCT album_id) FROM listening_sessions")
    suspend fun getUniqueAlbumsCount(): Long

    @Query("SELECT MAX(end_time - start_time) FROM listening_sessions WHERE completed = 1")
    suspend fun getLongestListeningSessionMs(): Long

    @Query("SELECT COUNT(*) FROM listening_sessions WHERE completed = 1")
    suspend fun getCompletedCount(): Long

    @Query("SELECT COUNT(*) FROM listening_sessions WHERE skipped = 1")
    suspend fun getSkippedCount(): Long

    @Query("SELECT SUM(pause_count) FROM listening_sessions")
    suspend fun getTotalPauses(): Long

    @Query("SELECT SUM(seek_count) FROM listening_sessions")
    suspend fun getTotalSeeks(): Long

    @Query("DELETE FROM listening_sessions")
    suspend fun clearAll()

    @Query("DELETE FROM listening_sessions WHERE session_id IN (SELECT session_id FROM listening_sessions ORDER BY start_time ASC LIMIT :count)")
    suspend fun deleteOldestSessions(count: Int): Int

    @Query("SELECT * FROM listening_sessions ORDER BY start_time DESC")
    suspend fun getAllSessions(): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions ORDER BY start_time ASC")
    suspend fun getAllSessionsAsc(): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions WHERE year_month >= :fromMonth AND year_month <= :toMonth ORDER BY start_time ASC")
    suspend fun getSessionsForMonthRange(fromMonth: String, toMonth: String): List<ListeningSessionEntity>

    data class SongStats(
        val songId: Long,
        val songTitle: String,
        val artistName: String,
        val totalPlayed: Long,
        val playCount: Int,
        val avgPlayed: Double
    )

    data class ArtistStats(
        val artistName: String,
        val totalPlayed: Long,
        val playCount: Int
    )

    data class AlbumStats(
        val albumId: Long,
        val albumName: String,
        val artistName: String,
        val totalPlayed: Long,
        val playCount: Int
    )

    data class PeriodStats(
        val period: String,
        val totalPlayed: Long,
        val sessionCount: Int
    )

    data class GenreStats(
        val genre: String,
        val totalPlayed: Long,
        val playCount: Int
    )
}
