package com.mardous.projectmusic.ui.screen.library.moods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.core.sort.MoodSortMode
import com.mardous.projectmusic.data.local.repository.MetadataRepository
import com.mardous.projectmusic.data.local.database.metadata.MetadataSource
import com.mardous.projectmusic.data.local.database.metadata.MoodEntity
import com.mardous.projectmusic.data.model.Mood
import com.mardous.projectmusic.ui.component.compose.expressive.LibraryFilterState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MoodGrouping { Alphabet, Rank, SongCount, RecentlyAdded }

class MoodsViewModel(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow<MoodSortMode>(MoodSortMode.AllMoods)
    val sortMode = _sortMode.asStateFlow()

    private val _filterState = MutableStateFlow(LibraryFilterState())
    val filterState = _filterState.asStateFlow()

    private val _grouping = MutableStateFlow(MoodGrouping.Alphabet)
    val grouping = _grouping.asStateFlow()

    private val _selectedMoodIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMoodIds = _selectedMoodIds.asStateFlow()

    val moods: StateFlow<Map<String, List<Mood>>> = combine(
        metadataRepository.getAllMoods(),
        _searchQuery,
        _sortMode,
        _filterState,
        _grouping
    ) { moods, query, sortMode, filter, grouping ->
        moods.asSequence()
            .filter { it.name.contains(query, ignoreCase = true) }
            .filter { mood ->
                if (filter.hasSongs && mood.songCount == 0) return@filter false
                if (mood.songCount < filter.minSongCount) return@filter false
                true
            }
            .toList()
            .let { with(sortMode) { it.sorted() } }
            .groupBy { mood ->
                when (grouping) {
                    MoodGrouping.Alphabet -> mood.name.firstOrNull()?.uppercase() ?: "#"
                    MoodGrouping.Rank -> {
                        when (mood.overallRank) {
                            in 1..10 -> "Top 10"
                            in 11..50 -> "Top 50"
                            in 51..100 -> "Top 100"
                            else -> "Other"
                        }
                    }
                    MoodGrouping.SongCount -> {
                        when (mood.songCount) {
                            0 -> "Empty"
                            in 1..10 -> "1-10 Songs"
                            in 11..50 -> "11-50 Songs"
                            in 51..100 -> "51-100 Songs"
                            else -> "100+ Songs"
                        }
                    }
                    MoodGrouping.RecentlyAdded -> {
                        val now = System.currentTimeMillis()
                        val diff = now - mood.createdAt
                        val dayMs = 24 * 60 * 60 * 1000L
                        when {
                            diff < dayMs -> "Today"
                            diff < 7 * dayMs -> "This Week"
                            diff < 30 * dayMs -> "This Month"
                            else -> "Older"
                        }
                    }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun updateSortMode(sortMode: MoodSortMode) {
        _sortMode.value = sortMode
    }

    fun updateFilter(newState: LibraryFilterState) {
        _filterState.value = newState
    }

    fun updateGrouping(newGrouping: MoodGrouping) {
        _grouping.value = newGrouping
    }

    // CRUD Operations
    fun createMood(name: String, description: String?, emoji: String?, color: Int?) {
        viewModelScope.launch {
            metadataRepository.createMood(name, description, emoji, color)
        }
    }

    fun updateMood(mood: Mood) {
        viewModelScope.launch {
            metadataRepository.getMoodById(mood.id).firstOrNull()?.let { entity ->
                metadataRepository.updateMood(entity.copy(
                    name = mood.name,
                    description = mood.description,
                    emoji = mood.emoji,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    fun deleteMood(moodId: Long) {
        viewModelScope.launch {
            metadataRepository.deleteMood(moodId)
        }
    }

    fun renameMood(moodId: Long, newName: String) {
        viewModelScope.launch {
            metadataRepository.renameMood(moodId, newName)
        }
    }

    // Selection Mode
    fun toggleSelection(moodId: Long) {
        _selectedMoodIds.update { 
            if (it.contains(moodId)) it - moodId else it + moodId
        }
    }

    fun clearSelection() {
        _selectedMoodIds.value = emptySet()
    }

    fun deleteSelectedMoods() {
        viewModelScope.launch {
            _selectedMoodIds.value.forEach { 
                metadataRepository.deleteMood(it)
            }
            clearSelection()
        }
    }

    fun assignSongsToSelected(songIds: List<Long>) {
        viewModelScope.launch {
            metadataRepository.assignSongsToMoods(
                moodIds = _selectedMoodIds.value.toList(),
                songIds = songIds,
                source = MetadataSource.Manual
            )
            clearSelection()
        }
    }
}
