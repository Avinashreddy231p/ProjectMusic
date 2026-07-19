# Tasks: Fix Metadata Assignment and Display

- [x] `[x]` **Database Schema Refactor**
    - [x] Update `SongMoodEntity`, `SongTagEntity`, `SongInstrumentEntity`, `SongGenreEntity` to remove foreign keys to `SongEntity` and ensure they use MediaStore IDs.
    - [x] Update `MoodSongRankingEntity`, `TagSongRankingEntity`, `InstrumentSongRankingEntity`, `GenreSongRankingEntity` to remove foreign keys to `SongEntity`.
    - [x] Update `ProjectMusicDatabase` version to 11 and added `MIGRATION_10_11`.
    - [x] Register migration in `MainModule`.
- [x] `[x]` **Data Layer Updates**
    - [x] Update `MetadataDao` with queries to fetch assigned song IDs.
    - [x] Update `RankingDao` to return song IDs (`Long`) instead of `SongEntity`.
    - [x] Update `MetadataRepository` (interface and implementation) to include all assigned songs in detail objects.
- [x] `[x]` **ViewModel & UI Fixes**
    - [x] Fix argument swap in `MoodDetailViewModel.assignSongs`.
    - [x] Update `MoodDetailScreen` to display all assigned songs.
    - [x] Verified `TagDetailScreen` and `InstrumentDetailScreen` exist and updated them as well.
- [ ] `[/]` **Verification**
    - [ ] Verify metadata assignment in the app.
    - [ ] Verify detail screens show correctly.
