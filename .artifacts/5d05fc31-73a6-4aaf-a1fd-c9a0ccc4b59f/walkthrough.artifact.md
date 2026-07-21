# Walkthrough - Fix Notification Lyrics Button Toggle

I have fixed the issue where the notification lyrics button would switch to lyrics but wouldn't correctly toggle back to the original song metadata.

## Changes Made

### Playback Service

#### [PlaybackService.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/playback/PlaybackService.kt)

- **Improved Metadata Restoration**: Added `setSubtitle(currentSong.songInfo())` to `restoreOriginalMetadata()`. This ensures that when lyrics mode is turned off, the notification's subtitle field (which was being overwritten by lyrics) is reset to the original song information (e.g., duration and artist).
- **Enhanced Notification Button Refresh**:
    - Updated `refreshMediaButtonCustomLayout()` to call `mediaSession.setCustomLayout()`.
    - This ensures that the notification's custom buttons (like the lyrics toggle) are updated globally across all controllers, correctly reflecting the "Show" or "Hide" state and changing the icon accordingly.
- **Fixed Missing Import**: Added the necessary import for the `songInfo` extension function.

## Verification Results

### Manual Verification Steps (Recommended)
1.  Play a song with synced lyrics.
2.  Open the playback notification.
3.  Tap the **Lyrics** icon in the notification.
    - Confirm the title, artist, and subtitle fields update to show the current and next lyrics lines.
    - Confirm the button icon changes to the filled version.
4.  Tap the **Lyrics** icon again.
    - Confirm all three fields (Title, Artist, Subtitle) return to their original values.
    - Confirm the button icon changes back to the outline version.
