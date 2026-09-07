"""Seed the database with a default admin + a test driver.

Usage:
    python -m scripts.seed            # default
    python -m scripts.seed --reset    # drop + recreate (DEV ONLY)

Run inside the backend container:
    docker compose exec backend python -m scripts.seed
"""
import argparse
import asyncio
import sys

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError

from app.core.database import AsyncSessionLocal, engine
from app.core.security import hash_password
from app.models import User


async def seed() -> None:
    """Create admin + test driver if they don't exist."""
    async with AsyncSessionLocal() as session:
        # Admin
        await _ensure_user(
            session,
            username="admin",
            password="admin12345",  # noqa: S106 — dev only
            role="admin",
        )
        # Test driver
        await _ensure_user(
            session,
            username="driver_01",
            password="driver12345",  # noqa: S106 — dev only
            role="driver",
        )
        await session.commit()


async def _ensure_user(
    session,
    username: str,
    password: str,
    role: str,
) -> None:
    existing = await session.scalar(select(User).where(User.username == username))
    if existing is not None:
        print(f"User '{username}' already exists — skipping")
        return

    user = User(
        username=username,
        password_hash=hash_password(password),
        role=role,
        is_active=True,
    )
    session.add(user)
    try:
        await session.flush()
        print(f"Created user '{username}' (role={role}, password='{password}')")
    except IntegrityError as e:
        await session.rollback()
        print(f"Failed to create '{username}': {e}", file=sys.stderr)


async def reset() -> None:
    """Drop all rows from all tables. DEV ONLY — never run in prod."""
    async with engine.begin() as conn:
        # Order matters: children first
        for table in (
            "idempotency_keys",
            "audit_logs",
            "refresh_tokens",
            "deliveries",
            "customers",
            "users",
        ):
            from sqlalchemy import text

            await conn.execute(text(f"TRUNCATE TABLE {table} CASCADE"))
            print(f"Truncated {table}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed Waselni database")
    parser.add_argument("--reset", action="store_true", help="Truncate all tables first (DEV ONLY)")
    args = parser.parse_args()

    if args.reset:
        confirm = input("This will DELETE all data. Type 'yes' to continue: ")
        if confirm.strip().lower() != "yes":
            print("Aborted")
            sys.exit(0)
        asyncio.run(reset())

    asyncio.run(seed())
    print("Seed complete")


if __name__ == "__main__":
    main()
