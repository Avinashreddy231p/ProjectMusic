# Implement "Aurora" Background Effect

The goal is to implement a new background effect for the "Now Playing" screen that follows the multi-layered approach shown in the user-provided breakdown (Step 1-9). This effect will be called "Aurora" and will be available as a new background style.

## Proposed Changes

### Core Model

#### [MODIFY] [VibrantBackgroundMode.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/core/model/theme/VibrantBackgroundMode.kt)
- Added `Aurora` to the `VibrantBackgroundMode` enum.

#### [MODIFY] [NowPlayingScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/core/model/theme/NowPlayingScreen.kt)
- Added `Aurora` to the `NowPlayingScreen` enum to allow it to be selected as a player style.
- Updated `when` expressions to handle the new `Aurora` style.

### Resources

#### [MODIFY] [strings.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/strings.xml)
- Added `aurora_style` string resource.

### UI Components

#### [MODIFY] [AnimatedBackground.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/component/compose/decoration/AnimatedBackground.kt)
- Implemented `auroraBackground` modifier that follows the 9-step breakdown:
    1. **Mesh Gradient**: Implemented using multiple moving radial gradient blobs with derived colors.
    2. **Gaussian Blur**: Applied using the `.blur()` modifier on the box.
    3. **Radial Glow**: Added a central glow layer.
    4. **Aurora Gradient**: Implemented a wavy ribbon path across the middle using `drawPath` with a vertical gradient.
    5. **Black Overlay**: Added a vignette and vertical dimming layer for contrast.
    6. **Noise Texture**: Added a fine white noise grain layer.

#### [MODIFY] [VibrantBackground.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/component/compose/decoration/VibrantBackground.kt)
- Updated `VibrantBackground` composable to support the new `VibrantBackgroundMode.Aurora`.

#### [MODIFY] [VibrantPlayerFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/player/styles/vibrantstyle/VibrantPlayerFragment.kt)
- Updated `setupBackground` to use `VibrantBackgroundMode.Aurora` when `NowPlayingScreen.Aurora` is selected.

## Verification Plan

### Manual Verification
- Deploy the app to a device/emulator.
- Open the Now Playing screen.
- Change the "Now Playing Style" in settings to "Aurora".
- Verify that the background effect matches the "Dreamer" style breakdown:
    - Moving colored blobs (Mesh).
    - Soft blurred appearance.
    - Central glow.
    - Wavy aurora ribbon.
    - Noise grain.
    - Vignette for UI clarity.
- Verify that the background reacts to playback (animations) and beats (pulsing).
