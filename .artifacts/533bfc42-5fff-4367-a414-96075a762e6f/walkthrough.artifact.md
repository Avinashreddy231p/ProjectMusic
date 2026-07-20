# MusicBrainz Selection Lookup Walkthrough

I have implemented the ability to perform MusicBrainz lookups on specific selections of songs or artists. This includes a new selection UI with "Select/Deselect All" functionality and a real-time progress tracker.

## Key Changes

### Data & Logic
- **[MusicBrainzRepository](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/data/local/repository/MusicBrainzRepository.kt)**: Added `lookupSongsByIds` and `lookupArtistsByIds` for targeted scanning.
- **[LibraryViewModel](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/screen/library/LibraryViewModel.kt)**: Added `MusicBrainzScanState` and orchestration methods to handle batch lookups.

### User Interface (Compose)
- **[MusicBrainzSelectionDialog](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/dialogs/metadata/MusicBrainzSelectionDialog.kt)**:
    - New BottomSheet dialog for picking items.
    - Added **Select All** and **Deselect All** options.
    - Integrated search filtering for large lists.
- **[MusicBrainzProgressDialog](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/dialogs/metadata/MusicBrainzProgressDialog.kt)**:
    - Provides real-time feedback with a progress bar and status labels during the network-intensive lookup process.

### Integration
- **[MenuItemClickExt.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/projectmusic/ui/component/menu/MenuItemClickExt.kt)**: Connected the "MusicBrainz Lookup" menu item in Song, Album, and Artist context menus.
- **Resources**: Added necessary strings and menu items across the library.

## How to use
1. Long-press to select items in any list (Songs, Artists, Albums).
2. Open the menu and select **MusicBrainz Lookup**.
3. Refine your selection in the dialog (use "Select All" if desired).
4. Click **Start Lookup** to begin the automated metadata enrichment.
