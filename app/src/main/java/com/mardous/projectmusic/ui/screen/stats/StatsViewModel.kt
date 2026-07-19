package com.mardous.projectmusic.ui.screen.stats

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.local.repository.MetadataRepository
import com.mardous.projectmusic.data.local.repository.StatsRepository
import com.mardous.projectmusic.data.local.database.dao.ListeningHistoryDao
import com.mardous.projectmusic.data.local.database.analytics.ListeningSessionEntity
import com.mardous.projectmusic.data.model.Instrument
import com.mardous.projectmusic.data.model.Mood
import com.mardous.projectmusic.data.model.Tag
import com.mardous.projectmusic.ui.screen.librarystats.DistributionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsTabUi(
    val totalListens: Long = 0,
    val uniqueTracks: Long = 0,
    val completionRate: Float = 0f,
    val avgPauses: Double = 0.0,
    val peakActivity: List<DistributionItem> = emptyList(),
    val audioProfile: List<DistributionItem> = emptyList()
)

data class TopChartsTabUi(
    val topSongs: List<ListeningHistoryDao.SongStats> = emptyList(),
    val topArtists: List<ListeningHistoryDao.ArtistStats> = emptyList(),
    val topAlbums: List<ListeningHistoryDao.AlbumStats> = emptyList(),
    val topMoods: List<Mood> = emptyList(),
    val topTags: List<Tag> = emptyList(),
    val topInstruments: List<Instrument> = emptyList(),
    val topGenres: List<ListeningHistoryDao.GenreStats> = emptyList()
)

data class LibraryProfileTabUi(
    val moodDistribution: List<DistributionItem> = emptyList(),
    val tagDistribution: List<DistributionItem> = emptyList(),
    val instrumentDistribution: List<DistributionItem> = emptyList(),
    val genreDistribution: List<DistributionItem> = emptyList()
)

data class WrappedCardUi(
    val type: WrappedCardType,
    val title: String,
    val primaryValue: String,
    val subtitle: String,
    val iconEmoji: String
)

enum class WrappedCardType { LISTENING_TIME, TOP_ARTIST, TOP_ALBUM_GENRE, PERSONALITY, MILESTONE }

data class WrappedTabUi(
    val thisMonth: List<WrappedCardUi> = emptyList(),
    val thisYear: List<WrappedCardUi> = emptyList()
)

data class BehaviorTabUi(
    val endReasons: List<DistributionItem> = emptyList(),
    val repeatModes: List<DistributionItem> = emptyList(),
    val isWeekend: List<DistributionItem> = emptyList()
)

data class ContextTabUi(
    val devices: List<DistributionItem> = emptyList(),
    val queueSources: List<DistributionItem> = emptyList(),
    val playbackOrigins: List<DistributionItem> = emptyList(),
    val screenOn: List<DistributionItem> = emptyList()
)

data class StatsUiState(
    val insights: InsightsTabUi = InsightsTabUi(),
    val topCharts: TopChartsTabUi = TopChartsTabUi(),
    val libraryProfile: LibraryProfileTabUi = LibraryProfileTabUi(),
    val behavior: BehaviorTabUi = BehaviorTabUi(),
    val context: ContextTabUi = ContextTabUi(),
    val historySessions: List<ListeningSessionEntity> = emptyList(),
    val totalListeningTimeMs: Long = 0L,
    val wrapped: WrappedTabUi = WrappedTabUi(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false
)

class StatsViewModel(
    application: Application,
    private val statsRepository: StatsRepository,
    private val metadataRepository: MetadataRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        loadHistory()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val now = System.currentTimeMillis()
                val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

                // Insights data
                val summary = statsRepository.getSummary()
                val hourly = statsRepository.getHourlyDistribution(thirtyDaysAgo, now)
                val audioFormats = statsRepository.getAudioFormatDistribution(thirtyDaysAgo, now)
                
                val completionRate = if (summary.totalSessions > 0) {
                    summary.completedCount.toFloat() / summary.totalSessions.toFloat()
                } else 0f
                
                // Group hourly into Time Periods (Morning, Afternoon, Evening, Night)
                var morning = 0; var afternoon = 0; var evening = 0; var night = 0
                hourly.forEach { h ->
                    when (h.hourOfDay) {
                        in 5..11 -> morning += h.playCount
                        in 12..16 -> afternoon += h.playCount
                        in 17..20 -> evening += h.playCount
                        else -> night += h.playCount
                    }
                }
                val peakActivity = listOf(
                    DistributionItem("Morning", morning),
                    DistributionItem("Afternoon", afternoon),
                    DistributionItem("Evening", evening),
                    DistributionItem("Night", night)
                ).filter { it.count > 0 }.sortedByDescending { it.count }

                val avgPauses = statsRepository.getAvgPauseCount(thirtyDaysAgo, now)

                val insightsTab = InsightsTabUi(
                    totalListens = summary.totalSessions,
                    uniqueTracks = summary.uniqueSongs,
                    completionRate = completionRate,
                    avgPauses = avgPauses,
                    peakActivity = peakActivity,
                    audioProfile = audioFormats.map { DistributionItem(it.audioFormat.ifEmpty { "Unknown" }, it.count) }
                )

                // Top Charts
                val topSongs = statsRepository.getTopSongs(limit = 50)
                val topArtists = statsRepository.getTopArtists(limit = 50)
                val topAlbums = statsRepository.getTopAlbums(limit = 50)
                val topGenres = statsRepository.getTopGenres(limit = 50)

                val allMoods = metadataRepository.getAllMoods().first()
                val allTagsMap = metadataRepository.getTagsByCategory().first()
                val allInstrumentsMap = metadataRepository.getInstrumentsByFamily().first()

                val topMoods = allMoods.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(50)
                val topTags = allTagsMap.values.flatten().sortedByDescending { it.songCount }.take(50)
                val topInstruments = allInstrumentsMap.values.flatten().sortedByDescending { it.songCount }.take(50)

                val topChartsTab = TopChartsTabUi(
                    topSongs = topSongs,
                    topArtists = topArtists,
                    topAlbums = topAlbums,
                    topMoods = topMoods,
                    topTags = topTags,
                    topInstruments = topInstruments,
                    topGenres = topGenres
                )

                // Library Profile
                val libraryProfileTab = LibraryProfileTabUi(
                    moodDistribution = allMoods.filter { it.songCount > 0 }
                        .sortedByDescending { it.songCount }
                        .map { DistributionItem("${it.emoji ?: "🎵"} ${it.name}", it.songCount) },
                    tagDistribution = allTagsMap.values.flatten()
                        .filter { it.songCount > 0 }
                        .sortedByDescending { it.songCount }
                        .map { DistributionItem(it.name, it.songCount) },
                    instrumentDistribution = allInstrumentsMap.values.flatten()
                        .filter { it.songCount > 0 }
                        .sortedByDescending { it.songCount }
                        .map { DistributionItem(it.name, it.songCount) },
                    genreDistribution = topGenres.map { DistributionItem(it.genre, it.playCount) }
                )

                // Wrapped & Milestones - Computing "This Month"
                val monthSummary = statsRepository.getSummaryInRange(thirtyDaysAgo, now)
                val monthTopArtist = statsRepository.getTopArtistsInRange(thirtyDaysAgo, now, 1).firstOrNull()
                val monthTopAlbum = statsRepository.getTopAlbumsInRange(thirtyDaysAgo, now, 1).firstOrNull()
                val monthTopGenre = statsRepository.getTopGenresInRange(thirtyDaysAgo, now, 1).firstOrNull()
                
                val totalMinutes = monthSummary.totalListeningMs / 60000
                
                // Calculate Personality
                val skipRate = if (monthSummary.totalSessions > 0) monthSummary.skippedCount.toFloat() / monthSummary.totalSessions else 0f
                val isSkipper = skipRate > 0.5f
                val isNightOwl = peakActivity.firstOrNull()?.name == "Night"
                val repeatModes = statsRepository.getRepeatModeDistribution(thirtyDaysAgo, now)
                val loopCount = repeatModes.find { it.repeatMode == "track" || it.repeatMode == "all" }?.count ?: 0
                val isLooper = loopCount > (monthSummary.totalSessions * 0.3f)
                
                val personality = when {
                    isLooper -> "The Looper"
                    isSkipper -> "The Skipper"
                    isNightOwl -> "The Night Owl"
                    else -> "The Explorer"
                }

                val thisMonthCards = mutableListOf<WrappedCardUi>()
                thisMonthCards.add(WrappedCardUi(WrappedCardType.LISTENING_TIME, "Time Listened", "$totalMinutes mins", "In the last 30 days", "\u23F1\uFE0F"))
                if (monthTopArtist != null) {
                    thisMonthCards.add(WrappedCardUi(WrappedCardType.TOP_ARTIST, "Top Artist", monthTopArtist.artistName, "${monthTopArtist.playCount} plays", "\uD83C\uDFA4"))
                }
                if (monthTopAlbum != null) {
                    val genreSub = monthTopGenre?.genre?.let { "Top Genre: $it" } ?: "Top Album"
                    thisMonthCards.add(WrappedCardUi(WrappedCardType.TOP_ALBUM_GENRE, genreSub, monthTopAlbum.albumName, "By ${monthTopAlbum.artistName}", "\uD83D\uDCBF"))
                }
                thisMonthCards.add(WrappedCardUi(WrappedCardType.PERSONALITY, "Your Personality", personality, "Based on your habits", "\uD83E\uDDE0"))
                
                if (summary.totalSessions >= 100) {
                    thisMonthCards.add(WrappedCardUi(WrappedCardType.MILESTONE, "Milestone Unlocked!", "Century Club", "You've reached 100 total plays.", "\uD83C\uDFC6"))
                }

                val wrappedTab = WrappedTabUi(thisMonth = thisMonthCards)

                // Behavior
                val endReasons = statsRepository.getEndReasonDistribution(thirtyDaysAgo, now)
                val weekendCount = statsRepository.getWeekendCount(thirtyDaysAgo, now)
                val weekdayCount = statsRepository.getWeekdayCount(thirtyDaysAgo, now)
                
                val behaviorTab = BehaviorTabUi(
                    endReasons = endReasons.map { DistributionItem(it.endReason.ifEmpty { "Unknown" }, it.count) },
                    repeatModes = repeatModes.map { DistributionItem(it.repeatMode.ifEmpty { "Unknown" }, it.count) },
                    isWeekend = listOf(
                        DistributionItem("Weekend", weekendCount.toInt()),
                        DistributionItem("Weekday", weekdayCount.toInt())
                    ).filter { it.count > 0 }
                )

                // Context
                val devices = statsRepository.getDeviceDistribution(thirtyDaysAgo, now)
                val queueSources = statsRepository.getQueueSourceDistribution(thirtyDaysAgo, now)
                val origins = statsRepository.getPlaybackOriginDistribution(thirtyDaysAgo, now)
                val screenOnCount = statsRepository.getScreenOnCount(thirtyDaysAgo, now)
                val screenOffCount = summary.totalSessions - screenOnCount
                
                val contextTab = ContextTabUi(
                    devices = devices.map { DistributionItem(it.outputDevice.ifEmpty { "Unknown" }, it.count) },
                    queueSources = queueSources.map { DistributionItem(it.queueSource.ifEmpty { "Unknown" }, it.count) },
                    playbackOrigins = origins.map { DistributionItem(it.playbackOrigin.ifEmpty { "Unknown" }, it.count) },
                    screenOn = listOf(
                        DistributionItem("Screen On", screenOnCount.toInt()),
                        DistributionItem("Screen Off", screenOffCount.toInt())
                    ).filter { it.count > 0 }
                )

                _uiState.update {
                    it.copy(
                        insights = insightsTab,
                        topCharts = topChartsTab,
                        libraryProfile = libraryProfileTab,
                        behavior = behaviorTab,
                        context = contextTab,
                        wrapped = wrappedTab,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val sessions = try {
                    statsRepository.getSessionsPaged(limit = 100, offset = 0)
                } catch (e: Exception) {
                    emptyList()
                }
                
                val totalTimeMs = statsRepository.getTotalListeningTimeMs()

                _uiState.update { 
                    it.copy(
                        historySessions = sessions,
                        totalListeningTimeMs = totalTimeMs
                    ) 
                }
            } catch (e: Exception) {
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            try {
                statsRepository.deleteSession(sessionId)
                loadStats()
                loadHistory()
            } catch (e: Exception) {
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                statsRepository.exportToCsvStreaming(uri = uri)
                Toast.makeText(getApplication(), R.string.stats_export_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), R.string.stats_export_failed, Toast.LENGTH_SHORT).show()
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                statsRepository.exportToJsonStreaming(uri = uri)
                Toast.makeText(getApplication(), R.string.stats_export_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), R.string.stats_export_failed, Toast.LENGTH_SHORT).show()
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    companion object {
        fun formatDurationProgressive(durationMs: Long): String {
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            val hours = (durationMs / (1000 * 60 * 60))
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}
