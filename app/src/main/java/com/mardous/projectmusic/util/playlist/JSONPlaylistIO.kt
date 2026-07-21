package com.mardous.projectmusic.util.playlist

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mardous.projectmusic.data.local.database.metadata.PlaylistWithSongs
import com.mardous.projectmusic.data.local.repository.SongRepository
import com.mardous.projectmusic.data.mapper.toSongs
import com.mardous.projectmusic.data.model.Song
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

object JSONPlaylistIO : KoinComponent {

    @Serializable
    private data class PlaylistJson(
        val name: String? = null,
        val songs: List<SongJson>
    )

    @Serializable
    private data class SongJson(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val path: String? = null,
        val filePath: String? = null,
        val uri: String? = null
    )

    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
    }

    suspend fun read(
        context: Context,
        uri: Uri,
        librarySongs: List<Song>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<Song> {
        // Build indexes
        val pathToSongMap = librarySongs.associateBy { it.data }
        val idToSongMap = librarySongs.associateBy { it.id }
        val titleArtistToSongMap = librarySongs.associateBy { "${it.title.lowercase()}|${it.artistName.lowercase()}" }
        
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = InputStreamReader(inputStream).readText()
                val playlistJson = json.decodeFromString<PlaylistJson>(content)
                val total = playlistJson.songs.size
                
                playlistJson.songs.mapIndexedNotNull { index, sj ->
                    onProgress?.invoke(index + 1, total)
                    var song: Song? = null
                    
                    // 1. Match by Path or URI
                    val pathOrUri = sj.uri ?: sj.filePath ?: sj.path
                    if (pathOrUri != null) {
                        song = pathToSongMap[pathOrUri] ?: if (pathOrUri.startsWith("content://")) {
                            val id = pathOrUri.substringAfterLast("/").toLongOrNull()
                            if (id != null) idToSongMap[id] else null
                        } else null
                    }
                    
                    // 2. Match by Title/Artist fallback
                    if (song == null && !sj.title.isNullOrEmpty()) {
                        val key = "${sj.title.lowercase()}|${(sj.artist ?: "").lowercase()}"
                        song = titleArtistToSongMap[key]
                    }
                    
                    song
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("JSONPlaylistIO", "Error reading JSON playlist: ${uri}", e)
            emptyList()
        }
    }

    fun write(outputStream: OutputStream, playlist: PlaylistWithSongs) {
        val songs = playlist.songs.toSongs()
        val playlistJson = PlaylistJson(
            name = playlist.playlistEntity.playlistName,
            songs = songs.map { SongJson(it.title, it.artistName, it.data) }
        )
        val content = json.encodeToString(playlistJson)
        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
            writer.write(content)
            writer.flush()
        }
    }
}
