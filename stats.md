# Booming Music — Advanced Listening Stats Schema & Analytics (v2)

---

## Part 1: Complete Schema

### Table: `listening_sessions` (68 Columns)

#### Identity

| # | Column | Type | Description |
|---|--------|------|-------------|
| 1 | `session_id` | BIGINT PRIMARY KEY | Auto-increment row identifier |
| 2 | `session_group_id` | TEXT | UUID grouping songs into one continuous listening session |
| 3 | `song_id` | INTEGER | Internal song identifier from MediaStore |
| 4 | `artist_id` | INTEGER | Internal artist identifier from MediaStore |

#### Song Metadata (Snapshot at Playback Time)

| # | Column | Type | Description |
|---|--------|------|-------------|
| 5 | `song_title` | TEXT | Song title |
| 6 | `artist_name` | TEXT | Primary artist name |
| 7 | `album_artist` | TEXT | Album artist (for compilations) |
| 8 | `album_id` | INTEGER | Internal album identifier |
| 9 | `album_name` | TEXT | Album name |
| 10 | `genre` | TEXT | Music genre |
| 11 | `release_year` | INTEGER | Song release year |
| 12 | `composer` | TEXT | Composer metadata |
| 13 | `lyricist` | TEXT | Lyricist metadata |
| 14 | `publisher` | TEXT | Publisher/label metadata |

#### Audio Info

| # | Column | Type | Description |
|---|--------|------|-------------|
| 15 | `song_duration_ms` | BIGINT | Total song length in milliseconds |
| 16 | `audio_format` | TEXT | Codec: mp3, flac, aac, ogg, opus, m4a, wav, alac |
| 17 | `audio_sample_rate` | INTEGER | Sample rate in Hz (44100, 48000, 96000) |
| 18 | `audio_channel_count` | INTEGER | Channel count (1=mono, 2=stereo, 6=5.1) |
| 19 | `bitrate_kbps` | INTEGER | Bitrate in kbps |
| 20 | `playback_speed` | REAL | Playback speed (1.0, 1.25, 1.5, 2.0) |
| 21 | `equalizer_active` | BOOLEAN | Whether EQ/bass/reverb was enabled |

#### Temporal (Pre-computed from Local Time at Insert)

| # | Column | Type | Description |
|---|--------|------|-------------|
| 22 | `start_time` | BIGINT | Playback start (UTC epoch millis) |
| 23 | `end_time` | BIGINT | Playback end (UTC epoch millis) |
| 24 | `time_standard` | TEXT | "UTC" |
| 25 | `timezone_id` | TEXT | IANA timezone ID: "Asia/Kolkata", "America/New_York" |
| 26 | `timezone_offset_minutes` | INTEGER | Device UTC offset in minutes (for fast numeric queries) |
| 27 | `start_date` | TEXT | Date only: "2026-07-12" |
| 28 | `start_time_only` | TEXT | Time only: "14:30:00" |
| 29 | `day_of_week` | TEXT | "Monday"–"Sunday" |
| 30 | `day_of_month` | INTEGER | 1–31 |
| 31 | `day_of_year` | INTEGER | 1–365/366 |
| 32 | `week_of_year` | INTEGER | 1–53 |
| 33 | `month` | INTEGER | 1–12 |
| 34 | `month_name` | TEXT | "January"–"December" |
| 35 | `quarter` | INTEGER | 1–4 |
| 36 | `year` | INTEGER | Calendar year |
| 37 | `year_month` | TEXT | "YYYY-MM" |
| 38 | `year_week` | TEXT | "YYYY-WW" |
| 39 | `hour` | INTEGER | 0–23 |
| 40 | `minute` | INTEGER | 0–59 |
| 41 | `second` | INTEGER | 0–59 |
| 42 | `time_period` | TEXT | Early Morning / Morning / Afternoon / Evening / Night |
| 43 | `is_weekend` | BOOLEAN | Saturday or Sunday |

#### Playback Metrics

| # | Column | Type | Description |
|---|--------|------|-------------|
| 44 | `playback_duration_ms` | BIGINT | Actual playback duration (wall clock) |
| 45 | `effective_listened_ms` | BIGINT | Effective listened time (minus noise/skip periods) |
| 46 | `completion_percent` | REAL | Percentage of song completed (0.0–100.0) |
| 47 | `end_reason` | TEXT | Why playback ended (see enum) |

> **Derived columns (NOT stored, computed in queries):**
> - `completed` = `completion_percent >= 90.0`
> - `skipped` = `completion_percent < 90.0 AND end_reason NOT IN ('track_finished', 'repeat')`

**`end_reason` values:**
```
track_finished     repeat            next
previous           seek              app_closed
service_destroyed  playlist_changed  error
sleep_timer        bluetooth_disconnect  audio_focus_loss
```

#### Behavior Tracking

| # | Column | Type | Description |
|---|--------|------|-------------|
| 48 | `pause_count` | INTEGER | Number of pause/resume cycles |
| 49 | `pause_duration_ms` | BIGINT | Total time spent paused |
| 50 | `seek_count` | INTEGER | Total seek operations |
| 51 | `seek_forward_count` | INTEGER | Forward seeks only |
| 52 | `seek_backward_count` | INTEGER | Backward seeks only |

#### Queue Context

| # | Column | Type | Description |
|---|--------|------|-------------|
| 53 | `shuffle_enabled` | BOOLEAN | Whether shuffle was active |
| 54 | `repeat_mode` | TEXT | "off" / "one" / "all" |
| 55 | `queue_position` | INTEGER | Position in queue (0-indexed) |
| 56 | `queue_source` | TEXT | What list/collection triggered playback |
| 57 | `playback_origin` | TEXT | How the user initiated playback |
| 58 | `playlist_id` | TEXT | Playlist ID if applicable |
| 59 | `playlist_name` | TEXT | Playlist name snapshot |
| 60 | `is_favorite` | BOOLEAN | Whether song was favorited at playback time |

**`queue_source` values (what collection):**
```
album        artist       playlist     genre
folder       search       recent       most_played
favorites    last_added   never_played shuffle_all
queue_next   auto
```

**`playback_origin` values (how initiated):**
```
manual_tap     voice_search   widget         notification
android_auto   sleep_resume   auto_play      external_intent
bluetooth_resume  shortcut    media_button   unknown
```

#### Device Context

| # | Column | Type | Description |
|---|--------|------|-------------|
| 61 | `output_device` | TEXT | Speaker / Wired Headset / Bluetooth / USB / HDMI / AUX |
| 62 | `volume_start` | INTEGER | Volume level at song start |
| 63 | `volume_end` | INTEGER | Volume level at song end |
| 64 | `battery_level` | INTEGER | Battery percentage (0–100) |
| 65 | `charging` | BOOLEAN | Whether device was charging |
| 66 | `screen_on` | BOOLEAN | Whether screen was on during playback |

#### App

| # | Column | Type | Description |
|---|--------|------|-------------|
| 67 | `app_version` | TEXT | App version at time of playback |

---

### Table: `listening_session_groups` (5 Columns)

Session-level aggregates stored once per session group, not repeated per track row.

| # | Column | Type | Description |
|---|--------|------|-------------|
| 1 | `group_id` | TEXT PRIMARY KEY | UUID |
| 2 | `total_songs` | INTEGER | Total songs in this session group |
| 3 | `total_duration_ms` | BIGINT | Total wall clock elapsed time |
| 4 | `start_time` | BIGINT | First song start (UTC epoch millis) |
| 5 | `end_time` | BIGINT | Last song end (UTC epoch millis) |

---

### Table: `playback_events` (Future — Phase 2)

Granular event log for deep behavior analytics.

| # | Column | Type | Description |
|---|--------|------|-------------|
| 1 | `event_id` | BIGINT PRIMARY KEY | Auto-increment |
| 2 | `session_id` | BIGINT FK | Links to listening_sessions.session_id |
| 3 | `event_type` | TEXT | pause / resume / seek / buffer / error / repeat / volume_change |
| 4 | `event_time` | BIGINT | Epoch millis |
| 5 | `event_position_ms` | BIGINT | Playback position at event |
| 6 | `event_data` | TEXT | JSON blob for event-specific data |

---

## Part 2: All Possible Analytics

### A. Overall Stats (Across All Data)

#### A1. Listening Volume
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Total sessions played | `COUNT(*)` | Every song played = 1 session |
| Total listening time | `SUM(playback_duration_ms)` | Wall clock playback time |
| Total effective listen time | `SUM(effective_listened_ms)` | Excluding noise/skip periods |
| Average session duration | `AVG(playback_duration_ms)` | Mean time per song |
| Median session duration | `PERCENTILE(playback_duration_ms, 0.5)` | Median time per song |
| Longest single session | `MAX(playback_duration_ms)` | Longest song play-through |
| Shortest single session | `MIN(playback_duration_ms)` | Shortest play-through |
| Total listening hours | `SUM(playback_duration_ms) / 3600000` | Human-readable total |
| Daily average listening | `SUM(playback_duration_ms) / COUNT(DISTINCT start_date)` | Avg minutes per active day |

#### A2. Library Exploration
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Unique songs played | `COUNT(DISTINCT song_id)` | Songs ever touched |
| Unique artists played | `COUNT(DISTINCT artist_name)` | Artists ever heard |
| Unique albums played | `COUNT(DISTINCT album_id)` | Albums ever explored |
| Unique genres played | `COUNT(DISTINCT genre)` | Genre diversity |
| Unique playlists used | `COUNT(DISTINCT playlist_id)` | Playlist variety |
| Library coverage % | `unique_songs_played / total_library_size` | % of library actually listened to |
| Most prolific day | `start_date` with `MAX(COUNT(*))` | Day with most plays |
| Most prolific month | `year_month` with `MAX(COUNT(*))` | Month with most plays |

#### A3. Completion & Skip Behavior (Derived)
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Overall completion rate | `AVG(CASE WHEN completion_percent >= 90.0 THEN 100.0 ELSE 0 END)` | % of songs finished |
| Overall skip rate | `SUM(CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1 ELSE 0 END) * 100.0 / COUNT(*)` | % of songs skipped |
| Average completion % | `AVG(completion_percent)` | Mean completion percentage |
| Median completion % | `PERCENTILE(completion_percent, 0.5)` | Median completion |
| Songs never finished | `COUNT(*) WHERE completion_percent < 10` | Barely listened songs |
| Songs fully completed | `COUNT(*) WHERE completion_percent >= 90.0` | Songs played to ~end |
| Skip-to-completion ratio | `skipped_count / completed_count` | Balance metric |

#### A4. Pause & Seek Behavior
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Average pauses per song | `AVG(pause_count)` | Engagement indicator |
| Total pause time | `SUM(pause_duration_ms)` | Time spent paused |
| Pause time % | `SUM(pause_duration_ms) * 100.0 / SUM(playback_duration_ms + pause_duration_ms)` | % of time paused |
| Average seeks per song | `AVG(seek_count)` | Navigation indicator |
| Total forward seeks | `SUM(seek_forward_count)` | Forward navigation |
| Total backward seeks | `SUM(seek_backward_count)` | Backward navigation |
| Forward:backward ratio | `SUM(seek_forward_count) / SUM(seek_backward_count)` | Exploration vs replay |
| High-seek songs | Songs with `seek_count > 5` | Songs users navigate around |
| High-pause songs | Songs with `pause_count > 3` | Songs users interrupt often |

#### A5. Temporal Patterns
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Peak listening hour | `GROUP BY hour ORDER BY COUNT(*) DESC LIMIT 1` | Busiest hour |
| Peak listening day | `GROUP BY day_of_week ORDER BY COUNT(*) DESC LIMIT 1` | Busiest day |
| Weekend vs weekday ratio | `SUM(is_weekend) / SUM(NOT is_weekend)` | Weekend listening preference |
| Weekend listening time | `SUM(playback_duration_ms) WHERE is_weekend` | Total weekend listening |
| Weekday listening time | `SUM(playback_duration_ms) WHERE NOT is_weekend` | Total weekday listening |
| Time period distribution | `GROUP BY time_period` | When user listens most |
| Late night listening % | `COUNT(*) WHERE hour BETWEEN 0 AND 4 * 100.0 / COUNT(*)` | Night owl metric |
| Early morning % | `COUNT(*) WHERE time_period = 'Early Morning'` | Morning person metric |
| Quarterly trend | `GROUP BY quarter` | Seasonal patterns |
| Monthly trend | `GROUP BY year_month` | Growth/decline over time |
| Weekly trend | `GROUP BY year_week` | Short-term patterns |
| Day-of-month distribution | `GROUP BY day_of_month` | Payday effect, etc. |
| Hourly heatmap | `GROUP BY day_of_week, hour` | Full weekly heatmap |

#### A6. Listening Streaks
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Current streak | Consecutive days with plays | Active listening streak |
| Longest ever streak | Max consecutive days | Record streak |
| Active days count | `COUNT(DISTINCT start_date)` | Days with any listening |
| Total days span | `MAX(start_date) - MIN(start_date)` | Account age |
| Listening frequency | `active_days / total_days_span` | Consistency metric |
| Days since last listen | `MAX(start_date)` recency | Lapsed user detection |

#### A7. End Reasons
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| End reason distribution | `GROUP BY end_reason` | How sessions end |
| App closed % | `COUNT(*) WHERE end_reason = 'app_closed'` | Abrupt endings |
| Natural finish % | `COUNT(*) WHERE end_reason = 'track_finished'` | Natural completions |
| Error rate | `COUNT(*) WHERE end_reason = 'error'` | Playback failures |
| Sleep timer usage | `COUNT(*) WHERE end_reason = 'sleep_timer'` | Feature adoption |
| Bluetooth disconnect % | `COUNT(*) WHERE end_reason = 'bluetooth_disconnect'` | Connectivity issues |

#### A8. Queue & Playback Mode
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Shuffle usage % | `SUM(shuffle_enabled) * 100.0 / COUNT(*)` | How often shuffle is on |
| Repeat mode distribution | `GROUP BY repeat_mode` | Off vs One vs All usage |
| Repeat One power users | `COUNT(*) WHERE repeat_mode = 'one'` | Repeat obsessives |
| Queue source distribution | `GROUP BY queue_source` | What collections are used |
| Playback origin distribution | `GROUP BY playback_origin` | How music is initiated |
| Manual tap % | `COUNT(*) WHERE playback_origin = 'manual_tap'` | Direct user action |
| Voice search % | `COUNT(*) WHERE playback_origin = 'voice_search'` | Voice-initiated |
| Widget play % | `COUNT(*) WHERE playback_origin = 'widget'` | Widget usage |
| Auto-play % | `COUNT(*) WHERE playback_origin = 'auto_play'` | Queue advancement |
| Queue position avg | `AVG(queue_position)` | Typical queue depth |
| First in queue % | `COUNT(*) WHERE queue_position = 0` | "Play now" frequency |

#### A9. Audio Quality
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Format distribution | `GROUP BY audio_format` | mp3 vs flac vs aac etc. |
| FLAC % | `SUM(CASE WHEN audio_format = 'flac' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)` | Audiophile ratio |
| Average bitrate | `AVG(bitrate_kbps)` | Quality level |
| High-res listening % | `SUM(CASE WHEN bitrate_kbps >= 320 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)` | Quality preference |
| Sample rate distribution | `GROUP BY audio_channel_count` | Stereo vs mono vs surround |
| EQ usage % | `SUM(equalizer_active) * 100.0 / COUNT(*)` | Power user ratio |
| Playback speed distribution | `GROUP BY playback_speed` | Speed preference |

#### A10. Device & Context
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Output device distribution | `GROUP BY output_device` | Speaker vs BT vs Wired |
| Bluetooth listening % | `SUM(CASE WHEN output_device LIKE '%Bluetooth%' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)` | Wireless preference |
| Wired headset % | `SUM(CASE WHEN output_device LIKE '%Wired%' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)` | Wired preference |
| Car listening % | `SUM(CASE WHEN output_device = 'Car' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)` | Commute listening |
| Screen-on listening % | `SUM(screen_on) * 100.0 / COUNT(*)` | Active vs background |
| Screen-off listening % | `SUM(NOT screen_on) * 100.0 / COUNT(*)` | Background listening |
| Battery at start distribution | `GROUP BY battery_level / 10 * 10` | Battery correlation |
| Charging while listening % | `SUM(charging) * 100.0 / COUNT(*)` | Charging habit |
| Volume at start avg | `AVG(volume_start)` | Typical volume |
| Volume change avg | `AVG(volume_end - volume_start)` | Volume fatigue |
| High volume % | `SUM(CASE WHEN volume_start >= 12 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)` | Loud listening |
| Low volume % | `SUM(CASE WHEN volume_start <= 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)` | Quiet listening |

#### A11. Favorites Behavior
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Favorite play % | `SUM(is_favorite) * 100.0 / COUNT(*)` | How often favorites are played |
| Favorite completion rate | `AVG(CASE WHEN is_favorite THEN completion_percent ELSE NULL END)` | Do favorites get finished more? |
| Non-favorite completion rate | `AVG(CASE WHEN NOT is_favorite THEN completion_percent ELSE NULL END)` | Comparison baseline |
| Favorite skip rate | `AVG(CASE WHEN is_favorite THEN CASE WHEN completion_percent < 90.0 THEN 1.0 ELSE 0.0 END ELSE NULL END)` | Do favorites get skipped? |

#### A12. Session Analytics (via listening_session_groups JOIN)
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Total session groups | `SELECT COUNT(*) FROM listening_session_groups` | Distinct listening sessions |
| Avg songs per session | `SELECT AVG(total_songs) FROM listening_session_groups` | Session depth |
| Avg session length | `SELECT AVG(total_duration_ms) FROM listening_session_groups` | Session duration |
| Max songs in session | `SELECT MAX(total_songs) FROM listening_session_groups` | Deepest session |
| Max session length | `SELECT MAX(total_duration_ms) FROM listening_session_groups` | Longest session |
| Single-song sessions % | Sessions with `total_songs = 1` × 100 / total | Quick-play ratio |
| Long sessions % (10+ songs) | Sessions with `total_songs >= 10` × 100 / total | Deep listening |

#### A13. Timezone & Travel
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Timezone count | `COUNT(DISTINCT timezone_id)` | Number of timezones used |
| Most common timezone | `GROUP BY timezone_id ORDER BY COUNT(*) DESC LIMIT 1` | Home timezone |
| Timezone changes | `COUNT(DISTINCT timezone_offset_minutes)` | Travel indicator |
| Travel listening patterns | Cross-filter by timezone_id | Listening while traveling |

---

### B. Per-Track Stats

#### B1. Track Performance
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Total plays | `COUNT(*) WHERE song_id = X` | Popularity |
| Total listening time | `SUM(playback_duration_ms) WHERE song_id = X` | Total time invested |
| Average completion % | `AVG(completion_percent) WHERE song_id = X` | Engagement quality |
| Completion rate | `AVG(CASE WHEN completion_percent >= 90.0 THEN 1.0 ELSE 0.0 END) WHERE song_id = X` | % of plays finished |
| Skip rate | `AVG(CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1.0 ELSE 0.0 END) WHERE song_id = X` | % of plays skipped |
| Average duration played | `AVG(playback_duration_ms) WHERE song_id = X` | Typical listen length |
| First played | `MIN(start_time) WHERE song_id = X` | Discovery date |
| Last played | `MAX(start_time) WHERE song_id = X` | Recency |
| Days since last play | `JULIANDAY('now') - JULIANDAY(MAX(start_date)) WHERE song_id = X` | Staleness |
| Days since first play | `JULIANDAY('now') - JULIANDAY(MIN(start_date)) WHERE song_id = X` | Age |
| Play frequency | `COUNT(*) / COUNT(DISTINCT start_date) WHERE song_id = X` | Plays per active day |
| Peak play hour | `GROUP BY hour ORDER BY COUNT(*) DESC LIMIT 1 WHERE song_id = X` | When this song is played |
| Peak play day | `GROUP BY day_of_week ORDER BY COUNT(*) DESC LIMIT 1 WHERE song_id = X` | Which day this song is played |

#### B2. Track Behavior
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Average pause count | `AVG(pause_count) WHERE song_id = X` | Interruption level |
| Average seek count | `AVG(seek_count) WHERE song_id = X` | Navigation level |
| Forward seek ratio | `SUM(seek_forward_count) / SUM(seek_count) WHERE song_id = X` | Exploration vs replay |
| High-pause plays | `COUNT(*) WHERE song_id = X AND pause_count > 3` | Interrupted plays |
| High-seek plays | `COUNT(*) WHERE song_id = X AND seek_count > 5` | Navigated plays |
| Perfect plays | `COUNT(*) WHERE song_id = X AND completion_percent >= 90.0 AND pause_count = 0 AND seek_count = 0` | Uninterrupted listens |
| Repeat plays (repeat_one) | `COUNT(*) WHERE song_id = X AND repeat_mode = 'one'` | Obsessive replays |
| Shuffle plays | `SUM(CASE WHEN shuffle_enabled THEN 1 ELSE 0 END) WHERE song_id = X` | Discovery plays |
| Favorite plays | `SUM(is_favorite) WHERE song_id = X` | Play-while-favorited |
| Playlist appearances | `COUNT(DISTINCT playlist_id) WHERE song_id = X AND playlist_id != ''` | Cross-playlist presence |

#### B3. Track Context
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Average completion by output device | `GROUP BY output_device, AVG(completion_percent) WHERE song_id = X` | Device preference |
| Completion when favorited vs not | `GROUP BY is_favorite, AVG(completion_percent) WHERE song_id = X` | Favorite boost |
| Completion by time period | `GROUP BY time_period, AVG(completion_percent) WHERE song_id = X` | Time context |
| Completion by shuffle on/off | `GROUP BY shuffle_enabled, AVG(completion_percent) WHERE song_id = X` | Shuffle impact |
| Volume when played | `AVG(volume_start) WHERE song_id = X` | Volume preference |
| Average playback speed | `AVG(playback_speed) WHERE song_id = X` | Speed preference |
| Playback origin breakdown | `GROUP BY playback_origin WHERE song_id = X` | How track is initiated |

---

### C. Per-Album Stats

#### C1. Album Performance
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Total plays | `COUNT(*) WHERE album_id = X` | Album popularity |
| Total listening time | `SUM(playback_duration_ms) WHERE album_id = X` | Time invested |
| Unique songs played | `COUNT(DISTINCT song_id) WHERE album_id = X` | Album exploration depth |
| Album completion % | `unique_songs_played / total_songs_in_album` | How much of album was heard |
| Average completion per song | `AVG(completion_percent) WHERE album_id = X` | Song-level engagement |
| Overall completion rate | `AVG(CASE WHEN completion_percent >= 90.0 THEN 1.0 ELSE 0.0 END) WHERE album_id = X` | Finish rate |
| Skip rate | `AVG(CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1.0 ELSE 0.0 END) WHERE album_id = X` | Skip tendency |
| First played | `MIN(start_time) WHERE album_id = X` | Discovery date |
| Last played | `MAX(start_time) WHERE album_id = X` | Recency |
| Days since last play | `JULIANDAY('now') - JULIANDAY(MAX(start_date)) WHERE album_id = X` | Staleness |
| Average plays per song | `COUNT(*) / COUNT(DISTINCT song_id) WHERE album_id = X` | Depth of engagement |
| Peak play period | `GROUP BY year_month ORDER BY COUNT(*) DESC LIMIT 1 WHERE album_id = X` | When album was hot |

#### C2. Album Behavior
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Full-album sessions | Sessions where all album songs were played | Complete listens |
| Partial listen % | Sessions where not all album songs were played | Incomplete sessions |
| Average songs per session | `AVG(sg.total_songs) FROM listening_sessions ls JOIN listening_session_groups sg ON ls.session_group_id = sg.group_id WHERE ls.album_id = X` | Session depth |
| Repeat mode distribution | `GROUP BY repeat_mode WHERE album_id = X` | How album was consumed |
| Shuffle vs sequential | `GROUP BY shuffle_enabled WHERE album_id = X` | Play order preference |
| Average pause count | `AVG(pause_count) WHERE album_id = X` | Interruption level |
| Average seek count | `AVG(seek_count) WHERE album_id = X` | Navigation level |
| Favorite plays | `SUM(is_favorite) WHERE album_id = X` | Favorited song plays within album |
| Top track on album | `GROUP BY song_id ORDER BY COUNT(*) DESC LIMIT 1 WHERE album_id = X` | Most played track |
| Weakest track on album | `GROUP BY song_id ORDER BY AVG(completion_percent) ASC LIMIT 1 WHERE album_id = X` | Lowest engagement track |

#### C3. Album Context
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Completion by output device | `GROUP BY output_device, AVG(completion_percent) WHERE album_id = X` | Device preference |
| Completion by time of day | `GROUP BY time_period, AVG(completion_percent) WHERE album_id = X` | Listening context |
| Completion by day of week | `GROUP BY day_of_week, AVG(completion_percent) WHERE album_id = X` | Day preference |
| Playback speed distribution | `GROUP BY playback_speed WHERE album_id = X` | Speed preference |
| EQ usage | `AVG(equalizer_active) WHERE album_id = X` | Power user engagement |
| Playlist appearances | `COUNT(DISTINCT playlist_id) WHERE album_id = X AND playlist_id != ''` | Cross-playlist presence |

---

### D. Per-Artist Stats

#### D1. Artist Performance
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Total plays | `COUNT(*) WHERE artist_name = X` | Popularity |
| Total listening time | `SUM(playback_duration_ms) WHERE artist_name = X` | Time invested |
| Unique songs played | `COUNT(DISTINCT song_id) WHERE artist_name = X` | Exploration depth |
| Unique albums played | `COUNT(DISTINCT album_id) WHERE artist_name = X` | Album exploration |
| Artist completion rate | `AVG(CASE WHEN completion_percent >= 90.0 THEN 1.0 ELSE 0.0 END) WHERE artist_name = X` | Engagement quality |
| Average completion % | `AVG(completion_percent) WHERE artist_name = X` | Mean completion |
| Skip rate | `AVG(CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1.0 ELSE 0.0 END) WHERE artist_name = X` | Skip tendency |
| First played | `MIN(start_time) WHERE artist_name = X` | Discovery date |
| Last played | `MAX(start_time) WHERE artist_name = X` | Recency |
| Days since last play | `JULIANDAY('now') - JULIANDAY(MAX(start_date)) WHERE artist_name = X` | Staleness |
| Play frequency | `COUNT(*) / COUNT(DISTINCT start_date) WHERE artist_name = X` | Consistency |
| Peak play hour | `GROUP BY hour ORDER BY COUNT(*) DESC LIMIT 1 WHERE artist_name = X` | When artist is played |
| Peak play day | `GROUP BY day_of_week ORDER BY COUNT(*) DESC LIMIT 1 WHERE artist_name = X` | Day preference |
| Peak play month | `GROUP BY month ORDER BY COUNT(*) DESC LIMIT 1 WHERE artist_name = X` | Seasonal preference |

#### D2. Artist Behavior
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Average pause count | `AVG(pause_count) WHERE artist_name = X` | Interruption level |
| Average seek count | `AVG(seek_count) WHERE artist_name = X` | Navigation level |
| Repeat listens (repeat_one) | `SUM(CASE WHEN repeat_mode = 'one' THEN 1 ELSE 0 END) WHERE artist_name = X` | Obsessive plays |
| Shuffle plays | `SUM(CASE WHEN shuffle_enabled THEN 1 ELSE 0 END) WHERE artist_name = X` | Discovery plays |
| Favorite plays | `SUM(is_favorite) WHERE artist_name = X` | Favorited plays |
| Perfect plays | `COUNT(*) WHERE artist_name = X AND completion_percent >= 90.0 AND pause_count = 0 AND seek_count = 0` | Uninterrupted listens |
| Most played track | `GROUP BY song_id ORDER BY COUNT(*) DESC LIMIT 1 WHERE artist_name = X` | Hit song |
| Least played track | `GROUP BY song_id ORDER BY COUNT(*) ASC LIMIT 1 WHERE artist_name = X` | Deep cut |
| Album diversity | `COUNT(DISTINCT album_id) / COUNT(DISTINCT song_id) WHERE artist_name = X` | Spread across albums |
| Track concentration | `MAX(COUNT(*)) / COUNT(*) WHERE artist_name = X GROUP BY song_id` | One-hit dominance |

#### D3. Artist Context
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Completion by output device | `GROUP BY output_device, AVG(completion_percent) WHERE artist_name = X` | Device preference |
| Completion by time period | `GROUP BY time_period, AVG(completion_percent) WHERE artist_name = X` | Time context |
| Completion by day of week | `GROUP BY day_of_week, AVG(completion_percent) WHERE artist_name = X` | Day preference |
| Completion by shuffle on/off | `GROUP BY shuffle_enabled, AVG(completion_percent) WHERE artist_name = X` | Shuffle impact |
| Volume when played | `AVG(volume_start) WHERE artist_name = X` | Volume preference |
| Bluetooth vs speaker | `GROUP BY output_device, COUNT(*) WHERE artist_name = X` | Listening context |
| Playlist appearances | `COUNT(DISTINCT playlist_id) WHERE artist_name = X AND playlist_id != ''` | Cross-playlist presence |
| Queue source distribution | `GROUP BY queue_source WHERE artist_name = X` | How artist is discovered |
| Playback origin distribution | `GROUP BY playback_origin WHERE artist_name = X` | How artist is initiated |

---

### E. Per-Playlist Stats

#### E1. Playlist Performance
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Total plays | `COUNT(*) WHERE playlist_id = X` | Playlist popularity |
| Total listening time | `SUM(playback_duration_ms) WHERE playlist_id = X` | Time invested |
| Unique songs played | `COUNT(DISTINCT song_id) WHERE playlist_id = X` | Exploration depth |
| Playlist completion % | `unique_songs_played / total_songs_in_playlist` | How much was heard |
| Average completion per song | `AVG(completion_percent) WHERE playlist_id = X` | Song-level engagement |
| Overall completion rate | `AVG(CASE WHEN completion_percent >= 90.0 THEN 1.0 ELSE 0.0 END) WHERE playlist_id = X` | Finish rate |
| Skip rate | `AVG(CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1.0 ELSE 0.0 END) WHERE playlist_id = X` | Skip tendency |
| First played | `MIN(start_time) WHERE playlist_id = X` | Discovery date |
| Last played | `MAX(start_time) WHERE playlist_id = X` | Recency |
| Days since last play | `JULIANDAY('now') - JULIANDAY(MAX(start_date)) WHERE playlist_id = X` | Staleness |
| Peak play period | `GROUP BY year_month ORDER BY COUNT(*) DESC LIMIT 1 WHERE playlist_id = X` | When playlist was hot |

#### E2. Playlist Behavior
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Average session depth | `AVG(sg.total_songs) ... WHERE ls.playlist_id = X` | Session context |
| Average session length | `AVG(sg.total_duration_ms) ... WHERE ls.playlist_id = X` | Typical session |
| Repeat mode distribution | `GROUP BY repeat_mode WHERE playlist_id = X` | Consumption style |
| Shuffle vs sequential | `GROUP BY shuffle_enabled WHERE playlist_id = X` | Play order preference |
| Average pause count | `AVG(pause_count) WHERE playlist_id = X` | Interruption level |
| Average seek count | `AVG(seek_count) WHERE playlist_id = X` | Navigation level |
| Favorite plays within playlist | `SUM(is_favorite) WHERE playlist_id = X` | Favorited song plays |
| Top track in playlist | `GROUP BY song_id ORDER BY COUNT(*) DESC LIMIT 1 WHERE playlist_id = X` | Hit song |
| Weakest track in playlist | `GROUP BY song_id ORDER BY AVG(completion_percent) ASC LIMIT 1 WHERE playlist_id = X` | Skip-prone song |
| Track order impact | Correlation between queue_position and completion_percent | Does position matter? |

#### E3. Playlist Context
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Completion by output device | `GROUP BY output_device, AVG(completion_percent) WHERE playlist_id = X` | Device preference |
| Completion by time period | `GROUP BY time_period, AVG(completion_percent) WHERE playlist_id = X` | Time context |
| Completion by day of week | `GROUP BY day_of_week, AVG(completion_percent) WHERE playlist_id = X` | Day preference |
| Completion by battery level | `GROUP BY battery_level/10*10, AVG(completion_percent) WHERE playlist_id = X` | Battery correlation |
| Completion by screen state | `GROUP BY screen_on, AVG(completion_percent) WHERE playlist_id = X` | Active vs background |
| Volume when played | `AVG(volume_start) WHERE playlist_id = X` | Volume preference |
| Device distribution | `GROUP BY output_device WHERE playlist_id = X` | Listening context |

---

### F. Per-Genre Stats

#### F1. Genre Performance
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Total plays | `COUNT(*) WHERE genre = X` | Genre popularity |
| Total listening time | `SUM(playback_duration_ms) WHERE genre = X` | Time invested |
| Unique songs played | `COUNT(DISTINCT song_id) WHERE genre = X` | Exploration depth |
| Unique artists played | `COUNT(DISTINCT artist_name) WHERE genre = X` | Artist diversity |
| Unique albums played | `COUNT(DISTINCT album_id) WHERE genre = X` | Album exploration |
| Average completion rate | `AVG(CASE WHEN completion_percent >= 90.0 THEN 1.0 ELSE 0.0 END) WHERE genre = X` | Engagement quality |
| Skip rate | `AVG(CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1.0 ELSE 0.0 END) WHERE genre = X` | Skip tendency |
| First played | `MIN(start_time) WHERE genre = X` | Discovery date |
| Last played | `MAX(start_time) WHERE genre = X` | Recency |
| Peak play hour | `GROUP BY hour ORDER BY COUNT(*) DESC LIMIT 1 WHERE genre = X` | When genre is played |
| Peak play month | `GROUP BY month ORDER BY COUNT(*) DESC LIMIT 1 WHERE genre = X` | Seasonal preference |
| Share of total listening | `COUNT(*) * 100.0 / (SELECT COUNT(*) FROM listening_sessions) WHERE genre = X` | Dominance |
| Top artist in genre | `GROUP BY artist_name ORDER BY COUNT(*) DESC LIMIT 1 WHERE genre = X` | Dominant artist |
| Top album in genre | `GROUP BY album_id ORDER BY COUNT(*) DESC LIMIT 1 WHERE genre = X` | Dominant album |

#### F2. Genre Behavior
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Average pause count | `AVG(pause_count) WHERE genre = X` | Interruption level |
| Average seek count | `AVG(seek_count) WHERE genre = X` | Navigation level |
| Repeat listens | `SUM(CASE WHEN repeat_mode = 'one' THEN 1 ELSE 0 END) WHERE genre = X` | Obsessive plays |
| Shuffle plays | `SUM(CASE WHEN shuffle_enabled THEN 1 ELSE 0 END) WHERE genre = X` | Discovery plays |
| Favorite plays | `SUM(is_favorite) WHERE genre = X` | Favorited plays |
| Perfect plays | `COUNT(*) WHERE genre = X AND completion_percent >= 90.0 AND pause_count = 0 AND seek_count = 0` | Uninterrupted listens |
| Avg bitrate | `AVG(bitrate_kbps) WHERE genre = X` | Audio quality preference |
| Format distribution | `GROUP BY audio_format WHERE genre = X` | Codec preference |

#### F3. Genre Context
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Completion by output device | `GROUP BY output_device, AVG(completion_percent) WHERE genre = X` | Device preference |
| Completion by time period | `GROUP BY time_period, AVG(completion_percent) WHERE genre = X` | Time context |
| Completion by shuffle on/off | `GROUP BY shuffle_enabled, AVG(completion_percent) WHERE genre = X` | Shuffle impact |
| Volume when played | `AVG(volume_start) WHERE genre = X` | Volume preference |
| Bluetooth vs speaker | `GROUP BY output_device, COUNT(*) WHERE genre = X` | Listening context |
| Screen-on ratio | `SUM(screen_on) * 100.0 / COUNT(*) WHERE genre = X` | Active vs background |

---

### G. Per-Year Stats

#### G1. Year Performance
| Stat | SQL Expression | Description |
|------|---------------|-------------|
| Total plays | `COUNT(*) WHERE year = X` | Year popularity |
| Total listening time | `SUM(playback_duration_ms) WHERE year = X` | Time invested |
| Unique songs played | `COUNT(DISTINCT song_id) WHERE year = X` | Exploration depth |
| Unique artists played | `COUNT(DISTINCT artist_name) WHERE year = X` | Artist diversity |
| Average completion rate | `AVG(CASE WHEN completion_percent >= 90.0 THEN 1.0 ELSE 0.0 END) WHERE year = X` | Engagement quality |
| Skip rate | `AVG(CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1.0 ELSE 0.0 END) WHERE year = X` | Skip tendency |
| Share of total listening | `COUNT(*) * 100.0 / (SELECT COUNT(*) FROM listening_sessions) WHERE year = X` | Dominance |
| Top genre from year | `GROUP BY genre ORDER BY COUNT(*) DESC LIMIT 1 WHERE year = X` | Dominant genre |
| Top artist from year | `GROUP BY artist_name ORDER BY COUNT(*) DESC LIMIT 1 WHERE year = X` | Dominant artist |
| Top album from year | `GROUP BY album_id ORDER BY COUNT(*) DESC LIMIT 1 WHERE year = X` | Dominant album |

---

### H. Cross-Dimensional Analytics

#### H1. Artist × Album Matrix
| Stat | Description |
|------|-------------|
| Artist album play distribution | Which albums get the most plays per artist |
| Album discovery timeline | Order in which albums were discovered per artist |
| Artist album completion rates | Which albums have best/worst engagement per artist |
| Deep cut ratio | Songs with <10 plays vs top plays per artist |

#### H2. Time × Behavior Matrix
| Stat | Description |
|------|-------------|
| Hour × skip rate | Do users skip more at certain hours? |
| Day × completion rate | Are weekends more relaxed (higher completion)? |
| Time period × shuffle usage | Do users shuffle more at night? |
| Month × genre preference | Seasonal genre shifts |
| Quarter × listening volume | Quarterly listening trends |
| Year-over-year comparison | Growth metrics |

#### H3. Device × Behavior Matrix
| Stat | Description |
|------|-------------|
| Output device × skip rate | Do users skip more on Bluetooth? |
| Output device × completion rate | Do wired listeners finish more songs? |
| Screen state × pause count | More pauses when screen off? |
| Battery level × listening duration | Do users listen less at low battery? |
| Network type × audio format | Do offline users play higher quality? |
| Volume level × completion rate | Do louder listens get finished more? |

#### H4. Context × Content Matrix
| Stat | Description |
|------|-------------|
| Genre × time period | Rock in morning, jazz at night? |
| Genre × day of week | Weekday vs weekend genre shifts |
| Genre × output device | Classical on speakers, hip-hop on BT? |
| Genre × shuffle usage | Do users shuffle certain genres more? |
| Genre × completion rate | Which genres hold attention? |
| Format × completion rate | Do FLAC listeners finish more songs? |
| Bitrate × completion rate | Does quality affect engagement? |
| Playback origin × completion rate | Do widget plays finish more than manual taps? |
| Playback origin × genre | Do voice searches play different genres? |

---

### I. Advanced / Derived Analytics

#### I1. Engagement Score (Custom Formula)
```
engagement_score = (completion_percent * 0.4)
                 + (1.0 / (1 + pause_count)) * 0.2
                 + (1.0 / (1 + seek_count)) * 0.2
                 + (is_favorite * 0.1)
                 + (CASE WHEN completion_percent >= 90.0 THEN 1.0 ELSE 0.0 END * 0.1)
```
- Per song: Average engagement score
- Per album: Average engagement score of all songs
- Per artist: Average engagement score of all songs
- Per playlist: Average engagement score of all songs

#### I2. Discovery Index
```
discovery_index = unique_songs_played / total_songs_in_library
```
- Tracks exploration breadth over time
- Compare monthly to see if user is exploring or replaying

#### I3. Replay Loyalty
```
replay_loyalty = plays_of_favorite_songs / total_plays
```
- High = user replays favorites
- Low = user explores new music

#### I4. Listening Velocity
```
listening_velocity = total_listening_time_ms / (max(start_time) - min(start_time))
```
- Average listening intensity over account lifetime
- Compare monthly for growth/decline

#### I5. Skip Propensity Score
```
skip_score = (skipped_count / total_sessions)
           + (high_skip_song_count / total_unique_songs)
```
- Overall tendency to skip
- Compare across time periods for behavior changes

#### I6. Session Depth Score
```
session_depth = avg_session_total_songs / median_session_total_songs
```
- >1 = some very long sessions pull the average up
- <1 = some very short sessions pull the average down
- =1 = consistent session lengths

#### I7. Peak Listening Intensity
```
peak_intensity = MAX(COUNT(*) GROUP BY hour)
average_intensity = COUNT(*) / 24
peak_to_average_ratio = peak_intensity / average_intensity
```
- How concentrated is listening in peak hours
- High ratio = listening is ritualized (commute, workout)
- Low ratio = listening is spread throughout day

---

## Part 3: Enum Definitions

### end_reason Values
| Value | Description | Triggered By |
|-------|-------------|-------------|
| `track_finished` | Song played to natural end | Player.STATE_ENDED |
| `repeat` | Song restarted via repeat mode | REPEAT_MODE_ONE restart |
| `next` | User pressed next | Manual or auto |
| `previous` | User pressed previous | Manual |
| `seek` | User seeked past end | DISCONTINUITY_REASON_SEEK |
| `app_closed` | User swiped app from recents | onTaskRemoved |
| `service_destroyed` | Service was destroyed | onDestroy |
| `playlist_changed` | Queue was replaced/cleared | Queue manipulation |
| `error` | Playback error occurred | onPlayerError |
| `sleep_timer` | Sleep timer fired | SleepTimer.onAlarm |
| `bluetooth_disconnect` | Bluetooth device disconnected | BluetoothReceiver |
| `audio_focus_loss` | Permanent audio focus loss | Audio focus callback |

### queue_source Values
| Value | Description | Triggered By |
|-------|-------------|-------------|
| `album` | Played from album detail | AlbumDetailFragment |
| `artist` | Played from artist detail | ArtistDetailFragment |
| `playlist` | Played from playlist | PlaylistDetailFragment |
| `genre` | Played from genre detail | GenreDetailFragment |
| `folder` | Played from folder browser | FolderDetailFragment |
| `search` | Played from search results | SearchFragment |
| `recent` | Played from recently played | SmartPlaylist |
| `most_played` | Played from most played | SmartPlaylist |
| `favorites` | Played from favorites | SmartPlaylist |
| `last_added` | Played from last added | SmartPlaylist |
| `never_played` | Played from never played | SmartPlaylist |
| `shuffle_all` | Played via shuffle all | ShuffleAll action |
| `queue_next` | Added to play next queue | Queue manipulation |
| `auto` | Auto-play / queue advancement | Player transition |

### playback_origin Values
| Value | Description | Triggered By |
|-------|-------------|-------------|
| `manual_tap` | User tapped a song/album/artist | Direct tap in UI |
| `voice_search` | Voice search / Google Assistant | Voice intent |
| `widget` | Played via app widget | GlanceWidget |
| `notification` | Played via notification action | MediaSession notification |
| `android_auto` | Played via Android Auto | MediaBrowserService |
| `sleep_resume` | Resumed after sleep timer | SleepTimer.consumePendingQuit |
| `auto_play` | Auto-queue advancement | Player transition |
| `external_intent` | Played via external app | ACTION_VIEW / ACTION_PLAY |
| `bluetooth_resume` | Resumed on BT connect | BluetoothReceiver |
| `shortcut` | Played via app shortcut | ShortcutIntent |
| `media_button` | Played via headset button | KeyEvent |
| `unknown` | Source not determined | Fallback |

### repeat_mode Values
| Value | Description | Player Constant |
|-------|-------------|-----------------|
| `off` | No repeat | REPEAT_MODE_OFF |
| `one` | Repeat current song | REPEAT_MODE_ONE |
| `all` | Repeat entire queue | REPEAT_MODE_ALL |

### output_device Values
| Value | Description | Detection Method |
|-------|-------------|-----------------|
| `Speaker` | Built-in speaker | Default / no headset |
| `Wired Headset` | 3.5mm / USB-C wired | AudioDeviceType.Headset |
| `Bluetooth` | Any BT audio device | AudioDeviceType.BluetoothA2dp |
| `USB` | USB DAC / audio interface | AudioDeviceType.UsbHeadset/UsbDevice |
| `HDMI` | HDMI audio output | AudioDeviceType.Hdmi |
| `AUX` | AUX line output | AudioDeviceType.Aux |
| `Unknown` | Cannot determine | Fallback |

### audio_format Values
| Value | Description |
|-------|-------------|
| `mp3` | MPEG-1 Audio Layer 3 |
| `flac` | Free Lossless Audio Codec |
| `aac` | Advanced Audio Codec |
| `ogg` | Ogg Vorbis |
| `opus` | Opus codec |
| `m4a` | MPEG-4 Audio |
| `wav` | Waveform Audio |
| `alac` | Apple Lossless Audio Codec |
| `wma` | Windows Media Audio |
| `unknown` | Unknown format |

### time_period Values
| Value | Hour Range |
|-------|-----------|
| `Early Morning` | 0:00 – 5:59 |
| `Morning` | 6:00 – 11:59 |
| `Afternoon` | 12:00 – 16:59 |
| `Evening` | 17:00 – 20:59 |
| `Night` | 21:00 – 23:59 |

---

## Part 4: UI Display Mapping

### Stats Screen Tabs

#### Tab 1: Overview
- Total listening time (formatted: "Xh Ymin")
- Total plays
- Unique songs / artists / albums
- Completion rate (derived)
- Skip rate (derived)
- Current streak / longest streak
- Insight cards (top artist, top song, top genre, peak time)

#### Tab 2: Listening Patterns
- Hourly distribution bar chart
- Day-of-week bar chart
- Time period pie chart (Morning/Afternoon/Evening/Night)
- Weekend vs weekday comparison
- Monthly trend line chart

#### Tab 3: Top Items
- Top 10 Songs (with play count, completion %)
- Top 10 Artists (with play count, total time)
- Top 10 Albums (with play count, completion %)
- Top Genres (with play count, total time)

#### Tab 4: Behavior
- Completion rate gauge (derived)
- Skip rate gauge (derived)
- Pause behavior stats
- Seek behavior stats
- Repeat mode usage
- Shuffle usage

#### Tab 5: Context
- Output device distribution pie chart
- Network type distribution
- Screen on/off ratio
- Battery level distribution
- Volume start/end distribution

#### Tab 6: Audio Quality
- Format distribution pie chart
- Bitrate distribution histogram
- Sample rate distribution
- EQ usage percentage

#### Tab 7: Session Analytics
- Avg songs per session (from listening_session_groups)
- Avg session length (from listening_session_groups)
- Session depth distribution
- Single-song session %

#### Tab 8: Wrapped / Year in Review
- Top song of the period
- Top artist of the period
- Total listening time
- Unique songs explored
- Top genre
- Peak listening hour
- Peak listening day
- Longest streak
- Discovery count
- Most listened month

---

## Part 5: Export Schema

### CSV Header (67 data columns)
```
session_id,session_group_id,song_id,artist_id,song_title,artist_name,album_artist,album_id,album_name,genre,release_year,composer,lyricist,publisher,song_duration_ms,audio_format,audio_sample_rate,audio_channel_count,bitrate_kbps,playback_speed,equalizer_active,start_time,end_time,time_standard,timezone_id,timezone_offset_minutes,start_date,start_time_only,day_of_week,day_of_month,day_of_year,week_of_year,month,month_name,quarter,year,year_month,year_week,hour,minute,second,time_period,is_weekend,playback_duration_ms,effective_listened_ms,completion_percent,end_reason,pause_count,pause_duration_ms,seek_count,seek_forward_count,seek_backward_count,shuffle_enabled,repeat_mode,queue_position,queue_source,playback_origin,playlist_id,playlist_name,is_favorite,output_device,volume_start,volume_end,battery_level,charging,screen_on,app_version
```

### Derived Columns (computed at export, not stored)
```
completed = CASE WHEN completion_percent >= 90.0 THEN 1 ELSE 0 END
skipped = CASE WHEN completion_percent < 90.0 AND end_reason NOT IN ('track_finished','repeat') THEN 1 ELSE 0 END
```

### JSON Structure
```json
{
  "session_id": 1,
  "session_group_id": "uuid",
  "song_id": 12345,
  "artist_id": 678,
  "song_title": "Song Name",
  "artist_name": "Artist Name",
  "album_artist": "Album Artist",
  "album_id": 901,
  "album_name": "Album Name",
  "genre": "Rock",
  "release_year": 2024,
  "composer": "",
  "lyricist": "",
  "publisher": "",
  "song_duration_ms": 240000,
  "audio_format": "flac",
  "audio_sample_rate": 44100,
  "audio_channel_count": 2,
  "bitrate_kbps": 1411,
  "playback_speed": 1.0,
  "equalizer_active": false,
  "start_time": 1720800000000,
  "end_time": 1720800240000,
  "time_standard": "UTC",
  "timezone_id": "Asia/Kolkata",
  "timezone_offset_minutes": 330,
  "start_date": "2026-07-12",
  "start_time_only": "14:30:00",
  "day_of_week": "Saturday",
  "day_of_month": 12,
  "day_of_year": 193,
  "week_of_year": 28,
  "month": 7,
  "month_name": "July",
  "quarter": 3,
  "year": 2026,
  "year_month": "2026-07",
  "year_week": "2026-W28",
  "hour": 14,
  "minute": 30,
  "second": 0,
  "time_period": "Afternoon",
  "is_weekend": true,
  "playback_duration_ms": 240000,
  "effective_listened_ms": 240000,
  "completion_percent": 100.0,
  "end_reason": "track_finished",
  "pause_count": 0,
  "pause_duration_ms": 0,
  "seek_count": 0,
  "seek_forward_count": 0,
  "seek_backward_count": 0,
  "shuffle_enabled": false,
  "repeat_mode": "off",
  "queue_position": 5,
  "queue_source": "playlist",
  "playback_origin": "manual_tap",
  "playlist_id": "42",
  "playlist_name": "Workout Mix",
  "is_favorite": true,
  "output_device": "Bluetooth",
  "volume_start": 12,
  "volume_end": 10,
  "battery_level": 75,
  "charging": false,
  "screen_on": true,
  "app_version": "1.3.1-beta.2",
  "completed": true,
  "skipped": false
}
```

---

## Part 6: Database Implementation

### Recommended Indexes

```sql
-- === IDENTITY & LOOKUP ===
CREATE INDEX IF NOT EXISTS idx_session_song_id ON listening_sessions(song_id);
CREATE INDEX IF NOT EXISTS idx_session_group_id ON listening_sessions(session_group_id);
CREATE INDEX IF NOT EXISTS idx_session_artist_id ON listening_sessions(artist_id);
CREATE INDEX IF NOT EXISTS idx_session_album_id ON listening_sessions(album_id);

-- === TEMPORAL ANALYTICS ===
CREATE INDEX IF NOT EXISTS idx_session_start_time ON listening_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_session_start_date ON listening_sessions(start_date);
CREATE INDEX IF NOT EXISTS idx_session_year_month ON listening_sessions(year_month);
CREATE INDEX IF NOT EXISTS idx_session_year_week ON listening_sessions(year_week);
CREATE INDEX IF NOT EXISTS idx_session_year ON listening_sessions(year);
CREATE INDEX IF NOT EXISTS idx_session_month ON listening_sessions(month);
CREATE INDEX IF NOT EXISTS idx_session_quarter ON listening_sessions(quarter);
CREATE INDEX IF NOT EXISTS idx_session_hour ON listening_sessions(hour);
CREATE INDEX IF NOT EXISTS idx_session_day_of_week ON listening_sessions(day_of_week);
CREATE INDEX IF NOT EXISTS idx_session_time_period ON listening_sessions(time_period);
CREATE INDEX IF NOT EXISTS idx_session_is_weekend ON listening_sessions(is_weekend);

-- === BEHAVIOR FILTERS ===
CREATE INDEX IF NOT EXISTS idx_session_end_reason ON listening_sessions(end_reason);
CREATE INDEX IF NOT EXISTS idx_session_shuffle ON listening_sessions(shuffle_enabled);
CREATE INDEX IF NOT EXISTS idx_session_repeat_mode ON listening_sessions(repeat_mode);
CREATE INDEX IF NOT EXISTS idx_session_is_favorite ON listening_sessions(is_favorite);
CREATE INDEX IF NOT EXISTS idx_session_equalizer ON listening_sessions(equalizer_active);

-- === CONTEXT FILTERS ===
CREATE INDEX IF NOT EXISTS idx_session_output_device ON listening_sessions(output_device);
CREATE INDEX IF NOT EXISTS idx_session_queue_source ON listening_sessions(queue_source);
CREATE INDEX IF NOT EXISTS idx_session_playback_origin ON listening_sessions(playback_origin);
CREATE INDEX IF NOT EXISTS idx_session_audio_format ON listening_sessions(audio_format);

-- === COMPOSITE INDEXES ===
CREATE INDEX IF NOT EXISTS idx_session_song_year_month ON listening_sessions(song_id, year_month);
CREATE INDEX IF NOT EXISTS idx_session_artist_time ON listening_sessions(artist_name, start_time);
CREATE INDEX IF NOT EXISTS idx_session_album_time ON listening_sessions(album_id, start_time);
CREATE INDEX IF NOT EXISTS idx_session_genre_time ON listening_sessions(genre, start_time);
CREATE INDEX IF NOT EXISTS idx_session_date_hour ON listening_sessions(start_date, hour);
CREATE INDEX IF NOT EXISTS idx_session_playlist_time ON listening_sessions(playlist_id, start_time);
CREATE INDEX IF NOT EXISTS idx_session_source_time ON listening_sessions(queue_source, start_time);
CREATE INDEX IF NOT EXISTS idx_session_origin_time ON listening_sessions(playback_origin, start_time);
```

### Migration SQL (8 → 9)

```sql
-- ========================================
-- PHASE 1: Create new tables
-- ========================================

CREATE TABLE IF NOT EXISTS listening_session_groups (
    group_id TEXT PRIMARY KEY NOT NULL,
    total_songs INTEGER NOT NULL DEFAULT 0,
    total_duration_ms INTEGER NOT NULL DEFAULT 0,
    start_time INTEGER NOT NULL DEFAULT 0,
    end_time INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_group_start_time ON listening_session_groups(start_time);

-- ========================================
-- PHASE 2: Create new listening_sessions table
-- (Clean schema, no ALTER TABLE on existing)
-- ========================================

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
);

-- ========================================
-- PHASE 3: Migrate data from old table
-- ========================================

INSERT INTO listening_sessions_new (
    session_id, session_group_id, song_id, artist_id,
    song_title, artist_name, album_artist, album_id, album_name,
    genre, release_year, composer, lyricist, publisher,
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
    -- Compute temporal columns from start_time
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
FROM listening_sessions;

-- ========================================
-- PHASE 4: Drop old table, rename new
-- ========================================

DROP TABLE IF EXISTS listening_sessions;
ALTER TABLE listening_sessions_new RENAME TO listening_sessions;

-- ========================================
-- PHASE 5: Create all indexes
-- ========================================

CREATE INDEX IF NOT EXISTS idx_session_song_id ON listening_sessions(song_id);
CREATE INDEX IF NOT EXISTS idx_session_group_id ON listening_sessions(session_group_id);
CREATE INDEX IF NOT EXISTS idx_session_artist_id ON listening_sessions(artist_id);
CREATE INDEX IF NOT EXISTS idx_session_album_id ON listening_sessions(album_id);
CREATE INDEX IF NOT EXISTS idx_session_start_time ON listening_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_session_start_date ON listening_sessions(start_date);
CREATE INDEX IF NOT EXISTS idx_session_year_month ON listening_sessions(year_month);
CREATE INDEX IF NOT EXISTS idx_session_year ON listening_sessions(year);
CREATE INDEX IF NOT EXISTS idx_session_month ON listening_sessions(month);
CREATE INDEX IF NOT EXISTS idx_session_quarter ON listening_sessions(quarter);
CREATE INDEX IF NOT EXISTS idx_session_hour ON listening_sessions(hour);
CREATE INDEX IF NOT EXISTS idx_session_day_of_week ON listening_sessions(day_of_week);
CREATE INDEX IF NOT EXISTS idx_session_time_period ON listening_sessions(time_period);
CREATE INDEX IF NOT EXISTS idx_session_is_weekend ON listening_sessions(is_weekend);
CREATE INDEX IF NOT EXISTS idx_session_end_reason ON listening_sessions(end_reason);
CREATE INDEX IF NOT EXISTS idx_session_shuffle ON listening_sessions(shuffle_enabled);
CREATE INDEX IF NOT EXISTS idx_session_repeat_mode ON listening_sessions(repeat_mode);
CREATE INDEX IF NOT EXISTS idx_session_is_favorite ON listening_sessions(is_favorite);
CREATE INDEX IF NOT EXISTS idx_session_output_device ON listening_sessions(output_device);
CREATE INDEX IF NOT EXISTS idx_session_queue_source ON listening_sessions(queue_source);
CREATE INDEX IF NOT EXISTS idx_session_playback_origin ON listening_sessions(playback_origin);
CREATE INDEX IF NOT EXISTS idx_session_audio_format ON listening_sessions(audio_format);
CREATE INDEX IF NOT EXISTS idx_session_song_year_month ON listening_sessions(song_id, year_month);
CREATE INDEX IF NOT EXISTS idx_session_artist_time ON listening_sessions(artist_name, start_time);
CREATE INDEX IF NOT EXISTS idx_session_album_time ON listening_sessions(album_id, start_time);
CREATE INDEX IF NOT EXISTS idx_session_genre_time ON listening_sessions(genre, start_time);
CREATE INDEX IF NOT EXISTS idx_session_date_hour ON listening_sessions(start_date, hour);
CREATE INDEX IF NOT EXISTS idx_session_playlist_time ON listening_sessions(playlist_id, start_time);
CREATE INDEX IF NOT EXISTS idx_session_origin_time ON listening_sessions(playback_origin, start_time);
```

### Performance Pragmas

```sql
PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA cache_size=-8000;
PRAGMA mmap_size=268435456;
PRAGMA optimize;
```

### Storage Estimates

| Sessions | Row Size | Table Size | With Indexes |
|----------|----------|------------|--------------|
| 10,000 | ~1.6 KB | ~16 MB | ~30 MB |
| 50,000 | ~1.6 KB | ~80 MB | ~140 MB |
| 100,000 | ~1.6 KB | ~160 MB | ~270 MB |
| 500,000 | ~1.6 KB | ~800 MB | ~1.3 GB |
| 1,000,000 | ~1.6 KB | ~1.6 GB | ~2.7 GB |

At 20 songs/day average, 100k sessions ≈ 13.7 years of data.

---

## Part 7: Wrapped / Year-in-Review

### Wrapped Data Points

```kotlin
data class WrappedStats(
    val year: Int,
    val totalListeningTimeMs: Long,
    val totalSongsPlayed: Int,
    val uniqueSongsPlayed: Int,
    val uniqueArtistsPlayed: Int,
    val uniqueAlbumsPlayed: Int,
    val topSong: TopItem,
    val topArtist: TopItem,
    val topAlbum: TopItem,
    val topGenre: TopItem,
    val topDay: TopItem,
    val peakHour: Int,
    val longestStreak: Int,
    val totalMinutesListened: Long,
    val avgDailyMinutes: Double,
    val favoriteGenreByMonth: Map<String, String>,
    val listeningByMonth: Map<String, Long>,
    val listeningByDayOfWeek: Map<String, Long>,
    val listeningByHour: Map<String, Long>,
    val discoveryCount: Int,
    val avgCompletionRate: Float,
    val topOutputDevice: String,
    val mostActiveDay: String,
    val wrappedScore: WrappedScore
)

data class TopItem(
    val name: String,
    val playCount: Int,
    val totalMinutes: Long,
    val completionRate: Float
)

data class WrappedScore(
    val explorerScore: Int,
    val loyaltyScore: Int,
    val consistencyScore: Int,
    val depthScore: Int,
    val nocturnalScore: Int,
    val audiophileScore: Int
)
```

---

## Part 8: File Implementation Map

### New Files to Create
| File | Path | Description |
|------|------|-------------|
| `TemporalColumns.kt` | `data/local/room/` | Pre-computes temporal fields from epoch millis |
| `DeviceContextCollector.kt` | `playback/stats/` | Collects device/context fields at playback time |
| `SessionCoordinator.kt` | `playback/stats/` | Manages UUID session grouping and song counts |

### Files to Rewrite
| File | Path | Change |
|------|------|--------|
| `ListeningSessionEntity.kt` | `data/local/room/` | 24 → 68 columns |
| `ListeningSessionGroupEntity.kt` | `data/local/room/` | **NEW** — 5-column session group table |
| `ListeningHistoryDao.kt` | `data/local/room/` | New queries for all analytics |
| `RealTimeStatsTracker.kt` | `playback/stats/` | Collect all 68 fields |
| `StatsRepository.kt` | `data/local/repository/` | New analytics queries, updated export |

### Files to Edit
| File | Path | Change |
|------|------|--------|
| `BoomingDatabase.kt` | `core/` | Add MIGRATION_8_9, new entity, version 9 |
| `StatsFlusher.kt` | `playback/stats/` | Pass DeviceContext + SessionCoordinator |
| `PlaybackService.kt` | `playback/` | Wire DeviceContextCollector, SessionCoordinator |
| `Song.kt` | `data/model/` | Add `artistId: Long` field |
| `StatsViewModel.kt` | `ui/screen/stats/` | New UI state fields |
| `StatsScreen.kt` | `ui/screen/stats/` | Display all new tabs |
| `MainModule.kt` | root package | Register new Koin dependencies |
| `SongMapper.kt` | `data/mapper/` | Map `artistId` from MediaStore |

### Dependency Graph
```
PlaybackService
  ├── creates DeviceContextCollector
  ├── creates SessionCoordinator
  ├── creates RealTimeStatsTracker
  ├── creates StatsFlusher
  └── calls StatsFlusher lifecycle methods

RealTimeStatsTracker
  ├── uses TemporalColumns.compute(epochMs)
  ├── uses DeviceContextCollector.snapshot()
  ├── uses SessionCoordinator (UUID, song count)
  ├── builds ListeningSessionEntity (68 cols)
  └── returns to StatsFlusher

StatsFlusher
  ├── flushes to StatsRepository.insertSessions()
  └── manages periodic flush

StatsRepository
  ├── wraps ListeningHistoryDao + ListeningSessionGroupDao
  ├── provides analytics queries
  ├── provides CSV/JSON export
  └── provides Wrapped stats

StatsViewModel
  ├── calls StatsRepository
  ├── exposes StateFlow<StatsUiState>
  └── handles export

StatsScreen (Compose)
  ├── observes StatsViewModel.uiState
  └── renders 8 tabs
```
