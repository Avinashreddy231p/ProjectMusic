package com.mardous.projectmusic.playback.stats

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class SessionCoordinator {

    private val currentGroupId = AtomicReference<String>("")
    private val songCount = AtomicInteger(0)
    private val groupStartMs = AtomicReference<Long>(0L)
    private val lastEndMs = AtomicReference<Long>(0L)
    private val accumulatedPlaybackMs = AtomicLong(0L)
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
            currentGroupId.set(uuid)
            songCount.set(0)
            accumulatedPlaybackMs.set(0L)
            groupStartMs.set(startMs)
            lastEndMs.set(startMs)
            return uuid
        }
    }

    fun onSongPlayed(endMs: Long = System.currentTimeMillis(), playbackDurationMs: Long = 0L) {
        synchronized(lock) {
            songCount.incrementAndGet()
            lastEndMs.set(endMs)
            accumulatedPlaybackMs.addAndGet(playbackDurationMs)
        }
    }

    fun getCurrentGroupId(): String = synchronized(lock) { currentGroupId.get() }

    fun onSessionEnd(endMs: Long = System.currentTimeMillis()): SessionGroupSummary? {
        synchronized(lock) {
            val count = songCount.get()
            if (count == 0) return null

            val groupId = currentGroupId.get().ifEmpty { UUID.randomUUID().toString() }
            val start = groupStartMs.get()
            val end = endMs.coerceAtLeast(start)

            val summary = SessionGroupSummary(
                groupId = groupId,
                totalSongs = count,
                totalPlaybackDurationMs = accumulatedPlaybackMs.get(),
                startTime = start,
                endTime = end
            )

            reset()
            return summary
        }
    }

    fun peekSongCount(): Int = songCount.get()

    fun reset() {
        synchronized(lock) {
            currentGroupId.set("")
            songCount.set(0)
            accumulatedPlaybackMs.set(0L)
            groupStartMs.set(0L)
            lastEndMs.set(0L)
        }
    }
}
