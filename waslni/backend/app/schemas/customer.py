"""Pydantic schemas for customer endpoints.

Wire format matches docs/04-api-contract.md §5.
"""
from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator


# === Requests ===

class CustomerCreate(BaseModel):
    """POST /customers body."""

    name: str = Field(min_length=2, max_length=120)
    phone: str = Field(min_length=7, max_length=30)
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    accuracy: float | None = Field(default=None, ge=0)

    @field_validator("name")
    @classmethod
    def normalize_name(cls, v: str) -> str:
        return v.strip()

    @field_validator("phone")
    @classmethod
    def normalize_phone(cls, v: str) -> str:
        # Strip spaces, dashes, parentheses — store digits + leading + only
        import re
        return re.sub(r"[\s\-()]", "", v.strip())


class CustomerUpdate(BaseModel):
    """PATCH /customers/{id} body — all fields optional."""

    name: str | None = Field(default=None, min_length=2, max_length=120)
    phone: str | None = Field(default=None, min_length=7, max_length=30)
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    accuracy: float | None = Field(default=None, ge=0)

    @field_validator("name")
    @classmethod
    def normalize_name(cls, v: str | None) -> str | None:
        return v.strip() if v is not None else None

    @field_validator("phone")
    @classmethod
    def normalize_phone(cls, v: str | None) -> str | None:
        if v is None:
            return None
        import re
        return re.sub(r"[\s\-()]", "", v.strip())


# === Responses ===

class CustomerResponse(BaseModel):
    """Public customer representation."""

    model_config = ConfigDict(from_attributes=True)

    id: UUID
    name: str
    phone: str
    latitude: float
    longitude: float
    accuracy: float | None
    created_at: datetime
    updated_at: datetime
