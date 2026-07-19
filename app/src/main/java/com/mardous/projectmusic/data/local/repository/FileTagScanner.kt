package com.mardous.projectmusic.data.local.repository

import android.os.ParcelFileDescriptor
import com.kyant.taglib.TagLib
import com.mardous.projectmusic.data.local.database.core.SongMetadataEntity
import com.mardous.projectmusic.data.local.database.dao.MetadataDao
import com.mardous.projectmusic.data.local.database.dao.RankingDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileTagScanner(
    private val rankingDao: RankingDao,
    private val metadataDao: MetadataDao
) {
    data class ScanResult(
        val songsScanned: Int = 0,
        val songsUpdated: Int = 0,
        val errors: Int = 0
    )

    suspend fun scanAll(): ScanResult = withContext(Dispatchers.IO) {
        var result = ScanResult()
        val songs = rankingDao.getAllSongs()
        val metadataMap = rankingDao.getAllSongMetadata().associateBy { it.songKey }

        for (song in songs) {
            try {
                val existing = metadataMap[song.songKey]
                val updated = readFileTags(song.data, existing)
                if (updated != null) {
                    metadataDao.upsertSongMetadata(updated)
                    result = result.copy(songsUpdated = result.songsUpdated + 1)
                }
                result = result.copy(songsScanned = result.songsScanned + 1)
            } catch (e: Exception) {
                result = result.copy(errors = result.errors + 1)
            }
        }
        result
    }

    private fun readFileTags(filePath: String, existing: SongMetadataEntity?): SongMetadataEntity? {
        val file = File(filePath)
        if (!file.exists()) return null

        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val metadata = TagLib.getMetadata(fd.dup().detachFd(), false) ?: return null
            val tags = metadata.propertyMap
            val base = existing ?: return null
            var changed = false
            var m = base

            fun readSingle(key: String): String? =
                tags[key]?.firstOrNull { it.isNotBlank() }

            fun readInt(key: String): Int =
                tags[key]?.firstOrNull()?.let { it.toIntOrNull() } ?: 0

            readSingle(MetadataReaderConsts.COMPOSER)?.let { v ->
                if (m.composer.isNullOrEmpty()) { changed = true; m = m.copy(composer = v) }
            }
            readSingle(MetadataReaderConsts.LYRICIST)?.let { v ->
                if (m.lyricist.isNullOrEmpty()) { changed = true; m = m.copy(lyricist = v) }
            }
            readSingle(MetadataReaderConsts.ARRANGER)?.let { v ->
                if (m.arranger.isNullOrEmpty()) { changed = true; m = m.copy(arranger = v) }
            }
            readSingle(MetadataReaderConsts.PRODUCER)?.let { v ->
                if (m.producer.isNullOrEmpty()) { changed = true; m = m.copy(producer = v) }
            }
            readSingle(MetadataReaderConsts.COPYRIGHT)?.let { v ->
                if (m.copyright.isNullOrEmpty()) { changed = true; m = m.copy(copyright = v) }
            }
            readSingle(MetadataReaderConsts.GENRE)?.let { v ->
                if (m.genre.isNullOrEmpty()) { changed = true; m = m.copy(genre = v) }
            }
            readSingle("CONDUCTOR")?.let { v ->
                if (m.conductor.isNullOrEmpty()) { changed = true; m = m.copy(conductor = v) }
            }
            readSingle("PUBLISHER")?.let { v ->
                if (m.publisher.isNullOrEmpty()) { changed = true; m = m.copy(publisher = v) }
            }
            readSingle("ENGINEER")?.let { v ->
                if (m.engineer.isNullOrEmpty()) { changed = true; m = m.copy(engineer = v) }
            }
            readSingle("ISRC")?.let { v ->
                if (m.isrc.isNullOrEmpty()) { changed = true; m = m.copy(isrc = v) }
            }
            readSingle("LANGUAGE")?.let { v ->
                if (m.language.isNullOrEmpty()) { changed = true; m = m.copy(language = v) }
            }
            readSingle("BPM")?.let { v ->
                v.toIntOrNull()?.let { bpm ->
                    if (m.bpm == 0) { changed = true; m = m.copy(bpm = bpm) }
                }
            }
            readSingle("KEY")?.let { v ->
                if (m.keySignature.isNullOrEmpty()) { changed = true; m = m.copy(keySignature = v) }
            }
            readSingle(MetadataReaderConsts.TRACK_NUMBER)?.let { v ->
                v.toIntOrNull()?.let { tn ->
                    if (m.trackNumber == 0) { changed = true; m = m.copy(trackNumber = tn) }
                }
            }
            readSingle(MetadataReaderConsts.DISC_NUMBER)?.let { v ->
                val dn = v.split("/")[0].toIntOrNull() ?: v.toIntOrNull()
                if (dn != null && m.discNumber == 0) { changed = true; m = m.copy(discNumber = dn) }
            }
            readSingle(MetadataReaderConsts.YEAR)?.let { v ->
                val year = v.take(4).toIntOrNull()
                if (year != null && m.year == 0) { changed = true; m = m.copy(year = year) }
            }

            return if (changed) m else null
        } finally {
            fd.close()
        }
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
