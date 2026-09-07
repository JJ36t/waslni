"""Customer model — a driver's customer with GPS coordinates."""
from datetime import datetime
from uuid import uuid4

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    Float,
    ForeignKey,
    Index,
    Numeric,
    String,
    UniqueConstraint,
    func,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class Customer(Base):
    """A customer saved by a driver.

    The customer's "address" IS the geographic coordinate — no street, house
    number, or description field by design.

    Unique constraint: (driver_id, phone) — a driver cannot have two customers
    with the same phone number. Different drivers CAN have the same phone.
    """

    __tablename__ = "customers"

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
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    phone: Mapped[str] = mapped_column(String(30), nullable=False)
    latitude: Mapped[float] = mapped_column(
        Numeric(10, 7),
        nullable=False,
    )
    longitude: Mapped[float] = mapped_column(
        Numeric(10, 7),
        nullable=False,
    )
    accuracy: Mapped[float | None] = mapped_column(Float, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )

    __table_args__ = (
        # Composite unique: one (driver, phone) pair
        UniqueConstraint("driver_id", "phone", name="uq_driver_phone"),
        # Indexes for the common query patterns
        Index("idx_customers_driver_name", "driver_id", "name"),
        Index("idx_customers_driver_phone", "driver_id", "phone"),
        # CHECK constraints
        CheckConstraint(
            "char_length(name) >= 2",
            name="chk_customer_name_length",
        ),
        CheckConstraint(
            "char_length(phone) >= 7",
            name="chk_customer_phone_length",
        ),
        CheckConstraint(
            "latitude >= -90 AND latitude <= 90",
            name="chk_latitude",
        ),
        CheckConstraint(
            "longitude >= -180 AND longitude <= 180",
            name="chk_longitude",
        ),
        CheckConstraint(
            "accuracy IS NULL OR accuracy >= 0",
            name="chk_accuracy_non_negative",
        ),
    )

    def __repr__(self) -> str:
        return (
            f"<Customer id={self.id} driver_id={self.driver_id} "
            f"name={self.name!r} phone={self.phone!r}>"
        )
