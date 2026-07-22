# Project Music — Software, APIs, and Packages Reference

This document provides a comprehensive list of the software versions, APIs, and packages used in the development and operation of Project Music.

---

## 1. Development Environment

| Component | Version |
| :--- | :--- |
| **Gradle** | 9.5.0 |
| **Kotlin** | 2.3.21 |
| **JDK (Toolchain)** | 21 |
| **Android Gradle Plugin (AGP)** | 9.3.0 |
| **KSP (Kotlin Symbol Processing)** | 2.3.6 |
| **Compose Compiler** | (Integrated with Kotlin 2.3.21) |

---

## 2. Android SDK Configuration

| Property | Value | API Level |
| :--- | :--- | :--- |
| **Minimum SDK** | 26 | Android 8.0 (Oreo) |
| **Target SDK** | 36 | Android 15 (Vanilla Ice Cream) |
| **Compile SDK** | 37 | Android 15 (Baklava) |

---

## 3. Application Versioning

| Property | Value |
| :--- | :--- |
| **Version Name** | 1.3.1-beta.2 |
| **Version Code** | 1310102 |

---

## 4. External APIs & Services

The application integrates with the following external services for metadata, lyrics, and scrobbling:

- **Last.fm API**: Metadata and scrobbling.
- **Genius API**: Lyrics retrieval.
- **Lyrically API**: Enhanced lyrics support.
- **NetEase/Kugou/LrcLib**: Lyrics providers (via custom integrations).
- **MusicBrainz**: Advanced metadata enrichment.
- **Acoustid**: Audio fingerprinting for identification.

---

## 5. Major Libraries & Frameworks

### Core AndroidX & UI
- **Jetpack Compose BOM**: `2026.06.01`
- **Material Components (M3)**: `1.14.0` / `1.5.0-alpha23`
- **Navigation**: `2.9.8`
- **Room Persistence**: `2.8.4`
- **WorkManager**: `2.11.2`

### Media & Playback
- **Media3 (ExoPlayer/Session)**: `1.10.1`
- **FFmpeg Decoder (Jellyfin)**: `1.9.0+1`
- **TagLib (KMP)**: `1.0.6`
- **JAudioTagger**: `2.3.15`

### Network & DI
- **Koin (Dependency Injection)**: `4.2.2`
- **Ktor (HTTP Client)**: `3.5.1`
- **Coil (Image Loading)**: `3.5.0`

---

## 6. Complete Package Inventory (`libs.versions.toml`)

| Category | Package / Module | Version |
| :--- | :--- | :--- |
| **Build Plugins** | Android Gradle Plugin | `9.3.0` |
| | Kotlin Android | `2.3.21` |
| | KSP | `2.3.6` |
| | AboutLibraries | `15.0.3` |
| **Foundation** | Kotlinx Coroutines | `1.11.0` |
| | Kotlinx Datetime | `0.8.0` |
| | AndroidX Core KTX | `1.19.0` |
| | AppCompat | `1.7.1` |
| **UI Components** | ConstraintLayout | `2.2.1` |
| | RecyclerView | `1.4.0` |
| | Palette | `1.0.0` |
| | Splashscreen | `1.2.0` |
| | Glance (Widgets) | `1.1.1` |
| **Audio/Media** | Media3 Suite | `1.10.1` |
| | M3Color (Dynamic Color) | `2026.1` |
| **Utilities** | Markwon (Markdown) | `4.6.2` |
| | Balloon (Tooltips) | `1.7.6` |
| | Ksoup (HTML Parsing) | `0.6.0` |
| | Apache Commons Text | `1.15.0` |
| | Juniversalchardet | `2.5.0` |
| | CustomActivityOnCrash | `2.4.0` |
