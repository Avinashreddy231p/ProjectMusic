# Custom Theme Colors and Fonts

Add customization options for Primary, Secondary, Tertiary, and Neutral colors, as well as Headline, Body, and Label fonts. These options will be available in the Appearance settings.

## Proposed Changes

### Core Utilities & Preferences

#### [Preferences.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/util/Preferences.kt)

- Add constants for the new preferences.
- Add getters and setters for `primaryColor`, `secondaryColor`, `tertiaryColor`, `neutralColor`, `headlineFont`, `bodyFont`, and `labelFont`.

### Resources & UI

#### [strings.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/strings.xml)

- Add titles and detailed summaries for the new settings explaining their effects.
- Add array entries for font selection.

#### [arrays.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/arrays.xml)

- Add `pref_font_family_entries` and `pref_font_family_values`.

#### [preferences_screen_appearance.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/xml/preferences_screen_appearance.xml)

- Add the new color and font preferences to the UI.

### Theme & Typography

#### [Type.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/theme/Type.kt)

- Implement a helper function `getFontFamily(fontName: String?)` to return the appropriate `FontFamily` (Google Sans, Sans Serif, Serif, or Monospace).

#### [Theme.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/theme/Theme.kt)

- Update `BoomingMusicTheme` to apply the custom colors and fonts from `Preferences`.
- Ensure "on" colors (like `onPrimary`) are calculated based on the selected custom colors to maintain readability.

### Settings Implementation

#### [AccentColorPreferenceDialog.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/component/preferences/dialog/AccentColorPreferenceDialog.kt)

- Update to handle the new color keys (`PRIMARY_COLOR`, `SECONDARY_COLOR`, etc.) and display their respective titles in the dialog.

#### [PreferencesScreenFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/PreferencesScreenFragment.kt)

- Register the new color keys in `onDisplayPreferenceDialog` to show the `AccentColorPreferenceDialog`.
- Add change listeners for font preferences to recreate the activity when a font is changed.

## Verification Plan

### Automated Tests
- I will verify the changes by compiling the app: `gradlew assembleDebug`.

### Manual Verification
- Deploy the app to a device/emulator.
- Navigate to **Settings > Appearance**.
- Change the **Primary Color** and verify it updates the UI (e.g., buttons, tabs).
- Change the **Secondary Color** and verify it updates secondary elements.
- Change the **Tertiary Color** and verify it updates complementary accents.
- Change the **Neutral Color** and verify it updates surface/background tones.
- Change the **Headline Font** and verify large titles change their typeface.
- Change the **Body Font** and verify list items and main content text change their typeface.
- Change the **Label Font** and verify small labels and captions change their typeface.
- Verify that "on" colors adapt to custom colors to ensure text remains readable.
