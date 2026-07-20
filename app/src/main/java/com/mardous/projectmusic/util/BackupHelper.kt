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
import com.mardous.projectmusic.data.local.database.dao.LyricsDao
import com.mardous.projectmusic.data.local.database.intel.LyricsEntity
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
    private val statsRepository by inject<StatsRepository>()
    private val jsonFormat = Json { prettyPrint = true }

    suspend fun createBackup(context: Context, uri: Uri?) {
        if (uri == null) return
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        val zipItems = mutableListOf<ZipItem>()
        zipItems.addAll(getPlaylistZipItems(context))
        zipItems.addAll(getSettingsZipItems(context))
        zipItems.addAll(getLyricsZipItems())
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
                            out.bufferedWriter().use { it.write(zipItem.fileContent) }
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
        for (playlist in repository.playlistsWithSongs()) {
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
        return playlistZipItems
    }

    private suspend fun getLyricsZipItems(): List<ZipItem> {
        val allLyrics = lyricsDao.getAllLyrics()
        if (allLyrics.isNotEmpty()) {
            return listOf(ZipItem(LYRICS_PATH.child("lyrics.json"), fileContent = Json.encodeToString(allLyrics)))
        }
        return emptyList()
    }

    private fun getSettingsZipItems(context: Context): List<ZipItem> {
        val sharedPrefPath = File(context.filesDir.parentFile, "shared_prefs")
        return listOf("${context.packageName}_preferences.xml")
            .map {
                ZipItem(SETTINGS_PATH.child(it), File(sharedPrefPath, it).absolutePath)
            }
    }

    private suspend fun getStatsZipItems(): List<ZipItem> {
        val allSessions = runCatching { statsRepository.getAllSessionsAsc() }.getOrDefault(emptyList())
        if (allSessions.isNotEmpty()) {
            val sessionJson = Json.encodeToString(allSessions.map { session ->
                mapOf(
                    "sessionId" to session.sessionId,
                    "sessionGroupId" to session.sessionGroupId,
                    "songId" to session.songId,
                    "artistId" to session.artistId,
                    "songTitle" to session.songTitle,
                    "artistName" to session.artistName,
                    "albumArtist" to session.albumArtist,
                    "albumId" to session.albumId,
                    "albumName" to session.albumName,
                    "genre" to session.genre,
                    "releaseYear" to session.releaseYear,
                    "composer" to session.composer,
                    "lyricist" to session.lyricist,
                    "publisher" to session.publisher,
                    "songDurationMs" to session.songDurationMs,
                    "audioFormat" to session.audioFormat,
                    "audioSampleRate" to session.audioSampleRate,
                    "audioChannelCount" to session.audioChannelCount,
                    "bitrateKbps" to session.bitrateKbps,
                    "playbackSpeed" to session.playbackSpeed,
                    "equalizerActive" to session.equalizerActive,
                    "startTime" to session.startTime,
                    "endTime" to session.endTime,
                    "timeStandard" to session.timeStandard,
                    "timezoneId" to session.timezoneId,
                    "timezoneOffsetMinutes" to session.timezoneOffsetMinutes,
                    "startDate" to session.startDate,
                    "startTimeOnly" to session.startTimeOnly,
                    "dayOfWeek" to session.dayOfWeek,
                    "dayOfMonth" to session.dayOfMonth,
                    "dayOfYear" to session.dayOfYear,
                    "weekOfYear" to session.weekOfYear,
                    "month" to session.month,
                    "monthName" to session.monthName,
                    "quarter" to session.quarter,
                    "year" to session.year,
                    "yearMonth" to session.yearMonth,
                    "yearWeek" to session.yearWeek,
                    "hour" to session.hour,
                    "minute" to session.minute,
                    "second" to session.second,
                    "timePeriod" to session.timePeriod,
                    "isWeekend" to session.isWeekend,
                    "playbackDurationMs" to session.playbackDurationMs,
                    "effectiveListenedMs" to session.effectiveListenedMs,
                    "completionPercent" to session.completionPercent,
                    "endReason" to session.endReason,
                    "pauseCount" to session.pauseCount,
                    "pauseDurationMs" to session.pauseDurationMs,
                    "seekCount" to session.seekCount,
                    "seekForwardCount" to session.seekForwardCount,
                    "seekBackwardCount" to session.seekBackwardCount,
                    "shuffleEnabled" to session.shuffleEnabled,
                    "repeatMode" to session.repeatMode,
                    "queuePosition" to session.queuePosition,
                    "queueSource" to session.queueSource,
                    "playbackOrigin" to session.playbackOrigin,
                    "playlistId" to session.playlistId,
                    "playlistName" to session.playlistName,
                    "isFavorite" to session.isFavorite,
                    "outputDevice" to session.outputDevice,
                    "volumeStart" to session.volumeStart,
                    "volumeEnd" to session.volumeEnd,
                    "batteryLevel" to session.batteryLevel,
                    "charging" to session.charging,
                    "screenOn" to session.screenOn,
                    "appVersion" to session.appVersion
                )
            })
            val json = jsonFormat.encodeToString(ListeningSessionBackup(sessionJson))
            return listOf(ZipItem(STATS_PATH.child("listening_history.json"), fileContent = json))
        }
        return emptyList()
    }

    private fun getCustomArtistZipItems(context: Context): List<ZipItem> {
        val zipItemList = mutableListOf<ZipItem>()
        val sharedPrefPath = File(context.filesDir.parentFile, "shared_prefs")
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

            File(sharedPrefPath, "custom_artist_images.xml").let {
                if (it.exists()) {
                    zipItemList.add(
                        ZipItem(
                            CUSTOM_ARTISTS_PATH.child("prefs").child("custom_artist_images.xml"),
                            it.absolutePath
                        )
                    )
                }
            }
        }
        return zipItemList
    }

    suspend fun restoreBackup(context: Context, uri: Uri?, contents: List<BackupContent>) {
        if (uri == null) return
        withContext(Dispatchers.IO) {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    ZipInputStream(inputStream).use {
                        var entry = it.nextEntry
                        while (entry != null) {
                            if (entry.isPlaylistEntry() && contents.contains(BackupContent.Playlists)) {
                                restorePlaylists(it, entry)
                            } else if (entry.isPreferenceEntry() && contents.contains(BackupContent.Settings)) {
                                restorePreferences(context, it, entry)
                            } else if (entry.isLyricsEntry() && contents.contains(BackupContent.Lyrics)) {
                                restoreLyrics(it)
                    } else if (entry.isCustomArtistEntry() && contents.contains(BackupContent.ArtistImages)) {
                        if (entry.isCustomArtistPrefEntry()) {
                            restoreCustomArtistPrefs(context, it, entry)
                        } else if (entry.isCustomArtistImageEntry()) {
                            restoreCustomArtistImages(context, it, entry)
                        }
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
                }
            }
        }
    }

    private suspend fun restoreLyrics(zipIn: ZipInputStream) {
        val serializedLyrics = zipIn.bufferedReader().readText()
        val lyrics = Json.decodeFromString<List<LyricsEntity>>(serializedLyrics)
        lyricsDao.insertLyrics(lyrics)
    }

    private fun restorePreferences(context: Context, zipIn: ZipInputStream, zipEntry: ZipEntry) {
        val file = File(context.filesDir.parent!! + File.separator + "shared_prefs" + File.separator + zipEntry.getFileName())
        if (file.exists()) {
            file.delete()
        }
        file.outputStream().buffered().use { bos ->
            zipIn.copyTo(bos)
        }
    }

    private suspend fun restorePlaylists(zipIn: ZipInputStream, zipEntry: ZipEntry) {
        val playlistName = zipEntry.getFileName().substringBeforeLast(".")
        val songs = mutableListOf<Song>()

        // Get songs from m3u playlist files
        zipIn.bufferedReader().lineSequence().forEach { line ->
            if (line.startsWith(File.separator)) {
                if (File(line).exists()) {
                    songs.add(repository.songByFilePath(line, true))
                }
            }
        }
        val playlistEntity = repository.checkPlaylistExists(playlistName).firstOrNull()
        if (playlistEntity != null) {
            repository.insertSongsInPlaylist(playlistEntity.playListId, songs)
        } else {
            val playListId = repository.createPlaylist(PlaylistEntity(playlistName = playlistName))
            repository.insertSongsInPlaylist(playListId, songs)
        }
    }

    private fun restoreCustomArtistImages(
        context: Context,
        zipIn: ZipInputStream,
        zipEntry: ZipEntry
    ) {
        val parentFolder = File(context.filesDir, "custom_artist_images")
        if (!parentFolder.exists()) {
            parentFolder.mkdirs()
        }
        val file = File(parentFolder, zipEntry.getFileName())
        file.outputStream().buffered().use { bos ->
            zipIn.copyTo(bos)
        }
    }

    private fun restoreCustomArtistPrefs(
        context: Context,
        zipIn: ZipInputStream,
        zipEntry: ZipEntry
    ) {
        val file = File(context.filesDir.parentFile, "shared_prefs".child(zipEntry.getFileName()))
        file.outputStream().buffered().use { bos ->
            zipIn.copyTo(bos)
        }
    }

    private suspend fun restoreStats(zipIn: ZipInputStream) {
        val json = zipIn.bufferedReader().readText()
        val backup = Json.decodeFromString<ListeningSessionBackup>(json)
        val sessionArray = Json.parseToJsonElement(backup.sessionsJson).jsonArray
        val sessions = sessionArray.map { element ->
            val obj = element.jsonObject
            ListeningSessionEntity(
                sessionId = obj["sessionId"]?.jsonPrimitive?.long ?: 0,
                sessionGroupId = obj["sessionGroupId"]?.jsonPrimitive?.content ?: "",
                songId = obj["songId"]?.jsonPrimitive?.long ?: 0,
                artistId = obj["artistId"]?.jsonPrimitive?.long ?: -1,
                songTitle = obj["songTitle"]?.jsonPrimitive?.content ?: "",
                artistName = obj["artistName"]?.jsonPrimitive?.content ?: "",
                albumArtist = obj["albumArtist"]?.jsonPrimitive?.contentOrNull,
                albumId = obj["albumId"]?.jsonPrimitive?.long ?: 0,
                albumName = obj["albumName"]?.jsonPrimitive?.content ?: "",
                genre = obj["genre"]?.jsonPrimitive?.contentOrNull,
                releaseYear = obj["releaseYear"]?.jsonPrimitive?.int ?: 0,
                composer = obj["composer"]?.jsonPrimitive?.contentOrNull,
                lyricist = obj["lyricist"]?.jsonPrimitive?.contentOrNull,
                publisher = obj["publisher"]?.jsonPrimitive?.contentOrNull,
                songDurationMs = obj["songDurationMs"]?.jsonPrimitive?.long ?: 0,
                audioFormat = obj["audioFormat"]?.jsonPrimitive?.content ?: "",
                audioSampleRate = obj["audioSampleRate"]?.jsonPrimitive?.int ?: 0,
                audioChannelCount = obj["audioChannelCount"]?.jsonPrimitive?.int ?: 0,
                bitrateKbps = obj["bitrateKbps"]?.jsonPrimitive?.int ?: 0,
                playbackSpeed = obj["playbackSpeed"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 1.0f,
                equalizerActive = obj["equalizerActive"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                startTime = obj["startTime"]?.jsonPrimitive?.long ?: 0,
                endTime = obj["endTime"]?.jsonPrimitive?.long ?: 0,
                timeStandard = obj["timeStandard"]?.jsonPrimitive?.content ?: "UTC",
                timezoneId = obj["timezoneId"]?.jsonPrimitive?.content ?: "",
                timezoneOffsetMinutes = obj["timezoneOffsetMinutes"]?.jsonPrimitive?.int ?: 0,
                startDate = obj["startDate"]?.jsonPrimitive?.content ?: "",
                startTimeOnly = obj["startTimeOnly"]?.jsonPrimitive?.content ?: "",
                dayOfWeek = obj["dayOfWeek"]?.jsonPrimitive?.content ?: "",
                dayOfMonth = obj["dayOfMonth"]?.jsonPrimitive?.int ?: 0,
                dayOfYear = obj["dayOfYear"]?.jsonPrimitive?.int ?: 0,
                weekOfYear = obj["weekOfYear"]?.jsonPrimitive?.int ?: 0,
                month = obj["month"]?.jsonPrimitive?.int ?: 0,
                monthName = obj["monthName"]?.jsonPrimitive?.content ?: "",
                quarter = obj["quarter"]?.jsonPrimitive?.int ?: 0,
                year = obj["year"]?.jsonPrimitive?.int ?: 0,
                yearMonth = obj["yearMonth"]?.jsonPrimitive?.content ?: "",
                yearWeek = obj["yearWeek"]?.jsonPrimitive?.content ?: "",
                hour = obj["hour"]?.jsonPrimitive?.int ?: 0,
                minute = obj["minute"]?.jsonPrimitive?.int ?: 0,
                second = obj["second"]?.jsonPrimitive?.int ?: 0,
                timePeriod = obj["timePeriod"]?.jsonPrimitive?.content ?: "",
                isWeekend = obj["isWeekend"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                playbackDurationMs = obj["playbackDurationMs"]?.jsonPrimitive?.long ?: 0,
                effectiveListenedMs = obj["effectiveListenedMs"]?.jsonPrimitive?.long ?: 0,
                completionPercent = obj["completionPercent"]?.jsonPrimitive?.double ?: 0.0,
                endReason = obj["endReason"]?.jsonPrimitive?.content ?: "",
                pauseCount = obj["pauseCount"]?.jsonPrimitive?.int ?: 0,
                pauseDurationMs = obj["pauseDurationMs"]?.jsonPrimitive?.long ?: 0,
                seekCount = obj["seekCount"]?.jsonPrimitive?.int ?: 0,
                seekForwardCount = obj["seekForwardCount"]?.jsonPrimitive?.int ?: 0,
                seekBackwardCount = obj["seekBackwardCount"]?.jsonPrimitive?.int ?: 0,
                shuffleEnabled = obj["shuffleEnabled"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                repeatMode = obj["repeatMode"]?.jsonPrimitive?.content ?: "off",
                queuePosition = obj["queuePosition"]?.jsonPrimitive?.int ?: -1,
                queueSource = obj["queueSource"]?.jsonPrimitive?.content ?: "",
                playbackOrigin = obj["playbackOrigin"]?.jsonPrimitive?.content ?: "unknown",
                playlistId = obj["playlistId"]?.jsonPrimitive?.content ?: "",
                playlistName = obj["playlistName"]?.jsonPrimitive?.content ?: "",
                isFavorite = obj["isFavorite"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                outputDevice = obj["outputDevice"]?.jsonPrimitive?.content ?: "",
                volumeStart = obj["volumeStart"]?.jsonPrimitive?.int ?: -1,
                volumeEnd = obj["volumeEnd"]?.jsonPrimitive?.int ?: -1,
                batteryLevel = obj["batteryLevel"]?.jsonPrimitive?.int ?: -1,
                charging = obj["charging"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                screenOn = obj["screenOn"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true,
                appVersion = obj["appVersion"]?.jsonPrimitive?.content ?: ""
            )
        }
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
    private const val STATS_PATH = "stats"

    private fun ZipEntry.isPlaylistEntry(): Boolean {
        return name.startsWith(PLAYLISTS_PATH)
    }

    private fun ZipEntry.isPreferenceEntry(): Boolean {
        return name.startsWith(SETTINGS_PATH)
    }

    private fun ZipEntry.isLyricsEntry(): Boolean {
        return name.startsWith(LYRICS_PATH)
    }

    private fun ZipEntry.isCustomArtistEntry(): Boolean {
        return name.startsWith(CUSTOM_ARTISTS_PATH)
    }

    private fun ZipEntry.isCustomArtistImageEntry(): Boolean {
        return name.startsWith(CUSTOM_ARTISTS_PATH) && name.contains("custom_artist_images")
    }

    private fun ZipEntry.isCustomArtistPrefEntry(): Boolean {
        return name.startsWith(CUSTOM_ARTISTS_PATH) && name.contains("prefs")
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
    ListeningHistory(R.string.backup_listening_history)
}

@Serializable
data class ListeningSessionBackup(
    val sessionsJson: String
)
