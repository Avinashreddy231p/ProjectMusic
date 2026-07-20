# Implementation Plan - Handle App Update Installation

The current implementation enqueues the APK download using `DownloadManager` but does not automatically trigger the installation once the download is complete. The user expects the app to handle the installation process.

## Proposed Changes

### 1. Permissions and Manifest
#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/AndroidManifest.xml)
- Add `android.permission.REQUEST_INSTALL_PACKAGES` to allow the app to trigger the package installer.
- Register a new `BroadcastReceiver` for `android.intent.action.DOWNLOAD_COMPLETE`.

### 2. Broadcast Receiver for Download Completion
#### [NEW] `UpdateDownloadReceiver.kt` in `com.mardous.projectmusic.ui.screen.update`
- Implement a `BroadcastReceiver` that listens for `ACTION_DOWNLOAD_COMPLETE`.
- Check if the completed download ID matches the `lastUpdateId` stored in `Preferences`.
- If it matches, retrieve the file URI from `DownloadManager`.
- Trigger the installation Intent using `FileProvider` to safely share the APK file with the system installer.

### 3. File Provider Configuration
#### [MODIFY] [provider_paths.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/xml/provider_paths.xml)
- Ensure the `Download` directory is accessible via `FileProvider`. (Current `external-path` might be sufficient, but adding a specific one for `Download` is safer).

## Verification Plan

### Automated Tests
- None possible for the full installation flow as it involves system dialogs.

### Manual Verification
1. Trigger an update check and click "Download".
2. Wait for the download to complete.
3. Verify that the system installation dialog appears automatically.
4. (Optional) Test on different Android versions to ensure `FileProvider` and `REQUEST_INSTALL_PACKAGES` are handled correctly.
