# Walkthrough - Separate Background Options for Vibrant Lyrics

I have implemented independent background mode settings for the **Vibrant Full View Lyrics** screen and fixed the reactivity issues to ensure changes take effect immediately.

## Changes Made

### Core & Preferences
- Added `VIBRANT_LYRICS_BACKGROUND_MODE` to [Preferences.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/util/Preferences.kt).
- Created a new preference property `vibrantLyricsBackgroundMode` with a default value of `Fluid`.
- Added a new resource ID `action_vibrant_lyrics_background` in [ids.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/ids.xml) for the quick-switch menu.

### Reactivity & State Management
- **Immediate Updates**: Added `vibrantBackgroundMode` to `LyricsViewSettings` in [LyricsViewSettings.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/core/model/lyrics/LyricsViewSettings.kt).
- **Preference Observation**: Updated [LyricsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/lyrics/LyricsViewModel.kt) to observe changes to `VIBRANT_LYRICS_BACKGROUND_MODE`. When the mode is changed (via settings or quick-menu), the ViewModel now refreshes the settings state, triggering an immediate UI recomposition.

### Settings UI
- Updated [SettingsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsViewModel.kt) to manage the new `vibrantLyricsBackgroundMode` state.
- Added a configuration option in [SettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsComposeScreen.kt) under **Lyrics Experience** -> **Background Effects**.

### Lyrics View
- Modified [VibrantLyricsScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/lyrics/VibrantLyricsScreen.kt) to render a full-screen `VibrantBackground` layer using the reactive mode from `lyricsViewSettings`.

### Player Integration
- Updated [VibrantPlayerFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/player/styles/vibrantstyle/VibrantPlayerFragment.kt) to include a **"Lyrics Background"** option in the overflow menu when the lyrics view is active.

## Verification Results

> [!NOTE]
> The background mode now updates **instantly** when selected from the quick-switch menu or settings, without needing to reload the lyrics screen.

- **Preference Persistence**: Verified that changing the background mode in Settings or via the quick-menu correctly updates the UI and persists across app restarts.
- **State Reactivity**: Confirmed that the `VibrantBackground` responds immediately to state changes from the `LyricsViewModel`.
