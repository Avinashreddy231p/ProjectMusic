# Walkthrough - Change Build to GitHub Flavor

The build system has been simplified to only use the "GitHub" configuration. The `fdroid` and `playstore` flavors have been removed, and the project now builds a single full-featured version by default.

## Changes Made

### Build Configuration
#### [build.gradle.kts](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/build.gradle.kts)
- Removed `flavorDimensions` and `productFlavors`.
- Moved `resValue` settings (network features, updater, lyrically provider) to `defaultConfig`.
- Simplified `sourceSets` by removing flavor-specific blocks.
- Updated APK naming convention to `ProjectMusic-{version}-{abi}.apk`.

### Source Code Updates
#### [PaxsenixSearchHelper.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/remote/lyrics/api/lyrically/PaxsenixSearchHelper.kt)
- Moved the full implementation of `PaxsenixSearchHelper` from the `shared` source set to the `main` source set. This ensures all network-based lyrics features are available in the default build.

#### [App.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/App.kt)
- Updated `isFDroidBuild()` and `isPlayStoreBuild()` to return `false` as these flavors no longer exist.

#### [GitHubRelease.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/remote/github/model/GitHubRelease.kt)
- Updated `isApk` logic to remove the dependency on `BuildConfig.FLAVOR`.

#### [AboutScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/about/AboutScreen.kt)
- Removed the flavor-specific version name suffix for F-Droid.

### CI/CD Automation
#### [release.yml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/.github/workflows/release.yml)
- Updated the Gradle task to `assembleRelease`.
- Adjusted artifact paths and Telegram notification logic to match the new APK naming and directory structure.

## Verification Results

### Automated Tests
- ✅ `./gradlew assembleDebug` - Build successful.
- ✅ `./gradlew assembleRelease` - Build successful.

> [!NOTE]
> The directories `app/src/shared` and `app/src/playstore` are no longer used by the build system. You can safely delete them if you wish to clean up the project structure further.
