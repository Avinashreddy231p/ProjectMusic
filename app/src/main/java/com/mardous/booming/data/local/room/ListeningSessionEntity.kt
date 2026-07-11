package com.mardous.booming.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "listening_sessions",
    indices = [
        Index(value = ["song_id"]),
        Index(value = ["year_month"]),
        Index(value = ["start_time"]),
        Index(value = ["song_id", "year_month"])
    ]
)
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "session_id")
    val sessionId: Long = 0,
    @ColumnInfo(name = "song_id")
    val songId: Long,
    @ColumnInfo(name = "song_title")
    val songTitle: String,
    @ColumnInfo(name = "artist_name")
    val artistName: String,
    @ColumnInfo(name = "album_artist")
    val albumArtist: String?,
    @ColumnInfo(name = "album_name")
    val albumName: String,
    @ColumnInfo(name = "album_id")
    val albumId: Long,
    val composer: String?,
    val lyricist: String?,
    val publisher: String?,
    val genre: String?,
    val year: Int,
    @ColumnInfo(name = "year_month")
    val yearMonth: String,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long,
    @ColumnInfo(name = "song_duration_ms")
    val songDurationMs: Long,
    @ColumnInfo(name = "duration_played_ms")
    val durationPlayedMs: Long,
    val completed: Boolean,
    val skipped: Boolean,
    @ColumnInfo(name = "pause_count")
    val pauseCount: Int,
    @ColumnInfo(name = "seek_count")
    val seekCount: Int,
    @ColumnInfo(name = "listened_seconds_ms")
    val listenedSecondsMs: Long,
    @ColumnInfo(name = "end_reason")
    val endReason: String
)
