# Fix NullPointerException in VibrantPlayerFragment

The user reported a `NullPointerException` when switching to the Vibrant Player screen. The stack trace indicates a failure in `FragmentExt.kt`'s `whichFragment` extension function, specifically when casting a `null` fragment to a non-null type.

## Research Findings
- `VibrantPlayerFragment` calls `whichFragment(R.id.playbackControlsFragment)` in `onCreateChildFragments()`.
- The layout `fragment_vibrant_player.xml` defines a `FragmentContainerView` with ID `playbackControlsFragment` but fails to specify the `android:name` attribute.
- Other player styles (Default, Expressive, Spotify, etc.) correctly specify their respective controls fragment classes in their XML layouts.
- `FragmentExt.kt` uses an unsafe cast `as T`, which throws an NPE if the fragment is not found.

## Proposed Changes

### [Layouts]

#### [MODIFY] [fragment_vibrant_player.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/layout/fragment_vibrant_player.xml)
Add `android:name="com.mardous.booming.ui.screen.player.styles.vibrantstyle.VibrantPlayerControlsFragment"` to the `FragmentContainerView` with id `playbackControlsFragment`.

### [Extensions]

#### [MODIFY] [FragmentExt.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/extensions/FragmentExt.kt)
Update `whichFragment` to provide a more descriptive error message when a fragment is not found, rather than relying on an unsafe cast.

## Verification Plan

### Automated Tests
- I will attempt to run `gradle_build` to ensure the project still compiles.

### Manual Verification
- The user should verify that switching to the "Vibrant" now playing screen no longer causes a crash.
