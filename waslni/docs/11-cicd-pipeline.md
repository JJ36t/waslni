# WASLNI — CI/CD Pipeline

> Automated testing, building, and deployment for the WASLNI project.

## Overview

```
git push → GitHub Actions
              │
              ├── Android CI (lint + tests + APK)
              ├── Backend CI  (lint + tests + Docker)
              │
              └── git tag v1.0.0 → Deploy Pipeline
                                    │
                                    ├── Build Docker image → Push to registry
                                    ├── SSH → server → pull + restart + migrate
                                    ├── Build AAB → Upload to GitHub Release
                                    └── Create GitHub Release (auto notes)
```

## Workflows

### 1. Android CI (`android-ci.yml`)
**Triggers:** PR to develop/main with `android/**` changes, push to develop/main.

| Step | Action | Tool |
|------|--------|------|
| 1 | Checkout | actions/checkout@v4 |
| 2 | Setup JDK 17 | actions/setup-java@v4 |
| 3 | Cache Gradle | actions/cache@v4 |
| 4 | Create local.properties | Mapbox tokens from secrets |
| 5 | Lint | ktlint + detekt (non-blocking for now) |
| 6 | Unit Tests | `./gradlew testDebugUnitTest` |
| 7 | Upload test results | artifact |
| 8 | Build Debug APK | `./gradlew assembleDebug` |
| 9 | Upload APK | artifact (14-day retention) |
| 10 | Build Release AAB | only on `main` branch, needs signing secrets |
| 11 | Upload AAB | artifact (30-day retention) |

**Required Secrets:**
- `MAPBOX_ACCESS_TOKEN` — Mapbox SDK access token
- `MAPBOX_DOWNLOADS_TOKEN` — Mapbox Maven repo token
- `WASLNI_KEYSTORE_FILE` — path to keystore (on main only)
- `WASLNI_KEYSTORE_PASSWORD` — keystore password
- `WASLNI_KEY_ALIAS` — key alias
- `WASLNI_KEY_PASSWORD` — key password

### 2. Backend CI (`backend-ci.yml`)
**Triggers:** PR to develop/main with `backend/**` changes, push to develop/main.

| Step | Action | Tool |
|------|--------|------|
| 1 | Checkout | actions/checkout@v4 |
| 2 | Start PostgreSQL service | postgres:15-alpine container |
| 3 | Setup Python 3.11 | actions/setup-python@v5 |
| 4 | Install dependencies | pip install requirements*.txt |
| 5 | Lint | ruff check |
| 6 | Format check | black --check |
| 7 | Type check | mypy (non-blocking) |
| 8 | Run migrations | alembic upgrade head |
| 9 | Tests with coverage | pytest --cov |
| 10 | Upload coverage | codecov |
| 11 | Security audit | pip-audit (non-blocking) |
| 12 | Build Docker image | docker build |

**Environment Variables (set in workflow):**
- `DATABASE_URL` — points to the PostgreSQL service container
- `JWT_SECRET` — CI-only secret (not production)
- `APP_ENV=test`

### 3. Deploy (`deploy.yml`)
**Triggers:** Tag push `v*` (e.g., `v1.0.0`).

| Step | Action | Tool |
|------|--------|------|
| 1 | Extract version from tag | `v1.0.0` |
| 2 | Build Docker image | tagged with version + latest |
| 3 | Login to container registry | Docker credentials from secrets |
| 4 | Push Docker image | version tag + latest tag |
| 5 | SSH into production server | appleboy/ssh-action |
| 6 | Deploy on server | git pull → docker compose pull → restart → migrate |
| 7 | Health check | curl /health |
| 8 | Create GitHub Release | auto-generated release notes |
| 9 | Build Release AAB | with signing secrets |
| 10 | Upload AAB to release | attached to GitHub release |

**Required Secrets:**
- `DOCKER_REGISTRY` — container registry URL
- `DOCKER_USER` — registry username
- `DOCKER_PASSWORD` — registry password
- `PROD_SERVER_HOST` — production server IP/hostname
- `PROD_SERVER_USER` — SSH username (waselni)
- `PROD_SERVER_SSH_KEY` — SSH private key
- Mapbox + signing secrets (same as Android CI)

## Branch Protection Rules

Configure in GitHub → Settings → Branches:

### `main` branch
- Require pull request before merging
- Require status checks: `android-ci`, `backend-ci`
- Require branches up to date before merging
- Require signed commits (recommended)
- Dismiss stale pull request approvals when new commits are pushed

### `develop` branch
- Require pull request before merging
- Require status checks: `android-ci`, `backend-ci`
- Allow force pushes (for rebasing)

## Required GitHub Secrets

Navigate to Settings → Secrets and Variables → Actions:

| Secret | Used By | Description |
|--------|---------|-------------|
| `MAPBOX_ACCESS_TOKEN` | Android CI, Deploy | Mapbox SDK access token |
| `MAPBOX_DOWNLOADS_TOKEN` | Android CI, Deploy | Mapbox Maven repo token |
| `WASLNI_KEYSTORE_FILE` | Android CI (main), Deploy | Path to release keystore |
| `WASLNI_KEYSTORE_PASSWORD` | Android CI (main), Deploy | Keystore password |
| `WASLNI_KEY_ALIAS` | Android CI (main), Deploy | Key alias in keystore |
| `WASLNI_KEY_PASSWORD` | Android CI (main), Deploy | Key password |
| `DOCKER_REGISTRY` | Deploy | Container registry URL |
| `DOCKER_USER` | Deploy | Registry username |
| `DOCKER_PASSWORD` | Deploy | Registry password/token |
| `PROD_SERVER_HOST` | Deploy | Production server hostname |
| `PROD_SERVER_USER` | Deploy | SSH username |
| `PROD_SERVER_SSH_KEY` | Deploy | SSH private key |

## Release Process

### Creating a Release
```bash
# 1. Ensure develop is up to date and all tests pass
git checkout develop
git pull origin develop

# 2. Merge develop into main
git checkout main
git pull origin main
git merge develop
git push origin main

# 3. Create a tag
git tag -a v1.0.0 -m "Release v1.0.0 — MVP Launch"
git push origin v1.0.0

# 4. GitHub Actions automatically:
#    - Builds + pushes Docker image
#    - SSH into server → deploy
#    - Builds AAB → attaches to GitHub Release
#    - Creates GitHub Release with auto-generated notes
```

### Rollback
```bash
# SSH into server
ssh waselni@api.waslni.com

# Roll back to previous image
cd /opt/waselni/backend
docker compose -f docker-compose.prod.yml down backend
docker pull waselni-backend:v0.9.0  # previous version
# Edit docker-compose.prod.yml to use v0.9.0
docker compose -f docker-compose.prod.yml up -d backend

# Or: git revert + redeploy
git revert HEAD
git push origin main
./scripts/deploy.sh --migrate
```

## CI/CD Checklist

- [x] Android CI workflow (lint + tests + build)
- [x] Backend CI workflow (lint + tests + migrations + Docker)
- [x] Deploy workflow (tag → build → SSH → deploy + AAB)
- [x] GitHub Release auto-creation with notes
- [x] Test result artifact upload
- [x] Coverage report upload (Codecov)
- [x] Security audit (pip-audit)
- [x] Gradle caching for faster builds
- [x] PostgreSQL service container for backend tests
- [x] Branch protection rules documented
- [ ] ktlint/detekt as required check (Phase 22+)
- [ ] mypy as required check (Phase 22+)
- [ ] Auto-deploy to staging on develop push (future)
- [ ] Play Store auto-upload (Phase 30)
