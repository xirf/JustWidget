# Just Widget

Just Widget is a minimal weather + clock widget built with Glance and Compose. It pulls
forecast data from Open-Meteo and can use device location (optional).

## Features
- Clean weather + clock widget
- 7-day forecast
- Periodic refresh via WorkManager (~30 minutes) plus manual refresh
- Optional location; falls back to Magetan when permission is denied
- Launcher icon can be hidden without removing the widget

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
Just Widget uses a launcher `activity-alias` (`LauncherAlias`) and toggles it on/off from
the settings screen. To hide the app from the launcher:
1. Open the app (or tap the widget to open settings).
2. Toggle **Hide app icon from launcher**.
3. To show it again, tap the widget to reopen settings and toggle it back on.

## Battery considerations
See [docs/battery-issues.md](docs/battery-issues.md) for background refresh and battery tips.
