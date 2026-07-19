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

package com.mardous.projectmusic.data.local.database.sync

import android.util.Log
import com.mardous.projectmusic.data.local.database.dao.RankingDao
import com.mardous.projectmusic.data.local.database.dao.MetadataDao
import com.mardous.projectmusic.data.local.database.dao.HealthDao
import com.mardous.projectmusic.data.local.database.core.SongEntity
import com.mardous.projectmusic.data.local.database.core.SongMetadataEntity
import com.mardous.projectmusic.data.local.database.intel.DatabaseHealthEntity
import com.mardous.projectmusic.data.local.database.metadata.ArtistEntity
import com.mardous.projectmusic.data.local.database.metadata.AlbumEntity
import com.mardous.projectmusic.data.local.database.metadata.AlbumArtistEntity
import com.mardous.projectmusic.data.local.database.metadata.GenreEntity
import com.mardous.projectmusic.data.local.database.dao.LyricsDao
import com.mardous.projectmusic.data.local.database.intel.LyricsEntity
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.mapper.toSongEntity
import com.mardous.projectmusic.data.local.MetadataReader
import com.mardous.projectmusic.data.local.repository.SongRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RankingEngine(
    private val rankingDao: RankingDao,
    private val metadataDao: MetadataDao,
    private val healthDao: HealthDao,
    private val lyricsDao: LyricsDao,
    private val songRepository: SongRepository
) {
    private val dirtyGlobal = AtomicBoolean(true)
    private val dirtyMoods = AtomicBoolean(true)
    private val dirtyGenres = AtomicBoolean(true)
    private val dirtyTags = AtomicBoolean(true)
    private val dirtyInstruments = AtomicBoolean(true)
    private val dirtyAlbumArtists = AtomicBoolean(true)
    private val dirtyPlaylists = AtomicBoolean(true)

    private val initialized = AtomicBoolean(false)
    private val flushCount = AtomicInteger(0)

    suspend fun initialize() {
        if (initialized.getAndSet(true)) return
        val health = healthDao.getHealth()
        if (health != null && health.schemaVersion >= 13) return

        Log.i(TAG, "First run detected — initializing alphabetical ranks")
        rankingDao.initializeAllAlphabeticalRanks()

        val now = System.currentTimeMillis()
        healthDao.upsertHealth(
            DatabaseHealthEntity(
                id = 0,
                lastSync = now,
                lastRanking = now,
                lastVacuum = 0,
                lastIntegrityCheck = now,
                schemaVersion = 13
            )
        )
    }

    suspend fun registerSong(song: Song): Long {
        val existingKey = rankingDao.getSongKeyByMediaStoreId(song.id)
        val songKey = if (existingKey != null) {
            existingKey
        } else {
            rankingDao.upsertSong(song.toSongEntity())
        }
        upsertSongMetadata(mutableListOf(song))
        markDirtyGlobal()
        return songKey
    }

    fun markDirty() {
        dirtyGlobal.set(true)
        dirtyMoods.set(true)
        dirtyGenres.set(true)
        dirtyTags.set(true)
        dirtyInstruments.set(true)
        dirtyAlbumArtists.set(true)
        dirtyPlaylists.set(true)
    }

    fun markDirtyGlobal() = dirtyGlobal.set(true)
    fun markDirtyMoods() = dirtyMoods.set(true)
    fun markDirtyGenres() = dirtyGenres.set(true)
    fun markDirtyTags() = dirtyTags.set(true)
    fun markDirtyInstruments() = dirtyInstruments.set(true)
    fun markDirtyAlbumArtists() = dirtyAlbumArtists.set(true)
    fun markDirtyPlaylists() = dirtyPlaylists.set(true)

    suspend fun recalculateSongRanks(songKey: Long) {
        batchRecalculate()
    }

    suspend fun batchRecalculate() {
        initialize()

        val hasGlobal = dirtyGlobal.getAndSet(false)
        val hasMoods = dirtyMoods.getAndSet(false)
        val hasGenres = dirtyGenres.getAndSet(false)
        val hasTags = dirtyTags.getAndSet(false)
        val hasInstruments = dirtyInstruments.getAndSet(false)
        val hasAlbumArtists = dirtyAlbumArtists.getAndSet(false)
        val hasPlaylists = dirtyPlaylists.getAndSet(false)

        if (!hasGlobal && !hasMoods && !hasGenres && !hasTags && !hasInstruments && !hasAlbumArtists && !hasPlaylists) {
            if (flushCount.incrementAndGet() % 50 == 0) runValidation()
            return
        }

        val mediaStoreSongs = songRepository.songs()
        if (mediaStoreSongs.isNotEmpty()) {
            metadataDao.upsertSongs(mediaStoreSongs.map { it.toSongEntity() })
            upsertSongMetadata(mediaStoreSongs)

            val distinctArtistNames = mediaStoreSongs.map { it.artistName }.distinct()
            for (name in distinctArtistNames) {
                if (metadataDao.getArtistByName(name) == null) {
                    metadataDao.upsertArtist(ArtistEntity(name = name))
                }
            }

            val distinctAlbumKeys = mediaStoreSongs.map { it.albumName to it.albumArtistName }.distinct()
            for ((name, artist) in distinctAlbumKeys) {
                if (metadataDao.getAlbumByNameAndArtist(name, artist) == null) {
                    metadataDao.upsertAlbum(AlbumEntity(name = name, albumArtist = artist))
                }
            }

            val distinctAlbumArtists = mediaStoreSongs.mapNotNull { it.albumArtistName }.distinct()
            for (name in distinctAlbumArtists) {
                if (metadataDao.getAlbumArtistByName(name) == null) {
                    metadataDao.upsertAlbumArtist(AlbumArtistEntity(name = name))
                }
            }

            val distinctGenres = mediaStoreSongs.mapNotNull { it.genreName }.distinct()
            for (name in distinctGenres) {
                metadataDao.upsertGenre(GenreEntity(name = name))
            }
        }

        val now = System.currentTimeMillis()

        if (hasGlobal) {
            rankingDao.rebuildGlobalSongStats()
            rankingDao.rebuildGlobalSongRankings()
            rankingDao.updateGlobalSongRanks(now)
            rankingDao.updateGlobalPlayDurationRanks()
            rankingDao.updateArtistStats(now)
            rankingDao.updateArtistGlobalRanks()
            rankingDao.updateArtistPlayDurationRanks()
            rankingDao.updateAlbumStats(now)
            rankingDao.updateAlbumGlobalRanks()
            rankingDao.updateAlbumPlayDurationRanks()
        }

        if (hasMoods) {
            for (id in rankingDao.getAllMoodIds()) {
                rankingDao.rebuildMoodRankings(id)
            }
            rankingDao.updateMoodStats(now)
            rankingDao.updateMoodGlobalRanks()
            rankingDao.updateMoodPlayDurationRanks()
        }

        if (hasGenres) {
            for (id in rankingDao.getAllGenreIds()) {
                rankingDao.rebuildGenreRankings(id)
            }
            rankingDao.updateGenreStats(now)
            rankingDao.updateGenreGlobalRanks()
            rankingDao.updateGenrePlayDurationRanks()
        }

        if (hasTags) {
            for (id in rankingDao.getAllTagIds()) {
                rankingDao.rebuildTagRankings(id)
            }
            rankingDao.updateTagStats(now)
            rankingDao.updateTagGlobalRanks()
            rankingDao.updateTagPlayDurationRanks()
        }

        if (hasInstruments) {
            for (id in rankingDao.getAllInstrumentIds()) {
                rankingDao.rebuildInstrumentRankings(id)
            }
            rankingDao.updateInstrumentStats(now)
            rankingDao.updateInstrumentGlobalRanks()
            rankingDao.updateInstrumentPlayDurationRanks()
        }

        if (hasAlbumArtists) {
            rankingDao.updateAlbumArtistStats(now)
            rankingDao.updateAlbumArtistGlobalRanks()
            rankingDao.updateAlbumArtistPlayDurationRanks()
        }

        if (hasPlaylists) {
            rankingDao.updatePlaylistStats(now)
            rankingDao.updatePlaylistGlobalRanks()
            rankingDao.updatePlaylistPlayDurationRanks()
        }

        healthDao.upsertHealth(
            DatabaseHealthEntity(
                id = 0,
                lastSync = now,
                lastRanking = now,
                lastVacuum = 0,
                lastIntegrityCheck = if (flushCount.incrementAndGet() % 50 == 0) {
                    runValidation(); now
                } else 0,
                schemaVersion = 13
            )
        )
    }

    private suspend fun runValidation() {
        val orphans = listOf(
            "song_rankings" to rankingDao.countOrphanedSongRankings(),
            "genre_song_ranking" to rankingDao.countOrphanedGenreRankings(),
            "mood_song_ranking" to rankingDao.countOrphanedMoodRankings(),
            "tag_song_ranking" to rankingDao.countOrphanedTagRankings(),
            "instrument_song_ranking" to rankingDao.countOrphanedInstrumentRankings(),
            "song_stats" to rankingDao.countOrphanedSongStats()
        )
        val total = orphans.sumOf { it.second }
        if (total > 0) {
            Log.w(TAG, "Orphaned records found: ${orphans.filter { it.second > 0 }}")
        } else {
            Log.i(TAG, "Validation passed — no orphaned records")
        }
    }

    private suspend fun upsertSongMetadata(songs: List<Song>) {
        for (song in songs) {
            val existingKey = rankingDao.getSongKeyByMediaStoreId(song.id) ?: continue
            metadataDao.upsertSongMetadata(
                SongMetadataEntity(
                    songKey = existingKey,
                    composer = song.composer,
                    year = song.year,
                    trackNumber = song.trackNumber,
                    fileSize = song.size,
                    dateAdded = song.dateAdded,
                    dateModified = song.rawDateModified * 1000
                )
            )
        }
    }

    companion object {
        private const val TAG = "RankingEngine"
    }
}
