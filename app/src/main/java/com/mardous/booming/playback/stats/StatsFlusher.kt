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
        val session = tracker.onSongTransition(
            currentSong = currentSong,
            nextSong = nextSong,
            currentPositionMs = currentPositionMs,
            transitionReason = transitionReason,
            maxDurationMs = maxDurationMs
        )
        if (session != null) {
            flushSingleSession(session)
        }
        flushPendingSessions()
    }

    fun onPlayPause(isPlaying: Boolean) {
        if (isPlaying) {
            tracker.onResumed()
        } else {
            tracker.onPaused()
        }
    }

    fun onSeek() {
        tracker.onSeekPerformed()
    }

    fun flushAndStop(endReason: String = "app_closed"): List<ListeningSessionEntity> {
        stopPeriodicFlush()
        val lastSession = tracker.flushCurrentSession(endReason)
        val remaining = tracker.drainPendingSessions()
        val allSessions = mutableListOf<ListeningSessionEntity>()
        if (lastSession != null) allSessions.add(lastSession)
        allSessions.addAll(remaining)
        if (allSessions.isNotEmpty()) {
            scope.launch {
                try {
                    statsRepository.insertSessions(allSessions)
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

    private fun flushSingleSession(session: ListeningSessionEntity) {
        scope.launch {
            try {
                statsRepository.insertSession(session)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert session", e)
            }
        }
    }

    private fun flushPendingSessions() {
        if (isFlushing) return
        isFlushing = true
        scope.launch {
            try {
                val sessions = tracker.drainPendingSessions()
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
