# وصلني (WASLNI) — Build Phases Roadmap

> **النسخة:** 1.0
> **التاريخ:** 2026-09-08
> **المرحلة:** Planning

---

## 1. نظرة عامة (Overview)

هذه الوثيقة تُعرّف الترتيب البرمجي الصحيح لتنفيذ المشروع. رغم أن PRD يحوي 100 قسم، إلا أننا لا نبنيها بالترتيب الرقمي حرفيًا. كل مرحلة هنا تحتوي على:

- **الهدف** — ما الذي نحققه.
- **المخرجات** — الملفات/الميزات الناتجة.
- **معايير القبول** — متى نعتبر المرحلة منتهية.
- **التبعيات** — ما الذي يجب إنجازه قبلها.

---

## 2. الخريطة الكلية (32 Phase)

```
Phase 1:  Planning (التخطيط) ✅ ← نحن هنا
Phase 2:  Android Foundation
Phase 3:  Local Data (Room)
Phase 4:  GPS & Location
Phase 5:  Maps (Mapbox)
Phase 6:  Customer Management
Phase 7:  Backend Foundation
Phase 8:  Backend Auth
Phase 9:  Backend Customers & Deliveries
Phase 10: Backend Sync Endpoint
Phase 11: Android ↔ Backend Integration
Phase 12: Offline Sync
Phase 13: Delivery Flow
Phase 14: Routing & ETA
Phase 15: Navigation & Alerts
Phase 16: Arrival Detection
Phase 17: History
Phase 18: Notifications (Local)
Phase 19: Settings
Phase 20: UI Polish (Dark Mode, RTL)
Phase 21: Security Hardening
Phase 22: Testing (Unit + Integration)
Phase 23: Performance Optimization
Phase 24: Real Device Testing
Phase 25: Backend Production Setup
Phase 26: Docker & Nginx
Phase 27: CI/CD
Phase 28: Monitoring & Logging
Phase 29: Release Build
Phase 30: Play Store Preparation
Phase 31: Documentation Finalization
Phase 32: MVP Launch
```

---

## Phase 1 — Planning ✅

**الهدف:** إنتاج وثائق مرجعية كاملة قبل أي كود.

**المخرجات:**
- [x] `01-PRD.md`
- [x] `02-architecture.md`
- [x] `03-database.md`
- [x] `04-api-contract.md`
- [x] `05-project-structure.md`
- [x] `06-build-phases.md`

**معايير القبول:**
- [x] كل الأطراف (Android + Backend) تتفق على الـ contract.
- [x] الـ DB schema محدد بدقة.
- [x] الـ API contract يغطي كل use cases.

**التبعيات:** لا شيء.

---

## Phase 2 — Android Foundation

**الهدف:** إنشاء مشروع Android فارغ بالمعمارية الصحيحة وجاهز للاستخدام.

**المخرجات:**
- مشروع Kotlin جديد بـ `com.waslni.driver`.
- `build.gradle.kts` + `libs.versions.toml` (version catalog).
- Jetpack Compose + Material 3 setup.
- Hilt DI معged مع `WaselApp` (`@HiltAndroidApp`).
- Navigation Compose مع skeleton للـ nav graph.
- Theme (Color, Type, Shape) + Dark/Light mode.
- RTL configuration (`values-ar/`).
- `MainActivity` + `WaselNavGraph`.
- Empty placeholder screens (Splash, Login, Home).

**معايير القبول:**
- [ ] التطبيق يبني ويشغل بنجاح.
- [ ] Hilt يعمل (inject خدمة dummy).
- [ ] التنقل بين placeholder screens يعمل.
- [ ] RTL يظهر بشكل صحيح.
- [ ] Dark/Light mode يعمل.

**التبعيات:** Phase 1.

---

## Phase 3 — Local Data (Room)

**الهدف:** إنشاء قاعدة البيانات المحلية وكل DAOs.

**المخرجات:**
- `WaselDatabase` مع 3 entities (Customer, Delivery, SyncOperation).
- `CustomerDao`, `DeliveryDao`, `SyncOperationDao`.
- `WaselConverters` (للأنواع المركبة إن لزم).
- Mappers (Entity ↔ Domain).
- Domain models (`Customer`, `Delivery`, `DeliveryStatus`, ...).
- Repository interfaces في `domain/`.
- `CustomerRepositoryImpl` (يقرأ من Room فقط في هذه المرحلة).
- Hilt module للـ Room.

**معايير القبول:**
- [ ] `WaselDatabase` يبني بنجاح.
- [ ] insert/query/update/delete يعمل لكل جدول.
- [ ] Search query يعمل على name و phone.
- [ ] Unit tests للـ DAOs (Robolectric) تمر.

**التبعيات:** Phase 2.

---

## Phase 4 — GPS & Location

**الهدف:** التقاط موقع GPS بدقة عالية وعرض جودة الدقة.

**المخرجات:**
- `LocationProvider` interface (في `core/location/`).
- `FusedLocationProvider` implementation.
- `LocationResult` model.
- `LocationException` (Permission, GPS disabled, Timeout).
- Permission handling (`ACCESS_FINE_LOCATION`).
- GPS accuracy check + threshold configurable.
- `GetCurrentLocationUseCase`.
- UI component لعرض الدقة (`AccuracyIndicator`).
- Empty "Capture Location" screen للتجربة.

**معايير القبول:**
- [ ] يلتقط موقع حقيقي على جهاز حقيقي.
- [ ] يعرض الدقة بشكل صحيح.
- [ ] يطلب permission عند الحاجة.
- [ ] يتعامل مع GPS disabled (يفتح الإعدادات).
- [ ] يتعامل مع رفض permission.

**التبعيات:** Phase 2.

---

## Phase 5 — Maps (Mapbox)

**الهدف:** عرض الخريطة مع Markers للمندوب والزبائن.

**المخرجات:**
- `MapProvider` interface (في `core/maps/`).
- `MapboxMapProvider` implementation.
- Mapbox SDK setup + access token.
- `HomeScreen` مع خريطة كاملة الشاشة.
- Driver marker (موقع المندوب الحالي).
- Customer markers (من Room).
- Marker interaction (click → show bottom sheet).
- Marker colors based on state (saved, active delivery, delivered).
- `MapType` enum (NORMAL, SATELLITE).
- Camera movement (zoom to driver, zoom to all markers).

**معايير القبول:**
- [ ] الخريطة تظهر بسلاسة.
- [ ] موقع المندوب يظهر بدقة.
- [ ] زبائن Room يظهرون كـ markers.
- [ ] الضغط على marker يفتح bottom sheet.
- [ ] الأداء جيد مع 100+ marker.

**التبعيات:** Phase 3, 4.

---

## Phase 6 — Customer Management

**الهدف:** CRUD كامل للزبائن.

**المخرجات:**
- `AddCustomerScreen` + `AddCustomerViewModel`.
- `EditCustomerScreen` + `EditCustomerViewModel`.
- `CustomerDetailsScreen` + `CustomerDetailsViewModel`.
- `CustomerListScreen` + `CustomerListViewModel`.
- Use cases:
  - `AddCustomerUseCase`
  - `UpdateCustomerUseCase`
  - `DeleteCustomerUseCase`
  - `SearchCustomersUseCase`
  - `ObserveCustomersUseCase`
  - `CheckDuplicatePhoneUseCase`
- Validation (name, phone, lat, lng, accuracy).
- Duplicate phone detection.
- "Update Location" flow (capture new GPS, replace old).

**معايير القبول:**
- [ ] إضافة زبون يعمل (مع GPS capture + accuracy check).
- [ ] تعديل بيانات زبون يعمل.
- [ ] تحديث موقع زبون يعمل.
- [ ] حذف زبون يعمل (مع تأكيد).
- [ ] البحث بالاسم والرقم يعمل.
- [ ] كشف التكرار بالرقم يعمل.
- [ ] كل العمليات تكتب لـ Room + Sync queue.

**التبعيات:** Phase 3, 4, 5.

---

## Phase 7 — Backend Foundation

**الهدف:** إنشاء FastAPI backend قادر على التشغيل.

**المخرجات:**
- FastAPI project structure (app/api, core, models, schemas, services, repositories).
- `core/config.py` (Pydantic Settings + env vars).
- `core/database.py` (SQLAlchemy async engine + session).
- `core/security.py` (JWT, password hashing).
- `main.py` مع CORS, exception handlers, routers.
- Alembic setup.
- `001_create_users.py` migration.
- `002_create_customers.py` migration.
- `003_create_deliveries.py` migration.
- `004_create_refresh_tokens.py` migration.
- `005_create_audit_logs.py` migration.
- `006_create_idempotency_keys.py` migration.
- `docker-compose.yml` (PostgreSQL + FastAPI).
- `requirements.txt` + `requirements-dev.txt`.
- Seed script (`scripts/seed.py`) ينشئ admin + test driver.

**معايير القبول:**
- [ ] `docker compose up` يبدأ كل الخدمات.
- [ ] `alembic upgrade head` ينفذ migrations.
- [ ] `/docs` (Swagger UI) متاح.
- [ ] `/health` يرجع 200.

**التبعيات:** Phase 1.

---

## Phase 8 — Backend Auth

**الهدف:** نظام مصادقة كامل على Backend.

**المخرجات:**
- `auth.py` router:
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
  - `GET /auth/me`
- `AuthService`:
  - `verify_password` (Argon2id)
  - `create_access_token`, `create_refresh_token`
  - `verify_token`
  - `revoke_refresh_token`
- `get_current_user` dependency.
- `TokenAuthenticator` middleware.
- Audit logging for login/logout.
- Rate limiting on `/auth/login`.
- Tests:
  - Login success/failure
  - Token refresh
  - Logout revokes token
  - Protected endpoint requires valid token

**معايير القبول:**
- [ ] Login بـ username + password يعمل.
- [ ] Access token صالح لـ 15 دقيقة.
- [ ] Refresh token صالح لـ 30 يوم.
- [ ] Refresh endpoint يعمل.
- [ ] Logout يبطل refresh token.
- [ ] Protected endpoints ترفض tokens المنتهية.
- [ ] Rate limiting يمنع brute force.

**التبعيات:** Phase 7.

---

## Phase 9 — Backend Customers & Deliveries

**الهدف:** APIs كاملة للزبائن والتوصيلات.

**المخرجات:**
- `customers.py` router:
  - `GET /customers` (paginated + search)
  - `POST /customers`
  - `GET /customers/{id}`
  - `PATCH /customers/{id}`
  - `DELETE /customers/{id}`
- `deliveries.py` router:
  - `GET /deliveries` (paginated + filters)
  - `POST /deliveries`
  - `GET /deliveries/{id}`
  - `PATCH /deliveries/{id}/status` (with Idempotency-Key)
- `CustomerService`, `DeliveryService`.
- Repositories with SQLAlchemy.
- Authorization: المندوب A لا يرى بيانات المندوب B.
- Validation (Pydantic).
- Audit logging.
- Tests كاملة (unit + integration).

**معايير القبول:**
- [ ] CRUD كامل للزبائن يعمل.
- [ ] CRUD كامل للتوصيلات يعمل.
- [ ] Authorization يمنع الوصول غير المصرّح.
- [ ] Idempotency-Key يمنع التكرار.
- [ ] State machine يمنع الانتقالات غير الصالحة.
- [ ] Pagination تعمل بشكل صحيح.

**التبعيات:** Phase 8.

---

## Phase 10 — Backend Sync Endpoint

**الهدف:** endpoint واحد للـ sync يتلقى batch من العمليات.

**المخرجات:**
- `sync.py` router:
  - `POST /sync` (batch operations).
- `SyncService`:
  - معالجة كل عملية على حدة.
  - Conflict resolution (latest-write-wins).
  - Idempotency لكل عملية.
  - إرجاع `server_changes` (download direction).
- Tests شاملة:
  - Create customer via sync
  - Update with conflict
  - Delete with active delivery
  - Idempotency
  - Multi-operation batch

**معايير القبول:**
- [ ] POST /sync يقبل دفعة من العمليات.
- [ ] كل عملية تُعالج بشكل مستقل.
- [ ] النتائج تُرجع لكل عملية.
- [ ] Idempotency يمنع التكرار.
- [ ] Conflict يرجع server_state.

**التبعيات:** Phase 9.

---

## Phase 11 — Android ↔ Backend Integration

**الهدف:** ربط Android بالـ Backend (Authentication + APIs).

**المخرجات:**
- Retrofit + OkHttp setup.
- `AuthInterceptor` (يضيف `Authorization` header).
- `TokenAuthenticator` (يعمل refresh تلقائيًا).
- `ErrorInterceptor` (يحوّل HTTP errors لـ `ApiException`).
- `AuthRepositoryImpl` (login, refresh, logout).
- Token storage في `EncryptedSharedPreferences`.
- `LoginScreen` كامل (مرتبط بـ Backend).
- Session management (auto-login if token valid).
- `CustomerRepositoryImpl` (محدّث ليجرب API عند failure أو sync).

**معايير القبول:**
- [ ] Login حقيقي يعمل (Android → Backend).
- [ ] Token يُخزّن بشكل آمن.
- [ ] Auto-refresh يعمل عند 401.
- [ ] Logout يبطل الجلسة.
- [ ] GET /customers يعمل من Android.

**التبعيات:** Phase 8, 9, 6.

---

## Phase 12 — Offline Sync

**الهدف:** مزامنة تلقائية للعمليات المعلقة.

**المخرجات:**
- `SyncRepositoryImpl` (يقرأ من `sync_operations` ويستدعي `/sync`).
- `SyncWorker` (CoroutineWorker).
- WorkManager constraints (NetworkType.CONNECTED).
- `ScheduleSyncUseCase`.
- `ProcessPendingSyncUseCase`.
- `ObservePendingSyncCountUseCase` (لعرض شارة "X عمليات بانتظار المزامنة").
- Conflict handling على Android (تحديث Room بـ server_state).
- Retry logic مع backoff.
- Network monitor (يعرض Online/Offline في UI).

**معايير القبول:**
- [ ] العمليات المحلية تُزامَل تلقائيًا عند توفر الإنترنت.
- [ ] العمليات لا تضيع عند إغلاق التطبيق.
- [ ] Conflicts تُحل بشكل صحيح.
- [ ] Retry logic يعمل بعد الفشل.
- [ ] شارة "X بانتظار المزامنة" تظهر في الـ UI.

**التبعيات:** Phase 10, 11.

---

## Phase 13 — Delivery Flow

**الهدف:** تدفق التوصيل الكامل (state machine).

**المخرجات:**
- `StartDeliveryUseCase` (ينشئ Delivery بـ status=ON_THE_WAY).
- `CompleteDeliveryUseCase` (يحدّث لـ DELIVERED + completed_at).
- `CancelDeliveryUseCase`.
- `ActiveDeliveryScreen` + ViewModel.
- State machine validation (لا قفزات غير صالحة).
- ربط مع Room + Sync queue.

**معايير القبول:**
- [ ] بدء توصيل ينشئ Delivery في Room + sync queue.
- [ ] إكمال توصيل يحدّث الحالة ووقت الإكمال.
- [ ] إلغاء توصيل يعمل.
- [ ] الانتقالات غير الصالحة ممنوعة.
- [ ] كل العمليات تعمل offline.

**التبعيات:** Phase 6, 12.

---

## Phase 14 — Routing & ETA

**الهدف:** حساب الطريق من موقع المندوب إلى الزبون.

**المخرجات:**
- `RoutingEngine` interface.
- `MapboxRoutingEngine` implementation (Mapbox Directions API).
- `CalculateRouteUseCase`.
- عرض المسافة و ETA في الـ UI.
- خطأ handling (routing failure → fallback لمسافة مستقيمة).

**معايير القبول:**
- [ ] يحسب الطريق بشكل صحيح.
- [ ] يعرض المسافة و ETA.
- [ ] يتعامل مع فشل الـ routing.
- [ ] الأداء جيد (لا حصول route لكل ثانية).

**التبعيات:** Phase 5, 13.

---

## Phase 15 — Navigation & Alerts

**الهدف:** ملاحة turn-by-turn مع تنبيهات.

**المخرجات:**
- `NavigationEngine` interface.
- `MapboxNavigationEngine` implementation (Mapbox Navigation SDK).
- `NavigationScreen` + ViewModel.
- Turn-by-turn instructions.
- Voice alerts (Mapbox Voice API أو TTS).
- Re-routing عند الانحراف.
- Local notifications للتنبيهات (لكن فقط عندما التطبيق في الـ foreground).

**معايير القبول:**
- [ ] الملاحة تعمل على جهاز حقيقي.
- [ ] التنبيهات الصوتية تعمل.
- [ ] Re-routing يعمل عند الانحراف.
- [ ] الأداء جيد أثناء القيادة.

**التبعيات:** Phase 14.

---

## Phase 16 — Arrival Detection

**الهدف:** كشف وصول المندوب لموقع الزبون.

**المخرجات:**
- `ArrivalDetector` class.
- حساب المسافة الحالية من الزبون.
- Threshold configurable (افتراضي: 50m).
- GPS drift handling (نأخذ متوسط آخر 3 قراءات).
- إظهار "وصلت إلى موقع الزبون" عند الوصول.
- ربط مع `ActiveDeliveryScreen` (يظهر زر "تم التسليم").

**معايير القبول:**
- [ ] الكشف يعمل على جهاز حقيقي.
- [ ] لا يُعتبر وصول من شارع مقابل.
- [ ] زر "تم التسليم" يظهر فقط عند الوصول.

**التبعيات:** Phase 15.

---

## Phase 17 — History

**الهدف:** سجل التوصيلات بفلترة (اليوم/الأسبوع/الشهر).

**المخرجات:**
- `HistoryScreen` + `HistoryViewModel`.
- `ObserveTodayDeliveriesUseCase`.
- `ObserveDeliveriesByDateRangeUseCase`.
- فلترة (Tabs: اليوم / الأسبوع / الشهر).
- إحصائيات سريعة (المكتمل، الملغى، الإجمالي).
- Pull-to-refresh.

**معايير القبول:**
- [ ] يعرض التوصيلات لليوم/الأسبوع/الشهر.
- [ ] الفلاتر تعمل.
- [ ] الإحصائيات صحيحة.
- [ ] الأداء جيد مع 100+ توصيل.

**التبعيات:** Phase 13.

---

## Phase 18 — Notifications (Local)

**الهدف:** تنبيهات محلية أثناء الملاحة.

**المخرجات:**
- `NotificationHelper` (إنشاء notification channels).
- Channel: `navigation_alerts`.
- Channel: `delivery_status`.
- إظهار notification عند:
  - "اقتربت من الزبون" (100m).
  - "وصلت إلى موقع الزبون".
  - "تم التسليم بنجاح".
- Permission handling (`POST_NOTIFICATIONS` API 33+).

**معايير القبول:**
- [ ] التنبيهات تظهر على جهاز حقيقي.
- [ ] Permission يُطلب بشكل صحيح.
- [ ] التنبيهات لا تزعج (rate-limited).

**التبعيات:** Phase 15, 16.

---

## Phase 19 — Settings

**الهدف:** صفحة إعدادات أساسية.

**المخرجات:**
- `SettingsScreen` + `SettingsViewModel`.
- تبديل Dark/Light mode.
- تبديل اللغة (عربي/إنجليزي لاحقًا).
- GPS accuracy threshold (slider).
- Arrive radius (slider).
- تسجيل الخروج.
- معلومات التطبيق (الإصدار).
- إعادة مزامنة يدوية (زر "Retry Sync").

**معايير القبول:**
- [ ] كل الإعدادات تُحفظ بشكل دائم.
- [ ] Dark/Light mode يعمل فورًا.
- [ ] تسجيل الخروج ينظف كل البيانات الحساسة.

**التبعيات:** Phase 12.

---

## Phase 20 — UI Polish (Dark Mode, RTL)

**الهدف:** تجربة مستخدم احترافية.

**المخرجات:**
- مراجعة كل screens للـ RTL.
- مراجعة الـ spacing, typography, colors.
- إضافة animations (transitions, button feedback).
- إضافة skeletons (loading states).
- إضافة empty states مع illustrations.
- إضافة error states مع retry.
- اختبار على أحجام شاشات مختلفة.

**معايير القبول:**
- [ ] RTL يعمل بشكل صحيح في كل screens.
- [ ] Dark mode يعمل في كل screens.
- [ ] Loading/Empty/Error states في كل screens.
- [ ] الأداء سلس على جهاز متوسط المواصفات.

**التبعيات:** Phase 19.

---

## Phase 21 — Security Hardening

**الهدف:** تأمين التطبيق والـ Backend.

**المخرجات:**
- مراجعة أمنية شاملة:
  - JWT storage آمن.
  - No secrets in code.
  - HTTPS فقط في الـ production.
  - Rate limiting على endpoints الحساسة.
  - Authorization على كل query.
  - Input validation على كل endpoint.
- Security testing:
  - محاولة الوصول لبيانات مندوب آخر.
  - SQL injection attempts.
  - XSS attempts (لو في admin panel لاحقًا).
  - Token tampering.
- إضافة security headers على Backend.

**معايير القبول:**
- [ ] لا توجد ثغرات معروفة.
- [ ] Authorization يمنع كل الوصول غير المصرّح.
- [ ] لا secrets في الكود.

**التبعيات:** Phase 12.

---

## Phase 22 — Testing (Unit + Integration)

**الهدف:** تغطية اختبارية شاملة.

**المخرجات:**
- Unit tests (Android):
  - Use cases
  - Repository (with fake DAOs/APIs)
  - ViewModels (with Turbine)
  - Validation
  - State transitions
- Database tests (Android, Robolectric):
  - DAOs
  - Migrations
- API tests (Backend):
  - All endpoints
  - Auth
  - Authorization
  - Validation
  - Idempotency
  - Sync
- Integration tests (Android + Backend):
  - Add Customer offline → sync → backend
  - Complete Delivery offline → sync → backend
- UI tests (Compose UI Test):
  - Login flow
  - Add Customer flow
  - Search flow

**معايير القبول:**
- [ ] Coverage ≥ 70% على الـ domain + data layers.
- [ ] Coverage ≥ 80% على الـ Backend.
- [ ] كل tests تمر في CI.

**التبعيات:** Phase 21.

---

## Phase 23 — Performance Optimization

**الهدف:** أداء سلس مع عدد كبير من الزبائن.

**المخرجات:**
- Marker clustering على الخريطة (إذا ≥ 50 markers).
- Viewport-based loading (لا نعرض markers خارج الشاشة).
- Pagination على `CustomerListScreen`.
- DB indexes (تأكد من وجودها).
- Lazy loading للصور إن وجدت.
- Memory profiling على Android Studio.
- API response caching (OkHttp Cache).
- Backend query optimization (EXPLAIN ANALYZE).

**معايير القبول:**
- [ ] التطبيق يعمل بسلاسة مع 5000+ زبون.
- [ ] Search < 100ms.
- [ ] Map rendering < 60fps.
- [ ] Memory < 200MB.
- [ ] API response < 500ms (95th percentile).

**التبعيات:** Phase 22.

---

## Phase 24 — Real Device Testing

**الهدف:** اختبار على أرض الواقع.

**المخرجات:**
- Test plan مفصّل.
- اختبارات على أجهزة حقيقية:
  - GPS قوي
  - GPS ضعيف
  - إنترنت قوي
  - إنترنت ضعيف
  - لا إنترنت
  - Background
  - شاشة مقفلة
  - حركة فعلية (مع سلامة السائق)
- تسجيل الـ crashes والـ ANRs.
- تحسينات بناءً على النتائج.

**معايير القبول:**
- [ ] كل الـ use cases تعمل على جهاز حقيقي.
- [ ] لا crashes حرجة.
- [ ] البطارية تستمر ليوم كامل مع استخدام متوسط.

**التبعيات:** Phase 23.

---

## Phase 25 — Backend Production Setup

**الهدف:** تجهيز الـ Backend للـ Production.

**المخرجات:**
- خادم Production (VPS أو Cloud).
- PostgreSQL Production (managed أو containerized).
- Automated backups (daily + weekly + retention).
- Restore testing (شهريًا).
- SSL certificates (Let's Encrypt).
- DNS configuration.
- Firewall (UFW).
- SSH key-only authentication.
- Log rotation.

**معايير القبول:**
- [ ] Backend يعمل على production server.
- [ ] HTTPS يعمل بشكل صحيح.
- [ ] Backups تعمل ومُختبرة.
- [ ] Restore يعمل.

**التبعيات:** Phase 22.

---

## Phase 26 — Docker & Nginx

**الهدف:** Containerization كاملة للـ Backend.

**المخرجات:**
- `Dockerfile` (multi-stage build).
- `docker-compose.yml` (production):
  - FastAPI (Gunicorn + Uvicorn workers)
  - PostgreSQL
  - Nginx
  - Redis (لاحقًا للـ rate limiting)
- `nginx.conf`:
  - Reverse proxy إلى FastAPI.
  - HTTPS termination.
  - Static files.
  - Rate limiting.
  - Gzip.
- Health checks.

**معايير القبول:**
- [ ] `docker compose up` يبدأ كل شيء.
- [ ] HTTPS يعمل.
- [ ] Health checks تعمل.

**التبعيات:** Phase 25.

---

## Phase 27 — CI/CD

**الهدف:** أتمتة الـ testing و deployment.

**المخرجات:**
- GitHub Actions:
  - Android CI (lint + tests + build).
  - Backend CI (lint + tests + migrations).
  - Deploy (on tag push).
- Branch protection rules.
- Required status checks.
- Auto-deploy على staging عند push لـ `develop`.
- Auto-deploy على production عند tag.

**معايير القبول:**
- [ ] CI يمر على كل PR.
- [ ] Deploy تلقائي على staging.
- [ ] Deploy تلقائي على production عند tag.

**التبعيات:** Phase 26.

---

## Phase 28 — Monitoring & Logging

**الهدف:** مراقبة الـ Production.

**المخرجات:**
- Backend:
  - Structured logging (JSON).
  - Sentry (أو بديل) للأخطاء.
  - Prometheus metrics (optional).
  - Uptime monitoring.
- Android:
  - Crashlytics (Firebase).
  - ANR reporting.
  - Custom events for key flows.

**معايير القبول:**
- [ ] كل خطأ في الـ production يصل للـ monitoring.
- [ ] Uptime monitoring يكتشف الأعطال.

**التبعيات:** Phase 27.

---

## Phase 29 — Release Build

**الهدف:** بناء نسخة Release موقّعة.

**المخرجات:**
- Keystore generation (تحفظ الـ key بأمان).
- `proguard-rules.pro` (obfuscation + optimization).
- `build.gradle.kts` release config:
  - `minifyEnabled = true`
  - `shrinkResources = true`
  - Signing config
- Build AAB (Android App Bundle).
- اختبر الـ release build على جهاز حقيقي.
- تأكد أن الـ logs مطفأة في الـ release.

**معايير القبول:**
- [ ] AAB يبني بنجاح.
- [ ] التطبيق يعمل بدون crashes.
- [ ] لا secrets في الـ APK.

**التبعيات:** Phase 24.

---

## Phase 30 — Play Store Preparation

**الهدف:** تجهيز كل متطلبات Play Store.

**المخرجات:**
- Store listing:
  - App name: وصلني / WASLNI
  - Short description
  - Full description
  - Screenshots (phone + tablet)
  - Feature graphic
  - App icon
- Privacy Policy URL.
- Terms of Service URL.
- Content rating questionnaire.
- Target audience.
- Data safety form.
- Internal testing track.
- Closed testing (مع 20 مستخدم).
- Production rollout ( staged: 10% → 50% → 100%).

**معايير القبول:**
- [ ] كل المتطلبات مكتملة.
- [ ] اختبار داخلي يمر.
- [ ] Production rollout ناجح.

**التبعيات:** Phase 29.

---

## Phase 31 — Documentation Finalization

**الهدف:** توثيق نهائي للمشروع.

**المخرجات:**
- تحديث `README.md` (root + android + backend).
- `docs/setup.md` — setup guide for new developers.
- `docs/deployment.md` — deployment guide.
- `docs/security.md` — security guidelines.
- `docs/troubleshooting.md` — common issues + solutions.
- API documentation (Swagger UI auto-generated).
- Architecture diagram (final version).
- Database diagram (final version).
- Changelog (`CHANGELOG.md`).

**معايير القبول:**
- [ ] كل الوثائق محدّثة.
- [ ] Setup guide يسمح لمطور جديد بالبدء في < 1 ساعة.
- [ ] API documentation كاملة.

**التبعيات:** Phase 30.

---

## Phase 32 — MVP Launch

**الهدف:** إطلاق الـ MVP للمستخدمين النهائيين.

**المخرجات:**
- Production rollout 100%.
- مراقبة الـ crashes والأداء لمدة 48 ساعة.
- جمع feedback أولي.
- تسجيل الـ issues الجديدة.
- تخطيط الـ Phase 2 (Route Optimization, Admin Panel, ...).

**معايير القبول:**
- [ ] لا crashes حرجة في أول 48 ساعة.
- [ ] الاستجابة من المستخدمين إيجابية بشكل عام.
- [ ] الـ Backend يستحمل الحمل.

**التبعيات:** Phase 31.

---

## 3. Dependency Graph

```
Phase 1 (Planning) ✅
    │
    ├── Phase 2 (Android Foundation)
    │       │
    │       ├── Phase 3 (Local Data)
    │       │       │
    │       │       └── Phase 5 (Maps) ← Phase 4 (GPS)
    │       │               │
    │       │               └── Phase 6 (Customer Mgmt)
    │       │                       │
    │       │                       └── Phase 13 (Delivery Flow)
    │       │                               │
    │       │                               ├── Phase 14 (Routing)
    │       │                               │       │
    │       │                               │       └── Phase 15 (Navigation)
    │       │                               │               │
    │       │                               │               └── Phase 16 (Arrival)
    │       │                               │                       │
    │       │                               │                       └── Phase 18 (Notifications)
    │       │                               │
    │       │                               └── Phase 17 (History)
    │       │
    │       └── Phase 4 (GPS)
    │
    └── Phase 7 (Backend Foundation)
            │
            └── Phase 8 (Backend Auth)
                    │
                    └── Phase 9 (Backend Customers & Deliveries)
                            │
                            └── Phase 10 (Backend Sync Endpoint)
                                    │
                                    └── Phase 11 (Android ↔ Backend Integration) ← Phase 6
                                            │
                                            └── Phase 12 (Offline Sync) ← Phase 10
                                                    │
                                                    └── Phase 19 (Settings)
                                                            │
                                                            └── Phase 20 (UI Polish)
                                                                    │
                                                                    └── Phase 21 (Security)
                                                                            │
                                                                            └── Phase 22 (Testing)
                                                                                    │
                                                                                    └── Phase 23 (Performance)
                                                                                            │
                                                                                            └── Phase 24 (Real Device)
                                                                                                    │
                                                                                                    └── Phase 25 (Production Setup)
                                                                                                            │
                                                                                                            └── Phase 26 (Docker + Nginx)
                                                                                                                    │
                                                                                                                    └── Phase 27 (CI/CD)
                                                                                                                            │
                                                                                                                            └── Phase 28 (Monitoring)
                                                                                                                                    │
                                                                                                                                    └── Phase 29 (Release Build)
                                                                                                                                            │
                                                                                                                                            └── Phase 30 (Play Store)
                                                                                                                                                    │
                                                                                                                                                    └── Phase 31 (Docs Final)
                                                                                                                                                            │
                                                                                                                                                            └── Phase 32 (MVP Launch) ✅
```

---

## 4. Timeline Estimate (Rough)

| المرحلة | المدة المقدّرة |
|--------|--------------|
| Phase 1 (Planning) | 1-2 أيام ✅ |
| Phase 2 (Android Foundation) | 2-3 أيام |
| Phase 3 (Local Data) | 2-3 أيام |
| Phase 4 (GPS) | 2-3 أيام |
| Phase 5 (Maps) | 3-4 أيام |
| Phase 6 (Customer Mgmt) | 4-5 أيام |
| Phase 7 (Backend Foundation) | 2-3 أيام |
| Phase 8 (Backend Auth) | 2-3 أيام |
| Phase 9 (Backend Customers & Deliveries) | 3-4 أيام |
| Phase 10 (Backend Sync) | 3-4 أيام |
| Phase 11 (Android ↔ Backend) | 3-4 أيام |
| Phase 12 (Offline Sync) | 3-4 أيام |
| Phase 13 (Delivery Flow) | 2-3 أيام |
| Phase 14 (Routing) | 2-3 أيام |
| Phase 15 (Navigation) | 4-5 أيام |
| Phase 16 (Arrival) | 2-3 أيام |
| Phase 17 (History) | 1-2 يوم |
| Phase 18 (Notifications) | 2-3 أيام |
| Phase 19 (Settings) | 1-2 يوم |
| Phase 20 (UI Polish) | 3-4 أيام |
| Phase 21 (Security) | 2-3 أيام |
| Phase 22 (Testing) | 5-7 أيام |
| Phase 23 (Performance) | 2-3 أيام |
| Phase 24 (Real Device) | 3-4 أيام |
| Phase 25 (Production Setup) | 2-3 أيام |
| Phase 26 (Docker + Nginx) | 1-2 يوم |
| Phase 27 (CI/CD) | 2-3 أيام |
| Phase 28 (Monitoring) | 1-2 يوم |
| Phase 29 (Release Build) | 1-2 يوم |
| Phase 30 (Play Store) | 2-3 أيام |
| Phase 31 (Docs Final) | 1-2 يوم |
| Phase 32 (MVP Launch) | 1-2 يوم |

**الإجمالي التقريبي:** 75-100 يوم عمل (لمطوّر واحد بدوام كامل).

---

## 5. Milestones

| Milestone | نهاية Phase | الإنجاز |
|-----------|------------|--------|
| **M1: Planning Complete** | Phase 1 | وثائق مرجعية كاملة |
| **M2: Android Core Ready** | Phase 6 | إدارة زبائن + خريطة + GPS |
| **M3: Backend Ready** | Phase 10 | Backend كامل + APIs + sync |
| **M4: Integration Ready** | Phase 12 | Android متصل بالـ Backend + Offline sync |
| **M5: Delivery Flow Ready** | Phase 17 | تدفق توصيل كامل + history |
| **M6: Production Ready** | Phase 24 | اختبار على جهاز حقيقي ناجح |
| **M7: MVP Launched** | Phase 32 | تطبيق على Play Store |

---

## 6. ما بعد الـ MVP

بعد Phase 32، ننتقل لـ:

- **Phase 2 (من PRD):** Route Optimization, Multi-Stop, Notifications (FCM), Earnings, Offline Maps.
- **Phase 3 (من PRD):** Admin Panel, Live Tracking, Driver Management, Analytics.

---

## 7. معايير الانتقال بين المراحل

قبل الانتقال من Phase N إلى Phase N+1:

- [ ] كل معايير القبول في Phase N مُنجزة.
- [ ] Unit tests للـ code الجديد مكتوبة وتنجح.
- [ ] لا توجد `TODO` حرجة في الكود.
- [ ] الـ code review تم.
- [ ] CI يمر.
- [ ] تم تحديث الـ worklog و الـ documentation اللازمة.

---

**نهاية وثيقة Build Phases Roadmap — WASLNI v1.0**
