package com.mardous.projectmusic.playback.stats

import java.util.UUID

class SessionCoordinator {

    private var currentGroupId: String = ""
    private var songCount: Int = 0
    private var groupStartMs: Long = 0L
    private var lastEndMs: Long = 0L
    private var accumulatedPlaybackMs: Long = 0L
    private val lock = Any()

    data class SessionGroupSummary(
        val groupId: String,
        val totalSongs: Int,
        val totalPlaybackDurationMs: Long,
        val startTime: Long,
        val endTime: Long
    )

    fun startNewGroup(startMs: Long = System.currentTimeMillis()): String {
        synchronized(lock) {
            val uuid = UUID.randomUUID().toString()
            currentGroupId = uuid
            songCount = 0
            accumulatedPlaybackMs = 0L
            groupStartMs = startMs
            lastEndMs = startMs
            return uuid
        }
    }

    fun onSongPlayed(endMs: Long = System.currentTimeMillis(), playbackDurationMs: Long = 0L) {
        synchronized(lock) {
            songCount++
            lastEndMs = endMs
            accumulatedPlaybackMs += playbackDurationMs
        }
    }

    fun getCurrentGroupId(): String = synchronized(lock) { currentGroupId }

    fun onSessionEnd(endMs: Long = System.currentTimeMillis()): SessionGroupSummary? {
        synchronized(lock) {
            val count = songCount
            if (count == 0) return null

            val groupId = currentGroupId.ifEmpty { UUID.randomUUID().toString() }
            val start = groupStartMs
            val end = endMs.coerceAtLeast(start)

            val summary = SessionGroupSummary(
                groupId = groupId,
                totalSongs = count,
                totalPlaybackDurationMs = accumulatedPlaybackMs,
                startTime = start,
                endTime = end
            )

            reset()
            return summary
        }
    }

    fun peekSongCount(): Int = synchronized(lock) { songCount }

    fun reset() {
        synchronized(lock) {
            currentGroupId = ""
            songCount = 0
            accumulatedPlaybackMs = 0L
            groupStartMs = 0L
            lastEndMs = 0L
        }
    }
}
