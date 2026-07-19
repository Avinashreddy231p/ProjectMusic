# Implementation Plan - Fix Room/KSP "Multiple properties have the same columnName: song_count" error

The build is failing because `MoodWithStatsEntity`, `TagWithStatsEntity`, and `InstrumentWithStatsEntity` each have an `@Embedded` entity (e.g., `MoodEntity`) and a separate `song_count` column. Since `MoodEntity` (and others) now also includes a `song_count` column, Room/KSP detects a name collision.

## Proposed Changes

### [Component] Database DAO and Entities

#### [MODIFY] [MetadataDao.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/database/dao/MetadataDao.kt)

- Update `MoodWithStatsEntity`, `TagWithStatsEntity`, and `InstrumentWithStatsEntity`:
    - Rename the Kotlin property `songCount` to `calculatedSongCount`.
    - Change `@ColumnInfo(name = "song_count")` to `@ColumnInfo(name = "calculated_song_count")`.
- Update queries `getAllMoodsWithStats`, `getAllTagsWithStats`, and `getAllInstrumentsWithStats`:
    - Change `COUNT(...) as song_count` to `COUNT(...) as calculated_song_count`.

### [Component] Repository

#### [MODIFY] [MetadataRepository.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/repository/MetadataRepository.kt)

- Update all usages of `entity.songCount` to `entity.calculatedSongCount` for `MoodWithStatsEntity`, `TagWithStatsEntity`, and `InstrumentWithStatsEntity`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:kspFdroidDebugKotlin` to verify the KSP error is resolved.
- Run a full build: `./gradlew assembleFdroidDebug`.

### Manual Verification
- N/A (Build fix)
