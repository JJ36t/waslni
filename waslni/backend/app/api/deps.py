"""FastAPI dependencies shared across routers.

  - get_current_user  — extract + verify the JWT, return the User row.
  - get_current_active_user — additionally require is_active=True.
  - get_auth_service   — construct an AuthService bound to the request's session.

These are imported by routers via Depends(...).
"""
from typing import Annotated
from uuid import UUID

from fastapi import Depends, Header, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.exceptions import (
    AccountDisabledError,
    ErrorCodes,
    TokenExpiredError as AppTokenExpiredError,
    TokenInvalidError as AppTokenInvalidError,
    UnauthorizedError,
)
from app.core.security import (
    TokenExpiredError,
    TokenInvalidError,
    decode_token,
)
from app.models import User
from app.repositories.audit_log_repo import AuditLogRepository
from app.repositories.refresh_token_repo import RefreshTokenRepository
from app.repositories.user_repo import UserRepository
from app.services.auth_service import AuthService


# === Session dependency (re-exported for convenience) ===
DbSession = Annotated[AsyncSession, Depends(get_db)]


# === Auth service dependency ===
async def get_auth_service(session: DbSession) -> AuthService:
    """Construct an AuthService bound to the request's session."""
    return AuthService(
        session=session,
        users=UserRepository(session),
        refresh_tokens=RefreshTokenRepository(session),
        audit=AuditLogRepository(session),
    )


AuthServiceDep = Annotated[AuthService, Depends(get_auth_service)]


# === Current user dependency ===
async def get_current_user(
    session: DbSession,
    authorization: str | None = Header(default=None),
) -> User:
    """Validate the Bearer token and return the authenticated user.

    Raises:
        UnauthorizedError: missing/malformed Authorization header.
        AppTokenInvalidError: bad signature or wrong token type.
        AppTokenExpiredError: token has expired.
    """
    if authorization is None or not authorization.startswith("Bearer "):
        raise UnauthorizedError(
            code=ErrorCodes.UNAUTHORIZED,
            message="Missing or invalid Authorization header",
        )

    token = authorization.removeprefix("Bearer ").strip()
    if not token:
        raise UnauthorizedError(
            code=ErrorCodes.UNAUTHORIZED,
            message="Empty bearer token",
        )

    try:
        payload = decode_token(token)
    except TokenExpiredError as e:
        raise AppTokenExpiredError(
            code=ErrorCodes.TOKEN_EXPIRED,
            message="Access token has expired",
        ) from e
    except TokenInvalidError as e:
        raise AppTokenInvalidError(
            code=ErrorCodes.TOKEN_INVALID,
            message="Invalid access token",
        ) from e

    if payload.get("type") != "access":
        raise AppTokenInvalidError(
            code=ErrorCodes.TOKEN_INVALID,
            message="Token is not an access token",
        )

    user_id_str = payload.get("sub")
    if not user_id_str:
        raise AppTokenInvalidError(
            code=ErrorCodes.TOKEN_INVALID,
            message="Token missing user id",
        )

    try:
        user_id = UUID(user_id_str)
    except ValueError as e:
        raise AppTokenInvalidError(
            code=ErrorCodes.TOKEN_INVALID,
            message="Token has malformed user id",
        ) from e

    user_repo = UserRepository(session)
    user = await user_repo.get_by_id(user_id)
    if user is None:
        raise UnauthorizedError(
            code=ErrorCodes.UNAUTHORIZED,
            message="User no longer exists",
        )

    return user


CurrentUser = Annotated[User, Depends(get_current_user)]


# === Active user dependency ===
async def get_current_active_user(user: CurrentUser) -> User:
    """Like [get_current_user] but also requires is_active=True.

    Use this for any endpoint that performs actions. Use [get_current_user]
    only for read-only / informational endpoints where disabled users might
    still need to see their own status.
    """
    if not user.is_active:
        raise AccountDisabledError(
            code=ErrorCodes.ACCOUNT_DISABLED,
            message="This account has been disabled",
        )
    return user


ActiveUser = Annotated[User, Depends(get_current_active_user)]


# === Client IP extractor ===
def get_client_ip(request: Request) -> str | None:
    """Extract the client IP from a request, honoring X-Forwarded-For.

    Used by audit logging. Returns the first non-empty forwarded IP, or the
    direct client IP if no proxy headers are present.
    """
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        # X-Forwarded-For is comma-separated; first entry is the original client
        return forwarded.split(",")[0].strip()
    if request.client:
        return request.client.host
    return None
