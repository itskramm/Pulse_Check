# PulseCheck — Setup Guide

## No API Key Required
This app uses **local SQLite authentication** — no Firebase, no internet connection needed
for login or registration. Everything is stored on-device.

---

## Prerequisites
- Android Studio Hedgehog or newer
- Android device or emulator (API 24+)

---

## Build & Run

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Run on a device or emulator — that's it

---

## How the App Works

### Authentication (fully offline)
- **Register**: Enter full name, phone number (10+ digits), password (6+ chars)
  - Password is hashed with SHA-256 + random salt before storing
  - Stored in local SQLite database on the device
- **Login**: Enter the same phone + password
  - Verified against the local database — no internet needed
- **Session**: Login state persists across app restarts via SharedPreferences

### Dead Man's Switch
- **Press and hold** the PulseCheck logo on the home screen
- **Release** → countdown starts (default 3 seconds, configurable)
- **Press again** during countdown → cancels the alert
- **Countdown reaches zero** → SMS sent to all emergency contacts

### Volume Button SOS
- Press **Volume Down 3 times within 2 seconds** → triggers SOS countdown
- A dialog appears with a Cancel option

### Emergency Contacts
- Go to **Contacts** tab → **+ Add Contacts**
- Fill in name and phone number (required)
- Stored in local SQLite database
- Long-press a contact to delete it

### Dark Mode
- Tap the **Settings icon** (top-left on home screen)
- Toggle **Dark mode** → applies immediately and persists across restarts

### Absence Duration
- Tap **Settings** → **Absence duration**
- Choose 3, 5, 7, or 10 seconds for the countdown before SOS fires

### Navigation
- Bottom nav bar: **Home → Location → Contacts → History**

---

## SMS Permissions
- Grant **SEND_SMS** permission when prompted on first launch
- Test on a real device for SMS to actually send (emulators cannot send real SMS)

---

## Known Limitations
- **Location screen**: Requires a Google Maps API key to show a live map
- **History screen**: Currently shows static demo data
- **Biometric login**: Toggle is present in UI; full implementation is a future enhancement
