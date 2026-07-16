# Implementation Plan - Redesign Library Settings Screen

Redesign the "Library" settings screen using modern Material 3 Expressive components, grouping relevant features, and using advanced interaction models like Split Buttons and Segmented Buttons.

## Proposed Changes

### 1. Data Model & State
#### [MODIFY] [SettingsUiState](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsViewModel.kt)
- Add missing fields for Library settings:
    - `trashMusicFiles` (Boolean)
    - `ignoreArticlesWhenSorting` (Boolean)
    - `enableHistoryPlaylist` (Boolean)
    - `historyInterval` (String)
    - `lastAddedInterval` (String)
    - `recursiveFolderActions` (Set<String>)
    - `minimumSongDuration` (Int)
    - `artistMinimumSongs` (Int)
    - `albumMinimumSongs` (Int)

#### [MODIFY] [SettingsViewModel](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsViewModel.kt)
- Initialize new fields in `loadInitialState()`.
- Add setter methods for each new field.

### 2. UI Components
#### [NEW] [LibrarySettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/LibrarySettingsComposeScreen.kt)
- Implement the new Library screen using `Scaffold` and `MediumTopAppBar`.
- **Grouping Strategy**:
    - **General**: Trash music files, Ignore articles (Segmented Group).
    - **Playlists**: "History & Suggestions" Split Button. Main switch for `enableHistoryPlaylist`, expanded for `historyInterval` and `lastAddedInterval` (Segmented Items).
    - **Folders**: "Folder Behaviors" Split Button. Main switch for `recursiveFolderActions` presence? Or just an Expressive Item.
    - **Exclusion & Filtering**:
        - Whitelist/Blacklist (Segmented Items with edit buttons).
        - Sliders for Min Song Duration, Artist Min Songs, Album Min Songs.

### 3. Integration
#### [MODIFY] [LibraryPreferencesFragment](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/PreferencesScreenFragment.kt)
- Replace XML-based `PreferenceFragmentCompat` implementation with `ComposeView`.
- Inject `SettingsViewModel` and host `LibrarySettingsComposeScreen`.

#### [MODIFY] [SettingsFragment](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsFragment.kt)
- Update `onDestinationChanged` to hide the XML toolbar for `nav_library_preferences`.

## Verification Plan

### Manual Verification
- Navigate to Settings -> Library.
- Verify the new UI layout and grouping.
- Test all toggles (Trash, Ignore Articles, History).
- Test Whitelist/Blacklist dialog triggers.
- Test all sliders.
- Verify Search functionality (it should find these keys if they are still in the XML or if I re-index).
