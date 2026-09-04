<div align="center">

  <img src="composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark.png" alt="Nuvio" width="300" />
  <br />
  <br />

  <h1>Nuvio Kai</h1>
  <p><strong>A High-Performance Anime & Media Client with Native AniList & AniChart Integration</strong></p>
  <p>Built with Kotlin Multiplatform & Compose Multiplatform for macOS and Android.</p>

  <br />

  [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE)
  [![Platform](https://img.shields.io/badge/Platform-macOS%20%7C%20Android-green.svg?style=for-the-badge)](https://github.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

</div>

---

## Features

### Native AniList Ecosystem
- **Zero-Friction Sync**: Instant, real-time two-way synchronization with your AniList account.
- **Interactive Tracker Sheet**: Track episodes, status (*Watching, Completed, Planning, Paused, Dropped*), scores (100-Point & 10-Star ratings), repeat counts, and notes directly from poster quick-actions or details pages.
- **Poster Score & Status Badges**: AniList and MyAnimeList (MAL) scores, library status pills, and title logos rendered seamlessly across all catalog shelves.

### AniChart Seasonal & Airing Schedule
- **Seasonal Grids**: Explore anime by season (*Winter, Spring, Summer, Fall*) with format chips (*TV, Movie, OVA/ONA, Shorts*), genre filters, and popularity sorting.
- **Live Airing Calendar**: Weekly countdown schedule with exact episode release times and air dates.
- **Smart Offline Cache**: In-memory response caching and automatic rate-limit backoff ensure instant, uninterrupted tab navigation.

### Community & Rich Metadata
- **Reviews & Ratings**: Read full community reviews with score breakdowns and upvotes.
- **Forum Discussions**: Browse discussion threads, episode comments, and community feedback.
- **Studio & Franchise Relations**: Explore studio credits, prequel/sequel timelines, and franchise relations.

### Advanced Media Playback & Stream Routing
- **ARM Stream Routing**: Automatic anime-to-stream provider resolution via Anime-Relations-Map (ARM).
- **Stremio Add-on Protocol**: Connect any standard Stremio add-on for high-speed streaming.
- **Custom Player Integrations**: Native macOS video player bridge with hardware acceleration, subtitle customization, and intro/outro skip support.

---

## Downloads & Installation

Download the latest releases for macOS and Android from the **Releases** tab:

- **macOS**: `.dmg` installer (Optimized for Apple Silicon `arm64` and Intel `x86_64`)
- **Android**: `.apk` (Optimized for Mobile and Android TV)

---

## Building From Source

### Prerequisites
- JDK 17 or higher
- Android SDK (for Android builds)
- macOS Xcode command line tools (for macOS DMG packaging)

### Clone & Build

```bash
git clone https://github.com/SoumaditYa21/Nuvio-Kai.git
cd Nuvio-Kai
```

#### Run Desktop Application (macOS):
```bash
./gradlew :composeApp:run
```

#### Package macOS Release DMG:
```bash
./gradlew clean :composeApp:packageReleaseDmg -Pnuvio.macos.arch=arm64
```

#### Assemble Android APK:
```bash
./gradlew :androidApp:assembleRelease
```

---

## Architecture

Nuvio Kai is structured using a clean, decoupled **Sidecar Hook Bridge (`KaiHooks`)**:

- `composeApp/src/commonMain/`: Shared Compose Multiplatform UI, ViewModels, and state management.
- `composeApp/src/commonMain/kotlin/.../features/anilist/`: 100% self-contained native AniList, AniChart, and community features.
- `composeApp/src/desktopMain/`: Desktop-specific windowing, macOS native video player bindings, and packaging.
- `androidApp/`: Android application launcher, Leanback TV support, and hardware acceleration.

---

## Legal & Disclaimer

Nuvio Kai is a client-side interface for browsing metadata and organizing your media libraries. It does not host, distribute, or store any media streams or files. All metadata is retrieved via public APIs (AniList, MetaHub, and user-configured add-ons).

---

## License & Credits

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.  
Based on the open-source Nuvio project with specialized native anime integrations and extensions.
