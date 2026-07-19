# Implementation Plan - Fix Unresolved Reference 'EraShapeFamily'

The build error `Unresolved reference 'EraShapeFamily'` in `AppearanceExpressiveSheets.kt` is caused by incorrect imports. The enums `EraShapeFamily` and `EraSurfaceMaterial` are currently defined in `com.mardous.projectmusic.util`, but the file attempts to import them from `com.mardous.projectmusic.core.model.theme`.

To align with the project's architecture where theme-related enums (like `EraFont`, `NowPlayingScreen`, and `VibrantBackgroundMode`) reside in `com.mardous.projectmusic.core.model.theme`, I propose moving `EraShapeFamily` and `EraSurfaceMaterial` to that package.

## Proposed Changes

### [core.model.theme]

#### [MODIFY] [EraDesignSystem.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/core/model/theme/EraDesignSystem.kt)
- Add `EraShapeFamily` and `EraSurfaceMaterial` enum definitions to this file.

### [util]

#### [MODIFY] [Preferences.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/util/Preferences.kt)
- Remove `EraShapeFamily` and `EraSurfaceMaterial` enum definitions.
- Add imports for `EraShapeFamily` and `EraSurfaceMaterial` from `com.mardous.projectmusic.core.model.theme`.

### [ui]

#### [MODIFY] [Shape.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/theme/Shape.kt)
- Update import for `EraShapeFamily` to point to the new package.

#### [MODIFY] [AbsSlidingMusicPanelActivity.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/component/base/AbsSlidingMusicPanelActivity.kt)
- Update the fully qualified reference to `EraSurfaceMaterial` or add an import.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileFdroidDebugKotlin` to verify the build error is resolved.

### Manual Verification
- None required as this is a compile-time fix.
