package com.mardous.projectmusic.ui.screen.librarystats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.data.local.repository.Repository
import com.mardous.projectmusic.data.local.repository.SongRepository
import com.mardous.projectmusic.data.local.repository.StatsRepository
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.network.LoginState
import com.mardous.projectmusic.data.model.network.ScrobblingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DistributionItem(
    val name: String,
    val count: Int
)

data class LibraryStatsUiState(
    val totalSongs: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L,
    val uniqueArtists: Int = 0,
    val uniqueAlbums: Int = 0,
    val uniqueGenres: Int = 0,

    val genreDistribution: List<DistributionItem> = emptyList(),
    val artistConcentrationInsight: String = "",
    val trackLengthDistribution: List<DistributionItem> = emptyList(),
    val decadeDistribution: List<DistributionItem> = emptyList(),
    val albumSizeDistribution: List<DistributionItem> = emptyList(),
    val alphabeticalDistribution: List<DistributionItem> = emptyList(),

    val formatDistribution: List<DistributionItem> = emptyList(),
    val bitrateDistribution: List<DistributionItem> = emptyList(),
    val fileSizeDistribution: List<DistributionItem> = emptyList(),
    val largestFoldersDistribution: List<DistributionItem> = emptyList(),

    val unplayedDistribution: List<DistributionItem> = emptyList(),
    val metadataHealthDistribution: List<DistributionItem> = emptyList(),
    val orphanedTracksCount: Int = 0,

    val lastFmTopArtists: List<DistributionItem> = emptyList(),
    val lastFmTopTracks: List<DistributionItem> = emptyList(),
    val isLastFmLoggedIn: Boolean = false,

    val isLoading: Boolean = true
)

class LibraryStatsViewModel(
    private val songRepository: SongRepository,
    private val statsRepository: StatsRepository,
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryStatsUiState())
    val uiState: StateFlow<LibraryStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            withContext(Dispatchers.IO) {
                val songs = try { songRepository.songs() } catch (e: Exception) { emptyList<Song>() }
                val playedSongIds = try {
                    statsRepository.getTopSongs(Int.MAX_VALUE).map { it.songId }.toSet()
                } catch (e: Exception) {
                    emptySet<Long>()
                }

                var totalDurationMs = 0L
                var totalSizeBytes = 0L

                val genreGroups = mutableMapOf<String, Int>()
                val artistGroups = mutableMapOf<String, Int>()
                val albumGroups = mutableMapOf<Long, Int>()
                val trackLengthGroups = mutableMapOf<String, Int>()
                val decadeGroups = mutableMapOf<String, Int>()
                val alphaGroups = mutableMapOf<String, Int>()
                val formatGroups = mutableMapOf<String, Int>()
                val bitrateGroups = mutableMapOf<String, Int>()
                val sizeGroups = mutableMapOf<String, Int>()
                val folderGroups = mutableMapOf<String, Int>()
                val metadataGroups = mutableMapOf<String, Int>()
                var playedCount = 0
                var unplayedCount = 0

                val uniqueGenresSet = mutableSetOf<String>()

                for (song in songs) {
                    totalDurationMs += song.duration
                    val size = song.size
                    totalSizeBytes += size

                    // Core: Genre
                    val genre = song.genreName.takeIf { !it.isNullOrBlank() } ?: "Unknown"
                    genreGroups[genre] = genreGroups.getOrDefault(genre, 0) + 1
                    if (genre != "Unknown") uniqueGenresSet.add(genre)

                    // Core: Artist
                    val artist = song.artistName.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown"
                    artistGroups[artist] = artistGroups.getOrDefault(artist, 0) + 1

                    // Core: Album Size
                    albumGroups[song.albumId] = albumGroups.getOrDefault(song.albumId, 0) + 1

                    // Core: Track Length
                    val durationMins = song.duration / 60000.0
                    val lengthCategory = when {
                        durationMins < 2.0 -> "< 2 mins"
                        durationMins < 3.0 -> "2-3 mins"
                        durationMins < 4.0 -> "3-4 mins"
                        durationMins < 5.0 -> "4-5 mins"
                        else -> "5+ mins"
                    }
                    trackLengthGroups[lengthCategory] = trackLengthGroups.getOrDefault(lengthCategory, 0) + 1

                    // Core: Decade
                    val year = song.year
                    if (year in 1900..2100) {
                        val decade = (year / 10) * 10
                        val decadeStr = "${decade}s"
                        decadeGroups[decadeStr] = decadeGroups.getOrDefault(decadeStr, 0) + 1
                    }

                    // Core: Alphabetical
                    val firstChar = artist.firstOrNull()?.uppercaseChar()
                    if (firstChar != null && firstChar in 'A'..'Z') {
                        val charStr = firstChar.toString()
                        alphaGroups[charStr] = alphaGroups.getOrDefault(charStr, 0) + 1
                    } else {
                        alphaGroups["#"] = alphaGroups.getOrDefault("#", 0) + 1
                    }

                    // Audiophile: Format
                    val format = song.data.substringAfterLast('.', "").uppercase().takeIf { it.isNotEmpty() } ?: "UNKNOWN"
                    formatGroups[format] = formatGroups.getOrDefault(format, 0) + 1

                    // Audiophile: Bitrate
                    val bitrateKbps = song.bitrate / 1000
                    val bitrateCategory = when {
                        bitrateKbps <= 0 -> "Unknown"
                        bitrateKbps < 192 -> "< 192 kbps (Low)"
                        bitrateKbps < 320 -> "192-256 kbps (Standard)"
                        bitrateKbps == 320 -> "320 kbps (High)"
                        else -> "> 320 kbps (Lossless)"
                    }
                    bitrateGroups[bitrateCategory] = bitrateGroups.getOrDefault(bitrateCategory, 0) + 1

                    // Audiophile: File Size
                    val sizeCategory = when {
                        size < 5 * 1024 * 1024 -> "< 5 MB"
                        size < 10 * 1024 * 1024 -> "5 - 10 MB"
                        size < 20 * 1024 * 1024 -> "10 - 20 MB"
                        else -> "> 20 MB"
                    }
                    sizeGroups[sizeCategory] = sizeGroups.getOrDefault(sizeCategory, 0) + 1

                    // Storage: Folders
                    val folderPath = try {
                        val path = song.data
                        if (path.contains('/')) {
                            path.substringBeforeLast('/').substringAfterLast('/')
                        } else {
                            "Unknown"
                        }
                    } catch (e: Exception) { "Unknown" }
                    folderGroups[folderPath] = folderGroups.getOrDefault(folderPath, 0) + 1

                    // Health: Metadata
                    if (song.albumName.isBlank() || song.albumName == "<unknown>") metadataGroups["Missing Album"] = metadataGroups.getOrDefault("Missing Album", 0) + 1
                    if (artist == "Unknown") metadataGroups["Missing Artist"] = metadataGroups.getOrDefault("Missing Artist", 0) + 1
                    if (year == 0) metadataGroups["Missing Year"] = metadataGroups.getOrDefault("Missing Year", 0) + 1

                    // Health: Played
                    if (playedSongIds.contains(song.id)) {
                        playedCount++
                    } else {
                        unplayedCount++
                    }
                }

                // Process Album Sizes
                val albumSizeCategories = mutableMapOf<String, Int>()
                for (count in albumGroups.values) {
                    val category = when (count) {
                        1 -> "Singles (1)"
                        in 2..6 -> "EPs (2-6)"
                        in 7..15 -> "Albums (7-15)"
                        else -> "Compilations (16+)"
                    }
                    albumSizeCategories[category] = albumSizeCategories.getOrDefault(category, 0) + 1
                }

                // Artist Concentration (Pareto)
                val sortedArtists = artistGroups.values.sortedDescending()
                val totalArtistCount = sortedArtists.size
                var cumulativeSongs = 0
                var paretoInsight = "Your library is highly diverse."
                if (totalArtistCount > 0 && songs.isNotEmpty()) {
                    val targetSongs = (songs.size * 0.8).toInt()
                    var artistsFor80Percent = 0
                    for (count in sortedArtists) {
                        cumulativeSongs += count
                        artistsFor80Percent++
                        if (cumulativeSongs >= targetSongs) break
                    }
                    val percentArtists = (artistsFor80Percent.toFloat() / totalArtistCount.toFloat()) * 100
                    paretoInsight = "${percentArtists.toInt()}% of your artists make up 80% of your entire library."
                }

                // Orphaned Tracks
                val orphanedCount = artistGroups.count { it.value == 1 }

                val lastFmLoggedIn = repository.getCurrentLoginState(ScrobblingService.Lastfm) is LoginState.LoggedIn
                val lastFmTopArtists = if (lastFmLoggedIn) {
                    repository.lastFmUserTopArtists("overall", 5)?.topartists?.artist?.map {
                        DistributionItem(it.name, it.playcount.toIntOrNull() ?: 0)
                    } ?: emptyList()
                } else emptyList()

                val lastFmTopTracks = if (lastFmLoggedIn) {
                    repository.lastFmUserTopTracks("overall", 5)?.toptracks?.track?.map {
                        DistributionItem("${it.name} - ${it.artist.name}", it.playcount.toIntOrNull() ?: 0)
                    } ?: emptyList()
                } else emptyList()

                _uiState.update {
                    it.copy(
                        totalSongs = songs.size,
                        totalDurationMs = totalDurationMs,
                        totalSizeBytes = totalSizeBytes,
                        uniqueArtists = artistGroups.size,
                        uniqueAlbums = albumGroups.size,
                        uniqueGenres = uniqueGenresSet.size,

                        genreDistribution = genreGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.count },
                        artistConcentrationInsight = paretoInsight,
                        trackLengthDistribution = trackLengthGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.count },
                        decadeDistribution = decadeGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.name },
                        albumSizeDistribution = albumSizeCategories.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.count },
                        alphabeticalDistribution = alphaGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedBy { item -> item.name },

                        formatDistribution = formatGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.count },
                        bitrateDistribution = bitrateGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.count },
                        fileSizeDistribution = sizeGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.count },
                        largestFoldersDistribution = folderGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.count }.take(10),

                        unplayedDistribution = listOf(
                            DistributionItem("Played", playedCount),
                            DistributionItem("Never Played", unplayedCount)
                        ).filter { item -> item.count > 0 },
                        metadataHealthDistribution = metadataGroups.map { entry -> DistributionItem(entry.key, entry.value) }.sortedByDescending { item -> item.count },
                        orphanedTracksCount = orphanedCount,

                        isLastFmLoggedIn = lastFmLoggedIn,
                        lastFmTopArtists = lastFmTopArtists,
                        lastFmTopTracks = lastFmTopTracks,

                        isLoading = false
                    )
                }
            }
        }
    }
}