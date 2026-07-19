/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.playback.stats

import android.util.Log
import com.mardous.projectmusic.data.local.MetadataReader
import com.mardous.projectmusic.data.local.database.analytics.ListeningSessionEntity
import com.mardous.projectmusic.data.local.database.analytics.ListeningSessionGroupEntity
import com.mardous.projectmusic.data.local.database.analytics.TemporalColumns
import com.mardous.projectmusic.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.min

class RealTimeStatsTracker(
    private val deviceContextCollector: DeviceContextCollector,
    private val sessionCoordinator: SessionCoordinator
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentSession = PendingSession()
    private var flushJob: Job? = null
    private var tickJob: Job? = null
    private val pendingSessions = mutableListOf<ListeningSessionEntity>()
    private val pendingGroups = mutableListOf<ListeningSessionGroupEntity>()
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
        val sessionGroupId: String = "",
        val songDurationMs: Long = 0L,
        val sessionStartWallMs: Long = 0L,
        var nanoBase: Long = 0L,
        var accumulatedPlayedNs: Long = 0L,
        var pauseCount: Int = 0,
        var seekCount: Int = 0,
        var seekForwardCount: Int = 0,
        var seekBackwardCount: Int = 0,
        var pauseStartNano: Long = 0L,
        var totalPauseNs: Long = 0L,
        var wasPaused: Boolean = false,
        var hasStarted: Boolean = false,
        var lastSeekPositionMs: Long = -1L,
        var volumeAtStart: Int = -1,
        var outputDeviceAtStart: String = "",
        var snapshot: DeviceContextCollector.DeviceSnapshot? = null,
        var lyricist: String? = null,
        var publisher: String? = null,
        var hasLyrics: Boolean = false
    )

    private var currentShuffleEnabled: Boolean = false
    private var currentRepeatMode: String = "off"
    private var currentQueuePosition: Int = -1
    private var currentQueueSource: String = ""
    private var currentPlaybackOrigin: String = "unknown"
    private var currentPlaylistId: String = ""
    private var currentPlaylistName: String = ""
    private var currentIsFavorite: Boolean = false
    private var currentEndReason: String = "track_finished"

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
        synchronized(lock) {
            currentShuffleEnabled = shuffleEnabled
            currentRepeatMode = repeatMode
            currentQueuePosition = queuePosition
            currentQueueSource = queueSource
            currentPlaybackOrigin = playbackOrigin
            currentPlaylistId = playlistId
            currentPlaylistName = playlistName
            currentIsFavorite = isFavorite
        }
    }

    fun setEndReason(endReason: String) {
        synchronized(lock) {
            currentEndReason = endReason
        }
    }

    fun onSongStarted(song: Song, initialPositionMs: Long = 0L) {
        synchronized(lock) {
            val nowWall = System.currentTimeMillis()
            val nowNano = System.nanoTime()
            val snapshot = deviceContextCollector.snapshot()
            val groupId = sessionCoordinator.getCurrentGroupId().ifEmpty {
                sessionCoordinator.startNewGroup(nowWall)
            }

            currentSession = PendingSession(
                song = song,
                isActive = true,
                sessionGroupId = groupId,
                songDurationMs = song.duration,
                sessionStartWallMs = nowWall,
                nanoBase = nowNano,
                accumulatedPlayedNs = TimeUnit.MILLISECONDS.toNanos(initialPositionMs),
                pauseCount = 0,
                seekCount = 0,
                seekForwardCount = 0,
                seekBackwardCount = 0,
                pauseStartNano = 0L,
                totalPauseNs = 0L,
                wasPaused = false,
                hasStarted = true,
                lastSeekPositionMs = initialPositionMs,
                volumeAtStart = snapshot.volumeStart,
                outputDeviceAtStart = snapshot.outputDevice,
                snapshot = snapshot
            )
        }
        scope.launch(Dispatchers.IO) {
            try {
                val metadataReader = MetadataReader(song.uri)
                val lyricist = metadataReader.merge(MetadataReader.LYRICIST)
                val publisher = metadataReader.merge(MetadataReader.COPYRIGHT)
                val hasLyrics = metadataReader.value(MetadataReader.LYRICS)?.isNotEmpty() == true
                synchronized(lock) {
                    currentSession.lyricist = lyricist
                    currentSession.publisher = publisher
                    currentSession.hasLyrics = hasLyrics
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read metadata for ${song.title}", e)
            }
        }
        updatePlaybackState()
        startPeriodicFlush()
        startTickUpdates()
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
                    val snapshot = deviceContextCollector.snapshot()
                    return@synchronized buildSession(
                        song = currentSong,
                        snapshot = snapshot,
                        sessionGroupId = currentSession.sessionGroupId,
                        startTime = now - currentPositionMs,
                        endTime = now,
                        playbackDurationMs = currentPositionMs,
                        songDurationMs = currentSong.duration,
                        pauseCount = 0,
                        seekCount = 0,
                        seekForwardCount = 0,
                        seekBackwardCount = 0,
                        pauseDurationMs = 0L,
                        endReason = transitionReason,
                        effectiveListenedMs = currentPositionMs,
                        shuffleEnabled = currentShuffleEnabled,
                        repeatMode = currentRepeatMode,
                        queuePosition = currentQueuePosition,
                        queueSource = currentQueueSource,
                        playbackOrigin = currentPlaybackOrigin,
                        playlistId = currentPlaylistId,
                        playlistName = currentPlaylistName,
                        isFavorite = currentIsFavorite,
                        volumeEnd = snapshot.volumeStart,
                        lyricist = currentSession.lyricist,
                        publisher = currentSession.publisher,
                        isLyrics = currentSession.hasLyrics
                    )
                }
                return@synchronized null
            }

            val nowWall = System.currentTimeMillis()
            val nowNano = System.nanoTime()
            val elapsedSinceResume = if (!currentSession.wasPaused) {
                TimeUnit.NANOSECONDS.toMillis(nowNano - currentSession.nanoBase)
            } else 0L
            val totalPlayedMs = TimeUnit.NANOSECONDS.toMillis(currentSession.accumulatedPlayedNs) + elapsedSinceResume
            val snapshot = currentSession.snapshot ?: deviceContextCollector.snapshot()

            var pauseNs = currentSession.totalPauseNs
            if (currentSession.wasPaused && currentSession.pauseStartNano > 0) {
                pauseNs += nowNano - currentSession.pauseStartNano
            }
            val pauseDurationMs = TimeUnit.NANOSECONDS.toMillis(pauseNs)
            val endSnapshot = deviceContextCollector.snapshot()

            buildSession(
                song = currentSession.song,
                snapshot = snapshot,
                sessionGroupId = currentSession.sessionGroupId,
                startTime = currentSession.sessionStartWallMs,
                endTime = nowWall,
                playbackDurationMs = totalPlayedMs,
                songDurationMs = currentSession.songDurationMs,
                pauseCount = currentSession.pauseCount,
                seekCount = currentSession.seekCount,
                seekForwardCount = currentSession.seekForwardCount,
                seekBackwardCount = currentSession.seekBackwardCount,
                pauseDurationMs = pauseDurationMs,
                endReason = transitionReason,
                effectiveListenedMs = totalPlayedMs,
                shuffleEnabled = currentShuffleEnabled,
                repeatMode = currentRepeatMode,
                queuePosition = currentQueuePosition,
                queueSource = currentQueueSource,
                playbackOrigin = currentPlaybackOrigin,
                playlistId = currentPlaylistId,
                playlistName = currentPlaylistName,
                isFavorite = currentIsFavorite,
                volumeEnd = endSnapshot.volumeStart,
                lyricist = currentSession.lyricist,
                publisher = currentSession.publisher,
                isLyrics = currentSession.hasLyrics
            )
        }

        if (session != null) {
            synchronized(lock) { pendingSessions.add(session) }
            sessionCoordinator.onSongPlayed(
                endMs = session.endTime,
                playbackDurationMs = session.playbackDurationMs
            )
        }

        if (nextSong != Song.emptySong) {
            val nowWall = System.currentTimeMillis()
            val nowNano = System.nanoTime()
            val snapshot = deviceContextCollector.snapshot()
            synchronized(lock) {
                currentSession = PendingSession(
                    song = nextSong,
                    isActive = true,
                    sessionGroupId = sessionCoordinator.getCurrentGroupId(),
                    songDurationMs = nextSong.duration,
                    sessionStartWallMs = nowWall,
                    nanoBase = nowNano,
                    accumulatedPlayedNs = 0L,
                    pauseCount = 0,
                    seekCount = 0,
                    seekForwardCount = 0,
                    seekBackwardCount = 0,
                    pauseStartNano = 0L,
                    totalPauseNs = 0L,
                    wasPaused = false,
                    hasStarted = true,
                    lastSeekPositionMs = -1L,
                    volumeAtStart = snapshot.volumeStart,
                    outputDeviceAtStart = snapshot.outputDevice,
                    snapshot = snapshot
                )
            }
        } else {
            synchronized(lock) {
                currentSession = PendingSession()
            }
            stopTickUpdates()
        }

        updatePlaybackState()
        return session
    }

    fun onPaused() {
        synchronized(lock) {
            if (currentSession.isActive && !currentSession.wasPaused) {
                val nowNano = System.nanoTime()
                currentSession.accumulatedPlayedNs += nowNano - currentSession.nanoBase
                currentSession.wasPaused = true
                currentSession.pauseStartNano = nowNano
                currentSession.pauseCount++
            }
        }
        stopTickUpdates()
        updatePlaybackState()
    }

    fun onResumed() {
        synchronized(lock) {
            if (currentSession.isActive && currentSession.wasPaused) {
                if (currentSession.pauseStartNano > 0) {
                    currentSession.totalPauseNs += System.nanoTime() - currentSession.pauseStartNano
                }
                currentSession.nanoBase = System.nanoTime()
                currentSession.wasPaused = false
            }
        }
        startTickUpdates()
        updatePlaybackState()
    }

    fun onSeekPerformed(oldPositionMs: Long, newPositionMs: Long) {
        synchronized(lock) {
            if (currentSession.isActive) {
                currentSession.seekCount++
                if (newPositionMs > oldPositionMs + 1000) {
                    currentSession.seekForwardCount++
                } else if (newPositionMs < oldPositionMs - 1000) {
                    currentSession.seekBackwardCount++
                }
                currentSession.lastSeekPositionMs = newPositionMs
            }
        }
        updatePlaybackState()
    }

    fun onPositionChanged(positionMs: Long) {
        synchronized(lock) {
            if (currentSession.isActive) {
                currentSession.lastSeekPositionMs = positionMs
            }
        }
    }

    fun flushCurrentSession(endReason: String = "app_closed"): ListeningSessionEntity? {
        val session = synchronized(lock) {
            if (!currentSession.isActive || !currentSession.hasStarted) return@synchronized null
            if (currentSession.song == Song.emptySong) return@synchronized null

            val nowWall = System.currentTimeMillis()
            val nowNano = System.nanoTime()
            val elapsedSinceResume = if (!currentSession.wasPaused) {
                TimeUnit.NANOSECONDS.toMillis((nowNano - currentSession.nanoBase).coerceAtLeast(0L))
            } else 0L
            val totalPlayed = TimeUnit.NANOSECONDS.toMillis(currentSession.accumulatedPlayedNs.coerceAtLeast(0L)) + elapsedSinceResume
            val snapshot = currentSession.snapshot ?: deviceContextCollector.snapshot()

            var pauseNs = currentSession.totalPauseNs
            if (currentSession.wasPaused && currentSession.pauseStartNano > 0) {
                pauseNs += nowNano - currentSession.pauseStartNano
            }
            val endSnapshot = deviceContextCollector.snapshot()

            buildSession(
                song = currentSession.song,
                snapshot = snapshot,
                sessionGroupId = currentSession.sessionGroupId,
                startTime = currentSession.sessionStartWallMs,
                endTime = nowWall,
                playbackDurationMs = totalPlayed,
                songDurationMs = currentSession.songDurationMs,
                pauseCount = currentSession.pauseCount,
                seekCount = currentSession.seekCount,
                seekForwardCount = currentSession.seekForwardCount,
                seekBackwardCount = currentSession.seekBackwardCount,
                pauseDurationMs = TimeUnit.NANOSECONDS.toMillis(pauseNs),
                endReason = endReason,
                effectiveListenedMs = totalPlayed,
                shuffleEnabled = currentShuffleEnabled,
                repeatMode = currentRepeatMode,
                queuePosition = currentQueuePosition,
                queueSource = currentQueueSource,
                playbackOrigin = currentPlaybackOrigin,
                playlistId = currentPlaylistId,
                playlistName = currentPlaylistName,
                isFavorite = currentIsFavorite,
                volumeEnd = endSnapshot.volumeStart,
                lyricist = currentSession.lyricist,
                publisher = currentSession.publisher,
                isLyrics = currentSession.hasLyrics
            ).also {
                currentSession = PendingSession()
            }
        }
        if (session != null) {
            synchronized(lock) { pendingSessions.add(session) }
            sessionCoordinator.onSongPlayed(
                endMs = session.endTime,
                playbackDurationMs = session.playbackDurationMs
            )
        }
        stopTickUpdates()
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

    fun drainPendingGroups(): List<ListeningSessionGroupEntity> {
        synchronized(lock) {
            val copy = pendingGroups.toList()
            pendingGroups.clear()
            return copy
        }
    }

    fun endCurrentSessionGroup(): ListeningSessionGroupEntity? {
        val summary = sessionCoordinator.onSessionEnd() ?: return null
        val group = ListeningSessionGroupEntity(
            groupId = summary.groupId,
            totalSongs = summary.totalSongs,
            totalDurationMs = summary.totalPlaybackDurationMs,
            startTime = summary.startTime,
            endTime = summary.endTime
        )
        synchronized(lock) { pendingGroups.add(group) }
        return group
    }

    fun peekPendingCount(): Int = synchronized(lock) { pendingSessions.size }

    fun isTrackingActive(): Boolean = synchronized(lock) { currentSession.isActive }

    fun reset() {
        synchronized(lock) {
            currentSession = PendingSession()
            pendingSessions.clear()
            pendingGroups.clear()
        }
        sessionCoordinator.reset()
        flushJob?.cancel()
        flushJob = null
        stopTickUpdates()
        updatePlaybackState()
    }

    private fun startTickUpdates() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                delay(1000L)
                updatePlaybackState()
            }
        }
    }

    private fun stopTickUpdates() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun startPeriodicFlush() {
        flushJob?.cancel()
        flushJob = scope.launch {
            while (isActive) {
                delay(30000L)
                updatePlaybackState()
            }
        }
    }

    fun getCurrentElapsedMs(): Long {
        synchronized(lock) {
            if (!currentSession.isActive) return 0L
            val nowNano = System.nanoTime()
            val elapsedSinceResume = if (!currentSession.wasPaused) {
                TimeUnit.NANOSECONDS.toMillis((nowNano - currentSession.nanoBase).coerceAtLeast(0L))
            } else 0L
            val total = TimeUnit.NANOSECONDS.toMillis(currentSession.accumulatedPlayedNs.coerceAtLeast(0L)) + elapsedSinceResume
            return min(total, currentSession.songDurationMs)
        }
    }

    private fun updatePlaybackState() {
        synchronized(lock) {
            val elapsedMs = getCurrentElapsedMs()

            _currentPlaybackState.value = PlaybackState(
                isTracking = currentSession.isActive,
                songTitle = currentSession.song.title,
                artistName = currentSession.song.artistName,
                elapsedMs = elapsedMs,
                durationMs = currentSession.songDurationMs,
                pauseCount = currentSession.pauseCount,
                seekCount = currentSession.seekCount,
                sessionStartMs = currentSession.sessionStartWallMs
            )
        }
    }

    companion object {
        private const val TAG = "RealTimeStatsTracker"

        private fun buildSession(
            song: Song,
            snapshot: DeviceContextCollector.DeviceSnapshot,
            sessionGroupId: String,
            startTime: Long,
            endTime: Long,
            playbackDurationMs: Long,
            songDurationMs: Long,
            pauseCount: Int,
            seekCount: Int,
            seekForwardCount: Int,
            seekBackwardCount: Int,
            pauseDurationMs: Long,
            endReason: String,
            effectiveListenedMs: Long,
            shuffleEnabled: Boolean = false,
            repeatMode: String = "off",
            queuePosition: Int = -1,
            queueSource: String = "",
            playbackOrigin: String = "unknown",
            playlistId: String = "",
            playlistName: String = "",
            isFavorite: Boolean = false,
            volumeEnd: Int = -1,
            lyricist: String? = null,
            publisher: String? = null,
            isLyrics: Boolean = false
        ): ListeningSessionEntity {
            val temporal = TemporalColumns.compute(startTime)
            val completionPercent = if (songDurationMs > 0) {
                (playbackDurationMs.toDouble() / songDurationMs.toDouble() * 100.0).coerceIn(0.0, 100.0)
            } else 0.0

            val endReasonFinal = when {
                endReason == "track_finished" && completionPercent < 90.0 -> "early_completion"
                else -> endReason
            }

            return ListeningSessionEntity(
                sessionGroupId = sessionGroupId,
                songId = song.id,
                artistId = song.artistId,
                songTitle = song.title,
                artistName = song.artistName,
                albumArtist = song.albumArtistName,
                albumId = song.albumId,
                albumName = song.albumName,
                genre = song.genreName,
                releaseYear = song.year,
                composer = song.composer,
                lyricist = lyricist,
                publisher = publisher,
                isLyrics = isLyrics,
                songDurationMs = songDurationMs,
                audioFormat = snapshot.audioFormat,
                audioSampleRate = snapshot.audioSampleRate,
                audioChannelCount = snapshot.audioChannelCount,
                bitrateKbps = snapshot.bitrateKbps,
                playbackSpeed = snapshot.playbackSpeed,
                equalizerActive = snapshot.equalizerActive,
                startTime = startTime,
                endTime = endTime,
                timeStandard = "UTC",
                timezoneId = temporal.timezoneId,
                timezoneOffsetMinutes = temporal.timezoneOffsetMinutes,
                startDate = temporal.startDate,
                startTimeOnly = temporal.startTimeOnly,
                dayOfWeek = temporal.dayOfWeek,
                dayOfMonth = temporal.dayOfMonth,
                dayOfYear = temporal.dayOfYear,
                weekOfYear = temporal.weekOfYear,
                month = temporal.month,
                monthName = temporal.monthName,
                quarter = temporal.quarter,
                year = temporal.year,
                yearMonth = temporal.yearMonth,
                yearWeek = temporal.yearWeek,
                hour = temporal.hour,
                minute = temporal.minute,
                second = temporal.second,
                timePeriod = temporal.timePeriod,
                isWeekend = temporal.isWeekend,
                playbackDurationMs = playbackDurationMs,
                effectiveListenedMs = effectiveListenedMs,
                completionPercent = completionPercent,
                endReason = endReasonFinal,
                pauseCount = pauseCount,
                pauseDurationMs = pauseDurationMs,
                seekCount = seekCount,
                seekForwardCount = seekForwardCount,
                seekBackwardCount = seekBackwardCount,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                queuePosition = queuePosition,
                queueSource = queueSource,
                playbackOrigin = playbackOrigin,
                playlistId = playlistId,
                playlistName = playlistName,
                isFavorite = isFavorite,
                outputDevice = snapshot.outputDevice,
                volumeStart = snapshot.volumeStart,
                volumeEnd = volumeEnd,
                batteryLevel = snapshot.batteryLevel,
                charging = snapshot.charging,
                screenOn = snapshot.screenOn,
                appVersion = snapshot.appVersion
            )
        }
    }
}
