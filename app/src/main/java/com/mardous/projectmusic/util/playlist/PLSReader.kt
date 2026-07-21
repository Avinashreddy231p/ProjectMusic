package com.mardous.projectmusic.util.playlist

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mardous.projectmusic.data.local.repository.SongRepository
import com.mardous.projectmusic.data.model.Song
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

object PLSReader : KoinComponent {

    suspend fun read(
        context: Context,
        uri: Uri,
        librarySongs: List<Song>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<Song> {
        val songs = mutableListOf<Song>()
        
        // Build in-memory indexes
        val pathToSongMap = librarySongs.associateBy { it.data }
        val titleArtistToSongMap = librarySongs.associateBy { "${it.title.lowercase()}|${it.artistName.lowercase()}" }
        val titleToSongMap = librarySongs.associateBy { it.title.lowercase() }
        
        try {
            val properties = Properties()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                properties.load(inputStream)
            }

            val numberOfEntries = properties.getProperty("NumberOfEntries")?.toIntOrNull() ?: 0
            if (numberOfEntries > 1000) {
                 Log.w("PLSReader", "Too many entries in PLS file: $numberOfEntries")
            }
            
            for (i in 1..numberOfEntries.coerceAtMost(5000)) {
                onProgress?.invoke(i, numberOfEntries)
                val filePath = properties.getProperty("File$i") ?: continue
                var song = matchInMemory(filePath, pathToSongMap, librarySongs)
                
                if (song == null) {
                    val title = properties.getProperty("Title$i")
                    if (!title.isNullOrEmpty()) {
                        val artist = if (title.contains(" - ")) title.substringBefore(" - ").trim() else ""
                        val trackTitle = if (title.contains(" - ")) title.substringAfter(" - ").trim() else title.trim()
                        val key = "${trackTitle.lowercase()}|${artist.lowercase()}"
                        song = titleArtistToSongMap[key] ?: titleToSongMap[trackTitle.lowercase()]
                    }
                }
                
                if (song != null) {
                    songs.add(song)
                }
            }
            
            if (songs.isEmpty()) {
                // Manual parsing fallback
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String?
                        val rawEntries = mutableMapOf<Int, String>()
                        val rawTitles = mutableMapOf<Int, String>()
                        
                        while (reader.readLine().also { line = it } != null) {
                            val trimmedLine = line?.trim() ?: continue
                            if (trimmedLine.contains("=")) {
                                val key = trimmedLine.substringBefore("=").trim()
                                val value = trimmedLine.substringAfter("=").trim()
                                
                                when {
                                    key.startsWith("File", true) -> {
                                        val index = key.substring(4).toIntOrNull()
                                        if (index != null) rawEntries[index] = value
                                    }
                                    key.startsWith("Title", true) -> {
                                        val index = key.substring(5).toIntOrNull()
                                        if (index != null) rawTitles[index] = value
                                    }
                                }
                            }
                        }
                        
                        rawEntries.keys.sorted().forEach { index ->
                            val path = rawEntries[index] ?: return@forEach
                            var song = matchInMemory(path, pathToSongMap, librarySongs)
                            
                            if (song == null) {
                                val title = rawTitles[index]
                                if (!title.isNullOrEmpty()) {
                                    val artist = if (title.contains(" - ")) title.substringBefore(" - ").trim() else ""
                                    val trackTitle = if (title.contains(" - ")) title.substringAfter(" - ").trim() else title.trim()
                                    val key = "${trackTitle.lowercase()}|${artist.lowercase()}"
                                    song = titleArtistToSongMap[key] ?: titleToSongMap[trackTitle.lowercase()]
                                }
                            }
                            
                            if (song != null) {
                                songs.add(song)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PLSReader", "Error reading PLS file: ${uri}", e)
        }
        
        return songs
    }

    private fun matchInMemory(path: String, pathToSongMap: Map<String, Song>, librarySongs: List<Song>): Song? {
        return pathToSongMap[path] ?: if (path.startsWith("content://")) {
            val id = path.substringAfterLast("/").toLongOrNull()
            librarySongs.find { it.id == id }
        } else null
    }
}
