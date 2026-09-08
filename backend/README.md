# Waselni — Backend (FastAPI)

Offline-first delivery navigation API for the Waselni Android app.

## Quick Start (Docker Compose)

```bash
# 1. Create .env from template
cp .env.example .env
# Edit .env: set JWT_SECRET to a strong random value

# 2. Start PostgreSQL + FastAPI
docker compose up -d

# 3. Run migrations
docker compose exec backend alembic upgrade head

# 4. Seed admin + test driver
docker compose exec backend python -m scripts.seed

# 5. Verify
curl http://localhost:8000/health
# {"status":"ok","db":"ok","env":"development","version":"1.0.0"}

# 6. Open Swagger UI
# http://localhost:8000/docs
```

## Quick Start (Local, no Docker)

```bash
# Prerequisites: Python 3.11+, PostgreSQL 15+
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt -r requirements-dev.txt

# Configure
cp .env.example .env
# Edit .env: set DATABASE_URL to your local Postgres

# Migrate
alembic upgrade head

# Seed
python -m scripts.seed

# Run dev server (hot reload)
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Testing

Tests expect a separate `waslni_test` database.

```bash
# Create the test DB once
psql -U postgres -c "CREATE DATABASE waslni_test;"
psql -U postgres -c "CREATE USER waselni WITH PASSWORD 'waselni';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE waslni_test TO waselni;"

# Run tests
pytest

# With coverage
pytest --cov=app --cov-report=term-missing

# Lint
ruff check .
black --check .
mypy app
```

## Default Credentials (dev only)

- **Admin:** `admin` / `admin12345`
- **Driver:** `driver_01` / `driver12345`

Change these in production via `scripts/seed.py --reset` after setting a real JWT_SECRET.

## API Documentation

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc
- OpenAPI JSON: http://localhost:8000/openapi.json

See `../docs/04-api-contract.md` for the full contract specification.

## Project Structure

```
backend/
├── app/
│   ├── api/          # FastAPI routers (v1.py, auth.py, customers.py, ...)
│   ├── core/         # config, database, security, exceptions, logging
│   ├── models/       # SQLAlchemy ORM (User, Customer, Delivery, ...)
│   ├── schemas/      # Pydantic schemas (request/response models)
│   ├── services/     # Business logic
│   ├── repositories/ # Data access (SQLAlchemy queries)
│   ├── middleware/   # Rate limiting, audit logging
│   ├── utils/        # Idempotency, validators
│   └── main.py       # App factory + entry point
│
├── migrations/       # Alembic
│   ├── env.py
│   └── versions/
│       └── 0001_initial_schema.py
│
├── scripts/
│   └── seed.py
│
├── tests/
│   ├── conftest.py
│   ├── test_config.py
│   ├── test_exceptions.py
│   ├── test_health.py
│   └── test_security.py
│
├── Dockerfile
├── docker-compose.yml
├── requirements.txt
├── requirements-dev.txt
├── alembic.ini
├── pytest.ini
└── .env.example
```

## Current Phase

**Phase 7 — Backend Foundation** ✅

What's done:
- Project structure + requirements
- Config (Pydantic Settings) + .env.example
- Async SQLAlchemy engine + session
- Security primitives (Argon2id + JWT)
- Unified error model
- 6 SQLAlchemy models (users, customers, deliveries, refresh_tokens, audit_logs, idempotency_keys)
- Initial Alembic migration
- FastAPI app with CORS + exception handlers + /health
- Dockerfile + docker-compose
- Seed script
- 4 test files (28 test methods)

What's next (Phase 8):
- /auth router (login, refresh, logout, me)
- AuthService
- get_current_user dependency
- Rate limiting on /auth/login

