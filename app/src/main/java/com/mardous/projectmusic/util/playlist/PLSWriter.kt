package com.mardous.projectmusic.util.playlist

import com.mardous.projectmusic.data.local.database.metadata.PlaylistWithSongs
import com.mardous.projectmusic.data.mapper.toSongs
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

object PLSWriter {

    fun write(outputStream: OutputStream, playlist: PlaylistWithSongs) {
        val songs = playlist.songs.toSongs()
        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
            writer.write("[playlist]")
            writer.newLine()
            writer.write("NumberOfEntries=${songs.size}")
            writer.newLine()
            
            songs.forEachIndexed { index, song ->
                val entryIndex = index + 1
                writer.write("File$entryIndex=${song.data}")
                writer.newLine()
                writer.write("Title$entryIndex=${song.artistName} - ${song.title}")
                writer.newLine()
                writer.write("Length$entryIndex=${song.duration / 1000}")
                writer.newLine()
            }
            
            writer.write("Version=2")
            writer.newLine()
            writer.flush()
        }
    }
}
