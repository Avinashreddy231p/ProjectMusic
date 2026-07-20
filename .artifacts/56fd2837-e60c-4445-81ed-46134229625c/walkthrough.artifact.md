# Walkthrough - Manual App Update Flow

I have implemented a manual app update flow that gives users more control over the download and installation process.

## Changes Made

### 1. Enhanced UI with Progress Tracking
- Added a `LinearProgressIndicator` to the [update dialog layout](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/layout/dialog_update_info.xml) to visualize the download progress.
- Updated the [UpdateDialog](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/update/UpdateDialog.kt) to observe real-time download status.

### 2. State-Driven Update Logic
- Refactored [UpdateViewModel](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/update/UpdateViewModel.kt) to include a `DownloadStatus` state machine:
    - `Idle`: Shows the "Download" button.
    - `Downloading`: Displays progress percentage and disables the button.
    - `Completed`: Changes the button to "Install".
    - `Failed`: Shows an error and allows retrying.
- Implemented background polling for `DownloadManager` progress.

### 3. Secure Manual Installation
- Added `installUpdate` method in the ViewModel which:
    - Verifies `REQUEST_INSTALL_PACKAGES` permission on Android 8.0+.
    - Redirects the user to system settings if the permission is missing.
    - Triggers the system package installer once ready.

### 4. Safety Improvements
- Disabled automatic installation in the [UpdateDownloadReceiver](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/update/UpdateDownloadReceiver.kt) to ensure the user must explicitly click "Install".

## Verification Results

### Automated Tests
- Verified project compilation by running `:app:assembleDebug`.

### Manual Testing Steps (Recommended)
1. Trigger an update check.
2. Click **Download** and observe the progress bar and button text updating.
3. Once the download finishes, click **Install**.
4. If prompted, grant "Install unknown apps" permission and verify the installer launches.
