# Add MusicBrainz Lookup Loader

The user wants better feedback when performing MusicBrainz lookups. Currently, the scan process in the Settings screen shows a progress bar only after the total number of items is determined. There is no immediate visual "loading" state when the scan starts.

## User Review Required

> [!IMPORTANT]
> The loader will be added to the manual scan buttons in the Network Settings screen. If there are other MusicBrainz lookup points (like single-song lookup) that were intended, please clarify.

## Proposed Changes

### [UI Component]

#### [MODIFY] [NetworkSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/settings/NetworkSettingsComposeScreen.kt)

- Update `ScanButtonWithProgress` to show a `CircularProgressIndicator` as `trailingContent` in the `ExpressivePreferenceItem` when `isScanning` is true.
- This ensures immediate visual feedback even before the progress bar appears.

## Verification Plan

### Manual Verification
- Deploy the app.
- Go to **Settings** → **Network Settings**.
- Enable **MusicBrainz Lookup**.
- Click **Look Up Song Tags** or **Look Up Artists**.
- Verify that a spinner appears immediately in the button.
- Verify that the spinner stays visible during the scan process alongside the progress bar.
