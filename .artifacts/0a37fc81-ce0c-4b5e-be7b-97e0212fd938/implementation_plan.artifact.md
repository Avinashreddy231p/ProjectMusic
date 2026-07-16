# Implementation Plan: User-defined Lyrics API Keys

Allow users to enter their own API keys or client access tokens for Genius and Lyrically providers via the settings UI using `SplitButtonPreference` components.

## User Review Required

> [!IMPORTANT]
> - User-entered keys will be stored in `SharedPreferences` and will take precedence over the keys provided in `BuildConfig`.
> - The UI will use `SplitButtonPreference` which allows toggling the provider while also providing an expandable section for API key configuration.

## Proposed Changes

### [Component: Preferences & Data]

#### [MODIFY] [NetworkFeature.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/model/network/NetworkFeature.kt)
- Add constants for user API keys: `GENIUS_API_KEY_KEY` and `LYRICALLY_API_KEY_KEY`.

#### [MODIFY] [Preferences.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/util/Preferences.kt)
- Add `geniusApiKey` and `lyricallyApiKey` properties with getters and setters.

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsViewModel.kt)
- Update `SettingsUiState` to include `geniusApiKey` and `lyricallyApiKey`.
- Update `loadInitialState` to load these keys.
- Add `setGeniusApiKey` and `setLyricallyApiKey` methods.

---

### [Component: Lyrics APIs]

#### [MODIFY] [GeniusApi.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/api/genius/GeniusApi.kt)
- Prioritize user-entered API key from `Preferences` over `BuildConfig.GENIUS_API_KEY`.

#### [MODIFY] [LyricallyApi.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/api/lyrically/LyricallyApi.kt)
- Update `paxsenix` extension to prioritize user-entered API key.

#### [MODIFY] [PaxsenixSearchHelper.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/shared/java/com/mardous/booming/data/remote/lyrics/api/lyrically/PaxsenixSearchHelper.kt)
- Prioritize user-entered API key in `getMusixmatchSearchResponse`.

---

### [Component: UI]

#### [MODIFY] [NetworkSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/NetworkSettingsComposeScreen.kt)
- Replace `SegmentedPreferenceItem` for Genius and Lyrically with `SplitButtonPreference`.
- Add `InputDialog` for entering the API keys.
- Update strings to support labels and hints for API keys.

---

## Verification Plan

### Automated Tests
- Build the project to ensure no regressions in settings UI.

### Manual Verification
- Go to Network Settings.
- Expand Genius or Lyrically item using the split button.
- Click on the API Key item to open the input dialog.
- Enter a key and verify it persists (reopen settings).
- Toggle the provider and verify it persists.
- (Optional) Use a proxy or logs to verify the correct API key is being sent in requests.
