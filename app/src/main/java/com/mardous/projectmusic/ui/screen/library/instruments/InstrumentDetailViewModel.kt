package com.mardous.projectmusic.ui.screen.library.instruments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.data.local.repository.InstrumentDetail
import com.mardous.projectmusic.data.local.repository.MetadataRepository
import com.mardous.projectmusic.data.local.database.metadata.MetadataSource
import com.mardous.projectmusic.data.model.Instrument
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InstrumentDetailViewModel(
    private val metadataRepository: MetadataRepository,
    private val instrumentId: Long
) : ViewModel() {

    val instrumentDetail: StateFlow<InstrumentDetail?> = metadataRepository.getInstrumentDetail(instrumentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val relatedInstruments: StateFlow<List<Instrument>> = instrumentDetail
        .flatMapLatest { detail ->
            if (detail == null) flowOf(emptyList())
            else metadataRepository.getInstrumentsByFamily()
                .map { map ->
                    map[detail.instrument.family]?.filter { it.id != instrumentId } ?: emptyList()
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun assignSongs(songIds: List<Long>) {
        viewModelScope.launch {
            metadataRepository.assignSongsToInstruments(listOf(instrumentId), songIds, MetadataSource.Manual)
        }
    }
}


