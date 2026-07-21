package com.mardous.projectmusic.util.playlist

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mardous.projectmusic.data.local.database.metadata.PlaylistWithSongs
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.util.m3u.M3UWriter
import java.io.OutputStream

object PlaylistFileIO {

    suspend fun read(
        context: Context,
        uri: Uri,
        librarySongs: List<Song>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<Song> {
        val extension = context.contentResolver.getType(uri)?.substringAfterLast("/") 
            ?: uri.path?.substringAfterLast(".")?.lowercase()
        
        Log.d("PlaylistFileIO", "Attempting to read playlist from URI: $uri, detected extension: $extension")
        
        // Safety check for file size (e.g. don't try to parse 100MB files as playlists)
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length > 5 * 1024 * 1024) { // 5MB limit for a text playlist
                    Log.w("PlaylistFileIO", "File too large to be a playlist: ${afd.length} bytes")
                    return emptyList()
                }
            }
        } catch (e: Exception) {
            Log.w("PlaylistFileIO", "Could not check file size", e)
        }

        val songs = when {
            extension == "json" || uri.path?.endsWith(".json", true) == true -> JSONPlaylistIO.read(context, uri, librarySongs, onProgress)
            extension == "m3u" || uri.path?.endsWith(".m3u", true) == true || uri.path?.endsWith(".m3u8", true) == true -> M3UReader.read(context, uri, librarySongs, onProgress)
            extension == "pls" || uri.path?.endsWith(".pls", true) == true -> PLSReader.read(context, uri, librarySongs, onProgress)
            else -> {
                Log.d("PlaylistFileIO", "Unknown extension, trying all readers")
                val m3uSongs = M3UReader.read(context, uri, librarySongs, onProgress)
                if (m3uSongs.isNotEmpty()) return m3uSongs
                val plsSongs = PLSReader.read(context, uri, librarySongs, onProgress)
                if (plsSongs.isNotEmpty()) return plsSongs
                JSONPlaylistIO.read(context, uri, librarySongs, onProgress)
            }
        }
        Log.d("PlaylistFileIO", "Found ${songs.size} songs in playlist")
        return songs
    }

    fun write(context: Context, uri: Uri, playlist: PlaylistWithSongs, format: String) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            when (format.lowercase()) {
                "json" -> JSONPlaylistIO.write(outputStream, playlist)
                "pls" -> PLSWriter.write(outputStream, playlist)
                "m3u" -> M3UWriter.writeToStream(outputStream, playlist)
            }
        }
    }
}
