package com.mardous.booming.ui.screen.librarystats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.local.repository.SongRepository
import com.mardous.booming.data.local.repository.StatsRepository
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LibraryStatsDetailViewModel(
    private val category: String,
    private val itemName: String,
    private val songRepository: SongRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _songs = MutableLiveData<List<Song>>()

    init {
        loadSongs()
    }

    fun getSongs(): LiveData<List<Song>> = _songs

    fun loadSongs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val allSongs = try { songRepository.songs() } catch (e: Exception) { emptyList<Song>() }
                val playedSongIds = try {
                    statsRepository.getTopSongs(Int.MAX_VALUE).map { it.songId }.toSet()
                } catch (e: Exception) {
                    emptySet<Long>()
                }

                // Album sizes need pre-calculation if category is "Album Size"
                val albumGroups = mutableMapOf<Long, Int>()
                if (category == "Album Size") {
                    for (song in allSongs) {
                        albumGroups[song.albumId] = albumGroups.getOrDefault(song.albumId, 0) + 1
                    }
                }
                
                // Artist groups for Orphaned Tracks
                val artistGroups = mutableMapOf<String, Int>()
                if (category == "Orphaned Tracks") {
                    for (song in allSongs) {
                        val artist = song.artistName.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown"
                        artistGroups[artist] = artistGroups.getOrDefault(artist, 0) + 1
                    }
                }

                val filteredList = allSongs.filter { song ->
                    when (category) {
                        "Genre" -> {
                            val genre = song.genreName.takeIf { !it.isNullOrBlank() } ?: "Unknown"
                            genre == itemName
                        }
                        "Track Length" -> {
                            val durationMins = song.duration / 60000.0
                            val lengthCategory = when {
                                durationMins < 2.0 -> "< 2 mins"
                                durationMins < 3.0 -> "2-3 mins"
                                durationMins < 4.0 -> "3-4 mins"
                                durationMins < 5.0 -> "4-5 mins"
                                else -> "5+ mins"
                            }
                            lengthCategory == itemName
                        }
                        "Decade" -> {
                            val year = song.year
                            if (year in 1900..2100) {
                                val decade = (year / 10) * 10
                                val decadeStr = "${decade}s"
                                decadeStr == itemName
                            } else false
                        }
                        "Album Size" -> {
                            val count = albumGroups.getOrDefault(song.albumId, 0)
                            val cat = when (count) {
                                1 -> "Singles (1)"
                                in 2..6 -> "EPs (2-6)"
                                in 7..15 -> "Albums (7-15)"
                                else -> "Compilations (16+)"
                            }
                            cat == itemName
                        }
                        "Alphabetical" -> {
                            val artist = song.artistName.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown"
                            val firstChar = artist.firstOrNull()?.uppercaseChar()
                            val charStr = if (firstChar != null && firstChar in 'A'..'Z') firstChar.toString() else "#"
                            charStr == itemName
                        }
                        "Format" -> {
                            val format = song.data.substringAfterLast('.', "").uppercase().takeIf { it.isNotEmpty() } ?: "UNKNOWN"
                            format == itemName
                        }
                        "Bitrate" -> {
                            val bitrateKbps = song.bitrate / 1000
                            val bitrateCategory = when {
                                bitrateKbps <= 0 -> "Unknown"
                                bitrateKbps < 192 -> "< 192 kbps (Low)"
                                bitrateKbps < 320 -> "192-256 kbps (Standard)"
                                bitrateKbps == 320 -> "320 kbps (High)"
                                else -> "> 320 kbps (Lossless)"
                            }
                            bitrateCategory == itemName
                        }
                        "File Size" -> {
                            val size = song.size
                            val sizeCategory = when {
                                size < 5 * 1024 * 1024 -> "< 5 MB"
                                size < 10 * 1024 * 1024 -> "5 - 10 MB"
                                size < 20 * 1024 * 1024 -> "10 - 20 MB"
                                else -> "> 20 MB"
                            }
                            sizeCategory == itemName
                        }
                        "Folder" -> {
                            val folderPath = try {
                                val path = song.data
                                if (path.contains('/')) {
                                    path.substringBeforeLast('/').substringAfterLast('/')
                                } else {
                                    "Unknown"
                                }
                            } catch (e: Exception) { "Unknown" }
                            folderPath == itemName
                        }
                        "Metadata Health" -> {
                            val artist = song.artistName.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown"
                            when (itemName) {
                                "Missing Album" -> song.albumName.isBlank() || song.albumName == "<unknown>"
                                "Missing Artist" -> artist == "Unknown"
                                "Missing Year" -> song.year == 0
                                else -> false
                            }
                        }
                        "Play Activity" -> {
                            if (itemName == "Played") {
                                playedSongIds.contains(song.id)
                            } else {
                                !playedSongIds.contains(song.id)
                            }
                        }
                        "Orphaned Tracks" -> {
                            val artist = song.artistName.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown"
                            artistGroups[artist] == 1
                        }
                        else -> false
                    }
                }

                _songs.postValue(filteredList)
            }
        }
    }
}
