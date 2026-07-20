# Implementation Plan - Manual App Update Flow

This plan outlines the changes required to implement a manual app update flow where the user can download an update and then explicitly click an "Install" button.

## User Review Required

> [!IMPORTANT]
> The app will require `REQUEST_INSTALL_PACKAGES` permission to install updates on Android 8.0 (API 26) and above. If this permission is missing, the app will redirect the user to the "Install unknown apps" settings page.

## Proposed Changes

### UI Components

#### [MODIFY] [dialog_update_info.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/layout/dialog_update_info.xml)
- Add a `LinearProgressIndicator` to show download progress.
- Initial state of progress bar will be `GONE`.

#### [MODIFY] [UpdateDialog.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/update/UpdateDialog.kt)
- Observe `downloadStatus` from `UpdateViewModel`.
- Update button text and icon based on status:
    - `Idle`: "Download" (current behavior)
    - `Downloading`: "Downloading (X%)"
    - `Completed`: "Install"
- Handle `downloadAction` click:
    - If `Idle`, call `viewModel.downloadUpdate()`.
    - If `Completed`, call `viewModel.installUpdate()`.
- Show/hide and update the progress bar.
- Prevent dialog dismissal when "Download" is clicked.

### Logic & Services

#### [MODIFY] [UpdateViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/update/UpdateViewModel.kt)
- Add `DownloadStatus` sealed class:
    - `Idle`
    - `Downloading(progress: Int)`
    - `Completed(downloadId: Long)`
    - `Failed(error: String?)`
- Add `downloadStatus` LiveData.
- Modify `downloadUpdate` to start a polling job that queries `DownloadManager` for progress.
- Add `installUpdate(context: Context)`:
    - Verify `REQUEST_INSTALL_PACKAGES` permission (for API 26+).
    - If missing, launch `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`.
    - If granted, trigger the APK installation intent.

#### [MODIFY] [UpdateDownloadReceiver.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/update/UpdateDownloadReceiver.kt)
- Remove automatic installation from `onReceive`.
- (Optional) Use it to trigger a final status update in the `ViewModel` if the polling job missed the last state.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify compilation.

### Manual Verification
1. Open **Settings** -> **Network**.
2. Click **Search for updates**.
3. Verify the update dialog shows up.
4. Click **Download**.
5. Observe the button changing to **Downloading...** with progress.
6. Verify the button changes to **Install** once finished.
7. Click **Install** and verify it prompts for permission or starts installation.
