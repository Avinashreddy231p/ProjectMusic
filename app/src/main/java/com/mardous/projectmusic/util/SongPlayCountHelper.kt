package com.mardous.projectmusic.util

import com.mardous.projectmusic.data.model.Song

class SongPlayCountHelper {

    private val stopWatch = StopWatch()
    var song = Song.emptySong
        private set

    fun shouldBumpPlayCount(): Boolean {
        val percentage = com.mardous.projectmusic.util.Preferences.lastfmScrobblePercentage / 100f
        return song.duration * percentage < stopWatch.elapsedTime
    }

    fun notifySongChanged(song: Song, isPlaying: Boolean) {
        synchronized(this) {
            stopWatch.reset()
            if (isPlaying) {
                stopWatch.start()
            }
            this.song = song
        }
    }

    fun notifyPlayStateChanged(isPlaying: Boolean) {
        synchronized(this) {
            if (isPlaying) {
                stopWatch.start()
            } else {
                stopWatch.pause()
            }
        }
    }

    companion object {
        val TAG: String = SongPlayCountHelper::class.java.simpleName
    }
}