<div align="center">

# HyperNative

**A privacy-focused, lightweight, and streamlined fork of GameNative.**

*Play your PC games from Steam, Epic Games, GOG, and Amazon directly on Android — without streaming, without telemetry.*

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Fork of GameNative](https://img.shields.io/badge/Fork%20of-GameNative-ff69b4?style=flat-square)](https://github.com/utkarshdalal/GameNative)

</div>

---

**HyperNative** is a community-driven fork of [GameNative](https://github.com/utkarshdalal/GameNative), developed and maintained with AI assistance. It aims to deliver a private, lightweight, and clutter-free local PC gaming experience on Android.

## 🚀 Key Improvements in HyperNative

* 🛡️ **Zero Telemetry & Tracking**: All third-party analytics (PostHog, background usage tracking, and diagnostic telemetry) have been completely removed. Your gameplay, account details, and session data stay strictly local.
* 🤖 **AI-Assisted Maintenance**: Code refactoring, bug fixes, and feature implementations driven and refined with modern AI workflows.

---

## 🗺️ Roadmap

- [x] Complete telemetry and tracker removal
- [ ] Multi-account management with default star toggle (Steam, Epic, GOG, Amazon)
- [ ] Per-game launch account selector
- [ ] Streamlined and modernized UI
- [ ] One-click update button for game compatibility configurations

---

## 🎮 Core Features

* **Run PC Games Locally**: Play games you own from Steam, Epic Games, GOG, and Amazon directly on your device via Wine, Box64/FEX, and DXVK.
* **Cloud Save Sync**: Synchronize your game saves seamlessly between PC and mobile.
* **Input Customization**: Full gamepad support, virtual touch controls, and on-screen HUD.
* **Compatibility Profiles**: Pre-tuned container settings applied per game.

---

## 🛠️ Building from Source

### Prerequisites

* **JDK 21** (e.g. Eclipse Temurin 21)
* **Android SDK** (API 35/36, CMake, NDK)

### Build Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Compute64bits/HyperNative.git
   cd HyperNative
   ```

2. **Configure `local.properties` (Required):**
   ```bash
   echo "sdk.dir=$HOME/Android/Sdk" > local.properties
   ```

   *(Optional) Add your SteamGridDB API key for automatic cover scraping:*
   ```bash
   echo "STEAMGRIDDB_API_KEY=your_api_key_here" >> local.properties
   ```


3. **Build the APK:**
* **Debug build:**
   ```bash
  ./gradlew :app:assembleModernDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx3584m"
   ```


* **Release build (Low-RAM configuration):**
   ```bash
  ./gradlew :app:assembleModernRelease --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx3072m" -x lint -x lintVitalAnalyzeModernRelease -x test
   ```
APK binaries will be generated in `app/build/outputs/apk/modern/`.

---

## 🔒 Privacy & Data Policy

HyperNative values complete user privacy:

* **No analytics collectors**: No telemetry frameworks are bundled or initialized.
* **No session logging**: Game launch statistics, FPS metrics, and device IDs are never sent to external servers.
* **Local authentication**: All OAuth credentials and refresh tokens remain securely inside Android's private internal storage (`filesDir`).

---

## 📄 Credits & License

* HyperNative is based on **[GameNative](https://github.com/utkarshdalal/GameNative)** by Utkarsh Dalal and contributors.
* Licensed under the **[GNU General Public License v3.0 (GPLv3)](https://www.google.com/search?q=LICENSE)**.
* See `THIRD_PARTY_NOTICES` for open-source licenses and notices of bundled dependencies (Wine, DXVK, Mesa, Box64, FEX, etc.).

---

**Disclaimer:** *HyperNative is an independent open-source project and is not affiliated with Valve, Epic Games, CD PROJEKT RED, or Amazon. This tool is intended exclusively for running games you legally own.*
