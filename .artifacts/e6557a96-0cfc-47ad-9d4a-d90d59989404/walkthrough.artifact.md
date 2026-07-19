# Walkthrough - Fixed Resource Linking Failure

I have resolved the "Android resource linking failed" error by adding the missing string resources referenced in the `abs_playlists.xml` layout.

## Changes Made

### Resources
#### [MODIFY] [strings.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/strings.xml)
- Added the following missing strings to the default `strings.xml` file:
    - `moods_label` ("Moods")
    - `tags_label` ("Tags")
    - `instruments_label` ("Instruments")

These strings are used as button labels in the `abs_playlists.xml` layout file, which is part of the home/playlists screen.

## Verification Results

### Automated Tests
- Executed `./gradlew :app:assembleFdroidDebug` to verify resource linking.
- **Result**: Build finished successfully.

```
{
  "status": "Build finished successfully."
}
```
