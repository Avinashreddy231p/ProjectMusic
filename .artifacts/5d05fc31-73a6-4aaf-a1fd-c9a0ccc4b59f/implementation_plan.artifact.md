# Implementation Plan - Rich Feedback & Progress Reporting

Add visual progress (e.g., "10/100") to the loading screen during playlist import and provide detailed feedback upon completion.

## User Review Required

> [!IMPORTANT]
> The import process will now show real-time progress. Since matching is extremely fast, the numbers will move quickly, but it provides assurance that the app is active.
>
> After completion, the feedback will include exactly how many songs were matched out of the total found in the file.

## Proposed Changes

### Progress State Management

#### [MODIFY] [LibraryResult](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/library/LibraryResult.kt)
- Update `PlaylistImportState` to include `Progress(current: Int, total: Int)`.

### Optimized Readers with Progress

#### [MODIFY] [M3UReader](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/util/playlist/M3UReader.kt), [PLSReader](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/util/playlist/PLSReader.kt), [JSONPlaylistIO](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/util/playlist/JSONPlaylistIO.kt)
- Add `onProgress: (Int, Int) -> Unit` parameter to `read` functions.
- For M3U/PLS: Pre-calculate the total entries to provide an accurate percentage.
- For JSON: Use the list size from the decoded object.

### ViewModel Refactor

#### [MODIFY] [LibraryViewModel](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/library/LibraryViewModel.kt)
- In `importPlaylistFromFile`, pass a lambda that updates `_playlistImportState` with `Progress`.
- Enrich the `Success` message with song matching stats.

### UI Overhaul

#### [MODIFY] [dialog_loading.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/layout/dialog_loading.xml)
- Add a `MaterialTextView` to show the "X/Y" progress text alongside the spinner.

#### [MODIFY] [PlaylistListFragment](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/library/playlists/PlaylistListFragment.kt)
- Update `toggleProgressDialog` to find and update the progress text.
- Observe `Progress` state and update the UI accordingly.

## Verification Plan

### Manual Verification
- **Progress Visibility**: Import a medium-sized playlist and verify that the "X/Y" text updates on the loading screen.
- **Detailed Feedback**: Verify the success Toast says something like "Imported Playlist (48/50 songs found)".
