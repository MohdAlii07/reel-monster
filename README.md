<<<<<<< HEAD
# Reel Monster

Native Android project for the Week 2 Reel Monster assignment. It includes the offline app shell, Level 1 manual counter, and an early Level 2 prototype with Android screen-capture consent, a local foreground service, a draggable bubble, usage-based app filtering, tracked time, and a vertical-swipe transition detector. The detector has not yet been tuned or validated against the PDF's 20-reel accuracy target.

## Project status

- Android project files are scaffolded at this local path.
- The app has a local manual counter, a level selector, and a seven-day count view.
- Counts use private app preferences and survive normal app restarts.
- The manifest intentionally has no `INTERNET` permission.
- Level 2 asks for overlay, usage access, notification, and per-session screen-capture consent. It samples the screen in memory only while Instagram or YouTube is foreground; frames are not saved or uploaded.
- The official Android command-line tools and Gradle 9.6.0 are installed locally; their downloads were checksum-verified.
- Android API 36, Build-Tools 36.0.0, and ADB platform-tools are installed.
- Debug APK built successfully at `app/build/outputs/apk/debug/app-debug.apk`.
- Installed and launched on the connected Samsung SM-A346E (Android device `a34x`) via ADB.
- The tracker uses adjacent-frame vertical movement to count after the first visible reel, reducing repeated counts from video scene changes. Detector accuracy still needs interactive checks.
- The current UI refresh adds a daily count hero with the supplied monster image, compact today/watch-time summaries, explicit Level 1 and Level 2 choices, and clearer tracking actions.
- The floating counter uses the supplied monster image with a compact count and hint. Its existing drag behavior is retained.
- The launcher icon uses the supplied phone-holding monster artwork, centered inside an adaptive icon. The visible sparkle watermark outside the rounded artwork is cropped away.
- Latest debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- This refreshed APK builds successfully and is installed on the authorized Samsung SM-A346E. The app was opened and visually checked on the phone; the top respects the camera/status-bar safe area, the bottom clears the navigation gesture area, and daily counts align in a separate column from dates.

## Brand assets

- `app/src/main/res/drawable-nodpi/monster_character.png` is the transparent mascot used in the app screen and floating counter.
- `app/src/main/res/drawable-nodpi/ic_launcher_art.png` is the centered, watermark-free crop used by the adaptive launcher icon.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` apply the artwork to Android's adaptive icon shape.
- The launcher artwork is declared as `android:icon` and `android:roundIcon` in `app/src/main/AndroidManifest.xml`. Updating the installed APK updates the app-drawer icon; if a home-screen shortcut retains a cached old image, remove and add the shortcut again from the app drawer.

## Build configuration

- Package: `com.ali.reelmonster`
- Minimum Android API: 26 (required for modern application overlays)
- Compile SDK: 36
- Target SDK: 36
- Android Gradle Plugin: 9.5.0
- Java/Kotlin bytecode target: 17

## Next milestones

1. On the phone, grant Level 2 permissions and check the overlay, notification, app filtering, and tracked time.
2. Compare the counter with ground truth over at least 20 reels; tune the vertical-swipe detector and record Instagram/YouTube behavior.
3. Verify manual and automatic counts after app and phone restarts, and check the seven-day view.

See [requirements.md](requirements.md) for the source requirements and acceptance checks, and [prompts.md](prompts.md) for the staged AI workflow.

The `.tools` folder is ignored by Git and holds the verified Android CLI download and extraction; it is not part of the app source. A copy of the CLI is also installed at `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`.
=======

>>>>>>> fd2d4c6e47eabd51fa852033c816eaae2de2212a
