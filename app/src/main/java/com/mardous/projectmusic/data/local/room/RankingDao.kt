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

package com.mardous.projectmusic.data.local.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RankingDao {

    // --- Genre Rankings ---
    @Upsert
    suspend fun upsertGenreRankings(rankings: List<GenreSongRankingEntity>)

    @Query("DELETE FROM genre_song_ranking WHERE genre_id = :genreId")
    suspend fun clearGenreRankings(genreId: Long)

    @Query("""
        SELECT s.* FROM SongEntity s
        INNER JOIN genre_song_ranking gsr ON s.song_key = gsr.song_id
        WHERE gsr.genre_id = :genreId
        ORDER BY gsr.combined_rank ASC
    """)
    fun getTopSongsByGenre(genreId: Long): Flow<List<SongEntity>>

    // --- Mood Rankings ---
    @Upsert
    suspend fun upsertMoodRankings(rankings: List<MoodSongRankingEntity>)

    @Query("DELETE FROM mood_song_ranking WHERE mood_id = :moodId")
    suspend fun clearMoodRankings(moodId: Long)

    @Query("""
        SELECT s.* FROM SongEntity s
        INNER JOIN mood_song_ranking msr ON s.song_key = msr.song_id
        WHERE msr.mood_id = :moodId
        ORDER BY msr.combined_rank ASC
    """)
    fun getTopSongsByMood(moodId: Long): Flow<List<SongEntity>>

    // --- Tag Rankings ---
    @Upsert
    suspend fun upsertTagRankings(rankings: List<TagSongRankingEntity>)

    @Query("DELETE FROM tag_song_ranking WHERE tag_id = :tagId")
    suspend fun clearTagRankings(tagId: Long)

    @Query("""
        SELECT s.* FROM SongEntity s
        INNER JOIN tag_song_ranking tsr ON s.song_key = tsr.song_id
        WHERE tsr.tag_id = :tagId
        ORDER BY tsr.combined_rank ASC
    """)
    fun getTopSongsByTag(tagId: Long): Flow<List<SongEntity>>

    // --- Instrument Rankings ---
    @Upsert
    suspend fun upsertInstrumentRankings(rankings: List<InstrumentSongRankingEntity>)

    @Query("DELETE FROM instrument_song_ranking WHERE instrument_id = :instrumentId")
    suspend fun clearInstrumentRankings(instrumentId: Long)

    @Query("""
        SELECT s.* FROM SongEntity s
        INNER JOIN instrument_song_ranking isr ON s.song_key = isr.song_id
        WHERE isr.instrument_id = :instrumentId
        ORDER BY isr.combined_rank ASC
    """)
    fun getTopSongsByInstrument(instrumentId: Long): Flow<List<SongEntity>>

    // --- Combined dimension rankings ---
    @Query("SELECT * FROM genre_song_ranking WHERE song_id = :songId AND genre_id = :genreId")
    suspend fun getGenreRanking(songId: Long, genreId: Long): GenreSongRankingEntity?

    @Query("SELECT * FROM mood_song_ranking WHERE song_id = :songId AND mood_id = :moodId")
    suspend fun getMoodRanking(songId: Long, moodId: Long): MoodSongRankingEntity?
}
