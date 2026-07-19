# Implementation Plan - Enhanced Seek Bar Customization & Dynamic Coloring

This plan covers implementing dynamic coloring for seek bars based on album art and enhancing seek bar customization options, including thumb size and separated visual/thumb styles.

## User Review Required

> [!IMPORTANT]
> The dynamic coloring will now be applied consistently across all player styles. The vibrant album art color will be used for both the play button background and the seek bar progress.
>
> A new "Thumb Size" setting will be added to the Seek Bar Customization menu.

## Proposed Changes

### [Core Logic & Preferences]

#### [MODIFY] [Preferences.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/util/Preferences.kt)
- Add `THUMB_SIZE` constant and property (float, default 1.0f).
- Ensure existing styles like `ProgressBarStyle`, `ThumbStyle`, and `ProgressControlStyle` are fully utilized.

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/SettingsViewModel.kt)
- Add `thumbSize` to `SettingsUiState`.
- Implement `setThumbSize(scale: Float)` in `SettingsViewModel`.
- Update `loadInitialState` to read the new preference.

### [Player UI Components - Dynamic Coloring]

#### [MODIFY] [MiniPlayerFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/other/MiniPlayerFragment.kt)
- Observe `playerViewModel.colorSchemeFlow`.
- Dynamically update `progressBar` and `bottomProgressBar` indicator colors using `scheme.primaryColor`.
- For Spotify theme, replace hardcoded green with the dynamic album color.

#### [MODIFY] [DefaultPlayerControlsFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/player/styles/defaultstyle/DefaultPlayerControlsFragment.kt) & [PlainPlayerControlsFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/player/styles/plainstyle/PlainPlayerControlsFragment.kt)
- In `getTintTargets`, consistently use `scheme.primaryColor` for `newEmphasisColor` to match play button and lyrics background.

#### [MODIFY] [FullCoverPlayerControlsFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/player/styles/fullcoverstyle/FullCoverPlayerControlsFragment.kt)
- Update `getTintTargets` to use `scheme.primaryColor` for the play button background and seek bar.

### [Seek Bar Design & Customization]

#### [MODIFY] [MusicThumbDrawable.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/component/views/MusicThumbDrawable.kt)
- Update `draw` methods to scale shapes based on a scale factor (derived from the new `thumbSize` preference).

#### [MODIFY] [MusicSlider.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/component/views/MusicSlider.kt)
- Update `inflateSliderView` to apply the `Preferences.thumbSize` scale when setting up `MusicThumbDrawable`.
- Listen for `Preferences.THUMB_SIZE` changes to refresh the UI.

#### [MODIFY] [NowPlayingSlider.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/component/compose/player/NowPlayingSlider.kt)
- Add `thumbSize: Float` parameter to `NowPlayingSlider` and `StyleThumb`.
- Scale dimensions (width, height, size) in `StyleThumb` by the `thumbSize` factor.

#### [MODIFY] [NowPlayingSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/NowPlayingSettingsComposeScreen.kt)
- In `SeekBarCustomizationBottomSheet`, add an `ExpressiveSliderItem` for "Thumb Size" (range 0.5 to 2.0).
- Re-organize sections to clearly separate "Visual Style", "Thumb Style", "Thumb Size", and "Control Mode".

## Verification Plan

### Manual Verification
1.  **Dynamic Coloring**: Play tracks with distinct album colors. Verify seek bar matches play button in all styles (Default, Spotify, Vibrant, etc.) and in the Mini Player.
2.  **Thumb Customization**: Open Settings -> Now Playing -> Seek Bar Customization.
    - Change **Visual Style** (e.g., Wavy, Rounded, Glow) and verify immediate update.
    - Change **Thumb Style** (e.g., Pill, Diamond, Glow) and verify immediate update.
    - Adjust **Thumb Size** slider and verify the thumb scales correctly in the player.
3.  **Contrast**: Check that the vibrant color remains legible against various backgrounds (Light/Dark/Vibrant).
