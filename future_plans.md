# Future Plans & Roadmap

This document outlines the planned features and enhancements for Booming Music, organized by implementation priority.

## Phase 1: Core Refinement & UX Polish
- **Performance Optimizations**: Faster scanning and indexing for libraries with 10k+ tracks.
- **Visual Consistency**: Ensuring Material 3 guidelines are strictly followed across all custom components.
- **Edge-to-Edge**: Perfecting system bar transparency and inset handling on all Android versions.

## Phase 2: Expanded Library Features
- **Advanced Metadata Editor**: Bulk editing support and improved album art fetching.
- **Smart Playlists**: Dynamic playlists based on rules (e.g., "Recently Added", "Top Rated", "Never Played").
- **External Lyrics Support**: Enhanced support for synced lyrics from local files (.lrc, .ttml).

## Phase 3: AI Lab Integration
- **Mood Detection**: Integration of AI models to automatically categorize songs by mood and energy.
- **Smart Search**: Semantic search capability to find songs by describing their "vibe" or lyrical themes.
- **Audio Analysis**: Visualizing audio characteristics like tempo (BPM) and key.

## Phase 4: Advanced Experimental Tools
- **AI Stem Separation (AI Mixer)**:
    - **Concept**: On-device separation of any song into individual stems (Vocals, Drums, Bass, Guitar, Piano, and Other).
    - **Technology**: TensorFlow Lite (TFLite) with hardware acceleration (GPU delegate).
    - **Features**: 
        - Multi-track mixing interface in "Now Playing".
        - Karaoke mode (Vocal removal).
        - Instrument isolation for practice.
    - **Note**: This is a resource-intensive feature planned for high-end devices with significant storage availability for caching.
