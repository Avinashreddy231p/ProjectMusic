package com.mardous.projectmusic.ui.screen.library.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.data.local.repository.MetadataRepository
import com.mardous.projectmusic.data.local.repository.TagDetail
import com.mardous.projectmusic.data.local.database.metadata.MetadataSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TagDetailViewModel(
    private val metadataRepository: MetadataRepository,
    private val tagId: Long
) : ViewModel() {

    val tagDetail: StateFlow<TagDetail?> = metadataRepository.getTagDetail(tagId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun assignSongs(songIds: List<Long>) {
        viewModelScope.launch {
            metadataRepository.assignSongsToTags(listOf(tagId), songIds, MetadataSource.Manual)
        }
    }
}
