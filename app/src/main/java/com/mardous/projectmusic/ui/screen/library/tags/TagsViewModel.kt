package com.mardous.projectmusic.ui.screen.library.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.core.sort.TagSortMode
import com.mardous.projectmusic.data.local.repository.MetadataRepository
import com.mardous.projectmusic.data.local.database.metadata.MetadataSource
import com.mardous.projectmusic.data.local.database.metadata.TagCategory
import com.mardous.projectmusic.data.local.database.metadata.TagEntity
import com.mardous.projectmusic.data.model.Tag
import com.mardous.projectmusic.ui.component.compose.expressive.LibraryFilterState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TagGrouping { Category, Alphabet, Rank, SongCount }

class TagsViewModel(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow<TagSortMode>(TagSortMode.AllTags)
    val sortMode = _sortMode.asStateFlow()

    private val _filterState = MutableStateFlow(LibraryFilterState())
    val filterState = _filterState.asStateFlow()

    private val _grouping = MutableStateFlow(TagGrouping.Category)
    val grouping = _grouping.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds = _selectedTagIds.asStateFlow()

    val categories = TagCategory.entries

    val tags: StateFlow<Map<String, List<Tag>>> = combine(
        metadataRepository.getTagsByCategory(),
        _searchQuery,
        _sortMode,
        _filterState,
        _grouping
    ) { tagsByCategory: Map<TagCategory, List<Tag>>, query: String, sortMode: TagSortMode, filter: LibraryFilterState, grouping: TagGrouping ->
        tagsByCategory.values.flatten()
            .filter { it.name.contains(query, ignoreCase = true) }
            .filter { tag ->
                if (filter.selectedTagCategories.isNotEmpty() && !filter.selectedTagCategories.contains(tag.category)) return@filter false
                if (filter.hasSongs && tag.songCount == 0) return@filter false
                if (tag.songCount < filter.minSongCount) return@filter false
                true
            }
            .let { with(sortMode) { it.sorted() } }
            .groupBy { tag ->
                when (grouping) {
                    TagGrouping.Category -> tag.category.name
                    TagGrouping.Alphabet -> tag.name.firstOrNull()?.uppercase() ?: "#"
                    TagGrouping.Rank -> {
                        when (tag.overallRank) {
                            in 1..10 -> "Top 10"
                            in 11..50 -> "Top 50"
                            in 51..100 -> "Top 100"
                            else -> "Other"
                        }
                    }
                    TagGrouping.SongCount -> {
                        when (tag.songCount) {
                            0 -> "Empty"
                            in 1..10 -> "1-10 Songs"
                            in 11..50 -> "11-50 Songs"
                            in 51..100 -> "51-100 Songs"
                            else -> "100+ Songs"
                        }
                    }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun updateSortMode(sortMode: TagSortMode) {
        _sortMode.value = sortMode
    }

    fun updateFilter(newState: LibraryFilterState) {
        _filterState.value = newState
    }

    fun updateGrouping(newGrouping: TagGrouping) {
        _grouping.value = newGrouping
    }

    // CRUD Operations
    fun createTag(name: String, description: String?, category: TagCategory, color: Int?) {
        viewModelScope.launch {
            metadataRepository.createTag(name, category.value, description, color)
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            metadataRepository.getTagById(tag.id).firstOrNull()?.let { entity ->
                metadataRepository.updateTag(entity.copy(
                    name = tag.name,
                    description = tag.description,
                    category = tag.category.value,
                    color = tag.color,
                    icon = tag.icon,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            metadataRepository.deleteTag(tagId)
        }
    }

    fun renameTag(tagId: Long, newName: String) {
        viewModelScope.launch {
            metadataRepository.renameTag(tagId, newName)
        }
    }

    // Selection Mode
    fun toggleSelection(tagId: Long) {
        _selectedTagIds.update { 
            if (it.contains(tagId)) it - tagId else it + tagId
        }
    }

    fun clearSelection() {
        _selectedTagIds.value = emptySet()
    }

    fun deleteSelectedTags() {
        viewModelScope.launch {
            _selectedTagIds.value.forEach { 
                metadataRepository.deleteTag(it)
            }
            clearSelection()
        }
    }

    fun assignSongsToSelected(songIds: List<Long>) {
        viewModelScope.launch {
            metadataRepository.assignSongsToTags(
                tagIds = _selectedTagIds.value.toList(),
                songIds = songIds,
                source = MetadataSource.Manual
            )
            clearSelection()
        }
    }
}
