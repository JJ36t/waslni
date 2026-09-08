# وصلني (WASLNI) — Architecture & Tech Stack

> **النسخة:** 1.0
> **التاريخ:** 2026-09-08
> **المرحلة:** Planning

---

## 1. نظرة معمارية شاملة (High-Level Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                      Android App                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Jetpack Compose UI (RTL, Material 3)               │   │
│  └─────────────────────────────────────────────────────┘   │
│                          ↓                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ViewModel (StateFlow / UiState)                    │   │
│  └─────────────────────────────────────────────────────┘   │
│                          ↓                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Use Cases (Domain Layer)                           │   │
│  └─────────────────────────────────────────────────────┘   │
│                          ↓                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Repository (Offline First)                         │   │
│  └─────────────────────────────────────────────────────┘   │
│              ↓                              ↓               │
│  ┌──────────────────┐              ┌────────────────────┐  │
│  │  Room (Local)    │              │  Retrofit (Remote) │  │
│  │  Source of Read  │              │  Sync Endpoint     │  │
│  └──────────────────┘              └────────────────────┘  │
│              ↓                              ↓               │
│  ┌──────────────────┐              ┌────────────────────┐  │
│  │  Sync Queue      │ ←──────────→ │  WorkManager       │  │
│  └──────────────────┘              └────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          │ HTTPS
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                  Backend (FastAPI)                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  API Layer (/api/v1/*)                              │   │
│  │  - /auth   /customers   /deliveries   /sync         │   │
│  └─────────────────────────────────────────────────────┘   │
│                          ↓                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Service Layer (Business Logic)                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                          ↓                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Repository Layer (Data Access)                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                          ↓                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  PostgreSQL (Source of Truth)                       │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. المبدأ المعماري الأساسي: Offline First

```
                ┌──────────────────┐
                │   PostgreSQL     │
                │   Source of      │
                │   Truth          │
                └────────▲─────────┘
                         │
                      Sync
                         │
                ┌────────┴─────────┐
                │   Room (Local)   │
                │   Source of Read │
                │   for UI         │
                └────────▲─────────┘
                         │
                         │
                ┌────────┴─────────┐
                │   Android UI     │
                └──────────────────┘
```

**القاعدة:** التطبيق يقرأ دائمًا من Room. لا يوجد قراءة مباشرة من API للـ UI.

---

## 3. Android Architecture — Clean Architecture + MVVM

### 3.1 الطبقات

```
┌─────────────────────────────────────────┐
│  presentation/                          │  ← UI + ViewModel
│  - auth/   home/   customers/           │
│  - delivery/   navigation/   history/   │
│  - settings/                            │
└────────────────────┬────────────────────┘
                     ↓
┌─────────────────────────────────────────┐
│  domain/                                │  ← Pure Kotlin
│  - model/   repository/   usecase/      │
└────────────────────┬────────────────────┘
                     ↓
┌─────────────────────────────────────────┐
│  data/                                  │  ← Implementation
│  - local/  (Room entities, DAOs)        │
│  - remote/ (Retrofit APIs, DTOs)        │
│  - repository/ (Implementations)        │
└────────────────────┬────────────────────┘
                     ↓
┌─────────────────────────────────────────┐
│  core/                                  │  ← Shared infra
│  - common/   database/   network/       │
│  - location/   maps/   security/   ui/  │
└─────────────────────────────────────────┘
```

### 3.2 قاعدة التبعية (Dependency Rule)

```
presentation  →  domain  ←  data
                ↑
                core
```

- **domain** لا يعرف أي شيء عن Android أو DB أو Network.
- **data** يطبق interfaces من domain.
- **presentation** يستخدم use cases من domain.
- **core** يحوي utilities يستخدمها الجميع.

### 3.3 مثال: تدفق بيانات "إضافة زبون"

```
CustomerScreen (Compose)
        ↓ (event: onSaveClicked)
CustomerViewModel
        ↓ (call)
AddCustomerUseCase
        ↓ (call)
CustomerRepository (interface in domain)
        ↓ (implemented by)
CustomerRepositoryImpl
        ├── Room: insert CustomerEntity
        ├── Room: insert SyncOperationEntity (status=PENDING)
        └── (لا ننتظر API — الـ UI يكمل فورًا)
        ↓
WorkManager (SyncWorker)
        ↓ (when network available)
CustomerApi.createCustomer(...)
        ↓ (success)
Update SyncOperationEntity.status = SYNCED
```

---

## 4. Android Technology Stack

| المجال | التقنية | السبب |
|-------|--------|------|
| Language | Kotlin | Modern, concise, official |
| UI | Jetpack Compose + Material 3 | Declarative, modern Android standard |
| Architecture | Clean Architecture + MVVM | قابلية الاختبار + الفصل |
| DI | Hilt | Standard Android DI |
| Local DB | Room | Official ORM, type-safe queries |
| Network | Retrofit + OkHttp | Standard, reliable |
| Serialization | Kotlin Serialization / Moshi | JSON parsing |
| Async | Coroutines + Flow | Modern async, reactive streams |
| Navigation | Navigation Compose | Single-activity, type-safe |
| Location | FusedLocationProviderClient | Battery-efficient location |
| Maps | Mapbox Maps SDK | مفتوح + قابل للتخصيص + دعم جيد للعراق |
| Routing | Mapbox Directions API | يتكامل مع Mapbox |
| Background Sync | WorkManager | Persistent, constraint-aware |
| Secure Storage | EncryptedSharedPreferences + Keystore | JWT storage |
| Auth | Username + Password → JWT | Simple, MVP-appropriate |
| Push (لاحقًا) | Firebase Cloud Messaging | Standard push |
| Testing | JUnit4 + MockK + Turbine + Coroutines Test | Standard Android testing |

### 4.1 Libraries Versions (مقترحة)

```kotlin
// build.gradle.kts (versions catalog)
[versions]
kotlin = "1.9.24"
composeBom = "2024.09.00"
composeCompiler = "1.5.14"
hilt = "2.51.1"
room = "2.6.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
workManager = "2.9.1"
navigation = "2.7.7"
mapbox = "11.6.0"
mapboxNavigation = "2.20.0"
securityCrypto = "1.1.0-alpha06"
serialization = "1.6.3"
```

---

## 5. Backend Architecture

### 5.1 الطبقات

```
┌─────────────────────────────────────────┐
│  api/ (FastAPI Routers)                 │
│  - auth.py                              │
│  - customers.py                         │
│  - deliveries.py                        │
│  - sync.py                              │
└────────────────────┬────────────────────┘
                     ↓
┌─────────────────────────────────────────┐
│  services/ (Business Logic)             │
│  - AuthService                          │
│  - CustomerService                      │
│  - DeliveryService                      │
│  - SyncService                          │
└────────────────────┬────────────────────┘
                     ↓
┌─────────────────────────────────────────┐
│  repositories/ (Data Access)            │
│  - SQLAlchemy queries                   │
└────────────────────┬────────────────────┘
                     ↓
┌─────────────────────────────────────────┐
│  models/ (SQLAlchemy ORM)               │
│  - User, Customer, Delivery, SyncOp     │
└────────────────────┬────────────────────┘
                     ↓
┌─────────────────────────────────────────┐
│  PostgreSQL                             │
└─────────────────────────────────────────┘
```

### 5.2 Backend Technology Stack

| المجال | التقنية |
|-------|--------|
| Language | Python 3.11+ |
| Framework | FastAPI |
| ORM | SQLAlchemy 2.0 (async) |
| Database | PostgreSQL 15+ |
| Migrations | Alembic |
| Validation | Pydantic v2 |
| Auth | JWT (python-jose) + passlib[argon2] |
| Server | Uvicorn (dev) / Gunicorn + Uvicorn workers (prod) |
| Containerization | Docker + Docker Compose |
| Reverse Proxy | Nginx |
| Testing | pytest + httpx + pytest-asyncio |
| Linting | ruff + black + mypy |

---

## 6. Map Provider Architecture (مهم)

نحن نفصل نظام الخرائط إلى 5 واجهات منفصلة، بحيث إذا غيرنا المزود لاحقًا لا نعيد بناء التطبيق.

```kotlin
// domain/repository/ (interfaces)

interface MapDisplay {
    fun showMap()
    fun moveCamera(latitude: Double, longitude: Double, zoom: Float)
    fun addMarker(id: String, lat: Double, lng: Double, type: MarkerType)
    fun removeMarker(id: String)
    fun clearMarkers()
}

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationResult
    fun observeLocationUpdates(interval: Long): Flow<LocationResult>
}

interface RoutingEngine {
    suspend fun calculateRoute(
        from: LatLng,
        to: LatLng
    ): RouteResult
}

interface NavigationEngine {
    fun startNavigation(route: RouteResult)
    fun stopNavigation()
    fun observeNavigationUpdates(): Flow<NavigationUpdate>
}

interface GeocodingProvider {
    suspend fun reverseGeocode(lat: Double, lng: Double): String?
}
```

### 6.1 Mapbox Implementation (المزود الافتراضي للـ MVP)

```
MapDisplay        → MapboxMap + Style + Annotations
LocationProvider  → FusedLocationProviderClient (Google Play Services)
RoutingEngine     → MapboxDirections
NavigationEngine  → MapboxNavigation
GeocodingProvider → MapboxGeocoding (لاحقًا)
```

> **ملاحظة:** `LocationProvider` يبقى FusedLocationProviderClient لأنه الأكثر كفاءة في البطارية على Android، وغير مرتبط بـ Map SDK.

### 6.2 الـ Markers والحالات

```
🔵 Driver (Current Location)
📍 Customer (Saved, no active delivery)
🟠 Customer (Active delivery - ON_THE_WAY)
🟢 Customer (Delivered today)
```

---

## 7. Authentication & Session Architecture

### 7.1 Login Flow

```
Android:
  POST /api/v1/auth/login { username, password }
        ↓
Backend:
  - verify password (Argon2id)
  - generate Access Token (15 min)
  - generate Refresh Token (30 days)
  - return { access_token, refresh_token, user }
        ↓
Android:
  - store tokens in EncryptedSharedPreferences
  - store user in SessionCache
  - navigate to Home
```

### 7.2 Authenticated Request Flow

```
Android → API:
  Authorization: Bearer <access_token>
        ↓
Backend Interceptor:
  - verify JWT
  - extract user_id
  - check authorization
        ↓
If 401 (token expired):
  Android → POST /auth/refresh { refresh_token }
        ↓
  If success: retry original request
  If fail: logout + navigate to Login
```

### 7.3 Token Storage

- **Access Token:** EncryptedSharedPreferences (Keystore-backed).
- **Refresh Token:** EncryptedSharedPreferences (Keystore-backed).
- لا SharedPreferences عادية.
- لا تخزين في الكود.

---

## 8. Offline First & Sync Architecture

### 8.1 قاعدة البيانات المحلية (Room) — Source of Read

```
┌────────────────────────────────────────┐
│            Room Database               │
├────────────────────────────────────────┤
│  customers                             │
│  deliveries                            │
│  sync_operations                       │
│  (وكلها تحوي syncState)                │
└────────────────────────────────────────┘
```

### 8.2 Sync Queue

كل عملية محلية تكتب `sync_operations`:

```
sync_operations
├── id            (UUID)
├── entity_id     (UUID)
├── entity_type   (CUSTOMER / DELIVERY)
├── operation     (CREATE / UPDATE / DELETE / COMPLETE)
├── payload       (JSON string)
├── created_at    (Long)
├── retry_count   (Int)
└── status        (PENDING / SYNCING / SYNCED / FAILED)
```

### 8.3 Sync Worker (WorkManager)

```kotlin
class SyncWorker(...) : CoroutineWorker(...) {

    override suspend fun doWork(): Result {
        val pendingOps = syncDao.getPendingOperations()
        for (op in pendingOps) {
            try {
                when (op.operation) {
                    "CREATE_CUSTOMER" -> customerApi.create(...)
                    "UPDATE_CUSTOMER" -> customerApi.update(...)
                    "DELETE_CUSTOMER" -> customerApi.delete(...)
                    "CREATE_DELIVERY" -> deliveryApi.create(...)
                    "COMPLETE_DELIVERY" -> deliveryApi.complete(...)
                }
                syncDao.markSynced(op.id)
            } catch (e: Exception) {
                syncDao.incrementRetry(op.id)
                if (op.retryCount >= MAX_RETRIES) {
                    syncDao.markFailed(op.id)
                }
            }
        }
        return Result.success()
    }

    companion object {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
```

### 8.4 Conflict Resolution Strategy

| نوع البيانات | الاستراتيجية |
|------------|-------------|
| Customer (name, phone) | Latest-write-wins (based on `updated_at`) |
| Customer (location) | Latest-write-wins |
| Delivery status | Idempotency-Key prevents duplicates |
| Delivery completion | Idempotency-Key prevents double-completion |

### 8.5 Idempotency-Key

```http
POST /api/v1/deliveries/{id}/complete
Idempotency-Key: <uuid-from-android>
```

- الـ Backend يخزن الـ key مع response.
- إذا وصل نفس الـ key مرة ثانية، يرجع نفس response بدون تنفيذ العملية مرة أخرى.

---

## 9. Security Architecture

### 9.1 النقل (Transport)
- HTTPS إجباري في الـ Production.
- HSTS headers.
- TLS 1.2+ فقط.

### 9.2 المصادقة (Authentication)
- JWT (HS256 أو RS256).
- Access Token: 15 دقيقة.
- Refresh Token: 30 يوم، يُخزن hashed في DB (يسمح بـ revocation).

### 9.3 التفويض (Authorization)
- كل request يحمل `user_id` من الـ JWT (لا يثق بـ body).
- كل query على customers/deliveries تضيف `WHERE driver_id = :user_id`.
- المندوب A لا يستطيع قراءة/تعديل بيانات المندوب B.

### 9.4 التخزين المحلي (Local Storage)
- JWT في EncryptedSharedPreferences (Keystore-backed).
- Room لا يخزن كلمات مرور.
- لا بيانات حساسة في logs.

### 9.5 Backend Security
- Password hashing: Argon2id (مُفضل) أو bcrypt.
- Rate Limiting: 100 req/min per user, 5 login attempts/min per IP.
- Parameterized queries (SQLAlchemy ORM).
- Input Validation (Pydantic).
- CORS محدد ( Origin whitelist).
- Security headers (X-Content-Type-Options, X-Frame-Options, etc.).
- Secrets في environment variables، لا في Git.

### 9.6 Audit Logs
نخزن:
- LOGIN_SUCCESS, LOGIN_FAILURE
- CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER
- START_DELIVERY, COMPLETE_DELIVERY, CANCEL_DELIVERY

لا نخزن: Tokens, passwords, full payloads الحساسة.

---

## 10. Battery Optimization Strategy

| الحالة | تردد تحديث الموقع |
|------|-----------------|
| Idle (لا يوجد توصيل نشط) | كل 5 دقائق أو إيقاف |
| ON_THE_WAY (ملاحة نشطة) | كل 2-5 ثواني |
| ARRIVED (انتظار التسليم) | كل 30 ثانية |

```kotlin
class LocationManager(...) {
    fun startTracking(mode: TrackingMode) {
        val interval = when (mode) {
            TrackingMode.IDLE -> 5 * 60 * 1000L
            TrackingMode.NAVIGATION -> 3000L
            TrackingMode.ARRIVED -> 30 * 1000L
        }
        // ...
    }
}
```

---

## 11. Error Handling Architecture

### 11.1 Unified UiState

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String, val code: String? = null) : UiState<Nothing>
}
```

### 11.2 Network Error Mapping

```kotlin
fun Throwable.toUserMessage(): String = when (this) {
    is IOException -> "لا يوجد اتصال بالإنترنت. تم حفظ العملية محليًا."
    is HttpException -> when (code()) {
        401 -> "انتهت الجلسة. الرجاء تسجيل الدخول."
        403 -> "ليس لديك صلاحية لهذه العملية."
        404 -> "العنصر غير موجود."
        in 500..599 -> "تعذر الاتصال بالسيرفر. حاول لاحقًا."
        else -> "حدث خطأ غير متوقع."
    }
    else -> "حدث خطأ غير متوقع."
}
```

---

## 12. Testing Architecture

```
Unit Tests (domain + data)
├── UseCases: business logic
├── Repository: with fake Room + fake API
├── Sync logic: state transitions, retry, conflict
└── Validation: input validators

Integration Tests
├── Room DAO tests (Robolectric)
├── API tests (MockWebServer)
└── End-to-end: Add Customer → Sync → Backend

UI Tests (Compose UI Test)
├── Login flow
├── Add Customer flow
├── Search flow
└── Delivery flow

Backend Tests (pytest)
├── API tests (httpx + TestClient)
├── Auth tests
├── Authorization tests (driver A cannot access driver B's data)
└── Sync endpoint tests (idempotency, conflict)
```

---

## 13. Deployment Architecture (Production)

```
Internet
   ↓
Nginx (Reverse Proxy, HTTPS termination)
   ↓
FastAPI (Uvicorn workers behind Gunicorn)
   ↓
PostgreSQL (managed or containerized)
   ↓
Automated Backups (daily + weekly + retention)
```

### Environments
| البيئة | القصد |
|-------|------|
| Development | محلي + Docker Compose |
| Staging | نسخة طبق الأصل من Prod، للاختبار قبل النشر |
| Production | البيئة الحقيقية |

كل بيئة لها:
- DB منفصلة
- Configuration منفصل (.env)
- Secrets منفصلة
- URL مختلف

---

## 14. المتغيرات البيئية (Environment Variables)

### Backend
```env
# Database
DATABASE_URL=postgresql+asyncpg://user:pass@host:5432/waslni

# JWT
JWT_SECRET=<random-256-bit-secret>
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=15
REFRESH_TOKEN_EXPIRE_DAYS=30

# App
APP_ENV=development|staging|production
CORS_ORIGINS=http://localhost:8080,https://app.waslni.com

# Rate Limiting
RATE_LIMIT_PER_MINUTE=100
LOGIN_RATE_LIMIT_PER_MINUTE=5

# Mapbox (إن احتاجها الـ Backend لاحقًا)
MAPBOX_API_KEY=<key>

# FCM (لاحقًا)
FCM_CREDENTIALS_PATH=/etc/waslni/fcm.json
```

### Android
- `API_BASE_URL` (BuildConfig field)
- `MAPBOX_ACCESS_TOKEN` (local.properties، لا يُرفع لـ Git)

---

## 15. المخطط المعماري النهائي

```
                 📱 WASLNI DRIVER APP
                          │
            ┌─────────────┴──────────────┐
            │                            │
         MAP                          CUSTOMERS
            │                            │
       Mapbox Maps                  Name
       Routing                      Phone
       GPS                          Coordinates
       Navigation Alerts            Accuracy
            │                            │
            └─────────────┬──────────────┘
                          │
                      DELIVERIES
                          │
                   State Machine
                   History
                   Sync Queue
                          │
                ┌─────────┴─────────┐
                │                   │
             Room (Local)      FastAPI (Remote)
                │                   │
             Sync               PostgreSQL
                │                   │
                └─────────┬─────────┘
                          │
                       Driver
                          │
                    Daily Workflow
```

---

**نهاية وثيقة Architecture — WASLNI v1.0**
