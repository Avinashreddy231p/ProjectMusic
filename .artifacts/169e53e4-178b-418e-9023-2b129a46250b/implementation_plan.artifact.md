# Redesign Android Auto "Now Playing" and "Browse" Experience

Since we are using the standard system player for Android Auto, we will focus on maximizing its customization options through `Media3` and `MediaLibraryService`. This "redesign" focuses on better organization, rich visual metadata, and easier access to lyrics.

## Proposed Changes

### 1. Visual Browse Experience
We will update the `LibraryProvider` to use Android Auto "Content Style Hints". This allows us to tell the car whether a folder should be displayed as a **Grid** (best for Albums/Artists) or a **List** (best for Songs/Playlists).

#### [MODIFY] [LibraryProvider.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/playback/library/LibraryProvider.kt)
- Update `getRootChildren` to use `CONTENT_STYLE_GRID` for Discovery and `CONTENT_STYLE_LIST` for Library.
- Ensure all category folders (Albums, Artists, Genres) have their **Artwork URIs** correctly set so icons appear in the car.
- Move the **Lyrics** folder to a more prominent position if music is playing.

### 2. Enhanced Control Buttons
We will optimize the "Custom Actions" that appear on the playback screen.

#### [MODIFY] [PlaybackService.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/playback/PlaybackService.kt)
- Reorder `refreshMediaButtonCustomLayout` to prioritize the most useful buttons for driving (e.g., Favorite, Shuffle).
- Ensure high-quality icons are used for all custom commands.

### 3. "Deep" Lyrics Integration
While the "Metadata Hack" shows lyrics in the title, we will improve the "Lyrics Browse" view.

#### [MODIFY] [LibraryProvider.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/playback/library/LibraryProvider.kt)
- Update `getLyricsChildren` to include the song title as the header.
- Add "Check" icons or similar markings to the current line in the browse list to help the user navigate.

## Verification Plan

### Manual Verification
1. Deploy the app to the phone.
2. Launch the **DHU** (Desktop Head Unit).
3. Open the **Browse** menu in the BoomingMusic car app:
    - Verify that Albums and Artists are shown in a **Grid** (with images).
    - Verify that Playlists and Songs are shown in a **List**.
4. Play a song and check the **Now Playing** screen:
    - Verify the order and appearance of the custom buttons (Shuffle, Repeat, Favorite).
5. Open the **Browse > Lyrics** folder:
    - Verify that the lyrics are displayed as a list of items and are easy to read.
