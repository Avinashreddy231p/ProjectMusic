package com.mardous.booming.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ListeningSessionEntity): Long

    @Query("DELETE FROM listening_sessions WHERE session_id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

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

    @Query("SELECT SUM(playback_duration_ms) FROM listening_sessions")
    suspend fun getTotalListeningTimeMs(): Long?

    @Query("SELECT SUM(playback_duration_ms) FROM listening_sessions WHERE song_id = :songId")
    suspend fun getTotalListeningTimeForSong(songId: Long): Long?

    @Query("SELECT SUM(playback_duration_ms) FROM listening_sessions WHERE year_month = :yearMonth")
    suspend fun getTotalListeningTimeMsForMonth(yearMonth: String): Long?

    @Query("SELECT SUM(playback_duration_ms) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getTotalListeningTimeMsInRange(from: Long, to: Long): Long?

    @Query("SELECT COUNT(DISTINCT song_id) FROM listening_sessions")
    suspend fun getUniqueSongsCount(): Long

    @Query("SELECT COUNT(DISTINCT artist_name) FROM listening_sessions")
    suspend fun getUniqueArtistsCount(): Long

    @Query("SELECT COUNT(DISTINCT album_id) FROM listening_sessions")
    suspend fun getUniqueAlbumsCount(): Long

    @Query("SELECT COUNT(DISTINCT genre) FROM listening_sessions WHERE genre IS NOT NULL AND genre != ''")
    suspend fun getUniqueGenresCount(): Long

    @Query("SELECT COUNT(DISTINCT year_month) FROM listening_sessions")
    suspend fun getActiveMonthsCount(): Int

    @Query("SELECT MIN(start_date) FROM listening_sessions WHERE start_date != ''")
    suspend fun getFirstListeningDate(): String?

    @Query("SELECT MAX(playback_duration_ms) FROM listening_sessions")
    suspend fun getLongestListeningSessionMs(): Long?

    @Query("SELECT COUNT(*) FROM listening_sessions WHERE completion_percent >= 90.0")
    suspend fun getCompletedCount(): Long

    @Query("SELECT COUNT(*) FROM listening_sessions WHERE completion_percent < 90.0 AND end_reason NOT IN ('track_finished', 'repeat')")
    suspend fun getSkippedCount(): Long

    @Query("SELECT SUM(pause_count) FROM listening_sessions")
    suspend fun getTotalPauses(): Long?

    @Query("SELECT SUM(seek_count) FROM listening_sessions")
    suspend fun getTotalSeeks(): Long?

    @Query("SELECT SUM(seek_forward_count) FROM listening_sessions")
    suspend fun getTotalForwardSeeks(): Long?

    @Query("SELECT SUM(seek_backward_count) FROM listening_sessions")
    suspend fun getTotalBackwardSeeks(): Long?

    @Query("SELECT SUM(pause_duration_ms) FROM listening_sessions")
    suspend fun getTotalPauseDurationMs(): Long?

    @Query("SELECT AVG(completion_percent) FROM listening_sessions")
    suspend fun getAvgCompletionPercent(): Double?

    @Query("DELETE FROM listening_sessions")
    suspend fun clearAll()

    @Query("DELETE FROM listening_sessions WHERE session_id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM listening_sessions WHERE session_id IN (SELECT session_id FROM listening_sessions ORDER BY start_time ASC LIMIT :count)")
    suspend fun deleteOldestSessions(count: Int): Int

    @Query("SELECT * FROM listening_sessions ORDER BY start_time DESC")
    suspend fun getAllSessions(): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions ORDER BY start_time ASC")
    suspend fun getAllSessionsAsc(): List<ListeningSessionEntity>

    @Query("SELECT * FROM listening_sessions WHERE year_month >= :fromMonth AND year_month <= :toMonth ORDER BY start_time ASC")
    suspend fun getSessionsForMonthRange(fromMonth: String, toMonth: String): List<ListeningSessionEntity>

    // --- Top Items ---

    @Query("SELECT song_id AS songId, song_title AS songTitle, artist_name AS artistName, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount, AVG(completion_percent) AS avgCompletion FROM listening_sessions GROUP BY song_id ORDER BY totalPlayed DESC LIMIT :limit")
    suspend fun getTopSongs(limit: Int = 50): List<SongStats>

    @Query("SELECT artist_name AS artistName, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount FROM listening_sessions GROUP BY artist_name ORDER BY totalPlayed DESC LIMIT :limit")
    suspend fun getTopArtists(limit: Int = 50): List<ArtistStats>

    @Query("SELECT album_id AS albumId, album_name AS albumName, artist_name AS artistName, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount FROM listening_sessions GROUP BY album_id ORDER BY totalPlayed DESC LIMIT :limit")
    suspend fun getTopAlbums(limit: Int = 50): List<AlbumStats>

    @Query("SELECT year_month AS period, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS sessionCount FROM listening_sessions GROUP BY year_month ORDER BY year_month DESC LIMIT :limit")
    suspend fun getMonthlyStats(limit: Int = 12): List<PeriodStats>

    @Query("SELECT year AS period, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS sessionCount FROM listening_sessions GROUP BY year ORDER BY year DESC LIMIT :limit")
    suspend fun getYearlyStats(limit: Int = 10): List<PeriodStats>

    @Query("SELECT genre, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount FROM listening_sessions WHERE genre IS NOT NULL AND genre != '' GROUP BY genre ORDER BY totalPlayed DESC LIMIT :limit")
    suspend fun getTopGenres(limit: Int = 20): List<GenreStats>

    // --- Time-range filtered ---

    @Query("SELECT song_id AS songId, song_title AS songTitle, artist_name AS artistName, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount, AVG(completion_percent) AS avgCompletion FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY song_id ORDER BY totalPlayed DESC LIMIT :limit")
    suspend fun getTopSongsInRange(from: Long, to: Long, limit: Int = 50): List<SongStats>

    @Query("SELECT artist_name AS artistName, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY artist_name ORDER BY totalPlayed DESC LIMIT :limit")
    suspend fun getTopArtistsInRange(from: Long, to: Long, limit: Int = 50): List<ArtistStats>

    @Query("SELECT album_id AS albumId, album_name AS albumName, artist_name AS artistName, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY album_id ORDER BY totalPlayed DESC LIMIT :limit")
    suspend fun getTopAlbumsInRange(from: Long, to: Long, limit: Int = 50): List<AlbumStats>

    @Query("SELECT genre, SUM(playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount FROM listening_sessions WHERE start_time >= :from AND start_time <= :to AND genre IS NOT NULL AND genre != '' GROUP BY genre ORDER BY totalPlayed DESC LIMIT :limit")
    suspend fun getTopGenresInRange(from: Long, to: Long, limit: Int = 50): List<GenreStats>

    @Query("SELECT COUNT(*) AS totalSessions, COALESCE(SUM(playback_duration_ms), 0) AS totalListeningMs, COUNT(DISTINCT song_id) AS uniqueSongs, COUNT(DISTINCT artist_name) AS uniqueArtists, COUNT(DISTINCT album_id) AS uniqueAlbums, SUM(CASE WHEN completion_percent >= 90.0 THEN 1 ELSE 0 END) AS completedCount, SUM(CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1 ELSE 0 END) AS skippedCount, SUM(pause_count) AS totalPauses, SUM(seek_count) AS totalSeeks FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getSummaryInRange(from: Long, to: Long): RangeSummaryResult

    // --- Activity heatmap ---

    @Query("SELECT CAST(start_time / 86400000 AS INTEGER) AS dayEpoch, COUNT(*) AS playCount FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY dayEpoch ORDER BY dayEpoch ASC")
    suspend fun getDailyActivity(from: Long, to: Long): List<DailyActivity>

    @Query("SELECT hour AS hourOfDay, COUNT(*) AS playCount FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY hour ORDER BY hour ASC")
    suspend fun getHourlyDistribution(from: Long, to: Long): List<HourlyCount>

    @Query("SELECT CASE CAST(strftime('%w', start_time / 1000, 'unixepoch') AS INTEGER) WHEN 0 THEN 'Sunday' WHEN 1 THEN 'Monday' WHEN 2 THEN 'Tuesday' WHEN 3 THEN 'Wednesday' WHEN 4 THEN 'Thursday' WHEN 5 THEN 'Friday' WHEN 6 THEN 'Saturday' ELSE 'Unknown' END AS dayOfWeek, COUNT(*) AS playCount FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY dayOfWeek ORDER BY playCount DESC")
    suspend fun getDayOfWeekDistribution(from: Long, to: Long): List<DayOfWeekCount>

    // --- End reasons ---

    @Query("SELECT end_reason AS endReason, COUNT(*) AS count FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY end_reason ORDER BY count DESC")
    suspend fun getEndReasonDistribution(from: Long, to: Long): List<EndReasonStats>

    // --- Queue source ---

    @Query("SELECT queue_source AS queueSource, COUNT(*) AS count FROM listening_sessions WHERE start_time >= :from AND start_time <= :to AND queue_source != '' GROUP BY queue_source ORDER BY count DESC")
    suspend fun getQueueSourceDistribution(from: Long, to: Long): List<QueueSourceStats>

    // --- Playback origin ---

    @Query("SELECT playback_origin AS playbackOrigin, COUNT(*) AS count FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY playback_origin ORDER BY count DESC")
    suspend fun getPlaybackOriginDistribution(from: Long, to: Long): List<PlaybackOriginStats>

    // --- Audio format ---

    @Query("SELECT audio_format AS audioFormat, COUNT(*) AS count FROM listening_sessions WHERE start_time >= :from AND start_time <= :to AND audio_format != '' GROUP BY audio_format ORDER BY count DESC")
    suspend fun getAudioFormatDistribution(from: Long, to: Long): List<AudioFormatStats>

    // --- Output device ---

    @Query("SELECT output_device AS outputDevice, COUNT(*) AS count FROM listening_sessions WHERE start_time >= :from AND start_time <= :to AND output_device != '' GROUP BY output_device ORDER BY count DESC")
    suspend fun getDeviceDistribution(from: Long, to: Long): List<DeviceStats>

    // --- Behavior stats ---

    @Query("SELECT AVG(pause_count) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getAvgPauseCount(from: Long, to: Long): Double?

    @Query("SELECT AVG(seek_count) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getAvgSeekCount(from: Long, to: Long): Double?

    @Query("SELECT SUM(CASE WHEN shuffle_enabled THEN 1 ELSE 0 END) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getShuffleEnabledCount(from: Long, to: Long): Long?

    @Query("SELECT repeat_mode, COUNT(*) AS count FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY repeat_mode ORDER BY count DESC")
    suspend fun getRepeatModeDistribution(from: Long, to: Long): List<RepeatModeStats>

    @Query("SELECT SUM(CASE WHEN is_favorite THEN 1 ELSE 0 END) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getFavoriteCount(from: Long, to: Long): Long?

    @Query("SELECT SUM(CASE WHEN is_weekend THEN 1 ELSE 0 END) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getWeekendCount(from: Long, to: Long): Long?

    @Query("SELECT SUM(CASE WHEN NOT is_weekend THEN 1 ELSE 0 END) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getWeekdayCount(from: Long, to: Long): Long?

    // --- Context stats ---

    @Query("SELECT SUM(CASE WHEN screen_on THEN 1 ELSE 0 END) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getScreenOnCount(from: Long, to: Long): Long?

    @Query("SELECT SUM(CASE WHEN charging THEN 1 ELSE 0 END) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getChargingCount(from: Long, to: Long): Long?

    @Query("SELECT AVG(volume_start) FROM listening_sessions WHERE start_time >= :from AND start_time <= :to AND volume_start >= 0")
    suspend fun getAvgVolumeStart(from: Long, to: Long): Double?

    // --- Trend ---

    @Query("SELECT year_month AS month, SUM(playback_duration_ms) AS totalTime FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY year_month ORDER BY year_month ASC")
    suspend fun getMonthlyTrends(from: Long, to: Long): List<TrendData>

    // --- Discovery ---

    @Query("SELECT song_id AS songId, song_title AS songTitle, artist_name AS artistName FROM listening_sessions WHERE start_time >= :from AND start_time <= :to AND song_id NOT IN (SELECT song_id FROM listening_sessions WHERE start_time < :from) GROUP BY song_id")
    suspend fun getNewDiscoveries(from: Long, to: Long): List<DiscoveryData>

    @Query("SELECT song_id AS songId, song_title AS songTitle, artist_name AS artistName, COUNT(*) AS playCount FROM listening_sessions WHERE start_time >= :from AND start_time <= :to GROUP BY song_id HAVING playCount > 1 ORDER BY playCount DESC")
    suspend fun getRepeatListens(from: Long, to: Long): List<RepeatData>

    @Query("SELECT playback_duration_ms FROM listening_sessions WHERE start_time >= :from AND start_time <= :to")
    suspend fun getAllSessionDurations(from: Long, to: Long): List<Long>

    @Query("SELECT DISTINCT timezone_id FROM listening_sessions WHERE timezone_id != '' AND start_time >= :from AND start_time <= :to")
    suspend fun getTimezonesInRange(from: Long, to: Long): List<String>

    // --- Data classes ---

    data class SongStats(
        val songId: Long,
        val songTitle: String,
        val artistName: String,
        val totalPlayed: Long,
        val playCount: Int,
        val avgCompletion: Double
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

    data class RangeSummaryResult(
        val totalSessions: Long,
        val totalListeningMs: Long,
        val uniqueSongs: Long,
        val uniqueArtists: Long,
        val uniqueAlbums: Long,
        val completedCount: Long,
        val skippedCount: Long,
        val totalPauses: Long,
        val totalSeeks: Long
    )

    data class DailyActivity(
        @ColumnInfo(name = "dayEpoch") val dayEpoch: Int,
        @ColumnInfo(name = "playCount") val playCount: Int
    )

    data class HourlyCount(
        @ColumnInfo(name = "hourOfDay") val hourOfDay: Int,
        @ColumnInfo(name = "playCount") val playCount: Int
    )

    data class DayOfWeekCount(
        val dayOfWeek: String,
        @ColumnInfo(name = "playCount") val playCount: Int
    )

    data class EndReasonStats(
        val endReason: String,
        val count: Int
    )

    data class QueueSourceStats(
        val queueSource: String,
        val count: Int
    )

    data class PlaybackOriginStats(
        val playbackOrigin: String,
        val count: Int
    )

    data class AudioFormatStats(
        val audioFormat: String,
        val count: Int
    )

    data class DeviceStats(
        val outputDevice: String,
        val count: Int
    )

    data class RepeatModeStats(
        @ColumnInfo(name = "repeat_mode") val repeatMode: String,
        val count: Int
    )

    data class TrendData(
        @ColumnInfo(name = "month") val month: String,
        @ColumnInfo(name = "totalTime") val totalTime: Long
    )

    data class DiscoveryData(
        @ColumnInfo(name = "songId") val songId: Long,
        @ColumnInfo(name = "songTitle") val songTitle: String,
        @ColumnInfo(name = "artistName") val artistName: String
    )

    data class RepeatData(
        @ColumnInfo(name = "songId") val songId: Long,
        @ColumnInfo(name = "songTitle") val songTitle: String,
        @ColumnInfo(name = "artistName") val artistName: String,
        @ColumnInfo(name = "playCount") val playCount: Int
    )
}
