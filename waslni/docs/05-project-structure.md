# وصلني (WASLNI) — Project Structure & Git Strategy

> **النسخة:** 1.0
> **التاريخ:** 2026-09-08
> **المرحلة:** Planning

---

## 1. Repository Structure (Monorepo)

```
waslni/
│
├── android/                       # Android App
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/waslni/driver/
│   │   │   │   │   ├── core/
│   │   │   │   │   ├── data/
│   │   │   │   │   ├── domain/
│   │   │   │   │   ├── di/
│   │   │   │   │   └── presentation/
│   │   │   │   │
│   │   │   │   ├── res/
│   │   │   │   │   ├── values/         # strings.xml, colors.xml, themes
│   │   │   │   │   ├── values-ar/     # عربي
│   │   │   │   │   ├── drawable/
│   │   │   │   │   └── mipmap-*/       # app icons
│   │   │   │   │
│   │   │   │   └── AndroidManifest.xml
│   │   │   │
│   │   │   ├── test/                  # Unit tests
│   │   │   └── androidTest/           # Instrumented tests
│   │   │
│   │   ├── build.gradle.kts
│   │   └── proguard-rules.pro
│   │
│   ├── gradle/
│   │   ├── wrapper/
│   │   └── libs.versions.toml         # version catalog
│   │
│   ├── build.gradle.kts               # root
│   ├── settings.gradle.kts
│   └── gradle.properties
│
├── backend/                       # FastAPI Backend
│   ├── app/
│   │   ├── api/
│   │   │   ├── __init__.py
│   │   │   ├── deps.py                # dependency injection
│   │   │   ├── auth.py
│   │   │   ├── customers.py
│   │   │   ├── deliveries.py
│   │   │   └── sync.py
│   │   │
│   │   ├── core/
│   │   │   ├── __init__.py
│   │   │   ├── config.py              # Pydantic Settings
│   │   │   ├── database.py            # SQLAlchemy engine
│   │   │   ├── security.py            # JWT, password hashing
│   │   │   └── logging.py
│   │   │
│   │   ├── models/
│   │   │   ├── __init__.py
│   │   │   ├── user.py
│   │   │   ├── customer.py
│   │   │   ├── delivery.py
│   │   │   ├── refresh_token.py
│   │   │   ├── audit_log.py
│   │   │   └── idempotency_key.py
│   │   │
│   │   ├── schemas/
│   │   │   ├── __init__.py
│   │   │   ├── auth.py
│   │   │   ├── customer.py
│   │   │   ├── delivery.py
│   │   │   ├── sync.py
│   │   │   └── common.py              # pagination, errors
│   │   │
│   │   ├── services/
│   │   │   ├── __init__.py
│   │   │   ├── auth_service.py
│   │   │   ├── customer_service.py
│   │   │   ├── delivery_service.py
│   │   │   ├── sync_service.py
│   │   │   └── audit_service.py
│   │   │
│   │   ├── repositories/
│   │   │   ├── __init__.py
│   │   │   ├── user_repo.py
│   │   │   ├── customer_repo.py
│   │   │   ├── delivery_repo.py
│   │   │   └── sync_repo.py
│   │   │
│   │   ├── middleware/
│   │   │   ├── __init__.py
│   │   │   ├── rate_limit.py
│   │   │   ├── audit_log.py
│   │   │   └── error_handler.py
│   │   │
│   │   ├── utils/
│   │   │   ├── __init__.py
│   │   │   ├── idempotency.py
│   │   │   └── validators.py
│   │   │
│   │   └── main.py
│   │
│   ├── migrations/                    # Alembic
│   │   ├── env.py
│   │   ├── alembic.ini
│   │   └── versions/
│   │       ├── 001_create_users.py
│   │       ├── 002_create_customers.py
│   │       └── ...
│   │
│   ├── tests/
│   │   ├── __init__.py
│   │   ├── conftest.py
│   │   ├── test_auth.py
│   │   ├── test_customers.py
│   │   ├── test_deliveries.py
│   │   ├── test_sync.py
│   │   └── test_authorization.py
│   │
│   ├── scripts/
│   │   ├── seed.py                    # seed admin + test driver
│   │   └── create_user.py
│   │
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── requirements.txt
│   ├── requirements-dev.txt
│   ├── .env.example
│   ├── .gitignore
│   └── README.md
│
├── docs/                          # Documentation
│   ├── 01-PRD.md
│   ├── 02-architecture.md
│   ├── 03-database.md
│   ├── 04-api-contract.md
│   ├── 05-project-structure.md     # this file
│   ├── 06-build-phases.md
│   └── assets/
│       ├── architecture-diagram.png
│       └── er-diagram.png
│
├── scripts/                       # Project-level scripts
│   ├── setup-dev.sh
│   └── new-feature.sh              # scaffolding helper
│
├── .github/
│   └── workflows/
│       ├── android-ci.yml
│       ├── backend-ci.yml
│       └── deploy.yml
│
├── .gitignore
├── .editorconfig
├── README.md
└── LICENSE
```

---

## 2. Android Package Structure (Detailed)

```
app/src/main/java/com/waslni/driver/
│
├── core/
│   ├── common/
│   │   ├── Extensions.kt
│   │   ├── Constants.kt
│   │   ├── Resource.kt              # Result wrapper
│   │   └── TimeUtils.kt
│   │
│   ├── database/
│   │   ├── WaselDatabase.kt
│   │   ├── WaselConverters.kt
│   │   └── DatabaseModule.kt        # Hilt module
│   │
│   ├── network/
│   │   ├── NetworkModule.kt         # Hilt module
│   │   ├── NetworkMonitor.kt
│   │   ├── HttpClient.kt
│   │   └── interceptor/
│   │       ├── AuthInterceptor.kt
│   │       ├── TokenAuthenticator.kt
│   │       ├── ErrorInterceptor.kt
│   │       └── LoggingInterceptor.kt
│   │
│   ├── location/
│   │   ├── LocationProvider.kt       # interface
│   │   ├── FusedLocationProvider.kt  # implementation
│   │   ├── LocationResult.kt
│   │   ├── LocationException.kt
│   │   └── LocationModule.kt
│   │
│   ├── maps/
│   │   ├── MapProvider.kt           # interface
│   │   ├── RoutingEngine.kt         # interface
│   │   ├── NavigationEngine.kt      # interface
│   │   ├── mapbox/
│   │   │   ├── MapboxMapProvider.kt
│   │   │   ├── MapboxRoutingEngine.kt
│   │   │   └── MapboxNavigationEngine.kt
│   │   ├── model/
│   │   │   ├── LatLng.kt
│   │   │   ├── MarkerType.kt
│   │   │   ├── RouteResult.kt
│   │   │   └── NavigationUpdate.kt
│   │   └── MapsModule.kt
│   │
│   ├── security/
│   │   ├── SecureStorage.kt
│   │   ├── EncryptedSharedPreferencesStorage.kt
│   │   ├── TokenManager.kt
│   │   └── SecurityModule.kt
│   │
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt
│       │   ├── Theme.kt
│       │   ├── Type.kt
│       │   └── Shape.kt
│       ├── components/
│       │   ├── WaselButton.kt
│       │   ├── WaselTextField.kt
│       │   ├── LoadingIndicator.kt
│       │   ├── ErrorView.kt
│       │   ├── EmptyState.kt
│       │   └── StateView.kt          # sealed: Loading/Success/Empty/Error
│       └── UiState.kt                # generic UiState sealed class
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── CustomerDao.kt
│   │   │   ├── DeliveryDao.kt
│   │   │   └── SyncOperationDao.kt
│   │   │
│   │   ├── entity/
│   │   │   ├── CustomerEntity.kt
│   │   │   ├── DeliveryEntity.kt
│   │   │   └── SyncOperationEntity.kt
│   │   │
│   │   ├── mapper/
│   │   │   ├── CustomerMapper.kt
│   │   │   ├── DeliveryMapper.kt
│   │   │   └── SyncOperationMapper.kt
│   │   │
│   │   └── prefs/
│   │       └── UserPreferences.kt
│   │
│   ├── remote/
│   │   ├── api/
│   │   │   ├── AuthApi.kt
│   │   │   ├── CustomerApi.kt
│   │   │   ├── DeliveryApi.kt
│   │   │   └── SyncApi.kt
│   │   │
│   │   ├── dto/
│   │   │   ├── auth/
│   │   │   │   ├── LoginRequest.kt
│   │   │   │   ├── TokenResponse.kt
│   │   │   │   └── UserResponse.kt
│   │   │   ├── customer/
│   │   │   │   ├── CustomerCreate.kt
│   │   │   │   ├── CustomerUpdate.kt
│   │   │   │   └── CustomerResponse.kt
│   │   │   ├── delivery/
│   │   │   │   ├── DeliveryCreate.kt
│   │   │   │   ├── DeliveryStatusUpdate.kt
│   │   │   │   └── DeliveryResponse.kt
│   │   │   └── sync/
│   │   │       ├── SyncRequest.kt
│   │   │       ├── SyncResponse.kt
│   │   │       └── PaginatedResponse.kt
│   │   │
│   │   └── mapper/
│   │       ├── CustomerDtoMapper.kt
│   │       ├── DeliveryDtoMapper.kt
│   │       └── SyncMapper.kt
│   │
│   └── repository/
│       ├── AuthRepositoryImpl.kt
│       ├── CustomerRepositoryImpl.kt
│       ├── DeliveryRepositoryImpl.kt
│       └── SyncRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── Customer.kt
│   │   ├── Delivery.kt
│   │   ├── DeliveryStatus.kt
│   │   ├── LocationResult.kt
│   │   └── RouteResult.kt
│   │
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── CustomerRepository.kt
│   │   ├── DeliveryRepository.kt
│   │   └── SyncRepository.kt
│   │
│   └── usecase/
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   ├── LogoutUseCase.kt
│       │   ├── RefreshTokenUseCase.kt
│       │   └── GetCurrentUserUseCase.kt
│       │
│       ├── customer/
│       │   ├── AddCustomerUseCase.kt
│       │   ├── UpdateCustomerUseCase.kt
│       │   ├── DeleteCustomerUseCase.kt
│       │   ├── GetCustomerUseCase.kt
│       │   ├── SearchCustomersUseCase.kt
│       │   ├── ObserveCustomersUseCase.kt
│       │   └── CheckDuplicatePhoneUseCase.kt
│       │
│       ├── delivery/
│       │   ├── StartDeliveryUseCase.kt
│       │   ├── CompleteDeliveryUseCase.kt
│       │   ├── CancelDeliveryUseCase.kt
│       │   ├── ObserveDeliveriesUseCase.kt
│       │   ├── ObserveTodayDeliveriesUseCase.kt
│       │   └── GetDeliveryUseCase.kt
│       │
│       └── sync/
│           ├── ScheduleSyncUseCase.kt
│           ├── ProcessPendingSyncUseCase.kt
│           └── ObservePendingSyncCountUseCase.kt
│
├── di/
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   ├── UseCaseModule.kt
│   └── LocationModule.kt
│
└── presentation/
    ├── MainActivity.kt
    ├── WaselApp.kt                  # Application class, @HiltAndroidApp
    ├── navigation/
    │   ├── WaselNavGraph.kt
    │   ├── Routes.kt                 # route constants
    │   └── Destinations.kt
    │
    ├── auth/
    │   ├── login/
    │   │   ├── LoginScreen.kt
    │   │   ├── LoginViewModel.kt
    │   │   └── LoginUiState.kt
    │   └── splash/
    │       └── SplashScreen.kt
    │
    ├── home/
    │   ├── HomeScreen.kt
    │   ├── HomeViewModel.kt
    │   ├── HomeUiState.kt
    │   └── components/
    │       ├── SearchBar.kt
    │       ├── BottomBar.kt
    │       └── TodayStatsCard.kt
    │
    ├── customers/
    │   ├── list/
    │   │   ├── CustomerListScreen.kt
    │   │   ├── CustomerListViewModel.kt
    │   │   └── CustomerListUiState.kt
    │   ├── add/
    │   │   ├── AddCustomerScreen.kt
    │   │   ├── AddCustomerViewModel.kt
    │   │   └── AddCustomerUiState.kt
    │   ├── edit/
    │   │   ├── EditCustomerScreen.kt
    │   │   └── EditCustomerViewModel.kt
    │   └── details/
    │       ├── CustomerDetailsScreen.kt
    │       └── CustomerDetailsViewModel.kt
    │
    ├── delivery/
    │   ├── active/
    │   │   ├── ActiveDeliveryScreen.kt
    │   │   └── ActiveDeliveryViewModel.kt
    │   └── history/
    │       ├── HistoryScreen.kt
    │       ├── HistoryViewModel.kt
    │       └── HistoryUiState.kt
    │
    ├── navigation/
    │   ├── NavigationScreen.kt
    │   └── NavigationViewModel.kt
    │
    └── settings/
        ├── SettingsScreen.kt
        └── SettingsViewModel.kt
```

---

## 3. Git Strategy

### 3.1 Branching Model

```
main            ←─ production-ready, tagged releases only
   │
   ├── develop  ←─ integration branch for next release
   │     │
   │     ├── feature/customer-management
   │     ├── feature/map-screen
   │     ├── feature/delivery-flow
   │     ├── bugfix/login-validation
   │     └── ...
   │
   ├── release/v1.0.0   ←─ stabilization before production
   │
   └── hotfix/v1.0.1    ←─ urgent fix on production
```

### 3.2 Branch Naming Convention

| النوع | الصيغة | مثال |
|------|-------|------|
| Feature | `feature/<short-description>` | `feature/customer-management` |
| Bugfix | `bugfix/<short-description>` | `bugfix/login-validation` |
| Hotfix | `hotfix/v<x.y.z>-<short-desc>` | `hotfix/v1.0.1-sync-crash` |
| Release | `release/v<x.y.z>` | `release/v1.0.0` |
| Chore | `chore/<short-description>` | `chore/update-deps` |

### 3.3 Commit Message Convention (Conventional Commits)

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: ميزة جديدة
- `fix`: إصلاح خطأ
- `docs`: توثيق
- `style`: تنسيق بدون تغيير الكود
- `refactor`: إعادة هيكلة بدون تغيير سلوك
- `test`: إضافة/تعديل اختبارات
- `chore`: مهام صيانة
- `build`: تغييرات build system
- `ci`: تغييرات CI/CD
- `revert`: تراجع عن commit

**Scopes (مقترحة):**
`auth`, `customer`, `delivery`, `sync`, `map`, `navigation`, `db`, `api`, `ui`, `core`

**أمثلة:**
```
feat(customer): add customer location capture

- Add CustomerViewModel with location state
- Integrate LocationProvider for GPS
- Show accuracy indicator on map
- Save to Room with syncState=PENDING

Closes #42
```

```
fix(sync): handle conflict when server has newer version

The sync worker was incorrectly overwriting server changes
with stale local data. Now we honor the server_state returned
by the sync endpoint.
```

```
docs(api): add idempotency-key section for delivery completion
```

### 3.4 Pull Request Workflow

1. **Create branch** من `develop`.
2. **Commit** بتغييراتك مع رسائل واضحة.
3. **Push** وافتح PR إلى `develop`.
4. **Code review** — مطلوب موافقة reviewer واحد على الأقل.
5. **CI checks** يجب أن تمر:
   - Lint
   - Unit tests
   - Build
6. **Squash and merge** إلى `develop`.
7. عند تجهيز release:
   - افتح PR من `develop` إلى `main`.
   - Tag الـ release: `v1.0.0`.
   - الـ CI ينشر تلقائيًا.

### 3.5 .gitignore (مقترح — جذر المستودع)

```gitignore
# === Android ===
android/app/build/
android/build/
android/.gradle/
android/local.properties
android/captures/
android/.idea/
android/*.iml
android/app/*.iml
android/app/release/
android/app/debug/
*.apk
*.aab
*.keystore
*.jks
!debug.keystore

# === Backend ===
backend/__pycache__/
backend/**/__pycache__/
backend/*.pyc
backend/.venv/
backend/venv/
backend/.env
backend/.env.local
backend/*.log
backend/.pytest_cache/
backend/.mypy_cache/
backend/.ruff_cache/
backend/htmlcov/
backend/.coverage

# === IDE ===
.idea/
.vscode/
*.swp
*.swo
*~
.DS_Store

# === OS ===
Thumbs.db

# === Secrets ===
*.pem
*.key
secrets/
credentials.json
google-services.json
google-services-dev.json

# === Docker ===
backend/postgres-data/

# === Coverage ===
coverage/
*.coverage
```

---

## 4. Code Style & Conventions

### 4.1 Kotlin
- **Style guide:** Kotlin Official + Android Kotlin Style Guide.
- **Linting:** ktlint + detekt.
- **Naming:**
  - Classes: PascalCase (`CustomerViewModel`)
  - Functions/variables: camelCase (`addCustomer`)
  - Constants: SCREAMING_SNAKE_CASE (`MAX_RETRIES`)
  - Packages: lowercase (`com.waslni.driver.data.local`)
- **Imports:** لا wildcards (إلا لـ Kotlin standard library).
- **Nullability:** استخدم `?` بوضوح، تجنب `!!`.

### 4.2 Python
- **Style guide:** PEP 8 + Black.
- **Linting:** ruff + mypy (strict).
- **Line length:** 100 chars.
- **Naming:**
  - Classes: PascalCase (`CustomerService`)
  - Functions/variables: snake_case (`create_customer`)
  - Constants: SCREAMING_SNAKE_CASE (`MAX_PAGINATION_LIMIT`)
- **Type hints:** إجباري على كل الدوال العامة.

### 4.3 Markdown
- **Line length:** لا حد.
- **Headings:** ATX style (`# Title`).
- **Lists:** `-` للقوائم غير المرقمة، `1.` للمرقمة.

---

## 5. CI/CD Pipeline

### 5.1 Android CI (`android-ci.yml`)

```yaml
name: Android CI

on:
  pull_request:
    paths:
      - 'android/**'
  push:
    branches: [develop, main]
    paths:
      - 'android/**'

jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: android

    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Cache Gradle
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/libs.versions.toml') }}

      - name: Decrypt secrets
        run: |
          echo "${{ secrets.MAPBOX_ACCESS_TOKEN }}" > local.properties
          echo "MAPBOX_ACCESS_TOKEN=${{ secrets.MAPBOX_ACCESS_TOKEN }}" >> local.properties

      - name: Lint
        run: ./gradlew ktlintCheck detekt

      - name: Unit tests
        run: ./gradlew testDebugUnitTest

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: apk-debug
          path: android/app/build/outputs/apk/debug/app-debug.apk
```

### 5.2 Backend CI (`backend-ci.yml`)

```yaml
name: Backend CI

on:
  pull_request:
    paths:
      - 'backend/**'
  push:
    branches: [develop, main]
    paths:
      - 'backend/**'

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: waslni_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    defaults:
      run:
        working-directory: backend

    steps:
      - uses: actions/checkout@v4

      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'

      - name: Install dependencies
        run: |
          pip install -r requirements.txt -r requirements-dev.txt

      - name: Lint (ruff)
        run: ruff check .

      - name: Format check (black)
        run: black --check .

      - name: Type check (mypy)
        run: mypy app

      - name: Run migrations
        env:
          DATABASE_URL: postgresql+asyncpg://postgres:postgres@localhost:5432/waslni_test
        run: alembic upgrade head

      - name: Tests
        env:
          DATABASE_URL: postgresql+asyncpg://postgres:postgres@localhost:5432/waslni_test
          JWT_SECRET: test-secret
        run: pytest --cov=app --cov-report=xml

      - name: Upload coverage
        uses: codecov/codecov-action@v4
```

### 5.3 Deploy (`deploy.yml`)

```yaml
name: Deploy

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy-backend:
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker image
        run: docker build -t waslni-backend:${{ github.ref_name }} ./backend

      - name: Push to registry
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USER }} --password-stdin
          docker tag waslni-backend:${{ github.ref_name }} ${{ secrets.DOCKER_USER }}/waslni-backend:${{ github.ref_name }}
          docker push ${{ secrets.DOCKER_USER }}/waslni-backend:${{ github.ref_name }}

      - name: Deploy to server
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SERVER_SSH_KEY }}
          script: |
            cd /opt/waslni
            docker compose pull
            docker compose up -d --no-deps backend
            docker compose exec backend alembic upgrade head
```

---

## 6. Versioning (Semantic Versioning)

```
v<MAJOR>.<MINOR>.<PATCH>

v1.0.0
│ │ │
│ │ └── bug fixes, backward compatible
│ └──── new features, backward compatible
└────── breaking changes
```

| التغيير | الإصدار |
|--------|--------|
| Bug fix | PATCH (1.0.0 → 1.0.1) |
| New feature | MINOR (1.0.1 → 1.1.0) |
| Breaking change | MAJOR (1.1.0 → 2.0.0) |

**Tags:**
- `v1.0.0` — Release.
- `v1.0.0-rc.1` — Release candidate.
- `v1.0.0-beta.1` — Beta.

---

## 7. Release Process

### 7.1 Backend Release
1. افتح PR من `develop` إلى `main`.
2. بعد الـ merge، أنشئ tag `v1.x.y` على `main`.
3. الـ CI يبني Docker image وينشرها.
4. الـ CI ينفذ `alembic upgrade head` على الـ Production DB.
5. تحقّق من `/health` endpoint.

### 7.2 Android Release
1. حدّث `versionCode` و `versionName` في `build.gradle.kts`.
2. افتح PR من `develop` إلى `main`.
3. بعد الـ merge، أنشئ tag `v1.x.y`.
4. الـ CI يبني AAB موقّع.
5. ارفع إلى Play Console → Internal Testing.
6. بعد الاختبار، رقّ إلى Production Track.

---

## 8. Environment Variables Management

### 8.1 Backend
| البيئة | المصدر |
|-------|------|
| Development | `.env` (محلي، لا يُرفع) |
| Staging | Docker secrets / `.env.staging` |
| Production | Docker secrets / Secret manager |

`.env.example` يحوي كل المفاتيح بدون قيم حساسة.

### 8.2 Android
| المتغير | المصدر |
|-------|------|
| `API_BASE_URL` | `BuildConfig` field في `build.gradle.kts` |
| `MAPBOX_ACCESS_TOKEN` | `local.properties` (لا يُرفع) |
| Signing keys | `keystore/` (لا يُرفع) |

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        val mapboxToken = project.findProperty("MAPBOX_ACCESS_TOKEN") ?: ""
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxToken\"")

        val apiBaseUrl = when (project.findProperty("buildEnvironment")) {
            "staging" -> "https://staging-api.waslni.com/api/v1"
            "production" -> "https://api.waslni.com/api/v1"
            else -> "http://10.0.2.2:8000/api/v1"
        }
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
}
```

---

## 9. Code Review Guidelines

### 9.1 ما يطلبه الـ Reviewer
- [ ] الكود يتبع الـ style guide.
- [ ] لا توجد secrets في الكود.
- [ ] Unit tests مكتوبة للميزة الجديدة.
- [ ] لا توجد `print()` أو `Log.d()` في الـ production code.
- [ ] Error handling واضح.
- [ ] Documentation للـ public APIs.
- [ ] الـ PR صغير ومركّز (أقل من 400 سطر مثالي).

### 9.2 علامات حمراء
- ملف واحد بأكثر من 500 سطر في PR واحد.
- تغيير في `domain` بدون سبب واضح.
- إضافة dependencies جديدة بدون مناقشة.
- `// TODO` بدون issue مرتبط.
- `@Suppress` بدون تعليق يشرح السبب.

---

## 10. Issue Tracking

### 10.1 Labels

| Label | الوصف |
|-------|------|
| `bug` | خطأ |
| `feature` | ميزة جديدة |
| `enhancement` | تحسين |
| `documentation` | توثيق |
| `refactor` | إعادة هيكلة |
| `testing` | اختبارات |
| `security` | أمن |
| `performance` | أداء |
| `backend` | متعلق بالـ Backend |
| `android` | متعلق بالـ Android |
| `priority: high/medium/low` | الأولوية |
| `size: S/M/L/XL` | الحجم |

### 10.2 Issue Template (مقترح)

```markdown
## الوصف
[وصف واضح ومختصر]

## النوع
- [ ] Bug
- [ ] Feature
- [ ] Enhancement
- [ ] Documentation

## الخطوات لإعادة الإنتاج (للأخطاء)
1. ...
2. ...

## السلوك المتوقع
...

## السلوك الفعلي
...

## البيئة
- App version:
- Android version:
- Device:

## Screenshots / Logs
```

---

## 11. Documentation Maintenance

- كل ميزة جديدة → تحديث `docs/` أولاً ثم الكود.
- الـ API changes → تحديث `04-api-contract.md` + bump version.
- DB changes → Alembic migration + تحديث `03-database.md`.
- Architecture changes → تحديث `02-architecture.md`.

---

## 12. README.md (Root — مقترح)

```markdown
# وصلني (WASLNI)

تطبيق Android للمندوب — Offline-first delivery navigation app.

## البنية
- `android/` — Android app (Kotlin + Compose + Hilt + Room + Mapbox)
- `backend/` — FastAPI backend (Python + PostgreSQL)
- `docs/` — Project documentation

## البدء السريع
- [Setup Guide](docs/setup.md)
- [Architecture](docs/02-architecture.md)
- [API Contract](docs/04-api-contract.md)

## التطوير
- Backend: `cd backend && docker compose up`
- Android: افتح `android/` في Android Studio

## الترخيص
MIT
```

---

**نهاية وثيقة Project Structure & Git Strategy — WASLNI v1.0**
