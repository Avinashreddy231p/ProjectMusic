# Implementation Plan - Library-wide Lyrics Lookup

Add a feature to search and download lyrics for the entire music library, similar to the existing MusicBrainz and File Tag scanners.

## User Review Required

> [!IMPORTANT]
> A full library lookup will perform network requests for every song in the database. This may take significant time and consume data. A small delay will be added between requests to prevent hitting rate limits on lyrics providers.

## Proposed Changes

### Core Repository

#### [MODIFY] [LyricsRepository.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/repository/LyricsRepository.kt)
- Add `scanAllLyrics(onProgress: (Int, Int, String) -> Unit): LyricsScanResult` to the interface and implementation.
- Update `lookupLyricsByIds` to include a small throttle (e.g., 500ms) to avoid rate limits during mass lookups.

### UI State and ViewModel

#### [MODIFY] [SettingsUiState.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/SettingsViewModel.kt)
- Add `lyricsScanning`, `lyricsScanResult`, `lyricsScanProgress`, `lyricsScanTotal`, and `lyricsScanLabel` to `SettingsUiState`.

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/SettingsViewModel.kt)
- Implement `runLyricsScan()`: fetches all song IDs and calls `lyricsRepository.scanAllLyrics`.
- Implement `clearLyricsScanResult()` to reset the UI state.

### UI Components

#### [MODIFY] [NetworkSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/NetworkSettingsComposeScreen.kt)
- Add the "Look Up Song Lyrics" button to the "Lyrics Search" section.
- Use `ScanButtonWithProgress` to show the progress, total songs, and the currently processing song title.

## Verification Plan

### Automated Tests
- Verify `scanAllLyrics` correctly retrieves all songs and calls the lookup logic.
- Verify `lookupLyricsByIds` respects the throttle delay.

### Manual Verification
- Go to **Settings > Network**.
- Click **Look Up Song Lyrics**.
- Observe the progress bar and labels updating as songs are processed.
- Verify that lyrics are actually saved to the database and embedded in files (where permissions allow).
