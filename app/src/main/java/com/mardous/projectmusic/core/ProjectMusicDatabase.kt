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

package com.mardous.projectmusic.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mardous.projectmusic.data.local.database.core.*
import com.mardous.projectmusic.data.local.database.metadata.*
import com.mardous.projectmusic.data.local.database.analytics.*
import com.mardous.projectmusic.data.local.database.intel.*
import com.mardous.projectmusic.data.local.database.dao.*
import com.mardous.projectmusic.data.local.database.sync.*

@Database(
    entities = [
        // Core
        SongEntity::class,
        SongMetadataEntity::class,
        SongStatsEntity::class,
        SongRankingEntity::class,
        
        // Metadata
        PlaylistEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        AlbumArtistEntity::class,
        GenreEntity::class,
        MoodEntity::class,
        TagEntity::class,
        InstrumentEntity::class,
        SongPlaylistEntity::class,
        SongGenreEntity::class,
        SongMoodEntity::class,
        SongTagEntity::class,
        SongInstrumentEntity::class,
        InclExclEntity::class,
        
        // Analytics
        ListeningSessionEntity::class,
        ListeningSessionGroupEntity::class,
        HistoryEntity::class,
        PlayCountEntity::class,
        QueueEntity::class,
        PendingScrobbleEntity::class,
        DailySummaryEntity::class,
        LibraryStatsEntity::class,
        
        // Intel
        LyricsEntity::class,
        GenreSongRankingEntity::class,
        MoodSongRankingEntity::class,
        TagSongRankingEntity::class,
        InstrumentSongRankingEntity::class,
        SongSearchFTS::class,
        DatabaseHealthEntity::class,
        SongAIEntity::class,
        SongFingerprintEntity::class,
        
        // Sync
        PendingWorkEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class ProjectMusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playCountDao(): PlayCountDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun inclExclDao(): InclExclDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun pendingScrobbleDao(): PendingScrobbleDao
    abstract fun listeningSessionGroupDao(): ListeningSessionGroupDao
    abstract fun metadataDao(): MetadataDao
    abstract fun rankingDao(): RankingDao
    abstract fun workDao(): WorkDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun healthDao(): HealthDao

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

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SongEntity updates
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN overall_play_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN overall_duration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN overall_combined_score REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN overall_play_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN overall_duration_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN overall_combined_rank INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_play_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_duration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_combined_score REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_play_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_duration_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_combined_rank INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE SongEntity ADD COLUMN artist_play_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN artist_duration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN artist_combined_score REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN artist_play_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN artist_duration_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN artist_combined_rank INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_artist_play_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_artist_duration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_artist_combined_score REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_artist_play_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_artist_duration_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SongEntity ADD COLUMN album_artist_combined_rank INTEGER NOT NULL DEFAULT 0")

                // LyricsEntity updates
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN checksum TEXT")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN last_updated INTEGER")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN provider_id TEXT")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN embedded_lyrics TEXT")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN synchronized_lyrics TEXT")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN unsynchronized_lyrics TEXT")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN language TEXT")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN source INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN word_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN has_embedded_lyrics INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE LyricsEntity ADD COLUMN has_synchronized_lyrics INTEGER NOT NULL DEFAULT 0")

                // Master tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `genres` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, `color` INTEGER, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_genres_name` ON `genres` (`name`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `moods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, `color` INTEGER, `emoji` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_moods_name` ON `moods` (`name`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, `color` INTEGER, `category` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `instruments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, `color` INTEGER, `family` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_instruments_name` ON `instruments` (`name`)")

                // Join tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `song_genre_relationship` (`song_id` INTEGER NOT NULL, `genre_id` INTEGER NOT NULL, `confidence_score` REAL NOT NULL, `source` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `genre_id`), FOREIGN KEY(`genre_id`) REFERENCES `genres`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_genre_relationship_song_id` ON `song_genre_relationship` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_genre_relationship_genre_id` ON `song_genre_relationship` (`genre_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `song_mood_relationship` (`song_id` INTEGER NOT NULL, `mood_id` INTEGER NOT NULL, `confidence_score` REAL NOT NULL, `source` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `mood_id`), FOREIGN KEY(`mood_id`) REFERENCES `moods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_mood_relationship_song_id` ON `song_mood_relationship` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_mood_relationship_mood_id` ON `song_mood_relationship` (`mood_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `song_tag_relationship` (`song_id` INTEGER NOT NULL, `tag_id` INTEGER NOT NULL, `confidence_score` REAL NOT NULL, `source` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `tag_id`), FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_tag_relationship_song_id` ON `song_tag_relationship` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_tag_relationship_tag_id` ON `song_tag_relationship` (`tag_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `song_instrument_relationship` (`song_id` INTEGER NOT NULL, `instrument_id` INTEGER NOT NULL, `confidence_score` REAL NOT NULL, `source` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `instrument_id`), FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_instrument_relationship_song_id` ON `song_instrument_relationship` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_instrument_relationship_instrument_id` ON `song_instrument_relationship` (`instrument_id`)")

                // Ranking tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `genre_song_ranking` (`song_id` INTEGER NOT NULL, `genre_id` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listened_duration_ms` INTEGER NOT NULL, `combined_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `combined_rank` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `genre_id`), FOREIGN KEY(`genre_id`) REFERENCES `genres`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_genre_song_ranking_song_id` ON `genre_song_ranking` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_genre_song_ranking_genre_id` ON `genre_song_ranking` (`genre_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_genre_song_ranking_combined_rank` ON `genre_song_ranking` (`combined_rank`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `mood_song_ranking` (`song_id` INTEGER NOT NULL, `mood_id` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listened_duration_ms` INTEGER NOT NULL, `combined_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `combined_rank` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `mood_id`), FOREIGN KEY(`mood_id`) REFERENCES `moods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mood_song_ranking_song_id` ON `mood_song_ranking` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mood_song_ranking_mood_id` ON `mood_song_ranking` (`mood_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mood_song_ranking_combined_rank` ON `mood_song_ranking` (`combined_rank`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `tag_song_ranking` (`song_id` INTEGER NOT NULL, `tag_id` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listened_duration_ms` INTEGER NOT NULL, `combined_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `combined_rank` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `tag_id`), FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tag_song_ranking_song_id` ON `tag_song_ranking` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tag_song_ranking_tag_id` ON `tag_song_ranking` (`tag_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tag_song_ranking_combined_rank` ON `tag_song_ranking` (`combined_rank`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `instrument_song_ranking` (`song_id` INTEGER NOT NULL, `instrument_id` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listened_duration_ms` INTEGER NOT NULL, `combined_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `combined_rank` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `instrument_id`), FOREIGN KEY(`instrument_id`) REFERENCES `instruments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_instrument_song_ranking_song_id` ON `instrument_song_ranking` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_instrument_song_ranking_instrument_id` ON `instrument_song_ranking` (`instrument_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_instrument_song_ranking_combined_rank` ON `instrument_song_ranking` (`combined_rank`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Skip logic, same as original
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new 'songs' table
                db.execSQL("CREATE TABLE IF NOT EXISTS `songs` (`song_key` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `media_store_id` INTEGER NOT NULL, `data` TEXT NOT NULL, `title` TEXT NOT NULL, `duration` INTEGER NOT NULL, `album_id` INTEGER NOT NULL, `album_name` TEXT NOT NULL, `artist_id` INTEGER NOT NULL, `artist_name` TEXT NOT NULL, `album_artist` TEXT, `genre_name` TEXT, `volume_name` TEXT, `bitrate` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_media_store_id` ON `songs` (`media_store_id`)")

                // 2. Populate 'songs' by deduplicating old 'SongEntity'
                db.execSQL("""
                    INSERT INTO `songs` (media_store_id, data, title, duration, album_id, album_name, artist_id, artist_name, album_artist, genre_name, bitrate)
                    SELECT id, data, title, duration, album_id, album_name, artist_id, artist_name, album_artist, genre_name, 0
                    FROM SongEntity
                    GROUP BY id
                """.trimIndent())

                // 3. Rename old metadata tables for modularity
                db.execSQL("ALTER TABLE PlaylistEntity RENAME TO playlists")
                db.execSQL("ALTER TABLE HistoryEntity RENAME TO history")
                db.execSQL("ALTER TABLE PlayCountEntity RENAME TO play_counts")
                db.execSQL("ALTER TABLE QueueEntity RENAME TO queue")
                db.execSQL("ALTER TABLE PendingScrobbleEntity RENAME TO pending_scrobbles")
                db.execSQL("ALTER TABLE LyricsEntity RENAME TO lyrics")

                // 4. Create 'song_playlist_relationship'
                db.execSQL("CREATE TABLE IF NOT EXISTS `song_playlist_relationship` (`song_id` INTEGER NOT NULL, `playlist_id` INTEGER NOT NULL, `date_added` INTEGER NOT NULL, `order_in_playlist` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `playlist_id`), FOREIGN KEY(`song_id`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`playlist_id`) REFERENCES `playlists`(`playlist_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_playlist_relationship_song_id` ON `song_playlist_relationship` (`song_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_playlist_relationship_playlist_id` ON `song_playlist_relationship` (`playlist_id`)")

                // 5. Populate 'song_playlist_relationship'
                db.execSQL("""
                    INSERT INTO `song_playlist_relationship` (song_id, playlist_id, date_added, order_in_playlist)
                    SELECT s.song_key, old.playlist_creator_id, old.date_added, 0
                    FROM SongEntity old
                    JOIN songs s ON old.id = s.media_store_id
                """.trimIndent())

                // 6. Create 'song_metadata'
                db.execSQL("CREATE TABLE IF NOT EXISTS `song_metadata` (`song_key` INTEGER PRIMARY KEY NOT NULL, `composer` TEXT, `lyricist` TEXT, `conductor` TEXT, `publisher` TEXT, `copyright` TEXT, `genre` TEXT, `year` INTEGER NOT NULL, `track_number` INTEGER NOT NULL, `disc_number` INTEGER NOT NULL, `file_size` INTEGER NOT NULL, `date_added` INTEGER NOT NULL, `date_modified` INTEGER NOT NULL, `audio_format` TEXT, `sample_rate` INTEGER NOT NULL, `channel_count` INTEGER NOT NULL, `bit_depth` INTEGER NOT NULL, `mime_type` TEXT, `replay_gain` REAL, `bpm` INTEGER NOT NULL, `key_signature` TEXT, `isrc` TEXT, `musicbrainz_track_id` TEXT, `musicbrainz_album_id` TEXT, `musicbrainz_artist_id` TEXT, `is_explicit` INTEGER NOT NULL, `is_compilation` INTEGER NOT NULL, `is_favorite` INTEGER NOT NULL, `rating` INTEGER NOT NULL, `artwork_uri` TEXT, `checksum` TEXT, `language` TEXT, `last_scanned_timestamp` INTEGER NOT NULL, FOREIGN KEY(`song_key`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE )")

                // 7. Populate 'song_metadata'
                db.execSQL("""
                    INSERT INTO `song_metadata` (song_key, year, track_number, file_size, date_added, date_modified, composer, lyricist, rating, is_favorite, is_explicit, is_compilation, sample_rate, channel_count, bit_depth, bpm, last_scanned_timestamp)
                    SELECT s.song_key, old.year, old.track_number, old.size, old.date_added, old.date_modified, NULL, NULL, 0, 0, 0, 0, 0, 0, 0, 0, 0
                    FROM (SELECT id, year, track_number, size, date_added, date_modified FROM SongEntity GROUP BY id) old
                    JOIN songs s ON old.id = s.media_store_id
                """.trimIndent())

                // 8. Create stats and ranking tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `song_stats` (`song_key` INTEGER PRIMARY KEY NOT NULL, `total_play_count` INTEGER NOT NULL, `total_skip_count` INTEGER NOT NULL, `total_listening_duration` INTEGER NOT NULL, `effective_listening_duration` INTEGER NOT NULL, `avg_completion_percentage` REAL NOT NULL, `last_played_timestamp` INTEGER NOT NULL, `first_played_timestamp` INTEGER NOT NULL, `total_pause_count` INTEGER NOT NULL, `total_seek_count` INTEGER NOT NULL, `longest_listening_streak` INTEGER NOT NULL, `monthly_play_count` INTEGER NOT NULL, `weekly_play_count` INTEGER NOT NULL, `daily_play_count` INTEGER NOT NULL, `last_synced` INTEGER NOT NULL, FOREIGN KEY(`song_key`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `song_rankings` (`song_key` INTEGER NOT NULL, `ranking_type` TEXT NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `overall_rank` INTEGER NOT NULL, `overall_score` REAL NOT NULL, `last_updated` INTEGER NOT NULL, `ranking_version` INTEGER NOT NULL, PRIMARY KEY(`song_key`, `ranking_type`), FOREIGN KEY(`song_key`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE )")

                // 9. Dimension tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `artists_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `is_favorite` INTEGER NOT NULL, `cover_uri` TEXT, `song_count` INTEGER NOT NULL, `total_duration` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listening_duration` INTEGER NOT NULL, `overall_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `overall_rank` INTEGER NOT NULL, `last_updated` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_artists_name` ON `artists_new` (`name`)")
                db.execSQL("DROP TABLE IF EXISTS artists")
                db.execSQL("ALTER TABLE artists_new RENAME TO artists")

                db.execSQL("CREATE TABLE IF NOT EXISTS `albums_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `album_artist` TEXT, `release_year` INTEGER NOT NULL, `is_favorite` INTEGER NOT NULL, `cover_uri` TEXT, `song_count` INTEGER NOT NULL, `total_duration` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listening_duration` INTEGER NOT NULL, `overall_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `overall_rank` INTEGER NOT NULL, `last_updated` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_albums_name_album_artist` ON `albums_new` (`name`, `album_artist`)")
                db.execSQL("DROP TABLE IF EXISTS albums")
                db.execSQL("ALTER TABLE albums_new RENAME TO albums")

                db.execSQL("CREATE TABLE IF NOT EXISTS `album_artists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `is_favorite` INTEGER NOT NULL, `cover_uri` TEXT, `album_count` INTEGER NOT NULL, `song_count` INTEGER NOT NULL, `total_duration` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listening_duration` INTEGER NOT NULL, `overall_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `overall_rank` INTEGER NOT NULL, `last_updated` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_album_artists_name` ON `album_artists` (`name`)")

                // 10. Metadata Categories
                db.execSQL("CREATE TABLE IF NOT EXISTS `genres_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, `color` INTEGER, `is_favorite` INTEGER NOT NULL, `song_count` INTEGER NOT NULL, `total_duration` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listening_duration` INTEGER NOT NULL, `overall_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `overall_rank` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_genres_name` ON `genres_new` (`name`)")
                db.execSQL("INSERT INTO `genres_new` (id, name, description, icon, color, is_favorite, song_count, total_duration, play_count, listening_duration, overall_score, play_rank, duration_rank, overall_rank, created_at, updated_at) SELECT id, name, description, icon, color, 0, 0, 0, 0, 0, 0.0, 0, 0, 0, created_at, updated_at FROM genres")
                db.execSQL("DROP TABLE IF EXISTS genres")
                db.execSQL("ALTER TABLE genres_new RENAME TO genres")

                db.execSQL("CREATE TABLE IF NOT EXISTS `moods_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, `color` INTEGER, `emoji` TEXT, `is_favorite` INTEGER NOT NULL, `song_count` INTEGER NOT NULL, `total_duration` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listening_duration` INTEGER NOT NULL, `overall_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `overall_rank` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_moods_name` ON `moods_new` (`name`)")
                db.execSQL("INSERT INTO `moods_new` (id, name, description, icon, color, emoji, is_favorite, song_count, total_duration, play_count, listening_duration, overall_score, play_rank, duration_rank, overall_rank, created_at, updated_at) SELECT id, name, description, icon, color, emoji, 0, 0, 0, 0, 0, 0.0, 0, 0, 0, created_at, updated_at FROM moods")
                db.execSQL("DROP TABLE IF EXISTS moods")
                db.execSQL("ALTER TABLE moods_new RENAME TO moods")

                db.execSQL("CREATE TABLE IF NOT EXISTS `tags_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, `color` INTEGER, `category` INTEGER NOT NULL, `is_favorite` INTEGER NOT NULL, `song_count` INTEGER NOT NULL, `total_duration` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listening_duration` INTEGER NOT NULL, `overall_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `overall_rank` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags_new` (`name`)")
                db.execSQL("INSERT INTO `tags_new` (id, name, description, icon, color, category, is_favorite, song_count, total_duration, play_count, listening_duration, overall_score, play_rank, duration_rank, overall_rank, created_at, updated_at) SELECT id, name, description, icon, color, category, 0, 0, 0, 0, 0, 0.0, 0, 0, 0, created_at, updated_at FROM tags")
                db.execSQL("DROP TABLE IF EXISTS tags")
                db.execSQL("ALTER TABLE tags_new RENAME TO tags")

                db.execSQL("CREATE TABLE IF NOT EXISTS `instruments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, `color` INTEGER, `family` INTEGER NOT NULL, `is_favorite` INTEGER NOT NULL, `song_count` INTEGER NOT NULL, `total_duration` INTEGER NOT NULL, `play_count` INTEGER NOT NULL, `listening_duration` INTEGER NOT NULL, `overall_score` REAL NOT NULL, `play_rank` INTEGER NOT NULL, `duration_rank` INTEGER NOT NULL, `overall_rank` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_instruments_name` ON `instruments_new` (`name`)")
                db.execSQL("INSERT INTO `instruments_new` (id, name, description, icon, color, family, is_favorite, song_count, total_duration, play_count, listening_duration, overall_score, play_rank, duration_rank, overall_rank, created_at, updated_at) SELECT id, name, description, icon, color, family, 0, 0, 0, 0, 0, 0.0, 0, 0, 0, created_at, updated_at FROM instruments")
                db.execSQL("DROP TABLE IF EXISTS instruments")
                db.execSQL("ALTER TABLE instruments_new RENAME TO instruments")

                // 11. Search and Monitoring
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `song_search_fts` USING fts4(`title`, `artist`, `album`, `lyrics`, `composer`, `lyricist`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `database_health` (`id` INTEGER PRIMARY KEY NOT NULL, `last_sync` INTEGER NOT NULL, `last_ranking` INTEGER NOT NULL, `last_vacuum` INTEGER NOT NULL, `last_integrity_check` INTEGER NOT NULL, `schema_version` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `library_stats` (`id` INTEGER PRIMARY KEY NOT NULL, `song_count` INTEGER NOT NULL, `album_count` INTEGER NOT NULL, `artist_count` INTEGER NOT NULL, `favorite_count` INTEGER NOT NULL, `total_duration` INTEGER NOT NULL, `last_updated` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `daily_summaries` (`date` TEXT PRIMARY KEY NOT NULL, `total_play_count` INTEGER NOT NULL, `total_duration_ms` INTEGER NOT NULL, `top_song_id` INTEGER NOT NULL, `top_artist_id` INTEGER NOT NULL)")

                // 12. Sync
                db.execSQL("CREATE TABLE IF NOT EXISTS `pending_work` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entity_type` TEXT NOT NULL, `entity_id` INTEGER NOT NULL, `reason` TEXT NOT NULL, `priority` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `retry_count` INTEGER NOT NULL, `status` TEXT NOT NULL)")

                // 13. Update InclExcl
                db.execSQL("ALTER TABLE InclExclEntity RENAME TO incl_excl")

                // 14. Drop old SongEntity
                db.execSQL("DROP TABLE SongEntity")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN song_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN total_duration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN play_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN listening_duration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN overall_score REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN play_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN duration_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN overall_rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN last_updated INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // song_metadata additions
                db.execSQL("ALTER TABLE song_metadata ADD COLUMN musicbrainz_work_id TEXT")
                db.execSQL("ALTER TABLE song_metadata ADD COLUMN iswc TEXT")
                db.execSQL("ALTER TABLE song_metadata ADD COLUMN label TEXT")
                db.execSQL("ALTER TABLE song_metadata ADD COLUMN catalog_number TEXT")
                db.execSQL("ALTER TABLE song_metadata ADD COLUMN arranger TEXT")
                db.execSQL("ALTER TABLE song_metadata ADD COLUMN producer TEXT")
                db.execSQL("ALTER TABLE song_metadata ADD COLUMN engineer TEXT")
                // albums additions
                db.execSQL("ALTER TABLE albums ADD COLUMN musicbrainz_release_id TEXT")
                db.execSQL("ALTER TABLE albums ADD COLUMN release_country TEXT")
                db.execSQL("ALTER TABLE albums ADD COLUMN label TEXT")
                db.execSQL("ALTER TABLE albums ADD COLUMN catalog_number TEXT")
                // artists additions
                db.execSQL("ALTER TABLE artists ADD COLUMN musicbrainz_id TEXT")
                db.execSQL("ALTER TABLE artists ADD COLUMN type TEXT")
                db.execSQL("ALTER TABLE artists ADD COLUMN gender TEXT")
                db.execSQL("ALTER TABLE artists ADD COLUMN country TEXT")
                db.execSQL("ALTER TABLE artists ADD COLUMN disambiguation TEXT")
                db.execSQL("ALTER TABLE artists ADD COLUMN begin_date TEXT")
                db.execSQL("ALTER TABLE artists ADD COLUMN end_date TEXT")
                // album_artists additions
                db.execSQL("ALTER TABLE album_artists ADD COLUMN musicbrainz_id TEXT")
                db.execSQL("ALTER TABLE album_artists ADD COLUMN type TEXT")
                db.execSQL("ALTER TABLE album_artists ADD COLUMN gender TEXT")
                db.execSQL("ALTER TABLE album_artists ADD COLUMN country TEXT")
                db.execSQL("ALTER TABLE album_artists ADD COLUMN disambiguation TEXT")
                db.execSQL("ALTER TABLE album_artists ADD COLUMN begin_date TEXT")
                db.execSQL("ALTER TABLE album_artists ADD COLUMN end_date TEXT")
            }
        }
    }
}
