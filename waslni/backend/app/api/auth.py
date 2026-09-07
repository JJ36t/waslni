"""Authentication endpoints: /auth/login, /auth/refresh, /auth/logout, /auth/me.

Wire format matches docs/04-api-contract.md §4.
"""
from fastapi import APIRouter, Depends, Request

from app.api.deps import AuthServiceDep, ActiveUser, get_client_ip
from app.core.config import settings
from app.core.exceptions import ErrorCodes, RateLimitError
from app.schemas.auth import (
    LoginRequest,
    LogoutRequest,
    RefreshRequest,
    TokenResponse,
    UserResponse,
)

router = APIRouter(prefix="/auth", tags=["auth"])


# === In-memory rate limiter for /auth/login ===
# Production should use Redis; this is fine for single-instance staging/dev.
# Tracks attempts per IP within a sliding 1-minute window.
_login_attempts: dict[str, list[float]] = {}
_LOGIN_WINDOW_SECONDS = 60.0


def _check_login_rate_limit(ip: str | None) -> None:
    """Allow at most LOGIN_RATE_LIMIT_PER_MINUTE attempts per minute per IP."""
    if not ip:
        return  # can't rate-limit without an IP — let it through (testing)

    import time

    now = time.monotonic()
    cutoff = now - _LOGIN_WINDOW_SECONDS

    # Purge old entries
    _login_attempts[ip] = [t for t in _login_attempts.get(ip, []) if t > cutoff]

    if len(_login_attempts[ip]) >= settings.LOGIN_RATE_LIMIT_PER_MINUTE:
        raise RateLimitError(
            code=ErrorCodes.RATE_LIMIT_EXCEEDED,
            message="Too many login attempts. Try again in a minute.",
        )

    _login_attempts[ip].append(now)


# === Endpoints ===

@router.post("/login", response_model=TokenResponse)
async def login(
    body: LoginRequest,
    request: Request,
    auth_service: AuthServiceDep,
) -> TokenResponse:
    """Authenticate with username + password, receive access + refresh tokens.

    Rate-limited to 5 attempts per minute per IP to slow brute-force attacks.
    """
    ip = get_client_ip(request)
    _check_login_rate_limit(ip)
    return await auth_service.login(
        username=body.username,
        password=body.password,
        ip_address=ip,
    )


@router.post("/refresh", response_model=TokenResponse)
async def refresh(
    body: RefreshRequest,
    request: Request,
    auth_service: AuthServiceDep,
) -> TokenResponse:
    """Exchange a valid refresh token for a new access + refresh pair.

    The old refresh token is revoked (rotation strategy) — clients must use
    the new refresh_token returned here for subsequent refreshes.
    """
    return await auth_service.refresh(
        refresh_token=body.refresh_token,
        ip_address=get_client_ip(request),
    )


@router.post("/logout", status_code=204)
async def logout(
    body: LogoutRequest,
    request: Request,
    auth_service: AuthServiceDep,
) -> None:
    """Revoke a refresh token.

    Idempotent — calling with an unknown or already-revoked token returns 204
    without raising.
    """
    await auth_service.logout(
        refresh_token=body.refresh_token,
        ip_address=get_client_ip(request),
    )
    return None


@router.get("/me", response_model=UserResponse)
async def me(user: ActiveUser) -> UserResponse:
    """Return the current authenticated user's profile."""
    return UserResponse.model_validate(user)
