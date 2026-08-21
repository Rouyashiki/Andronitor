# Andronitor

Andronitor is a native Android application for tracking and analyzing X/Twitter follower relationships.

It reads the account relationship lists using an X session cookie, stores local snapshots in SQLite, and reports profile changes, new relationships, unfollows, mutual changes, suspensions, and deactivations. Account data, relationship history, media history, and settings remain on the device unless the user explicitly exports a backup.

## Features

- Multiple X accounts with encrypted session-cookie storage.
- Followers/following synchronization through the X REST and GraphQL endpoints.
- Local relationship snapshots and change reports.
- User search, profile details, avatar/banner history, and ZIP backup/restore.
- Compose UI with light/dark system themes and phone-friendly navigation.

## Xposed cookie import (optional)

The Andronitor APK doubles as an Xposed/LSPosed module. When enabled, it captures the X web-session cookie directly from the official X app instead of requiring a manual paste or in-app browser login.

1. Install the Andronitor APK on a device with the Xposed framework (LSPosed).
2. In LSPosed, enable the **Andronitor** module and set its scope to the **X** app (`com.twitter.android`). Force-stop X afterward so the hook loads.
3. In Andronitor, open **Accounts → Add account → Import from X app**, then use the X app. Once X materializes a web session (`auth_token` / `ct0` / `twid`), the cookie is forwarded to Andronitor and the account is added automatically.

The hook reads only the framework `CookieManager` cookie jar for `x.com`/`twitter.com` inside the X process and broadcasts a complete cookie to Andronitor (addressed by package, guarded by a shared secret). Note: the X app authenticates natively with OAuth 1.0a and only populates a web cookie during web-content flows (e.g. login); if no such flow occurs, no cookie is captured and the manual/browser paths remain available.

## Build

Open the project in Android Studio, or run:

```text
gradlew.bat :app:assembleDebug
```

The source tree is Kotlin-only and uses Jetpack Compose with a JDK 17+ build toolchain, compile SDK 37, and Android API 26 or newer. No server or companion runtime is required.

The Android data and media stack uses OkHttp for HTTP, Room for SQLite access, Moshi for local JSON serialization, and Coil for image loading.

## License

This project is licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE).
