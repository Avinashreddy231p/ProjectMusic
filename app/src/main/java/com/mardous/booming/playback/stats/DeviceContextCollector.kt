package com.mardous.booming.playback.stats

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import com.mardous.booming.BuildConfig
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.core.model.audiodevice.AudioDeviceType
import com.mardous.booming.core.model.audiodevice.getDeviceType
import com.mardous.booming.playback.equalizer.EqualizerManager

class DeviceContextCollector(
    private val context: Context,
    private val audioOutputObserver: AudioOutputObserver,
    private val equalizerManager: EqualizerManager
) {

    data class DeviceSnapshot(
        val audioFormat: String,
        val audioSampleRate: Int,
        val audioChannelCount: Int,
        val bitrateKbps: Int,
        val playbackSpeed: Float,
        val equalizerActive: Boolean,
        val outputDevice: String,
        val volumeStart: Int,
        val batteryLevel: Int,
        val charging: Boolean,
        val screenOn: Boolean,
        val appVersion: String
    )

    fun snapshot(): DeviceSnapshot {
        return DeviceSnapshot(
            audioFormat = audioOutputObserver.currentFormat,
            audioSampleRate = audioOutputObserver.currentSampleRate.coerceAtLeast(0),
            audioChannelCount = audioOutputObserver.currentChannelCount.coerceAtLeast(0),
            bitrateKbps = audioOutputObserver.currentBitrate.coerceAtLeast(0),
            playbackSpeed = 1.0f,
            equalizerActive = equalizerManager.eqSession.active,
            outputDevice = resolveOutputDevice(),
            volumeStart = getCurrentVolume(),
            batteryLevel = getBatteryLevel(),
            charging = isCharging(),
            screenOn = isScreenOn(),
            appVersion = BuildConfig.VERSION_NAME
        )
    }

    private fun resolveOutputDevice(): String {
        val audioDevice = audioOutputObserver.audioDevice.value
        return when (audioDevice.type) {
            AudioDeviceType.BluetoothA2dp -> "Bluetooth"
            AudioDeviceType.Headset -> "Wired Headset"
            AudioDeviceType.UsbHeadset, AudioDeviceType.UsbDevice -> "USB"
            AudioDeviceType.Hdmi -> "HDMI"
            AudioDeviceType.Aux -> "AUX"
            AudioDeviceType.BuiltinSpeaker -> "Speaker"
            else -> "Speaker"
        }
    }

    private fun getCurrentVolume(): Int {
        return audioOutputObserver.systemVolumeState.value.volumePercent.toInt().coerceIn(0, 100)
    }

    private fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100f / scale).toInt().coerceIn(0, 100)
        } else {
            -1
        }
    }

    private fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isScreenOn(): Boolean {
        val powerManager = context.getSystemService<PowerManager>() ?: return true
        return powerManager.isInteractive
    }

    fun updateAudioInfo(
        format: String = "",
        sampleRate: Int = 0,
        channelCount: Int = 0,
        bitrateKbps: Int = 0,
        playbackSpeed: Float = 1.0f
    ): DeviceSnapshot {
        val base = snapshot()
        return base.copy(
            audioFormat = format,
            audioSampleRate = sampleRate,
            audioChannelCount = channelCount,
            bitrateKbps = bitrateKbps,
            playbackSpeed = playbackSpeed
        )
    }
}
