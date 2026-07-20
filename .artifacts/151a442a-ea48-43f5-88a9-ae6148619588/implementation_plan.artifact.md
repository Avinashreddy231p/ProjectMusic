# Remove Legacy Icon Assets

The user wants to completely remove the old icon designs from the project. This involves deleting legacy `.webp` and `.png` files and updating UI references that were pointing to those assets.

## Proposed Changes

### Assets

#### [NEW] [ic_app_logo.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/drawable/ic_app_logo.xml)
Create a new vector drawable for use in the app UI (like the About screen) that uses the normalized square logo design.

#### [DELETE] Legacy Image Assets
Remove all instances of the old logo design:
- `app/src/main/res/mipmap-*/ic_launcher.webp`
- `app/src/main/res/mipmap-*/ic_launcher_round.webp`
- `app/src/main/res/drawable-*/icon_web.webp`
- `app/src/main/ic_launcher-playstore.png`
- `metadata/en-US/images/icon.png`

### UI Updates

#### [MODIFY] [AboutScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/about/AboutScreen.kt)
Update the logo reference in the About screen from `R.drawable.icon_web` to the new `R.drawable.ic_app_logo`.

## Verification Plan

### Automated Tests
- Run a build to ensure no broken resource references remain.

### Manual Verification
- Navigate to the "About" screen in the app and verify the new square logo is displayed correctly.
- Check the project structure to ensure the deleted files are gone.
