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
        ListeningSessionEntity::class
    ],
    version = 7,
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
    }
}