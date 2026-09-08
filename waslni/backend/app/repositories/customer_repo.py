"""Customer repository — data access for the customers table.

All queries are scoped by `driver_id` so the service layer can enforce
authorization (a driver cannot read or modify another driver's customers).
"""
from uuid import UUID

from sqlalchemy import delete, func, select, update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models import Customer


class CustomerRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    # === Reads ===

    async def get_by_id(self, customer_id: UUID, driver_id: UUID) -> Customer | None:
        """Fetch a customer, scoped to the given driver."""
        result = await self.session.execute(
            select(Customer).where(
                Customer.id == customer_id,
                Customer.driver_id == driver_id,
            )
        )
        return result.scalar_one_or_none()

    async def get_by_phone(
        self, phone: str, driver_id: UUID, exclude_id: UUID | None = None
    ) -> Customer | None:
        """Find a customer by phone within a driver's collection.

        Used for duplicate detection. `exclude_id` lets the update flow
        exclude the customer being edited.
        """
        stmt = select(Customer).where(
            Customer.phone == phone,
            Customer.driver_id == driver_id,
        )
        if exclude_id is not None:
            stmt = stmt.where(Customer.id != exclude_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_customers(
        self,
        driver_id: UUID,
        page: int = 1,
        limit: int = 50,
        search: str | None = None,
    ) -> tuple[list[Customer], int]:
        """Paginated list of a driver's customers, optionally filtered by search.

        Returns (rows, total_count).
        """
        offset = (page - 1) * limit

        # Base filters
        conditions = [Customer.driver_id == driver_id]
        if search:
            # ILIKE for case-insensitive prefix match on name OR phone
            like = f"{search}%"
            conditions.append(
                (Customer.name.ilike(like)) | (Customer.phone.ilike(like))
            )

        # Count query
        count_stmt = select(func.count()).select_from(Customer).where(*conditions)
        total = (await self.session.execute(count_stmt)).scalar_one()

        # Data query
        data_stmt = (
            select(Customer)
            .where(*conditions)
            .order_by(Customer.name.asc())
            .offset(offset)
            .limit(limit)
        )
        rows = (await self.session.execute(data_stmt)).scalars().all()

        return list(rows), total

    # === Writes ===

    async def create(self, customer: Customer) -> Customer:
        self.session.add(customer)
        await self.session.flush()
        return customer

    async def update_fields(
        self, customer_id: UUID, driver_id: UUID, **fields
    ) -> Customer | None:
        """Update specific fields on a customer (scoped to driver_id)."""
        # Bump updated_at server-side
        from datetime import datetime, timezone
        fields["updated_at"] = datetime.now(timezone.utc)

        result = await self.session.execute(
            update(Customer)
            .where(Customer.id == customer_id, Customer.driver_id == driver_id)
            .values(**fields)
            .returning(Customer)
        )
        return result.scalar_one_or_none()

    async def delete(self, customer_id: UUID, driver_id: UUID) -> bool:
        """Delete a customer (scoped). Returns True if a row was deleted."""
        result = await self.session.execute(
            delete(Customer).where(
                Customer.id == customer_id,
                Customer.driver_id == driver_id,
            )
        )
        return (result.rowcount or 0) > 0

    async def has_active_delivery(self, customer_id: UUID) -> bool:
        """Check whether this customer has an ON_THE_WAY or ARRIVED delivery."""
        from app.models import Delivery

        result = await self.session.execute(
            select(func.count())
            .select_from(Delivery)
            .where(
                Delivery.customer_id == customer_id,
                Delivery.status.in_(["ON_THE_WAY", "ARRIVED"]),
            )
        )
        return (result.scalar_one() or 0) > 0
