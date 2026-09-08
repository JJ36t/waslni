"""Pydantic schemas for delivery endpoints.

Wire format matches docs/04-api-contract.md §6.
"""
from datetime import datetime
from enum import Enum
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.customer import CustomerResponse


class DeliveryStatusEnum(str, Enum):
    """Allowed values for the delivery status field."""

    PENDING = "PENDING"
    ASSIGNED = "ASSIGNED"
    ON_THE_WAY = "ON_THE_WAY"
    ARRIVED = "ARRIVED"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"


# === Requests ===

class DeliveryCreate(BaseModel):
    """POST /deliveries body."""

    customer_id: UUID = Field(description="The customer to deliver to.")
    status: DeliveryStatusEnum = Field(
        default=DeliveryStatusEnum.ON_THE_WAY,
        description="Initial status — defaults to ON_THE_WAY with started_at=now.",
    )


class DeliveryStatusUpdate(BaseModel):
    """PATCH /deliveries/{id}/status body."""

    status: DeliveryStatusEnum


# === Responses ===

class DeliveryResponse(BaseModel):
    """Public delivery representation."""

    model_config = ConfigDict(from_attributes=True)

    id: UUID
    customer_id: UUID
    customer: CustomerResponse | None = None
    status: DeliveryStatusEnum
    created_at: datetime
    started_at: datetime | None = None
    arrived_at: datetime | None = None
    completed_at: datetime | None = None
    cancelled_at: datetime | None = None
