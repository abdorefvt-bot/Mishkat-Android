# Mishkat Android

A standalone Android application wrapping the existing Mishkat website without changing its content or behavior, with native background audio and scheduled reminder support planned as first-class Android components.

## Project status

Initial repository setup. This is not yet a release APK. The implementation and device-level validation are still in progress.

## Requirements

- Preserve the complete website experience in a WebView.
- Keep existing prayer times, prayer tracking, recitations, supplications, and other site features intact.
- Implement reliable background playback using Android media foreground-service APIs and MediaSession/Media3 after verifying the website's actual audio playback path.
- Schedule alternating Quran passage and dhikr reminders every two hours, persist sequence state, and restore schedules after reboot.
- Add the supplied transparent SVG as the launcher icon only at the final branding step.

## Build

Open this project in Android Studio with a compatible Android Gradle Plugin/Gradle installation. A release APK must not be considered complete until it builds successfully and the core behaviors are tested on a physical device.