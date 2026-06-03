# Just Widget

Just Widget is a minimal weather + clock widget built with Glance and Compose. It pulls
forecast data from Open-Meteo and can use device location (optional).

## Features
- Clean weather + clock widget
- 7-day forecast
- Periodic refresh via WorkManager (~30 minutes) plus manual refresh
- Optional location; falls back to Magetan when permission is denied
- Launcher icon can be hidden without removing the widget
- Customizable color mode, background style (glass/transparent), highlight color, text color, and custom location settings

## Downloads & Releases

### v1.1.0 Release
- **APK Download**: [app-debug.apk](https://github.com/xirf/JustWidget/releases/download/v1.1.0/app-debug.apk)
- **SHA-256 Checksum**: `A5DAF81626BC239973783E3EF9ECCA8B17DE1C0021D2556567225B02A87B75F7`

## Installation Guide

### Option 1: Using the Pre-built APK (On Device)
1. Download the latest `app-debug.apk` from the [Releases](https://github.com/xirf/JustWidget/releases) page.
2. If prompted, enable **Install from Unknown Sources** in your Android system settings for your browser/file manager.
3. Open the downloaded APK and tap **Install**.

### Option 2: Installing via ADB (Developer Mode)
If you have ADB set up on your computer and developer options enabled on your device:
1. Connect your device to your computer via USB or Wi-Fi debugging.
2. Build and install using Gradle:
   ```bash
   ./gradlew installDebug
   ```
   Or install the built APK directly via ADB:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Adding the Widget to Your Home Screen
1. Long-press an empty space on your home screen.
2. Select **Widgets** (or **Plugins/Cards** depending on your launcher).
3. Scroll down to locate **Just Widget**.
4. Drag and drop the **WeatherWidget** onto your home screen.
5. Tap the widget to configure colors, background transparency, highlight colors, and your desired location!

## Project structure
- `app/` – Android app + widget
- `docs/` – additional documentation

## Requirements
- Android Studio (or JDK 17 + Android SDK)
- Android SDK 36 (compile/target)

## Build
```bash
./gradlew assembleDebug
```

## Permissions
- `INTERNET` (required)
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` (optional)

## Hiding the app icon
Just Widget uses a launcher `activity-alias` (`LauncherAlias`) and toggles it on/off from the settings screen. To hide the app from the launcher:
1. Open the app (or tap the widget to open settings).
2. Toggle **Hide app icon from launcher**.
3. To show it again, tap the widget to reopen settings and toggle it back on.

## Battery considerations
See [docs/battery-issues.md](docs/battery-issues.md) for background refresh and battery tips.

## License

This project is licensed under the MIT License:

```text
MIT License

Copyright (c) 2026 JustWidget Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
