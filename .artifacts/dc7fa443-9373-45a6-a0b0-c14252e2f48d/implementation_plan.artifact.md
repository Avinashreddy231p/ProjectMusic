# Implementation Plan - Redesign Advanced Settings Screen

Redesign the "Advanced" settings screen using modern Material 3 Expressive components, grouping relevant features, and using advanced interaction models like Split Buttons and Segmented Buttons.

## Proposed Changes

### 1. Data Model & State
#### [MODIFY] [SettingsUiState](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsViewModel.kt)
- Add missing fields for Advanced settings:
    - `language_name` (String)
    - `enable_rotation_lock` (Boolean)
    - `pause_on_zero_volume` (Boolean)
    - `mp3_index_seeking` (Boolean)
    - `stop_when_closed_from_recents` (Boolean)
    - `ignore_media_store` (Boolean)
    - `use_folder_art` (Boolean)

#### [MODIFY] [SettingsViewModel](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsViewModel.kt)
- Initialize new fields in `loadInitialState()`.
- Add setter methods for each new field.
- Implement logic for Language change (AppLocales) and Rotation lock.

### 2. UI Components
#### [NEW] [AdvancedSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/AdvancedSettingsComposeScreen.kt)
- Implement the new Advanced screen using `CollapsibleAppBarScaffold` and a Search Bar.
- Use `ExpressivePreferenceItem`, `SplitButtonPreference`, and `SegmentedPreferenceGroup` from the internal library.
- **Grouping Strategy**:
    - **Localization & System**: Language (Expressive Item), Rotation Lock (Segmented Item).
    - **Data Management**: Backup & Restore (Using a Segmented Button row or dedicated buttons).
    - **Engine Settings**:
        - "Playback Engine" Split Button: Main switch for `mp3_index_seeking`, expanded for `pause_on_zero_volume` and `stop_when_closed_from_recents`.
        - "Library Engine" Split Button: Main switch for `ignore_media_store`, expanded for `use_folder_art`.
    - **App Updates**: Update search mode (Segmented Button), "Check for updates" (Expressive Item).

### 3. Integration
#### [MODIFY] [AdvancedPreferencesFragment](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/PreferencesScreenFragment.kt)
- Replace XML-based `PreferenceFragmentCompat` implementation with `ComposeView`.
- Inject `SettingsViewModel` and host `AdvancedSettingsComposeScreen`.

## Verification Plan

### Manual Verification
- Navigate to Settings -> Advanced.
- Verify the new UI layout and grouping.
- Test Language selection.
- Test Backup and Restore actions.
- Test all toggles (Rotation Lock, Engine settings).
- Test Update frequency selection and "Check for updates" button.
- Verify Search functionality within the Advanced screen.
