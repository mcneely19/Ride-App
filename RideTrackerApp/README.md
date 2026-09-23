# Ride Telemetry (Android)

A native Android port of your Onewheel VESC ride tracker — same 39-ride dataset, same Speed Efficiency / Temperature Efficiency charts (with the inverted axis so lower Wh/mi reads as "up"), same board specs, same "Log a ride" flow. Data is stored locally on the device with Room (SQLite), so it works fully offline and nothing needs a live connection.

## What's inside

- **Jetpack Compose** UI, Material 3, light/dark theming
- **Room database** stores rides and boards locally — fully offline, nothing leaves the device
- Board filter chips, trendline toggle, stat tiles, two Canvas-drawn scatter charts with least-squares trendlines, a scrollable ride log, and an "Add ride" bottom sheet that computes Wh/mi and estimated range live
- **📷 From screenshot** — pick a photo of a ride summary and the app runs on-device OCR (Google ML Kit's bundled text model — no network call, works in airplane mode) to guess the numbers and pre-fill the Add Ride form. OCR is never perfect, so it always opens as an editable form for you to check before saving, never a silent auto-save.
- **📄 Import JSON** — pick a `.json` file of ride records and bulk-import them straight into the local database. Re-importing the same file is safe: entries with an id that's already in the database are skipped, not overwritten.
- **⚙ Boards are fully user-managed** — no board is hardcoded. Tap "⚙ Boards" to add, edit, or delete a board (name, total battery Wh, an optional subtitle, and a color). A board can't be deleted while it still has rides logged against it. On a brand-new install with no boards yet, the app opens straight into a first-run setup screen instead of an empty dashboard — handy if you're setting this up for someone else's board rather than your own.

## How to build it

1. Install **Android Studio** (Koala or newer) if you don't have it: https://developer.android.com/studio
2. Unzip this project and open the `RideTrackerApp` folder with **File → Open** in Android Studio.
3. Let it sync — Android Studio will generate the Gradle wrapper automatically on first open (a "Gradle wrapper is missing" prompt may appear; accept it) and download the SDK/dependencies.
4. Plug in your phone (with USB debugging on) or start an emulator, then hit **Run**.

No signing setup is needed for a debug build — Run installs it straight to your device.

## Notes

- Minimum Android version: Android 8.0 (API 26).
- The app icon uses a placeholder system icon — swap in a real one later via **Image Asset Studio** (right-click `res` → New → Image Asset) if you want a custom one.
- **Upgrading from an earlier build that had hardcoded XRV/X7 boards:** your existing rides are untouched — the database migration adds the boards table and auto-fills XRV (648 Wh) and X7 (518 Wh) as real, editable board entries so nothing breaks. You can rename their subtitle/color or add more boards from "⚙ Boards" right away.
- A board's **name can't be changed** once it has rides logged against it (the name is how rides reference it) — subtitle and color can always be edited, and you can delete a board once it has no rides left.
