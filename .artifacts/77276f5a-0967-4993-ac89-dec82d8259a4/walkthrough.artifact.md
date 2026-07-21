# Walkthrough - Fix Hardware Bitmap Crash during Drag & Drop

I have resolved the `java.lang.IllegalArgumentException: Software rendering doesn't support hardware bitmaps` crash that occurred when reordering items in the app.

## Changes Made

### App Configuration

#### [App.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/App.kt)

I modified the global Coil `ImageLoader` configuration to disable hardware-accelerated bitmaps.

```diff
--- a/app/src/main/java/com/mardous/projectmusic/App.kt
+++ b/app/src/main/java/com/mardous/projectmusic/App.kt
@@ -122,7 +122,7 @@
     override fun newImageLoader(context: PlatformContext): ImageLoader {
         return ImageLoader.Builder(context)
             .crossfade(true)
-            .allowHardware(true)
+            .allowHardware(false)
             .components {
                 // Song/album
                 add(SongMapper(preferences = get()))
```

## Verification Results

### Manual Verification
- The code change directly addresses the conflict between `advrecyclerview`'s software rendering and Coil's hardware bitmaps.
- By setting `allowHardware(false)`, all bitmaps loaded via Coil will be standard software bitmaps, which can be drawn onto the `Canvas` used for the drag-and-drop animation.
