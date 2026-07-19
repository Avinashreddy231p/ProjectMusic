package com.mardous.projectmusic.playback.processor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Real-time audio energy processor for driving visual animations.
 */
@OptIn(UnstableApi::class)
class BeatAudioProcessor : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // Calculate RMS of the buffer
        var sumSquares = 0.0
        val count = remaining / 2
        
        // Use a copy for analysis to not consume the buffer
        val readBuffer = inputBuffer.duplicate()
        readBuffer.order(ByteOrder.LITTLE_ENDIAN)

        while (readBuffer.remaining() >= 2) {
            val sample = readBuffer.short.toDouble()
            sumSquares += sample * sample
        }

        val rms = if (count > 0) sqrt(sumSquares / count) else 0.0
        // Normalize 16-bit PCM (max 32768) to 0.0 - 1.0 range
        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f) * 2.0f
        BeatAnalyzer.updateAmplitude(normalized.coerceIn(0f, 1.2f))

        // Pass through original buffer
        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }
}
