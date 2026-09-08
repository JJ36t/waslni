# WASLNI — Developer Setup Guide

> Get a new developer from zero to running in under 30 minutes.

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Android Studio | Hedgehog+ | https://developer.android.com/studio |
| JDK | 17 | Bundled with Android Studio |
| Python | 3.11+ | https://python.org |
| Docker | 24+ | https://docker.com |
| Docker Compose | 2.20+ | Bundled with Docker Desktop |
| PostgreSQL | 15+ (or use Docker) | https://postgresql.org |
| Git | 2.40+ | https://git-scm.com |

## Step 1: Clone the Repository

```bash
git clone https://github.com/your-org/waslni.git
cd waslni
```

## Step 2: Backend Setup

### 2.1 Configure environment
```bash
cd backend
cp .env.example .env
```
Edit `.env`:
```ini
JWT_SECRET=dev-secret-change-me-32-chars-min
POSTGRES_PASSWORD=waselni
DATABASE_URL=postgresql+asyncpg://waselni:waselni@localhost:5432/waslni
```

### 2.2 Start PostgreSQL + FastAPI (Docker)
```bash
docker compose up -d
```
Verify:
```bash
curl http://localhost:8000/health
# {"status":"ok","db":"ok","env":"development","version":"1.0.0"}
```

### 2.3 Run migrations + seed
```bash
docker compose exec backend alembic upgrade head
docker compose exec backend python -m scripts.seed
```

### 2.4 (Alternative) Run backend locally without Docker
```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt -r requirements-dev.txt
# Ensure PostgreSQL is running on localhost:5432
alembic upgrade head
python -m scripts.seed
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 2.5 Run backend tests
```bash
# Create test database (once)
psql -U postgres -c "CREATE DATABASE waslni_test;"
psql -U postgres -c "GRANT ALL ON DATABASE waslni_test TO waselni;"

# Run tests
pytest
```

### 2.6 API documentation
Open http://localhost:8000/docs for Swagger UI.

## Step 3: Android Setup

### 3.1 Get Mapbox tokens
1. Create a free account at https://account.mapbox.com
2. Copy your access token (starts with `pk.`)
3. Also get a secret downloads token for the Maven repo

### 3.2 Configure local.properties
```bash
cd android
cp local.properties.example local.properties
```
Edit `local.properties`:
```properties
MAPBOX_ACCESS_TOKEN=pk.eyJ1Ijoi...
MAPBOX_DOWNLOADS_TOKEN=pk.eyJ1Ijoi...
```

### 3.3 Firebase setup (for Crashlytics)
1. Create a Firebase project at https://console.firebase.google.com
2. Add Android app → package `com.waslni.driver`
3. Download `google-services.json` → place at `android/app/google-services.json`
4. (Optional for dev — Crashlytics will gracefully no-op without it)

### 3.4 Open in Android Studio
```bash
# From Android Studio: File → Open → select waslni/android/
# Wait for Gradle sync to complete
# Click Run (▶) — app launches on emulator or device
```

### 3.5 (Alternative) Build from command line
```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### 3.6 Run Android tests
```bash
./gradlew testDebugUnitTest
```

## Step 4: Development Workflow

### Branching
```bash
git checkout develop
git pull origin develop
git checkout -b feature/your-feature
# ... code ...
git push origin feature/your-feature
# Open PR to develop
```

### Commit Convention (Conventional Commits)
```
feat(customer): add duplicate phone detection
fix(sync): handle conflict when server has newer version
docs(api): add idempotency-key section
test(auth): add token refresh tests
```

### Backend Development
```bash
# Start backend with hot reload
docker compose up -d
# Or: uvicorn app.main:app --reload

# Run a specific test
pytest tests/test_auth.py::TestLogin

# Lint
ruff check .
black --check .

# Create a new migration
alembic revision --autogenerate -m "add new column"
alembic upgrade head
```

### Android Development
```bash
# Run specific test
./gradlew testDebugUnitTest --tests "com.waslni.driver.data.local.dao.CustomerDaoTest"

# Build debug APK
./gradlew assembleDebug

# Build release AAB (needs signing config)
./gradlew bundleRelease
```

## Step 5: Common Issues

### "Mapbox SDK not downloading"
Ensure `MAPBOX_DOWNLOADS_TOKEN` is set in `~/.gradle/gradle.properties`:
```properties
MAPBOX_DOWNLOADS_TOKEN=pk.eyJ1Ijoi...
```

### "google-services.json not found"
Download from Firebase Console → place at `android/app/google-services.json`.
For dev without Firebase, the app still works — Crashlytics gracefully no-ops.

### "Database connection refused"
```bash
# Check PostgreSQL is running
docker compose ps
# Or: pg_isready -h localhost -p 5432

# Recreate the database
docker compose down -v
docker compose up -d
docker compose exec backend alembic upgrade head
```

### "Tests fail with 'database waslni_test does not exist'"
```bash
psql -U postgres -c "CREATE DATABASE waslni_test;"
psql -U postgres -c "CREATE USER waselni WITH PASSWORD 'waselni';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE waslni_test TO waselni;"
```

### "Emulator can't reach backend"
The Android emulator uses `10.0.2.2` for the host machine's localhost.
The debug build's `API_BASE_URL` is already set to `http://10.0.2.2:8000/api/v1/`.

## Step 6: Project Resources

| Resource | Link |
|----------|------|
| Architecture | [docs/02-architecture.md](docs/02-architecture.md) |
| API Contract | [docs/04-api-contract.md](docs/04-api-contract.md) |
| Database Schema | [docs/03-database.md](docs/03-database.md) |
| Testing Strategy | [docs/08-testing-strategy.md](docs/08-testing-strategy.md) |
| CI/CD Pipeline | [docs/11-cicd-pipeline.md](docs/11-cicd-pipeline.md) |
| Security Checklist | [docs/07-security-checklist.md](docs/07-security-checklist.md) |

## Need Help?

- Backend issues: check `docker compose logs backend`
- Android issues: check `adb logcat | grep Waselni`
- Sync issues: check WorkManager logs: `adb logcat | grep SyncWorker`
- General: support@waslni.com
