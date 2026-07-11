package com.mardous.booming.playback.stats

import android.util.Log
import com.mardous.booming.data.local.room.ListeningSessionEntity
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class RealTimeStatsTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentSession = PendingSession()
    private var flushJob: Job? = null
    private val pendingSessions = mutableListOf<ListeningSessionEntity>()
    private val lock = Any()

    private val _currentPlaybackState = MutableStateFlow(PlaybackState())
    val currentPlaybackState = _currentPlaybackState.asStateFlow()

    data class PlaybackState(
        val isTracking: Boolean = false,
        val songTitle: String = "",
        val artistName: String = "",
        val elapsedMs: Long = 0L,
        val durationMs: Long = 0L,
        val pauseCount: Int = 0,
        val seekCount: Int = 0,
        val sessionStartMs: Long = 0L
    )

    private data class PendingSession(
        val song: Song = Song.emptySong,
        val isActive: Boolean = false,
        val songDurationMs: Long = 0L,
        val sessionStartTimeMs: Long = 0L,
        var lastResumeTimeMs: Long = 0L,
        var accumulatedPlayedMs: Long = 0L,
        var pauseCount: Int = 0,
        var seekCount: Int = 0,
        var wasPaused: Boolean = false,
        var hasStarted: Boolean = false,
        var lastSeekPositionMs: Long = -1L
    )

    fun onSongStarted(song: Song, initialPositionMs: Long = 0L) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            currentSession = PendingSession(
                song = song,
                isActive = true,
                songDurationMs = song.duration,
                sessionStartTimeMs = now,
                lastResumeTimeMs = now,
                accumulatedPlayedMs = 0L,
                pauseCount = 0,
                seekCount = 0,
                wasPaused = false,
                hasStarted = true,
                lastSeekPositionMs = -1L
            )
        }
        updatePlaybackState()
        startPeriodicFlush()
    }

    fun onSongTransition(
        currentSong: Song,
        nextSong: Song,
        currentPositionMs: Long,
        transitionReason: String,
        maxDurationMs: Long
    ): ListeningSessionEntity? {
        val session = synchronized(lock) {
            if (!currentSession.isActive || currentSession.song == Song.emptySong) {
                val now = System.currentTimeMillis()
                if (currentSession.hasStarted || (currentSession.song == Song.emptySong && currentSong != Song.emptySong)) {
                    return@synchronized buildSession(
                        song = currentSong,
                        startTime = now - currentPositionMs,
                        endTime = now,
                        durationPlayedMs = currentPositionMs,
                        songDurationMs = currentSong.duration,
                        pauseCount = 0,
                        seekCount = 0,
                        completed = currentPositionMs >= currentSong.duration * 0.9,
                        skipped = transitionReason != "track_finished" && transitionReason != "repeat",
                        endReason = transitionReason,
                        listenedMs = currentPositionMs
                    )
                }
                return@synchronized null
            }

            val now = System.currentTimeMillis()
            val elapsedSinceResume = if (!currentSession.wasPaused) {
                now - currentSession.lastResumeTimeMs
            } else 0L
            val totalPlayed = currentSession.accumulatedPlayedMs + elapsedSinceResume
            val adjustedDuration = min(totalPlayed, currentSession.songDurationMs)
            val completedThreshold = currentSession.songDurationMs * 0.9
            val isCompleted = adjustedDuration >= completedThreshold
            val isSkipped = transitionReason != "track_finished" && transitionReason != "repeat"

            buildSession(
                song = currentSession.song,
                startTime = currentSession.sessionStartTimeMs,
                endTime = now,
                durationPlayedMs = totalPlayed,
                songDurationMs = currentSession.songDurationMs,
                pauseCount = currentSession.pauseCount,
                seekCount = currentSession.seekCount,
                completed = isCompleted,
                skipped = isSkipped,
                endReason = transitionReason,
                listenedMs = totalPlayed
            )
        }

        if (session != null) {
            synchronized(lock) { pendingSessions.add(session) }
        }

        if (nextSong != Song.emptySong) {
            val now = System.currentTimeMillis()
            synchronized(lock) {
                currentSession = PendingSession(
                    song = nextSong,
                    isActive = true,
                    songDurationMs = nextSong.duration,
                    sessionStartTimeMs = now,
                    lastResumeTimeMs = now,
                    accumulatedPlayedMs = 0L,
                    pauseCount = 0,
                    seekCount = 0,
                    wasPaused = false,
                    hasStarted = true,
                    lastSeekPositionMs = -1L
                )
            }
        } else {
            synchronized(lock) {
                currentSession = PendingSession()
            }
        }

        updatePlaybackState()
        return session
    }

    fun onPaused() {
        synchronized(lock) {
            if (currentSession.isActive && !currentSession.wasPaused) {
                val now = System.currentTimeMillis()
                currentSession.accumulatedPlayedMs += now - currentSession.lastResumeTimeMs
                currentSession.wasPaused = true
                currentSession.pauseCount++
            }
        }
        updatePlaybackState()
    }

    fun onResumed() {
        synchronized(lock) {
            if (currentSession.isActive && currentSession.wasPaused) {
                currentSession.lastResumeTimeMs = System.currentTimeMillis()
                currentSession.wasPaused = false
            }
        }
        updatePlaybackState()
    }

    fun onSeekPerformed() {
        synchronized(lock) {
            if (currentSession.isActive) {
                currentSession.seekCount++
            }
        }
        updatePlaybackState()
    }

    fun onPositionChanged(positionMs: Long) {
        synchronized(lock) {
            if (currentSession.isActive) {
                currentSongPositionMs = positionMs
            }
        }
    }

    fun flushCurrentSession(endReason: String = "app_closed"): ListeningSessionEntity? {
        val session = synchronized(lock) {
            if (!currentSession.isActive || !currentSession.hasStarted) return@synchronized null
            if (currentSession.song == Song.emptySong) return@synchronized null

            val now = System.currentTimeMillis()
            val elapsedSinceResume = if (!currentSession.wasPaused) {
                (now - currentSession.lastResumeTimeMs).coerceAtLeast(0L)
            } else 0L
            val totalPlayed = (currentSession.accumulatedPlayedMs + elapsedSinceResume)
                .coerceAtMost(currentSession.songDurationMs)
            val completedThreshold = currentSession.songDurationMs * 0.9
            val isCompleted = totalPlayed >= completedThreshold

            buildSession(
                song = currentSession.song,
                startTime = currentSession.sessionStartTimeMs,
                endTime = now,
                durationPlayedMs = totalPlayed,
                songDurationMs = currentSession.songDurationMs,
                pauseCount = currentSession.pauseCount,
                seekCount = currentSession.seekCount,
                completed = isCompleted,
                skipped = false,
                endReason = endReason,
                listenedMs = totalPlayed
            ).also {
                currentSession = PendingSession()
            }
        }
        if (session != null) {
            synchronized(lock) { pendingSessions.add(session) }
        }
        updatePlaybackState()
        return session
    }

    fun drainPendingSessions(): List<ListeningSessionEntity> {
        synchronized(lock) {
            val copy = pendingSessions.toList()
            pendingSessions.clear()
            return copy
        }
    }

    fun peekPendingCount(): Int = synchronized(lock) { pendingSessions.size }

    fun isTrackingActive(): Boolean = synchronized(lock) { currentSession.isActive }

    fun reset() {
        synchronized(lock) {
            currentSession = PendingSession()
            pendingSessions.clear()
        }
        flushJob?.cancel()
        flushJob = null
        updatePlaybackState()
    }

    private fun startPeriodicFlush() {
        flushJob?.cancel()
        flushJob = scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                updatePlaybackState()
            }
        }
    }

    private var currentSongPositionMs: Long = 0L

    private fun updatePlaybackState() {
        synchronized(lock) {
            val elapsedMs = if (currentSession.isActive) {
                val elapsedSinceResume = if (!currentSession.wasPaused && currentSession.lastResumeTimeMs > 0) {
                    System.currentTimeMillis() - currentSession.lastResumeTimeMs
                } else 0L
                (currentSession.accumulatedPlayedMs + elapsedSinceResume).coerceAtMost(currentSession.songDurationMs)
            } else 0L

            _currentPlaybackState.value = PlaybackState(
                isTracking = currentSession.isActive,
                songTitle = currentSession.song.title,
                artistName = currentSession.song.artistName,
                elapsedMs = elapsedMs,
                durationMs = currentSession.songDurationMs,
                pauseCount = currentSession.pauseCount,
                seekCount = currentSession.seekCount,
                sessionStartMs = currentSession.sessionStartTimeMs
            )
        }
    }

    companion object {
        private const val TAG = "RealTimeStatsTracker"
        private const val FLUSH_INTERVAL_MS = 30000L

        private val dateFormat = SimpleDateFormat("yyyy-MM", Locale.US)

        private fun getYearMonth(epochMs: Long): String {
            return dateFormat.format(Date(epochMs))
        }

        private fun buildSession(
            song: Song,
            startTime: Long,
            endTime: Long,
            durationPlayedMs: Long,
            songDurationMs: Long,
            pauseCount: Int,
            seekCount: Int,
            completed: Boolean,
            skipped: Boolean,
            endReason: String,
            listenedMs: Long
        ): ListeningSessionEntity {
            return ListeningSessionEntity(
                songId = song.id,
                songTitle = song.title,
                artistName = song.artistName,
                albumArtist = song.albumArtistName,
                albumName = song.albumName,
                albumId = song.albumId,
                composer = null,
                lyricist = null,
                publisher = null,
                genre = song.genreName,
                year = song.year,
                yearMonth = getYearMonth(startTime),
                startTime = startTime,
                endTime = endTime,
                songDurationMs = songDurationMs,
                durationPlayedMs = durationPlayedMs,
                completed = completed,
                skipped = skipped,
                pauseCount = pauseCount,
                seekCount = seekCount,
                listenedSecondsMs = listenedMs,
                endReason = endReason
            )
        }
    }
}
