# Progress Style Rework & Visibility Fix Task List

- `[x]` Rework View-based styles in `SquigglyProgress.kt`
    - `[x]` Standardize drawing heights for all styles
    - `[x]` Refine `drawDots` (smaller radius, better spacing)
    - `[x]` Refine `drawSegmented` (subtle gaps, rounded)
    - `[x]` Refine `drawGlow` (elegant falloff)
    - `[x]` Fix `onLevelChange` return value
- `[x]` Fix visibility and initialization in `MusicSlider.kt`
    - `[x]` Implement theme-based color fallback
    - `[x]` Optimize initialization order for state restoration
- `[x]` Rework Compose-based styles in `NowPlayingSlider.kt`
    - `[x]` Sync proportions with refined View logic
    - `[x]` Fix glow tip positioning
- `[x]` Verify fixes across different control styles (Classic/Expressive)
