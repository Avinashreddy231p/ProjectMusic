# Replace Add to Playlist with Edit Lyrics in Vibrant Lyrics Screen

This plan describes the changes required to replace the "Add to Playlist" icon with an "Edit Lyrics" icon in the full-screen lyrics view (specifically the Vibrant and Spotify styles) and ensure the edit functionality is correctly linked.

## Proposed Changes

### [Component: UI - Lyrics]

#### [MODIFY] [LyricsScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/lyrics/LyricsScreen.kt)

- **Modify `SpotifyLyricsBottomControls`**:
    - Replace the `IconButton` that currently calls `onAddClick` with one that calls `onEditClick`.
    - Change the icon from `R.drawable.ic_playlist_add_24dp` to `R.drawable.ic_edit_note_24dp`.
    - Update the `contentDescription` to `@string/action_lyrics_editor`.
    - Remove the `onAddClick` parameter from `SpotifyLyricsBottomControls` as it will no longer be used in this component.
    - Fix the `imageTintList` in the `AndroidView` (more menu icon) to use the provided `contentColor` instead of hardcoded white.
- **Update `SpotifyLyricsScreen`**:
    - Remove the `onAddClick` argument when calling `SpotifyLyricsBottomControls`.

#### [MODIFY] [VibrantLyricsScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/lyrics/VibrantLyricsScreen.kt)

- **Update `VibrantLyricsScreen`**:
    - Remove the `onAddClick` argument when calling `SpotifyLyricsBottomControls`.

## Verification Plan

### Manual Verification
1.  Open the "Vibrant" player style or "Spotify" player style.
2.  Open the full-screen lyrics.
3.  Verify that the "Add to Playlist" icon (plus sign) is replaced by the "Edit Lyrics" icon (note with pencil).
4.  Click the "Edit Lyrics" icon and verify that it opens the Lyrics Editor screen.
5.  Verify that the "More" menu icon (three dots) is correctly tinted (especially in light mode if applicable).
6.  Verify that "Add to Playlist" is still accessible from the "More" menu.
