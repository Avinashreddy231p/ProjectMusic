package com.mardous.projectmusic.data.local.repository

import android.os.ParcelFileDescriptor
import com.kyant.taglib.TagLib
import com.mardous.projectmusic.data.local.database.core.SongEntity
import com.mardous.projectmusic.data.local.database.core.SongMetadataEntity
import com.mardous.projectmusic.data.local.database.dao.LyricsDao
import com.mardous.projectmusic.data.local.database.dao.MetadataDao
import com.mardous.projectmusic.data.local.database.dao.RankingDao
import com.mardous.projectmusic.data.local.database.intel.LyricsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileTagScanner(
    private val rankingDao: RankingDao,
    private val metadataDao: MetadataDao,
    private val lyricsDao: LyricsDao
) {
    data class ScanResult(
        val songsScanned: Int = 0,
        val songsUpdated: Int = 0,
        val lyricsUpdated: Int = 0,
        val errors: Int = 0
    )

    suspend fun scanAll(onProgress: ((current: Int, total: Int, label: String) -> Unit)? = null): ScanResult = withContext(Dispatchers.IO) {
        var result = ScanResult()
        val songs = rankingDao.getAllSongs()
        val metadataMap = rankingDao.getAllSongMetadata().associateBy { it.songKey }
        val total = songs.size

        for ((index, song) in songs.withIndex()) {
            try {
                val existingMeta = metadataMap[song.songKey]
                
                val file = File(song.data)
                if (!file.exists()) {
                    result = result.copy(songsScanned = result.songsScanned + 1)
                    continue
                }

                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                try {
                    val tagMetadata = TagLib.getMetadata(fd.dup().detachFd(), false)
                    if (tagMetadata != null) {
                        val tags = tagMetadata.propertyMap
                        
                        // 1. Update SongEntity if core tags changed
                        val updatedSong = updateSongEntityIfNeeded(song, tags)
                        if (updatedSong != null) {
                            rankingDao.upsertSong(updatedSong)
                            // result = result.copy(songsUpdated = result.songsUpdated + 1) // Counted in metadata update usually
                        }

                        // 2. Update SongMetadataEntity
                        val updatedMeta = updateMetadataEntity(song.songKey, existingMeta, tags)
                        if (updatedMeta != null) {
                            metadataDao.upsertSongMetadata(updatedMeta)
                            result = result.copy(songsUpdated = result.songsUpdated + 1)
                        }

                        // 3. Update Lyrics if present
                        val lyrics = tags[MetadataReaderConsts.LYRICS]?.firstOrNull { it.isNotBlank() }
                        if (!lyrics.isNullOrEmpty()) {
                            val existingLyrics = lyricsDao.getLyrics(song.songKey)
                            if (existingLyrics == null || existingLyrics.embeddedLyrics != lyrics) {
                                val lyricsEntity = (existingLyrics ?: LyricsEntity(id = song.songKey)).copy(
                                    embeddedLyrics = lyrics,
                                    hasEmbeddedLyrics = true
                                )
                                lyricsDao.insertLyrics(lyricsEntity)
                                result = result.copy(lyricsUpdated = result.lyricsUpdated + 1)
                            }
                        }
                    }
                } finally {
                    fd.close()
                }
                
                result = result.copy(songsScanned = result.songsScanned + 1)
            } catch (e: Exception) {
                result = result.copy(errors = result.errors + 1)
            }
            onProgress?.invoke(index + 1, total, song.title)
        }
        onProgress?.invoke(total, total, "Done")
        result
    }

    private fun updateSongEntityIfNeeded(song: SongEntity, tags: Map<String, Array<String>>): SongEntity? {
        val title = tags[MetadataReaderConsts.TITLE]?.firstOrNull { it.isNotBlank() } ?: song.title
        val artist = tags[MetadataReaderConsts.ARTIST]?.firstOrNull { it.isNotBlank() } ?: song.artistName
        val album = tags[MetadataReaderConsts.ALBUM]?.firstOrNull { it.isNotBlank() } ?: song.albumName
        val albumArtist = tags[MetadataReaderConsts.ALBUM_ARTIST]?.firstOrNull { it.isNotBlank() } ?: song.albumArtist
        val genre = tags[MetadataReaderConsts.GENRE]?.firstOrNull { it.isNotBlank() } ?: song.genreName

        return if (title != song.title || artist != song.artistName || album != song.albumName || 
            albumArtist != song.albumArtist || genre != song.genreName) {
            song.copy(
                title = title,
                artistName = artist,
                albumName = album,
                albumArtist = albumArtist,
                genreName = genre
            )
        } else null
    }

    private fun updateMetadataEntity(songKey: Long, existing: SongMetadataEntity?, tags: Map<String, Array<String>>): SongMetadataEntity? {
        val base = existing ?: SongMetadataEntity(songKey = songKey)
        var m = base
        var changed = false

        fun readSingle(key: String): String? = tags[key]?.firstOrNull { it.isNotBlank() }
        fun readInt(key: String): Int = tags[key]?.firstOrNull()?.toIntOrNull() ?: 0

        readSingle(MetadataReaderConsts.COMPOSER)?.let { if (m.composer != it) { m = m.copy(composer = it); changed = true } }
        readSingle(MetadataReaderConsts.LYRICIST)?.let { if (m.lyricist != it) { m = m.copy(lyricist = it); changed = true } }
        readSingle(MetadataReaderConsts.ARRANGER)?.let { if (m.arranger != it) { m = m.copy(arranger = it); changed = true } }
        readSingle(MetadataReaderConsts.PRODUCER)?.let { if (m.producer != it) { m = m.copy(producer = it); changed = true } }
        readSingle(MetadataReaderConsts.COPYRIGHT)?.let { if (m.copyright != it) { m = m.copy(copyright = it); changed = true } }
        readSingle(MetadataReaderConsts.GENRE)?.let { if (m.genre != it) { m = m.copy(genre = it); changed = true } }
        readSingle("CONDUCTOR")?.let { if (m.conductor != it) { m = m.copy(conductor = it); changed = true } }
        readSingle("PUBLISHER")?.let { if (m.publisher != it) { m = m.copy(publisher = it); changed = true } }
        readSingle("ENGINEER")?.let { if (m.engineer != it) { m = m.copy(engineer = it); changed = true } }
        readSingle("ISRC")?.let { if (m.isrc != it) { m = m.copy(isrc = it); changed = true } }
        readSingle("LANGUAGE")?.let { if (m.language != it) { m = m.copy(language = it); changed = true } }
        readSingle("KEY")?.let { if (m.keySignature != it) { m = m.copy(keySignature = it); changed = true } }
        readSingle(MetadataReaderConsts.COMMENT)?.let { if (m.comment != it) { m = m.copy(comment = it); changed = true } }

        readInt("BPM").let { if (it > 0 && m.bpm != it) { m = m.copy(bpm = it); changed = true } }
        readInt(MetadataReaderConsts.TRACK_NUMBER).let { if (it > 0 && m.trackNumber != it) { m = m.copy(trackNumber = it); changed = true } }
        readInt(MetadataReaderConsts.TRACK_TOTAL).let { if (it > 0 && m.trackTotal != it) { m = m.copy(trackTotal = it); changed = true } }
        
        readSingle(MetadataReaderConsts.DISC_NUMBER)?.let { v ->
            val dn = v.split("/")[0].toIntOrNull() ?: v.toIntOrNull()
            if (dn != null && dn > 0 && m.discNumber != dn) { m = m.copy(discNumber = dn); changed = true }
        }
        readInt(MetadataReaderConsts.DISC_TOTAL).let { if (it > 0 && m.discTotal != it) { m = m.copy(discTotal = it); changed = true } }

        readSingle(MetadataReaderConsts.YEAR)?.let { v ->
            val year = v.take(4).toIntOrNull()
            if (year != null && year > 0 && m.year != year) { m = m.copy(year = year); changed = true }
        }

        return if (changed) m else null
    }

    private object MetadataReaderConsts {
        const val TITLE = "TITLE"
        const val ARTIST = "ARTIST"
        const val ALBUM = "ALBUM"
        const val ALBUM_ARTIST = "ALBUMARTIST"
        const val GENRE = "GENRE"
        const val TRACK_NUMBER = "TRACKNUMBER"
        const val TRACK_TOTAL = "TRACKTOTAL"
        const val DISC_NUMBER = "DISCNUMBER"
        const val DISC_TOTAL = "DISCTOTAL"
        const val YEAR = "DATE"
        const val LYRICS = "LYRICS"
        const val LYRICIST = "LYRICIST"
        const val ARRANGER = "ARRANGER"
        const val COMPOSER = "COMPOSER"
        const val PRODUCER = "PRODUCER"
        const val COMMENT = "COMMENT"
        const val ENCODER = "ENCODER"
        const val COPYRIGHT = "COPYRIGHT"
    }
}
