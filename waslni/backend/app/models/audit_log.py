"""Audit log model — for security-relevant events."""
from datetime import datetime
from typing import Any
from uuid import uuid4

from sqlalchemy import DateTime, ForeignKey, Index, String, func
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class AuditLog(Base):
    """An audit log entry — security-relevant action record.

    We log:
      - LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT
      - CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER
      - START_DELIVERY, COMPLETE_DELIVERY, CANCEL_DELIVERY
      - TOKEN_REFRESHED, TOKEN_REVOKED

    We DO NOT log:
      - Passwords (ever)
      - Full JWT tokens
      - Sensitive payloads (phone numbers, locations)

    `metadata` is JSONB — flexible payload. We trust the service layer to
    put only non-sensitive fields here.
    """

    __tablename__ = "audit_logs"

    id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid4,
    )
    user_id: Mapped[str | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,  # null for failed logins (no user found)
    )
    action: Mapped[str] = mapped_column(String(50), nullable=False)
    entity_type: Mapped[str | None] = mapped_column(String(30), nullable=True)
    entity_id: Mapped[str | None] = mapped_column(
        UUID(as_uuid=True),
        nullable=True,
    )
    metadata_: Mapped[dict[str, Any] | None] = mapped_column(
        "metadata",
        JSONB,
        nullable=True,
    )
    ip_address: Mapped[str | None] = mapped_column(String(45), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )

    __table_args__ = (
        Index("idx_audit_user_date", "user_id", "created_at"),
        Index("idx_audit_action", "action"),
    )

    def __repr__(self) -> str:
        return f"<AuditLog id={self.id} action={self.action!r}>"
