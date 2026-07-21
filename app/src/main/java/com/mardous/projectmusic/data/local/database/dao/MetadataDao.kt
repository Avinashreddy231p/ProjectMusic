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
import com.mardous.projectmusic.data.local.database.metadata.*
import com.mardous.projectmusic.data.local.database.core.SongMetadataEntity
import com.mardous.projectmusic.data.local.database.intel.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Dao
interface MetadataDao {

    // --- GenreEntity CRUD ---
    @Upsert
    suspend fun upsertGenre(genre: GenreEntity): Long

    @Delete
    suspend fun deleteGenre(genre: GenreEntity)

    @Query("SELECT * FROM genres ORDER BY name ASC")
    fun getAllGenres(): Flow<List<GenreEntity>>

    @Query("SELECT * FROM genres WHERE id = :genreId")
    suspend fun getGenreById(genreId: Long): GenreEntity?

    // --- MoodEntity CRUD ---
    @Upsert
    suspend fun upsertMood(mood: MoodEntity): Long

    @Delete
    suspend fun deleteMood(mood: MoodEntity)

    @Query("SELECT * FROM moods ORDER BY name ASC")
    fun getAllMoods(): Flow<List<MoodEntity>>

    @Query("SELECT * FROM moods WHERE id = :moodId")
    suspend fun getMoodById(moodId: Long): MoodEntity?

    @Query("SELECT * FROM moods WHERE name = :name")
    suspend fun getMoodByName(name: String): MoodEntity?

    @Update
    suspend fun updateMood(mood: MoodEntity)

    // --- TagEntity CRUD ---
    @Upsert
    suspend fun upsertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name AND category = :category")
    suspend fun getTagByName(name: String, category: Int): TagEntity?

    @Update
    suspend fun updateTag(tag: TagEntity)

    // --- InstrumentEntity CRUD ---
    @Upsert
    suspend fun upsertInstrument(instrument: InstrumentEntity): Long

    @Delete
    suspend fun deleteInstrument(instrument: InstrumentEntity)

    @Query("SELECT * FROM instruments ORDER BY name ASC")
    fun getAllInstruments(): Flow<List<InstrumentEntity>>

    @Query("SELECT * FROM instruments WHERE id = :instrumentId")
    suspend fun getInstrumentById(instrumentId: Long): InstrumentEntity?

    @Query("SELECT * FROM instruments WHERE name = :name AND family = :family")
    suspend fun getInstrumentByName(name: String, family: Int): InstrumentEntity?

    @Update
    suspend fun updateInstrument(instrument: InstrumentEntity)

    @Upsert
    suspend fun upsertSongs(songs: List<com.mardous.projectmusic.data.local.database.core.SongEntity>)

    @Upsert
    suspend fun upsertSongMetadata(metadata: SongMetadataEntity)

    @Query("SELECT m.*, s.data FROM song_metadata m INNER JOIN songs s ON m.song_key = s.song_key")
    suspend fun getAllMetadataWithPaths(): List<MetadataWithPath>

    // --- ArtistEntity CRUD ---
    @Upsert
    suspend fun upsertArtist(artist: ArtistEntity): Long

    @Query("SELECT * FROM artists WHERE id = :artistId")
    suspend fun getArtistById(artistId: Long): ArtistEntity?

    @Query("SELECT * FROM artists WHERE name = :name")
    suspend fun getArtistByName(name: String): ArtistEntity?

    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists ORDER BY name ASC")
    suspend fun getAllArtistsList(): List<ArtistEntity>

    // --- AlbumArtistEntity CRUD ---
    @Upsert
    suspend fun upsertAlbumArtist(albumArtist: AlbumArtistEntity): Long

    @Query("SELECT * FROM album_artists WHERE name = :name")
    suspend fun getAlbumArtistByName(name: String): AlbumArtistEntity?

    @Query("SELECT * FROM album_artists ORDER BY name ASC")
    suspend fun getAllAlbumArtistsList(): List<AlbumArtistEntity>

    // --- AlbumEntity CRUD ---
    @Upsert
    suspend fun upsertAlbum(album: AlbumEntity): Long

    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: Long): AlbumEntity?

    @Query("SELECT * FROM albums WHERE name = :name AND IFNULL(album_artist, '') = IFNULL(:artist, '')")
    suspend fun getAlbumByNameAndArtist(name: String, artist: String?): AlbumEntity?

    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    // --- Relationship Entities CRUD ---
    @Upsert
    suspend fun upsertSongGenre(songGenre: SongGenreEntity)

    @Delete
    suspend fun deleteSongGenre(songGenre: SongGenreEntity)

    @Upsert
    suspend fun upsertSongMood(songMood: SongMoodEntity)

    @Delete
    suspend fun deleteSongMood(songMood: SongMoodEntity)

    @Upsert
    suspend fun upsertSongInstrument(songInstrument: SongInstrumentEntity)

    @Delete
    suspend fun deleteSongInstrument(songInstrument: SongInstrumentEntity)

    @Upsert
    suspend fun upsertSongTag(songTag: SongTagEntity)

    @Delete
    suspend fun deleteSongTag(songTag: SongTagEntity)

    @Upsert
    suspend fun upsertSongMoods(songMoods: List<SongMoodEntity>)

    @Upsert
    suspend fun upsertSongTags(songTags: List<SongTagEntity>)

    @Upsert
    suspend fun upsertSongInstruments(songInstruments: List<SongInstrumentEntity>)

    // --- Specialized Queries ---
    @Query("""
        SELECT m.* FROM moods m
        INNER JOIN song_mood_relationship smr ON m.id = smr.mood_id
        WHERE smr.song_id = :songId
        ORDER BY smr.confidence_score DESC
    """)
    fun getMoodsForSong(songId: Long): Flow<List<MoodEntity>>

    @Query("""
        SELECT g.* FROM genres g
        INNER JOIN song_genre_relationship sgr ON g.id = sgr.genre_id
        WHERE sgr.song_id = :songId
        ORDER BY sgr.confidence_score DESC
    """)
    fun getGenresForSong(songId: Long): Flow<List<GenreEntity>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN song_tag_relationship str ON t.id = str.tag_id
        WHERE str.song_id = :songId
    """)
    fun getTagsForSong(songId: Long): Flow<List<TagEntity>>

    @Query("""
        SELECT i.* FROM instruments i
        INNER JOIN song_instrument_relationship sir ON i.id = sir.instrument_id
        WHERE sir.song_id = :songId
        ORDER BY sir.confidence_score DESC
    """)
    fun getInstrumentsForSong(songId: Long): Flow<List<InstrumentEntity>>

    @Query("DELETE FROM song_genre_relationship WHERE song_id = :songId")
    suspend fun clearGenresForSong(songId: Long)

    @Query("DELETE FROM song_mood_relationship WHERE song_id = :songId")
    suspend fun clearMoodsForSong(songId: Long)

    @Query("DELETE FROM song_instrument_relationship WHERE song_id = :songId")
    suspend fun clearInstrumentsForSong(songId: Long)

    @Query("DELETE FROM song_tag_relationship WHERE song_id = :songId")
    suspend fun clearTagsForSong(songId: Long)

    @Query("SELECT song_id FROM song_mood_relationship WHERE mood_id = :moodId")
    fun getSongIdsForMood(moodId: Long): Flow<List<Long>>

    @Query("SELECT song_id FROM song_tag_relationship WHERE tag_id = :tagId")
    fun getSongIdsForTag(tagId: Long): Flow<List<Long>>

    @Query("SELECT song_id FROM song_instrument_relationship WHERE instrument_id = :instrumentId")
    fun getSongIdsForInstrument(instrumentId: Long): Flow<List<Long>>

    // --- Stats Queries ---
    @Query("""
        SELECT m.*, 
               COUNT(smr.song_id) as calculated_song_count,
               SUM(COALESCE(msr.play_count, 0)) as total_play_count,
               SUM(COALESCE(msr.listened_duration_ms, 0)) as total_duration_ms
        FROM moods m
        LEFT JOIN song_mood_relationship smr ON m.id = smr.mood_id
        LEFT JOIN mood_song_ranking msr ON m.id = msr.mood_id AND smr.song_id = msr.song_id
        GROUP BY m.id
    """)
    fun getAllMoodsWithStats(): Flow<List<MoodWithStatsEntity>>

    @Query("""
        SELECT t.*, 
               COUNT(str.song_id) as calculated_song_count,
               SUM(COALESCE(tsr.play_count, 0)) as total_play_count,
               SUM(COALESCE(tsr.listened_duration_ms, 0)) as total_duration_ms
        FROM tags t
        LEFT JOIN song_tag_relationship str ON t.id = str.tag_id
        LEFT JOIN tag_song_ranking tsr ON t.id = tsr.tag_id AND str.song_id = tsr.song_id
        GROUP BY t.id
    """)
    fun getAllTagsWithStats(): Flow<List<TagWithStatsEntity>>

    @Query("""
        SELECT i.*, 
               COUNT(sir.song_id) as calculated_song_count,
               SUM(COALESCE(isr.play_count, 0)) as total_play_count,
               SUM(COALESCE(isr.listened_duration_ms, 0)) as total_duration_ms
        FROM instruments i
        LEFT JOIN song_instrument_relationship sir ON i.id = sir.instrument_id
        LEFT JOIN instrument_song_ranking isr ON i.id = isr.instrument_id AND sir.song_id = isr.song_id
        GROUP BY i.id
    """)
    fun getAllInstrumentsWithStats(): Flow<List<InstrumentWithStatsEntity>>

    // --- Detail Screen Stats Queries ---

    // Mood Stats
    @Query("""
        SELECT ls.artist_name AS artistName, SUM(ls.playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount
        FROM listening_sessions ls
        INNER JOIN song_mood_relationship smr ON ls.song_id = smr.song_id
        INNER JOIN mood_song_ranking msr ON smr.mood_id = msr.mood_id AND smr.song_id = msr.song_id
        WHERE smr.mood_id = :moodId
        GROUP BY ls.artist_name
        ORDER BY totalPlayed DESC
        LIMIT :limit
    """)
    fun getTopArtistsForMood(moodId: Long, limit: Int): Flow<List<ListeningHistoryDao.ArtistStats>>

    @Query("""
        SELECT ls.album_id AS albumId, ls.album_name AS albumName, ls.artist_name AS artistName, 
               SUM(ls.playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount
        FROM listening_sessions ls
        INNER JOIN song_mood_relationship smr ON ls.song_id = smr.song_id
        INNER JOIN mood_song_ranking msr ON smr.mood_id = msr.mood_id AND smr.song_id = msr.song_id
        WHERE smr.mood_id = :moodId
        GROUP BY ls.album_id
        ORDER BY totalPlayed DESC
        LIMIT :limit
    """)
    fun getTopAlbumsForMood(moodId: Long, limit: Int): Flow<List<ListeningHistoryDao.AlbumStats>>

    @Query("""
        SELECT ls.year_month AS month, SUM(ls.playback_duration_ms) AS totalTime
        FROM listening_sessions ls
        INNER JOIN song_mood_relationship smr ON ls.song_id = smr.song_id
        INNER JOIN mood_song_ranking msr ON smr.mood_id = msr.mood_id AND smr.song_id = msr.song_id
        WHERE smr.mood_id = :moodId
        GROUP BY ls.year_month
        ORDER BY ls.year_month ASC
    """)
    fun getListeningTrendForMood(moodId: Long): Flow<List<ListeningHistoryDao.TrendData>>

    // Tag Stats
    @Query("""
        SELECT ls.artist_name AS artistName, SUM(ls.playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount
        FROM listening_sessions ls
        INNER JOIN song_tag_relationship str ON ls.song_id = str.song_id
        INNER JOIN tag_song_ranking tsr ON str.tag_id = tsr.tag_id AND str.song_id = tsr.song_id
        WHERE str.tag_id = :tagId
        GROUP BY ls.artist_name
        ORDER BY totalPlayed DESC
        LIMIT :limit
    """)
    fun getTopArtistsForTag(tagId: Long, limit: Int): Flow<List<ListeningHistoryDao.ArtistStats>>

    @Query("""
        SELECT ls.album_id AS albumId, ls.album_name AS albumName, ls.artist_name AS artistName, 
               SUM(ls.playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount
        FROM listening_sessions ls
        INNER JOIN song_tag_relationship str ON ls.song_id = str.song_id
        INNER JOIN tag_song_ranking tsr ON str.tag_id = tsr.tag_id AND str.song_id = tsr.song_id
        WHERE str.tag_id = :tagId
        GROUP BY ls.album_id
        ORDER BY totalPlayed DESC
        LIMIT :limit
    """)
    fun getTopAlbumsForTag(tagId: Long, limit: Int): Flow<List<ListeningHistoryDao.AlbumStats>>

    @Query("""
        SELECT ls.year_month AS month, SUM(ls.playback_duration_ms) AS totalTime
        FROM listening_sessions ls
        INNER JOIN song_tag_relationship str ON ls.song_id = str.song_id
        INNER JOIN tag_song_ranking tsr ON str.tag_id = tsr.tag_id AND str.song_id = tsr.song_id
        WHERE str.tag_id = :tagId
        GROUP BY ls.year_month
        ORDER BY ls.year_month ASC
    """)
    fun getListeningTrendForTag(tagId: Long): Flow<List<ListeningHistoryDao.TrendData>>

    // Instrument Stats
    @Query("""
        SELECT ls.artist_name AS artistName, SUM(ls.playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount
        FROM listening_sessions ls
        INNER JOIN song_instrument_relationship sir ON ls.song_id = sir.song_id
        INNER JOIN instrument_song_ranking isr ON sir.instrument_id = isr.instrument_id AND sir.song_id = isr.song_id
        WHERE sir.instrument_id = :instrumentId
        GROUP BY ls.artist_name
        ORDER BY totalPlayed DESC
        LIMIT :limit
    """)
    fun getTopArtistsForInstrument(instrumentId: Long, limit: Int): Flow<List<ListeningHistoryDao.ArtistStats>>

    @Query("""
        SELECT ls.album_id AS albumId, ls.album_name AS albumName, ls.artist_name AS artistName, 
               SUM(ls.playback_duration_ms) AS totalPlayed, COUNT(*) AS playCount
        FROM listening_sessions ls
        INNER JOIN song_instrument_relationship sir ON ls.song_id = sir.song_id
        INNER JOIN instrument_song_ranking isr ON sir.instrument_id = isr.instrument_id AND sir.song_id = isr.song_id
        WHERE sir.instrument_id = :instrumentId
        GROUP BY ls.album_id
        ORDER BY totalPlayed DESC
        LIMIT :limit
    """)
    fun getTopAlbumsForInstrument(instrumentId: Long, limit: Int): Flow<List<ListeningHistoryDao.AlbumStats>>

    @Query("""
        SELECT ls.year_month AS month, SUM(ls.playback_duration_ms) AS totalTime
        FROM listening_sessions ls
        INNER JOIN song_instrument_relationship sir ON ls.song_id = sir.song_id
        INNER JOIN instrument_song_ranking isr ON sir.instrument_id = isr.instrument_id AND sir.song_id = isr.song_id
        WHERE sir.instrument_id = :instrumentId
        GROUP BY ls.year_month
        ORDER BY ls.year_month ASC
    """)
    fun getListeningTrendForInstrument(instrumentId: Long): Flow<List<ListeningHistoryDao.TrendData>>
}

data class MoodWithStatsEntity(
    @Embedded val mood: MoodEntity,
    @ColumnInfo(name = "calculated_song_count") val calculatedSongCount: Int,
    @ColumnInfo(name = "total_play_count") val totalPlayCount: Long,
    @ColumnInfo(name = "total_duration_ms") val totalDurationMs: Long
)

data class TagWithStatsEntity(
    @Embedded val tag: TagEntity,
    @ColumnInfo(name = "calculated_song_count") val calculatedSongCount: Int,
    @ColumnInfo(name = "total_play_count") val totalPlayCount: Long,
    @ColumnInfo(name = "total_duration_ms") val totalDurationMs: Long
)

data class InstrumentWithStatsEntity(
    @Embedded val instrument: InstrumentEntity,
    @ColumnInfo(name = "calculated_song_count") val calculatedSongCount: Int,
    @ColumnInfo(name = "total_play_count") val totalPlayCount: Long,
    @ColumnInfo(name = "total_duration_ms") val totalDurationMs: Long
)

@Serializable
data class MetadataWithPath(
    @Embedded val metadata: SongMetadataEntity,
    val data: String
)
