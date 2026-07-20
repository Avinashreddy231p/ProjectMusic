# Implementation Plan - Remove Play Store and F-Droid Flavors

The project has already had the `playstore` and `fdroid` product flavors removed from the build system, but several artifacts, source directories, and code references remain. This plan aims to complete the cleanup by removing these leftovers.

## User Review Required

> [!IMPORTANT]
> This will permanently delete the `app/src/playstore` and `app/src/shared` directories. Any unique code in these directories (though none was found during research) will be lost.

## Proposed Changes

### Build Configuration & Properties

#### [MODIFY] [public.properties](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/public.properties)
- Remove all `fdroid.*` and `playstore.*` property overrides.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/build.gradle.kts)
- (Optional) Simplify property loading logic if desired, though the current `loadFlavorProperties` still works for default values.

### Source Code Cleanup

#### [MODIFY] [App.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/App.kt)
- Remove `isFDroidBuild()` and `isPlayStoreBuild()` methods from the `companion object`.

#### [MODIFY] [AboutScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/about/AboutScreen.kt)
- Remove the `if (!App.isPlayStoreBuild())` check around the donation button, as it is now always shown.

### File Deletion

#### [DELETE] `app/src/playstore`
- Remove the entire directory.

#### [DELETE] `app/src/shared`
- Remove the entire directory (confirmed redundant with `app/src/main`).

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project still builds correctly without flavor-specific sources.

### Manual Verification
- Check the "About" screen in the app to ensure the Donate button is visible (if possible to run).
- Verify that `BuildConfig` no longer contains flavor-specific fields (other than defaults).
