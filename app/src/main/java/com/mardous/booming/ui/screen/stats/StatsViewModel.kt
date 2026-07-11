package com.mardous.booming.ui.screen.stats

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.R
import com.mardous.booming.data.local.repository.StatsRepository
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.mardous.booming.data.local.room.ListeningHistoryDao

data class StatsSummaryUi(
    val totalSessions: Long = 0,
    val totalTimeMs: Long = 0,
    val uniqueSongs: Long = 0,
    val completionRate: Float = 0f
)

data class StatsUiState(
    val summary: StatsSummaryUi = StatsSummaryUi(),
    val topSongs: List<ListeningHistoryDao.SongStats> = emptyList(),
    val topArtists: List<ListeningHistoryDao.ArtistStats> = emptyList(),
    val topAlbums: List<ListeningHistoryDao.AlbumStats> = emptyList(),
    val topGenres: List<ListeningHistoryDao.GenreStats> = emptyList(),
    val monthlyStats: List<ListeningHistoryDao.PeriodStats> = emptyList(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false
)

class StatsViewModel(
    application: Application,
    private val statsRepository: StatsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val summary = statsRepository.getSummary()
                val topSongs = statsRepository.getTopSongs(limit = 10)
                val topArtists = statsRepository.getTopArtists(limit = 10)
                val topAlbums = statsRepository.getTopAlbums(limit = 10)
                val topGenres = statsRepository.getTopGenres(limit = 10)
                val monthly = statsRepository.getMonthlyStats(limit = 12)

                val completionRate = if (summary.totalSessions > 0) {
                    summary.completedCount.toFloat() / summary.totalSessions.toFloat()
                } else 0f

                _uiState.update {
                    it.copy(
                        summary = StatsSummaryUi(
                            totalSessions = summary.totalSessions,
                            totalTimeMs = summary.totalListeningMs,
                            uniqueSongs = summary.uniqueSongs,
                            completionRate = completionRate
                        ),
                        topSongs = topSongs,
                        topArtists = topArtists,
                        topAlbums = topAlbums,
                        topGenres = topGenres,
                        monthlyStats = monthly,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val context = getApplication<Application>()
                val uri = createDownloadsUri(context, "listening_history.csv", "text/csv")
                if (uri != null) {
                    statsRepository.exportToCsvStreaming(uri = uri)
                    Toast.makeText(context, R.string.stats_export_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.stats_export_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), R.string.stats_export_failed, Toast.LENGTH_SHORT).show()
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val context = getApplication<Application>()
                val uri = createDownloadsUri(context, "listening_history.json", "application/json")
                if (uri != null) {
                    statsRepository.exportToJsonStreaming(uri = uri)
                    Toast.makeText(context, R.string.stats_export_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.stats_export_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), R.string.stats_export_failed, Toast.LENGTH_SHORT).show()
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun createDownloadsUri(context: Context, fileName: String, mimeType: String): android.net.Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                android.net.Uri.fromFile(java.io.File(dir, fileName))
            }
        } catch (e: Exception) {
            null
        }
    }
}
