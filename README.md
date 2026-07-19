# Project Music

[![GitHub Release](https://img.shields.io/github/v/release/Avinashreddy231p/ProjectMusic?label=Latest%20Release)](https://github.com/Avinashreddy231p/ProjectMusic/releases)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE.txt)
[![Telegram](https://img.shields.io/badge/Telegram-Join%20Community-blue?logo=telegram)](https://t.me/mardousdev)

**Project Music** is a modern, feature-rich music player for Android built with the latest technologies. It focuses on a clean, intuitive design while providing powerful features for audiophiles and casual listeners alike.

> "Modern design. Pure sound. Fully yours."

## ✨ Features

- **Modern UI:** Built entirely with Jetpack Compose, featuring Material 3 design and smooth animations.
- **High-Quality Playback:** Powered by AndroidX Media3 for robust and high-fidelity audio handling.
- **Smart Library:** Automatically organizes your music by artists, albums, and folders.
- **Search:** Quickly find your favorite tracks, artists, or albums.
- **Playlists:** Create and manage custom playlists.
- **Lyrics Support:** Integrated lyrics provider to sing along with your favorite songs.
- **Highly Customizable:** Theming support to match your style.
- **Open Source:** Fully transparent and community-driven.

## 🛠️ Technology Stack

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Audio Engine:** [AndroidX Media3](https://developer.android.com/guide/topics/media/media3)
- **Dependency Injection:** [Koin](https://insert-koin.io/)
- **Database:** [Room](https://developer.android.com/training/data-storage/room)
- **Networking:** [Ktor](https://ktor.io/)
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
- **Serialization:** [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)

## 📁 Project Structure

The project follows a modular and organized package structure within the `:app` module:

- [`com.mardous.projectmusic.ui`](app/src/main/java/com/mardous/projectmusic/ui): Jetpack Compose screens, components, and themes.
- [`com.mardous.projectmusic.data`](app/src/main/java/com/mardous/projectmusic/data): Data layer including Room database, repositories, and local/remote data sources.
- [`com.mardous.projectmusic.core`](app/src/main/java/com/mardous/projectmusic/core): Common models, interfaces, and core logic.
- [`com.mardous.projectmusic.playback`](app/src/main/java/com/mardous/projectmusic/playback): Media3-based playback service and controller integration.
- [`com.mardous.projectmusic.util`](app/src/main/java/com/mardous/projectmusic/util): Utility classes, constants, and helper functions.
- [`com.mardous.projectmusic.extensions`](app/src/main/java/com/mardous/projectmusic/extensions): Kotlin extension functions for various Android components.
- [`com.mardous.projectmusic.coil`](app/src/main/java/com/mardous/projectmusic/coil): Custom Coil configurations for efficient image loading (artist/album art).

## 🚀 Build Flavors

Project Music supports multiple build flavors to cater to different distribution platforms:

- **GitHub:** The standard version with all network features enabled (Last.fm, Genius, etc.).
- **F-Droid:** A strictly open-source version with some proprietary network features disabled for compliance.
- **Play Store:** Optimized for Google Play Store distribution.

## 🤝 Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to get started.

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE.txt](LICENSE.txt) file for details.

---

Made with ❤️ by [PARKER](https://www.github.com/Avinashreddy231p) and the community.
