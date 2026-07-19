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

package com.mardous.projectmusic.data.local.repository

import com.mardous.projectmusic.core.sort.*
import com.mardous.projectmusic.data.local.database.dao.MetadataDao
import com.mardous.projectmusic.data.local.database.dao.RankingDao
import com.mardous.projectmusic.data.local.database.dao.ListeningHistoryDao
import com.mardous.projectmusic.data.local.database.metadata.*
import com.mardous.projectmusic.data.local.database.dao.MoodWithStatsEntity
import com.mardous.projectmusic.data.local.database.dao.TagWithStatsEntity
import com.mardous.projectmusic.data.local.database.dao.InstrumentWithStatsEntity
import com.mardous.projectmusic.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

interface MetadataRepository {
    fun getAllMoods(): Flow<List<Mood>>
    fun getTagsByCategory(): Flow<Map<TagCategory, List<Tag>>>
    fun getInstrumentsByFamily(): Flow<Map<InstrumentFamily, List<Instrument>>>

    fun getMoodDetail(moodId: Long): Flow<MoodDetail?>
    fun getTagDetail(tagId: Long): Flow<TagDetail?>
    fun getInstrumentDetail(instrumentId: Long): Flow<InstrumentDetail?>

    // CRUD Moods
    suspend fun createMood(name: String, description: String? = null, emoji: String? = null, color: Int? = null): Long
    suspend fun updateMood(mood: MoodEntity)
    suspend fun deleteMood(moodId: Long)
    suspend fun renameMood(moodId: Long, newName: String)
    fun getMoodById(moodId: Long): Flow<MoodEntity?>

    // CRUD Tags
    suspend fun createTag(name: String, category: Int, description: String? = null, color: Int? = null): Long
    suspend fun updateTag(tag: TagEntity)
    suspend fun deleteTag(tagId: Long)
    suspend fun renameTag(tagId: Long, newName: String)
    fun getTagById(tagId: Long): Flow<TagEntity?>

    // CRUD Instruments
    suspend fun createInstrument(name: String, family: Int, description: String? = null): Long
    suspend fun updateInstrument(instrument: InstrumentEntity)
    suspend fun deleteInstrument(instrumentId: Long)
    fun getInstrumentById(instrumentId: Long): Flow<InstrumentEntity?>

    suspend fun assignSongsToMoods(moodIds: List<Long>, songIds: List<Long>, source: MetadataSource)
    suspend fun assignSongsToTags(tagIds: List<Long>, songIds: List<Long>, source: MetadataSource)
    suspend fun assignSongsToInstruments(instrumentIds: List<Long>, songIds: List<Long>, source: MetadataSource)
}

class RealMetadataRepository(
    private val metadataDao: MetadataDao,
    private val rankingDao: RankingDao,
    private val songRepository: SongRepository
) : MetadataRepository {

    override fun getAllMoods(): Flow<List<Mood>> {
        return metadataDao.getAllMoodsWithStats()
            .distinctUntilChanged()
            .map { entities ->
                with(MoodSortMode.AllMoods) {
                    entities.map { entity ->
                    Mood(
                        id = entity.mood.id,
                        name = entity.mood.name,
                        description = entity.mood.description,
                        emoji = entity.mood.emoji,
                        songCount = entity.calculatedSongCount,
                        playCount = entity.totalPlayCount,
                        listenedDurationMs = entity.totalDurationMs,
                        overallRank = entity.mood.overallRank,
                        favoriteCount = 0,
                        avgCompletion = 0f,
                        createdAt = entity.mood.createdAt,
                        updatedAt = entity.mood.updatedAt
                    )
                }.sorted()
            }
        }
    }

    override fun getTagsByCategory(): Flow<Map<TagCategory, List<Tag>>> {
        return metadataDao.getAllTagsWithStats()
            .distinctUntilChanged()
            .map { entities ->
                with(TagSortMode.AllTags) {
                    entities.map { entity ->
                    Tag(
                        id = entity.tag.id,
                        name = entity.tag.name,
                        category = TagCategory.entries.find { it.value == entity.tag.category } ?: TagCategory.Activity,
                        songCount = entity.calculatedSongCount,
                        playCount = entity.totalPlayCount,
                        listenedDurationMs = entity.totalDurationMs,
                        overallRank = entity.tag.overallRank,
                        createdAt = entity.tag.createdAt,
                        updatedAt = entity.tag.updatedAt,
                        description = entity.tag.description,
                        icon = entity.tag.icon,
                        color = entity.tag.color
                    )
                }.sorted().groupBy { it.category }
            }
        }
    }

    override fun getInstrumentsByFamily(): Flow<Map<InstrumentFamily, List<Instrument>>> {
        return metadataDao.getAllInstrumentsWithStats()
            .distinctUntilChanged()
            .map { entities ->
                with(InstrumentSortMode.AllInstruments) {
                    entities.map { entity ->
                    Instrument(
                        id = entity.instrument.id,
                        name = entity.instrument.name,
                        family = InstrumentFamily.entries.find { it.value == entity.instrument.family } ?: InstrumentFamily.Strings,
                        songCount = entity.calculatedSongCount,
                        playCount = entity.totalPlayCount,
                        listenedDurationMs = entity.totalDurationMs,
                        overallRank = entity.instrument.overallRank,
                        avgConfidence = 0f,
                        createdAt = entity.instrument.createdAt,
                        updatedAt = entity.instrument.updatedAt,
                        description = entity.instrument.description,
                        icon = entity.instrument.icon,
                        color = entity.instrument.color
                    )
                }.sorted().groupBy { it.family }
            }
        }
    }

    override fun getMoodDetail(moodId: Long): Flow<MoodDetail?> {
        return combine(
            metadataDao.getAllMoodsWithStats().map { list -> list.find { it.mood.id == moodId } },
            metadataDao.getSongIdsForMood(moodId),
            rankingDao.getTopSongIdsByMood(moodId),
            metadataDao.getTopArtistsForMood(moodId, 10),
            metadataDao.getTopAlbumsForMood(moodId, 10),
            metadataDao.getListeningTrendForMood(moodId)
        ) { array ->
            val stats = array[0] as MoodWithStatsEntity?
            val songIds = array[1] as List<Long>
            val topSongIds = array[2] as List<Long>
            val topArtists = array[3] as List<ListeningHistoryDao.ArtistStats>
            val topAlbums = array[4] as List<ListeningHistoryDao.AlbumStats>
            val trends = array[5] as List<ListeningHistoryDao.TrendData>

            if (stats == null) return@combine null

            val songs = songRepository.songs(songIds)
            val topSongs = songRepository.songs(topSongIds)

            MoodDetail(
                mood = Mood(
                    id = stats.mood.id,
                    name = stats.mood.name,
                    description = stats.mood.description,
                    emoji = stats.mood.emoji,
                    songCount = stats.calculatedSongCount,
                    playCount = stats.totalPlayCount,
                    listenedDurationMs = stats.totalDurationMs,
                    overallRank = stats.mood.overallRank,
                    favoriteCount = 0,
                    avgCompletion = 0f,
                    createdAt = stats.mood.createdAt,
                    updatedAt = stats.mood.updatedAt
                ),
                songs = songs,
                topSongs = topSongs,
                topArtists = topArtists,
                topAlbums = topAlbums,
                trends = trends,
                rankTrend = emptyList()
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getTagDetail(tagId: Long): Flow<TagDetail?> {
        return combine(
            metadataDao.getAllTagsWithStats().map { list -> list.find { it.tag.id == tagId } },
            metadataDao.getSongIdsForTag(tagId),
            rankingDao.getTopSongIdsByTag(tagId),
            metadataDao.getTopArtistsForTag(tagId, 10),
            metadataDao.getTopAlbumsForTag(tagId, 10),
            metadataDao.getListeningTrendForTag(tagId)
        ) { array ->
            val stats = array[0] as TagWithStatsEntity?
            val songIds = array[1] as List<Long>
            val topSongIds = array[2] as List<Long>
            val topArtists = array[3] as List<ListeningHistoryDao.ArtistStats>
            val topAlbums = array[4] as List<ListeningHistoryDao.AlbumStats>
            val trends = array[5] as List<ListeningHistoryDao.TrendData>

            if (stats == null) return@combine null

            val songs = songRepository.songs(songIds)
            val topSongs = songRepository.songs(topSongIds)

            TagDetail(
                tag = Tag(
                    id = stats.tag.id,
                    name = stats.tag.name,
                    category = TagCategory.entries.find { it.value == stats.tag.category } ?: TagCategory.Activity,
                    songCount = stats.calculatedSongCount,
                    playCount = stats.totalPlayCount,
                    listenedDurationMs = stats.totalDurationMs,
                    overallRank = stats.tag.overallRank,
                    createdAt = stats.tag.createdAt,
                    updatedAt = stats.tag.updatedAt,
                    description = stats.tag.description,
                    icon = stats.tag.icon,
                    color = stats.tag.color
                ),
                songs = songs,
                topSongs = topSongs,
                topArtists = topArtists,
                topAlbums = topAlbums,
                trends = trends,
                rankTrend = emptyList()
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getInstrumentDetail(instrumentId: Long): Flow<InstrumentDetail?> {
        return combine(
            metadataDao.getAllInstrumentsWithStats().map { list -> list.find { it.instrument.id == instrumentId } },
            metadataDao.getSongIdsForInstrument(instrumentId),
            rankingDao.getTopSongIdsByInstrument(instrumentId),
            metadataDao.getTopArtistsForInstrument(instrumentId, 10),
            metadataDao.getTopAlbumsForInstrument(instrumentId, 10),
            metadataDao.getListeningTrendForInstrument(instrumentId)
        ) { array ->
            val stats = array[0] as InstrumentWithStatsEntity?
            val songIds = array[1] as List<Long>
            val topSongIds = array[2] as List<Long>
            val topArtists = array[3] as List<ListeningHistoryDao.ArtistStats>
            val topAlbums = array[4] as List<ListeningHistoryDao.AlbumStats>
            val trends = array[5] as List<ListeningHistoryDao.TrendData>

            if (stats == null) return@combine null

            val songs = songRepository.songs(songIds)
            val topSongs = songRepository.songs(topSongIds)

            InstrumentDetail(
                instrument = Instrument(
                    id = stats.instrument.id,
                    name = stats.instrument.name,
                    family = InstrumentFamily.entries.find { it.value == stats.instrument.family } ?: InstrumentFamily.Strings,
                    songCount = stats.calculatedSongCount,
                    playCount = stats.totalPlayCount,
                    listenedDurationMs = stats.totalDurationMs,
                    overallRank = stats.instrument.overallRank,
                    avgConfidence = 0f,
                    createdAt = stats.instrument.createdAt,
                    updatedAt = stats.instrument.updatedAt,
                    description = stats.instrument.description,
                    icon = stats.instrument.icon,
                    color = stats.instrument.color
                ),
                songs = songs,
                topSongs = topSongs,
                topArtists = topArtists,
                topAlbums = topAlbums,
                trends = trends,
                rankTrend = emptyList()
            )
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun createMood(name: String, description: String?, emoji: String?, color: Int?): Long {
        val existing = metadataDao.getMoodByName(name)
        if (existing != null) return existing.id
        return metadataDao.upsertMood(MoodEntity(name = name, description = description, emoji = emoji, color = color))
    }

    override suspend fun updateMood(mood: MoodEntity) {
        metadataDao.updateMood(mood)
    }

    override suspend fun deleteMood(moodId: Long) {
        metadataDao.getMoodById(moodId)?.let { metadataDao.deleteMood(it) }
    }

    override suspend fun renameMood(moodId: Long, newName: String) {
        metadataDao.getMoodById(moodId)?.let {
            metadataDao.updateMood(it.copy(name = newName, updatedAt = System.currentTimeMillis()))
        }
    }

    override fun getMoodById(moodId: Long): Flow<MoodEntity?> = flow {
        emit(metadataDao.getMoodById(moodId))
    }

    override suspend fun createTag(name: String, category: Int, description: String?, color: Int?): Long {
        val existing = metadataDao.getTagByName(name, category)
        if (existing != null) return existing.id
        return metadataDao.upsertTag(
            TagEntity(
                name = name,
                category = category,
                description = description,
                color = color
            )
        )
    }

    override suspend fun updateTag(tag: TagEntity) {
        metadataDao.updateTag(tag)
    }

    override suspend fun deleteTag(tagId: Long) {
        metadataDao.getTagById(tagId)?.let { metadataDao.deleteTag(it) }
    }

    override suspend fun renameTag(tagId: Long, newName: String) {
        metadataDao.getTagById(tagId)?.let {
            metadataDao.updateTag(it.copy(name = newName, updatedAt = System.currentTimeMillis()))
        }
    }

    override fun getTagById(tagId: Long): Flow<TagEntity?> = flow {
        emit(metadataDao.getTagById(tagId))
    }

    override suspend fun createInstrument(name: String, family: Int, description: String?): Long {
        val existing = metadataDao.getInstrumentByName(name, family)
        if (existing != null) return existing.id
        return metadataDao.upsertInstrument(InstrumentEntity(name = name, family = family, description = description))
    }

    override suspend fun updateInstrument(instrument: InstrumentEntity) {
        metadataDao.updateInstrument(instrument)
    }

    override suspend fun deleteInstrument(instrumentId: Long) {
        metadataDao.getInstrumentById(instrumentId)?.let { metadataDao.deleteInstrument(it) }
    }

    override fun getInstrumentById(instrumentId: Long): Flow<InstrumentEntity?> = flow {
        emit(metadataDao.getInstrumentById(instrumentId))
    }

    override suspend fun assignSongsToMoods(moodIds: List<Long>, songIds: List<Long>, source: MetadataSource) {
        val songMoods = moodIds.flatMap { moodId ->
            songIds.map { songId ->
                SongMoodEntity(
                    songId = songId,
                    moodId = moodId,
                    confidenceScore = 1.0,
                    source = source.value
                )
            }
        }
        metadataDao.upsertSongMoods(songMoods)
    }

    override suspend fun assignSongsToTags(tagIds: List<Long>, songIds: List<Long>, source: MetadataSource) {
        val songTags = tagIds.flatMap { tagId ->
            songIds.map { songId ->
                SongTagEntity(
                    songId = songId,
                    tagId = tagId,
                    confidenceScore = 1.0,
                    source = source.value
                )
            }
        }
        metadataDao.upsertSongTags(songTags)
    }

    override suspend fun assignSongsToInstruments(instrumentIds: List<Long>, songIds: List<Long>, source: MetadataSource) {
        val songInstruments = instrumentIds.flatMap { instrumentId ->
            songIds.map { songId ->
                SongInstrumentEntity(
                    songId = songId,
                    instrumentId = instrumentId,
                    confidenceScore = 1.0,
                    source = source.value
                )
            }
        }
        metadataDao.upsertSongInstruments(songInstruments)
    }
}

data class MoodDetail(
    val mood: Mood,
    val songs: List<Song>,
    val topSongs: List<Song>,
    val topArtists: List<ListeningHistoryDao.ArtistStats>,
    val topAlbums: List<ListeningHistoryDao.AlbumStats>,
    val trends: List<ListeningHistoryDao.TrendData>,
    val rankTrend: List<Int>
)

data class TagDetail(
    val tag: Tag,
    val songs: List<Song>,
    val topSongs: List<Song>,
    val topArtists: List<ListeningHistoryDao.ArtistStats>,
    val topAlbums: List<ListeningHistoryDao.AlbumStats>,
    val trends: List<ListeningHistoryDao.TrendData>,
    val rankTrend: List<Int>
)

data class InstrumentDetail(
    val instrument: Instrument,
    val songs: List<Song>,
    val topSongs: List<Song>,
    val topArtists: List<ListeningHistoryDao.ArtistStats>,
    val topAlbums: List<ListeningHistoryDao.AlbumStats>,
    val trends: List<ListeningHistoryDao.TrendData>,
    val rankTrend: List<Int>
)
