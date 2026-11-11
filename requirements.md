# Reel Monster Level 2 requirements

## Source requirements from Week 2

- Install as a real Android app.
- Show a cute monster bubble over Instagram and YouTube.
- Show a reel counter on the bubble.
- Offer Level 1 manual and Level 2 automatic counting.
- Grow the monster and show funny lines as the count rises.
- Show today's count, time lost, and the last seven days.
- Keep the count after phone restart.
- In Level 2, count more than 20 reels with an error of no more than two.
- Work without internet and without login.

## Local implementation decisions

- Kotlin Android app, built with the command-line Android SDK and Gradle.
- No INTERNET permission. Counts and history stay on this phone.
- Start with the simple counter and installable app shell; add the overlay next.
- Use an explicit, user-started screen-capture session for automatic detection. Process frames on-device and do not save or upload them.
- Keep Level 1 as a test mode and fallback while Level 2 detection is being calibrated.
- Define “time lost” as tracked foreground time in Instagram and YouTube during a tracking session. This is a product interpretation because the PDF doesn't define its calculation.

## Acceptance checks

1. Install and launch on a real Android phone.
2. Grant overlay access and confirm the bubble appears over both target apps.
3. Verify the bubble count and local dashboard update.
4. Restart the phone and confirm saved counts remain.
5. Disable internet and verify the app's local features continue working.
6. Manually record a sequence of at least 21 reels in each target app; automatic total must differ by at most two per test sequence.

The Level 2 detector is not complete until the last check passes on the user's phone.
