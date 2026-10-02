# PulseCheck

PulseCheck is an Android safety app designed for solo travel and personal emergency response. It combines local account management, trusted contacts, a dead-man switch, SOS triggers, and emergency alert workflows into a single offline-first mobile app prototype.

> Safety note: this app is a prototype for testing and demonstration. It is not a substitute for emergency services, and alert flows should only be tested with consenting contacts.

## Features

### Core Security & Authentication
- Local registration and login using SQLite with salted SHA-256 password hashes
- Session persistence across app restarts
- Biometric authentication support

### Emergency Alert System
- Dead Man's Switch countdown on the home screen
- Hold-to-activate SOS button (volume button trigger removed)
- **5-second video recording segments** that auto-restart while button is held
- SMS emergency alerts to saved contacts with secure video links
- Location tracking and map-based location view
- Alert history, notifications, and settings

### Video Evidence System (NEW)
- **5-second continuous video segments** during SOS activation
- **AES-256-GCM encryption** with hardware-backed Android KeyStore
- **Local encrypted storage** before cloud upload
- **Automatic cloud backup** via Supabase with retry logic
- **Secure time-limited links** (72-hour expiry) sent via SMS
- WiFi-preference for uploads to save mobile data
- Video metadata tracking (duration, file size, sharing logs)

### Contact Management
- Trusted contacts management with add, edit, and delete actions
- Contact groups and relationship tracking
- Emergency contact verification

### Smart Delivery System
- **Carrier-specific SMS delays** (Globe, Smart, Sun, DITO detection)
- Email fallback for alert delivery
- Delivery status tracking and retry logic
- Anti-spam optimization

### Cloud Integration
- Supabase backend for video storage and user data sync
- Row Level Security (RLS) for data protection
- Real-time location updates during active alerts
- Optional Firebase integration for additional cloud features

## Tech stack

- **Language:** Kotlin
- **Platform:** Android (targeting API 35, minimum API 24)
- **Build System:** Gradle with Kotlin DSL
- **UI Framework:** Material 3 and AndroidX UI components
- **Local Storage:** SQLite and SharedPreferences
- **Cloud Backend:** Supabase (REST API)
- **Location Services:** Google Play Services Location
- **Video Recording:** CameraX / Camera2 API
- **Encryption:** Android KeyStore with AES-256-GCM
- **Networking:** OkHttp3 for HTTP requests
- **Email:** JavaMail / Android Mail API
- **Optional:** Firebase (Authentication, Firestore, Realtime Database, Cloud Messaging)

### Key Dependencies
```gradle
// Video Recording
implementation 'androidx.camera:camera-camera2:1.2.3'
implementation 'androidx.camera:camera-lifecycle:1.2.3'

// Encryption & Security
implementation 'androidx.security:security-crypto:1.1.0-alpha06'

// Networking
implementation 'com.squareup.okhttp3:okhttp:4.11.0'

// Email
implementation 'com.sun.mail:android-mail:1.6.7'

// JSON
implementation 'com.google.code.gson:gson:2.10.1'
```

## Project status

This repository is a working Android prototype with app screens and local data handling already implemented. It is suitable for local testing and feature validation on a physical device, but it is not a production-grade emergency system.

**Latest Update (2026-09-30)**: Complete frontend redesign with soft pink + coral design system. New Material3 UI with rounded components, improved accessibility, and consistent visual language.

## Requirements

- Android Studio Hedgehog or newer
- JDK 11+
- Android SDK Platform 35 installed
- A physical Android device recommended for SMS testing
- API 24+ emulator or device for running the app

## Quick start

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync and download dependencies.
4. Connect a real device or start an API 24+ emulator.
5. Run the app from Android Studio or build it with:

```bash
./gradlew assembleDebug
```

On Windows, use:

```bash
gradlew.bat assembleDebug
```

## App flow

The app starts at the splash screen and then routes through onboarding, registration/login, and the main home experience.

Common screens and flows include:

- Splash and onboarding
- Registration and login
- Home with emergency controls
- Location screen
- Contacts management
- Alert history and notifications
- Settings and user preferences

## Project structure

```text
PulseCheck/
├── app/
│   ├── src/main/kotlin/com/example/pulsecheck/   # Kotlin app logic and activities
│   ├── src/main/res/                             # Layouts, drawables, themes, and values
│   ├── src/main/AndroidManifest.xml             # App permissions and activity declarations
│   └── google-services.json                     # Firebase config for Android app
├── gradle/                                      # Gradle wrapper and version catalog
├── build.gradle.kts                             # Root Gradle config
├── settings.gradle.kts                          # Project settings
├── README.md                                    # Project overview
├── SETUP.md                                     # Setup and usage notes
├── gradlew                                      # Unix build wrapper
├── gradlew.bat                                  # Windows build wrapper
└── .gitignore
```

## Permissions used

The manifest requests the following runtime permissions:

- INTERNET
- ACCESS_FINE_LOCATION
- ACCESS_COARSE_LOCATION
- SEND_SMS
- VIBRATE
- WAKE_LOCK
- CALL_PHONE
- RECEIVE_BOOT_COMPLETED
- USE_BIOMETRIC
- USE_FINGERPRINT

For emergency SMS delivery and location features, test on a real device with the required permissions granted.

## Firebase and cloud setup

The project includes Firebase dependencies and a Google Services configuration file. Firebase is optional for the core local experience, but it can be enabled for cloud features such as anonymous auth, user data sync, and more advanced tracking workflows.

If you want to use Firebase:

1. Create an Android app in Firebase with the package name `com.example.pulsecheck`
2. Download `google-services.json` and place it in `app/`
3. Enable the services you want to use
4. Apply appropriate security rules for any cloud resources

If Firebase is unavailable, the app still runs in a local-first mode for registration, contacts, and SMS-based alerting.

## Testing and validation

Run the project checks from the repository root:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

## Known limitations

- SMS sending requires a real device with an active SMS plan
- Some location and cloud features depend on permissions and network access
- The app includes prototype UI and integration points for future enhancements
- This is not a production emergency or medical alert system

## Related docs

- [SETUP.md](SETUP.md) — setup, usage, and device testing notes

## License

There is currently no license file in the repository. Treat the project as all rights reserved unless a license is added later.
