# Reel Monster — Requirements

## Purpose

Reel Monster helps users become more aware of their short-video viewing habits by counting reels and tracking time spent in supported apps.

## Features

### Level 1: Manual counting

- Let the user select manual mode.
- Let the user tap a button to count one reel.
- Update today’s count on the dashboard.

### Level 2: Automatic tracking

- Let the user start automatic tracking after granting the Android permissions it needs.
- Track supported apps only while they are in the foreground.
- Show a draggable floating counter while tracking is active.
- Let the user stop tracking.

### Dashboard

- Show today’s reel count and tracked time.
- Show reel counts for the last seven days.
- Display the monster artwork and app icon.

### Local data

- Store counts and tracked time on the phone.
- Keep the data after the app closes and reopens.
- Do not upload or save captured screen frames.

## Acceptance checklist

- [ ] Manual mode adds one reel when tapped.
- [ ] The dashboard updates after a manual count.
- [ ] Today’s count, tracked time, and seven-day history are visible.
- [ ] Automatic tracking can be started and stopped.
- [ ] The draggable counter appears during tracking.
- [ ] Tracking only runs for supported apps in the foreground.
- [ ] Counts remain after closing and reopening the app.
- [ ] The app works without an internet connection.
