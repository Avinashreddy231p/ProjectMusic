package com.mardous.projectmusic.playback.processor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton to bridge real-time audio data from the PlaybackService to the UI.
 */
object BeatAnalyzer {
    private val _amplitude = MutableStateFlow(0f)
    val amplitude = _amplitude.asStateFlow()

    fun updateAmplitude(value: Float) {
        _amplitude.value = value
    }
}
