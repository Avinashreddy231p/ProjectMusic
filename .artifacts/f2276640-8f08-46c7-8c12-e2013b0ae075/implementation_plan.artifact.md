# Implementation Plan - Total Restoration of Settings

Systematically restore **every** single component and setting from the legacy XML configurations into the new Material 3 Expressive Compose UI.

## User Review Required

> [!IMPORTANT]
> **Total Restoration**: I am adding back roughly 60+ missing toggles, sliders, and dialogs across all screens.
> **Clean IA**: To avoid "Setting Overload," I will use **Modal Bottom Sheets** and **Expandable Split Buttons** to tuck away secondary details while keeping them discoverable.

## Proposed Changes

### 1. Appearance & Customization [MODIFY]
*   **Era Engine**: Add missing seeds (Secondary, Tertiary, Error) and missing tokens (Motion Intensity, Surface Material, Type Scale, Asymmetric Shapes).
*   **Widget Settings**: Implement a dedicated "Widget Customization" Bottom Sheet containing all 4 widget-specific settings.
*   **Interface**: Add `ui_theme` selector, `library_categories` color picker/selector, and navigation preferences.

### 2. Now Playing Visuals [MODIFY]
*   **Gestures Group**: A new section containing all 5 cover tap actions (Single, Double, Left/Right Double, Long) and all 5 swipe gestures.
*   **Controls Group**: Restore `circle_play_button`, `adaptive_controls`, `animate_player_control`, and `add_extra_controls`.
*   **Visual Fidelity**: Restore `player_blur_radius`, `open_on_play`, and `lyrics_card_corner_radius`.
*   **Metadata**: Restore "Scrolling Text", "Remaining Time", and "Extra Info" configurations.

### 3. Lyrics Experience [MODIFY]
*   **Advanced Typography**: Split into "Synced" and "Plain" lyrics groups with individual bold and font-size controls.
*   **Visual Effects**: Restore `accent_color`, `background_effect` (List), `karaoke_style`, and `progressive_coloring`.
*   **Processing**: Restore instrumental identifiers, blank line filtering, and UTF-8 enforcement.

### 4. Playback & Audio [MODIFY]
*   **Queue Engine**: Restore visibility toggles, clear-on-completion, and "Queue Next" logic.
*   **Shuffle Logic**: Restore "Remember Shuffle" and the distinct Album/Artist shuffle modes.
*   **Connectivity**: Restore all 4 states (Resume/Pause on Connect/Disconnect for both Wired and Bluetooth).

### 5. Library & Files [MODIFY]
*   **Smart Playlists**: Restore history intervals and recently added cutoffs.
*   **Folders**: Restore `recursive_folder_actions` (Multi-select logic).
*   **Exclusion**: Restore song duration and minimum count filters.

### 6. Network & Services [MODIFY]
*   **Lyrics Search**: Restore individual provider toggles (LRCLIB, BetterLyrics, Lyrically).
*   **Online Assets**: Restore provider selection, image size, and specific artist/album artwork toggles.

### 7. System & Advanced [MODIFY]
*   **Globalization**: Restore `language_name` (App Language).
*   **Engine Tweaks**: Restore "MP3 Index Seeking", "Pause on Zero Volume", and "Stop when closed".

---

## Verification Plan

### Automated Tests
*   `gradle :app:assembleDebug` to verify all new bindings and logic.

### Manual Verification
*   Open every settings page and verify against original XMLs.
*   Test "Import Custom Font" action in Lyrics.
*   Verify "Clear Cache" and "Backup/Restore" flows.
*   Test Dynamic Color switching (Material You) with the restored toggles.
