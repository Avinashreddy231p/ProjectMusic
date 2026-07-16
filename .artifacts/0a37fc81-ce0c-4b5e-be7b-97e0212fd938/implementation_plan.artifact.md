# Implementation Plan: Expand Lyrics Providers

Expand the lyrics capabilities of the app by adding Genius API and LyricsPlus (KPoe) providers, and expanding the existing Lyrically (Paxsenix) provider.

## Proposed Changes

### [Component: Data Models]

#### [MODIFY] [SearchResponse.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/model/SearchResponse.kt)
- Add `GeniusSearchResponse` and its nested classes (`Hit`, `Result`, `PrimaryArtist`) to support Genius search results.

#### [MODIFY] [LyricsResponse.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/model/LyricsResponse.kt)
- Add `LyricsPlusResponse` and its nested `LyricsPlusLine` class to support the KPoe backend response format.

---

### [Component: Network Features & Preferences]

#### [MODIFY] [NetworkFeature.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/model/network/NetworkFeature.kt)
- Add `Genius` and `LyricsPlus` objects to the `Lyrics` sealed class.
- Define new preference keys: `GENIUS_ENABLED_KEY` and `LYRICSPLUS_ENABLED_KEY`.

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/SettingsViewModel.kt)
- Update `SettingsUiState` to include `geniusEnabled` and `lyricsPlusEnabled`.
- Add `setGeniusEnabled` and `setLyricsPlusEnabled` methods.

#### [MODIFY] [NetworkSettingsComposeScreen.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/settings/NetworkSettingsComposeScreen.kt)
- Add UI toggles for Genius and LyricsPlus in the "Lyrics Providers" section.

---

### [Component: Lyrics APIs]

#### [MODIFY] [LyricallyApi.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/api/lyrically/LyricallyApi.kt)
- Expand to support Musixmatch search via Paxsenix if the Apple Music search doesn't yield results.

#### [NEW] [GeniusApi.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/api/genius/GeniusApi.kt)
- Implement `LyricsApi` for Genius.
- Use Genius search API to find the song.
- Implement a basic scraper to extract lyrics from the Genius webpage (since their API doesn't provide lyrics text).

#### [NEW] [LyricsPlusApi.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/api/lyricsplus/LyricsPlusApi.kt)
- Implement `LyricsApi` for the LyricsPlus (KPoe) backend.
- Fetch synced lyrics from `https://lyricsplus.prjktla.my.id/v2/lyrics/get`.

---

### [Component: Integration]

#### [MODIFY] [LyricsDownloadService.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/data/remote/lyrics/LyricsDownloadService.kt)
- Add `GeniusApi` and `LyricsPlusApi` to the `lyricsApi` list.

---

## Verification Plan

### Manual Verification
- **Settings**: Verify that new toggles appear in Network Settings and correctly persist their state.
- **Lyrics Download**:
    - Play various songs and observe the Logcat to see which provider successfully returns lyrics.
    - Verify that Genius lyrics (scraped) are correctly displayed as plain text.
    - Verify that LyricsPlus lyrics are correctly displayed as synced lyrics.
    - Test edge cases like no internet or no lyrics found.
