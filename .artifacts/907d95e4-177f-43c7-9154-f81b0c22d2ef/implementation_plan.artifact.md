# Implementation Plan - Global High Text Contrast Enforcement

The user wants to ensure high contrast for all text in the application, specifically overriding the lyrics accent color when contrast is low. This plan outlines enhancing our color utilities and applying them to lyrics and theme generation.

## User Review Required

> [!IMPORTANT]
> This change will automatically adjust user-selected accent colors if they result in poor readability (low contrast). The adjustment will maintain the hue but change lightness/saturation to meet WCAG AA standards (4.5:1 contrast ratio).

## Proposed Changes

### Core Color Utilities

#### [MODIFY] [ColorExt.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/extensions/resources/ColorExt.kt)
- Enhance `ensureContrastAgainst` by porting the `findContrastColor` logic from `NotificationColorUtil`.
- Use a default `minContrastRatio` of 4.5 (WCAG AA) for text.

### Lyrics Components

#### [MODIFY] [LyricsScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/lyrics/LyricsScreen.kt)
- Update `LyricsSurface` to apply `ensureContrastAgainst` to the `finalContentColor`.
- For background effects (Blur/Gradient), use the enhanced contrast check against the specific background luminance instead of just hard-switching to White/Black.

### Theme Engine

#### [MODIFY] [EraThemeEngine.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/core/model/theme/EraThemeEngine.kt)
- Apply a "contrast safety pass" to the generated `ColorScheme`.
- Ensure `primary`, `secondary`, `tertiary` have sufficient contrast against `surface` or `background` when used for text roles.

## Verification Plan

### Manual Verification
- Set the lyrics accent color to a color very similar to the background (e.g., light grey on white).
- Verify the lyrics text automatically darkens/lightens to remain legible.
- Switch themes (Light/Dark/Black) and verify contrast is maintained everywhere.
- Test both Spotify-style and standard lyrics screens.
