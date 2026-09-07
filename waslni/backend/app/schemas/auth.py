"""Pydantic schemas for authentication endpoints.

All schemas:
  - Validate input on the way in (Pydantic v2 with field validators).
  - Strip whitespace from user-provided strings.
  - Use ISO 8601 string format for timestamps on the way out.

The wire format matches docs/04-api-contract.md §4 exactly.
"""
from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator


# === Requests ===

class LoginRequest(BaseModel):
    """POST /auth/login body."""

    username: str = Field(
        min_length=3,
        max_length=50,
        description="The user's username (case-sensitive).",
    )
    password: str = Field(
        min_length=1,
        max_length=128,
        description="Plain-text password — verified against the stored Argon2id hash.",
    )

    @field_validator("username")
    @classmethod
    def normalize_username(cls, v: str) -> str:
        return v.strip()


class RefreshRequest(BaseModel):
    """POST /auth/refresh body."""

    refresh_token: str = Field(
        min_length=10,
        description="A valid, non-revoked refresh token issued by /auth/login or /auth/refresh.",
    )


class LogoutRequest(BaseModel):
    """POST /auth/logout body."""

    refresh_token: str = Field(
        min_length=10,
        description="The refresh token to revoke. After this call, it can no longer be used.",
    )


# === Responses ===

class UserResponse(BaseModel):
    """Public user representation — never includes password_hash."""

    model_config = ConfigDict(from_attributes=True)

    id: UUID
    username: str
    role: str
    is_active: bool
    last_login_at: datetime | None = None


class TokenResponse(BaseModel):
    """Returned by /auth/login and /auth/refresh."""

    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int = Field(
        description="Access token lifetime in seconds.",
    )
    user: UserResponse
