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

object M3UReader : KoinComponent {

    suspend fun read(
        context: Context,
        uri: Uri,
        librarySongs: List<Song>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<Song> {
        val songs = mutableListOf<Song>()
        
        // Build in-memory indexes for hyper-speed matching
        val pathToSongMap = librarySongs.associateBy { it.data }
        val idToSongMap = librarySongs.associateBy { it.id }
        val titleArtistToSongMap = librarySongs.associateBy { "${it.title.lowercase()}|${it.artistName.lowercase()}" }
        val titleToSongMap = librarySongs.associateBy { it.title.lowercase() }
        
        try {
            // Pre-count entries for progress (approximate by counting lines that aren't empty/headers)
            var totalEntries = 0
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    while (reader.readLine() != null) {
                        totalEntries++
                    }
                }
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var lineCount = 0
                    var lastTitle = ""
                    var lastArtist = ""
                    
                    while (reader.readLine().also { line = it } != null) {
                        lineCount++
                        if (lineCount > 20000) {
                            Log.w("M3UReader", "Playlist file too large, stopping at 20,000 lines")
                            break
                        }
                        
                        onProgress?.invoke(lineCount, totalEntries)
                        
                        val trimmedLine = line?.trim() ?: continue
                        if (trimmedLine.isEmpty()) continue
                        
                        if (trimmedLine.startsWith("#EXTINF:", ignoreCase = true)) {
                            val info = trimmedLine.substringAfter(",")
                            if (info.contains(" - ")) {
                                lastArtist = info.substringBefore(" - ").trim()
                                lastTitle = info.substringAfter(" - ").trim()
                            } else {
                                lastTitle = info.trim()
                                lastArtist = ""
                            }
                            continue
                        }
                        
                        if (trimmedLine.startsWith("#")) continue
                        
                        // Hyper-speed match in memory
                        var song: Song? = pathToSongMap[trimmedLine]
                        
                        if (song == null && trimmedLine.startsWith("content://")) {
                            val id = trimmedLine.substringAfterLast("/").toLongOrNull()
                            if (id != null) {
                                song = idToSongMap[id]
                            }
                        }
                        
                        if (song == null && lastTitle.isNotEmpty()) {
                            // Fallback to in-memory fuzzy match
                            val key = "${lastTitle.lowercase()}|${lastArtist.lowercase()}"
                            song = titleArtistToSongMap[key] ?: titleToSongMap[lastTitle.lowercase()]
                        }
                        
                        if (song != null) {
                            songs.add(song)
                        }
                        
                        lastTitle = ""
                        lastArtist = ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("M3UReader", "Error reading M3U file: ${uri}", e)
        }
        return songs
    }
}
