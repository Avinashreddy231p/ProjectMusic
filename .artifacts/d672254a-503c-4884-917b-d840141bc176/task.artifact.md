# Task - Appearance Settings Redesign (Material 3 Expressive)

- [ ] **Infrastructure & Base Components**
    - [ ] Create `ExpressiveBottomSheet` base component
    - [ ] Create `PreviewCard` components (Player, NavBar, Swipe)
    - [ ] Create `RadioCard` and `ColorSwatch` selection components

- [ ] **Dashboard Redesign**
    - [ ] Update `AppearanceSettingsComposeScreen.kt` main layout
    - [ ] Group settings into "Appearance" and "Now Playing" sections
    - [ ] Connect all items to trigger bottom sheets

- [ ] **Implement Specialized Bottom Sheets**
    - [ ] `PlayerStyleBottomSheet` with miniature player previews
    - [ ] `ThemeSelectionBottomSheet` (General Theme, Accent Color, Custom Palette)
    - [ ] `NavigationBottomSheet` (Tab Titles Mode with live NavBar preview)
    - [ ] `LibraryCategoriesBottomSheet` with Drag-and-Drop + Search
    - [ ] `SwipeActionsBottomSheet` with animated action previews
    - [ ] `VisualEngineBottomSheet` (Era Design System settings)

- [ ] **Logic & Persistence**
    - [ ] Ensure all selections in bottom sheets update `SettingsViewModel`
    - [ ] Verify persistence of new settings

- [ ] **Refinement & Polish**
    - [ ] Add Material Motion transitions for selections
    - [ ] Ensure proper edge-to-edge support and insets in all sheets
    - [ ] Final UI/UX audit against Expressive guidelines

- [ ] **Verification**
    - [ ] Build project
    - [ ] Manual verification of all bottom sheets and previews
