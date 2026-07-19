# Walkthrough - Fixed Room/KSP Column Name Collision

The build was failing due to a naming conflict in the database entities where both an embedded entity and the wrapper entity defined a `song_count` column.

## Changes Made

### Database DAO and Entities

#### [MetadataDao.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/database/dao/MetadataDao.kt)

- Renamed `songCount` to `calculatedSongCount` in:
    - `MoodWithStatsEntity`
    - `TagWithStatsEntity`
    - `InstrumentWithStatsEntity`
- Updated `@ColumnInfo(name = "song_count")` to `@ColumnInfo(name = "calculated_song_count")` for these entities.
- Updated queries `getAllMoodsWithStats`, `getAllTagsWithStats`, and `getAllInstrumentsWithStats` to return the count as `calculated_song_count`.

### Repository

#### [MetadataRepository.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/repository/MetadataRepository.kt)

- Updated all usages of `entity.songCount` to `entity.calculatedSongCount` for the affected entities.

## Verification Results

### Automated Tests
- Executed `:app:kspFdroidDebugKotlin` task:
    - **Status**: Build finished successfully.
