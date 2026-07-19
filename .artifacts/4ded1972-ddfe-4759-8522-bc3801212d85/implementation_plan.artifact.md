# AI Lab and Stem Separation Implementation Plan

This plan outlines the implementation of the "AI Lab" feature, focusing on on-device Stem Separation (AI Mixer) as a standout experimental tool.

## User Review Required

> [!IMPORTANT]
> **Performance & Storage**: On-device stem separation is resource-intensive. Processing can take 20-90 seconds depending on the device. Stems will also consume significant storage (~5x the original file). A cache management system is crucial.

> [!NOTE]
> **Architecture**: We will use a background processing approach. Real-time separation is not feasible. The app will "prepare" stems in the background and then allow instant "AI Mixer" playback.

## Proposed Changes

### 1. AI Lab UI Foundation
We will create a central hub for all AI-powered features.

#### [NEW] [AILabFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/ailab/AILabFragment.kt)
A Compose-based fragment listing:
- AI Mixer (Stem Separation)
- Smart Playlist Generator (Placeholder)
- Mood Detection (Integration with existing Moods)
- AI Lyrics Search (Placeholder)

#### [MODIFY] [menu_now_playing.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/menu/menu_now_playing.xml)
Add an "AI Tools" menu item.

### 2. Stem Separation Engine (Architecture)
#### [NEW] [StemSeparationWorker.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/worker/StemSeparationWorker.kt)
A `WorkManager` worker that handles the background processing of audio files.
*   **Stage 1**: Decoding the original file to PCM.
*   **Stage 2**: Running the AI model (Initially a placeholder for architectural verification).
*   **Stage 3**: Encoding stems into the local cache.

#### [NEW] [StemCacheManager.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/cache/StemCacheManager.kt)
Manages the storage of separated stems in `context.cacheDir/stems/`. Provides methods to check if stems exist for a given `songId` and to delete them.

### 3. AI Mixer UI
#### [NEW] [AIMixerDialog.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/component/ailab/AIMixerDialog.kt)
A bottom sheet or dialog appearing from the Now Playing screen.
*   Displays vertical/horizontal sliders for:
    *   **Vocals**
    *   **Drums**
    *   **Bass**
    *   **Guitar** (New)
    *   **Piano** (New)
    *   **Other** (Strings, Synths, etc.)
*   "Process" button to start the background separation if not already cached.
*   "Reset" button to restore original balance.

### 4. Playback Integration
#### [MODIFY] [PlaybackService.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/playback/PlaybackService.kt)
Extend the player logic to support `MergingMediaSource` when stems are available. This allows synchronized playback of multiple audio files with individual volume control.

## Verification Plan

### Automated Tests
- `StemCacheManagerTest`: Verify correct storage and cleanup of stem files.
- `StemSeparationWorkerTest`: Verify the background job triggers correctly and updates the database/cache state.

### Manual Verification
1.  Open Now Playing screen.
2.  Tap "AI Tools" -> "AI Mixer".
3.  Observe the "Processing..." state (initially using a mock delay).
4.  Once processed, adjust sliders and verify the volume changes for each stem (verified via logcat or mock mixing logic).
5.  Check "Storage Settings" to see the impact of generated stems.
