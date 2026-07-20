# Unified Canvas Implementation Plan

This plan integrates both **Local Video Canvases** and the **Spotify Canvas API** into ProjectMusic. This allows you to have the official Spotify looping video backgrounds for your tracks if you have a Spotify ID, or use your own local videos.

## User Review Required

> [!IMPORTANT]
> **Server Hosting**: To use the Spotify Canvas, you must host the [Spotify-Canvas-API](https://github.com/Paxsenix0/Spotify-Canvas-API) yourself (e.g., on Vercel or Railway).
> **Spotify IDs**: You will need to manually or automatically assign Spotify Track IDs to your local songs for the remote fetching to work.
> **Two Modes**:
> 1. **Manual**: Pick a video from your gallery.
> 2. **Auto**: Fetches from your hosted API using a Spotify ID.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [SongMetadataEntity.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/database/core/SongMetadataEntity.kt)
- Add `canvasVideoUri: String?` (for local videos).
- Add `spotifyTrackId: String?` (to link with Spotify).

### [Networking]

#### [NEW] [SpotifyCanvasService.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/remote/SpotifyCanvasService.kt)
- Retrofit interface to call your hosted `Spotify-Canvas-API` instance.

### [Player & UI]

#### [NEW] [CanvasPlayerFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/player/styles/canvas/CanvasPlayerFragment.kt)
- A dedicated player style for Canvases.
- Uses **ExoPlayer** for high-performance looping.
- **Priority Logic**:
  1. If `canvasVideoUri` exists, play local video.
  2. Else if `spotifyTrackId` exists, fetch and play from Spotify API.
  3. Else, fallback to standard Vibrant background.

#### [MODIFY] [TagEditorFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/tageditor/TagEditorFragment.kt)
- Add UI to enter a **Spotify Track ID**.
- Add button to **Select Local Video**.

### [Resources]

#### [MODIFY] [strings.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/strings.xml)
- Add "Spotify Track ID", "Fetch Canvas", "Looping Background" strings.

## Verification Plan

### Automated Tests
- Unit test for `CanvasPriorityLogic` to ensure it picks the right source (Local vs Remote).

### Manual Verification
1. **Local Mode**: Pick a TikTok video for a song in Tag Editor -> Play song -> Verify looping.
2. **Spotify Mode**: Enter a Spotify ID (e.g., `3OHfY25tqY28d16oZczHc8`) -> Set your API URL in settings -> Play song -> Verify Spotify Canvas fetches and plays.
