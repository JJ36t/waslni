"""Metrics endpoint for uptime + health monitoring.

Provides a lightweight /metrics endpoint that external monitoring tools
(UptimeRobot, Pingdom, BetterStack) can poll to verify the server is alive
and the database is reachable.

Returns:
  - status: "ok" / "degraded" / "down"
  - db: "ok" / "unreachable"
  - env: current environment
  - version: app version
  - uptime_seconds: how long the process has been running
  - active_connections: approximate DB pool usage

This endpoint is NOT authenticated — it's meant for external monitors.
It does NOT expose any business metrics (customer count, delivery count, etc.)
to avoid information leakage.
"""
import time
from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.database import get_db

router = APIRouter(tags=["meta"])

# Track process start time for uptime calculation
_PROCESS_START = time.monotonic()


@router.get("/metrics")
async def metrics(db: AsyncSession = Depends(get_db)):
    """Lightweight metrics endpoint for external monitoring.

    Returns system health + uptime. No business metrics (no info leak).
    """
    from sqlalchemy import text

    db_ok = True
    db_latency_ms = 0.0

    try:
        start = time.monotonic()
        await db.execute(text("SELECT 1"))
        db_latency_ms = round((time.monotonic() - start) * 1000, 2)
    except Exception:
        db_ok = False

    uptime_seconds = round(time.monotonic() - _PROCESS_START, 2)

    status = "ok" if db_ok else "degraded"

    return {
        "status": status,
        "db": "ok" if db_ok else "unreachable",
        "db_latency_ms": db_latency_ms,
        "env": settings.APP_ENV,
        "version": "1.0.0",
        "uptime_seconds": uptime_seconds,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
