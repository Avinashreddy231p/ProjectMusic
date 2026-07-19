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

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore.Audio.AudioColumns
import com.mardous.projectmusic.data.local.MediaQueryDispatcher
import com.mardous.projectmusic.data.local.database.dao.HistoryDao
import com.mardous.projectmusic.data.local.database.analytics.HistoryEntity
import com.mardous.projectmusic.data.local.database.dao.ListeningHistoryDao
import com.mardous.projectmusic.data.local.database.dao.PlayCountDao
import com.mardous.projectmusic.data.local.database.analytics.PlayCountEntity
import com.mardous.projectmusic.data.mapper.toHistoryEntity
import com.mardous.projectmusic.data.mapper.toSong
import com.mardous.projectmusic.data.model.Album
import com.mardous.projectmusic.data.model.Artist
import com.mardous.projectmusic.data.model.ContentType
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.util.Constants.MAX_ITEMS_PER_CHUNK
import com.mardous.projectmusic.util.Preferences
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

interface SmartRepository {
    suspend fun topAlbums(): List<Album>
    suspend fun topAlbumArtists(): List<Artist>
    suspend fun recentSongs(): List<Song>
    suspend fun recentSongs(query: String, contentType: ContentType): List<Song>
    suspend fun recentAlbums(): List<Album>
    suspend fun recentAlbumArtists(): List<Artist>
    suspend fun historyAlbums(): List<Album>
    suspend fun historyAlbumArtists(): List<Artist>
    suspend fun notRecentlyPlayedSongs(): List<Song>
    suspend fun playCountSongs(): List<Song>
    fun playCountSongsFlow(): Flow<List<Song>>
    suspend fun findSongsInPlayCount(songs: List<Song>): List<PlayCountEntity>
    suspend fun findSongInPlayCount(songId: Long): PlayCountEntity?
    suspend fun deleteSongInPlayCount(songId: Long)
    suspend fun deleteSongsInPlayCount(songIds: List<Long>)
    suspend fun insetOrIncrementPlayCount(song: Song, timePlayed: Long)
    suspend fun insetOrIncrementSkipCount(song: Song)
    suspend fun clearPlayCount()
    suspend fun historySongs(): List<Song>
    fun historySongsFlow(): Flow<List<Song>>
    suspend fun upsertSongInHistory(currentSong: Song)
    suspend fun deleteSongInHistory(songId: Long)
    suspend fun deleteSongsInHistory(songIds: List<Long>)
    suspend fun clearSongHistory()
    suspend fun songStats(songId: Long): com.mardous.projectmusic.data.local.database.core.SongStatsEntity?
    suspend fun songRank(songId: Long, type: String): com.mardous.projectmusic.data.local.database.core.SongRankingEntity?
    suspend fun deleteSongCompletely(songId: Long)
    suspend fun deleteSongsCompletely(songIds: List<Long>)
}

class RealSmartRepository(
    private val context: Context,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val historyDao: HistoryDao,
    private val playCountDao: PlayCountDao,
    private val listeningHistoryDao: ListeningHistoryDao,
    private val rankingDao: com.mardous.projectmusic.data.local.database.dao.RankingDao
) : SmartRepository {

    override suspend fun topAlbums(): List<Album> =
        albumRepository.splitIntoAlbums(playCountSongs(), sorted = false)

    override suspend fun topAlbumArtists(): List<Artist> =
        artistRepository.splitIntoAlbumArtists(topAlbums())

    override suspend fun recentSongs(): List<Song> =
        songRepository.songs(makeLastAddedCursor(null, ContentType.RecentSongs))

    override suspend fun recentSongs(query: String, contentType: ContentType): List<Song> =
        songRepository.songs(makeLastAddedCursor(query, contentType))

    override suspend fun recentAlbums(): List<Album> =
        albumRepository.splitIntoAlbums(recentSongs(), sorted = false)

    override suspend fun recentAlbumArtists(): List<Artist> =
        artistRepository.splitIntoAlbumArtists(recentAlbums())

    override suspend fun historyAlbums(): List<Album> =
        albumRepository.splitIntoAlbums(historySongs(), sorted = false)

    override suspend fun historyAlbumArtists(): List<Artist> =
        artistRepository.splitIntoAlbumArtists(historyAlbums())

    override suspend fun notRecentlyPlayedSongs(): List<Song> {
        return buildList {
            addAll(songRepository.songs())

            val playedSongIds = historyDao.playedSongIds()
            removeAll { it.id in playedSongIds }

            val oldSongIds = historyDao.notPlayedSongIds(
                cutoff = Preferences.getHistoryCutoff(context).interval
            )
            val oldSongs = songRepository.songs(
                songRepository.makeSongCursor(
                    selection = "${AudioColumns._ID} IN (${oldSongIds.joinToString(",") { "?" }})",
                    selectionValues = oldSongIds.map { it.toString() }.toTypedArray()
                )
            )
            addAll(oldSongs)
        }
    }

    override suspend fun playCountSongs(): List<Song> = playCountDao.playCountSongs()
        .fromPlayCountToSongs()

    override fun playCountSongsFlow(): Flow<List<Song>> =
        playCountDao.playCountSongsFlow().map { playCountEntities ->
            playCountEntities.fromPlayCountToSongs()
        }

    override suspend fun findSongsInPlayCount(songs: List<Song>): List<PlayCountEntity> {
        if (songs.isEmpty()) return emptyList()
        return buildList {
            songs.map { it.id }.chunked(MAX_ITEMS_PER_CHUNK).forEach { chunkIds ->
                addAll(playCountDao.findSongsExistInPlayCount(chunkIds))
            }
        }
    }

    override suspend fun findSongInPlayCount(songId: Long): PlayCountEntity? =
        playCountDao.findSongExistInPlayCount(songId)

    override suspend fun deleteSongInPlayCount(songId: Long) =
        playCountDao.deleteSongInPlayCount(songId)

    override suspend fun deleteSongsInPlayCount(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        songIds.chunked(MAX_ITEMS_PER_CHUNK).forEach { chunkIds ->
            playCountDao.deleteSongsInPlayCount(chunkIds)
        }
    }

    override suspend fun insetOrIncrementPlayCount(song: Song, timePlayed: Long) =
        playCountDao.insertOrIncrementPlayCount(song, timePlayed)

    override suspend fun insetOrIncrementSkipCount(song: Song) =
        playCountDao.insertOrIncrementSkipCount(song)

    override suspend fun clearPlayCount() {
        playCountDao.clearPlayCount()
    }

    override suspend fun historySongs(): List<Song> =
        historyDao.historySongs(Preferences.getHistoryCutoff(context).interval).
            fromHistoryToSongs()

    override fun historySongsFlow(): Flow<List<Song>> =
        historyDao.historySongsFlow(Preferences.getHistoryCutoff(context).interval)
            .map { historyEntities -> historyEntities.fromHistoryToSongs() }

    override suspend fun upsertSongInHistory(currentSong: Song) =
        historyDao.upsertSongInHistory(currentSong.toHistoryEntity(System.currentTimeMillis()))

    override suspend fun deleteSongInHistory(songId: Long) {
        historyDao.deleteSongInHistory(songId)
    }

    override suspend fun deleteSongsInHistory(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        songIds.chunked(MAX_ITEMS_PER_CHUNK).forEach { chunkIds ->
            historyDao.deleteSongsInHistory(chunkIds)
        }
    }

    override suspend fun clearSongHistory() {
        historyDao.clearHistory()
    }

    override suspend fun songStats(songId: Long) = rankingDao.getSongStatsByMediaStoreId(songId)

    override suspend fun songRank(songId: Long, type: String) = rankingDao.getSongRankingByMediaStoreId(songId, type)

    override suspend fun deleteSongCompletely(songId: Long) {
        rankingDao.deleteSongCompletely(songId)
        listeningHistoryDao.deleteSessionsBySongId(songId)
        historyDao.deleteSongInHistory(songId)
        playCountDao.deleteSongInPlayCount(songId)
    }

    override suspend fun deleteSongsCompletely(songIds: List<Long>) {
        for (id in songIds) {
            deleteSongCompletely(id)
        }
    }

    private fun makeLastAddedCursor(query: String?, contentType: ContentType): Cursor? {
        val cutoff = Preferences.getLastAddedCutoff(context).interval
        val queryDispatcher = MediaQueryDispatcher()
            .setProjection(RealSongRepository.getBaseProjection())
            .setSelection("${AudioColumns.DATE_ADDED}>?")
            .setSelectionArguments(arrayOf(cutoff.toString()))
            .setSortOrder("${AudioColumns.DATE_ADDED} DESC")
        if (!query.isNullOrEmpty()) {
            when (contentType) {
                ContentType.RecentAlbums -> queryDispatcher.addSelection("${AudioColumns.ALBUM} LIKE ?")
                ContentType.RecentArtists -> queryDispatcher.addSelection("${AudioColumns.ALBUM_ARTIST} LIKE ?")
                ContentType.RecentSongs -> queryDispatcher.addSelection("${AudioColumns.TITLE} LIKE ?")
                else -> error("Content type is not valid: $contentType")
            }
            queryDispatcher.addArguments("%$query%")
        }
        return songRepository.makeSongCursor(queryDispatcher)
    }

    private suspend fun List<PlayCountEntity>.fromPlayCountToSongs(): List<Song> = withContext(IO) {
        val (deletedTracks, validTracks) = partition { it.id == -1L || !File(it.data).exists() }
        if (deletedTracks.isNotEmpty()) {
            deletedTracks.map { it.id }
                .chunked(MAX_ITEMS_PER_CHUNK)
                .forEach { chunkIds ->
                    deleteSongsInPlayCount(chunkIds)
                }
        }

        validTracks.map { it.toSong() }
    }

    private suspend fun List<HistoryEntity>.fromHistoryToSongs(): List<Song> = withContext(IO) {
        val (deletedTracks, validTracks) = partition { it.id == -1L || !File(it.data).exists() }
        if (deletedTracks.isNotEmpty()) {
            deletedTracks.map { it.id }
                .chunked(MAX_ITEMS_PER_CHUNK)
                .forEach { chunkIds ->
                    deleteSongsInHistory(chunkIds)
                }
        }

        validTracks.map { it.toSong() }
    }
}
