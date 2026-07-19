# Research: Auto-Update Setup for ProjectMusic

The goal is to provide a way for users (or testers) to receive updates "straight to the app" after each build.

## Current Infrastructure

### 1. In-App Updater (`github` flavor only)
- **Class**: `UpdateViewModel`, `GitHubService`
- **Logic**:
    - Fetches releases from GitHub repository using GitHub REST API.
    - Compares `latestRelease` version with current app version.
    - Downloads APK via `DownloadManager`.
    - Automatically checks on app startup (`MainActivity.prepareUpdateViewModel`).
- **Trigger**: Manually in Settings or on App Start.
- **Availability**: Controlled by `enable_builtin_updater` boolean resource (true for `github` flavor).

### 2. CI/CD (GitHub Actions)
- **`release.yml`**:
    - Triggered by tags (`v*`).
    - Builds `github`, `fdroid`, `playstore` flavors.
    - Creates a GitHub Release and uploads APKs.
- **`android.yml`**:
    - Triggered on every push.
    - Builds the app.
    - Sends APK to Telegram (if configured).
    - Uploads APK as a GitHub Action artifact.

## Gap Analysis

1. **Automation for "Each Build"**:
    - The `release.yml` only runs on tags.
    - The `android.yml` builds APKs but doesn't make them discoverable by the in-app updater (because it doesn't create a GitHub Release).
2. **"Straight to the App"**:
    - The app currently prompts the user. True "auto-update" (silent) is not supported on standard Android for non-Play Store apps.
    - Google Play In-App Update API could be used for the Play Store flavor, but it requires the app to be on the Play Store.

## Proposed Strategy

### Option A: Enhanced GitHub-based Updater (Recommended for non-Play Store)
1. **CI Change**: Update `android.yml` (or create a new one) to create a "Prerelease" or update a specific "Latest Dev" release on GitHub on every push to `main`.
2. **App Change**: Ensure the app checks for these "Prereleases". (The code already has an `allowExperimental` flag in `searchForUpdate`).
3. **User Setting**: Explain how users can enable "Experimental Updates" in settings to get these "each build" updates.

### Option B: Google Play In-App Updates (Standard for Play Store)
1. Explain how to integrate the Play Update Library.
2. This is "straight to the app" and highly automated but only for Play Store builds.

### Option C: Firebase App Distribution (Best for Testers)
1. Best for "after each build" distribution to a specific group.
2. Not "straight to the app" for general users, but has a tester app that handles updates.

## Recommendation for the User

I will explain:
1. How to use GitHub Actions to automate releases for "each build".
2. How to configure the app to pick up these "development" builds.
3. Mention Play Store In-App updates as the alternative for production.
