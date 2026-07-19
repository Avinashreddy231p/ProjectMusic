# Walkthrough: Metadata Assignment and Display Fixes

I have fixed the issue where adding songs to Moods, Tags, and Instruments was not working and they were not appearing in the detail screens.

## Key Changes

### 1. Database Schema Refactor
Standardized the metadata system to use **MediaStore IDs** instead of internal database keys.
- **Removed Foreign Keys to `SongEntity`**: This ensures that songs can be tagged even if they are not in any playlist.
- **Recreated Tables**: Updated `SongMoodEntity`, `SongTagEntity`, etc., to decouple them from the playlist-specific `SongEntity`.
- **Database Migration**: Incremented database version to **11** and provided `MIGRATION_10_11` to safely update the schema by recreating the metadata relationship and ranking tables.

### 2. Data Layer Improvements
- **MetadataDao**: Added new methods `getSongIdsForMood`, `getSongIdsForTag`, and `getSongIdsForInstrument` to retrieve all assigned songs.
- **RankingDao**: Updated ranking queries to return song IDs instead of entities, ensuring consistency.
- **MetadataRepository**: Updated to fetch the full list of assigned songs and include them in the `MoodDetail`, `TagDetail`, and `InstrumentDetail` objects.

### 3. Logic Fixes
- **ViewModel Argument Fix**: Fixed a bug in `MoodDetailViewModel`, `TagDetailViewModel`, and `InstrumentDetailViewModel` where song IDs and category IDs were being swapped during assignment.
- **Detail Screen Updates**: Updated `MoodDetailScreen`, `TagDetailScreen`, and `InstrumentDetailScreen` to show **all assigned songs** under a "Songs" section, instead of just the top 5 most played ones. This ensures newly added songs are immediately visible.

## Verification Results
- Assigned songs are now correctly stored using their MediaStore IDs.
- The "Songs" section in detail screens correctly lists all songs assigned to that mood/tag/instrument.
- Database migration was implemented to handle the schema change.

> [!IMPORTANT]
> Because the metadata relationship tables were recreated to fix the schema, any existing Mood/Tag/Instrument assignments made in version 10 will be reset. New assignments will now work correctly and persist across updates.
