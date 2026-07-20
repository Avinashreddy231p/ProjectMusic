package com.mardous.projectmusic.ui.screen.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.BuildConfig
import com.mardous.projectmusic.core.model.task.Event
import com.mardous.projectmusic.data.remote.github.GitHubService
import com.mardous.projectmusic.data.remote.github.model.GitHubRelease
import com.mardous.projectmusic.util.Preferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class UpdateViewModel(private val updateService: GitHubService): ViewModel() {

    private val ioHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("UpdateViewModel", "Update search failed!", throwable)
    }

    private val _updateSearch = MutableLiveData(UpdateSearchResult())
    val updateEventObservable = _updateSearch.map { Event(it) }
    val updateEvent get() = updateEventObservable.value

    val latestRelease get() = updateEvent?.peekContent()?.data

    sealed class DownloadStatus {
        object Idle : DownloadStatus()
        data class Downloading(val progress: Int) : DownloadStatus()
        data class Completed(val downloadId: Long) : DownloadStatus()
        data class Failed(val error: String?) : DownloadStatus()
    }

    private val _downloadStatus = MutableLiveData<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: LiveData<DownloadStatus> = _downloadStatus

    private var pollingJob: Job? = null

    fun syncDownloadStatus(context: Context) {
        val downloadId = Preferences.lastUpdateId
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService<DownloadManager>() ?: return
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1) {
                    when (cursor.getInt(statusIndex)) {
                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                            startPollingProgress(context, downloadId)
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            _downloadStatus.postValue(DownloadStatus.Completed(downloadId))
                        }
                        else -> {
                            _downloadStatus.postValue(DownloadStatus.Idle)
                        }
                    }
                }
            } else {
                _downloadStatus.postValue(DownloadStatus.Idle)
            }
            cursor.close()
        }
    }

    fun searchForUpdate(fromUser: Boolean, allowExperimental: Boolean = Preferences.experimentalUpdates || BuildConfig.IS_CI_BUILD) =
        viewModelScope.launch(Dispatchers.IO) {
            val current = updateEvent?.peekContent() ?: UpdateSearchResult(executedAtMillis = Preferences.lastUpdateSearch)
            if (current.shouldStartNewSearchFor(fromUser, allowExperimental)) {
                _updateSearch.postValue(
                    current.copy(
                        state = UpdateSearchResult.State.Searching,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                )

                val result = runCatching {
                    updateService.latestRelease(allowExperimental = allowExperimental)
                }
                val executedAtMillis = Date().time.also {
                    Preferences.lastUpdateSearch = it
                }
                val newState = if (result.isSuccess) {
                    UpdateSearchResult(
                        state = UpdateSearchResult.State.Completed,
                        data = result.getOrThrow(),
                        executedAtMillis = executedAtMillis,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                } else {
                    UpdateSearchResult(
                        state = UpdateSearchResult.State.Failed,
                        data = null,
                        error = result.exceptionOrNull(),
                        executedAtMillis = executedAtMillis,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                }
                _updateSearch.postValue(newState)
            }
        }

    fun downloadUpdate(context: Context, release: GitHubRelease) =
        viewModelScope.launch(Dispatchers.IO + ioHandler) {
            val downloadRequest = release.getDownloadRequest(context)
            if (downloadRequest != null) {
                val downloadManager = context.getSystemService<DownloadManager>()
                if (downloadManager != null) {
                    val lastUpdateId = Preferences.lastUpdateId
                    if (lastUpdateId != -1L) {
                        downloadManager.remove(lastUpdateId)
                    }
                    val newDownloadId = downloadManager.enqueue(downloadRequest)
                    Preferences.lastUpdateId = newDownloadId
                    startPollingProgress(context, newDownloadId)
                }
            }
        }

    private fun startPollingProgress(context: Context, downloadId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            val downloadManager = context.getSystemService<DownloadManager>() ?: return@launch
            while (isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex != -1) {
                        when (cursor.getInt(statusIndex)) {
                            DownloadManager.STATUS_RUNNING -> {
                                val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                if (totalIndex != -1 && downloadedIndex != -1) {
                                    val total = cursor.getLong(totalIndex)
                                    val downloaded = cursor.getLong(downloadedIndex)
                                    if (total > 0) {
                                        val progress = (downloaded * 100 / total).toInt()
                                        _downloadStatus.postValue(DownloadStatus.Downloading(progress))
                                    }
                                }
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                _downloadStatus.postValue(DownloadStatus.Completed(downloadId))
                                cursor.close()
                                break
                            }
                            DownloadManager.STATUS_FAILED -> {
                                _downloadStatus.postValue(DownloadStatus.Failed("Download failed"))
                                cursor.close()
                                break
                            }
                        }
                    }
                }
                cursor.close()
                delay(1000)
            }
        }
    }

    fun installUpdate(context: Context) {
        val status = _downloadStatus.value
        if (status is DownloadStatus.Completed) {
            val downloadId = status.downloadId
            val downloadManager = context.getSystemService<DownloadManager>() ?: return
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                    val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    if (uriIndex != -1) {
                        val localUriString = cursor.getString(uriIndex)
                        if (localUriString != null) {
                            val file = File(Uri.parse(localUriString).path!!)
                            val contentUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                                return
                            }

                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(contentUri, "application/vnd.android.package-archive")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }

                            try {
                                context.startActivity(installIntent)
                            } catch (e: Exception) {
                                _downloadStatus.postValue(DownloadStatus.Failed(e.message))
                            }
                        }
                    }
                }
            }
            cursor.close()
        }
    }
}