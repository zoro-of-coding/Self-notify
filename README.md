# Self-notify

Send a notification to yourself, with a custom **title** and **text**, right now or **scheduled** for any future date & time.

## Features

- **Set the title and text** of the notification before sending.
- **Send now** — shows the notification immediately.
- **Schedule (one-time)** — pick a date & time and the notification fires then, even if the app is closed.
- **Notification "types"** via Android notification channels:
  - *Default* — normal priority
  - *High priority (heads-up)* — pops up on screen
  - *Low priority (quiet)* — no heads-up, ordered below others
  - *Silent* — plays no sound at all
- Tapping a notification opens the app.
- Scheduled notifications are saved and **survive reboots** (re-armed via `BootReceiver`).
- A list of upcoming notifications is shown in the app, with a **Cancel all** button.

## How to build & run

1. Open the project folder in **Android Studio** (it syncs Gradle automatically).
2. Connect an Android device (with USB debugging) or start an emulator.
3. Press **Run** ▶ or run:

   ```bash
   gradlew.bat installDebug
   ```

## Required permissions (explained)

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Android 13+ asks at runtime to allow showing notifications. |
| `SCHEDULE_EXACT_ALARM` | Android 12+ may ask to allow "Alarms & reminders" so scheduled times are exact (the app prompts you to open the right Settings screen). |
| `RECEIVE_BOOT_COMPLETED` | Re-schedules your saved notifications after a reboot. |

## Tech notes

- Kotlin + XML views (ViewBinding), Material 3, min SDK 26 (Android 8.0).
- Scheduling uses `AlarmManager.setExactAndAllowWhileIdle` with graceful fallback if exact alarms are not granted.
- Pending schedule is stored in `SharedPreferences` as JSON and cleaned up as each notification fires.
