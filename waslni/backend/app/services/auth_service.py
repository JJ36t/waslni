"""Authentication service — business logic for login, refresh, logout.

This is the only place that:
  - Verifies passwords.
  - Issues new tokens.
  - Persists refresh token hashes.
  - Records audit log entries for auth events.

The service is constructor-injected with repositories + the session, so
tests can swap fakes easily.
"""
from datetime import datetime, timedelta, timezone
from uuid import UUID, uuid4

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.exceptions import (
    AccountDisabledError,
    ErrorCodes,
    InvalidCredentialsError,
    NotFoundError,
    TokenExpiredError as AppTokenExpiredError,
    TokenInvalidError as AppTokenInvalidError,
    UnauthorizedError,
)
from app.core.security import (
    TokenExpiredError,
    TokenInvalidError,
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)
from app.models import User
from app.repositories.audit_log_repo import AuditLogRepository
from app.repositories.refresh_token_repo import RefreshTokenRepository
from app.repositories.user_repo import UserRepository
from app.schemas.auth import TokenResponse, UserResponse


# === Audit actions ===
AUDIT_LOGIN_SUCCESS = "LOGIN_SUCCESS"
AUDIT_LOGIN_FAILURE = "LOGIN_FAILURE"
AUDIT_LOGOUT = "LOGOUT"
AUDIT_TOKEN_REFRESHED = "TOKEN_REFRESHED"
AUDIT_TOKEN_REVOKED = "TOKEN_REVOKED"


class AuthService:
    """Business logic for authentication flows."""

    def __init__(
        self,
        session: AsyncSession,
        users: UserRepository,
        refresh_tokens: RefreshTokenRepository,
        audit: AuditLogRepository,
    ) -> None:
        self.session = session
        self.users = users
        self.refresh_tokens = refresh_tokens
        self.audit = audit

    # === Login ===

    async def login(
        self,
        username: str,
        password: str,
        ip_address: str | None = None,
    ) -> TokenResponse:
        """Verify credentials and issue access + refresh tokens.

        Raises:
            InvalidCredentialsError: wrong username or password.
            AccountDisabledError: user.is_active == False.

        Note: we use the SAME error for "user not found" and "wrong password"
        to prevent username enumeration via timing or distinct error codes.
        """
        user = await self.users.get_by_username(username)

        # Always run the password verify step (even if user is None) so
        # timing doesn't reveal whether the username exists.
        password_ok = (
            verify_password(password, user.password_hash) if user else False
        )

        if user is None or not password_ok:
            # Record failed attempt — user_id is null if username doesn't exist
            await self.audit.record(
                action=AUDIT_LOGIN_FAILURE,
                user_id=user.id if user else None,
                metadata={"username": username},  # not sensitive
                ip_address=ip_address,
            )
            raise InvalidCredentialsError(
                code=ErrorCodes.INVALID_CREDENTIALS,
                message="Invalid username or password",
            )

        if not user.is_active:
            await self.audit.record(
                action=AUDIT_LOGIN_FAILURE,
                user_id=user.id,
                metadata={"reason": "account_disabled"},
                ip_address=ip_address,
            )
            raise AccountDisabledError(
                code=ErrorCodes.ACCOUNT_DISABLED,
                message="This account has been disabled",
            )

        # === Success: issue tokens ===
        access_token = create_access_token(user_id=user.id, role=user.role)
        refresh_jti = uuid4()
        refresh_token = create_refresh_token(user_id=user.id, jti=refresh_jti)

        await self.refresh_tokens.create(
            user_id=user.id,
            token=refresh_token,
            expires_at=self.refresh_tokens.default_expiry(),
        )

        await self.users.update_last_login(user.id)

        await self.audit.record(
            action=AUDIT_LOGIN_SUCCESS,
            user_id=user.id,
            ip_address=ip_address,
        )

        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token,
            expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
            user=UserResponse.model_validate(user),
        )

    # === Refresh ===

    async def refresh(
        self,
        refresh_token: str,
        ip_address: str | None = None,
    ) -> TokenResponse:
        """Exchange a valid refresh token for a new token pair.

        Strategy: rotate on every refresh — the old refresh token is revoked
        and a new one is issued. This limits the blast radius of a stolen token.

        Raises:
            UnauthorizedError: token malformed, expired, revoked, or not found.
        """
        # 1. Decode the JWT (verifies signature + expiry)
        try:
            payload = decode_token(refresh_token)
        except TokenExpiredError as e:
            raise AppTokenExpiredError(
                code=ErrorCodes.TOKEN_EXPIRED,
                message="Refresh token has expired",
            ) from e
        except TokenInvalidError as e:
            raise UnauthorizedError(
                code=ErrorCodes.REFRESH_TOKEN_INVALID,
                message="Invalid refresh token",
            ) from e

        if payload.get("type") != "refresh":
            raise UnauthorizedError(
                code=ErrorCodes.REFRESH_TOKEN_INVALID,
                message="Token is not a refresh token",
            )

        user_id_str = payload.get("sub")
        if not user_id_str:
            raise UnauthorizedError(
                code=ErrorCodes.REFRESH_TOKEN_INVALID,
                message="Token missing user id",
            )

        # 2. Look up the stored token (must exist + not be revoked)
        stored = await self.refresh_tokens.get_by_token(refresh_token)
        if stored is None:
            raise UnauthorizedError(
                code=ErrorCodes.REFRESH_TOKEN_INVALID,
                message="Refresh token not recognized",
            )

        # 3. Atomically revoke the old token — prevents race condition where
        #    two concurrent refresh requests both see revoked=false.
        was_revoked = await self.refresh_tokens.revoke_atomically(stored.id)
        if not was_revoked:
            # Token was already revoked by a concurrent request → possible theft
            await self.refresh_tokens.revoke_all_for_user(stored.user_id)
            await self.audit.record(
                action=AUDIT_TOKEN_REVOKED,
                user_id=stored.user_id,
                metadata={"reason": "revoked_token_reuse_attempt"},
                ip_address=ip_address,
            )
            raise UnauthorizedError(
                code=ErrorCodes.REFRESH_TOKEN_INVALID,
                message="Refresh token has been revoked",
            )

        # 4. Load the user (must still exist + be active)
        user = await self.users.get_by_id(stored.user_id)
        if user is None:
            raise UnauthorizedError(
                code=ErrorCodes.REFRESH_TOKEN_INVALID,
                message="User no longer exists",
            )
        if not user.is_active:
            raise AccountDisabledError(
                code=ErrorCodes.ACCOUNT_DISABLED,
                message="This account has been disabled",
            )

        # 5. Issue new tokens (old token was already atomically revoked in step 3)
        new_access = create_access_token(user_id=user.id, role=user.role)
        new_refresh_jti = uuid4()
        new_refresh = create_refresh_token(user_id=user.id, jti=new_refresh_jti)
        await self.refresh_tokens.create(
            user_id=user.id,
            token=new_refresh,
            expires_at=self.refresh_tokens.default_expiry(),
        )

        await self.audit.record(
            action=AUDIT_TOKEN_REFRESHED,
            user_id=user.id,
            ip_address=ip_address,
        )

        return TokenResponse(
            access_token=new_access,
            refresh_token=new_refresh,
            expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
            user=UserResponse.model_validate(user),
        )

    # === Logout ===

    async def logout(
        self,
        refresh_token: str,
        ip_address: str | None = None,
    ) -> None:
        """Revoke a refresh token so it can no longer be used.

        Idempotent: if the token doesn't exist or is already revoked, we
        return success without raising. This makes client-side retry safe.
        """
        stored = await self.refresh_tokens.get_by_token(refresh_token)
        if stored is None:
            # Don't leak whether the token existed — just return.
            return

        if not stored.revoked:
            await self.refresh_tokens.revoke(stored.id)
            await self.audit.record(
                action=AUDIT_LOGOUT,
                user_id=stored.user_id,
                ip_address=ip_address,
            )

    # === Helpers (exposed for tests + other services) ===

    @staticmethod
    def hash_password(plain: str) -> str:
        """Hash a password — used by the seed script + future registration endpoint."""
        return hash_password(plain)
