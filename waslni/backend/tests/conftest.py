"""Pytest configuration.

Fixtures:
  - `client` — async httpx ASGITransport client bound to the FastAPI app.
  - `db_session` — isolated async session against the test database.
  - `test_user` / `test_token` — pre-created driver user + access token.

Test DB:
  - Tests expect a separate database (waslni_test) running on localhost:5432.
  - Truncated between test functions via the `db_session` fixture.
"""
from collections.abc import AsyncGenerator
from typing import Any
from uuid import uuid4

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.core.config import settings
from app.core.database import Base, get_db
from app.core.security import create_access_token, hash_password
from app.main import create_app
from app.models import User


# === Test database engine ===
# Uses the same DATABASE_URL but expects a separate `waslni_test` DB
# (the developer must create it before running tests).
TEST_DATABASE_URL = settings.DATABASE_URL.replace("/waslni", "/waslni_test")

test_engine = create_async_engine(TEST_DATABASE_URL, echo=False)
TestSessionLocal = async_sessionmaker(
    bind=test_engine,
    class_=AsyncSession,
    expire_on_commit=False,
)


@pytest.fixture(scope="session")
def event_loop():
    """Single event loop for the whole test session."""
    import asyncio

    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture(scope="session", autouse=True)
async def setup_database() -> AsyncGenerator[None, None]:
    """Create all tables once at the start of the session."""
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    await test_engine.dispose()


@pytest_asyncio.fixture(autouse=True)
async def truncate_tables() -> AsyncGenerator[None, None]:
    """Truncate all tables between tests for isolation."""
    async with test_engine.begin() as conn:
        for table in (
            "idempotency_keys",
            "audit_logs",
            "refresh_tokens",
            "deliveries",
            "customers",
            "users",
        ):
            await conn.execute(text(f"TRUNCATE TABLE {table} CASCADE"))
    yield


@pytest_asyncio.fixture
async def db_session() -> AsyncGenerator[AsyncSession, None]:
    """Yield a session bound to the test DB."""
    async with TestSessionLocal() as session:
        yield session


@pytest_asyncio.fixture
async def app_with_db(db_session: AsyncSession) -> Any:
    """FastAPI app instance with get_db overridden to use the test session."""
    app = create_app()

    async def override_get_db() -> AsyncGenerator[AsyncSession, None]:
        yield db_session
        # Don't commit — let the test control transactions
        await db_session.rollback()

    app.dependency_overrides[get_db] = override_get_db
    yield app
    app.dependency_overrides.clear()


@pytest_asyncio.fixture
async def client(app_with_db) -> AsyncGenerator[AsyncClient, None]:
    """HTTP client bound to the test app."""
    transport = ASGITransport(app=app_with_db)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Pre-created driver user."""
    user = User(
        id=uuid4(),
        username="test_driver",
        password_hash=hash_password("testpassword123"),
        role="driver",
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def auth_headers(test_user: User) -> dict[str, str]:
    """Authorization header for the test user."""
    token = create_access_token(user_id=test_user.id, role=test_user.role)
    return {"Authorization": f"Bearer {token}"}
