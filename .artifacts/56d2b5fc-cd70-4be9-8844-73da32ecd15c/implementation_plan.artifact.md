# Fix Argument Type Mismatch in SongRepository

The project fails to build because of a type mismatch in the `Song` constructor call within `SongRepository.kt`. The constructor call passes `volumeName` (a `String?`) as the 16th positional argument, but the 16th parameter in the `Song` class is `overallPlayCount: Long`.

## User Review Required

> [!IMPORTANT]
> The fix involves changing the positional arguments to named arguments for the trailing parameters (`volumeName`, `composer`, `bitrate`) in `SongRepository.kt`. This allows skipping the intermediate statistical parameters that have default values in the `Song` data class.

## Proposed Changes

### app component

#### [MODIFY] [SongRepository.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/repository/SongRepository.kt)

- Update the `Song` constructor call in `getSongFromCursorImpl` to use named arguments for `volumeName`, `composer`, and `bitrate`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileFdroidDebugKotlin` to verify the compilation error is resolved.

### Manual Verification
- None required as this is a compilation fix.
