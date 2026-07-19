# Implementation Plan - Dynamic Fonts & Icon Packs

Integrate 4 Google Fonts and 3 new Icon Packs (Phosphor, Lucide, Iconoir) into the "Era" Design System, allowing users to switch styles and weights dynamically from the settings.

## User Review Required

> [!IMPORTANT]
> **Dynamic Icon Loading**: Switching icon packs throughout the app requires a centralized "Icon Provider" pattern. I will implement this using a `CompositionLocal` so that all Compose components automatically update when the user changes their preference.
> **Dependency Note**: I will add community-maintained Compose libraries for these icon sets. These are efficient and optimized for `ImageVector` usage.

## Proposed Changes

### 1. Build & Dependencies [MODIFY]
*   **[libs.versions.toml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/gradle/libs.versions.toml)**: Add versions and library entries for:
    - `androidx.compose.ui:ui-text-google-fonts`
    - `io.github.dev778g-me:phosphoricon-compose` (Phosphor)
    - `com.composables:icons-lucide` (Lucide)
    - `com.github.iconoir-icons:iconoir-android` (Iconoir)
*   **[app/build.gradle.kts](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/build.gradle.kts)**: Add the new dependencies to the `dependencies` block.

### 2. Design System Tokens [NEW] [MODIFY]
*   **[Preferences.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/util/Preferences.kt)**: Add keys for `ERA_FONT_FAMILY`, `ERA_ICON_PACK`, and `ERA_ICON_WEIGHT`.
*   **[EraDesignSystem.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/core/model/theme/EraDesignSystem.kt)** [NEW]: Define enums for `EraFont`, `IconPack`, and `IconWeight`.

### 3. Dynamic Typography [MODIFY]
*   **[Type.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/theme/Type.kt)**:
    - Implement Google Fonts integration.
    - Add support for **Inter**, **Montserrat**, **Outfit**, and **Manrope**.
    - Add a function to resolve the current `FontFamily` from preferences.

### 4. Centralized Icon & Animation Engine [NEW]
*   **[AppIcons.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/theme/AppIcons.kt)** [NEW]:
    - Define an `AppIcons` data class/interface containing all semantic icons (e.g., `Home`, `Play`, `Settings`, `MusicNote`).
    - Implement a `CompositionLocal` named `LocalAppIcons`.
    - Provide a mapper that returns the correct `ImageVector` from the selected `IconPack` and `IconWeight`.
*   **[AnimatedIcon.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/component/compose/AnimatedIcon.kt)** [NEW]:
    - Create a wrapper for the standard `Icon` composable that uses `Crossfade` or `AnimatedContent` when the `ImageVector` changes due to an icon pack swap, ensuring a smooth visual transition.

### 5. Settings UI [MODIFY]
*   **[AppearanceSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/AppearanceSettingsComposeScreen.kt)**:
    - Add a "Typography" section with a Font picker in the `DesignSystemBottomSheet`.
    - Add an "Iconography" section with an Icon Pack picker and a Weight/Style slider/segmented button.

### 6. Theme Integration [MODIFY]
*   **[Theme.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/theme/Theme.kt)**:
    - Update `ProjectMusicTheme` to provide the dynamic `FontFamily` and the `LocalAppIcons`.

## Verification Plan

### Automated Tests
*   `gradle :app:assembleDebug` to ensure all library dependencies are correctly resolved.

### Manual Verification
*   **Visual Check**: Verify that changing the font in settings instantly updates all text in the app.
*   **Icon Switch**: Change from "Material" to "Phosphor" and verify that navigation icons, player controls, and list icons update.
*   **Weight Test**: Change the weight (e.g., to "Bold") and ensure it reflects in the icons.
*   **Animation Test**: Verify that switching icon packs or fonts results in a smooth crossfade/animation rather than a hard jump.
