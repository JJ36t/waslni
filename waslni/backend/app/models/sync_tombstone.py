"""SyncTombstone model — records deletions for multi-device sync."""
from datetime import datetime
from uuid import uuid4

from sqlalchemy import DateTime, ForeignKey, Index, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class SyncTombstone(Base):
    """Records that an entity was deleted — used for download-direction sync.

    When Device A deletes a customer, a tombstone row is created. When Device B
    syncs, it receives the tombstone as a server_change with operation=DELETE,
    and removes the customer from its local Room database.
    """

    __tablename__ = "sync_tombstones"

    id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid4,
    )
    driver_id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    entity_id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )
    entity_type: Mapped[str] = mapped_column(String(30), nullable=False)
    deleted_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )

    __table_args__ = (
        Index("idx_tombstones_driver", "driver_id"),
        Index("idx_tombstones_entity", "entity_type", "entity_id"),
        Index("idx_tombstones_deleted_at", "deleted_at"),
    )
