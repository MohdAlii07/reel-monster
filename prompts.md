# Reel Monster build prompts

Keep prompts small. Ask the coding agent to inspect the existing files, modify only the current milestone, explain what changed, and list any behavior that is not yet tested on the phone.

## Milestone 1 — App shell

> Inspect the Reel Monster requirements and the existing Android project. Build only the native Kotlin app shell, a local Level 1 counter, level selector, and seven-day dashboard. Do not claim Level 2, overlay, or time tracking works yet. Keep the app offline with no login. Explain every file changed and how to build/install it with the Android command line.

## Milestone 2 — Overlay

> Add the draggable monster bubble using Android's application overlay permission. Ask the user to grant the permission from the app's setup screen. Show the counter and growth states. Keep Level 1 tap-to-count working, persist changes locally, and explain how to test the bubble over Instagram and YouTube.

## Milestone 3 — Tracking session and local time

> Add a user-started tracking session with a visible notification and local elapsed-time accounting for Instagram and YouTube. Do not record or upload screen frames. Keep all counts and summaries on device, and explain permission requirements and device-specific limits.

## Milestone 4 — Level 2 detector

> Implement automatic new-reel detection as a separate, testable component for Instagram Reels and YouTube Shorts. Use explicit user-approved screen capture, process frames locally, discard them immediately, and guard against counting normal video motion as new reels. Add a debug counter/log that stores only timestamps and detector decisions. Do not report Level 2 as complete until phone tests show an error of at most two over more than 20 reels in both apps.
