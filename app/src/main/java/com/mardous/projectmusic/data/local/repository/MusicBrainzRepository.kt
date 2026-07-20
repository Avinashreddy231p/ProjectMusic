package com.mardous.projectmusic.data.local.repository

import android.os.ParcelFileDescriptor
import com.kyant.taglib.TagLib
import com.mardous.projectmusic.data.local.database.core.SongEntity
import com.mardous.projectmusic.data.local.database.core.SongMetadataEntity
import com.mardous.projectmusic.data.local.database.dao.MetadataDao
import com.mardous.projectmusic.data.local.database.dao.RankingDao
import com.mardous.projectmusic.data.remote.musicbrainz.MusicBrainzService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class MusicBrainzRepository(
    private val musicBrainzService: MusicBrainzService,
    private val metadataDao: MetadataDao,
    private val rankingDao: RankingDao
) {
    private suspend fun throttle() = delay(1100)

    data class ScanResult(
        val songsScanned: Int = 0,
        val songsUpdated: Int = 0,
        val tagsWritten: Int = 0,
        val errors: Int = 0
    )

    data class ArtistScanResult(
        val artistsScanned: Int = 0,
        val artistsUpdated: Int = 0,
        val tagsWritten: Int = 0,
        val errors: Int = 0
    )

    suspend fun scanArtists(onProgress: ((current: Int, total: Int, label: String) -> Unit)? = null): ArtistScanResult = withContext(Dispatchers.IO) {
        val allArtists = metadataDao.getAllArtistsList()
        val allAlbumArtists = metadataDao.getAllAlbumArtistsList()
        lookupArtistsInternal(allArtists, allAlbumArtists, onProgress)
    }

    suspend fun lookupArtistsByIds(artistIds: List<Long>, onProgress: ((current: Int, total: Int, label: String) -> Unit)? = null): ArtistScanResult = withContext(Dispatchers.IO) {
        val allArtists = metadataDao.getAllArtistsList().filter { artistIds.contains(it.id) }
        val allAlbumArtists = metadataDao.getAllAlbumArtistsList().filter { artistIds.contains(it.id) }
        lookupArtistsInternal(allArtists, allAlbumArtists, onProgress)
    }

    private suspend fun lookupArtistsInternal(
        allArtists: List<com.mardous.projectmusic.data.local.database.metadata.ArtistEntity>,
        allAlbumArtists: List<com.mardous.projectmusic.data.local.database.metadata.AlbumArtistEntity>,
        onProgress: ((current: Int, total: Int, label: String) -> Unit)?
    ): ArtistScanResult {
        var result = ArtistScanResult()
        val total = allArtists.size + allAlbumArtists.size
        var processed = 0

        for (artist in allArtists) {
            try {
                throttle()
                val updated = lookupArtist(artist)
                if (updated != null) {
                    metadataDao.upsertArtist(updated)
                    result = result.copy(artistsUpdated = result.artistsUpdated + 1)

                    val songs = rankingDao.getAllSongs().filter { it.artistName.equals(artist.name, ignoreCase = true) }
                    for (song in songs) {
                        val meta = rankingDao.getSongMetadata(song.songKey)
                        if (meta != null && (meta.musicbrainzArtistId.isNullOrEmpty())) {
                            metadataDao.upsertSongMetadata(meta.copy(musicbrainzArtistId = updated.musicbrainzId))
                        }
                        if (updated.musicbrainzId != null) {
                            writeTags(song.data, updated.musicbrainzId, updated.type)
                            result = result.copy(tagsWritten = result.tagsWritten + 1)
                        }
                    }
                }
                result = result.copy(artistsScanned = result.artistsScanned + 1)
            } catch (e: Exception) {
                result = result.copy(errors = result.errors + 1)
            }
            processed++
            onProgress?.invoke(processed, total, "Artist: ${artist.name}")
        }

        for (aa in allAlbumArtists) {
            try {
                if (allArtists.any { it.name.equals(aa.name, ignoreCase = true) }) continue
                throttle()
                val aaSearch = try {
                    musicBrainzService.searchArtist(aa.name).artists.firstOrNull()
                } catch (e: Exception) { null }
                if (aaSearch != null) {
                    throttle()
                    val fullDetail = try { musicBrainzService.getArtist(aaSearch.id) } catch (e: Exception) { aaSearch }
                    metadataDao.upsertAlbumArtist(aa.copy(
                        musicbrainzId = aa.musicbrainzId ?: fullDetail.id,
                        type = aa.type ?: fullDetail.type,
                        gender = aa.gender ?: fullDetail.gender,
                        country = aa.country ?: fullDetail.country,
                        disambiguation = aa.disambiguation ?: fullDetail.disambiguation,
                        beginDate = aa.beginDate ?: fullDetail.lifeSpan?.begin,
                        endDate = aa.endDate ?: fullDetail.lifeSpan?.end
                    ))
                    result = result.copy(artistsUpdated = result.artistsUpdated + 1)
                }
                result = result.copy(artistsScanned = result.artistsScanned + 1)
            } catch (e: Exception) {
                result = result.copy(errors = result.errors + 1)
            }
            processed++
            onProgress?.invoke(processed, total, "Album Artist: ${aa.name}")
        }
        onProgress?.invoke(total, total, "Done")
        return result
    }

    private suspend fun lookupArtist(artistEntity: com.mardous.projectmusic.data.local.database.metadata.ArtistEntity): com.mardous.projectmusic.data.local.database.metadata.ArtistEntity? {
        if (artistEntity.musicbrainzId.isNullOrEmpty()) {
            return try {
                val search = musicBrainzService.searchArtist(artistEntity.name)
                val best = search.artists.firstOrNull() ?: return null
                val detail = try { musicBrainzService.getArtist(best.id) } catch (e: Exception) { best }
                artistEntity.copy(
                    musicbrainzId = detail.id,
                    type = detail.type,
                    gender = detail.gender,
                    country = detail.country,
                    disambiguation = detail.disambiguation,
                    beginDate = detail.lifeSpan?.begin,
                    endDate = detail.lifeSpan?.end
                )
            } catch (e: Exception) { null }
        }
        return null
    }

    suspend fun scanAndWriteAll(onProgress: ((current: Int, total: Int, label: String) -> Unit)? = null): ScanResult = withContext(Dispatchers.IO) {
        val songs = rankingDao.getAllSongs()
        lookupSongsInternal(songs, onProgress)
    }

    suspend fun lookupSongsByIds(songIds: List<Long>, onProgress: ((current: Int, total: Int, label: String) -> Unit)? = null): ScanResult = withContext(Dispatchers.IO) {
        val songs = rankingDao.getAllSongs().filter { songIds.contains(it.mediaStoreId) }
        lookupSongsInternal(songs, onProgress)
    }

    private suspend fun lookupSongsInternal(
        songs: List<com.mardous.projectmusic.data.local.database.core.SongEntity>,
        onProgress: ((current: Int, total: Int, label: String) -> Unit)?
    ): ScanResult {
        var result = ScanResult()
        val metadataMap = rankingDao.getAllSongMetadata().associateBy { it.songKey }
        val total = songs.size

        for ((index, song) in songs.withIndex()) {
            try {
                val existing = metadataMap[song.songKey]
                if (existing != null) {
                    val updated = lookupSong(song, existing)
                    if (updated != null) {
                        metadataDao.upsertSongMetadata(updated)
                        result = result.copy(songsUpdated = result.songsUpdated + 1)
                        writeTags(song.data, updated.musicbrainzArtistId, null) // FIXED
                        result = result.copy(tagsWritten = result.tagsWritten + 1)
                    }
                }
                result = result.copy(songsScanned = result.songsScanned + 1)
            } catch (e: Exception) {
                result = result.copy(errors = result.errors + 1)
            }
            if (index % 50 == 0 && index > 0) {
                delay(1000)
            }
            onProgress?.invoke(index + 1, total, song.title)
        }
        onProgress?.invoke(total, total, "Done")
        return result
    }

    private suspend fun lookupSong(song: SongEntity, existing: SongMetadataEntity): SongMetadataEntity? {
        var changed = false
        var currentMeta = existing
        
        val artistMbid: String?
        if (currentMeta.musicbrainzArtistId.isNullOrEmpty()) {
            throttle()
            val result = try {
                val search = musicBrainzService.searchArtist(song.artistName)
                val artist = search.artists.firstOrNull()
                if (artist != null) {
                    changed = true
                    val artistEntity = metadataDao.getArtistByName(song.artistName)
                    if (artistEntity != null) {
                        metadataDao.upsertArtist(artistEntity.copy(
                            musicbrainzId = artistEntity.musicbrainzId ?: artist.id,
                            type = artistEntity.type ?: artist.type,
                            gender = artistEntity.gender ?: artist.gender,
                            country = artistEntity.country ?: artist.country,
                            disambiguation = artistEntity.disambiguation ?: artist.disambiguation,
                            beginDate = artistEntity.beginDate ?: artist.lifeSpan?.begin,
                            endDate = artistEntity.endDate ?: artist.lifeSpan?.end
                        ))
                    }
                    artist.id
                } else null
            } catch (e: Exception) { null }
            artistMbid = result
            if (changed) currentMeta = currentMeta.copy(musicbrainzArtistId = artistMbid)
        } else {
            artistMbid = currentMeta.musicbrainzArtistId
        }

        if (currentMeta.musicbrainzAlbumId.isNullOrEmpty()) {
            throttle()
            val result = try {
                val search = musicBrainzService.searchRelease(song.albumName, song.artistName)
                val release = search.releases.firstOrNull()
                if (release != null) {
                    changed = true
                    val albumEntity = metadataDao.getAlbumByNameAndArtist(song.albumName, song.albumArtist)
                    if (albumEntity != null) {
                        metadataDao.upsertAlbum(albumEntity.copy(
                            musicbrainzReleaseId = albumEntity.musicbrainzReleaseId ?: release.id,
                            releaseYear = if (albumEntity.releaseYear == 0) release.date?.substringBefore("-")?.toIntOrNull() ?: 0 else albumEntity.releaseYear,
                            releaseCountry = albumEntity.releaseCountry ?: release.country,
                            label = albumEntity.label ?: release.labelInfo?.firstOrNull()?.label?.name,
                            catalogNumber = albumEntity.catalogNumber ?: release.labelInfo?.firstOrNull()?.catalogNumber
                        ))
                    }
                    release.id
                } else null
            } catch (e: Exception) { null }
            if (result != null) {
                changed = true
                currentMeta = currentMeta.copy(musicbrainzAlbumId = result)
            }
        }

        if (currentMeta.musicbrainzTrackId.isNullOrEmpty()) {
            throttle()
            try {
                val search = musicBrainzService.searchRecording(song.title, song.artistName)
                val recording = search.recordings.firstOrNull()
                if (recording != null) {
                    changed = true
                    throttle()
                    val detail = try { musicBrainzService.getRecording(recording.id) } catch (e: Exception) { null }
                    
                    val recordingIsrc = detail?.isrcs?.firstOrNull()
                    val recordingRelations = detail?.relations ?: emptyList()

                    var m = currentMeta.copy(musicbrainzTrackId = recording.id)

                    if (recordingIsrc != null && m.isrc.isNullOrEmpty()) {
                        m = m.copy(isrc = recordingIsrc)
                    }
                    for (relation in recordingRelations) {
                        val artistName = relation.artist?.name
                        if (artistName != null) {
                            when (relation.type) {
                                "lyricist" -> if (m.lyricist.isNullOrEmpty()) { m = m.copy(lyricist = artistName) }
                                "conductor" -> if (m.conductor.isNullOrEmpty()) { m = m.copy(conductor = artistName) }
                                "arranger" -> if (m.arranger.isNullOrEmpty()) { m = m.copy(arranger = artistName) }
                                "producer" -> if (m.producer.isNullOrEmpty()) { m = m.copy(producer = artistName) }
                                "engineer" -> if (m.engineer.isNullOrEmpty()) { m = m.copy(engineer = artistName) }
                                "publisher" -> if (m.publisher.isNullOrEmpty()) { m = m.copy(publisher = artistName) }
                            }
                        }
                        val work = relation.work
                        if (work != null && m.musicbrainzWorkId.isNullOrEmpty()) {
                            m = m.copy(
                                musicbrainzWorkId = work.id,
                                iswc = if (m.iswc.isNullOrEmpty()) work.iswc else m.iswc,
                                language = if (m.language.isNullOrEmpty()) work.language else m.language
                            )
                            val copyrightAttr = work.attributes?.firstOrNull { it.type == "copyright" }?.value
                            if (copyrightAttr != null && m.copyright.isNullOrEmpty()) {
                                m = m.copy(copyright = copyrightAttr)
                            }
                        }
                    }
                    currentMeta = m
                }
            } catch (e: Exception) { }
        }

        return if (changed) currentMeta else null
    }

    private fun writeTags(filePath: String, artistMbid: String?, artistType: String?) {
        try {
            val file = File(filePath)
            if (!file.exists()) return
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
            try {
                val current = TagLib.getMetadata(fd.dup().detachFd(), false)?.propertyMap ?: hashMapOf()
                val props = HashMap(current)

                fun put(key: String, value: String?) {
                    if (!value.isNullOrBlank()) props[key] = arrayOf(value.trim())
                }
                put("MUSICBRAINZ_ARTISTID", artistMbid)
                if (!artistType.isNullOrBlank()) {
                    put("ARTIST_TYPE", artistType)
                }

                TagLib.savePropertyMap(fd.dup().detachFd(), props)
            } finally {
                fd.close()
            }
        } catch (e: Exception) { }
    }
}
