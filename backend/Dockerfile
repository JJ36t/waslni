# Multi-stage build for smaller production image.
# Stage 1: build deps with uv (faster than pip alone)
# Stage 2: copy app + installed deps into a slim image

FROM python:3.11-slim AS builder

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# System deps for asyncpg / argon2 / etc.
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential libpq-dev curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --upgrade pip && pip install --user -r requirements.txt


# === Stage 2: runtime ===
FROM python:3.11-slim AS runtime

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH=/root/.local/bin:$PATH

WORKDIR /app

# Runtime system deps (no build-essential)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

# Copy installed Python packages
COPY --from=builder /root/.local /root/.local

# Copy application code
COPY . .

# Don't run as root in production — create a non-privileged user
RUN useradd -m -u 1000 waselni && chown -R waselni:waselni /app
USER waselni

EXPOSE 8000

# Healthcheck hits the /health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsS http://localhost:8000/health || exit 1

# Uvicorn with 4 workers — tune via env in docker-compose for prod
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
