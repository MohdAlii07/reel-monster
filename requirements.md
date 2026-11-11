
### `requirements.md`

```markdown
# Reel Monster — Requirements

## Purpose

Help users notice their short-video scrolling habits by counting reels and tracking time spent in supported apps.

## Features

### Level 1: Manual counting

- Let the user select manual mode.
- Let the user tap a control to add one reel to today’s count.
- Show the updated count on the dashboard.

### Level 2: Automatic tracking

- Let the user request automatic tracking.
- Explain Android permissions before they are needed.
- Track only supported short-video apps while they are in the foreground.
- Show a draggable floating counter while tracking is active.
- Allow the user to stop tracking.

### Dashboard

- Show today’s reel count.
- Show tracked time for today.
- Show counts for the previous seven days.
- Display the supplied monster artwork and app icon.

### Local data

- Store counts and tracked time on the device.
- Keep stored data available after the app closes and reopens.
- Do not upload screen frames or usage information.

## Interaction notes

- Manual mode must remain usable without screen-capture permission.
- Automatic tracking must only run after the user grants Android’s requested permissions.
- Make the active tracking state and stop action easy to find.

## Acceptance checklist

- [ ] The user can manually add a reel.
- [ ] The dashboard updates after a manual count.
- [ ] The app displays today’s tracked time and seven-day history.
- [ ] The user can start and stop automatic tracking.
- [ ] The floating counter appears during an automatic tracking session and can be moved.
- [ ] The app only tracks supported apps while they are in the foreground.
- [ ] Counts remain after the app is closed and reopened.
- [ ] The app works without an internet connection.
