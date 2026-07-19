package com.mardous.projectmusic.ui.screen.library.instruments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.core.sort.InstrumentSortMode
import com.mardous.projectmusic.data.local.repository.MetadataRepository
import com.mardous.projectmusic.data.local.database.metadata.InstrumentEntity
import com.mardous.projectmusic.data.local.database.metadata.InstrumentFamily
import com.mardous.projectmusic.data.local.database.metadata.MetadataSource
import com.mardous.projectmusic.data.model.Instrument
import com.mardous.projectmusic.ui.component.compose.expressive.LibraryFilterState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class InstrumentGrouping { Family, Alphabet, Rank, SongCount }

class InstrumentsViewModel(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow<InstrumentSortMode>(InstrumentSortMode.AllInstruments)
    val sortMode = _sortMode.asStateFlow()

    private val _grouping = MutableStateFlow(InstrumentGrouping.Family)
    val grouping = _grouping.asStateFlow()

    private val _filterState = MutableStateFlow(LibraryFilterState())
    val filterState = _filterState.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode = _selectionMode.asStateFlow()

    private val _selectedInstruments = MutableStateFlow<Set<Long>>(emptySet())
    val selectedInstruments = _selectedInstruments.asStateFlow()

    val instruments: StateFlow<Map<String, List<Instrument>>> = combine(
        metadataRepository.getInstrumentsByFamily(),
        _searchQuery,
        _sortMode,
        _filterState,
        _grouping
    ) { instrumentsMap: Map<InstrumentFamily, List<Instrument>>, query: String, sortMode: InstrumentSortMode, filter: LibraryFilterState, grouping: InstrumentGrouping ->
        instrumentsMap.values.flatten()
            .filter { instrument ->
                instrument.name.contains(query, ignoreCase = true) &&
                        instrument.songCount >= filter.minSongCount &&
                        (filter.selectedInstrumentFamilies.isEmpty() || filter.selectedInstrumentFamilies.contains(instrument.family))
            }
            .let { with(sortMode) { it.sorted() } }
            .groupBy { instrument ->
                when (grouping) {
                    InstrumentGrouping.Family -> instrument.family.name
                    InstrumentGrouping.Alphabet -> instrument.name.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"
                    InstrumentGrouping.Rank -> when {
                        instrument.overallRank in 1..10 -> "Top 10"
                        instrument.overallRank in 11..50 -> "Top 50"
                        else -> "Others"
                    }
                    InstrumentGrouping.SongCount -> when {
                        instrument.songCount == 0 -> "0 songs"
                        instrument.songCount in 1..10 -> "1-10 songs"
                        instrument.songCount in 11..50 -> "11-50 songs"
                        else -> "50+ songs"
                    }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // CRUD Operations
    fun createInstrument(name: String, description: String?, family: InstrumentFamily, color: Int?) {
        viewModelScope.launch {
            val id = metadataRepository.createInstrument(name, family.value, description)
            if (color != null) {
                metadataRepository.getInstrumentById(id).firstOrNull()?.let { entity ->
                    metadataRepository.updateInstrument(entity.copy(color = color))
                }
            }
        }
    }

    fun updateInstrument(instrument: Instrument) {
        viewModelScope.launch {
            metadataRepository.updateInstrument(instrument.toEntity())
        }
    }

    fun deleteInstrument(instrumentId: Long) {
        viewModelScope.launch {
            metadataRepository.deleteInstrument(instrumentId)
        }
    }

    fun renameInstrument(instrumentId: Long, newName: String) {
        viewModelScope.launch {
            metadataRepository.getInstrumentById(instrumentId).firstOrNull()?.let { entity ->
                metadataRepository.updateInstrument(entity.copy(name = newName, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    // Selection Mode Support
    fun toggleSelectionMode() {
        _selectionMode.value = !_selectionMode.value
        if (!_selectionMode.value) {
            _selectedInstruments.value = emptySet()
        }
    }

    fun toggleInstrumentSelection(instrumentId: Long) {
        _selectedInstruments.update {
            if (it.contains(instrumentId)) it - instrumentId else it + instrumentId
        }
    }

    fun clearSelection() {
        _selectedInstruments.value = emptySet()
    }

    fun selectAll(instrumentIds: List<Long>) {
        _selectedInstruments.value = instrumentIds.toSet()
    }

    fun assignSongsToSelected(songIds: List<Long>) {
        viewModelScope.launch {
            metadataRepository.assignSongsToInstruments(
                instrumentIds = _selectedInstruments.value.toList(),
                songIds = songIds,
                source = MetadataSource.Manual
            )
            clearSelection()
        }
    }

    // Search and Sort
    fun search(query: String) {
        _searchQuery.value = query
    }

    fun updateSortMode(sortMode: InstrumentSortMode) {
        _sortMode.value = sortMode
    }

    fun updateGrouping(grouping: InstrumentGrouping) {
        _grouping.value = grouping
    }

    fun updateFilterState(filterState: LibraryFilterState) {
        _filterState.value = filterState
    }

    private fun Instrument.toEntity() = InstrumentEntity(
        id = id,
        name = name,
        description = description,
        icon = icon,
        color = color,
        family = family.value,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
