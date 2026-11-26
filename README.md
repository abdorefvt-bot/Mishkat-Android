# Mishkat Android

Android WebView wrapper for the live Mishkat website:

https://mishkatt.framer.website

The application loads the website at runtime, so website-side updates are delivered by the live site without rebuilding the APK. The Android layer adds hourly reminders and background-playback support around the website's media.

## Android behavior

- Loads the live website in a WebView with JavaScript and DOM storage enabled.
- Preserves the website's own responsive layout; Android handles the status-bar inset natively.
- Requests notification permission on Android 13 and later.
- Schedules one reminder per hour, alternating from dhikr to Quran passage.
- Reschedules reminders after device reboot, app replacement, and exact-alarm permission changes.
- Uses a dedicated monochrome Mishkat status icon and the notification title "Mishkat".
- Starts a media-playback foreground service while website audio is active and avoids pausing WebView media when the Activity moves to the background.

## Build and validation

GitHub Actions runs the JVM unit tests and assembles a debug APK on pushes to `main`.

A successful CI build verifies compilation and the automated tests; it does not replace real-device checks. In particular, background audio behavior, OEM battery restrictions, notification permission, exact-alarm permission, and layout on specific phone/tablet/desktop-sized screens must be tested on physical devices. Android APKs run on Android phones and tablets; a desktop browser can load the website itself, but an APK is not a native desktop application.

## Notes

The app does not bundle a static copy of the website. It loads the live URL above, so website updates appear when the WebView next loads or refreshes the site.
