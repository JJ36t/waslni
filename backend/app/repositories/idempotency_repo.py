"""Idempotency key repository — for safe retry of mutating operations."""
import hashlib
from datetime import datetime, timedelta, timezone
from typing import Any
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import IdempotencyKey


def _hash_request(payload: dict[str, Any]) -> str:
    """Stable SHA-256 of a request payload — for idempotency conflict detection."""
    import json

    body = json.dumps(payload, sort_keys=True, default=str).encode("utf-8")
    return hashlib.sha256(body).hexdigest()


class IdempotencyKeyRepository:
    """Stores Idempotency-Key → response mappings.

    Flow:
      1. Client sends a mutating request with `Idempotency-Key: <uuid>` header.
      2. Server computes request_hash of the body.
      3. Looks up the key:
         - Found + same hash  → return cached response.
         - Found + diff hash  → 409 IDEMPOTENCY_CONFLICT.
         - Not found          → execute, cache result.
    """

    DEFAULT_TTL = timedelta(hours=24)

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def lookup(
        self, key: str, user_id: UUID
    ) -> IdempotencyKey | None:
        """Fetch an existing idempotency record for this key + user."""
        result = await self.session.execute(
            select(IdempotencyKey).where(
                IdempotencyKey.key == key,
                IdempotencyKey.user_id == user_id,
            )
        )
        return result.scalar_one_or_none()

    async def store(
        self,
        key: str,
        user_id: UUID,
        endpoint: str,
        request_payload: dict[str, Any],
        response: dict[str, Any],
        status_code: int,
        ttl: timedelta | None = None,
    ) -> IdempotencyKey:
        """Persist a new idempotency record."""
        record = IdempotencyKey(
            key=key,
            user_id=user_id,
            endpoint=endpoint,
            request_hash=_hash_request(request_payload),
            response=response,
            status_code=status_code,
            expires_at=datetime.now(timezone.utc) + (ttl or self.DEFAULT_TTL),
        )
        self.session.add(record)
        await self.session.flush()
        return record

    @staticmethod
    def compute_request_hash(payload: dict[str, Any]) -> str:
        """Expose the hash function for conflict-check comparisons."""
        return _hash_request(payload)

    async def delete_expired(self) -> int:
        """Periodic cleanup of expired records. Returns count deleted."""
        from sqlalchemy import delete as sql_delete

        result = await self.session.execute(
            sql_delete(IdempotencyKey).where(
                IdempotencyKey.expires_at < datetime.now(timezone.utc)
            )
        )
        return result.rowcount or 0
