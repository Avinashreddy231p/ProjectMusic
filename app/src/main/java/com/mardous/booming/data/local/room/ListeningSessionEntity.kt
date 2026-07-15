package com.mardous.booming.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_sessions",
    indices = [
        Index(value = ["song_id"]),
        Index(value = ["session_group_id"]),
        Index(value = ["artist_id"]),
        Index(value = ["album_id"]),
        Index(value = ["start_time"]),
        Index(value = ["start_date"]),
        Index(value = ["year_month"]),
        Index(value = ["year_week"]),
        Index(value = ["year"]),
        Index(value = ["month"]),
        Index(value = ["quarter"]),
        Index(value = ["hour"]),
        Index(value = ["day_of_week"]),
        Index(value = ["time_period"]),
        Index(value = ["is_weekend"]),
        Index(value = ["end_reason"]),
        Index(value = ["shuffle_enabled"]),
        Index(value = ["repeat_mode"]),
        Index(value = ["is_favorite"]),
        Index(value = ["output_device"]),
        Index(value = ["queue_source"]),
        Index(value = ["playback_origin"]),
        Index(value = ["audio_format"]),
        Index(value = ["song_id", "year_month"]),
        Index(value = ["artist_name", "start_time"]),
        Index(value = ["album_id", "start_time"]),
        Index(value = ["genre", "start_time"]),
        Index(value = ["start_date", "hour"]),
        Index(value = ["playlist_id", "start_time"]),
        Index(value = ["playback_origin", "start_time"])
    ]
)
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "session_id")
    val sessionId: Long = 0,

    // Identity (4)
    @ColumnInfo(name = "session_group_id")
    val sessionGroupId: String = "",
    @ColumnInfo(name = "song_id")
    val songId: Long = 0,
    @ColumnInfo(name = "artist_id")
    val artistId: Long = -1,

    // Song Metadata (10)
    @ColumnInfo(name = "song_title")
    val songTitle: String = "",
    @ColumnInfo(name = "artist_name")
    val artistName: String = "",
    @ColumnInfo(name = "album_artist")
    val albumArtist: String? = null,
    @ColumnInfo(name = "album_id")
    val albumId: Long = 0,
    @ColumnInfo(name = "album_name")
    val albumName: String = "",
    val genre: String? = null,
    @ColumnInfo(name = "release_year")
    val releaseYear: Int = 0,
    val composer: String? = null,
    val lyricist: String? = null,
    val publisher: String? = null,
    @ColumnInfo(name = "is_lyrics")
    val isLyrics: Boolean = false,

    // Audio Info (7)
    @ColumnInfo(name = "song_duration_ms")
    val songDurationMs: Long = 0,
    @ColumnInfo(name = "audio_format")
    val audioFormat: String = "",
    @ColumnInfo(name = "audio_sample_rate")
    val audioSampleRate: Int = 0,
    @ColumnInfo(name = "audio_channel_count")
    val audioChannelCount: Int = 0,
    @ColumnInfo(name = "bitrate_kbps")
    val bitrateKbps: Int = 0,
    @ColumnInfo(name = "playback_speed")
    val playbackSpeed: Float = 1.0f,
    @ColumnInfo(name = "equalizer_active")
    val equalizerActive: Boolean = false,

    // Temporal (20)
    @ColumnInfo(name = "start_time")
    val startTime: Long = 0,
    @ColumnInfo(name = "end_time")
    val endTime: Long = 0,
    @ColumnInfo(name = "time_standard")
    val timeStandard: String = "UTC",
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String = "",
    @ColumnInfo(name = "timezone_offset_minutes")
    val timezoneOffsetMinutes: Int = 0,
    @ColumnInfo(name = "start_date")
    val startDate: String = "",
    @ColumnInfo(name = "start_time_only")
    val startTimeOnly: String = "",
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: String = "",
    @ColumnInfo(name = "day_of_month")
    val dayOfMonth: Int = 0,
    @ColumnInfo(name = "day_of_year")
    val dayOfYear: Int = 0,
    @ColumnInfo(name = "week_of_year")
    val weekOfYear: Int = 0,
    val month: Int = 0,
    @ColumnInfo(name = "month_name")
    val monthName: String = "",
    val quarter: Int = 0,
    val year: Int = 0,
    @ColumnInfo(name = "year_month")
    val yearMonth: String = "",
    @ColumnInfo(name = "year_week")
    val yearWeek: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
    @ColumnInfo(name = "time_period")
    val timePeriod: String = "",
    @ColumnInfo(name = "is_weekend")
    val isWeekend: Boolean = false,

    // Playback Metrics (4)
    @ColumnInfo(name = "playback_duration_ms")
    val playbackDurationMs: Long = 0,
    @ColumnInfo(name = "effective_listened_ms")
    val effectiveListenedMs: Long = 0,
    @ColumnInfo(name = "completion_percent")
    val completionPercent: Double = 0.0,
    @ColumnInfo(name = "end_reason")
    val endReason: String = "",

    // Behavior (5)
    @ColumnInfo(name = "pause_count")
    val pauseCount: Int = 0,
    @ColumnInfo(name = "pause_duration_ms")
    val pauseDurationMs: Long = 0,
    @ColumnInfo(name = "seek_count")
    val seekCount: Int = 0,
    @ColumnInfo(name = "seek_forward_count")
    val seekForwardCount: Int = 0,
    @ColumnInfo(name = "seek_backward_count")
    val seekBackwardCount: Int = 0,

    // Queue Context (8)
    @ColumnInfo(name = "shuffle_enabled")
    val shuffleEnabled: Boolean = false,
    @ColumnInfo(name = "repeat_mode")
    val repeatMode: String = "off",
    @ColumnInfo(name = "queue_position")
    val queuePosition: Int = -1,
    @ColumnInfo(name = "queue_source")
    val queueSource: String = "",
    @ColumnInfo(name = "playback_origin")
    val playbackOrigin: String = "unknown",
    @ColumnInfo(name = "playlist_id")
    val playlistId: String = "",
    @ColumnInfo(name = "playlist_name")
    val playlistName: String = "",
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    // Device Context (6)
    @ColumnInfo(name = "output_device")
    val outputDevice: String = "",
    @ColumnInfo(name = "volume_start")
    val volumeStart: Int = -1,
    @ColumnInfo(name = "volume_end")
    val volumeEnd: Int = -1,
    @ColumnInfo(name = "battery_level")
    val batteryLevel: Int = -1,
    @ColumnInfo(name = "charging")
    val charging: Boolean = false,
    @ColumnInfo(name = "screen_on")
    val screenOn: Boolean = true,

    // App (1)
    @ColumnInfo(name = "app_version")
    val appVersion: String = ""
)
