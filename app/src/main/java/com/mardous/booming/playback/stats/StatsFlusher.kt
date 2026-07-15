package com.mardous.booming.playback.stats

import android.util.Log
import com.mardous.booming.data.local.repository.StatsRepository
import com.mardous.booming.data.local.room.ListeningSessionEntity
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StatsFlusher(
    private val tracker: RealTimeStatsTracker,
    private val statsRepository: StatsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flushJob: Job? = null
    private var isFlushing = false

    fun startPeriodicFlush(intervalMs: Long = 30_000L) {
        stopPeriodicFlush()
        flushJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(intervalMs)
                flushPendingSessions()
            }
        }
    }

    fun stopPeriodicFlush() {
        flushJob?.cancel()
        flushJob = null
    }

    fun onSongStarted(song: Song, initialPositionMs: Long = 0L) {
        tracker.onSongStarted(song, initialPositionMs)
    }

    fun onSongTransition(
        currentSong: Song,
        nextSong: Song,
        currentPositionMs: Long,
        transitionReason: String,
        maxDurationMs: Long
    ) {
        tracker.onSongTransition(
            currentSong = currentSong,
            nextSong = nextSong,
            currentPositionMs = currentPositionMs,
            transitionReason = transitionReason,
            maxDurationMs = maxDurationMs
        )
        flushPendingSessions()
    }

    fun onPlayPause(isPlaying: Boolean) {
        if (isPlaying) {
            tracker.onResumed()
        } else {
            tracker.onPaused()
        }
    }

    fun onSeek(oldPositionMs: Long, newPositionMs: Long) {
        tracker.onSeekPerformed(oldPositionMs, newPositionMs)
    }

    fun setPlaybackContext(
        shuffleEnabled: Boolean = false,
        repeatMode: String = "off",
        queuePosition: Int = -1,
        queueSource: String = "",
        playbackOrigin: String = "unknown",
        playlistId: String = "",
        playlistName: String = "",
        isFavorite: Boolean = false
    ) {
        tracker.setPlaybackContext(
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            queuePosition = queuePosition,
            queueSource = queueSource,
            playbackOrigin = playbackOrigin,
            playlistId = playlistId,
            playlistName = playlistName,
            isFavorite = isFavorite
        )
    }

    fun setEndReason(endReason: String) {
        tracker.setEndReason(endReason)
    }

    fun flushAndStop(endReason: String = "app_closed"): List<ListeningSessionEntity> {
        stopPeriodicFlush()
        tracker.flushCurrentSession(endReason)
        val allSessions = tracker.drainPendingSessions()

        val group = tracker.endCurrentSessionGroup()
        val remainingGroups = tracker.drainPendingGroups()
        val allGroups = mutableListOf<com.mardous.booming.data.local.room.ListeningSessionGroupEntity>()
        if (group != null) allGroups.add(group)
        allGroups.addAll(remainingGroups)

        if (allSessions.isNotEmpty() || allGroups.isNotEmpty()) {
            scope.launch {
                try {
                    if (allGroups.isNotEmpty()) {
                        statsRepository.insertGroups(allGroups)
                    }
                    if (allSessions.isNotEmpty()) {
                        statsRepository.insertSessions(allSessions)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to flush sessions on stop", e)
                }
            }
        }
        return allSessions
    }

    fun forceFlushNow() {
        flushPendingSessions()
    }

    private fun flushPendingSessions() {
        if (isFlushing) return
        isFlushing = true
        scope.launch {
            try {
                val sessions = tracker.drainPendingSessions()
                val groups = tracker.drainPendingGroups()
                if (groups.isNotEmpty()) {
                    statsRepository.insertGroups(groups)
                }
                if (sessions.isNotEmpty()) {
                    statsRepository.insertSessions(sessions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush pending sessions", e)
            } finally {
                isFlushing = false
            }
        }
    }

    fun reset() {
        stopPeriodicFlush()
        tracker.reset()
    }

    companion object {
        private const val TAG = "StatsFlusher"
    }
}
