# WASLNI — Release Build Guide

> Step-by-step guide for building, signing, and testing the release build.

## Prerequisites

### 1. Generate a Release Keystore (one-time)
```bash
keytool -genkeypair \
  -alias waselni \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -keystore waselni-release.jks

# You'll be prompted for:
# - Keystore password (strong — store securely!)
# - Key alias: waselni
# - Key password (can be same as keystore)
# - Your name, organization, city, country
```

**CRITICAL:** Back up the keystore in multiple secure locations. If you lose it, you can NEVER update the app on the Play Store with the same package name.

### 2. Configure Signing (local.properties)
```properties
# android/local.properties
MAPBOX_ACCESS_TOKEN=pk.eyJ1Ijoi...
MAPBOX_DOWNLOADS_TOKEN=pk.eyJ1Ijoi...
WASLNI_KEYSTORE_FILE=/path/to/waselni-release.jks
WASLNI_KEYSTORE_PASSWORD=your_keystore_password
WASLNI_KEY_ALIAS=waselni
WASLNI_KEY_PASSWORD=your_key_password
```

### 3. Download google-services.json
1. Go to Firebase Console → Project Settings
2. Download `google-services.json` for `com.waslni.driver`
3. Place at `android/app/google-services.json`

---

## Build Steps

### Step 1: Clean + Test
```bash
cd android

# Clean previous builds
./gradlew clean

# Run all unit tests
./gradlew testDebugUnitTest

# All tests must pass before proceeding
```

### Step 2: Build Release AAB
```bash
# Build the release Android App Bundle
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

### Step 3: Verify the AAB
```bash
# Check the AAB is signed
jarsigner -verify app/build/outputs/bundle/release/app-release.aab

# Check AAB contents (optional — use bundletool)
java -jar bundletool.jar build-apks \
  --bundle=app-release.aab \
  --output=app-release.apks \
  --ks=/path/to/waselni-release.jks \
  --ks-key-alias=waselni

# Install on device for testing
java -jar bundletool.jar install-apks \
  --apks=app-release.apks
```

### Step 4: Test on Real Device
Install the release build on a real device and verify:
- [ ] App launches without crash
- [ ] Login works (against staging or production backend)
- [ ] Map loads
- [ ] GPS capture works
- [ ] Add/edit/delete customer works
- [ ] Start delivery + navigation works
- [ ] Offline mode + sync works
- [ ] No Log.d/Log.e output in logcat (debug logging should be stripped)
- [ ] Dark/Light mode toggle works
- [ ] No "DEBUG" watermark or debug indicators

### Step 5: Build APK for Internal Testing (optional)
```bash
# If you need an APK instead of AAB (for direct installation):
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Release Checklist

### Build Configuration
- [ ] `versionCode` incremented (each release must have a unique code)
- [ ] `versionName` updated (e.g., "1.0.0" → "1.0.1")
- [ ] `API_BASE_URL` points to `https://api.waslni.com/api/v1/` (not localhost)
- [ ] `isMinifyEnabled = true` (R8 code shrinking)
- [ ] `isShrinkResources = true` (unused resource removal)
- [ ] `isDebuggable = false` (no debug flag in release)
- [ ] ProGuard rules cover Hilt, Room, Retrofit, Serialization, Mapbox, Firebase
- [ ] Signing config reads from env vars / local.properties
- [ ] `google-services.json` present (Firebase Crashlytics)

### Code Quality
- [ ] All unit tests pass (`./gradlew testDebugUnitTest`)
- [ ] No `TODO` or `FIXME` in critical paths
- [ ] No hardcoded API keys or secrets in source
- [ ] No `Log.d()` / `Log.v()` calls in release code (use CrashReporter)
- [ ] No `BuildConfig.DEBUG` branches that leak in release

### Security
- [ ] Network security config enforces HTTPS in production
- [ ] Certificate pinning configured for `api.waslni.com`
- [ ] Tokens stored in EncryptedSharedPreferences (not plain SharedPreferences)
- [ ] ProGuard obfuscation active (classes renamed — harder to reverse-engineer)
- [ ] No source code or comments with internal server IPs/passwords

### Firebase
- [ ] `google-services.json` is for the production Firebase project
- [ ] Crashlytics enabled in Firebase Console
- [ ] Test crash: `CrashReporter.reportException(RuntimeException("test"))` → verify in dashboard

### Backend
- [ ] Production backend deployed and healthy (`curl https://api.waslni.com/health`)
- [ ] Database migrations applied
- [ ] Seed users created (admin + test driver)
- [ ] SSL certificate valid and not expiring soon
- [ ] Backups running (daily + weekly cron)

### Testing
- [ ] Release build installed on at least 2 real devices
- [ ] Full delivery flow tested (add customer → start delivery → navigate → complete)
- [ ] Offline mode tested (airplane mode → add customer → reconnect → sync)
- [ ] No crashes during 30-minute active use session
- [ ] Battery consumption within target (< 15% per hour)
- [ ] Real Device Test Plan sign-off (Phase 24)

---

## Version History

| Version | Code | Date | Notes |
|---------|------|------|-------|
| 1.0.0 | 1 | 2026-09-08 | MVP Launch — initial release |

### Versioning Convention
- **Major** (1.x.x): Breaking changes, major new features
- **Minor** (x.1.x): New features, backward-compatible
- **Patch** (x.x.1): Bug fixes, no new features
- **versionCode**: Always increment by 1 for each upload to Play Store

---

## CI/CD Release

The release process is automated via GitHub Actions (Phase 27):

```bash
# 1. Merge develop → main
git checkout main
git merge develop
git push origin main

# 2. Tag the release
git tag -a v1.0.0 -m "MVP Launch — Waselni v1.0.0"
git push origin v1.0.0

# 3. GitHub Actions automatically:
#    - Builds release AAB with signing
#    - Attaches AAB to GitHub Release
#    - Deploys backend to production
```

### Manual Release (if CI/CD is unavailable)
```bash
cd android

# Set signing env vars
export WASLNI_KEYSTORE_FILE=/path/to/waselni-release.jks
export WASLNI_KEYSTORE_PASSWORD=...
export WASLNI_KEY_ALIAS=waselni
export WASLNI_KEY_PASSWORD=...

# Build
./gradlew clean bundleRelease

# Verify
ls -la app/build/outputs/bundle/release/app-release.aab

# Upload to Play Console manually
```
