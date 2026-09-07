"""Idempotency key model — for safe retry of mutating operations."""
from datetime import datetime
from typing import Any
from uuid import uuid4

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, func
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class IdempotencyKey(Base):
    """Stored Idempotency-Key → response mapping.

    When a client sends a mutating request with an `Idempotency-Key` header:
      1. Server looks up the key.
      2. If found AND request_hash matches → return cached response.
      3. If found AND request_hash differs → return 409 IDEMPOTENCY_CONFLICT.
      4. If not found → execute the request, cache (key, hash, response).

    Used for: COMPLETE_DELIVERY (prevents double-completion on retry),
    and any other operation where retry would be destructive.

    Rows expire (default 24h) and are purged by a periodic job.
    """

    __tablename__ = "idempotency_keys"

    id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid4,
    )
    key: Mapped[str] = mapped_column(String(255), nullable=False, unique=True)
    user_id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    endpoint: Mapped[str] = mapped_column(String(100), nullable=False)
    request_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    response: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False)
    status_code: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
    expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )

    __table_args__ = (
        # Composite index for the lookup query: WHERE key = ? AND user_id = ?
        Index("idx_idempotency_key_user", "key", "user_id"),
        Index("idx_idempotency_expires", "expires_at"),
    )

    def __repr__(self) -> str:
        return f"<IdempotencyKey key={self.key!r} endpoint={self.endpoint!r}>"
