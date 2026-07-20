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
 */

package com.mardous.projectmusic.data.local.database.dao

import androidx.room.*
import com.mardous.projectmusic.data.local.database.intel.*
import com.mardous.projectmusic.data.local.database.core.SongStatsEntity
import com.mardous.projectmusic.data.local.database.core.SongRankingEntity
import com.mardous.projectmusic.data.local.database.metadata.AlbumEntity
import com.mardous.projectmusic.data.local.database.metadata.AlbumArtistEntity
import com.mardous.projectmusic.data.local.database.metadata.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RankingDao {

    @Upsert
    suspend fun upsertSong(song: com.mardous.projectmusic.data.local.database.core.SongEntity): Long

    @Query("SELECT * FROM songs ORDER BY title ASC")
    suspend fun getAllSongs(): List<com.mardous.projectmusic.data.local.database.core.SongEntity>

    @Query("SELECT * FROM song_stats")
    suspend fun getAllSongStats(): List<SongStatsEntity>

    @Query("SELECT * FROM song_rankings ORDER BY ranking_type, overall_rank")
    suspend fun getAllSongRankings(): List<SongRankingEntity>

    @Query("SELECT * FROM song_metadata")
    suspend fun getAllSongMetadata(): List<com.mardous.projectmusic.data.local.database.core.SongMetadataEntity>

    @Query("SELECT * FROM lyrics")
    suspend fun getAllLyrics(): List<com.mardous.projectmusic.data.local.database.intel.LyricsEntity>

    @Query("SELECT song_key FROM songs WHERE media_store_id = :mediaStoreId")
    suspend fun getSongKeyByMediaStoreId(mediaStoreId: Long): Long?

    @Query("SELECT * FROM songs WHERE song_key = :songKey")
    suspend fun getSongByKey(songKey: Long): com.mardous.projectmusic.data.local.database.core.SongEntity?

    @Query("SELECT * FROM song_metadata WHERE song_key = :songKey")
    suspend fun getSongMetadata(songKey: Long): com.mardous.projectmusic.data.local.database.core.SongMetadataEntity?

    // --- Genre Rankings ---
    @Upsert
    suspend fun upsertGenreRankings(rankings: List<GenreSongRankingEntity>)

    @Query("DELETE FROM genre_song_ranking WHERE genre_id = :genreId")
    suspend fun clearGenreRankings(genreId: Long)

    @Query("""
        SELECT song_id FROM genre_song_ranking
        WHERE genre_id = :genreId
        ORDER BY combined_rank ASC
    """)
    fun getTopSongIdsByGenre(genreId: Long): Flow<List<Long>>

    // --- Mood Rankings ---
    @Upsert
    suspend fun upsertMoodRankings(rankings: List<MoodSongRankingEntity>)

    @Query("DELETE FROM mood_song_ranking WHERE mood_id = :moodId")
    suspend fun clearMoodRankings(moodId: Long)

    @Query("""
        SELECT song_id FROM mood_song_ranking
        WHERE mood_id = :moodId
        ORDER BY combined_rank ASC
    """)
    fun getTopSongIdsByMood(moodId: Long): Flow<List<Long>>

    // --- Tag Rankings ---
    @Upsert
    suspend fun upsertTagRankings(rankings: List<TagSongRankingEntity>)

    @Query("DELETE FROM tag_song_ranking WHERE tag_id = :tagId")
    suspend fun clearTagRankings(tagId: Long)

    @Query("""
        SELECT song_id FROM tag_song_ranking
        WHERE tag_id = :tagId
        ORDER BY combined_rank ASC
    """)
    fun getTopSongIdsByTag(tagId: Long): Flow<List<Long>>

    // --- Instrument Rankings ---
    @Upsert
    suspend fun upsertInstrumentRankings(rankings: List<InstrumentSongRankingEntity>)

    @Query("DELETE FROM instrument_song_ranking WHERE instrument_id = :instrumentId")
    suspend fun clearInstrumentRankings(instrumentId: Long)

    @Query("""
        SELECT song_id FROM instrument_song_ranking
        WHERE instrument_id = :instrumentId
        ORDER BY combined_rank ASC
    """)
    fun getTopSongIdsByInstrument(instrumentId: Long): Flow<List<Long>>

    // --- Combined dimension rankings ---
    @Query("SELECT * FROM genre_song_ranking WHERE song_id = :songId AND genre_id = :genreId")
    suspend fun getGenreRanking(songId: Long, genreId: Long): GenreSongRankingEntity?

    @Query("SELECT * FROM mood_song_ranking WHERE song_id = :songId AND mood_id = :moodId")
    suspend fun getMoodRanking(songId: Long, moodId: Long): MoodSongRankingEntity?

    @Query("""
        INSERT INTO mood_song_ranking (song_id, mood_id, play_count, listened_duration_ms, combined_score, play_rank, duration_rank, combined_rank)
        SELECT 
            smr.song_id, 
            :moodId, 
            COUNT(ls.session_id) as play_count,
            COALESCE(SUM(ls.playback_duration_ms), 0) as listened_duration_ms,
            COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM songs s INNER JOIN song_metadata sm ON s.song_key = sm.song_key WHERE s.media_store_id = smr.song_id), 0)
            + COALESCE(SUM(CASE WHEN ls.completion_percent >= 90.0 THEN 1 ELSE 0 END), 0) * 10
            + COUNT(ls.session_id) * 5
            + COALESCE(SUM(ls.playback_duration_ms), 0) / 60000.0 * 5 as score,
            0, 0, 0
        FROM song_mood_relationship smr
        LEFT JOIN listening_sessions ls ON smr.song_id = ls.song_id
        WHERE smr.mood_id = :moodId
        GROUP BY smr.song_id
    """)
    suspend fun insertMoodRankingsFromSessions(moodId: Long)

    @Query("""
        WITH ranked AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY mood_id ORDER BY combined_score DESC, song_id ASC) AS rn
            FROM mood_song_ranking WHERE mood_id = :moodId
        )
        UPDATE mood_song_ranking SET combined_rank = (SELECT rn FROM ranked WHERE ranked.song_id = mood_song_ranking.song_id)
        WHERE mood_id = :moodId
    """)
    suspend fun updateMoodRanks(moodId: Long)

    @Query("""
        WITH play AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY mood_id ORDER BY play_count DESC, song_id ASC) AS rn
            FROM mood_song_ranking WHERE mood_id = :moodId
        ),
        dur AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY mood_id ORDER BY listened_duration_ms DESC, song_id ASC) AS rn
            FROM mood_song_ranking WHERE mood_id = :moodId
        )
        UPDATE mood_song_ranking 
        SET play_rank = (SELECT rn FROM play WHERE play.song_id = mood_song_ranking.song_id),
            duration_rank = (SELECT rn FROM dur WHERE dur.song_id = mood_song_ranking.song_id)
        WHERE mood_id = :moodId
    """)
    suspend fun updateMoodPlayDurationRanks(moodId: Long)

    @Transaction
    suspend fun rebuildMoodRankings(moodId: Long) {
        clearMoodRankings(moodId)
        insertMoodRankingsFromSessions(moodId)
        updateMoodRanks(moodId)
        updateMoodPlayDurationRanks(moodId)
    }

    @Transaction
    suspend fun rebuildGenreRankings(genreId: Long) {
        clearGenreRankings(genreId)
        insertGenreRankingsFromSessions(genreId)
        updateGenreRanks(genreId)
        updateGenrePlayDurationRanks(genreId)
    }

    @Query("""
        INSERT INTO genre_song_ranking (song_id, genre_id, play_count, listened_duration_ms, combined_score, play_rank, duration_rank, combined_rank)
        SELECT 
            sgr.song_id, 
            :genreId, 
            COUNT(ls.session_id),
            COALESCE(SUM(ls.playback_duration_ms), 0),
            COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM songs s INNER JOIN song_metadata sm ON s.song_key = sm.song_key WHERE s.media_store_id = sgr.song_id), 0)
            + COALESCE(SUM(CASE WHEN ls.completion_percent >= 90.0 THEN 1 ELSE 0 END), 0) * 10
            + COUNT(ls.session_id) * 5
            + COALESCE(SUM(ls.playback_duration_ms), 0) / 60000.0 * 5,
            0, 0, 0
        FROM song_genre_relationship sgr
        LEFT JOIN listening_sessions ls ON sgr.song_id = ls.song_id
        WHERE sgr.genre_id = :genreId
        GROUP BY sgr.song_id
    """)
    suspend fun insertGenreRankingsFromSessions(genreId: Long)

    @Query("""
        WITH ranked AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY genre_id ORDER BY combined_score DESC, song_id ASC) AS rn
            FROM genre_song_ranking WHERE genre_id = :genreId
        )
        UPDATE genre_song_ranking SET combined_rank = (SELECT rn FROM ranked WHERE ranked.song_id = genre_song_ranking.song_id)
        WHERE genre_id = :genreId
    """)
    suspend fun updateGenreRanks(genreId: Long)

    @Query("""
        WITH play AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY genre_id ORDER BY play_count DESC, song_id ASC) AS rn
            FROM genre_song_ranking WHERE genre_id = :genreId
        ),
        dur AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY genre_id ORDER BY listened_duration_ms DESC, song_id ASC) AS rn
            FROM genre_song_ranking WHERE genre_id = :genreId
        )
        UPDATE genre_song_ranking 
        SET play_rank = (SELECT rn FROM play WHERE play.song_id = genre_song_ranking.song_id),
            duration_rank = (SELECT rn FROM dur WHERE dur.song_id = genre_song_ranking.song_id)
        WHERE genre_id = :genreId
    """)
    suspend fun updateGenrePlayDurationRanks(genreId: Long)

    @Query("SELECT id FROM genres")
    suspend fun getAllGenreIds(): List<Long>

    @Transaction
    suspend fun rebuildTagRankings(tagId: Long) {
        clearTagRankings(tagId)
        insertTagRankingsFromSessions(tagId)
        updateTagRanks(tagId)
        updateTagPlayDurationRanks(tagId)
    }

    @Query("""
        INSERT INTO tag_song_ranking (song_id, tag_id, play_count, listened_duration_ms, combined_score, play_rank, duration_rank, combined_rank)
        SELECT 
            str.song_id, 
            :tagId, 
            COUNT(ls.session_id),
            COALESCE(SUM(ls.playback_duration_ms), 0),
            COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM songs s INNER JOIN song_metadata sm ON s.song_key = sm.song_key WHERE s.media_store_id = str.song_id), 0)
            + COALESCE(SUM(CASE WHEN ls.completion_percent >= 90.0 THEN 1 ELSE 0 END), 0) * 10
            + COUNT(ls.session_id) * 5
            + COALESCE(SUM(ls.playback_duration_ms), 0) / 60000.0 * 5,
            0, 0, 0
        FROM song_tag_relationship str
        LEFT JOIN listening_sessions ls ON str.song_id = ls.song_id
        WHERE str.tag_id = :tagId
        GROUP BY str.song_id
    """)
    suspend fun insertTagRankingsFromSessions(tagId: Long)

    @Query("""
        WITH ranked AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY tag_id ORDER BY combined_score DESC, song_id ASC) AS rn
            FROM tag_song_ranking WHERE tag_id = :tagId
        )
        UPDATE tag_song_ranking SET combined_rank = (SELECT rn FROM ranked WHERE ranked.song_id = tag_song_ranking.song_id)
        WHERE tag_id = :tagId
    """)
    suspend fun updateTagRanks(tagId: Long)

    @Query("""
        WITH play AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY tag_id ORDER BY play_count DESC, song_id ASC) AS rn
            FROM tag_song_ranking WHERE tag_id = :tagId
        ),
        dur AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY tag_id ORDER BY listened_duration_ms DESC, song_id ASC) AS rn
            FROM tag_song_ranking WHERE tag_id = :tagId
        )
        UPDATE tag_song_ranking 
        SET play_rank = (SELECT rn FROM play WHERE play.song_id = tag_song_ranking.song_id),
            duration_rank = (SELECT rn FROM dur WHERE dur.song_id = tag_song_ranking.song_id)
        WHERE tag_id = :tagId
    """)
    suspend fun updateTagPlayDurationRanks(tagId: Long)

    @Query("SELECT id FROM tags")
    suspend fun getAllTagIds(): List<Long>

    @Query("""
        UPDATE tags 
        SET 
            song_count = COALESCE((SELECT COUNT(song_id) FROM song_tag_relationship WHERE tag_id = tags.id), 0),
            play_count = COALESCE((SELECT SUM(play_count) FROM tag_song_ranking WHERE tag_id = tags.id), 0),
            listening_duration = COALESCE((SELECT SUM(listened_duration_ms) FROM tag_song_ranking WHERE tag_id = tags.id), 0),
            overall_score = COALESCE((SELECT SUM(combined_score) FROM tag_song_ranking WHERE tag_id = tags.id), 0.0),
            updated_at = :timestamp
    """)
    suspend fun updateTagStats(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH ranked AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY overall_score DESC, id ASC) AS rn FROM tags
        )
        UPDATE tags SET overall_rank = (SELECT rn FROM ranked WHERE ranked.id = tags.id)
    """)
    suspend fun updateTagGlobalRanks()

    @Query("""
        WITH play AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY play_count DESC, id ASC) AS rn FROM tags
        ),
        dur AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY listening_duration DESC, id ASC) AS rn FROM tags
        )
        UPDATE tags 
        SET play_rank = (SELECT rn FROM play WHERE play.id = tags.id),
            duration_rank = (SELECT rn FROM dur WHERE dur.id = tags.id)
    """)
    suspend fun updateTagPlayDurationRanks()

    @Transaction
    suspend fun rebuildInstrumentRankings(instrumentId: Long) {
        clearInstrumentRankings(instrumentId)
        insertInstrumentRankingsFromSessions(instrumentId)
        updateInstrumentRanks(instrumentId)
        updateInstrumentPlayDurationRanks(instrumentId)
    }

    @Query("""
        INSERT INTO instrument_song_ranking (song_id, instrument_id, play_count, listened_duration_ms, combined_score, play_rank, duration_rank, combined_rank)
        SELECT 
            sir.song_id, 
            :instrumentId, 
            COUNT(ls.session_id),
            COALESCE(SUM(ls.playback_duration_ms), 0),
            COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM songs s INNER JOIN song_metadata sm ON s.song_key = sm.song_key WHERE s.media_store_id = sir.song_id), 0)
            + COALESCE(SUM(CASE WHEN ls.completion_percent >= 90.0 THEN 1 ELSE 0 END), 0) * 10
            + COUNT(ls.session_id) * 5
            + COALESCE(SUM(ls.playback_duration_ms), 0) / 60000.0 * 5,
            0, 0, 0
        FROM song_instrument_relationship sir
        LEFT JOIN listening_sessions ls ON sir.song_id = ls.song_id
        WHERE sir.instrument_id = :instrumentId
        GROUP BY sir.song_id
    """)
    suspend fun insertInstrumentRankingsFromSessions(instrumentId: Long)

    @Query("""
        WITH ranked AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY instrument_id ORDER BY combined_score DESC, song_id ASC) AS rn
            FROM instrument_song_ranking WHERE instrument_id = :instrumentId
        )
        UPDATE instrument_song_ranking SET combined_rank = (SELECT rn FROM ranked WHERE ranked.song_id = instrument_song_ranking.song_id)
        WHERE instrument_id = :instrumentId
    """)
    suspend fun updateInstrumentRanks(instrumentId: Long)

    @Query("""
        WITH play AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY instrument_id ORDER BY play_count DESC, song_id ASC) AS rn
            FROM instrument_song_ranking WHERE instrument_id = :instrumentId
        ),
        dur AS (
            SELECT song_id, ROW_NUMBER() OVER (PARTITION BY instrument_id ORDER BY listened_duration_ms DESC, song_id ASC) AS rn
            FROM instrument_song_ranking WHERE instrument_id = :instrumentId
        )
        UPDATE instrument_song_ranking 
        SET play_rank = (SELECT rn FROM play WHERE play.song_id = instrument_song_ranking.song_id),
            duration_rank = (SELECT rn FROM dur WHERE dur.song_id = instrument_song_ranking.song_id)
        WHERE instrument_id = :instrumentId
    """)
    suspend fun updateInstrumentPlayDurationRanks(instrumentId: Long)

    @Query("SELECT id FROM instruments")
    suspend fun getAllInstrumentIds(): List<Long>

    @Query("""
        UPDATE instruments 
        SET 
            song_count = COALESCE((SELECT COUNT(song_id) FROM song_instrument_relationship WHERE instrument_id = instruments.id), 0),
            play_count = COALESCE((SELECT SUM(play_count) FROM instrument_song_ranking WHERE instrument_id = instruments.id), 0),
            listening_duration = COALESCE((SELECT SUM(listened_duration_ms) FROM instrument_song_ranking WHERE instrument_id = instruments.id), 0),
            overall_score = COALESCE((SELECT SUM(combined_score) FROM instrument_song_ranking WHERE instrument_id = instruments.id), 0.0),
            updated_at = :timestamp
    """)
    suspend fun updateInstrumentStats(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH ranked AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY overall_score DESC, id ASC) AS rn FROM instruments
        )
        UPDATE instruments SET overall_rank = (SELECT rn FROM ranked WHERE ranked.id = instruments.id)
    """)
    suspend fun updateInstrumentGlobalRanks()

    @Query("""
        WITH play AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY play_count DESC, id ASC) AS rn FROM instruments
        ),
        dur AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY listening_duration DESC, id ASC) AS rn FROM instruments
        )
        UPDATE instruments 
        SET play_rank = (SELECT rn FROM play WHERE play.id = instruments.id),
            duration_rank = (SELECT rn FROM dur WHERE dur.id = instruments.id)
    """)
    suspend fun updateInstrumentPlayDurationRanks()

    @Query("SELECT id FROM moods")
    suspend fun getAllMoodIds(): List<Long>

    @Query("""
        INSERT OR REPLACE INTO song_stats (
            song_key, 
            total_play_count, 
            completed_play_count,
            total_skip_count, 
            total_listening_duration, 
            effective_listening_duration, 
            avg_completion_percentage,
            last_played_timestamp,
            first_played_timestamp,
            total_pause_count,
            total_seek_count,
            longest_listening_streak,
            monthly_play_count,
            weekly_play_count,
            daily_play_count,
            last_synced
        )
        SELECT 
            s.song_key, 
            COALESCE(COUNT(ls.session_id), 0),
            COALESCE(SUM(CASE WHEN ls.completion_percent >= 90.0 THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN ls.completion_percent < 90.0 AND ls.end_reason NOT IN ('track_finished', 'repeat') THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(ls.playback_duration_ms), 0), 
            COALESCE(SUM(ls.effective_listened_ms), 0),
            COALESCE(AVG(ls.completion_percent), 0.0),
            COALESCE(MAX(ls.start_time), 0),
            COALESCE(MIN(ls.start_time), 0),
            COALESCE(SUM(ls.pause_count), 0),
            COALESCE(SUM(ls.seek_count), 0),
            COALESCE(MAX(ls.effective_listened_ms), 0),
            COALESCE(SUM(CASE WHEN ls.start_time >= strftime('%s', 'now', 'start of month') * 1000 THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN ls.start_time >= strftime('%s', 'now', '-7 days') * 1000 THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN ls.start_time >= strftime('%s', 'now', 'start of day') * 1000 THEN 1 ELSE 0 END), 0),
            strftime('%s','now') * 1000
        FROM songs s
        LEFT JOIN listening_sessions ls ON s.media_store_id = ls.song_id
        GROUP BY s.song_key
    """)
    suspend fun rebuildGlobalSongStats()

    @Query("""
        INSERT OR REPLACE INTO song_rankings (song_key, ranking_type, play_rank, duration_rank, overall_rank, overall_score, last_updated, ranking_version)
        SELECT 
            song_key, 
            'overall',
            0, 0, 0,
            COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM song_metadata sm WHERE sm.song_key = song_stats.song_key), 0)
            + COALESCE(completed_play_count, 0) * 10
            + COALESCE(total_play_count, 0) * 5
            + COALESCE(CAST(total_listening_duration AS REAL) / 60000.0, 0.0) * 5,
            :timestamp, 1
        FROM song_stats
    """)
    suspend fun rebuildGlobalSongRankings(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH play AS (
            SELECT ss.song_key, ROW_NUMBER() OVER (ORDER BY ss.total_play_count DESC, ss.song_key ASC) AS rn
            FROM song_stats ss
        ),
        dur AS (
            SELECT ss.song_key, ROW_NUMBER() OVER (ORDER BY ss.total_listening_duration DESC, ss.song_key ASC) AS rn
            FROM song_stats ss
        )
        UPDATE song_rankings 
        SET play_rank = (SELECT rn FROM play WHERE play.song_key = song_rankings.song_key),
            duration_rank = (SELECT rn FROM dur WHERE dur.song_key = song_rankings.song_key)
        WHERE ranking_type = 'overall'
    """)
    suspend fun updateGlobalPlayDurationRanks()

    @Query("""
        WITH ranked AS (
            SELECT song_key, ROW_NUMBER() OVER (ORDER BY overall_score DESC, song_key ASC) AS rn
            FROM song_rankings WHERE ranking_type = 'overall'
        )
        UPDATE song_rankings SET overall_rank = (SELECT rn FROM ranked WHERE ranked.song_key = song_rankings.song_key), last_updated = :timestamp
        WHERE ranking_type = 'overall'
    """)
    suspend fun updateGlobalSongRanks(timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT ss.* FROM song_stats ss
        INNER JOIN songs s ON ss.song_key = s.song_key
        WHERE s.media_store_id = :mediaStoreId
    """)
    suspend fun getSongStatsByMediaStoreId(mediaStoreId: Long): SongStatsEntity?

    @Query("""
        SELECT sr.* FROM song_rankings sr
        INNER JOIN songs s ON sr.song_key = s.song_key
        WHERE s.media_store_id = :mediaStoreId AND sr.ranking_type = :type
    """)
    suspend fun getSongRankingByMediaStoreId(mediaStoreId: Long, type: String): SongRankingEntity?

    @Query("""
        UPDATE moods 
        SET 
            song_count = COALESCE((SELECT COUNT(song_id) FROM song_mood_relationship WHERE mood_id = moods.id), 0),
            play_count = COALESCE((SELECT SUM(play_count) FROM mood_song_ranking WHERE mood_id = moods.id), 0),
            listening_duration = COALESCE((SELECT SUM(listened_duration_ms) FROM mood_song_ranking WHERE mood_id = moods.id), 0),
            overall_score = COALESCE((SELECT SUM(combined_score) FROM mood_song_ranking WHERE mood_id = moods.id), 0.0),
            updated_at = :timestamp
    """)
    suspend fun updateMoodStats(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH ranked AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY overall_score DESC, id ASC) AS rn FROM moods
        )
        UPDATE moods SET overall_rank = (SELECT rn FROM ranked WHERE ranked.id = moods.id)
    """)
    suspend fun updateMoodGlobalRanks()

    @Query("""
        WITH play AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY play_count DESC, id ASC) AS rn FROM moods
        ),
        dur AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY listening_duration DESC, id ASC) AS rn FROM moods
        )
        UPDATE moods 
        SET play_rank = (SELECT rn FROM play WHERE play.id = moods.id),
            duration_rank = (SELECT rn FROM dur WHERE dur.id = moods.id)
    """)
    suspend fun updateMoodPlayDurationRanks()

    @Query("""
        UPDATE artists 
        SET 
            song_count = COALESCE((SELECT COUNT(*) FROM songs s WHERE UPPER(s.artist_name) = UPPER(artists.name)), 0),
            total_duration = COALESCE((SELECT COALESCE(SUM(s.duration), 0) FROM songs s WHERE UPPER(s.artist_name) = UPPER(artists.name)), 0),
            play_count = COALESCE((SELECT SUM(total_play_count) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.artist_name) = UPPER(artists.name)), 0),
            listening_duration = COALESCE((SELECT SUM(total_listening_duration) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.artist_name) = UPPER(artists.name)), 0),
            overall_score = COALESCE((SELECT SUM(COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM song_metadata sm WHERE sm.song_key = ss.song_key), 0) + COALESCE(ss.completed_play_count, 0) * 10 + COALESCE(ss.total_play_count, 0) * 5 + COALESCE(CAST(ss.total_listening_duration AS REAL) / 60000.0, 0.0) * 5) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.artist_name) = UPPER(artists.name)), 0.0),
            last_updated = :timestamp
    """)
    suspend fun updateArtistStats(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH ranked AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY overall_score DESC, id ASC) AS rn FROM artists
        )
        UPDATE artists SET overall_rank = (SELECT rn FROM ranked WHERE ranked.id = artists.id)
    """)
    suspend fun updateArtistGlobalRanks()

    @Query("""
        WITH play AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY play_count DESC, id ASC) AS rn FROM artists
        ),
        dur AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY listening_duration DESC, id ASC) AS rn FROM artists
        )
        UPDATE artists 
        SET play_rank = (SELECT rn FROM play WHERE play.id = artists.id),
            duration_rank = (SELECT rn FROM dur WHERE dur.id = artists.id)
    """)
    suspend fun updateArtistPlayDurationRanks()

    @Query("""
        UPDATE albums 
        SET 
            song_count = COALESCE((SELECT COUNT(*) FROM songs s WHERE UPPER(s.album_name) = UPPER(albums.name) AND UPPER(IFNULL(s.album_artist, '')) = UPPER(IFNULL(albums.album_artist, ''))), 0),
            total_duration = COALESCE((SELECT COALESCE(SUM(s.duration), 0) FROM songs s WHERE UPPER(s.album_name) = UPPER(albums.name) AND UPPER(IFNULL(s.album_artist, '')) = UPPER(IFNULL(albums.album_artist, ''))), 0),
            play_count = COALESCE((SELECT SUM(total_play_count) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.album_name) = UPPER(albums.name) AND UPPER(IFNULL(s.album_artist, '')) = UPPER(IFNULL(albums.album_artist, ''))), 0),
            listening_duration = COALESCE((SELECT SUM(total_listening_duration) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.album_name) = UPPER(albums.name) AND UPPER(IFNULL(s.album_artist, '')) = UPPER(IFNULL(albums.album_artist, ''))), 0),
            overall_score = COALESCE((SELECT SUM(COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM song_metadata sm WHERE sm.song_key = ss.song_key), 0) + COALESCE(ss.completed_play_count, 0) * 10 + COALESCE(ss.total_play_count, 0) * 5 + COALESCE(CAST(ss.total_listening_duration AS REAL) / 60000.0, 0.0) * 5) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.album_name) = UPPER(albums.name) AND UPPER(IFNULL(s.album_artist, '')) = UPPER(IFNULL(albums.album_artist, ''))), 0.0),
            last_updated = :timestamp
    """)
    suspend fun updateAlbumStats(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH ranked AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY overall_score DESC, id ASC) AS rn FROM albums
        )
        UPDATE albums SET overall_rank = (SELECT rn FROM ranked WHERE ranked.id = albums.id)
    """)
    suspend fun updateAlbumGlobalRanks()

    @Query("""
        WITH play AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY play_count DESC, id ASC) AS rn FROM albums
        ),
        dur AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY listening_duration DESC, id ASC) AS rn FROM albums
        )
        UPDATE albums 
        SET play_rank = (SELECT rn FROM play WHERE play.id = albums.id),
            duration_rank = (SELECT rn FROM dur WHERE dur.id = albums.id)
    """)
    suspend fun updateAlbumPlayDurationRanks()

    @Query("SELECT * FROM albums ORDER BY name ASC")
    suspend fun getAllAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM artists ORDER BY name ASC")
    suspend fun getAllArtists(): List<ArtistEntity>

    @Query("SELECT * FROM album_artists ORDER BY name ASC")
    suspend fun getAllAlbumArtists(): List<AlbumArtistEntity>

    @Query("""
        UPDATE genres 
        SET 
            song_count = COALESCE((SELECT COUNT(song_id) FROM song_genre_relationship WHERE genre_id = genres.id), 0),
            play_count = COALESCE((SELECT SUM(play_count) FROM genre_song_ranking WHERE genre_id = genres.id), 0),
            listening_duration = COALESCE((SELECT SUM(listened_duration_ms) FROM genre_song_ranking WHERE genre_id = genres.id), 0),
            overall_score = COALESCE((SELECT SUM(combined_score) FROM genre_song_ranking WHERE genre_id = genres.id), 0.0),
            updated_at = :timestamp
    """)
    suspend fun updateGenreStats(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH ranked AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY overall_score DESC, id ASC) AS rn FROM genres
        )
        UPDATE genres SET overall_rank = (SELECT rn FROM ranked WHERE ranked.id = genres.id)
    """)
    suspend fun updateGenreGlobalRanks()

    @Query("""
        WITH play AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY play_count DESC, id ASC) AS rn FROM genres
        ),
        dur AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY listening_duration DESC, id ASC) AS rn FROM genres
        )
        UPDATE genres 
        SET play_rank = (SELECT rn FROM play WHERE play.id = genres.id),
            duration_rank = (SELECT rn FROM dur WHERE dur.id = genres.id)
    """)
    suspend fun updateGenrePlayDurationRanks()

    // ─── Album Artists Pipeline ──────────────────────────────────────────

    @Query("""
        UPDATE album_artists 
        SET 
            play_count = COALESCE((SELECT SUM(ss.total_play_count) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.album_artist) = UPPER(album_artists.name)), 0),
            listening_duration = COALESCE((SELECT SUM(ss.total_listening_duration) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.album_artist) = UPPER(album_artists.name)), 0),
            overall_score = COALESCE((SELECT SUM(COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM song_metadata sm WHERE sm.song_key = ss.song_key), 0) + COALESCE(ss.completed_play_count, 0) * 10 + COALESCE(ss.total_play_count, 0) * 5 + COALESCE(CAST(ss.total_listening_duration AS REAL) / 60000.0, 0.0) * 5) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key WHERE UPPER(s.album_artist) = UPPER(album_artists.name)), 0.0),
            last_updated = :timestamp
    """)
    suspend fun updateAlbumArtistStats(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH ranked AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY overall_score DESC, id ASC) AS rn FROM album_artists
        )
        UPDATE album_artists SET overall_rank = (SELECT rn FROM ranked WHERE ranked.id = album_artists.id)
    """)
    suspend fun updateAlbumArtistGlobalRanks()

    @Query("""
        WITH play AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY play_count DESC, id ASC) AS rn FROM album_artists
        ),
        dur AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY listening_duration DESC, id ASC) AS rn FROM album_artists
        )
        UPDATE album_artists 
        SET play_rank = (SELECT rn FROM play WHERE play.id = album_artists.id),
            duration_rank = (SELECT rn FROM dur WHERE dur.id = album_artists.id)
    """)
    suspend fun updateAlbumArtistPlayDurationRanks()

    // ─── Playlist Pipeline ──────────────────────────────────────────────

    @Query("""
        UPDATE playlists 
        SET 
            song_count = COALESCE((SELECT COUNT(*) FROM song_playlist_relationship WHERE playlist_id = playlists.playlist_id), 0),
            total_duration = COALESCE((SELECT SUM(s.duration) FROM songs s INNER JOIN song_playlist_relationship spr ON s.media_store_id = spr.song_id WHERE spr.playlist_id = playlists.playlist_id), 0),
            play_count = COALESCE((SELECT SUM(ss.total_play_count) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key INNER JOIN song_playlist_relationship spr ON s.media_store_id = spr.song_id WHERE spr.playlist_id = playlists.playlist_id), 0),
            listening_duration = COALESCE((SELECT SUM(ss.total_listening_duration) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key INNER JOIN song_playlist_relationship spr ON s.media_store_id = spr.song_id WHERE spr.playlist_id = playlists.playlist_id), 0),
            overall_score = COALESCE((SELECT SUM(COALESCE((SELECT CAST(CASE WHEN sm.is_favorite THEN 1 ELSE 0 END AS INTEGER) * 25 FROM song_metadata sm WHERE sm.song_key = ss.song_key), 0) + COALESCE(ss.completed_play_count, 0) * 10 + COALESCE(ss.total_play_count, 0) * 5 + COALESCE(CAST(ss.total_listening_duration AS REAL) / 60000.0, 0.0) * 5) FROM song_stats ss INNER JOIN songs s ON ss.song_key = s.song_key INNER JOIN song_playlist_relationship spr ON s.media_store_id = spr.song_id WHERE spr.playlist_id = playlists.playlist_id), 0.0),
            last_updated = :timestamp
    """)
    suspend fun updatePlaylistStats(timestamp: Long = System.currentTimeMillis())

    @Query("""
        WITH ranked AS (
            SELECT playlist_id, ROW_NUMBER() OVER (ORDER BY overall_score DESC, playlist_id ASC) AS rn FROM playlists
        )
        UPDATE playlists SET overall_rank = (SELECT rn FROM ranked WHERE ranked.playlist_id = playlists.playlist_id)
    """)
    suspend fun updatePlaylistGlobalRanks()

    @Query("""
        WITH play AS (
            SELECT playlist_id, ROW_NUMBER() OVER (ORDER BY play_count DESC, playlist_id ASC) AS rn FROM playlists
        ),
        dur AS (
            SELECT playlist_id, ROW_NUMBER() OVER (ORDER BY listening_duration DESC, playlist_id ASC) AS rn FROM playlists
        )
        UPDATE playlists 
        SET play_rank = (SELECT rn FROM play WHERE play.playlist_id = playlists.playlist_id),
            duration_rank = (SELECT rn FROM dur WHERE dur.playlist_id = playlists.playlist_id)
    """)
    suspend fun updatePlaylistPlayDurationRanks()

    @Query("SELECT playlist_id FROM playlists")
    suspend fun getAllPlaylistIds(): List<Long>

    // ─── Alphabetical Initialization ─────────────────────────────────────

    @Query("""
        INSERT OR IGNORE INTO song_rankings (song_key, ranking_type, play_rank, duration_rank, overall_rank, overall_score, last_updated, ranking_version)
        SELECT s.song_key, 'overall', 0, 0, 0, 0.0, :timestamp, 1
        FROM songs s
        LEFT JOIN song_rankings sr ON s.song_key = sr.song_key AND sr.ranking_type = 'overall'
        WHERE sr.song_key IS NULL
    """)
    suspend fun ensureSongRankingsExist(timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun initializeAllAlphabeticalRanks() {
        ensureSongRankingsExist()
        initializeSongAlphabeticalRanks()
        initializeArtistAlphabeticalRanks()
        initializeAlbumAlphabeticalRanks()
        initializeGenreAlphabeticalRanks()
        initializeMoodAlphabeticalRanks()
        initializeTagAlphabeticalRanks()
        initializeInstrumentAlphabeticalRanks()
        initializeAlbumArtistAlphabeticalRanks()
        initializePlaylistAlphabeticalRanks()
    }

    @Query("""
        UPDATE song_rankings 
        SET overall_rank = (
            SELECT COUNT(*) + 1 FROM songs s2 
            INNER JOIN song_rankings sr2 ON s2.song_key = sr2.song_key
            WHERE sr2.ranking_type = 'overall' AND (s2.title < (SELECT s.title FROM songs s WHERE s.song_key = song_rankings.song_key) OR (s2.title = (SELECT s.title FROM songs s WHERE s.song_key = song_rankings.song_key) AND s2.song_key < song_rankings.song_key))
        ),
        play_rank = (
            SELECT COUNT(*) + 1 FROM songs s2 
            INNER JOIN song_rankings sr2 ON s2.song_key = sr2.song_key
            WHERE sr2.ranking_type = 'overall' AND (s2.title < (SELECT s.title FROM songs s WHERE s.song_key = song_rankings.song_key) OR (s2.title = (SELECT s.title FROM songs s WHERE s.song_key = song_rankings.song_key) AND s2.song_key < song_rankings.song_key))
        ),
        duration_rank = (
            SELECT COUNT(*) + 1 FROM songs s2 
            INNER JOIN song_rankings sr2 ON s2.song_key = sr2.song_key
            WHERE sr2.ranking_type = 'overall' AND (s2.title < (SELECT s.title FROM songs s WHERE s.song_key = song_rankings.song_key) OR (s2.title = (SELECT s.title FROM songs s WHERE s.song_key = song_rankings.song_key) AND s2.song_key < song_rankings.song_key))
        ),
        overall_score = CAST(overall_rank AS REAL)
        WHERE ranking_type = 'overall'
    """)
    suspend fun initializeSongAlphabeticalRanks()

    @Query("""
        UPDATE artists 
        SET overall_rank = (SELECT COUNT(*) + 1 FROM artists a2 WHERE a2.name < artists.name OR (a2.name = artists.name AND a2.id < artists.id)),
            play_rank = (SELECT COUNT(*) + 1 FROM artists a2 WHERE a2.name < artists.name OR (a2.name = artists.name AND a2.id < artists.id)),
            duration_rank = (SELECT COUNT(*) + 1 FROM artists a2 WHERE a2.name < artists.name OR (a2.name = artists.name AND a2.id < artists.id)),
            overall_score = CAST(overall_rank AS REAL)
    """)
    suspend fun initializeArtistAlphabeticalRanks()

    @Query("""
        UPDATE albums 
        SET overall_rank = (SELECT COUNT(*) + 1 FROM albums a2 WHERE a2.name < albums.name OR (a2.name = albums.name AND IFNULL(a2.album_artist, '') < IFNULL(albums.album_artist, '')) OR (a2.name = albums.name AND IFNULL(a2.album_artist, '') = IFNULL(albums.album_artist, '') AND a2.id < albums.id)),
            play_rank = (SELECT COUNT(*) + 1 FROM albums a2 WHERE a2.name < albums.name OR (a2.name = albums.name AND IFNULL(a2.album_artist, '') < IFNULL(albums.album_artist, '')) OR (a2.name = albums.name AND IFNULL(a2.album_artist, '') = IFNULL(albums.album_artist, '') AND a2.id < albums.id)),
            duration_rank = (SELECT COUNT(*) + 1 FROM albums a2 WHERE a2.name < albums.name OR (a2.name = albums.name AND IFNULL(a2.album_artist, '') < IFNULL(albums.album_artist, '')) OR (a2.name = albums.name AND IFNULL(a2.album_artist, '') = IFNULL(albums.album_artist, '') AND a2.id < albums.id)),
            overall_score = CAST(overall_rank AS REAL)
    """)
    suspend fun initializeAlbumAlphabeticalRanks()

    @Query("""
        UPDATE genres 
        SET overall_rank = (SELECT COUNT(*) + 1 FROM genres g2 WHERE g2.name < genres.name OR (g2.name = genres.name AND g2.id < genres.id)),
            play_rank = (SELECT COUNT(*) + 1 FROM genres g2 WHERE g2.name < genres.name OR (g2.name = genres.name AND g2.id < genres.id)),
            duration_rank = (SELECT COUNT(*) + 1 FROM genres g2 WHERE g2.name < genres.name OR (g2.name = genres.name AND g2.id < genres.id)),
            overall_score = CAST(overall_rank AS REAL)
    """)
    suspend fun initializeGenreAlphabeticalRanks()

    @Query("""
        UPDATE moods 
        SET overall_rank = (SELECT COUNT(*) + 1 FROM moods m2 WHERE m2.name < moods.name OR (m2.name = moods.name AND m2.id < moods.id)),
            play_rank = (SELECT COUNT(*) + 1 FROM moods m2 WHERE m2.name < moods.name OR (m2.name = moods.name AND m2.id < moods.id)),
            duration_rank = (SELECT COUNT(*) + 1 FROM moods m2 WHERE m2.name < moods.name OR (m2.name = moods.name AND m2.id < moods.id)),
            overall_score = CAST(overall_rank AS REAL)
    """)
    suspend fun initializeMoodAlphabeticalRanks()

    @Query("""
        UPDATE tags 
        SET overall_rank = (SELECT COUNT(*) + 1 FROM tags t2 WHERE t2.name < tags.name OR (t2.name = tags.name AND t2.id < tags.id)),
            play_rank = (SELECT COUNT(*) + 1 FROM tags t2 WHERE t2.name < tags.name OR (t2.name = tags.name AND t2.id < tags.id)),
            duration_rank = (SELECT COUNT(*) + 1 FROM tags t2 WHERE t2.name < tags.name OR (t2.name = tags.name AND t2.id < tags.id)),
            overall_score = CAST(overall_rank AS REAL)
    """)
    suspend fun initializeTagAlphabeticalRanks()

    @Query("""
        UPDATE instruments 
        SET overall_rank = (SELECT COUNT(*) + 1 FROM instruments i2 WHERE i2.name < instruments.name OR (i2.name = instruments.name AND i2.id < instruments.id)),
            play_rank = (SELECT COUNT(*) + 1 FROM instruments i2 WHERE i2.name < instruments.name OR (i2.name = instruments.name AND i2.id < instruments.id)),
            duration_rank = (SELECT COUNT(*) + 1 FROM instruments i2 WHERE i2.name < instruments.name OR (i2.name = instruments.name AND i2.id < instruments.id)),
            overall_score = CAST(overall_rank AS REAL)
    """)
    suspend fun initializeInstrumentAlphabeticalRanks()

    @Query("""
        UPDATE album_artists 
        SET overall_rank = (SELECT COUNT(*) + 1 FROM album_artists a2 WHERE IFNULL(a2.name, '') < IFNULL(album_artists.name, '') OR (IFNULL(a2.name, '') = IFNULL(album_artists.name, '') AND a2.id < album_artists.id)),
            play_rank = (SELECT COUNT(*) + 1 FROM album_artists a2 WHERE IFNULL(a2.name, '') < IFNULL(album_artists.name, '') OR (IFNULL(a2.name, '') = IFNULL(album_artists.name, '') AND a2.id < album_artists.id)),
            duration_rank = (SELECT COUNT(*) + 1 FROM album_artists a2 WHERE IFNULL(a2.name, '') < IFNULL(album_artists.name, '') OR (IFNULL(a2.name, '') = IFNULL(album_artists.name, '') AND a2.id < album_artists.id)),
            overall_score = CAST(overall_rank AS REAL)
    """)
    suspend fun initializeAlbumArtistAlphabeticalRanks()

    @Query("""
        UPDATE playlists 
        SET overall_rank = (SELECT COUNT(*) + 1 FROM playlists p2 WHERE p2.playlist_name < playlists.playlist_name OR (p2.playlist_name = playlists.playlist_name AND p2.playlist_id < playlists.playlist_id)),
            play_rank = (SELECT COUNT(*) + 1 FROM playlists p2 WHERE p2.playlist_name < playlists.playlist_name OR (p2.playlist_name = playlists.playlist_name AND p2.playlist_id < playlists.playlist_id)),
            duration_rank = (SELECT COUNT(*) + 1 FROM playlists p2 WHERE p2.playlist_name < playlists.playlist_name OR (p2.playlist_name = playlists.playlist_name AND p2.playlist_id < playlists.playlist_id)),
            overall_score = CAST(overall_rank AS REAL)
    """)
    suspend fun initializePlaylistAlphabeticalRanks()

    // ─── Song Deletion Cleanup ──────────────────────────────────────────

    @Query("DELETE FROM genre_song_ranking WHERE song_id = :songId")
    suspend fun deleteSongFromGenreRankings(songId: Long)

    @Query("DELETE FROM mood_song_ranking WHERE song_id = :songId")
    suspend fun deleteSongFromMoodRankings(songId: Long)

    @Query("DELETE FROM tag_song_ranking WHERE song_id = :songId")
    suspend fun deleteSongFromTagRankings(songId: Long)

    @Query("DELETE FROM instrument_song_ranking WHERE song_id = :songId")
    suspend fun deleteSongFromInstrumentRankings(songId: Long)

    @Query("DELETE FROM song_stats WHERE song_key = :songKey")
    suspend fun deleteSongStats(songKey: Long)

    @Query("DELETE FROM song_rankings WHERE song_key = :songKey")
    suspend fun deleteSongRankings(songKey: Long)

    @Query("DELETE FROM song_metadata WHERE song_key = :songKey")
    suspend fun deleteSongMetadata(songKey: Long)

    @Query("DELETE FROM songs WHERE media_store_id = :mediaStoreId")
    suspend fun deleteSongFromSongsTable(mediaStoreId: Long)

    @Query("DELETE FROM song_genre_relationship WHERE song_id = :mediaStoreId")
    suspend fun deleteSongGenreRelationships(mediaStoreId: Long)

    @Query("DELETE FROM song_mood_relationship WHERE song_id = :mediaStoreId")
    suspend fun deleteSongMoodRelationships(mediaStoreId: Long)

    @Query("DELETE FROM song_tag_relationship WHERE song_id = :mediaStoreId")
    suspend fun deleteSongTagRelationships(mediaStoreId: Long)

    @Query("DELETE FROM song_instrument_relationship WHERE song_id = :mediaStoreId")
    suspend fun deleteSongInstrumentRelationships(mediaStoreId: Long)

    @Query("SELECT song_key FROM songs WHERE media_store_id = :mediaStoreId")
    suspend fun getSongKey(mediaStoreId: Long): Long?

    @Transaction
    suspend fun deleteSongCompletely(mediaStoreId: Long) {
        val songKey = getSongKey(mediaStoreId)
        if (songKey != null) {
            deleteSongStats(songKey)
            deleteSongRankings(songKey)
            deleteSongMetadata(songKey)
        }
        deleteSongFromGenreRankings(mediaStoreId)
        deleteSongFromMoodRankings(mediaStoreId)
        deleteSongFromTagRankings(mediaStoreId)
        deleteSongFromInstrumentRankings(mediaStoreId)
        deleteSongGenreRelationships(mediaStoreId)
        deleteSongMoodRelationships(mediaStoreId)
        deleteSongTagRelationships(mediaStoreId)
        deleteSongInstrumentRelationships(mediaStoreId)
        deleteSongFromSongsTable(mediaStoreId)
    }

    @Transaction
    suspend fun deleteSongsCompletely(mediaStoreIds: List<Long>) {
        for (id in mediaStoreIds) {
            deleteSongCompletely(id)
        }
    }

    // ─── Validation Queries ────────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM song_rankings sr 
        LEFT JOIN song_stats ss ON sr.song_key = ss.song_key 
        WHERE sr.ranking_type = 'overall' AND ss.song_key IS NULL
    """)
    suspend fun countOrphanedSongRankings(): Int

    @Query("""
        SELECT COUNT(*) FROM genre_song_ranking gsr 
        LEFT JOIN songs s ON gsr.song_id = s.media_store_id 
        WHERE s.song_key IS NULL
    """)
    suspend fun countOrphanedGenreRankings(): Int

    @Query("""
        SELECT COUNT(*) FROM mood_song_ranking msr 
        LEFT JOIN songs s ON msr.song_id = s.media_store_id 
        WHERE s.song_key IS NULL
    """)
    suspend fun countOrphanedMoodRankings(): Int

    @Query("""
        SELECT COUNT(*) FROM tag_song_ranking tsr 
        LEFT JOIN songs s ON tsr.song_id = s.media_store_id 
        WHERE s.song_key IS NULL
    """)
    suspend fun countOrphanedTagRankings(): Int

    @Query("""
        SELECT COUNT(*) FROM instrument_song_ranking isr 
        LEFT JOIN songs s ON isr.song_id = s.media_store_id 
        WHERE s.song_key IS NULL
    """)
    suspend fun countOrphanedInstrumentRankings(): Int

    @Query("""
        SELECT COUNT(*) FROM song_stats ss 
        LEFT JOIN songs s ON ss.song_key = s.song_key 
        WHERE s.song_key IS NULL
    """)
    suspend fun countOrphanedSongStats(): Int
}
