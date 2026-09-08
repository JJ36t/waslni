# وصلني (WASLNI) — Delivery Navigation App

<div align="center">

**تطبيق المندوب — Offline-first delivery navigation for Android**

[![Android CI](https://github.com/your-org/waslni/actions/workflows/android-ci.yml/badge.svg)](https://github.com/your-org/waslni/actions/workflows/android-ci.yml)
[![Backend CI](https://github.com/your-org/waslni/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/your-org/waslni/actions/workflows/backend-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 🎯 ما هو وصلني؟

وصلني تطبيق Android للمندوب يساعده على:
- 📍 **حفظ الزبائن** بالاسم + رقم الموبايل + إحداثيات GPS الدقيقة (لا عناوين نصية)
- 🗺️ **الخريطة والملاحة** — Mapbox مع turn-by-turn + إرشادات صوتية بالعربية
- 📦 **إدارة التوصيلات** — state machine كامل (ON_THE_WAY → ARRIVED → DELIVERED)
- 📶 **Offline First** — كل العمليات تعمل بدون إنترنت + مزامنة تلقائية
- 🔔 **كشف الوصول** — تنبيه تلقائي عند الاقتراب من الزبون (GPS drift handling)
- 📊 **سجل التوصيلات** — يومي/أسبوعي/شهري مع إحصائيات

## 🏗️ Architecture

```
                    ┌─────────────────┐
                    │   Android App   │
                    │  (Kotlin +      │
                    │   Compose +     │
                    │   Hilt + Room)  │
                    └────────┬────────┘
                             │ HTTPS
                    ┌────────▼────────┐
                    │  FastAPI Backend │
                    │  (Python 3.11 +  │
                    │   SQLAlchemy +   │
                    │   PostgreSQL)    │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   PostgreSQL 15  │
                    │  (Source of      │
                    │   Truth)         │
                    └─────────────────┘
```

### Android Stack
| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Local DB | Room (Offline First) |
| Networking | Retrofit + OkHttp |
| Maps | Mapbox Maps + Navigation SDK |
| Background Sync | WorkManager |
| Crash Reporting | Firebase Crashlytics |
| Min SDK | 24 (Android 7.0) |

### Backend Stack
| Layer | Technology |
|-------|-----------|
| Framework | FastAPI |
| ORM | SQLAlchemy 2.0 (async) |
| Database | PostgreSQL 15 |
| Migrations | Alembic |
| Auth | JWT (HS256) + Argon2id |
| Container | Docker + Docker Compose |
| Reverse Proxy | Nginx (HTTPS + rate limiting) |

## 📁 Project Structure

```
waslni/
├── android/                    # Android app
│   ├── app/src/main/java/com/waslni/driver/
│   │   ├── core/              # Infrastructure (location, maps, network, security, monitoring)
│   │   ├── data/              # Data layer (Room, Retrofit, repositories, sync)
│   │   ├── domain/            # Domain layer (models, repositories, use cases)
│   │   ├── di/                # Hilt modules
│   │   └── presentation/      # UI (Compose screens + ViewModels)
│   ├── app/src/test/          # Unit tests (32 files, 350+ tests)
│   └── gradle/                # Version catalog
│
├── backend/                    # FastAPI backend
│   ├── app/
│   │   ├── api/               # Routers (auth, customers, deliveries, sync, metrics)
│   │   ├── core/              # Config, database, security, exceptions, logging
│   │   ├── models/            # SQLAlchemy ORM (6 tables)
│   │   ├── repositories/      # Data access
│   │   ├── schemas/           # Pydantic models
│   │   ├── services/          # Business logic
│   │   └── middleware/        # Security headers, rate limiting
│   ├── migrations/            # Alembic
│   ├── tests/                 # 13 test files, 150+ tests
│   ├── scripts/               # Seed + backup + deploy scripts
│   ├── nginx/                 # Production Nginx config
│   └── docker-compose.prod.yml
│
├── docs/                      # 16 documentation files
├── .github/workflows/         # CI/CD pipelines (3 workflows)
└── README.md                  # This file
```

## 🚀 Quick Start

### Backend (Docker)
```bash
cd backend
cp .env.example .env  # Edit JWT_SECRET + POSTGRES_PASSWORD
docker compose up -d
docker compose exec backend alembic upgrade head
docker compose exec backend python -m scripts.seed
# API: http://localhost:8000/docs
```

### Android (Android Studio)
```bash
cd android
cp local.properties.example local.properties  # Add MAPBOX_ACCESS_TOKEN
# Open in Android Studio → Run
```

### Default Credentials (dev only)
- **Admin:** `admin` / `admin12345`
- **Driver:** `driver_01` / `driver12345`

## 📖 Documentation

| # | Document | Description |
|---|----------|-------------|
| 01 | [PRD](docs/01-PRD.md) | Product Requirements Document |
| 02 | [Architecture](docs/02-architecture.md) | System architecture + tech stack |
| 03 | [Database](docs/03-database.md) | PostgreSQL + Room schema design |
| 04 | [API Contract](docs/04-api-contract.md) | OpenAPI specification |
| 05 | [Project Structure](docs/05-project-structure.md) | Repo layout + Git strategy |
| 06 | [Build Phases](docs/06-build-phases.md) | 32-phase roadmap |
| 07 | [Security Checklist](docs/07-security-checklist.md) | Pre-launch security audit |
| 08 | [Testing Strategy](docs/08-testing-strategy.md) | Test coverage + principles |
| 09 | [Performance Targets](docs/09-performance-targets.md) | Benchmarks + optimizations |
| 10 | [Deployment Guide](docs/10-deployment-guide.md) | Production server setup |
| 11 | [CI/CD Pipeline](docs/11-cicd-pipeline.md) | GitHub Actions workflows |
| 12 | [Monitoring](docs/12-monitoring.md) | Crashlytics + logging + uptime |
| 13 | [Release Build](docs/13-release-build-guide.md) | Signing + ProGuard + checklist |
| 14 | [Play Store](docs/14-play-store.md) | Store listing + data safety |
| 15 | [Privacy Policy](docs/15-privacy-policy.md) | Data collection + security |
| — | [Real Device Test Plan](docs/testing/real-device-test-plan.md) | 50+ test scenarios |

## 🧪 Testing

```bash
# Backend
cd backend && pytest --cov=app

# Android
cd android && ./gradlew testDebugUnitTest
```

**350+ test methods** across both platforms.

## 🔒 Security

- HTTPS + certificate pinning (Let's Encrypt)
- JWT (15-min access + 30-day refresh with rotation)
- Argon2id password hashing
- AES-256-GCM token storage (Android Keystore)
- Rate limiting (5/min login, 100/min API)
- ProGuard/R8 code obfuscation
- No secrets in source code (env vars only)

## 📄 License

MIT — see [LICENSE](LICENSE)

## 🤝 Contributing

1. Fork → feature branch (`feature/your-feature`)
2. Ensure CI passes (`android-ci` + `backend-ci`)
3. Open PR to `develop`
4. Code review → squash merge
5. `develop` → `main` → tag `v*` → auto-deploy

## 📞 Support

- Email: support@waslni.com
- Website: https://waslni.com

---

<div align="center">

**وصلني — بسيط، سريع، ويعمل دائماً.**

</div>
