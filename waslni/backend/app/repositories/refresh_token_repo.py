"""Refresh token repository — data access for refresh_tokens table."""
import hashlib
from datetime import datetime, timedelta, timezone
from uuid import UUID, uuid4

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models import RefreshToken


def _hash_token(token: str) -> str:
    """SHA-256 hash of a refresh token for storage.

    We never store the raw token — only its hash. This way:
      - DB leaks don't expose valid tokens.
      - We can verify a token by hashing it and comparing.
    """
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


class RefreshTokenRepository:
    """Manages refresh token persistence."""

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def create(
        self,
        user_id: UUID,
        token: str,
        expires_at: datetime,
        device_info: str | None = None,
    ) -> RefreshToken:
        """Store a new refresh token (hashed)."""
        rt = RefreshToken(
            id=uuid4(),
            user_id=user_id,
            token_hash=_hash_token(token),
            expires_at=expires_at,
            revoked=False,
            device_info=device_info,
        )
        self.session.add(rt)
        await self.session.flush()
        return rt

    async def get_by_token(self, token: str) -> RefreshToken | None:
        """Look up a refresh token by its hash."""
        result = await self.session.execute(
            select(RefreshToken).where(RefreshToken.token_hash == _hash_token(token))
        )
        return result.scalar_one_or_none()

    async def revoke(self, token_id: UUID) -> None:
        """Mark a refresh token as revoked (logout)."""
        await self.session.execute(
            update(RefreshToken)
            .where(RefreshToken.id == token_id)
            .values(revoked=True)
        )

    async def revoke_atomically(self, token_id: UUID) -> bool:
        """Atomically revoke a token — returns True if the token was NOT already revoked.

        Uses UPDATE ... WHERE revoked = false RETURNING to prevent race conditions.
        If two requests try to revoke the same token simultaneously, only one succeeds.
        """
        from sqlalchemy import update as sql_update

        result = await self.session.execute(
            sql_update(RefreshToken)
            .where(RefreshToken.id == token_id, RefreshToken.revoked.is_(False))
            .values(revoked=True)
            .returning(RefreshToken.id)
        )
        return result.scalar_one_or_none() is not None

    async def revoke_all_for_user(self, user_id: UUID) -> int:
        """Revoke all refresh tokens for a user (logout-all-devices).

        Returns the number of tokens revoked.
        """
        result = await self.session.execute(
            update(RefreshToken)
            .where(RefreshToken.user_id == user_id, RefreshToken.revoked.is_(False))
            .values(revoked=True)
            .returning(RefreshToken.id)
        )
        return len(result.all())

    async def delete_expired(self, before: datetime | None = None) -> int:
        """Delete expired tokens — called by a periodic cleanup job.

        Returns the number of rows deleted.
        """
        cutoff = before or datetime.now(timezone.utc)
        result = await self.session.execute(
            __import__("sqlalchemy").delete(RefreshToken).where(RefreshToken.expires_at < cutoff)
        )
        return result.rowcount or 0

    @staticmethod
    def default_expiry() -> datetime:
        """Default refresh token expiry — REFRESH_TOKEN_EXPIRE_DAYS from settings."""
        return datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
