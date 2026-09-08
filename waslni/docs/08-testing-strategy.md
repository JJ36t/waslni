# WASLNI — Testing Strategy

> Comprehensive testing coverage for the WASLNI delivery navigation app.

## Overview

The project follows a layered testing strategy:

| Layer | Android | Backend | Purpose |
|-------|---------|---------|---------|
| **Unit Tests** | 30+ test files | 13 test files | Business logic in isolation |
| **Integration Tests** | Robolectric DAO tests | httpx + TestClient | Data layer + API endpoints |
| **Security Tests** | — | 22 audit tests | Token tampering, SQL injection, cross-driver |
| **UI Tests** | (Planned: Phase 24) | — | Compose UI flows |
| **Real Device** | (Planned: Phase 24) | — | GPS, battery, offline |

## Backend Tests

### Test Files (13 files, 150+ test methods)

| File | Tests | Coverage |
|------|-------|----------|
| `test_config.py` | 6 | Settings defaults, CORS parsing, env flags |
| `test_exceptions.py` | 12 | Error hierarchy, status codes, format_error_response |
| `test_security.py` | 10 | Argon2 hashing, JWT create/decode, tampered/expired tokens |
| `test_health.py` | 3 | /health, /api/v1/health, /openapi.json |
| `test_auth.py` | 18 | Login, refresh, logout, /me, rate limiting |
| `test_customers.py` | 21 | CRUD, pagination, search, authorization |
| `test_deliveries.py` | 18 | CRUD, state machine, idempotency, authorization |
| `test_sync.py` | 17 | Batch processing, conflict resolution, server_changes |
| `test_security_audit.py` | 22 | Headers, token tampering, cross-driver, SQL injection, validation |
| `test_customer_service.py` | 13 | Service-layer create/get/update/delete/list |
| `test_delivery_service.py` | 11 | State machine, idempotency, active delivery check |
| `conftest.py` | — | Fixtures: db_session, client, test_user, auth_headers |

### Running Backend Tests

```bash
cd backend

# Create test database (once)
psql -U postgres -c "CREATE DATABASE waslni_test;"
psql -U postgres -c "CREATE USER waselni WITH PASSWORD 'waselni';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE waslni_test TO waselni;"

# Run all tests with coverage
pytest

# Run specific test class
pytest tests/test_auth.py::TestLogin

# Run with verbose output
pytest -v

# Security audit only
pytest tests/test_security_audit.py

# Coverage report
pytest --cov=app --cov-report=term-missing
# HTML report: open htmlcov/index.html
```

### Coverage Configuration

- `.coveragerc` — excludes `__init__.py`, `main.py`, `logging.py`
- `pytest.ini` — `--cov=app --cov-report=term-missing --cov-report=html` by default
- Target: ≥ 80% on `app/` (services + repositories + API)

### Dependency Audit

```bash
pip-audit -r requirements.txt
pip-audit -r requirements-dev.txt
```

## Android Tests

### Test Files (30+ files, 200+ test methods)

| Category | Files | Tests | Coverage |
|----------|-------|-------|----------|
| **Domain Models** | 4 | 50+ | Customer validation, DeliveryStatus state machine, LatLng Haversine, RouteResult formatting, NavigationUpdate |
| **DAO Tests** (Robolectric) | 3 | 30+ | CustomerDao, DeliveryDao, SyncOperationDao — insert/search/update/delete, FK constraints |
| **Repository Tests** | 1 | 8 | CustomerRepositoryImpl — offline-first, duplicate detection, active delivery check, sync enqueue |
| **Use Case Tests** | 5 | 40+ | AddCustomer, GetCurrentLocation, CalculateRoute (fallback), DeliveryUseCases, LoginUseCase |
| **ViewModel Tests** | 5 | 50+ | AddCustomerViewModel, LoginViewModel, CaptureLocationViewModel, ActiveDeliveryUiState, HistoryUiState |
| **Sync Tests** | 2 | 18 | SyncResultProcessor, ServerChangeApplier |
| **Location Tests** | 1 | 9 | ArrivalDetector — GPS drift, smoothing, threshold |
| **Map Tests** | 2 | 20+ | MapProvider contract, HomeUiState markers |
| **Network Tests** | 2 | 20+ | ApiException hierarchy, DtoMappers |
| **Security Tests** | 1 | 9 | TokenManager |
| **UI Component Tests** | 2 | 17 | AccuracyIndicator, UiState |
| **Notification Tests** | 1 | 5 | NotificationType enum |
| **Settings Tests** | 1 | 9 | SettingsUiState, ThemeMode |

### Running Android Tests

```bash
cd android

# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.waslni.driver.data.local.dao.CustomerDaoTest"

# Run with coverage
./gradlew testDebugUnitTest --coverage

# Instrumented tests (require emulator/device)
./gradlew connectedAndroidTest
```

## Test Strategy

### What We Test

1. **Domain models** — validation in `init` blocks, state machine transitions, computed properties.
2. **Repositories** — offline-first logic: Room writes + sync queue + scheduleSync.
3. **Use cases** — delegation + parameter propagation + exception pass-through.
4. **ViewModels** — state transitions, error mapping, side effects.
5. **DAOs** — SQL queries (search, filtering, FK constraints) via Robolectric + in-memory Room.
6. **Services (backend)** — business logic in isolation (not through HTTP).
7. **API endpoints (backend)** — full HTTP round-trip with real DB.
8. **Security** — token tampering, cross-driver access, SQL injection, input validation, data leakage.

### What We Don't Test (Yet)

1. **Mapbox SDK** — tested manually on-device (Phase 24).
2. **Compose UI rendering** — planned for Phase 24 (Compose UI Test).
3. **WorkManager scheduling** — tested via integration on-device.
4. **Real GPS** — tested on-device (Phase 24).
5. **Battery consumption** — measured on-device (Phase 24).

### Test Principles

1. **Fast** — unit tests run in < 10 seconds (no emulator needed).
2. **Isolated** — each test truncates the DB (backend) or uses mocks (Android).
3. **Readable** — test names describe the scenario in plain English.
4. **Deterministic** — no flaky tests (no real GPS, no real network in unit tests).
5. **Comprehensive** — happy path + error cases + edge cases for every feature.

## Coverage Targets

| Layer | Target | Current |
|-------|--------|---------|
| Backend `app/services/` | ≥ 80% | ~85% |
| Backend `app/api/` | ≥ 80% | ~90% |
| Backend `app/repositories/` | ≥ 70% | ~75% |
| Android `domain/` | ≥ 90% | ~90% |
| Android `data/` | ≥ 70% | ~75% |
| Android `presentation/` (ViewModels) | ≥ 70% | ~70% |
