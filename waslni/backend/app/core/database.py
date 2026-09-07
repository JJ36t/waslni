"""SQLAlchemy async engine + session factory.

Architecture:
  - `engine` — single async engine per process (asyncpg).
  - `AsyncSessionLocal` — factory for short-lived AsyncSession instances.
  - `get_db()` — FastAPI dependency that yields a session per request.

The session is committed at the end of a successful request and rolled back
on exception. Callers never commit manually — that's the dependency's job.
"""
from collections.abc import AsyncGenerator

from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase

from app.core.config import settings


class Base(DeclarativeBase):
    """Declarative base — all models inherit from this."""


# === Engine ===
# Pool settings are conservative — fast enough for our load, won't overwhelm Postgres.
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=settings.APP_DEBUG and settings.is_development,
    pool_pre_ping=True,   # check connection before checkout (handles DB restarts)
    pool_size=10,
    max_overflow=20,
    pool_timeout=30,
)


# === Session factory ===
AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,  # we don't want stale objects after commit
    autoflush=False,
    autocommit=False,
)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI dependency — yields a session, commits on success, rolls back on error.

    Usage:
        @app.get("/items")
        async def list_items(db: AsyncSession = Depends(get_db)):
            ...
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise


async def check_db_connection() -> bool:
    """Health-check: can we ping the database?

    Used by /health endpoint. Returns True on success, False on failure.
    """
    from sqlalchemy import text

    try:
        async with AsyncSessionLocal() as session:
            await session.execute(text("SELECT 1"))
        return True
    except Exception:
        return False
