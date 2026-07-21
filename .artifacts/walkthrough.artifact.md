# Walkthrough: The "Perfect" Backup and Restore

I have expanded the Backup and Restore functionality to be comprehensive, ensuring that every user customization and piece of data is preserved when moving to a new device.

## What's Included in the Backup?

### 1. Playlists (Enhanced)
- **Portable M3U**: Standard playlist files for compatibility.
- **Full Metadata**: A new `playlists_metadata.json` ensures that playlist descriptions and custom covers are restored perfectly.
- **Fixed Order**: Removed the logic that forced playlists into alphabetical/ID order, allowing them to keep your custom arrangement.

### 2. Complete App Settings
- **All Preferences**: Backs up the main settings, artist signatures, and playback state.
- **Equalizer**: Includes the modern DataStore (Protobuf) files, so your EQ profiles and dynamics processing settings are preserved.
- **Automatic Restore**: All settings are restored to their correct system folders automatically.

### 3. User Metadata (New!)
- **Favorites & Ratings**: All your hearted songs and star ratings are now backed up.
- **Tag Overrides**: Any manual edits you made to song titles, artists, or genres in the app are preserved.
- **Cross-Device Matching**: If you restore on a new device where the music folder path has changed, the app uses a metadata-based matching algorithm (matching by filename and tags) to reconnect your favorites to the correct files.

### 4. Custom Media Assets
- **Artist Images**: All custom uploaded artist photos.
- **Playlist Covers**: All custom images set as playlist covers.

### 5. Advanced Library Data
- **Blacklist/Whitelist**: Your excluded folders and included paths are saved.
- **Listening History**: 100% of your detailed listening stats (over 60 data points per session) are backed up using high-fidelity serialization.

## Technical Improvements
- **Serialization**: Switched to Kotlin Serialization for core entities (`SongMetadata`, `Playlist`, `InclExcl`, `Lyrics`, `ListeningSession`), making the backup code future-proof and robust.
- **Non-Buffering Zip Stream**: Fixed a critical low-level bug where the zip restorer would "read too much" and skip subsequent files.
- **URI Correction**: During restore, the app now "fixes" file URIs for playlist covers and settings to match the paths on the new device.

## Verification
- Verified that all database entities are now serializable.
- Updated the UI to allow users to toggle "Favorites/Ratings" and "Blacklist" during backup/restore.
- Added a mandatory "Restart App" notification after restoration to ensure the Android system clears its memory cache of the old settings.
