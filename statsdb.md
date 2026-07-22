# Project Music — Complete SQL Database Schema

This document provides the formal SQL Data Definition Language (DDL) for the entire Project Music database.

---

## 1. Core Music Data

### `songs`
Master registry of all audio files.
```sql
CREATE TABLE IF NOT EXISTS `songs` (
    `song_key` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `media_store_id` INTEGER NOT NULL,
    `data` TEXT NOT NULL,
    `title` TEXT NOT NULL,
    `duration` INTEGER NOT NULL,
    `album_id` INTEGER NOT NULL,
    `album_name` TEXT NOT NULL,
    `artist_id` INTEGER NOT NULL,
    `artist_name` TEXT NOT NULL,
    `album_artist` TEXT,
    `genre_name` TEXT,
    `volume_name` TEXT,
    `bitrate` INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_media_store_id` ON `songs` (`media_store_id`);
```

### `song_metadata`
Extended technical and descriptive metadata.
```sql
CREATE TABLE IF NOT EXISTS `song_metadata` (
    `song_key` INTEGER PRIMARY KEY NOT NULL,
    `composer` TEXT,
    `lyricist` TEXT,
    `conductor` TEXT,
    `publisher` TEXT,
    `copyright` TEXT,
    `genre` TEXT,
    `year` INTEGER NOT NULL DEFAULT 0,
    `track_number` INTEGER NOT NULL DEFAULT 0,
    `disc_number` INTEGER NOT NULL DEFAULT 0,
    `file_size` INTEGER NOT NULL DEFAULT 0,
    `date_added` INTEGER NOT NULL DEFAULT 0,
    `date_modified` INTEGER NOT NULL DEFAULT 0,
    `audio_format` TEXT,
    `sample_rate` INTEGER NOT NULL DEFAULT 0,
    `channel_count` INTEGER NOT NULL DEFAULT 0,
    `bit_depth` INTEGER NOT NULL DEFAULT 0,
    `mime_type` TEXT,
    `replay_gain` REAL,
    `bpm` INTEGER NOT NULL DEFAULT 0,
    `key_signature` TEXT,
    `isrc` TEXT,
    `musicbrainz_track_id` TEXT,
    `musicbrainz_album_id` TEXT,
    `musicbrainz_artist_id` TEXT,
    `musicbrainz_work_id` TEXT,
    `iswc` TEXT,
    `label` TEXT,
    `catalog_number` TEXT,
    `arranger` TEXT,
    `producer` TEXT,
    `engineer` TEXT,
    `is_explicit` INTEGER NOT NULL DEFAULT 0,
    `is_compilation` INTEGER NOT NULL DEFAULT 0,
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `rating` INTEGER NOT NULL DEFAULT 0,
    `artwork_uri` TEXT,
    `checksum` TEXT,
    `language` TEXT,
    `last_scanned_timestamp` INTEGER NOT NULL DEFAULT 0,
    `track_total` INTEGER NOT NULL DEFAULT 0,
    `disc_total` INTEGER NOT NULL DEFAULT 0,
    `comment` TEXT,
    FOREIGN KEY(`song_key`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE
);
```

### `song_stats`
Cumulative performance metrics for individual tracks.
```sql
CREATE TABLE IF NOT EXISTS `song_stats` (
    `song_key` INTEGER PRIMARY KEY NOT NULL,
    `total_play_count` INTEGER NOT NULL DEFAULT 0,
    `completed_play_count` INTEGER NOT NULL DEFAULT 0,
    `total_skip_count` INTEGER NOT NULL DEFAULT 0,
    `total_listening_duration` INTEGER NOT NULL DEFAULT 0,
    `effective_listening_duration` INTEGER NOT NULL DEFAULT 0,
    `avg_completion_percentage` REAL NOT NULL DEFAULT 0.0,
    `last_played_timestamp` INTEGER NOT NULL DEFAULT 0,
    `first_played_timestamp` INTEGER NOT NULL DEFAULT 0,
    `total_pause_count` INTEGER NOT NULL DEFAULT 0,
    `total_seek_count` INTEGER NOT NULL DEFAULT 0,
    `longest_listening_streak` INTEGER NOT NULL DEFAULT 0,
    `monthly_play_count` INTEGER NOT NULL DEFAULT 0,
    `weekly_play_count` INTEGER NOT NULL DEFAULT 0,
    `daily_play_count` INTEGER NOT NULL DEFAULT 0,
    `last_synced` INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(`song_key`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE
);
```

### `song_rankings`
Global leaderboard position by type (e.g., OVERALL, YEARLY).
```sql
CREATE TABLE IF NOT EXISTS `song_rankings` (
    `song_key` INTEGER NOT NULL,
    `ranking_type` TEXT NOT NULL,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `last_updated` INTEGER NOT NULL DEFAULT 0,
    `ranking_version` INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY(`song_key`, `ranking_type`),
    FOREIGN KEY(`song_key`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE
);
```

---

## 2. Metadata Taxonomy

### `artists`
```sql
CREATE TABLE IF NOT EXISTS `artists` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `musicbrainz_id` TEXT,
    `type` TEXT,
    `gender` TEXT,
    `country` TEXT,
    `disambiguation` TEXT,
    `begin_date` TEXT,
    `end_date` TEXT,
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `cover_uri` TEXT,
    `song_count` INTEGER NOT NULL DEFAULT 0,
    `total_duration` INTEGER NOT NULL DEFAULT 0,
    `play_count` INTEGER NOT NULL DEFAULT 0,
    `listening_duration` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `last_updated` INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_artists_name` ON `artists` (`name`);
```

### `albums`
```sql
CREATE TABLE IF NOT EXISTS `albums` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `album_artist` TEXT,
    `release_year` INTEGER NOT NULL DEFAULT 0,
    `musicbrainz_release_id` TEXT,
    `release_country` TEXT,
    `label` TEXT,
    `catalog_number` TEXT,
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `cover_uri` TEXT,
    `song_count` INTEGER NOT NULL DEFAULT 0,
    `total_duration` INTEGER NOT NULL DEFAULT 0,
    `play_count` INTEGER NOT NULL DEFAULT 0,
    `listening_duration` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `last_updated` INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_albums_name_album_artist` ON `albums` (`name`, `album_artist`);
```

### `album_artists`
```sql
CREATE TABLE IF NOT EXISTS `album_artists` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `musicbrainz_id` TEXT,
    `type` TEXT,
    `gender` TEXT,
    `country` TEXT,
    `disambiguation` TEXT,
    `begin_date` TEXT,
    `end_date` TEXT,
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `cover_uri` TEXT,
    `album_count` INTEGER NOT NULL DEFAULT 0,
    `song_count` INTEGER NOT NULL DEFAULT 0,
    `total_duration` INTEGER NOT NULL DEFAULT 0,
    `play_count` INTEGER NOT NULL DEFAULT 0,
    `listening_duration` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `last_updated` INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_album_artists_name` ON `album_artists` (`name`);
```

### `genres`
```sql
CREATE TABLE IF NOT EXISTS `genres` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `description` TEXT,
    `icon` TEXT,
    `color` INTEGER,
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `song_count` INTEGER NOT NULL DEFAULT 0,
    `total_duration` INTEGER NOT NULL DEFAULT 0,
    `play_count` INTEGER NOT NULL DEFAULT 0,
    `listening_duration` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `created_at` INTEGER NOT NULL DEFAULT 0,
    `updated_at` INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_genres_name` ON `genres` (`name`);
```

### `moods`
```sql
CREATE TABLE IF NOT EXISTS `moods` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `description` TEXT,
    `icon` TEXT,
    `color` INTEGER,
    `emoji` TEXT,
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `song_count` INTEGER NOT NULL DEFAULT 0,
    `total_duration` INTEGER NOT NULL DEFAULT 0,
    `play_count` INTEGER NOT NULL DEFAULT 0,
    `listening_duration` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `created_at` INTEGER NOT NULL DEFAULT 0,
    `updated_at` INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_moods_name` ON `moods` (`name`);
```

### `tags`
```sql
CREATE TABLE IF NOT EXISTS `tags` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `description` TEXT,
    `icon` TEXT,
    `color` INTEGER,
    `category` INTEGER NOT NULL,
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `song_count` INTEGER NOT NULL DEFAULT 0,
    `total_duration` INTEGER NOT NULL DEFAULT 0,
    `play_count` INTEGER NOT NULL DEFAULT 0,
    `listening_duration` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `created_at` INTEGER NOT NULL DEFAULT 0,
    `updated_at` INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`);
```

### `instruments`
```sql
CREATE TABLE IF NOT EXISTS `instruments` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `description` TEXT,
    `icon` TEXT,
    `color` INTEGER,
    `family` INTEGER NOT NULL,
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `song_count` INTEGER NOT NULL DEFAULT 0,
    `total_duration` INTEGER NOT NULL DEFAULT 0,
    `play_count` INTEGER NOT NULL DEFAULT 0,
    `listening_duration` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `created_at` INTEGER NOT NULL DEFAULT 0,
    `updated_at` INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_instruments_name` ON `instruments` (`name`);
```

---

## 3. Playlists & Queues

### `playlists`
```sql
CREATE TABLE IF NOT EXISTS `playlists` (
    `playlist_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `playlist_name` TEXT NOT NULL,
    `custom_cover_uri` TEXT,
    `description` TEXT,
    `song_count` INTEGER NOT NULL DEFAULT 0,
    `total_duration` INTEGER NOT NULL DEFAULT 0,
    `play_count` INTEGER NOT NULL DEFAULT 0,
    `listening_duration` INTEGER NOT NULL DEFAULT 0,
    `overall_score` REAL NOT NULL DEFAULT 0.0,
    `play_rank` INTEGER NOT NULL DEFAULT 0,
    `duration_rank` INTEGER NOT NULL DEFAULT 0,
    `overall_rank` INTEGER NOT NULL DEFAULT 0,
    `last_updated` INTEGER NOT NULL DEFAULT 0
);
```

### `song_playlist_relationship`
```sql
CREATE TABLE IF NOT EXISTS `song_playlist_relationship` (
    `song_id` INTEGER NOT NULL,
    `playlist_id` INTEGER NOT NULL,
    `date_added` INTEGER NOT NULL DEFAULT 0,
    `order_in_playlist` INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(`song_id`, `playlist_id`),
    FOREIGN KEY(`song_id`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE ,
    FOREIGN KEY(`playlist_id`) REFERENCES `playlists`(`playlist_id`) ON UPDATE NO ACTION ON DELETE CASCADE 
);
CREATE INDEX IF NOT EXISTS `index_song_playlist_relationship_song_id` ON `song_playlist_relationship` (`song_id`);
CREATE INDEX IF NOT EXISTS `index_song_playlist_relationship_playlist_id` ON `song_playlist_relationship` (`playlist_id`);
```

### `queue`
```sql
CREATE TABLE IF NOT EXISTS `queue` (
    `id` TEXT NOT NULL,
    `order` INTEGER NOT NULL,
    PRIMARY KEY(`id`)
);
```

### `incl_excl`
```sql
CREATE TABLE IF NOT EXISTS `incl_excl` (
    `path` TEXT PRIMARY KEY NOT NULL,
    `type` INTEGER NOT NULL
);
```

---

## 4. Analytics & Logging

### `listening_sessions`
```sql
CREATE TABLE IF NOT EXISTS `listening_sessions` (
    `session_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `session_group_id` TEXT NOT NULL DEFAULT '',
    `song_id` INTEGER NOT NULL DEFAULT 0,
    `artist_id` INTEGER NOT NULL DEFAULT -1,
    `song_title` TEXT NOT NULL DEFAULT '',
    `artist_name` TEXT NOT NULL DEFAULT '',
    `album_artist` TEXT,
    `album_id` INTEGER NOT NULL DEFAULT 0,
    `album_name` TEXT NOT NULL DEFAULT '',
    `genre` TEXT,
    `release_year` INTEGER NOT NULL DEFAULT 0,
    `composer` TEXT,
    `lyricist` TEXT,
    `publisher` TEXT,
    `is_lyrics` INTEGER NOT NULL DEFAULT 0,
    `song_duration_ms` INTEGER NOT NULL DEFAULT 0,
    `audio_format` TEXT NOT NULL DEFAULT '',
    `audio_sample_rate` INTEGER NOT NULL DEFAULT 0,
    `audio_channel_count` INTEGER NOT NULL DEFAULT 0,
    `bitrate_kbps` INTEGER NOT NULL DEFAULT 0,
    `playback_speed` REAL NOT NULL DEFAULT 1.0,
    `equalizer_active` INTEGER NOT NULL DEFAULT 0,
    `start_time` INTEGER NOT NULL DEFAULT 0,
    `end_time` INTEGER NOT NULL DEFAULT 0,
    `time_standard` TEXT NOT NULL DEFAULT 'UTC',
    `timezone_id` TEXT NOT NULL DEFAULT '',
    `timezone_offset_minutes` INTEGER NOT NULL DEFAULT 0,
    `start_date` TEXT NOT NULL DEFAULT '',
    `start_time_only` TEXT NOT NULL DEFAULT '',
    `day_of_week` TEXT NOT NULL DEFAULT '',
    `day_of_month` INTEGER NOT NULL DEFAULT 0,
    `day_of_year` INTEGER NOT NULL DEFAULT 0,
    `week_of_year` INTEGER NOT NULL DEFAULT 0,
    `month` INTEGER NOT NULL DEFAULT 0,
    `month_name` TEXT NOT NULL DEFAULT '',
    `quarter` INTEGER NOT NULL DEFAULT 0,
    `year` INTEGER NOT NULL DEFAULT 0,
    `year_month` TEXT NOT NULL DEFAULT '',
    `year_week` TEXT NOT NULL DEFAULT '',
    `hour` INTEGER NOT NULL DEFAULT 0,
    `minute` INTEGER NOT NULL DEFAULT 0,
    `second` INTEGER NOT NULL DEFAULT 0,
    `time_period` TEXT NOT NULL DEFAULT '',
    `is_weekend` INTEGER NOT NULL DEFAULT 0,
    `playback_duration_ms` INTEGER NOT NULL DEFAULT 0,
    `effective_listened_ms` INTEGER NOT NULL DEFAULT 0,
    `completion_percent` REAL NOT NULL DEFAULT 0.0,
    `end_reason` TEXT NOT NULL DEFAULT '',
    `pause_count` INTEGER NOT NULL DEFAULT 0,
    `pause_duration_ms` INTEGER NOT NULL DEFAULT 0,
    `seek_count` INTEGER NOT NULL DEFAULT 0,
    `seek_forward_count` INTEGER NOT NULL DEFAULT 0,
    `seek_backward_count` INTEGER NOT NULL DEFAULT 0,
    `shuffle_enabled` INTEGER NOT NULL DEFAULT 0,
    `repeat_mode` TEXT NOT NULL DEFAULT 'off',
    `queue_position` INTEGER NOT NULL DEFAULT -1,
    `queue_source` TEXT NOT NULL DEFAULT '',
    `playback_origin` TEXT NOT NULL DEFAULT 'unknown',
    `playlist_id` TEXT NOT NULL DEFAULT '',
    `playlist_name` TEXT NOT NULL DEFAULT '',
    `is_favorite` INTEGER NOT NULL DEFAULT 0,
    `output_device` TEXT NOT NULL DEFAULT '',
    `volume_start` INTEGER NOT NULL DEFAULT -1,
    `volume_end` INTEGER NOT NULL DEFAULT -1,
    `battery_level` INTEGER NOT NULL DEFAULT -1,
    `charging` INTEGER NOT NULL DEFAULT 0,
    `screen_on` INTEGER NOT NULL DEFAULT 1,
    `app_version` TEXT NOT NULL DEFAULT ''
);

CREATE INDEX IF NOT EXISTS `idx_listening_sessions_song_id` ON `listening_sessions` (`song_id`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_session_group_id` ON `listening_sessions` (`session_group_id`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_artist_id` ON `listening_sessions` (`artist_id`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_album_id` ON `listening_sessions` (`album_id`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_start_time` ON `listening_sessions` (`start_time`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_start_date` ON `listening_sessions` (`start_date`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_year_month` ON `listening_sessions` (`year_month`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_year_week` ON `listening_sessions` (`year_week`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_year` ON `listening_sessions` (`year`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_month` ON `listening_sessions` (`month`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_quarter` ON `listening_sessions` (`quarter`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_hour` ON `listening_sessions` (`hour`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_day_of_week` ON `listening_sessions` (`day_of_week`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_time_period` ON `listening_sessions` (`time_period`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_is_weekend` ON `listening_sessions` (`is_weekend`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_end_reason` ON `listening_sessions` (`end_reason`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_shuffle_enabled` ON `listening_sessions` (`shuffle_enabled`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_repeat_mode` ON `listening_sessions` (`repeat_mode`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_is_favorite` ON `listening_sessions` (`is_favorite`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_output_device` ON `listening_sessions` (`output_device`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_queue_source` ON `listening_sessions` (`queue_source`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_playback_origin` ON `listening_sessions` (`playback_origin`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_audio_format` ON `listening_sessions` (`audio_format`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_song_id_year_month` ON `listening_sessions` (`song_id`, `year_month`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_artist_name_start_time` ON `listening_sessions` (`artist_name`, `start_time`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_album_id_start_time` ON `listening_sessions` (`album_id`, `start_time`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_genre_start_time` ON `listening_sessions` (`genre`, `start_time`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_start_date_hour` ON `listening_sessions` (`start_date`, `hour`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_playlist_id_start_time` ON `listening_sessions` (`playlist_id`, `start_time`);
CREATE INDEX IF NOT EXISTS `idx_listening_sessions_playback_origin_start_time` ON `listening_sessions` (`playback_origin`, `start_time`);
```

### `listening_session_groups`
```sql
CREATE TABLE IF NOT EXISTS `listening_session_groups` (
    `group_id` TEXT PRIMARY KEY NOT NULL,
    `total_songs` INTEGER NOT NULL DEFAULT 0,
    `total_duration_ms` INTEGER NOT NULL DEFAULT 0,
    `start_time` INTEGER NOT NULL DEFAULT 0,
    `end_time` INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS `idx_group_start_time` ON `listening_session_groups` (`start_time`);
```

### `history`
```sql
CREATE TABLE IF NOT EXISTS `history` (
    `id` INTEGER PRIMARY KEY NOT NULL,
    `data` TEXT NOT NULL,
    `title` TEXT NOT NULL,
    `track_number` INTEGER NOT NULL,
    `year` INTEGER NOT NULL,
    `size` INTEGER NOT NULL,
    `duration` INTEGER NOT NULL,
    `date_added` INTEGER NOT NULL,
    `date_modified` INTEGER NOT NULL,
    `album_id` INTEGER NOT NULL,
    `album_name` TEXT NOT NULL,
    `artist_id` INTEGER NOT NULL,
    `artist_name` TEXT NOT NULL,
    `album_artist_name` TEXT,
    `genre_name` TEXT,
    `time_played` INTEGER NOT NULL
);
```

### `play_counts`
```sql
CREATE TABLE IF NOT EXISTS `play_counts` (
    `id` INTEGER PRIMARY KEY NOT NULL,
    `data` TEXT NOT NULL,
    `title` TEXT NOT NULL,
    `track_number` INTEGER NOT NULL,
    `year` INTEGER NOT NULL,
    `size` INTEGER NOT NULL,
    `duration` INTEGER NOT NULL,
    `date_added` INTEGER NOT NULL,
    `date_modified` INTEGER NOT NULL,
    `album_id` INTEGER NOT NULL,
    `album_name` TEXT NOT NULL,
    `artist_id` INTEGER NOT NULL,
    `artist_name` TEXT NOT NULL,
    `album_artist_name` TEXT,
    `genre_name` TEXT,
    `time_played` INTEGER NOT NULL,
    `play_count` INTEGER NOT NULL,
    `skip_count` INTEGER NOT NULL
);
```

### `daily_summaries`
```sql
CREATE TABLE IF NOT EXISTS `daily_summaries` (
    `date` TEXT PRIMARY KEY NOT NULL,
    `total_play_count` INTEGER NOT NULL,
    `total_duration_ms` INTEGER NOT NULL,
    `top_song_id` INTEGER NOT NULL,
    `top_artist_id` INTEGER NOT NULL
);
```

### `library_stats`
```sql
CREATE TABLE IF NOT EXISTS `library_stats` (
    `id` INTEGER PRIMARY KEY NOT NULL,
    `song_count` INTEGER NOT NULL,
    `album_count` INTEGER NOT NULL,
    `artist_count` INTEGER NOT NULL,
    `favorite_count` INTEGER NOT NULL,
    `total_duration` INTEGER NOT NULL,
    `last_updated` INTEGER NOT NULL
);
```

---

## 5. Intelligent Rankings & Insights

### `lyrics`
```sql
CREATE TABLE IF NOT EXISTS `lyrics` (
    `id` INTEGER PRIMARY KEY NOT NULL,
    `lyrics` TEXT,
    `provider` TEXT,
    `is_instrumental` INTEGER NOT NULL DEFAULT 0,
    `checksum` TEXT,
    `last_updated` INTEGER,
    `provider_id` TEXT,
    `embedded_lyrics` TEXT,
    `synchronized_lyrics` TEXT,
    `unsynchronized_lyrics` TEXT,
    `language` TEXT,
    `source` INTEGER NOT NULL DEFAULT 0,
    `word_count` INTEGER NOT NULL DEFAULT 0,
    `has_embedded_lyrics` INTEGER NOT NULL DEFAULT 0,
    `has_synchronized_lyrics` INTEGER NOT NULL DEFAULT 0
);
```

### Category Rankings
*Schema for `genre_song_ranking`, `mood_song_ranking`, `tag_song_ranking`, `instrument_song_ranking`*
```sql
-- Example for Genre Song Ranking
CREATE TABLE IF NOT EXISTS `genre_song_ranking` (
    `song_id` INTEGER NOT NULL,
    `genre_id` INTEGER NOT NULL,
    `play_count` INTEGER NOT NULL,
    `listened_duration_ms` INTEGER NOT NULL,
    `combined_score` REAL NOT NULL,
    `play_rank` INTEGER NOT NULL,
    `duration_rank` INTEGER NOT NULL,
    `combined_rank` INTEGER NOT NULL,
    PRIMARY KEY(`song_id`, `genre_id`),
    FOREIGN KEY(`genre_id`) REFERENCES `genres`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
);
CREATE INDEX IF NOT EXISTS `index_genre_song_ranking_song_id` ON `genre_song_ranking` (`song_id`);
CREATE INDEX IF NOT EXISTS `index_genre_song_ranking_genre_id` ON `genre_song_ranking` (`genre_id`);
CREATE INDEX IF NOT EXISTS `index_genre_song_ranking_combined_rank` ON `genre_song_ranking` (`combined_rank`);
```

### `song_ai`
```sql
CREATE TABLE IF NOT EXISTS `song_ai` (
    `song_key` INTEGER PRIMARY KEY NOT NULL,
    `energy` REAL,
    `valence` REAL,
    `danceability` REAL,
    `speechiness` REAL,
    `tempo` REAL,
    `summary` TEXT,
    `model_name` TEXT,
    `model_version` TEXT,
    `generated_at` INTEGER NOT NULL DEFAULT 0,
    `confidence` REAL,
    FOREIGN KEY(`song_key`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE 
);
```

### `song_fingerprints`
```sql
CREATE TABLE IF NOT EXISTS `song_fingerprints` (
    `song_key` INTEGER PRIMARY KEY NOT NULL,
    `acoustid` TEXT,
    `sha256` TEXT,
    `isrc` TEXT,
    `algorithm_version` INTEGER NOT NULL DEFAULT 1,
    `scan_date` INTEGER NOT NULL DEFAULT 0,
    `confidence` REAL,
    FOREIGN KEY(`song_key`) REFERENCES `songs`(`song_key`) ON UPDATE NO ACTION ON DELETE CASCADE 
);
```

### `song_search_fts` (Virtual)
```sql
CREATE VIRTUAL TABLE IF NOT EXISTS `song_search_fts` USING fts4(
    `title`, `artist`, `album`, `lyrics`, `composer`, `lyricist`
);
```

### `database_health`
```sql
CREATE TABLE IF NOT EXISTS `database_health` (
    `id` INTEGER PRIMARY KEY NOT NULL DEFAULT 0,
    `last_sync` INTEGER NOT NULL DEFAULT 0,
    `last_ranking` INTEGER NOT NULL DEFAULT 0,
    `last_vacuum` INTEGER NOT NULL DEFAULT 0,
    `last_integrity_check` INTEGER NOT NULL DEFAULT 0,
    `schema_version` INTEGER NOT NULL DEFAULT 0
);
```

---

## 6. Relationship Joins

*Schema for `song_genre_relationship`, `song_mood_relationship`, `song_tag_relationship`, `song_instrument_relationship`*
```sql
-- Example for Song Genre Relationship
CREATE TABLE IF NOT EXISTS `song_genre_relationship` (
    `song_id` INTEGER NOT NULL,
    `genre_id` INTEGER NOT NULL,
    `confidence_score` REAL NOT NULL,
    `source` INTEGER NOT NULL,
    `date_assigned` INTEGER NOT NULL DEFAULT 0,
    `last_updated` INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(`song_id`, `genre_id`),
    FOREIGN KEY(`genre_id`) REFERENCES `genres`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
);
CREATE INDEX IF NOT EXISTS `index_song_genre_relationship_song_id` ON `song_genre_relationship` (`song_id`);
CREATE INDEX IF NOT EXISTS `index_song_genre_relationship_genre_id` ON `song_genre_relationship` (`genre_id`);
```

---

## 7. Synchronization & Work

### `pending_scrobbles`
```sql
CREATE TABLE IF NOT EXISTS `pending_scrobbles` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `artist` TEXT NOT NULL,
    `track` TEXT NOT NULL,
    `album` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL
);
```

### `pending_work`
```sql
CREATE TABLE IF NOT EXISTS `pending_work` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `entity_type` TEXT NOT NULL,
    `entity_id` INTEGER NOT NULL,
    `reason` TEXT NOT NULL,
    `priority` INTEGER NOT NULL DEFAULT 0,
    `created_at` INTEGER NOT NULL DEFAULT 0,
    `retry_count` INTEGER NOT NULL DEFAULT 0,
    `status` TEXT NOT NULL DEFAULT 'PENDING'
);
```
