# Redesign Appearance Settings with Material 3 Expressive

This plan outlines the complete redesign of the Appearance Settings screen using modern Material 3 Expressive components. Every configurable option will be moved into specialized Bottom Sheets with live previews, replacing the current navigation-based or simple dialog-based selection.

## User Review Required

> [!IMPORTANT]
> - **Navigation Change**: Nested settings screens (like Swipe Actions) will be removed in favor of Modal Bottom Sheets.
> - **Live Previews**: Each bottom sheet will feature a "Live Preview" card to show immediate visual impact of the setting.
> - **Expressive Design**: The UI will strictly follow Material 3 Expressive guidelines, using specialized list items and motion.

## Proposed Changes

### [Component] Appearance Settings Screen

#### [MODIFY] [AppearanceSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/AppearanceSettingsComposeScreen.kt)
- Redesign the main layout to group settings into **Now Playing** and **Appearance** sections.
- Use `ExpressivePreferenceItem` and `SegmentedPreferenceGroup` for all top-level items.
- Replace all navigation calls and simple dialogs with specific Bottom Sheet triggers.

### [Component] Expressive Bottom Sheets

#### [NEW] `ExpressiveBottomSheet` (Base Component)
- Create a reusable base for all settings bottom sheets with:
    - Large Title and supporting Description.
    - Container for Live Preview Card.
    - Content area for options (Radio Cards, Segmented Buttons, Search).
    - Sticky Apply/Reset footer where appropriate.

#### [NEW] Specialized Sheets:
1. **Player Style Bottom Sheet**: Shows miniature player previews for each `NowPlayingScreen` style.
2. **Color Scheme & Accent Color Sheets**: Large circular swatches with real-time theme preview.
3. **Tab Titles Mode Sheet**: Preview of the library navigation bar (BottomNav).
4. **Library Categories Sheet**: Integrated drag-and-drop ordering with visibility toggles and search.
5. **Swipe Actions Sheet**: Animated swipe preview demonstrating selected actions (Play Next, Favorite, etc.).
6. **Background Style Sheets**: Previews for Vibrant and Lyrics background modes.

### [Component] Previews & Visuals

#### [NEW] Preview Components
- Implement "Miniature" versions of:
    - Player Layouts (simplified versions of full player styles).
    - Bottom Navigation Bar (for Tab Titles Mode).
    - List Item with Swipe indicator (for Swipe Actions).
    - Color Swatch Grid.

## Verification Plan

### Automated Tests
- Build the project to ensure all new Compose components compile correctly.
- Verify that setting changes in the new UI correctly update the `SettingsViewModel` and persist to `SharedPreferences`.

### Manual Verification
- **Navigation**: Verify that clicking each setting opens the correct bottom sheet.
- **Live Preview**: Confirm that changing an option in a bottom sheet updates the preview card instantly.
- **Persistence**: Change several settings, restart the app, and verify they are still applied.
- **Responsiveness**: Test on different screen sizes to ensure bottom sheets and previews adapt correctly.
- **Interaction**: Test drag-and-drop in Library Categories and search functionality in long lists.
