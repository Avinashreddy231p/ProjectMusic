# Implementation Plan - Specific Shuffle Icon Path

The user provided a specific SVG path for the shuffle icon and wants to use it instead of the previous design. This SVG uses a stroked path rather than a filled one.

## Proposed Changes

### [Drawables]

#### [MODIFY] [ic_shuffle_24dp.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/drawable/ic_shuffle_24dp.xml)
- Replace current filled paths with the stroked path provided by the user.
- Set `android:strokeWidth="2"`, `android:strokeLineCap="round"`, and `android:strokeLineJoin="round"`.

#### [MODIFY] [ic_shuffle_on_24dp.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/drawable/ic_shuffle_on_24dp.xml)
- Use the same stroked path.
- Retain the active state dot at the bottom.

#### [MODIFY] [ic_launcher_shuffle_all.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/drawable/ic_launcher_shuffle_all.xml)
- Update to match the new stroked design for consistency.

## Verification Plan

### Manual Verification
- Deploy the app.
- Verify that the shuffle icon matches the provided SVG style (stroked lines).
- Verify that the "on" state correctly shows the dot underneath the new stroked icon.
