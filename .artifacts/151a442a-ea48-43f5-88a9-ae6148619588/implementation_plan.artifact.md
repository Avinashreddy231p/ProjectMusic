# Add Reveal Animation to App Opening

The user wants to add a "reveal animation" when the app opens. I will implement a circular reveal transition that occurs when the splash screen finishes, revealing the main app content.

## Proposed Changes

### Logic

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/MainActivity.kt)
- Update `installSplashScreen()` usage to set an `OnExitAnimationListener`.
- Implement a circular reveal animation on the `SplashScreenView`. The animation will start from the center of the logo and shrink the splash screen to reveal the app content underneath.
- Add an interpolator for a smooth "reveal" feel.

### Assets

#### [MODIFY] [m_reveal.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/animator/m_reveal.xml)
- Update `valueTo` for `scaleX` and `scaleY` to `0.43` to match the non-clipping scale recently established for the splash logo.

## Verification Plan

### Manual Verification
- Deploy the app to an AVD (Android 12+ preferred to see the `core-splashscreen` behavior).
- Observe the splash screen icon animation.
- Observe the transition from splash screen to the main screen, verifying the circular reveal effect.
