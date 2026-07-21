# Implementation Plan - Fix Notification Lyrics Button Toggle

The goal is to ensure the lyrics button in the notification correctly toggles between showing lyrics in the metadata and showing the original song metadata, and that the button icon/state updates accordingly.

## User Review Required

> [!IMPORTANT]
> The fix involves ensuring that all metadata fields (Title, Artist, and Subtitle) are restored when lyrics mode is toggled off. Currently, the Subtitle field is not being restored, which might lead to lyrics remaining visible in the notification even after toggling off.

## Proposed Changes

### Playback Service

#### [MODIFY] [PlaybackService.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/playback/PlaybackService.kt)

1.  **Improve `restoreOriginalMetadata`**:
    *   Ensure `subtitle` is also restored using `currentSong.songInfo()`.
2.  **Enhance `refreshMediaButtonCustomLayout`**:
    *   Call `mediaSession?.setCustomLayout()` with the updated button list to ensure all controllers (including the standard notification) receive the updated layout, even if they aren't explicitly identified as "remote" by the existing check.
3.  **Refine `lyricsCommand` logic**:
    *   Ensure the icon and display name correctly reflect whether lyrics are currently being shown.

#### [MODIFY] [PlaybackExtension.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/playback/PlaybackExtension.kt)

*   (Optional) Add a helper if needed, but the existing ones seem sufficient.

## Verification Plan

### Automated Tests
*   Verify that `PlaybackService` correctly handles `Playback.SHOW_LYRICS` command.
*   Check that `restoreOriginalMetadata` resets title, artist, and subtitle.

### Manual Verification
1.  Play a song with synced lyrics.
2.  Open the notification.
3.  Tap the "Show Lyrics" button.
    *   Verify notification title/artist/subtitle change to lyrics lines.
    *   Verify button icon changes to "Hide Lyrics" (filled icon).
4.  Tap the button again.
    *   Verify notification title/artist/subtitle return to original song metadata.
    *   Verify button icon returns to "Show Lyrics" (outline icon).
