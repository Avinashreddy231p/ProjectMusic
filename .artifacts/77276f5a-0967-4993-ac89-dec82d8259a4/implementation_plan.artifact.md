# Implementation Plan - Fix Hardware Bitmap Crash during Drag & Drop

The user reported a crash `java.lang.IllegalArgumentException: Software rendering doesn't support hardware bitmaps` when dragging items in a `RecyclerView`. This occurs because the `advrecyclerview` library uses software rendering to create a dragging item image, but the `ImageView`s in the items contain hardware-accelerated bitmaps loaded by Coil.

## Proposed Changes

### [App Configuration]

#### [MODIFY] [App.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/App.kt)

- Change global Coil `ImageLoader` configuration to disable hardware bitmaps. While hardware bitmaps are memory-efficient, they are incompatible with software-rendered canvases used by some legacy view-based libraries and custom drawing logic (like the one in `advrecyclerview`).

### [Coil Extensions] (Optional but recommended for consistency)

#### [MODIFY] [AdapterExt.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/extensions/AdapterExt.kt)

- Explicitly add `allowHardware(false)` to `loadPaletteImage` if needed, although the global change in `App.kt` should cover it.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to a playlist or the playing queue.
- Attempt to drag an item to reorder it.
- Verify that the app no longer crashes and the dragging image appears correctly.
