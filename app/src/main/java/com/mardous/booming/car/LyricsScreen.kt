package com.mardous.booming.car

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mardous.booming.R
import com.mardous.booming.data.local.repository.LyricsRepository
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.SyncedLyrics
import com.mardous.booming.playback.PlaybackService
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LyricsScreen(carContext: CarContext) : Screen(carContext), KoinComponent, Player.Listener {

    private val repository: Repository by inject()
    private val lyricsRepository: LyricsRepository by inject()

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var currentSong: Song = Song.emptySong
    private var currentSyncedLyrics: SyncedLyrics? = null
    private var lastLineIndex = -1

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (mediaController?.isPlaying == true) {
                invalidate()
            }
            handler.postDelayed(this, 500)
        }
    }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                val sessionToken = SessionToken(carContext, ComponentName(carContext, PlaybackService::class.java))
                mediaControllerFuture = MediaController.Builder(carContext, sessionToken).build()
                mediaControllerFuture?.addListener({
                    mediaController = mediaControllerFuture?.get()
                    mediaController?.addListener(this@LyricsScreen)
                    updateCurrentSong(mediaController?.currentMediaItem)
                }, MoreExecutors.directExecutor())
            }

            override fun onStart(owner: LifecycleOwner) {
                handler.post(updateRunnable)
            }

            override fun onStop(owner: LifecycleOwner) {
                handler.removeCallbacks(updateRunnable)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                mediaController?.removeListener(this@LyricsScreen)
                mediaControllerFuture?.let { MediaController.releaseFuture(it) }
                scope.cancel()
            }
        })
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        updateCurrentSong(mediaItem)
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        scope.launch {
            val song = withContext(Dispatchers.IO) {
                repository.songByMediaItem(mediaItem)
            }
            if (song.id != currentSong.id) {
                currentSong = song
                loadLyrics(song)
                invalidate()
            }
        }
    }

    private suspend fun loadLyrics(song: Song) {
        val rawLyrics = withContext(Dispatchers.IO) {
            lyricsRepository.fileLyrics(song)
                ?: lyricsRepository.embeddedLyrics(song)
                ?: lyricsRepository.storedLyrics(song, false)
        }
        currentSyncedLyrics = rawLyrics?.let { lyricsRepository.parseRawLyrics(song, it) }
        lastLineIndex = -1
    }

    override fun onGetTemplate(): Template {
        val lyrics = currentSyncedLyrics
        val position = mediaController?.currentPosition ?: 0L

        val paneBuilder = Pane.Builder()

        if (lyrics != null && lyrics.hasContent) {
            val currentIndex = findLineIndexAt(position, lyrics)
            lastLineIndex = currentIndex

            // Show 3-4 lines around current
            val startIndex = (currentIndex - 1).coerceAtLeast(0)
            val endIndex = (currentIndex + 2).coerceAtMost(lyrics.lines.size - 1)

            for (i in startIndex..endIndex) {
                val line = lyrics.lines[i]
                val isCurrent = i == currentIndex
                
                val lineText = buildSpannedString {
                    if (isCurrent) {
                        // Mimic "vibrant" current line: Large and emphasized
                        // We can't really do "bold" easily with Span in Car App Pane text for all hosts, 
                        // but we can use ForegroundColorSpan or just plain text.
                        // Some hosts support bold if we use CarText.
                        append("▶ ")
                        append(line.content.content)
                    } else {
                        append("  ")
                        append(line.content.content)
                    }
                }

                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(CarText.Builder(lineText).build())
                        .build()
                )
            }
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(currentSong.title)
                    .addText(currentSong.artistName)
                    .build()
            )
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.no_lyrics_found))
                    .build()
            )
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeaderAction(Action.BACK)
            .setTitle(carContext.getString(R.string.action_show_lyrics))
            .build()
    }

    private fun findLineIndexAt(position: Long, lyrics: SyncedLyrics): Int {
        if (position < 0) return -1
        val lines = lyrics.lines
        for (i in lines.lastIndex downTo 0) {
            if (position >= lines[i].start) {
                return i
            }
        }
        return -1
    }
}
