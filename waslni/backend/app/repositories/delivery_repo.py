"""Delivery repository — data access for the deliveries table.

All queries are scoped by `driver_id` for authorization.
"""
from datetime import datetime
from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models import Customer, Delivery


class DeliveryRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    # === Reads ===

    async def get_by_id(self, delivery_id: UUID, driver_id: UUID) -> Delivery | None:
        """Fetch a delivery with its customer, scoped to the driver."""
        result = await self.session.execute(
            select(Delivery)
            .options(selectinload(Delivery.customer))
            .where(
                Delivery.id == delivery_id,
                Delivery.driver_id == driver_id,
            )
        )
        return result.scalar_one_or_none()

    async def list_deliveries(
        self,
        driver_id: UUID,
        page: int = 1,
        limit: int = 50,
        status: str | None = None,
        from_date: datetime | None = None,
        to_date: datetime | None = None,
    ) -> tuple[list[Delivery], int]:
        """Paginated list of a driver's deliveries with optional filters."""
        offset = (page - 1) * limit

        conditions = [Delivery.driver_id == driver_id]
        if status:
            conditions.append(Delivery.status == status)
        if from_date:
            conditions.append(Delivery.created_at >= from_date)
        if to_date:
            conditions.append(Delivery.created_at < to_date)

        # Count
        count_stmt = select(func.count()).select_from(Delivery).where(*conditions)
        total = (await self.session.execute(count_stmt)).scalar_one()

        # Data — eager-load customer for the response shape
        data_stmt = (
            select(Delivery)
            .options(selectinload(Delivery.customer))
            .where(*conditions)
            .order_by(Delivery.created_at.desc())
            .offset(offset)
            .limit(limit)
        )
        rows = (await self.session.execute(data_stmt)).scalars().all()

        return list(rows), total

    async def get_active_for_customer(
        self, customer_id: UUID, driver_id: UUID
    ) -> Delivery | None:
        """Return the active delivery (ON_THE_WAY/ARRIVED) for a customer, if any."""
        result = await self.session.execute(
            select(Delivery).where(
                Delivery.customer_id == customer_id,
                Delivery.driver_id == driver_id,
                Delivery.status.in_(["ON_THE_WAY", "ARRIVED"]),
            )
        )
        return result.scalar_one_or_none()

    # === Writes ===

    async def create(self, delivery: Delivery) -> Delivery:
        self.session.add(delivery)
        await self.session.flush()
        # Reload to populate the customer relationship
        await self.session.refresh(delivery, ["customer"])
        return delivery

    async def update_status(
        self,
        delivery_id: UUID,
        driver_id: UUID,
        new_status: str,
        expected_current_status: str | None = None,
        **extra_fields,
    ) -> Delivery | None:
        """Conditionally update a delivery's status.

        If `expected_current_status` is provided, the UPDATE only succeeds if the
        current status matches. This prevents race conditions where two concurrent
        requests try to transition the same delivery.

        Returns the updated delivery (with customer eager-loaded), or None if the
        conditional update didn't match any row (meaning the status changed
        between read and write — caller should treat as INVALID_STATE_TRANSITION).
        """
        from sqlalchemy import update as sql_update

        values = {"status": new_status, **extra_fields}

        conditions = [
            Delivery.id == delivery_id,
            Delivery.driver_id == driver_id,
        ]
        if expected_current_status is not None:
            conditions.append(Delivery.status == expected_current_status)

        result = await self.session.execute(
            sql_update(Delivery)
            .where(*conditions)
            .values(**values)
            .returning(Delivery)
        )
        delivery = result.scalar_one_or_none()
        if delivery is not None:
            await self.session.refresh(delivery, ["customer"])
        return delivery
