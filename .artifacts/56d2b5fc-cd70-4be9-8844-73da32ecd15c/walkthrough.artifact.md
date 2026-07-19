# Walkthrough - Fix Argument Type Mismatch in SongRepository

I have resolved the compilation error where `volumeName` (a `String?`) was being passed as a positional argument to the `Song` constructor instead of the expected `Long` (statistical fields).

## Changes Made

### app component

#### [SongRepository.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/repository/SongRepository.kt)

Modified the `Song` constructor call in `getSongFromCursorImpl` to use named arguments for the trailing parameters. This allows the compiler to correctly skip the intermediate statistical fields that have default values.

```diff
-            genreName,
-            volumeName,
-            composer,
-            bitrate
+            genreName,
+            volumeName = volumeName,
+            composer = composer,
+            bitrate = bitrate
```

## Verification Results

### Automated Tests
- Executed `./gradlew :app:compileFdroidDebugKotlin` which now finishes successfully.

```
{
  "status": "Build finished successfully."
}
```
