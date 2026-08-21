# Andronitor

Andronitor is a native Android application for tracking and analyzing X/Twitter follower relationships.

It reads the account relationship lists using an X session cookie, stores local snapshots in SQLite, and reports profile changes, new relationships, unfollows, mutual changes, suspensions, and deactivations. Account data, relationship history, media history, and settings remain on the device unless the user explicitly exports a backup.

## Features

- Multiple X accounts with encrypted session-cookie storage.
- Followers/following synchronization through the X REST and GraphQL endpoints.
- Local relationship snapshots and change reports.
- User search, profile details, avatar/banner history, and ZIP backup/restore.
- Compose UI with light/dark system themes and phone-friendly navigation.

## Build

Open the project in Android Studio, or run:

```text
gradlew.bat :app:assembleDebug
```

The source tree is Kotlin-only and uses Jetpack Compose with a JDK 17+ build toolchain, compile SDK 37, and Android API 26 or newer. No server or companion runtime is required.

The Android data and media stack uses OkHttp for HTTP, Room for SQLite access, Moshi for local JSON serialization, and Coil for image loading.

## License

This project is licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE).
