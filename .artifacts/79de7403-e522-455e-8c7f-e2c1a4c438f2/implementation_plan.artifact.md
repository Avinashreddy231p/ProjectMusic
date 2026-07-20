# Implementation Plan - Fix Update Detection for Rolling Releases

The user reported that the "Check for updates" feature is not showing updates even after pushing new changes to GitHub. This plan addresses the flaw in how the app determines the most recent release and whether it's "newer" than the installed version, especially for rolling releases like `latest-ci` (Continuous Builds).

## User Review Required

> [!IMPORTANT]
> **Experimental Updates Toggle**: For the app to detect `latest-ci` (Continuous Builds) or any pre-release, the user MUST have "Experimental Updates" enabled in the Advanced Settings, UNLESS they are running a build that was itself created by the CI (where `IS_CI_BUILD` is true).

> [!NOTE]
> **Debug Builds**: Debug builds (built locally via Android Studio) do not have `IS_CI_BUILD` enabled by default. If you are testing this on a debug build, please ensure you enable "Experimental Updates" in the app settings.

## Proposed Changes

### [Component Name]

#### [MODIFY] [GitHubRelease.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/remote/github/model/GitHubRelease.kt)
- Add `updated_at` field to `ReleaseAsset` to track when APKs are actually uploaded.
- Add `updated_at` field to `GitHubRelease` (optional but good for consistency).
- Implement a `lastUpdatedAt` property that returns the maximum of `published_at` and all APK assets' `updated_at`.
- Update `isNewer` for `latest-ci` to use `lastUpdatedAt` instead of `publishedAt`.

#### [MODIFY] [GitHubService.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/remote/github/GitHubService.kt)
- Update `latestRelease` logic to use `lastUpdatedAt` when determining the `mostRecent` release from the list of all releases. This ensures rolling releases (like `latest-ci`) are correctly identified as the latest if they were updated recently, even if their original publication date is old.

## Verification Plan

### Automated Tests
- I will verify the code compiles by running a build (optional, as it's a small change).
- I can add a unit test for `GitHubRelease.isNewer` with mocked timestamps.

### Manual Verification
- The user should test this by:
    1. Ensuring "Experimental Updates" is enabled (if on a local/debug build).
    2. Pushing a change to GitHub.
    3. Waiting for the CI to finish updating the `latest-ci` release.
    4. Clicking "Check for updates" in the app.
