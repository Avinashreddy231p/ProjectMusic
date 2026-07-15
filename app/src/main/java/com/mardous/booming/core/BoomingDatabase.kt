package com.mardous.booming.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mardous.booming.data.local.room.*

@Database(
    entities = [
        PlaylistEntity::class,
        SongEntity::class,
        HistoryEntity::class,
        PlayCountEntity::class,
        QueueEntity::class,
        InclExclEntity::class,
        LyricsEntity::class,
        ListeningSessionEntity::class,
        PendingScrobbleEntity::class,
        ListeningSessionGroupEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class BoomingDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playCountDao(): PlayCountDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun inclExclDao(): InclExclDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun pendingScrobbleDao(): PendingScrobbleDao
    abstract fun listeningSessionGroupDao(): ListeningSessionGroupDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN custom_cover_uri TEXT")
                db.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN description TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `QueueEntity` (`id` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `CanvasEntity` (`id` INT NOT NULL, `canvas_url` TEXT NOT NULL, `fetch_time` INT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS CanvasEntity")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS LyricsEntity")
                db.execSQL("""
                    CREATE TABLE LyricsEntity (
                        id INTEGER PRIMARY KEY NOT NULL,
                        lyrics TEXT,
                        provider TEXT,
                        is_instrumental INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS listening_sessions (
                        session_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        song_id INTEGER NOT NULL,
                        song_title TEXT NOT NULL DEFAULT '',
                        artist_name TEXT NOT NULL DEFAULT '',
                        album_artist TEXT,
                        album_name TEXT NOT NULL DEFAULT '',
                        album_id INTEGER NOT NULL DEFAULT 0,
                        composer TEXT,
                        lyricist TEXT,
                        publisher TEXT,
                        genre TEXT,
                        year INTEGER NOT NULL DEFAULT 0,
                        year_month TEXT NOT NULL DEFAULT '',
                        start_time INTEGER NOT NULL DEFAULT 0,
                        end_time INTEGER NOT NULL DEFAULT 0,
                        song_duration_ms INTEGER NOT NULL DEFAULT 0,
                        duration_played_ms INTEGER NOT NULL DEFAULT 0,
                        completed INTEGER NOT NULL DEFAULT 0,
                        skipped INTEGER NOT NULL DEFAULT 0,
                        pause_count INTEGER NOT NULL DEFAULT 0,
                        seek_count INTEGER NOT NULL DEFAULT 0,
                        listened_seconds_ms INTEGER NOT NULL DEFAULT 0,
                        end_reason TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_listening_sessions_song_id ON listening_sessions(song_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_listening_sessions_year_month ON listening_sessions(year_month)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_listening_sessions_start_time ON listening_sessions(start_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_listening_sessions_song_year_month ON listening_sessions(song_id, year_month)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS PendingScrobbleEntity (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        artist TEXT NOT NULL,
                        track TEXT NOT NULL,
                        album TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS listening_session_groups (
                        group_id TEXT PRIMARY KEY NOT NULL,
                        total_songs INTEGER NOT NULL DEFAULT 0,
                        total_duration_ms INTEGER NOT NULL DEFAULT 0,
                        start_time INTEGER NOT NULL DEFAULT 0,
                        end_time INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_group_start_time ON listening_session_groups(start_time)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS listening_sessions_new (
                        session_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        session_group_id TEXT NOT NULL DEFAULT '',
                        song_id INTEGER NOT NULL DEFAULT 0,
                        artist_id INTEGER NOT NULL DEFAULT -1,
                        song_title TEXT NOT NULL DEFAULT '',
                        artist_name TEXT NOT NULL DEFAULT '',
                        album_artist TEXT,
                        album_id INTEGER NOT NULL DEFAULT 0,
                        album_name TEXT NOT NULL DEFAULT '',
                        genre TEXT,
                        release_year INTEGER NOT NULL DEFAULT 0,
                        composer TEXT,
                        lyricist TEXT,
                        publisher TEXT,
                        is_lyrics INTEGER NOT NULL DEFAULT 0,
                        song_duration_ms INTEGER NOT NULL DEFAULT 0,
                        audio_format TEXT NOT NULL DEFAULT '',
                        audio_sample_rate INTEGER NOT NULL DEFAULT 0,
                        audio_channel_count INTEGER NOT NULL DEFAULT 0,
                        bitrate_kbps INTEGER NOT NULL DEFAULT 0,
                        playback_speed REAL NOT NULL DEFAULT 1.0,
                        equalizer_active INTEGER NOT NULL DEFAULT 0,
                        start_time INTEGER NOT NULL DEFAULT 0,
                        end_time INTEGER NOT NULL DEFAULT 0,
                        time_standard TEXT NOT NULL DEFAULT 'UTC',
                        timezone_id TEXT NOT NULL DEFAULT '',
                        timezone_offset_minutes INTEGER NOT NULL DEFAULT 0,
                        start_date TEXT NOT NULL DEFAULT '',
                        start_time_only TEXT NOT NULL DEFAULT '',
                        day_of_week TEXT NOT NULL DEFAULT '',
                        day_of_month INTEGER NOT NULL DEFAULT 0,
                        day_of_year INTEGER NOT NULL DEFAULT 0,
                        week_of_year INTEGER NOT NULL DEFAULT 0,
                        month INTEGER NOT NULL DEFAULT 0,
                        month_name TEXT NOT NULL DEFAULT '',
                        quarter INTEGER NOT NULL DEFAULT 0,
                        year INTEGER NOT NULL DEFAULT 0,
                        year_month TEXT NOT NULL DEFAULT '',
                        year_week TEXT NOT NULL DEFAULT '',
                        hour INTEGER NOT NULL DEFAULT 0,
                        minute INTEGER NOT NULL DEFAULT 0,
                        second INTEGER NOT NULL DEFAULT 0,
                        time_period TEXT NOT NULL DEFAULT '',
                        is_weekend INTEGER NOT NULL DEFAULT 0,
                        playback_duration_ms INTEGER NOT NULL DEFAULT 0,
                        effective_listened_ms INTEGER NOT NULL DEFAULT 0,
                        completion_percent REAL NOT NULL DEFAULT 0.0,
                        end_reason TEXT NOT NULL DEFAULT '',
                        pause_count INTEGER NOT NULL DEFAULT 0,
                        pause_duration_ms INTEGER NOT NULL DEFAULT 0,
                        seek_count INTEGER NOT NULL DEFAULT 0,
                        seek_forward_count INTEGER NOT NULL DEFAULT 0,
                        seek_backward_count INTEGER NOT NULL DEFAULT 0,
                        shuffle_enabled INTEGER NOT NULL DEFAULT 0,
                        repeat_mode TEXT NOT NULL DEFAULT 'off',
                        queue_position INTEGER NOT NULL DEFAULT -1,
                        queue_source TEXT NOT NULL DEFAULT '',
                        playback_origin TEXT NOT NULL DEFAULT 'unknown',
                        playlist_id TEXT NOT NULL DEFAULT '',
                        playlist_name TEXT NOT NULL DEFAULT '',
                        is_favorite INTEGER NOT NULL DEFAULT 0,
                        output_device TEXT NOT NULL DEFAULT '',
                        volume_start INTEGER NOT NULL DEFAULT -1,
                        volume_end INTEGER NOT NULL DEFAULT -1,
                        battery_level INTEGER NOT NULL DEFAULT -1,
                        charging INTEGER NOT NULL DEFAULT 0,
                        screen_on INTEGER NOT NULL DEFAULT 1,
                        app_version TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO listening_sessions_new (
                        session_id, session_group_id, song_id, artist_id,
                        song_title, artist_name, album_artist, album_id, album_name,
                        genre, release_year, composer, lyricist, publisher, is_lyrics,
                        song_duration_ms, audio_format, audio_sample_rate, audio_channel_count,
                        bitrate_kbps, playback_speed, equalizer_active,
                        start_time, end_time, time_standard, timezone_id, timezone_offset_minutes,
                        start_date, start_time_only, day_of_week, day_of_month, day_of_year,
                        week_of_year, month, month_name, quarter, year,
                        year_month, year_week, hour, minute, second,
                        time_period, is_weekend,
                        playback_duration_ms, effective_listened_ms, completion_percent, end_reason,
                        pause_count, pause_duration_ms, seek_count, seek_forward_count, seek_backward_count,
                        shuffle_enabled, repeat_mode, queue_position, queue_source, playback_origin,
                        playlist_id, playlist_name, is_favorite,
                        output_device, volume_start, volume_end,
                        battery_level, charging, screen_on, app_version
                    )
                    SELECT
                        session_id,
                        '' AS session_group_id,
                        song_id,
                        -1 AS artist_id,
                        song_title,
                        artist_name,
                        album_artist,
                        album_id,
                        album_name,
                        genre,
                        year AS release_year,
                        composer,
                        lyricist,
                        publisher,
                        0 AS is_lyrics,
                        song_duration_ms,
                        '' AS audio_format,
                        0 AS audio_sample_rate,
                        0 AS audio_channel_count,
                        0 AS bitrate_kbps,
                        1.0 AS playback_speed,
                        0 AS equalizer_active,
                        start_time,
                        end_time,
                        'UTC' AS time_standard,
                        '' AS timezone_id,
                        0 AS timezone_offset_minutes,
                        date(start_time / 1000, 'unixepoch') AS start_date,
                        time(start_time / 1000, 'unixepoch') AS start_time_only,
                        CASE CAST(strftime('%w', start_time / 1000, 'unixepoch') AS INTEGER)
                            WHEN 0 THEN 'Sunday' WHEN 1 THEN 'Monday' WHEN 2 THEN 'Tuesday'
                            WHEN 3 THEN 'Wednesday' WHEN 4 THEN 'Thursday' WHEN 5 THEN 'Friday'
                            WHEN 6 THEN 'Saturday' ELSE 'Unknown' END AS day_of_week,
                        CAST(strftime('%d', start_time / 1000, 'unixepoch') AS INTEGER) AS day_of_month,
                        CAST(strftime('%j', start_time / 1000, 'unixepoch') AS INTEGER) AS day_of_year,
                        CAST(strftime('%W', start_time / 1000, 'unixepoch') AS INTEGER) AS week_of_year,
                        CAST(strftime('%m', start_time / 1000, 'unixepoch') AS INTEGER) AS month,
                        CASE CAST(strftime('%m', start_time / 1000, 'unixepoch') AS INTEGER)
                            WHEN 1 THEN 'January' WHEN 2 THEN 'February' WHEN 3 THEN 'March'
                            WHEN 4 THEN 'April' WHEN 5 THEN 'May' WHEN 6 THEN 'June'
                            WHEN 7 THEN 'July' WHEN 8 THEN 'August' WHEN 9 THEN 'September'
                            WHEN 10 THEN 'October' WHEN 11 THEN 'November' WHEN 12 THEN 'December'
                            ELSE 'Unknown' END AS month_name,
                        (CAST(strftime('%m', start_time / 1000, 'unixepoch') AS INTEGER) - 1) / 3 + 1 AS quarter,
                        CAST(strftime('%Y', start_time / 1000, 'unixepoch') AS INTEGER) AS year,
                        strftime('%Y-%m', start_time / 1000, 'unixepoch') AS year_month,
                        strftime('%Y-W', start_time / 1000, 'unixepoch') || printf('%02d', CAST(strftime('%W', start_time / 1000, 'unixepoch') AS INTEGER)) AS year_week,
                        CAST(strftime('%H', start_time / 1000, 'unixepoch') AS INTEGER) AS hour,
                        CAST(strftime('%M', start_time / 1000, 'unixepoch') AS INTEGER) AS minute,
                        CAST(strftime('%S', start_time / 1000, 'unixepoch') AS INTEGER) AS second,
                        CASE
                            WHEN CAST(strftime('%H', start_time / 1000, 'unixepoch') AS INTEGER) BETWEEN 0 AND 5 THEN 'Early Morning'
                            WHEN CAST(strftime('%H', start_time / 1000, 'unixepoch') AS INTEGER) BETWEEN 6 AND 11 THEN 'Morning'
                            WHEN CAST(strftime('%H', start_time / 1000, 'unixepoch') AS INTEGER) BETWEEN 12 AND 16 THEN 'Afternoon'
                            WHEN CAST(strftime('%H', start_time / 1000, 'unixepoch') AS INTEGER) BETWEEN 17 AND 20 THEN 'Evening'
                            ELSE 'Night' END AS time_period,
                        CASE WHEN CAST(strftime('%w', start_time / 1000, 'unixepoch') AS INTEGER) IN (0, 6) THEN 1 ELSE 0 END AS is_weekend,
                        duration_played_ms AS playback_duration_ms,
                        listened_seconds_ms AS effective_listened_ms,
                        CASE WHEN song_duration_ms > 0 THEN (CAST(duration_played_ms AS REAL) / CAST(song_duration_ms AS REAL) * 100.0) ELSE 0.0 END AS completion_percent,
                        end_reason,
                        pause_count,
                        0 AS pause_duration_ms,
                        seek_count,
                        0 AS seek_forward_count,
                        0 AS seek_backward_count,
                        0 AS shuffle_enabled,
                        'off' AS repeat_mode,
                        -1 AS queue_position,
                        '' AS queue_source,
                        'unknown' AS playback_origin,
                        '' AS playlist_id,
                        '' AS playlist_name,
                        0 AS is_favorite,
                        '' AS output_device,
                        -1 AS volume_start,
                        -1 AS volume_end,
                        -1 AS battery_level,
                        0 AS charging,
                        1 AS screen_on,
                        '' AS app_version
                    FROM listening_sessions
                """.trimIndent())

                db.execSQL("DROP TABLE IF EXISTS listening_sessions")
                db.execSQL("ALTER TABLE listening_sessions_new RENAME TO listening_sessions")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_song_id ON listening_sessions(song_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_session_group_id ON listening_sessions(session_group_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_artist_id ON listening_sessions(artist_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_album_id ON listening_sessions(album_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_start_time ON listening_sessions(start_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_start_date ON listening_sessions(start_date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_year_month ON listening_sessions(year_month)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_year_week ON listening_sessions(year_week)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_year ON listening_sessions(year)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_month ON listening_sessions(month)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_quarter ON listening_sessions(quarter)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_hour ON listening_sessions(hour)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_day_of_week ON listening_sessions(day_of_week)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_time_period ON listening_sessions(time_period)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_is_weekend ON listening_sessions(is_weekend)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_end_reason ON listening_sessions(end_reason)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_shuffle_enabled ON listening_sessions(shuffle_enabled)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_repeat_mode ON listening_sessions(repeat_mode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_is_favorite ON listening_sessions(is_favorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_output_device ON listening_sessions(output_device)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_queue_source ON listening_sessions(queue_source)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_playback_origin ON listening_sessions(playback_origin)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_audio_format ON listening_sessions(audio_format)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_song_id_year_month ON listening_sessions(song_id, year_month)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_artist_name_start_time ON listening_sessions(artist_name, start_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_album_id_start_time ON listening_sessions(album_id, start_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_genre_start_time ON listening_sessions(genre, start_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_start_date_hour ON listening_sessions(start_date, hour)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_playlist_id_start_time ON listening_sessions(playlist_id, start_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_sessions_playback_origin_start_time ON listening_sessions(playback_origin, start_time)")
            }
        }
    }
}
