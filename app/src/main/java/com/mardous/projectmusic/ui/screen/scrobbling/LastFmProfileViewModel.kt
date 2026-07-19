package com.mardous.projectmusic.ui.screen.scrobbling

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.data.local.repository.NetworkRepository
import com.mardous.projectmusic.data.local.database.dao.PendingScrobbleDao
import com.mardous.projectmusic.data.remote.lastfm.model.*
import com.mardous.projectmusic.util.Preferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LastFmProfileUiState(
    val user: LastFmUser? = null,
    val topArtists: List<LastFmTopArtist> = emptyList(),
    val topTracks: List<LastFmTopTrack> = emptyList(),
    val topAlbums: List<LastFmTopAlbum> = emptyList(),
    val recentTracks: List<LastFmRecentTrack> = emptyList(),
    val pendingScrobblesCount: Int = 0,
    val isLoading: Boolean = false,
    val selectedPeriod: String = "overall",
    val error: String? = null,
    val isSyncing: Boolean = false
)

class LastFmProfileViewModel(
    private val networkRepository: NetworkRepository,
    private val pendingScrobbleDao: PendingScrobbleDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LastFmProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfileData()
        observePendingScrobbles()
        observePreferences()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val user = networkRepository.lastFmUserInfo()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load user info") }
                return@launch
            }

            _uiState.update { it.copy(user = user) }
            refreshCharts()
        }
    }

    private fun observePendingScrobbles() {
        pendingScrobbleDao.getAllFlow()
            .onEach { scrobbles ->
                _uiState.update { it.copy(pendingScrobblesCount = scrobbles.size) }
            }
            .launchIn(viewModelScope)
    }

    private fun observePreferences() {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            // Update state if needed, but settings are now handled in NetworkPreferencesFragment
        }
        Preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setPeriod(period: String) {
        if (_uiState.value.selectedPeriod == period) return
        _uiState.update { it.copy(selectedPeriod = period) }
        refreshCharts()
    }

    fun refreshCharts() {
        viewModelScope.launch {
            val period = _uiState.value.selectedPeriod
            
            _uiState.update { it.copy(isLoading = true) }
            
            val topArtists = networkRepository.lastFmUserTopArtists(period, 10)
            val topTracks = networkRepository.lastFmUserTopTracks(period, 10)
            val topAlbums = networkRepository.lastFmUserTopAlbums(period, 10)
            val recentTracks = networkRepository.lastFmUserRecentTracks(20)

            _uiState.update {
                it.copy(
                    topArtists = topArtists?.topartists?.artist ?: emptyList(),
                    topTracks = topTracks?.toptracks?.track ?: emptyList(),
                    topAlbums = topAlbums?.topalbums?.album ?: emptyList(),
                    recentTracks = recentTracks?.recenttracks?.track ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            networkRepository.triggerOfflineScrobbleSync()
            _uiState.update { it.copy(isSyncing = false) }
        }
    }
}
