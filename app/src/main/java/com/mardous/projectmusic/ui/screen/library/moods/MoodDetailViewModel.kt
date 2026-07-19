package com.mardous.projectmusic.ui.screen.library.moods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.data.local.repository.MetadataRepository
import com.mardous.projectmusic.data.local.repository.MoodDetail
import com.mardous.projectmusic.data.local.database.metadata.MetadataSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MoodDetailViewModel(
    private val metadataRepository: MetadataRepository,
    private val moodId: Long
) : ViewModel() {

    val moodDetail: StateFlow<MoodDetail?> = metadataRepository.getMoodDetail(moodId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun assignSongs(songIds: List<Long>) {
        viewModelScope.launch {
            metadataRepository.assignSongsToMoods(listOf(moodId), songIds, MetadataSource.Manual)
        }
    }
}
