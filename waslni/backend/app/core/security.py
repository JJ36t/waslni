"""Security primitives: password hashing (Argon2id) and JWT tokens.

Why Argon2id over bcrypt:
  - Argon2 won the Password Hashing Competition (2015).
  - Resistant to GPU/ASIC attacks (memory-hard).
  - Recommended by OWASP for new applications.

JWT structure:
  - Access tokens carry `sub` (user_id), `role`, `type=access`, `exp`, `iat`.
  - Refresh tokens carry `sub` (user_id), `type=refresh`, `exp`, `iat`, `jti` (token id).
  - We sign with HS256 (symmetric) — simpler than RS256 for our scale.
"""
from __future__ import annotations

import uuid
from datetime import datetime, timedelta, timezone
from typing import Any, Literal

from jose import JWTError, jwt
from passlib.context import CryptContext

from app.core.config import settings


# === Password hashing ===
# Argon2id with default OWASP-recommended parameters.
# CryptContext handles verification of legacy hashes if we ever migrate.
pwd_context = CryptContext(
    schemes=["argon2", "bcrypt"],
    deprecated="auto",
    argon2__memory_cost=65536,   # 64 MB
    argon2__time_cost=3,
    argon2__parallelism=4,
)


def hash_password(password: str) -> str:
    """Hash a plain-text password using Argon2id."""
    return pwd_context.hash(password)


def verify_password(plain: str, hashed: str) -> bool:
    """Verify a plain password against a stored hash.

    Returns False on any failure (wrong password, unsupported scheme, etc.)
    — never raises, so callers don't leak timing info via exceptions.
    """
    try:
        return pwd_context.verify(plain, hashed)
    except Exception:
        return False


# === JWT tokens ===

TokenType = Literal["access", "refresh"]


def create_access_token(
    user_id: uuid.UUID,
    role: str,
    expires_delta: timedelta | None = None,
) -> str:
    """Create a short-lived access token (default 15 min)."""
    now = datetime.now(timezone.utc)
    expire = now + (expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES))
    payload: dict[str, Any] = {
        "sub": str(user_id),
        "role": role,
        "type": "access",
        "iat": now,
        "exp": expire,
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)


def create_refresh_token(
    user_id: uuid.UUID,
    jti: uuid.UUID,
    expires_delta: timedelta | None = None,
) -> str:
    """Create a long-lived refresh token (default 30 days).

    `jti` is the unique token ID — stored in DB so we can revoke it.
    """
    now = datetime.now(timezone.utc)
    expire = now + (expires_delta or timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS))
    payload: dict[str, Any] = {
        "sub": str(user_id),
        "jti": str(jti),
        "type": "refresh",
        "iat": now,
        "exp": expire,
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)


class TokenError(Exception):
    """Base error for token decode / validation failures."""


class TokenExpiredError(TokenError):
    """Token signature is valid but it has expired."""


class TokenInvalidError(TokenError):
    """Token signature is invalid or payload is malformed."""


def decode_token(token: str) -> dict[str, Any]:
    """Decode and validate a JWT.

    Returns the payload on success.
    Raises:
        TokenExpiredError: signature OK but `exp` is in the past.
        TokenInvalidError: bad signature, malformed, or missing fields.
    """
    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET,
            algorithms=[settings.JWT_ALGORITHM],
        )
    except jwt.ExpiredJWTError as e:
        raise TokenExpiredError("Token has expired") from e
    except JWTError as e:
        raise TokenInvalidError(f"Invalid token: {e}") from e

    # Required fields
    if "sub" not in payload or "type" not in payload or "exp" not in payload:
        raise TokenInvalidError("Token missing required fields")

    return payload
