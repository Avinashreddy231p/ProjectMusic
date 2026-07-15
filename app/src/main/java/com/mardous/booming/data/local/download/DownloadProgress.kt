package com.mardous.booming.data.local.download

import android.net.Uri

data class DownloadProgress(
    val songId: String,
    val state: State,
    val progress: Int = 0,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val destinationUri: Uri? = null,
    val error: String? = null
) {
    enum class State {
        PENDING,
        DOWNLOADING,
        COMPLETE,
        ERROR,
        CANCELLED
    }

    val isComplete: Boolean get() = state == State.COMPLETE
    val isDownloading: Boolean get() = state == State.DOWNLOADING
    val hasError: Boolean get() = state == State.ERROR
    val progressPercent: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
}
