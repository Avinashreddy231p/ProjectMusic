package com.mardous.projectmusic.data.local.repository

import com.mardous.projectmusic.data.local.database.dao.ListeningHistoryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * An in-memory cache of statistical data derived from ListeningHistoryDao.
 * This ensures O(1) synchronous lookups during memory-bound sorting operations.
 */
class StatsCache(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val applicationScope: CoroutineScope,
    private val maxCacheSize: Int = 10000
) {

    private val songPlayCounts = ConcurrentHashMap<Long, Long>()
    private val songDurations = ConcurrentHashMap<Long, Long>()
    
    private val albumPlayCounts = ConcurrentHashMap<Long, Long>()
    private val albumDurations = ConcurrentHashMap<Long, Long>()
    
    private val artistPlayCounts = ConcurrentHashMap<String, Long>()
    private val artistDurations = ConcurrentHashMap<String, Long>()

    init {
        listeningHistoryDao.getAllSessionsFlow()
            .onEach { refresh() }
            .launchIn(applicationScope)
    }

    private fun refresh() {
        applicationScope.launch(Dispatchers.IO) {
            songPlayCounts.clear()
            songDurations.clear()
            val topSongs = listeningHistoryDao.getTopSongs(maxCacheSize)
            topSongs.forEach { 
                songPlayCounts[it.songId] = it.playCount.toLong()
                songDurations[it.songId] = it.totalPlayed
            }

            albumPlayCounts.clear()
            albumDurations.clear()
            val topAlbums = listeningHistoryDao.getTopAlbums(maxCacheSize)
            topAlbums.forEach { 
                albumPlayCounts[it.albumId] = it.playCount.toLong()
                albumDurations[it.albumId] = it.totalPlayed
            }

            artistPlayCounts.clear()
            artistDurations.clear()
            val topArtists = listeningHistoryDao.getTopArtists(maxCacheSize)
            topArtists.forEach { 
                artistPlayCounts[it.artistName] = it.playCount.toLong()
                artistDurations[it.artistName] = it.totalPlayed
            }
        }
    }

    fun getSongPlayCount(songId: Long): Long = songPlayCounts[songId] ?: 0L
    fun getSongDuration(songId: Long): Long = songDurations[songId] ?: 0L

    fun getAlbumPlayCount(albumId: Long): Long = albumPlayCounts[albumId] ?: 0L
    fun getAlbumDuration(albumId: Long): Long = albumDurations[albumId] ?: 0L

    fun getArtistPlayCount(artistName: String): Long = artistPlayCounts[artistName] ?: 0L
    fun getArtistDuration(artistName: String): Long = artistDurations[artistName] ?: 0L
}
