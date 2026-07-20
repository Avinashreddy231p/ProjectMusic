# Task List - Manual App Update Flow

- [x] Update UI Layout
    - [x] Add `LinearProgressIndicator` to `dialog_update_info.xml`
- [x] Refactor `UpdateViewModel`
    - [x] Add `DownloadStatus` sealed class
    - [x] Implement download progress polling
    - [x] Implement `installUpdate` logic with permission handling
- [x] Update `UpdateDialog`
    - [x] Observe `downloadStatus`
    - [x] Update button text, icon, and progress bar visibility
    - [x] Handle button clicks for Download and Install states
- [x] Update `UpdateDownloadReceiver`
    - [x] Remove automatic installation logic
- [x] Verification
    - [x] Verify compilation
