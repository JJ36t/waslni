"""Audit log repository — records security-relevant events."""
from typing import Any
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession

from app.models import AuditLog


class AuditLogRepository:
    """Writes audit log entries.

    Audit logs are append-only — we never update or delete them in normal
    operation. Cleanup (if ever) is via a periodic job that archives old
    rows to cold storage.
    """

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def record(
        self,
        action: str,
        user_id: UUID | None = None,
        entity_type: str | None = None,
        entity_id: UUID | None = None,
        metadata: dict[str, Any] | None = None,
        ip_address: str | None = None,
    ) -> AuditLog:
        """Insert an audit log entry.

        `metadata` should NEVER contain:
          - Passwords
          - Full JWT tokens
          - Phone numbers or GPS coordinates
        Only non-sensitive context (e.g. {"attempt": "wrong_password"}).
        """
        entry = AuditLog(
            action=action,
            user_id=user_id,
            entity_type=entity_type,
            entity_id=entity_id,
            metadata_=metadata,
            ip_address=ip_address,
        )
        self.session.add(entry)
        await self.session.flush()
        return entry
