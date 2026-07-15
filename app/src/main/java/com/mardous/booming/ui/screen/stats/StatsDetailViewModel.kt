package com.mardous.booming.ui.screen.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.local.repository.SongRepository
import com.mardous.booming.data.local.repository.StatsRepository
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsDetailViewModel(
    private val category: String,
    private val itemName: String,
    private val songRepository: SongRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _songs = MutableLiveData<List<Song>>()

    init {
        loadSongs()
    }

    fun getSongs(): LiveData<List<Song>> = _songs

    fun loadSongs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Fetch all sessions
                val allSessions = try {
                    statsRepository.getSessionsPaged(limit = 10000, offset = 0)
                } catch (e: Exception) {
                    emptyList()
                }

                // Filter sessions based on category and itemName
                val filteredSessions = allSessions.filter { session ->
                    when (category) {
                        "Time Period" -> {
                            val timePeriod = when (session.hour) {
                                in 5..11 -> "Morning"
                                in 12..16 -> "Afternoon"
                                in 17..20 -> "Evening"
                                else -> "Night"
                            }
                            timePeriod == itemName
                        }
                        "Audio Format" -> {
                            val format = session.audioFormat.ifEmpty { "Unknown" }
                            format == itemName
                        }
                        "End Reason" -> {
                            val endReason = session.endReason.ifEmpty { "Unknown" }
                            endReason == itemName
                        }
                        "Repeat Mode" -> {
                            val repeatMode = session.repeatMode.ifEmpty { "Unknown" }
                            repeatMode == itemName
                        }
                        "Day Type" -> {
                            val dayType = if (session.isWeekend) "Weekend" else "Weekday"
                            dayType == itemName
                        }
                        "Output Device" -> {
                            val device = session.outputDevice.ifEmpty { "Unknown" }
                            device == itemName
                        }
                        "Queue Source" -> {
                            val queueSource = session.queueSource.ifEmpty { "Unknown" }
                            queueSource == itemName
                        }
                        "Playback Origin" -> {
                            val origin = session.playbackOrigin.ifEmpty { "Unknown" }
                            origin == itemName
                        }
                        "Screen Status" -> {
                            val status = if (session.screenOn) "Screen On" else "Screen Off"
                            status == itemName
                        }
                        else -> false
                    }
                }

                // Collect the unique song IDs from the matching sessions
                val matchingSongIds = filteredSessions.map { it.songId }.toSet()

                // Fetch all songs and filter those present in matchingSongIds
                val allSongs = try { songRepository.songs() } catch (e: Exception) { emptyList() }
                val filteredSongs = allSongs.filter { matchingSongIds.contains(it.id) }
                
                _songs.postValue(filteredSongs)
            }
        }
    }
}
