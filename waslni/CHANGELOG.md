# Changelog

All notable changes to the WASLNI project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — 2026-09-08 — MVP Launch

### Added — Android App (147 Kotlin files)

#### Core Infrastructure
- **Clean Architecture + MVVM** with 4 layers (core, data, domain, presentation)
- **Hilt** dependency injection (7 modules: App, Database, Network, Repository, UseCase, Location, Maps, Sync, Monitoring, Serialization)
- **Jetpack Compose + Material 3** UI with Dark/Light/System theme modes (RTL Arabic)
- **Navigation Compose** with 12 routes (splash, login, home, customers, add/edit/details, active delivery, navigation, history, settings, capture location)

#### Location & Maps
- **LocationProvider** abstraction with FusedLocationProviderClient implementation
- **GPS accuracy threshold** (default 10m, configurable in Settings)
- **Permission flow** (rationale → system dialog → settings fallback for permanently denied)
- **Mapbox Maps SDK** with driver + customer markers (color-coded by delivery state)
- **Marker clustering** (grid-based, O(n), threshold 50 markers)
- **Mapbox Directions API** routing with straight-line fallback (offline)
- **Mapbox Navigation SDK** turn-by-turn with Arabic voice announcements (TTS)
- **Re-routing** on deviation from route
- **Arrival detection** with GPS drift handling (3-reading sliding window, min 2 readings, 50m radius)

#### Customer Management
- CRUD: add, edit (name/phone/location), delete (with active delivery check), search (name/phone)
- Duplicate phone detection (per-driver unique)
- GPS location capture with accuracy indicator (Excellent ≤5m / Good ≤threshold / Poor)
- Customer details screen with call + start delivery + edit + delete
- Customer list with avatar circles + search bar

#### Delivery Flow
- State machine: PENDING → ASSIGNED → ON_THE_WAY → ARRIVED → DELIVERED / CANCELLED
- Active delivery screen with status banner (color-coded), route info card (distance + ETA), arrival suggestion banner
- Complete + cancel with confirmation dialog
- Active delivery banner on Home screen
- Delivery history with Today/Week/Month tabs + stats card (total/completed/cancelled/completion rate)

#### Offline First & Sync
- **Room** local database (3 entities: Customer, Delivery, SyncOperation)
- **Sync queue** — every mutation enqueues a sync operation + schedules WorkManager
- **SyncWorker** (CoroutineWorker) — reads pending ops, calls POST /sync, processes results
- **Conflict resolution** — latest-write-wins for customers, idempotency for delivery completion
- **Server changes** — download direction for multi-device sync
- **SyncPreferences** — latest_sync_timestamp watermark for incremental sync
- **NetworkMonitor** — online/offline detection with NET_CAPABILITY_VALIDATED
- **Sync status badge** on Home screen (green=online, red=offline, blue=pending)

#### Authentication & Security
- JWT login (username + password) with access (15min) + refresh (30d, rotated) tokens
- **TokenAuthenticator** — auto-refresh on 401 with mutex (no concurrent refresh storms)
- **EncryptedSharedPreferences** for token storage (AES-256-GCM, Keystore-backed)
- **Network security config** with certificate pinning (Let's Encrypt ISRG Root X1 + X2)
- **ProGuard/R8** code obfuscation + resource shrinking
- **ErrorInterceptor** — unified HTTP → ApiException mapping with Arabic messages

#### Notifications
- **NotificationHelper** with 2 channels (delivery_status=LOW, navigation_alerts=HIGH)
- 4 notification types: approaching customer, arrived, delivery completed, delivery cancelled
- Vibration for high-priority alerts
- POST_NOTIFICATIONS permission handling (API 33+)

#### Monitoring
- **CrashReporter** wired to Firebase Crashlytics
- Global uncaught exception handler (crashes reported before app dies)
- Event breadcrumbs (login, logout, delivery, sync events)
- User ID correlation (UUID, not PII)

#### UI Polish
- **StateView** unified Loading/Empty/Error/Content rendering
- **SkeletonBox + SkeletonList** shimmering loading placeholders
- **WaselButton** with press scale animation (0.96x)
- **ConfirmationDialog** for destructive actions

#### Settings
- Theme toggle (System / Light / Dark) — immediate effect, persisted
- GPS accuracy threshold slider (1-50m)
- Arrival radius slider (10-200m)
- Manual "retry sync" button
- Logout with confirmation

### Added — Backend (62 Python files)

#### API (16 endpoints)
- **Auth**: POST /auth/login, POST /auth/refresh, POST /auth/logout, GET /auth/me
- **Customers**: GET (paginated+search), POST, GET/{id}, PATCH/{id}, DELETE/{id}
- **Deliveries**: GET (paginated+filtered), POST, GET/{id}, PATCH/{id}/status (with Idempotency-Key)
- **Sync**: POST /sync (batch operations + server_changes)
- **Meta**: GET /health, GET /api/v1/health, GET /api/v1/metrics

#### Infrastructure
- **FastAPI** with async SQLAlchemy 2.0 + asyncpg
- **PostgreSQL 15** with 6 tables (users, customers, deliveries, refresh_tokens, audit_logs, idempotency_keys)
- **14 database indexes** for query performance
- **Alembic** migrations (initial schema)
- **Argon2id** password hashing (OWASP-recommended: 64MB/3/4)
- **JWT** (HS256) with typed errors (TokenExpired vs TokenInvalid)
- **Unified error model** (9 exception subclasses + 20 error codes)
- **Security headers middleware** (HSTS, nosniff, DENY, no-store, no-referrer)
- **Rate limiting middleware** (5/min login, 100/min global, sliding window)
- **Structured logging** (structlog JSON with sensitive field redaction)
- **Query optimization** (EXPLAIN ANALYZE helper, QueryTimer, N+1 fix via selectinload)

#### Services
- **AuthService** — login, refresh (rotation + reuse detection), logout (idempotent)
- **CustomerService** — CRUD with duplicate phone detection + active delivery check
- **DeliveryService** — state machine + idempotency + authorization (scoped by driver_id)
- **SyncService** — batch processing, conflict resolution (latest-write-wins), server_changes

#### Docker & Deployment
- **Dockerfile** (multi-stage, non-root user, health check)
- **docker-compose.yml** (dev: PostgreSQL + FastAPI with hot reload)
- **docker-compose.prod.yml** (prod: PostgreSQL + Gunicorn(4 workers) + Nginx)
- **nginx.conf** (HTTPS + rate limiting + security headers + gzip + WebSocket)
- **Backup script** (daily 7-day + weekly 4-week retention)
- **Deploy script** (git pull + restart + migrate + health check)

#### CI/CD
- **Android CI** (lint + tests + build APK/AAB)
- **Backend CI** (lint + tests + coverage + pip-audit + Docker build)
- **Deploy pipeline** (tag → build Docker → push → SSH → deploy → AAB → GitHub Release)

### Added — Documentation (16 files)
- PRD, Architecture, Database, API Contract, Project Structure, Build Phases
- Security Checklist, Testing Strategy, Performance Targets
- Deployment Guide, CI/CD Pipeline, Monitoring
- Release Build Guide, Play Store, Privacy Policy, Developer Setup

### Added — Testing (350+ test methods)
- **Android**: 32 test files (domain models, DAOs, repositories, use cases, ViewModels, sync, location, maps, network, security, UI, notifications, settings)
- **Backend**: 13 test files (config, exceptions, security, health, auth, customers, deliveries, sync, security audit, customer service, delivery service)

### Security
- HTTPS + certificate pinning (Let's Encrypt)
- JWT (15-min access + 30-day refresh with rotation + reuse detection)
- Argon2id password hashing
- AES-256-GCM token storage (Android Keystore)
- Rate limiting (5/min login, 100/min API at both Nginx + app level)
- ProGuard/R8 code obfuscation
- No secrets in source (env vars only)
- Same 404 for "not found" and "not yours" (no information leak)
- SQL injection protection (parameterized queries, verified by tests)
- Input validation (Pydantic + Android init-block validation)
- Sensitive field redaction in logs (11 field names)
- 22 security audit tests (token tampering, cross-driver, SQL injection, validation, leakage)

### Performance
- Marker clustering (O(n) grid-based, threshold 50)
- OkHttp 10MB response cache
- LazyColumn with stable keys
- Flow + WhileSubscribed(5000) for battery
- Battery-aware location intervals (15s idle / 5s active / 3s navigation)
- 14 database indexes
- Eager loading (no N+1 queries)
- Pagination on all list endpoints
- Gunicorn 4 workers + connection pool (10+20)
- Gzip compression

## [Unreleased]

### Planned for v1.1.0
- Route optimization (multi-stop)
- Push notifications (FCM)
- Customer import (CSV)
- Analytics dashboard
- Admin panel (web)
- Live tracking
- Delivery zones
