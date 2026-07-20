# Implementation Plan - Change Build to GitHub Flavor

The user wants to simplify the build system to only use the "GitHub" flavor configuration. Currently, the project has three flavors: `github`, `fdroid`, and `playstore`. The `github` flavor is the most feature-complete, including network features, a built-in updater, and the Lyrically provider.

## Proposed Changes

### Build Configuration
#### [MODIFY] [build.gradle.kts](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/build.gradle.kts)
- Remove `flavorDimensions` and `productFlavors` blocks.
- Move `resValue` entries from the `github` flavor to `defaultConfig`.
- Simplify `sourceSets` by removing flavor-specific named blocks.
- Update `androidComponents` logic to remove flavor-based property loading and naming.
- Update APK naming convention to reflect the single build type.

### Source Code Organization
#### [MOVE] `app/src/shared/java` content to `app/src/main/java`
- Move `com.mardous.projectmusic.data.remote.lyrics.api.lyrically.PaxsenixSearchHelper` to the main source set.
#### [DELETE] `app/src/shared`
#### [DELETE] `app/src/playstore`
- Remove the stubbed version of `PaxsenixSearchHelper`.

### CI/CD Automation
#### [MODIFY] [release.yml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/.github/workflows/release.yml)
- Update Gradle tasks from `assembleGithubRelease` to `assembleRelease`.
- Update artifact paths and naming in the workflow.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleRelease` to ensure the build completes successfully.
- Run `./gradlew assembleDebug` to verify local development build.

### Manual Verification
- Verify that `BuildConfig.GITHUB_API_URL` and other properties are correctly populated from `public.properties`.
- Check that the generated APK name follows the new simplified format.
