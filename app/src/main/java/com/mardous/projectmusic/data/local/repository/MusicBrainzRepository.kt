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

    suspend fun scanArtists(): ArtistScanResult = withContext(Dispatchers.IO) {
        var result = ArtistScanResult()
        val allArtists = metadataDao.getAllArtistsList()
        val allAlbumArtists = metadataDao.getAllAlbumArtistsList()

        for ((index, artist) in allArtists.withIndex()) {
            try {
                val updated = lookupArtist(artist)
                if (updated != null) {
                    metadataDao.upsertArtist(updated)
                    result = result.copy(artistsUpdated = result.artistsUpdated + 1)

                    val songs = rankingDao.getAllSongs().filter { it.artistName.equals(artist.name, ignoreCase = true) }
                    for (song in songs) {
                        val meta = rankingDao.getSongMetadata(song.songKey)
                        if (meta != null && meta.musicbrainzArtistId.isNullOrEmpty()) {
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
            if (index % 50 == 0 && index > 0) {
                delay(1000)
            }
        }

        for (aa in allAlbumArtists) {
            try {
                if (allArtists.any { it.name.equals(aa.name, ignoreCase = true) }) continue
                val aaSearch = try {
                    musicBrainzService.searchArtist(aa.name).artists.firstOrNull()
                } catch (e: Exception) { null }
                if (aaSearch != null) {
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
        }
        result
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

    suspend fun scanAndWriteAll(): ScanResult = withContext(Dispatchers.IO) {
        var result = ScanResult()
        val songs = rankingDao.getAllSongs()
        val metadataMap = rankingDao.getAllSongMetadata().associateBy { it.songKey }

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
        }
        result
    }

    private suspend fun lookupSong(song: SongEntity, existing: SongMetadataEntity): SongMetadataEntity? {
        var changed = false
        val artistMbid: String?
        val albumMbid: String?
        val trackMbid: String?

        val metadataWithArtistMbid: SongMetadataEntity
        if (existing.musicbrainzArtistId.isNullOrEmpty()) {
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
                    val aaName = song.albumArtist
                    if (aaName != null && aaName != song.artistName) {
                        val aaEntity = metadataDao.getAlbumArtistByName(aaName)
                        if (aaEntity != null) {
                            metadataDao.upsertAlbumArtist(aaEntity.copy(
                                musicbrainzId = aaEntity.musicbrainzId ?: artist.id,
                                type = aaEntity.type ?: artist.type,
                                gender = aaEntity.gender ?: artist.gender,
                                country = aaEntity.country ?: artist.country,
                                disambiguation = aaEntity.disambiguation ?: artist.disambiguation,
                                beginDate = aaEntity.beginDate ?: artist.lifeSpan?.begin,
                                endDate = aaEntity.endDate ?: artist.lifeSpan?.end
                            ))
                        }
                    }
                    artist.id
                } else null
            } catch (e: Exception) { null }
            artistMbid = result
            metadataWithArtistMbid = existing.copy(musicbrainzArtistId = artistMbid)
        } else {
            artistMbid = existing.musicbrainzArtistId
            metadataWithArtistMbid = existing
        }

        val metadataWithAlbumMbid: SongMetadataEntity
        if (metadataWithArtistMbid.musicbrainzAlbumId.isNullOrEmpty()) {
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
            albumMbid = result
            metadataWithAlbumMbid = metadataWithArtistMbid.copy(musicbrainzAlbumId = albumMbid)
        } else {
            albumMbid = metadataWithArtistMbid.musicbrainzAlbumId
            metadataWithAlbumMbid = metadataWithArtistMbid
        }

        val metadataWithTrackMbid: SongMetadataEntity
        if (metadataWithAlbumMbid.musicbrainzTrackId.isNullOrEmpty() && artistMbid != null) {
            val recordingId = try {
                val search = musicBrainzService.searchRelease(song.title, song.artistName)
                val release = search.releases.firstOrNull()
                if (release != null) {
                    changed = true
                    val rid = release.id
                    try {
                        val recording = musicBrainzService.getRecording(rid)
                        val recordingIsrc = recording.isrcs?.firstOrNull()
                        val recordingRelations = recording.relations ?: emptyList()

                        var m = metadataWithAlbumMbid.copy(musicbrainzTrackId = rid)

                        if (recordingIsrc != null && m.isrc.isNullOrEmpty()) {
                            changed = true; m = m.copy(isrc = recordingIsrc)
                        }
                        for (relation in recordingRelations) {
                            val artistName = relation.artist?.name
                            if (artistName != null) {
                                when (relation.type) {
                                    "lyricist" -> if (m.lyricist.isNullOrEmpty()) { changed = true; m = m.copy(lyricist = artistName) }
                                    "conductor" -> if (m.conductor.isNullOrEmpty()) { changed = true; m = m.copy(conductor = artistName) }
                                    "arranger" -> if (m.arranger.isNullOrEmpty()) { changed = true; m = m.copy(arranger = artistName) }
                                    "producer" -> if (m.producer.isNullOrEmpty()) { changed = true; m = m.copy(producer = artistName) }
                                    "engineer" -> if (m.engineer.isNullOrEmpty()) { changed = true; m = m.copy(engineer = artistName) }
                                    "publisher" -> if (m.publisher.isNullOrEmpty()) { changed = true; m = m.copy(publisher = artistName) }
                                }
                            }
                            val work = relation.work
                            if (work != null && m.musicbrainzWorkId.isNullOrEmpty()) {
                                changed = true
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
                    } catch (e: Exception) { }
                    rid
                } else null
            } catch (e: Exception) { null }
            trackMbid = recordingId
            metadataWithTrackMbid = metadataWithAlbumMbid.copy(musicbrainzTrackId = trackMbid)
        } else {
            trackMbid = metadataWithAlbumMbid.musicbrainzTrackId
            metadataWithTrackMbid = metadataWithAlbumMbid
        }

        return if (changed) metadataWithTrackMbid else null
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
