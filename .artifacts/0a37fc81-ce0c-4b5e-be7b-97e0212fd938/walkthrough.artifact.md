# Walkthrough: User-defined Lyrics API Keys

I have implemented a feature that allows users to provide their own API keys or client access tokens for Genius and Lyrically providers directly through the app settings.

## Changes Made

### 1. Settings UI Overhaul
- Converted **Genius** and **Lyrically** preference items to use the `SplitButtonPreference` component.
- Added an expandable "API Key" or "Client Access Token" setting inside each provider.
- Implemented an `InputDialog` that appears when clicking the key setting, allowing users to paste their tokens easily.
- Added real-time summary updates showing a masked version of the custom key when it's set.

### 2. Prioritized API Key Logic
- Updated `GeniusApi` and `LyricallyApi` (including `PaxsenixSearchHelper`) to check for a user-entered key first.
- If a user key is present, it is used for all network requests.
- If no user key is set, the app falls back to the built-in keys provided during build time.

### 3. Data & State Management
- Registered new preference keys in `NetworkFeature`.
- Added accessors in `Preferences.kt` and state handling in `SettingsViewModel`.
- Added new localized strings (English only for now) to support the new UI labels and hints.

## How to Test
1. Go to **Settings > Network**.
2. Look for the **Genius** or **Lyrically** providers in the **Lyrics Providers** section.
3. Use the dropdown arrow on the right to expand the item.
4. Click on **API Key** or **Client Access Token**.
5. Enter your token in the dialog and click **OK**.
6. Observe that the summary now says "Using custom key: [masked_key]".
7. Toggle the provider switch to ensure it remains enabled/disabled as expected.

render_diffs(file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/NetworkSettingsComposeScreen.kt)
render_diffs(file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/api/genius/GeniusApi.kt)
render_diffs(file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/api/lyrically/LyricallyApi.kt)
render_diffs(file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/shared/java/com/mardous/booming/data/remote/lyrics/api/lyrically/PaxsenixSearchHelper.kt)
