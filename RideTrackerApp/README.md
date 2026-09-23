# Ride Telemetry (Android)

A native Android port of your Onewheel VESC ride tracker — same 39-ride dataset, same Speed Efficiency / Temperature Efficiency charts (with the inverted axis so lower Wh/mi reads as "up"), same board specs, same "Log a ride" flow. Data is stored locally on the device with Room (SQLite), so it works fully offline and nothing needs a live connection.

## What's inside

- **Jetpack Compose** UI, Material 3, matching the web tracker's palette (teal for XRV, burnt orange for X7) and light/dark theming
- **Room database** seeded on first launch with all 39 rides (identical to the web version, including the Sep 7 XRV ride)
- Board filter chips, trendline toggle, stat tiles, two Canvas-drawn scatter charts with least-squares trendlines, a scrollable ride log, and an "Add ride" bottom sheet that computes Wh/mi and estimated range live

## How to build it

1. Install **Android Studio** (Koala or newer) if you don't have it: https://developer.android.com/studio
2. Unzip this project and open the `RideTrackerApp` folder with **File → Open** in Android Studio.
3. Let it sync — Android Studio will generate the Gradle wrapper automatically on first open (a "Gradle wrapper is missing" prompt may appear; accept it) and download the SDK/dependencies.
4. Plug in your phone (with USB debugging on) or start an emulator, then hit **Run**.

No signing setup is needed for a debug build — Run installs it straight to your device.

## Notes

- Minimum Android version: Android 8.0 (API 26).
- The app icon uses a placeholder system icon — swap in a real one later via **Image Asset Studio** (right-click `res` → New → Image Asset) if you want a custom one.
- The two boards and their pack sizes (XRV 648 Wh, X7 518 Wh) are defined in `data/Ride.kt` — GT-S isn't included, matching the current web tracker.
- To keep this in sync with future rides, tell me about them the same way you have been — I'll add them to `data/SeedData.kt` and send you an updated project (or, if you'd rather, add an "export/import" flow so you can move data between the phone and me directly; just ask).
