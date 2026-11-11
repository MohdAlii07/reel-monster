# Reel Monster

Reel Monster helps you become more aware of how much time you spend scrolling short videos. It tracks reels viewed and time spent in Instagram and YouTube, with a small floating counter while tracking is active.

## Features

- **Level 1 · Manual:** Tap the button to count a reel yourself.
- **Level 2 · Automatic:** Tracks supported apps and displays a draggable counter bubble.
- Shows today’s reel count and watch time.
- Shows reel counts for the last seven days.
- Uses a custom monster illustration and app icon.
- Stores tracking data locally on your phone.
- Requests Android’s screen-capture permission for automatic tracking.

## Privacy

Screen frames are sampled on your device while automatic tracking is running. The app does not save or upload those frames. Counts and watch time are stored locally in the app’s private storage.

## Build

1. Install Android Studio and the Android SDK.
2. Open this folder in Android Studio and allow it to sync.
3. Connect an Android phone or start an emulator.
4. Select **Run** in Android Studio.

To build a debug APK from PowerShell:

```powershell
.\gradlew.bat assembleDebug
