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

package com.mardous.projectmusic.util

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.local.repository.StatsRepository
import com.mardous.projectmusic.data.local.repository.Repository
import com.mardous.projectmusic.data.local.database.analytics.ListeningSessionEntity
import com.mardous.projectmusic.data.local.database.dao.InclExclDao
import com.mardous.projectmusic.data.local.database.dao.LyricsDao
import com.mardous.projectmusic.data.local.database.dao.MetadataDao
import com.mardous.projectmusic.data.local.database.dao.MetadataWithPath
import com.mardous.projectmusic.data.local.database.intel.LyricsEntity
import com.mardous.projectmusic.data.local.database.metadata.InclExclEntity
import com.mardous.projectmusic.data.local.database.metadata.PlaylistEntity
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.files.zipOutputStream
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.util.m3u.M3UWriter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object BackupHelper : KoinComponent {

    private val repository by inject<Repository>()
    private val lyricsDao by inject<LyricsDao>()
    private val metadataDao by inject<MetadataDao>()
    private val inclExclDao by inject<InclExclDao>()
    private val statsRepository by inject<StatsRepository>()
    private val jsonFormat = Json { prettyPrint = true }

    suspend fun createBackup(context: Context, uri: Uri?) {
        if (uri == null) return
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        val zipItems = mutableListOf<ZipItem>()
        zipItems.addAll(getPlaylistZipItems(context))
        zipItems.addAll(getSettingsZipItems(context))
        zipItems.addAll(getLyricsZipItems())
        zipItems.addAll(getMetadataZipItems())
        zipItems.addAll(getInclExclZipItems())
        zipItems.addAll(getCustomArtistZipItems(context))
        zipItems.addAll(getStatsZipItems())
        zipAll(context, zipItems, outputStream)
        // Clean Cache Playlist Directory
        File(context.filesDir, PLAYLISTS_PATH).deleteRecursively()
    }

    private suspend fun zipAll(context: Context, zipItems: List<ZipItem>, output: OutputStream) =
        withContext(Dispatchers.IO) {
            runCatching {
                output.zipOutputStream().use { out ->
                    out.setComment(context.getString(R.string.app_name))
                    for (zipItem in zipItems) {
                        if (zipItem.filePath != null) {
                            File(zipItem.filePath).inputStream().buffered().use { origin ->
                                val entry = ZipEntry(zipItem.zipPath)
                                out.putNextEntry(entry)
                                origin.copyTo(out)
                            }
                        } else if (!zipItem.fileContent.isNullOrEmpty()) {
                            val entry = ZipEntry(zipItem.zipPath)
                            out.putNextEntry(entry)
                            out.write(zipItem.fileContent.toByteArray())
                        }
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.backup_failed)
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.backup_successful)
                }
            }
        }

    private suspend fun getPlaylistZipItems(context: Context): List<ZipItem> {
        val playlistZipItems = mutableListOf<ZipItem>()
        // Cache Playlist files in App storage
        val playlistFolder = File(context.filesDir, PLAYLISTS_PATH)
        if (!playlistFolder.exists()) {
            playlistFolder.mkdirs()
        }
        val playlistsWithSongs = repository.playlistsWithSongs()
        val playlistEntities = mutableListOf<PlaylistEntity>()

        for (playlist in playlistsWithSongs) {
            playlistEntities.add(playlist.playlistEntity)
            runCatching {
                M3UWriter.writeToDirectory(playlistFolder, playlist)
            }.onSuccess { playlistFile ->
                if (playlistFile.exists()) {
                    playlistZipItems.add(
                        ZipItem(
                            PLAYLISTS_PATH.child(playlistFile.name),
                            playlistFile.absolutePath
                        )
                    )
                }
            }
        }
        if (playlistEntities.isNotEmpty()) {
            playlistZipItems.add(
                ZipItem(
                    PLAYLISTS_PATH.child("playlists_metadata.json"),
                    fileContent = Json.encodeToString(playlistEntities)
                )
            )
        }
        return playlistZipItems
    }

    private suspend fun getLyricsZipItems(): List<ZipItem> {
        val allLyrics = lyricsDao.getAllLyrics()
        if (allLyrics.isNotEmpty()) {
            return listOf(ZipItem(LYRICS_PATH.child("lyrics.json"), fileContent = Json.encodeToString(allLyrics)))
        }
        return emptyList()
    }

    private suspend fun getMetadataZipItems(): List<ZipItem> {
        val allMetadata = metadataDao.getAllMetadataWithPaths()
        if (allMetadata.isNotEmpty()) {
            return listOf(ZipItem(METADATA_PATH.child("metadata.json"), fileContent = Json.encodeToString(allMetadata)))
        }
        return emptyList()
    }

    private suspend fun getInclExclZipItems(): List<ZipItem> {
        val allInclExcl = inclExclDao.getAllInclExcl()
        if (allInclExcl.isNotEmpty()) {
            return listOf(ZipItem(BLACKLIST_PATH.child("blacklist.json"), fileContent = Json.encodeToString(allInclExcl)))
        }
        return emptyList()
    }

    private fun getSettingsZipItems(context: Context): List<ZipItem> {
        val sharedPrefPath = File(context.filesDir.parentFile, "shared_prefs")
        val prefs = listOf(
            "${context.packageName}_preferences.xml",
            "artist_signatures.xml",
            "playback_state.xml",
            "custom_artist_images.xml"
        )
        val zipItems = prefs.filter { File(sharedPrefPath, it).exists() }.map {
            ZipItem(SETTINGS_PATH.child(it), File(sharedPrefPath, it).absolutePath)
        }.toMutableList()

        // DataStore
        val dataStorePath = File(context.filesDir, "datastore")
        if (dataStorePath.exists()) {
            dataStorePath.listFiles()?.forEach {
                zipItems.add(ZipItem(SETTINGS_PATH.child("datastore").child(it.name), it.absolutePath))
            }
        }
        return zipItems
    }

    private suspend fun getStatsZipItems(): List<ZipItem> {
        val allSessions = runCatching { statsRepository.getAllSessionsAsc() }.getOrDefault(emptyList())
        if (allSessions.isNotEmpty()) {
            val json = jsonFormat.encodeToString(allSessions)
            return listOf(ZipItem(STATS_PATH.child("listening_history.json"), fileContent = json))
        }
        return emptyList()
    }

    private fun getCustomArtistZipItems(context: Context): List<ZipItem> {
        val zipItemList = mutableListOf<ZipItem>()
        val customArtistImagesDir = FileUtil.customArtistImagesDirectory()
        if (customArtistImagesDir != null) {
            zipItemList.addAll(
                customArtistImagesDir.listFiles()
                    ?.map {
                        ZipItem(
                            CUSTOM_ARTISTS_PATH.child("custom_artist_images").child(it.name),
                            it.absolutePath
                        )
                    }?.toList() ?: listOf()
            )
        }

        val customPlaylistImagesDir = FileUtil.customPlaylistImagesDirectory()
        if (customPlaylistImagesDir != null) {
            zipItemList.addAll(
                customPlaylistImagesDir.listFiles()
                    ?.map {
                        ZipItem(
                            CUSTOM_PLAYLIST_IMAGES_PATH.child(it.name),
                            it.absolutePath
                        )
                    }?.toList() ?: listOf()
            )
        }
        return zipItemList
    }

    private var restoredPlaylistMetadata = mutableMapOf<String, PlaylistEntity>()

    suspend fun restoreBackup(context: Context, uri: Uri?, contents: List<BackupContent>) {
        if (uri == null) return
        restoredPlaylistMetadata.clear()
        withContext(Dispatchers.IO) {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // First pass: find metadata files
                    ZipInputStream(inputStream).use {
                        var entry = it.nextEntry
                        while (entry != null) {
                            if (entry.name == PLAYLISTS_PATH.child("playlists_metadata.json")) {
                                val json = it.readBytes().decodeToString()
                                Json.decodeFromString<List<PlaylistEntity>>(json).forEach { p ->
                                    restoredPlaylistMetadata[p.playlistName] = p
                                }
                            }
                            entry = it.nextEntry
                        }
                    }

                    // Second pass: restore data
                    val inputStream2 = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException()
                    ZipInputStream(inputStream2).use {
                        var entry = it.nextEntry
                        while (entry != null) {
                            if (entry.isPlaylistEntry() && contents.contains(BackupContent.Playlists)) {
                                if (entry.name != PLAYLISTS_PATH.child("playlists_metadata.json")) {
                                    restorePlaylists(it, entry)
                                }
                            } else if (entry.isPreferenceEntry() && contents.contains(BackupContent.Settings)) {
                                restorePreferences(context, it, entry)
                            } else if (entry.isLyricsEntry() && contents.contains(BackupContent.Lyrics)) {
                                restoreLyrics(it)
                            } else if (entry.isMetadataEntry() && contents.contains(BackupContent.Metadata)) {
                                restoreMetadata(it)
                            } else if (entry.isBlacklistEntry() && contents.contains(BackupContent.Blacklist)) {
                                restoreInclExcl(it)
                            } else if (entry.isCustomArtistEntry() && contents.contains(BackupContent.ArtistImages)) {
                                restoreCustomArtistImages(context, it, entry)
                            } else if (entry.isCustomPlaylistImageEntry() && contents.contains(BackupContent.Playlists)) {
                                restoreCustomPlaylistImages(context, it, entry)
                            } else if (entry.isStatsEntry() && contents.contains(BackupContent.ListeningHistory)) {
                                restoreStats(it)
                            }
                            entry = it.nextEntry
                        }
                    }
                } else {
                    throw FileNotFoundException()
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.could_not_restore_data)
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.data_restored_successfully)
                    if (contents.contains(BackupContent.Settings)) {
                        context.showToast(R.string.restart_app_to_apply_settings)
                    }
                }
            }
        }
    }

    private suspend fun restoreLyrics(zipIn: ZipInputStream) {
        val bytes = zipIn.readBytes()
        val serializedLyrics = bytes.decodeToString()
        val lyrics = Json.decodeFromString<List<LyricsEntity>>(serializedLyrics)
        lyricsDao.insertLyrics(lyrics)
    }

    private fun restorePreferences(context: Context, zipIn: ZipInputStream, zipEntry: ZipEntry) {
        val fileName = zipEntry.getFileName()
        val file = if (zipEntry.name.contains("datastore")) {
            val dataStoreDir = File(context.filesDir, "datastore")
            if (!dataStoreDir.exists()) dataStoreDir.mkdirs()
            File(dataStoreDir, fileName)
        } else {
            File(context.filesDir.parent!! + File.separator + "shared_prefs" + File.separator + fileName)
        }
        if (file.exists()) {
            file.delete()
        }
        file.outputStream().buffered().use { bos ->
            zipIn.copyTo(bos)
        }
    }

    private suspend fun restoreMetadata(zipIn: ZipInputStream) {
        val json = zipIn.readBytes().decodeToString()
        val metadataList = Json.decodeFromString<List<MetadataWithPath>>(json)
        metadataList.forEach { item ->
            val song = repository.songByFilePath(item.data, true)
            if (song != Song.emptySong) {
                metadataDao.upsertSongMetadata(item.metadata.copy(songKey = song.id))
            } else {
                // Fallback: match by filename
                val fileName = item.data.substringAfterLast(File.separator)
                val matchedSong = repository.searchSongs(fileName).firstOrNull { s ->
                    s.data.endsWith(fileName)
                }
                if (matchedSong != null) {
                    metadataDao.upsertSongMetadata(item.metadata.copy(songKey = matchedSong.id))
                }
            }
        }
    }

    private suspend fun restoreInclExcl(zipIn: ZipInputStream) {
        val json = zipIn.readBytes().decodeToString()
        val inclExclList = Json.decodeFromString<List<InclExclEntity>>(json)
        inclExclDao.insertPaths(inclExclList)
    }

    private fun restoreCustomPlaylistImages(
        context: Context,
        zipIn: ZipInputStream,
        zipEntry: ZipEntry
    ) {
        val parentFolder = FileUtil.customPlaylistImagesDirectory() ?: return
        val file = File(parentFolder, zipEntry.getFileName())
        file.outputStream().buffered().use { bos ->
            zipIn.copyTo(bos)
        }
    }

    private suspend fun restorePlaylists(zipIn: ZipInputStream, zipEntry: ZipEntry) {
        val playlistName = zipEntry.getFileName().substringBeforeLast(".")
        val songs = mutableListOf<Song>()

        // Get songs from m3u playlist files
        val lines = zipIn.readBytes().decodeToString().split("\n")
        lines.forEach { line ->
            val path = line.trim()
            if (path.isNotEmpty() && !path.startsWith("#")) {
                val song = repository.songByFilePath(path, true)
                if (song != Song.emptySong) {
                    songs.add(song)
                } else {
                    // Metadata fallback: Try to find by filename if path is different.
                    val fileName = path.substringAfterLast(File.separator)
                    val matchedSong = repository.searchSongs(fileName).firstOrNull { s ->
                        s.data.endsWith(fileName) 
                    }
                    if (matchedSong != null) {
                        songs.add(matchedSong)
                    }
                }
            }
        }
        
        val metadata = restoredPlaylistMetadata[playlistName]
        val playlistEntity = repository.checkPlaylistExists(playlistName).firstOrNull()
        
        if (playlistEntity != null) {
            if (metadata != null) {
                repository.updatePlaylist(playlistEntity.copy(
                    description = metadata.description,
                    customCoverUri = metadata.customCoverUri?.let { fixRestoredUri(it) }
                ))
            }
            repository.insertSongsInPlaylist(playlistEntity.playListId, songs)
        } else {
            val newPlaylist = if (metadata != null) {
                metadata.copy(
                    playListId = 0,
                    customCoverUri = metadata.customCoverUri?.let { fixRestoredUri(it) }
                )
            } else {
                PlaylistEntity(playlistName = playlistName)
            }
            val playListId = repository.createPlaylist(newPlaylist)
            repository.insertSongsInPlaylist(playListId, songs)
        }
    }

    private fun fixRestoredUri(oldUriString: String): String {
        val oldUri = Uri.parse(oldUriString)
        if (oldUri.scheme == "file") {
            val fileName = oldUri.path?.substringAfterLast(File.separator) ?: return oldUriString
            val newDir = FileUtil.customPlaylistImagesDirectory() ?: return oldUriString
            return Uri.fromFile(File(newDir, fileName)).toString()
        }
        return oldUriString
    }

    private fun restoreCustomArtistImages(
        context: Context,
        zipIn: ZipInputStream,
        zipEntry: ZipEntry
    ) {
        val parentFolder = FileUtil.customArtistImagesDirectory() ?: return
        val file = File(parentFolder, zipEntry.getFileName())
        file.outputStream().buffered().use { bos ->
            zipIn.copyTo(bos)
        }
    }

    private suspend fun restoreStats(zipIn: ZipInputStream) {
        val json = zipIn.readBytes().decodeToString()
        val sessions = Json.decodeFromString<List<ListeningSessionEntity>>(json)
        if (sessions.isNotEmpty()) {
            statsRepository.insertSessions(sessions)
        }
    }

    const val BACKUP_EXTENSION = "bmgbak"
    const val APPEND_EXTENSION = ".$BACKUP_EXTENSION"
    private const val PLAYLISTS_PATH = "Playlists"
    private const val SETTINGS_PATH = "prefs"
    private const val LYRICS_PATH = "lyrics"
    private const val CUSTOM_ARTISTS_PATH = "artistImages"
    private const val CUSTOM_PLAYLIST_IMAGES_PATH = "playlistImages"
    private const val STATS_PATH = "stats"
    private const val METADATA_PATH = "metadata"
    private const val BLACKLIST_PATH = "blacklist"

    private fun ZipEntry.isPlaylistEntry(): Boolean {
        return name.startsWith(PLAYLISTS_PATH)
    }

    private fun ZipEntry.isPreferenceEntry(): Boolean {
        return name.startsWith(SETTINGS_PATH)
    }

    private fun ZipEntry.isLyricsEntry(): Boolean {
        return name.startsWith(LYRICS_PATH)
    }

    private fun ZipEntry.isMetadataEntry(): Boolean {
        return name.startsWith(METADATA_PATH)
    }

    private fun ZipEntry.isBlacklistEntry(): Boolean {
        return name.startsWith(BLACKLIST_PATH)
    }

    private fun ZipEntry.isCustomArtistEntry(): Boolean {
        return name.startsWith(CUSTOM_ARTISTS_PATH)
    }

    private fun ZipEntry.isCustomPlaylistImageEntry(): Boolean {
        return name.startsWith(CUSTOM_PLAYLIST_IMAGES_PATH)
    }

    private fun ZipEntry.isStatsEntry(): Boolean {
        return name.startsWith(STATS_PATH)
    }

    private fun ZipEntry.getFileName(): String {
        return name.substring(name.lastIndexOf(File.separator) + 1)
    }
}

data class ZipItem(
    val zipPath: String,
    val filePath: String? = null,
    val fileContent: String? = null
)

fun String.child(child: String): String {
    return this + File.separator + child
}

enum class BackupContent(@StringRes val titleRes: Int) {
    Settings(R.string.backup_settings),
    Lyrics(R.string.backup_synced_lyrics),
    ArtistImages(R.string.backup_artist_images),
    Playlists(R.string.backup_playlists),
    ListeningHistory(R.string.backup_listening_history),
    Metadata(R.string.backup_metadata),
    Blacklist(R.string.backup_blacklist)
}
