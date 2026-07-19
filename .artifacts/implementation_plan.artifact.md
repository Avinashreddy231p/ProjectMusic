# Redesign Appearance Settings Screen with Material 3 Expressive

This plan outlines the complete redesign of the Appearance Settings screen, transitioning from a legacy XML-based `PreferenceScreen` to a modern, expressive Jetpack Compose implementation. The focus is on visual elegance, better information architecture, and premium Material 3 Expressive components.

## User Review Required

> [!IMPORTANT]
> - The Appearance Settings screen will be rewritten in Jetpack Compose.
> - The technical "Era Design System" tokens (Harmony mode, seed colors, shape/type scales) will be consolidated into a dedicated sub-section to reduce main-screen clutter.
> - Premium Material 3 Expressive components will be used, including segmented buttons for theme selection.

## Proposed Changes

### [Architecture & State Management]

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/SettingsViewModel.kt)
- Update `SettingsUiState` and `SettingsViewModel` to ensure all appearance-related preferences (especially Era tokens and Widget settings) are fully reactive and have appropriate setters.

### [UI Components]

#### [NEW] [AppearanceSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/AppearanceSettingsComposeScreen.kt)
- **Categorization & Layout:**
    - **Theme & Style:** General Theme (Segmented Button), UI Style (Segmented Button), Material You (Switch), Pure Black (Switch).
    - **Branding & Colors:** Accent Color (Expressive Item), and a "Design System" entry point.
    - **Interface & Layout:** AppBar Mode, Library Categories, Navigation (Tab behavior), Display (Swipe actions, Header images).
    - **Widgets:** Consolidated Widget customization section.
- **Sub-sections (Bottom Sheets):**
    - **Design System:** Consolidates Era tokens: Harmony mode, seed colors (Primary, Secondary, Tertiary, Error), Shape family/scale, Type scale, Contrast, Surface material, Motion intensity.
    - **Widget Settings:** Small layout style, Dynamic colors, Corner radius, Third info line.

### [Navigation & Integration]

#### [MODIFY] [PreferencesScreenFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/PreferencesScreenFragment.kt)
- Update `AppearancePreferencesFragment` to host `AppearanceSettingsComposeScreen` via `ComposeView`.

#### [MODIFY] [preferences_screen_appearance.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/xml/preferences_screen_appearance.xml)
- Mark as legacy for search indexing purposes only.

---

## Redesigned Information Architecture

| Section | Settings included | Component/UX |
| :--- | :--- | :--- |
| **Themes** | General Theme, Material You, UI Style, Pure Black | Segmented Buttons & Switch Groups |
| **Design System** | Era Tokens: Harmony, Seed Colors, Shapes, Typography, Motion | Dedicated Bottom Sheet / Sub-page |
| **Interface** | Header Style, Library Categories, Navigation behavior | Expressive List Items & Dialogs |
| **Personalization**| Swipe Actions, Header Image sizing | Cards & Navigation Actions |
| **Widgets** | Layout Style, Dynamic Colors, Corner Radius, Info line | Dedicated Bottom Sheet |

## Verification Plan

### Automated Tests
- N/A (UI-centric redesign).

### Manual Verification
1. Navigate to Settings -> Appearance.
2. Verify theme switching (Light/Dark/Black/Material You) works instantly.
3. Test "Design System" bottom sheet to ensure fine-grained design tokens persist.
4. Verify "Widget Settings" persistence and functionality.
5. Check visual alignment and spacing against Material 3 Expressive guidelines.
