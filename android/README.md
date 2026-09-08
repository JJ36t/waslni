# Waselni — Android App

تطبيق المندوب — Offline-first delivery navigation app for Android.

## Setup

### Requirements
- Android Studio Hedgehog (or newer)
- JDK 17
- Android SDK 34
- Kotlin 1.9.24

### Mapbox token
1. Get a Mapbox access token from https://account.mapbox.com/
2. Create `local.properties` in the `android/` directory:
```properties
MAPBOX_ACCESS_TOKEN=pk.eyJ1Ijoi...
```
3. Also set the Mapbox downloads token in `gradle.properties` (or `~/.gradle/gradle.properties`):
```properties
MAPBOX_DOWNLOADS_TOKEN=pk.eyJ1Ijoi...
```

## Build
```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew ktlintCheck
```

## Architecture
- Clean Architecture + MVVM
- Jetpack Compose + Material 3
- Hilt for DI
- Room for local DB (Phase 3)
- Retrofit for networking (Phase 11)
- Mapbox for maps (Phase 5)

See `../docs/02-architecture.md` for full details.

## Project structure
```
app/src/main/java/com/waslni/driver/
├── core/          # Shared infrastructure
│   ├── common/    # Utilities, constants
│   ├── database/  # (Phase 3) Room
│   ├── location/  # (Phase 4) GPS
│   ├── maps/      # (Phase 5) Mapbox
│   ├── network/   # (Phase 11) Retrofit
│   ├── security/  # (Phase 11) SecureStorage
│   └── ui/        # Theme, common components
├── data/          # (Phase 3+) Data layer
├── domain/        # (Phase 3+) Domain layer
├── di/            # Hilt modules
└── presentation/  # UI (Compose screens)
```

## Current Phase
**Phase 2 — Android Foundation** ✅

Project skeleton with:
- Compose + Material 3 + Hilt + Navigation
- Theme (Dark/Light) + Arabic RTL
- Placeholder screens for all main destinations
