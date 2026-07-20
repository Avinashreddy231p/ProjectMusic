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
 * along with this program.  See the GNU General Public License
 * for more details.
 *
 */

package com.mardous.projectmusic.playback.stats

import android.util.Log
import com.mardous.projectmusic.data.local.repository.StatsRepository
import com.mardous.projectmusic.data.local.database.analytics.ListeningSessionEntity
import com.mardous.projectmusic.data.local.database.analytics.ListeningSessionGroupEntity
import com.mardous.projectmusic.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.mardous.projectmusic.data.local.database.sync.RankingEngine

class StatsFlusher(
    private val tracker: RealTimeStatsTracker,
    private val statsRepository: StatsRepository,
    private val rankingEngine: RankingEngine? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flushJob: Job? = null
    private val mutex = Mutex()

    fun startPeriodicFlush(intervalMs: Long = 30_000L) {
        stopPeriodicFlush()
        flushJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(intervalMs)
                flushPendingSessions()
            }
        }
    }

    private fun launchFlush() {
        scope.launch { flushPendingSessions() }
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
        launchFlush()
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
        val allGroups = mutableListOf<ListeningSessionGroupEntity>()
        if (group != null) allGroups.add(group)
        allGroups.addAll(remainingGroups)

        if (allSessions.isNotEmpty() || allGroups.isNotEmpty()) {
            kotlinx.coroutines.runBlocking {
                try {
                    if (allGroups.isNotEmpty()) {
                        statsRepository.insertGroups(allGroups)
                    }
                    if (allSessions.isNotEmpty()) {
                        statsRepository.insertSessions(allSessions)
                    }
                    rankingEngine?.markDirty()
                    rankingEngine?.batchRecalculate()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to flush sessions on stop", e)
                }
            }
        }
        return allSessions
    }

    fun forceFlushNow() {
        launchFlush()
    }

    private suspend fun flushPendingSessions() {
        mutex.withLock {
            try {
                val sessions = tracker.drainPendingSessions()
                val groups = tracker.drainPendingGroups()
                if (groups.isNotEmpty()) {
                    statsRepository.insertGroups(groups)
                }
                if (sessions.isNotEmpty()) {
                    statsRepository.insertSessions(sessions)
                    rankingEngine?.markDirty()
                    rankingEngine?.batchRecalculate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush pending sessions", e)
            }
        }
    }

    fun reset() {
        stopPeriodicFlush()
        tracker.reset()
    }

    fun stop() {
        stopPeriodicFlush()
        scope.cancel()
    }

    companion object {
        private const val TAG = "StatsFlusher"
    }
}
