# Implementation Plan - Ultra-Fluid Live REDESIGN (Real Beat Sync)

This plan details a major visual and technical overhaul to provide **Real-Time Beat Sync**, a **Global Vibrant Background**, and a **Fluid Home Screen Redesign**.

## User Review Required

> [!IMPORTANT]
> I will implement **Real-Time Audio Analysis** via a custom PCM Processor. This provides a "Live" feel that reacts to the actual loudness of the music. No extra permissions required.

> [!CAUTION]
> The **Global Vibrant Background** feature will change the look of the entire app (Home, Songs, Albums, etc.). I will add a setting to toggle this behavior so the user can choose between the classic look and the new Redesign.

## Proposed Changes

### 1. Real-Time Audio (The "Live" Signal)

#### [NEW] [BeatAudioProcessor.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/playback/processor/BeatAudioProcessor.kt)
- Implements `BaseAudioProcessor`.
- Calculates the RMS (Root Mean Square) energy of the PCM buffer.
- Updates a singleton `AmplitudeFlow`.

#### [MODIFY] [BoomingMusicRenderersFactory.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/playback/renderer/BoomingMusicRenderersFactory.kt)
- Add the `BeatAudioProcessor` to the audio sink pipeline.

### 2. Global Background System

#### [MODIFY] [sliding_music_panel_layout.xml](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/res/layout/sliding_music_panel_layout.xml)
- Add a `ComposeView` at the bottom of the `CoordinatorLayout` for the **Global Vibrant Background**.
- Remove the hardcoded `?colorSurface` background from the `fragment_container`.

#### [MODIFY] [AbsSlidingMusicPanelActivity.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/component/base/AbsSlidingMusicPanelActivity.kt)
- Manage the visibility and state of the Global Background based on the new setting.

### 3. Home Screen Redesign (Fluid Look)

#### [MODIFY] [HomeFragment.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/screen/library/home/HomeFragment.kt)
- Update the layout to use a transparent background.
- Apply subtle glass-morphism (blur/translucency) to the stats card and list items so they look "floating" over the global background.

### 4. Background Visual Refinement

#### [MODIFY] [VibrantBackground.kt](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/app/src/main/java/com/mardous/booming/ui/component/compose/decoration/VibrantBackground.kt)
- Replace the simulated `beatPulse` with the `AmplitudeFlow` signal.
- **Chromatic Aberration:** Implement slightly shifted color layers for Flow and Grid modes that intensify on loud peaks.
- **Liquid Blending:** Use `BlendMode.Plus` for mesh circles to create glowing color intersections.

## Verification Plan

### Automated Tests
- Build verification.

### Manual Verification
1.  **Real-Time Sync:** Play a bass-heavy track and verify that the particles/glow react to the actual "kicks" in the music.
2.  **Global Mode:** Enable "Global Vibrant Background" and verify that the home screen shows the animated background behind the list items.
3.  **Performance:** Ensure that even with global rendering, the app remains responsive on mid-range devices.
4.  **Pause Behavior:** Verify that everything drifts to a halt gracefully when music stops.
