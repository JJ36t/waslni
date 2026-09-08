"""Customer service — business logic for customer CRUD.

Authorization:
  - Every method takes a `driver_id` and scopes all queries to it.
  - A driver can never read or modify another driver's customers.

Conflict resolution:
  - Duplicate phone detection on create + update.
  - Active delivery check on delete.
"""
from datetime import datetime, timezone
from uuid import UUID, uuid4

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import (
    ConflictError,
    ErrorCodes,
    NotFoundError,
)
from app.models import Customer
from app.repositories.audit_log_repo import AuditLogRepository
from app.repositories.customer_repo import CustomerRepository
from app.schemas.customer import CustomerCreate, CustomerResponse, CustomerUpdate


# === Audit actions ===
AUDIT_CUSTOMER_CREATED = "CREATE_CUSTOMER"
AUDIT_CUSTOMER_UPDATED = "UPDATE_CUSTOMER"
AUDIT_CUSTOMER_DELETED = "DELETE_CUSTOMER"


class CustomerService:
    """CRUD + business rules for customers."""

    def __init__(
        self,
        session: AsyncSession,
        customers: CustomerRepository,
        audit: AuditLogRepository,
    ) -> None:
        self.session = session
        self.customers = customers
        self.audit = audit

    async def create(
        self,
        driver_id: UUID,
        data: CustomerCreate,
        ip_address: str | None = None,
    ) -> CustomerResponse:
        """Create a new customer for the given driver.

        Raises:
            ConflictError: duplicate phone for this driver.
        """
        # Duplicate phone check
        existing = await self.customers.get_by_phone(data.phone, driver_id)
        if existing is not None:
            raise ConflictError(
                code=ErrorCodes.DUPLICATE_PHONE,
                message=f"Phone number {data.phone} is already registered",
                details={"existing_customer_id": str(existing.id)},
            )

        now = datetime.now(timezone.utc)
        customer = Customer(
            id=uuid4(),
            driver_id=driver_id,
            name=data.name,
            phone=data.phone,
            latitude=data.latitude,
            longitude=data.longitude,
            accuracy=data.accuracy,
            created_at=now,
            updated_at=now,
        )
        await self.customers.create(customer)

        await self.audit.record(
            action=AUDIT_CUSTOMER_CREATED,
            user_id=driver_id,
            entity_type="CUSTOMER",
            entity_id=customer.id,
            ip_address=ip_address,
        )

        return CustomerResponse.model_validate(customer)

    async def get(
        self, customer_id: UUID, driver_id: UUID
    ) -> CustomerResponse:
        """Fetch a single customer.

        Raises:
            NotFoundError: customer doesn't exist or belongs to another driver.
        """
        customer = await self._require_customer(customer_id, driver_id)
        return CustomerResponse.model_validate(customer)

    async def list(
        self,
        driver_id: UUID,
        page: int = 1,
        limit: int = 50,
        search: str | None = None,
    ) -> tuple[list[CustomerResponse], int]:
        """Paginated list of the driver's customers."""
        rows, total = await self.customers.list_customers(
            driver_id=driver_id, page=page, limit=limit, search=search
        )
        return [CustomerResponse.model_validate(r) for r in rows], total

    async def update(
        self,
        customer_id: UUID,
        driver_id: UUID,
        data: CustomerUpdate,
        ip_address: str | None = None,
    ) -> CustomerResponse:
        """Update a customer's fields.

        Only fields explicitly set (not None) in `data` are updated.

        Raises:
            NotFoundError: customer doesn't exist or belongs to another driver.
            ConflictError: new phone conflicts with another customer.
        """
        customer = await self._require_customer(customer_id, driver_id)

        # If phone is changing, check for duplicates
        if data.phone is not None and data.phone != customer.phone:
            dup = await self.customers.get_by_phone(
                data.phone, driver_id, exclude_id=customer_id
            )
            if dup is not None:
                raise ConflictError(
                    code=ErrorCodes.DUPLICATE_PHONE,
                    message=f"Phone number {data.phone} is already registered",
                    details={"existing_customer_id": str(dup.id)},
                )

        # Build the update dict (only non-None fields)
        updates = data.model_dump(exclude_none=True)
        if updates:
            updated = await self.customers.update_fields(
                customer_id=customer_id,
                driver_id=driver_id,
                **updates,
            )
            if updated is None:
                # Should not happen since we already verified existence
                raise NotFoundError(
                    code=ErrorCodes.CUSTOMER_NOT_FOUND,
                    message="Customer not found",
                )
            customer = updated

        await self.audit.record(
            action=AUDIT_CUSTOMER_UPDATED,
            user_id=driver_id,
            entity_type="CUSTOMER",
            entity_id=customer.id,
            ip_address=ip_address,
        )

        return CustomerResponse.model_validate(customer)

    async def delete(
        self,
        customer_id: UUID,
        driver_id: UUID,
        ip_address: str | None = None,
    ) -> None:
        """Delete a customer.

        Raises:
            NotFoundError: customer doesn't exist or belongs to another driver.
            ConflictError: customer has an active delivery (ON_THE_WAY or ARRIVED).
        """
        customer = await self._require_customer(customer_id, driver_id)

        # Block deletion if active delivery exists
        if await self.customers.has_active_delivery(customer_id):
            raise ConflictError(
                code=ErrorCodes.CUSTOMER_HAS_ACTIVE_DELIVERY,
                message="Cannot delete a customer with an active delivery",
                details={"customer_id": str(customer_id)},
            )

        await self.customers.delete(customer_id, driver_id)

        await self.audit.record(
            action=AUDIT_CUSTOMER_DELETED,
            user_id=driver_id,
            entity_type="CUSTOMER",
            entity_id=customer_id,
            ip_address=ip_address,
        )

    # === Internals ===

    async def _require_customer(
        self, customer_id: UUID, driver_id: UUID
    ) -> Customer:
        """Fetch a customer or raise NotFoundError.

        Same error whether the customer doesn't exist OR belongs to another
        driver — no information leak about other drivers' customer IDs.
        """
        customer = await self.customers.get_by_id(customer_id, driver_id)
        if customer is None:
            raise NotFoundError(
                code=ErrorCodes.CUSTOMER_NOT_FOUND,
                message="Customer not found",
                details={"customer_id": str(customer_id)},
            )
        return customer
