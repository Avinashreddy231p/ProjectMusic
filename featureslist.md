# 🎵 Project Music — Comprehensive Feature List

This document catalogs every feature in Project Music, ranging from core playback functionality to advanced analytical and audio processing tools.

---

## 1. Core Playback (The Basics)
*   **Gapless Playback**: High-performance audio engine for seamless transitions.
*   **Broad Format Support**: MP3, FLAC, WAV, AAC, M4A, OGG, and more.
*   **Media Session Integration**: Full support for Android media controls and lock screen.
*   **Audio Focus Management**: Intelligent handling of transient and permanent focus loss (ducking, auto-pause).
*   **Headset Multi-Click**: Support for wired headset button controls (1x: Play/Pause, 2x: Next, 3x: Previous).
*   **Notification Controls**: Customizable playback notification with expanded controls.
*   **Bluetooth & Wired Headset Support**: Automatic pause/resume behavior.
*   **Seek Controls**: Smooth seeking with customizable intervals (5s to 60s).
*   **Volume Control**: Integrated software volume slider.
*   **Loop & Shuffle**: Standard repeat (All/One/Off) and shuffle modes.
*   **Startup Behavior**: Option to resume playback on app launch.

---

## 2. Library & Organization
*   **Automated Scanning**: Lightning-fast Media Store indexing.
*   **Direct File Scanning**: Bypass the system Media Store for more accurate metadata.
*   **Trash Music**: Capability to delete audio files directly from the physical storage via the app.
*   **Folder View**: Browse music by file structure with an optional **Hierarchy Mode**.
*   **Search**: Unified search across tracks, artists, albums, and genres.
*   **Advanced Filtering**:
    *   Hide short tracks (configurable minimum duration).
    *   Filter out "Singles" (hide albums with few tracks).
    *   Minimum track count for artists/albums to appear in lists.
*   **Blacklist & Whitelist**: Precise control over which directories are scanned.
*   **Sorting**: Extensive sorting options (Alphabetical, Date Added, Year, Track Number, etc.).
*   **Smart Playlists**:
    *   **Recently Added**: Tracks added within a custom interval.
    *   **History**: Recently played tracks.
    *   **Top Tracks**: Frequently played songs.
*   **Custom Playlists**: Create, edit, and organize with drag-and-drop support.
*   **Custom Artwork**: Set personalized images for **Artists** and **Playlists**.
*   **Release Years**: Browse and filter your entire collection by release year.
*   **Artwork Management**: Support for embedded art, `cover.jpg`/`folder.jpg`, and online art downloading.

---

## 3. Visuals & Customization (The "Era" Design)
*   **Material You**: Dynamic color synchronization with the system wallpaper.
*   **Theming**: Light, Dark, and **Pure Black** (OLED) modes.
*   **Era Design System**:
    *   **Harmony Mode**: Generates unified palettes from a single seed.
    *   **Shape Customization**: Choose between Rounded, Cut, or Squircle shape families.
    *   **Asymmetric Shapes**: Experimental UI layouts for a unique look.
    *   **Vibrancy & Contrast**: Fine-tune the saturation and readability of the UI.
    *   **Motion Intensity**: Adjust the speed and flair of animations.
*   **Typography**: Personalize the app's font family and scale.
*   **Navigation & Tabs**:
    *   **Category Management**: Enable, disable, or reorder home screen tabs (Songs, Artists, Albums, etc.).
    *   **Tab Persistence**: Remember the last opened page upon relaunch.
    *   **Hold Tab to Search**: Long-press any navigation tab to quickly trigger search.
*   **Layout Flexibility**:
    *   Toggleable library tabs.
    *   Multiple AppBar styles.
    *   Compact views for lists.
    *   Horizontal album carousels in artist profiles.
*   **Swipe Actions**: Fully configurable gestures (Left/Right) for list items (e.g., Add to Queue, Play Next, Favorite).
*   **Home Screen Widgets**:
    *   **Visual Personalization**: Adjust image corner radius and toggle dynamic colors.
    *   **Informational Customization**: Choose which metadata fields appear on the third line of the widget.

---

## 4. Advanced Audio Processing
*   **Dual-Engine Equalizer**:
    *   **Legacy Engine**: Classic 5-band EQ for maximum compatibility.
    *   **Precision Engine**: Dynamics-based engine with up to **32 bands**.
*   **Dynamics Processing**:
    *   **Multi-band Compressor (MBC)**: Individual compression across frequency ranges.
    *   **Limiter**: Prevents distortion and clipping at high volumes.
*   **Audio Enhancers**:
    *   **Bass Boost**: Targeted low-end enhancement.
    *   **Virtualizer**: Expands the soundstage for headphones.
    *   **Loudness Enhancer**: Target gain control for quieter tracks.
*   **ReplayGain**: Support for Track and Album gain tags with a manual preamp.
*   **Advanced Engine Hooks**:
    *   **Audio Offload**: Hardware-accelerated processing for power efficiency.
    *   **Float Output**: High-bitrate 32-bit float audio pipeline.
    *   **Skip Silence**: Automatically jumps over silent parts of tracks.
*   **Playback Modifications**: Real-time **Tempo** and **Pitch** control.

---

## 5. Lyrics Experience
*   **High-Precision Sync**: Support for **Syllable-level (Rich Sync)** timing.
*   **Lyrics View**: Fluid, Spotify-style UI with automatic scrolling.
*   **Translation & Transliteration**: Multilingual support for international tracks.
*   **Source Integration**: Fetches from LrcLib, NetEase, Kugou, Genius, BetterLyrics (Unison), and more.
*   **Community Contribution**: Built-in support for submitting and correcting lyrics via the **Unison** platform.
*   **Local Support**: Reads external `.lrc` and `.ttml` files.
*   **Embedded Lyrics**: Reads and writes lyrics directly to audio file tags.
*   **Lyrics Editor**: Built-in tool to adjust timing and content manually.
*   **Interactive Seek**: Click any lyric line to jump to that timestamp.
*   **Background Effects**: Animated gradients and blurs based on album art.

---

## 6. Deep Analytics & Insights
*   **68 Unique Metrics**: Tracking every aspect of your listening habits.
*   **Behavioral Tracking**:
    *   **End Reasons**: Know why you stop songs (skip, finish, timeout).
    *   **Activity Peaks**: Identify your most active listening hours and days.
    *   **Weekend vs Weekday**: Comparative analysis of habits.
*   **Contextual Analytics**:
    *   **Device Profiles**: Stats grouped by headphones, speakers, or Bluetooth.
    *   **Queue Sources**: Track where you discover music (Search vs Folders vs Playlists).
    *   **Audio Profiles**: Analysis of formats (FLAC vs MP3) and bitrates.
*   **Categorical Rankings**: Top tracks/artists/albums/genres, but also **Moods**, **Tags**, and **Instruments**.
*   **Monthly Wrapped**: Automated summary of your month in music.
*   **Data Export**: Export your entire listening history to **CSV** or **JSON**.

---

## 7. Metadata & Intelligence
*   **Advanced Tag Editor**: Full control over ID3 tags, Vorbis comments, and MP4 atoms.
*   **Deep Tag Support**: Edit Composer, Conductor, Lyricist, Arranger, BPM, and more.
*   **Artwork Fetcher**: Integrated Deezer API for high-resolution cover art.
*   **Metadata Enrichment**: Advanced lookup and data filling via the **MusicBrainz** database.
*   **AI Tagging**: Automated detection of Energy, Valence, and Danceability (powered by local engine).
*   **Audio Fingerprinting**: Uses Acoustid and SHA256 for precise track identification.
*   **Web Search Integration**: One-tap search for song details or art on the web.

---

## 8. Utilities & Power Features
*   **Backup & Restore**: Full local backup of stats, playlists, and settings.
*   **Sleep Timer**: Auto-stop music with optional fade-out.
*   **MP3 Index Seeking**: Precise seeking for CBR MP3 files.
*   **Scrobbling**: Native support for **Last.fm** and **ListenBrainz** with a **Pending Scrobbles** manager for offline use.
*   **Social Profiles**: View your Last.fm profile details directly within the app.
*   **In-App Updater**: Built-in system for Stable, Beta, and Alpha updates.
*   **Diagnostics**: Internal tools to verify database health, index settings for search, and view detailed device and build information.
