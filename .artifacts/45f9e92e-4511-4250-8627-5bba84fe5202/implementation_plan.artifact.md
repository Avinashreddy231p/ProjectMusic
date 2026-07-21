# Fix Crash: Software rendering doesn't support hardware bitmaps

The user reported a crash `java.lang.IllegalArgumentException: Software rendering doesn't support hardware bitmaps` when dragging items in a `RecyclerView`. This occurs because the `advrecyclerview` library uses software rendering to create dragging item images, while some `ImageView`s (likely `ShapeableImageView`) contain hardware-accelerated bitmaps loaded by Coil.

## Proposed Changes

We will explicitly disable hardware bitmaps for image requests that load into `ImageView`s used in lists, especially those supporting drag-and-drop. Although a global setting exists in `App.kt`, adding it explicitly ensures it's applied even if the singleton loader is bypassed or misconfigured for specific requests.

### [Component: UI Extensions]

#### [MODIFY] [AdapterExt.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/extensions/AdapterExt.kt)
- Add `allowHardware(false)` to `loadPaletteImage` request.

#### [MODIFY] [CoilExtension.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/coil/CoilExtension.kt)
- Add `allowHardware(false)` to `songImage`, `albumImage`, `artistImage`, and `playlistImage` requests.

## Verification Plan

### Manual Verification
- Deploy the app to a device or emulator.
- Go to the Playing Queue or a Playlist.
- Attempt to drag a song item.
- Verify that the app no longer crashes with `IllegalArgumentException`.
