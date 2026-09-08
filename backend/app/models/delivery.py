"""Delivery model — a single delivery to a customer."""
from datetime import datetime
from uuid import uuid4

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    ForeignKey,
    Index,
    String,
    func,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class Delivery(Base):
    """A delivery to a customer.

    Lifecycle:
        PENDING → ASSIGNED → ON_THE_WAY → ARRIVED → DELIVERED
                              │             │
                              └─────────────┴──→ CANCELLED

    The CHECK constraint validates that timestamps are consistent with the
    status — e.g. a DELIVERED delivery must have started_at + completed_at
    set, while a PENDING delivery has all timestamps null.
    """

    __tablename__ = "deliveries"

    id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid4,
    )
    customer_id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("customers.id", ondelete="RESTRICT"),
        nullable=False,
    )
    driver_id: Mapped[str] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    status: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="PENDING",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
    started_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )
    arrived_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )
    completed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )
    cancelled_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )

    # === Relationships ===
    customer: Mapped["Customer"] = relationship(lazy="selectin")

    __table_args__ = (
        CheckConstraint(
            "status IN ('PENDING', 'ASSIGNED', 'ON_THE_WAY', 'ARRIVED', "
            "'DELIVERED', 'CANCELLED')",
            name="chk_delivery_status",
        ),
        # Timestamps must match status
        CheckConstraint(
            """
            (status = 'PENDING' AND started_at IS NULL AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'ASSIGNED' AND started_at IS NULL AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'ON_THE_WAY' AND started_at IS NOT NULL AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'ARRIVED' AND started_at IS NOT NULL AND arrived_at IS NOT NULL AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'DELIVERED' AND started_at IS NOT NULL AND completed_at IS NOT NULL AND cancelled_at IS NULL)
            OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
            """,
            name="chk_delivery_times",
        ),
        Index("idx_deliveries_driver_status", "driver_id", "status"),
        Index("idx_deliveries_driver_created", "driver_id", "created_at"),
        Index("idx_deliveries_customer", "customer_id"),
    )

    def __repr__(self) -> str:
        return (
            f"<Delivery id={self.id} customer_id={self.customer_id} "
            f"status={self.status}>"
        )
