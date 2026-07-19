# Task List - Dynamic Fonts & Icon Packs Implementation

- `[ ]` Add dependencies to `libs.versions.toml` and `app/build.gradle.kts`
- `[ ]` Define Design System Enums (`EraFont`, `IconPack`, `IconWeight`) in `EraDesignSystem.kt`
- `[ ]` Add preference keys and accessors in `Preferences.kt`
- `[ ]` Implement Google Fonts integration in `Type.kt`
- `[ ]` Create `AppIcons.kt` with `CompositionLocal` and semantic icon mapping
- `[ ]` Create `AnimatedIcon.kt` for smooth iconography transitions
- `[ ]` Integrate dynamic typography and icon provider into `ProjectMusicTheme` in `Theme.kt`
- `[ ]` Update `DesignSystemBottomSheet` in `AppearanceSettingsComposeScreen.kt` with Font and Icon pack selectors
- `[ ]` Global search and replace of standard `Icon` usages with `AnimatedIcon` (or specific semantic icon calls)
- `[ ]` Verify animations and persistence of settings
