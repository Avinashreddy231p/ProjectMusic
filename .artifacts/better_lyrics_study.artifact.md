# Study of better-lyrics project

This document summarizes the findings from studying the [better-lyrics](https://github.com/better-lyrics/better-lyrics) project and its relevance to **ProjectMusic**.

## Project Overview

**Better Lyrics** is an open-source browser extension that enhances the YouTube Music experience with a focus on:
- **High-Precision Lyrics:** Support for syllable-level (rich sync) timing.
- **Interactivity:** Click-to-seek functionality.
- **Multilingual Support:** Real-time translations and romanization (transliteration).
- **Customization:** A theme marketplace and advanced visual effects.
- **Crowdsourcing:** Integration with the **Unison** platform for community-driven lyrics.

## Comparison with ProjectMusic

| Feature | better-lyrics | ProjectMusic |
| :--- | :--- | :--- |
| **Syllable-Level Sync** | Supported (via Unison, BiniLyrics) | Supported (via Unison, NetEase, etc.) |
| **Translation/Transliteration** | Supported | Supported |
| **Interactive Seek** | Click any line to seek | Click any line to seek |
| **Lyrics Sources** | Unison, Musixmatch, LRCLib, BiniLyrics, YT Captions | LrcLib, NetEase, Kugou, Lyrically, BetterLyrics (Unison), LyricsPlus, Genius |
| **Community Contribution** | Submit/Correct to Unison | Not implemented |
| **Visual Effects** | Blur, Glow, Theme Marketplace | Blur, Gradient, Vibrant Backgrounds |

## Key Insights and Potential Improvements

### 1. Additional Lyrics Sources
- **BiniLyrics:** This is a major source for `better-lyrics` that is currently missing in `ProjectMusic`. It often provides high-quality syllable-synced lyrics where other sources might only have line-synced or plain text.
- **YouTube Captions:** Fallback to YouTube captions could be useful for songs without traditional lyrics but with CC available.

### 2. Unison Community Contributions
- `better-lyrics` has a strong focus on community. Implementing a **"Submit to Unison"** or **"Correct Timing"** feature in the `LyricsEditorScreen` would allow `ProjectMusic` users to contribute back to the ecosystem.
- *Technical Note:* This requires ECDSA P-256 signing of requests, which is a significant implementation task but provides high value.

### 3. UI/UX Refinements
- **Passive Scroll:** For unsynced (plain) lyrics, implementing a smooth automatic scroll that estimates the current position based on song progress could improve the experience for tracks without timing data.
- **Lyrics Animations:** While `ProjectMusic` has excellent animations, `better-lyrics` uses specialized TTML parsing for complex background vocal rendering and multi-person lyrics, which could be further explored.

### 4. Technical Integration
- `ProjectMusic` already uses the `https://unison.boidu.dev/` endpoints in `BetterLyricsApi.kt`.
- There is an opportunity to unify more of the lyrics fetching logic by adopting similar provider priority and fallback strategies as used in the extension.

## Conclusion

`ProjectMusic` is already well-aligned with the `better-lyrics` ecosystem. The most impactful next steps would be:
1.  **Add BiniLyrics API** to expand rich-sync coverage.
2.  **Explore Unison Submissions** to enable community contributions.
3.  **Refine TTML parsing** to support more complex lyrics structures found in the Unison database.
