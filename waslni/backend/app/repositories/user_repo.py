"""User repository — data access for the users table."""
from uuid import UUID

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import User


class UserRepository:
    """Encapsulates all DB access for User rows.

    Constructor-injected with an AsyncSession so each request gets its own
    session via the get_db() dependency.
    """

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get_by_id(self, user_id: UUID) -> User | None:
        """Fetch a user by primary key."""
        return await self.session.get(User, user_id)

    async def get_by_username(self, username: str) -> User | None:
        """Fetch a user by username — used by /auth/login."""
        result = await self.session.execute(
            select(User).where(User.username == username)
        )
        return result.scalar_one_or_none()

    async def update_last_login(self, user_id: UUID) -> None:
        """Set last_login_at = now() — called on successful login."""
        await self.session.execute(
            update(User)
            .where(User.id == user_id)
            .values(last_login_at=__import__("datetime").datetime.now(__import__("datetime").timezone.utc))
        )
