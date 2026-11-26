# Mishkat Android

A standalone Android application for the existing Mishkat website:

https://mishkatt.framer.website

The app is intended to preserve the complete website experience while adding carefully integrated native Android capabilities.

## Implemented in the repository

- Android application scaffold (Kotlin, AndroidX, Gradle).
- WebView entry point configured for JavaScript, DOM storage, media playback initiated by the user, and saved navigation state.
- Notification permission request on Android 13 and later.
- Reminder alarm, notification receiver, and reboot receiver infrastructure.
- GitHub Actions workflow to attempt a debug APK build on pushes to `main`.

## Not complete yet

- The exact 40 Quran passages and 10 user-approved adhkar have not yet been inserted. The data file intentionally remains empty rather than risk altering sacred text or inventing missing wording.
- The actual website audio player and stream URLs have not yet been inspected. Native Media3/MediaSession background playback is therefore not implemented; adding a disconnected media service would not make the website's current player continue reliably.
- No launcher artwork has been supplied yet. The SVG icon will be integrated at the final branding step.
- A successful CI build and real-device functional tests have not yet been confirmed.

## Build

The GitHub Actions workflow at `.github/workflows/android-build.yml` runs `gradle assembleDebug` with JDK 17 and Gradle 8.9, then uploads the debug APK as an Actions artifact if the build succeeds.

A successful build alone does not certify audio behavior, notification timing, reboot restoration, or preservation of all website features. Those require functional tests on a physical Android device.
