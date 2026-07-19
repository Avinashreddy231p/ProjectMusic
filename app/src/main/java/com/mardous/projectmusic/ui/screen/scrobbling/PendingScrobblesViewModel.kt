package com.mardous.projectmusic.ui.screen.scrobbling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.data.local.database.dao.PendingScrobbleDao
import com.mardous.projectmusic.data.local.database.analytics.PendingScrobbleEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.mardous.projectmusic.data.local.repository.NetworkRepository

class PendingScrobblesViewModel(
    private val pendingScrobbleDao: PendingScrobbleDao,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    val pendingScrobbles: StateFlow<List<PendingScrobbleEntity>> = pendingScrobbleDao.getAllFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteScrobble(id: Long) {
        viewModelScope.launch {
            pendingScrobbleDao.deleteById(id)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            pendingScrobbleDao.deleteAll()
        }
    }

    fun updateScrobble(scrobble: PendingScrobbleEntity) {
        viewModelScope.launch {
            pendingScrobbleDao.update(scrobble)
        }
    }

    fun syncScrobbles() {
        viewModelScope.launch {
            networkRepository.triggerOfflineScrobbleSync()
        }
    }
}
