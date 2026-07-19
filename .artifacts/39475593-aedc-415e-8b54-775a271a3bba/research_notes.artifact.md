# Research Notes - Seek Bar Dynamic Coloring

The goal is to update seek bars (progress bars) across the app to use a color derived from the album art, specifically matching the vibrant accent color used for play buttons and lyrics cards.

## Key Findings

### 1. Color Extraction logic
- `PlayerColorScheme` is responsible for generating the color scheme based on the `PaletteColor` extracted from album art.
- In `VibrantColor` mode:
    - `surfaceColor` = `color.backgroundColor` (Dominant/Muted background)
    - `primaryColor` = `color.primaryColor` (Vibrant accent)
    - `onSurfaceColor` = `color.primaryTextColor` (High contrast text color, e.g., White/Black)

### 2. Current Seek Bar Coloring
- **Spotify Style**: Already uses `scheme.primaryColor` for both play button and seek bar.
- **Vibrant Style**: Already uses `scheme.primaryColor` for both play button and seek bar.
- **M3 Style**: Already uses `scheme.primaryColor` for both play button and seek bar.
- **Default Style**: In `VibrantColor` mode, it uses `scheme.onSurfaceColor` (e.g., White) for both.
- **Plain Style**: Same as Default, uses `scheme.onSurfaceColor` in `VibrantColor` mode.
- **Full Cover Style**: Uses `scheme.onSurfaceColor` for both.
- **Mini Player**: Currently uses hardcoded `spotifyGreen` for Spotify theme and likely default theme colors for others. It does NOT observe `PlayerColorScheme` for dynamic updates.

### 3. Seek Bar Types
- **XML SeekBars**: Wrapped in `MusicSlider.kt`, which uses a custom `SquigglyProgress` drawable.
- **Compose Sliders**: Used in `NowPlayingSlider.kt`, which relies on `MaterialTheme.colorScheme.primary`.
- **LinearProgressIndicator**: Used in `MiniPlayerFragment.kt`.

## Proposed Changes

1.  **MiniPlayerFragment.kt**:
    - Observe `playerViewModel.colorSchemeFlow`.
    - Update `progressBar` and `bottomProgressBar` indicator colors dynamically using `scheme.primaryColor`.

2.  **DefaultPlayerControlsFragment.kt** & **PlainPlayerControlsFragment.kt**:
    - In `getTintTargets`, change `newEmphasisColor` to always use `scheme.primaryColor` when in `VibrantColor` mode (or simply always use `primaryColor` if it's preferred as the accent color).

3.  **FullCoverPlayerControlsFragment.kt**:
    - Update `getTintTargets` to use `scheme.primaryColor` for the play button background and seek bar, instead of `onSurfaceColor`.

4.  **SquigglyProgress.kt / MusicSlider.kt**:
    - Ensure that the "album art color" is applied consistently. `MusicSlider` already has a `currentColor` property which is tinted via `getTintTargets`.

5.  **Compose Components**:
    - Ensure `PlayerTheme` is used where necessary to provide the dynamic `primary` color to `Slider` components.

## Questions for User
- Should the seek bar ALWAYS use the vibrant album art color, even in themes like "Material You" or "App Theme", or only when "Vibrant Color" mode is active?
- For `FullCover` style, using the vibrant color might sometimes have lower contrast than White/Black. Is this desired?
