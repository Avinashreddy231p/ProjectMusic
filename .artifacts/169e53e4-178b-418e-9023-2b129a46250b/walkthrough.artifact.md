# Walkthrough: Integrated Lyrics for Android Auto

I have implemented a seamless way to view lyrics directly inside the standard Android Auto music player. This approach is user-friendly as it requires no developer settings and works "officially" across all car displays.

## Features: Integrated Metadata Lyrics
This seamless experience injects lyrics into the fields normally used for song information.

- **How it works**:
    - Replaces the **Song Title** with the *current lyric line*.
    - Replaces the **Artist Name** with the *next lyric line*.
    - Works in the standard music player, dashboard clusters, and connected smartwatches.
- **Toggle**: You can turn this on or off in the app settings (**Settings > Lyrics > Display lyrics in main player**).
- **No Developer Mode Required**: Unlike standalone car apps, this works automatically once enabled in the phone app.

---

## Technical Changes

### 1. Playback Service Integration
- **[PlaybackService.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/playback/PlaybackService.kt)**:
    - Added a real-time metadata observer that triggers when an Automotive controller connects.
    - Implemented `updateMetadataWithLyrics()` to swap song info with synced lyrics every 500ms.
    - Ensures original metadata is restored when lyrics are turned off or the song ends.

### 2. User Settings
- **[strings.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/values/strings.xml)**: Added clear descriptions for the new Android Auto lyrics setting.
- **[preferences_screen_lyrics.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/xml/preferences_screen_lyrics.xml)**: Added a simple toggle switch for the feature.

---

## How to Test

1.  Deploy the updated app to your phone.
2.  Open **Settings > Lyrics** and enable **"Display lyrics in main player"**.
3.  Connect your phone to Android Auto (or DHU).
4.  Play a song with synced lyrics.
5.  Open the standard **Music** player in the car. You will see the lyrics scrolling in real-time where the song title normally is.
