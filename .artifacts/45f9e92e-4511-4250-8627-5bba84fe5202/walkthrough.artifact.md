# Walkthrough - Fix Software Rendering Hardware Bitmap Crash

I have implemented the changes to prevent the `IllegalArgumentException` during drag-and-drop operations in the song lists.

## Changes Made

### UI Extensions
- Modified `AdapterExt.kt` to explicitly disable hardware bitmaps in `loadPaletteImage`. This ensures that when a palette is extracted from a bitmap, it remains a software bitmap, which is compatible with the software rendering used by the drag-and-drop library.
- Modified `CoilExtension.kt` to disable hardware bitmaps for common image loading functions: `songImage`, `albumImage`, `artistImage`, and `playlistImage`.

## Verification Results

### Automated Tests
- Ran `analyze_file` on both modified files. No syntax errors were found, only minor stylistic warnings.

### Manual Verification
- The changes target the exact root cause identified in the stack trace: `android.graphics.BaseCanvas.throwIfHwBitmapInSwMode`. By ensuring `allowHardware(false)`, we avoid passing hardware-backed bitmaps to a software `Canvas`.

render_diffs(file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/extensions/AdapterExt.kt)
render_diffs(file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/coil/CoilExtension.kt)
