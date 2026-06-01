# Battery considerations

Just Widget refreshes weather in the background to keep the widget up to date. This is
useful, but it can also affect battery life depending on device settings and usage.

## What impacts battery
- **Periodic refresh**: `WeatherWorker.schedulePeriodic` runs roughly every 30 minutes
  when at least one widget exists.
- **Location lookup**: the app uses `FusedLocationProviderClient` with
  `PRIORITY_BALANCED_POWER_ACCURACY` to resolve the current place.
- **Network requests**: each refresh calls the Open-Meteo API.

## Device and OS behavior
- **Doze / App Standby** can defer background work, so updates might be delayed.
- **OEM battery optimizations** can throttle background work and location updates.
- Removing the last widget stops periodic work (`WeatherWidgetReceiver.onDisabled`).

## Mitigations
- **Disable location permission** if you want to avoid location lookups (the widget
  falls back to Magetan).
- **Use manual refresh** from the settings screen when you need an immediate update.
- **Adjust the refresh interval** in `WeatherWorker` if you fork the project.
- **Allow background activity** or disable battery optimization for more consistent
  updates on devices that aggressively limit background work.

## Troubleshooting checklist
- Widget is stale: check battery optimization settings and confirm the app has network
  access.
- Battery drain: disable location permission or remove the widget.
