package com.mardous.projectmusic.playback.processor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.mardous.projectmusic.playback.processor.ByteUtils.getInt24
import com.mardous.projectmusic.playback.processor.ByteUtils.putInt24
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class BalanceAudioProcessor(
    private var leftGain: Float = 1.0f,
    private var rightGain: Float = 1.0f
) : BaseAudioProcessor() {

    @Synchronized
    fun setBalance(left: Float, right: Float) {
        leftGain = left
        rightGain = right
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_24BIT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val buffer = replaceOutputBuffer(remaining)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        when (outputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> process16Bit(inputBuffer, buffer)
            C.ENCODING_PCM_24BIT -> process24Bit(inputBuffer, buffer)
        }

        if (inputBuffer.hasRemaining()) {
            buffer.put(inputBuffer)
        }

        buffer.flip()
    }

    private fun process16Bit(inputBuffer: ByteBuffer, buffer: ByteBuffer) {
        if (inputAudioFormat.channelCount == 2) {
            while (inputBuffer.remaining() >= 4) {
                val left = inputBuffer.short
                val right = inputBuffer.short
                val newLeft = (left * leftGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val newRight = (right * rightGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer.putShort(newLeft)
                buffer.putShort(newRight)
            }
        } else if (inputAudioFormat.channelCount == 1) {
            while (inputBuffer.remaining() >= 2) {
                val sample = inputBuffer.short
                val gain = (leftGain + rightGain) / 2f
                val newSample = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer.putShort(newSample)
            }
        }
    }

    private fun process24Bit(inputBuffer: ByteBuffer, buffer: ByteBuffer) {
        if (inputAudioFormat.channelCount == 2) {
            while (inputBuffer.remaining() >= 6) {
                val left = inputBuffer.getInt24()
                val right = inputBuffer.getInt24()
                val newLeft = (left.toDouble() * leftGain).toInt().coerceIn(ByteUtils.Int24_MIN_VALUE, ByteUtils.Int24_MAX_VALUE)
                val newRight = (right.toDouble() * rightGain).toInt().coerceIn(ByteUtils.Int24_MIN_VALUE, ByteUtils.Int24_MAX_VALUE)
                buffer.putInt24(newLeft)
                buffer.putInt24(newRight)
            }
        } else if (inputAudioFormat.channelCount == 1) {
            while (inputBuffer.remaining() >= 3) {
                val sample = inputBuffer.getInt24()
                val gain = (leftGain + rightGain) / 2f
                val newSample = (sample.toDouble() * gain).toInt().coerceIn(ByteUtils.Int24_MIN_VALUE, ByteUtils.Int24_MAX_VALUE)
                buffer.putInt24(newSample)
            }
        }
    }
}
