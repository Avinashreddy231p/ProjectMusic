# Remove Design Customization Features

This plan outlines the removal of the "Design Customization" (List Style) features from the app, while preserving the "New Era Customization" features.

## User Review Required

> [!IMPORTANT]
> This will remove the ability to customize playlist list styles (shapes, alignments, subtitles). The app's lists will revert to their default styling. "New Era" theme customizations (colors, scales, etc.) will be kept.

## Proposed Changes

### Settings & Navigation

#### [MODIFY] [preferences_screen_appearance.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/xml/preferences_screen_appearance.xml)
- Remove the `PreferenceCategory` for "Design Customization" (`design_customization_header`) and the `ListStyle` preference entry.

#### [DELETE] [preferences_screen_list_style.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/xml/preferences_screen_list_style.xml)
- Completely remove the list style preference screen.

#### [MODIFY] [graph_settings.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/navigation/graph_settings.xml)
- Remove the `listStylePreferencesFragment` destination and the action `action_to_listStylePreferences` pointing to it.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsScreen.kt)
- Remove the `ListStyle` enum entry.

#### [MODIFY] [PreferencesScreenFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/PreferencesScreenFragment.kt)
- Remove the `ListStylePreferencesFragment` class.
- Remove the import of `ListStyleHelper`.

---

### Logic & Utilities

#### [DELETE] [ListStyleHelper.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/util/ListStyleHelper.kt)
- Completely remove the list style application utility.

#### [MODIFY] [Preferences.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/util/Preferences.kt)
- Remove `playlistItemShape`, `playlistCardStyle`, `playlistTextAlignment`, and `playlistShowSubtitle` preferences and their corresponding constants.

#### [MODIFY] [PlaylistAdapter.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/adapters/PlaylistAdapter.kt), [PlaylistSongAdapter.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/adapters/song/PlaylistSongAdapter.kt), [PlaylistDetailFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/library/playlists/PlaylistDetailFragment.kt)
- Remove `ListStyleHelper` calls and imports.

---

### Resources

#### [MODIFY] [strings.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/strings.xml)
- Remove `design_customization_header` and all `playlist_` style related strings.

#### [MODIFY] [arrays.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/arrays.xml)
- Remove all `pref_playlist_` related arrays.

#### [MODIFY] [dimens.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/dimens.xml)
- Remove `playlist_soft_corner_radius` and `playlist_expressive_corner_radius`.

## Verification Plan

### Automated Tests
- Run `gradle build` to ensure no broken references remain.

### Manual Verification
- Open **Settings → Appearance** and verify "Design Customization" is gone while "New Era Customization" remains.
- Verify that playlists and song lists in playlists look correct with default styling.
