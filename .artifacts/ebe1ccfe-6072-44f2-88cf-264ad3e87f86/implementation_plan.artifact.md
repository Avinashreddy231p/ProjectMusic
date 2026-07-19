# Implementation Plan - Revert Icons Update

This plan outlines the steps to revert the "Icons Update" which introduced Phosphor and Lucide icon packs. The goal is to return to a Material-only iconography system while maintaining the centralized `AppIcons` structure for consistency across the codebase.

## User Review Required

> [!NOTE]
> I am removing the ability to switch icon packs and weights from the settings. The app will default back to Material Symbols (Filled/Outlined based on the "Regular" weight setting).

## Proposed Changes

### Build & Dependencies

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/gradle/libs.versions.toml)
- Remove `phosphor` version.
- Remove `ext-phosphor` library.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/build.gradle.kts)
- Remove `implementation(libs.ext.phosphor)`.

---

### Design System & Icons

#### [MODIFY] [EraDesignSystem.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/core/model/theme/EraDesignSystem.kt)
- Remove `IconPack` and `IconWeight` enums.

#### [MODIFY] [AppIcons.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/theme/AppIcons.kt)
- Remove Phosphor imports.
- Simplify `IconProvider.getIcons()` to always return Material icons.
- Remove `getPhosphorIcons` and `getLucideIcons` (already cleared logic).

#### [MODIFY] [Theme.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/theme/Theme.kt)
- Update icon resolution logic to not require `eraIconPack` and `eraIconWeight`.

---

### Preferences & Settings

#### [MODIFY] [Preferences.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/util/Preferences.kt)
- Remove `ERA_ICON_PACK`, `ERA_ICON_WEIGHT`, and `ERA_ADAPTIVE_ICONOGRAPHY` keys and properties.

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/SettingsViewModel.kt)
- Remove icon-related state from `SettingsUiState`.
- Remove `setEraIconPack`, `setEraIconWeight`, and `setEraAdaptiveIconography` methods.

#### [MODIFY] [AppearanceSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/AppearanceSettingsComposeScreen.kt)
- Remove the "Iconography" section from the `DesignSystemBottomSheet`.

## Verification Plan

### Automated Tests
- Run Gradle sync.
- Execute build: `./gradlew :app:compileFdroidDebugKotlin`.

### Manual Verification
- Verify that the app builds and runs without unresolved reference errors.
- Check the settings UI to ensure the "Iconography" section is gone.
- Verify that icons are correctly displayed using Material Symbols.
