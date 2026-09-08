"""Delivery service — business logic for delivery creation + state transitions.

State machine (see docs/03-database.md §2.4):

    PENDING ──→ ASSIGNED ──→ ON_THE_WAY ──→ ARRIVED ──→ DELIVERED
       │           │              │              │
       └───────────┴──────────────┴──────────────┘
                          ↓
                      CANCELLED

Terminal states: DELIVERED, CANCELLED.

Idempotency:
  - PATCH /deliveries/{id}/status accepts an Idempotency-Key header.
  - The service records the (key, request_hash, response) so a retried
    request returns the cached response instead of double-executing.
"""
from datetime import datetime, timezone
from uuid import UUID, uuid4

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import (
    ConflictError,
    ErrorCodes,
    ForbiddenError,
    NotFoundError,
)
from app.models import Delivery
from app.repositories.audit_log_repo import AuditLogRepository
from app.repositories.customer_repo import CustomerRepository
from app.repositories.delivery_repo import DeliveryRepository
from app.repositories.idempotency_repo import IdempotencyKeyRepository
from app.schemas.customer import CustomerResponse
from app.schemas.delivery import DeliveryCreate, DeliveryResponse, DeliveryStatusEnum


# === Allowed transitions ===
_TRANSITIONS: dict[DeliveryStatusEnum, set[DeliveryStatusEnum]] = {
    DeliveryStatusEnum.PENDING:    {DeliveryStatusEnum.ASSIGNED, DeliveryStatusEnum.CANCELLED},
    DeliveryStatusEnum.ASSIGNED:   {DeliveryStatusEnum.ON_THE_WAY, DeliveryStatusEnum.CANCELLED},
    DeliveryStatusEnum.ON_THE_WAY: {DeliveryStatusEnum.ARRIVED, DeliveryStatusEnum.CANCELLED},
    DeliveryStatusEnum.ARRIVED:    {DeliveryStatusEnum.DELIVERED, DeliveryStatusEnum.CANCELLED},
    DeliveryStatusEnum.DELIVERED:  set(),  # terminal
    DeliveryStatusEnum.CANCELLED:  set(),  # terminal
}


def _is_allowed_transition(
    current: DeliveryStatusEnum, target: DeliveryStatusEnum
) -> bool:
    return target in _TRANSITIONS.get(current, set())


# === Audit actions ===
AUDIT_DELIVERY_STARTED = "START_DELIVERY"
AUDIT_DELIVERY_ARRIVED = "ARRIVE_DELIVERY"
AUDIT_DELIVERY_COMPLETED = "COMPLETE_DELIVERY"
AUDIT_DELIVERY_CANCELLED = "CANCEL_DELIVERY"


class DeliveryService:
    """CRUD + state machine for deliveries."""

    def __init__(
        self,
        session: AsyncSession,
        deliveries: DeliveryRepository,
        customers: CustomerRepository,
        idempotency: IdempotencyKeyRepository,
        audit: AuditLogRepository,
    ) -> None:
        self.session = session
        self.deliveries = deliveries
        self.customers = customers
        self.idempotency = idempotency
        self.audit = audit

    async def create(
        self,
        driver_id: UUID,
        data: DeliveryCreate,
        ip_address: str | None = None,
    ) -> DeliveryResponse:
        """Create a new delivery.

        The driver typically starts directly at ON_THE_WAY with started_at=now.

        Raises:
            NotFoundError: customer doesn't exist or belongs to another driver.
            ConflictError: customer already has an active delivery.
        """
        # Customer must exist + belong to this driver
        customer = await self.customers.get_by_id(data.customer_id, driver_id)
        if customer is None:
            raise NotFoundError(
                code=ErrorCodes.CUSTOMER_NOT_FOUND,
                message="Customer not found",
                details={"customer_id": str(data.customer_id)},
            )

        # Block if there's already an active delivery for this customer
        active = await self.deliveries.get_active_for_customer(data.customer_id, driver_id)
        if active is not None:
            raise ConflictError(
                code=ErrorCodes.CUSTOMER_HAS_ACTIVE_DELIVERY,
                message="Customer already has an active delivery",
                details={"existing_delivery_id": str(active.id)},
            )

        now = datetime.now(timezone.utc)
        started_at = now if data.status == DeliveryStatusEnum.ON_THE_WAY else None

        delivery = Delivery(
            id=uuid4(),
            customer_id=data.customer_id,
            driver_id=driver_id,
            status=data.status.value,
            created_at=now,
            started_at=started_at,
        )
        await self.deliveries.create(delivery)

        # Reload to populate customer relationship for the response
        refreshed = await self.deliveries.get_by_id(delivery.id, driver_id)

        await self.audit.record(
            action=AUDIT_DELIVERY_STARTED,
            user_id=driver_id,
            entity_type="DELIVERY",
            entity_id=delivery.id,
            ip_address=ip_address,
        )

        return _to_response(refreshed or delivery)

    async def get(
        self, delivery_id: UUID, driver_id: UUID
    ) -> DeliveryResponse:
        """Fetch a single delivery.

        Raises:
            NotFoundError: delivery doesn't exist or belongs to another driver.
        """
        delivery = await self._require_delivery(delivery_id, driver_id)
        return _to_response(delivery)

    async def list(
        self,
        driver_id: UUID,
        page: int = 1,
        limit: int = 50,
        status: DeliveryStatusEnum | None = None,
        from_date: datetime | None = None,
        to_date: datetime | None = None,
    ) -> tuple[list[DeliveryResponse], int]:
        """Paginated list of the driver's deliveries."""
        rows, total = await self.deliveries.list_deliveries(
            driver_id=driver_id,
            page=page,
            limit=limit,
            status=status.value if status else None,
            from_date=from_date,
            to_date=to_date,
        )
        return [_to_response(r) for r in rows], total

    async def transition(
        self,
        delivery_id: UUID,
        driver_id: UUID,
        target: DeliveryStatusEnum,
        idempotency_key: str | None = None,
        request_payload: dict | None = None,
        ip_address: str | None = None,
    ) -> DeliveryResponse:
        """Transition a delivery to a new status.

        State machine:
          - Validates the transition is allowed.
          - Sets the appropriate timestamp (started_at / arrived_at / completed_at / cancelled_at).
          - Records an audit log entry.

        Idempotency:
          - If `idempotency_key` is provided and was used before with the
            same request payload, the cached response is returned.
          - If the key was used with a DIFFERENT payload, raises 409.

        Raises:
            NotFoundError: delivery doesn't exist or belongs to another driver.
            ConflictError: invalid state transition OR idempotency conflict.
        """
        # === Idempotency check (before any state change) ===
        if idempotency_key and request_payload is not None:
            cached = await self.idempotency.lookup(idempotency_key, driver_id)
            if cached is not None:
                new_hash = IdempotencyKeyRepository.compute_request_hash(request_payload)
                if cached.request_hash != new_hash:
                    raise ConflictError(
                        code=ErrorCodes.IDEMPOTENCY_CONFLICT,
                        message="Idempotency-Key was used with a different request body",
                        details={"idempotency_key": idempotency_key},
                    )
                # Return cached response
                return DeliveryResponse.model_validate(cached.response)

        # === Load the delivery ===
        delivery = await self._require_delivery(delivery_id, driver_id)
        current = DeliveryStatusEnum(delivery.status)

        # === Validate transition ===
        if not _is_allowed_transition(current, target):
            raise ConflictError(
                code=ErrorCodes.INVALID_STATE_TRANSITION,
                message=f"Cannot transition from {current.value} to {target.value}",
                details={
                    "current_status": current.value,
                    "target_status": target.value,
                },
            )

        # === Apply the transition (conditional UPDATE prevents race conditions) ===
        now = datetime.now(timezone.utc)
        extra_fields: dict = {}
        audit_action: str

        if target == DeliveryStatusEnum.ARRIVED:
            extra_fields["arrived_at"] = now
            audit_action = AUDIT_DELIVERY_ARRIVED
        elif target == DeliveryStatusEnum.DELIVERED:
            extra_fields["completed_at"] = now
            audit_action = AUDIT_DELIVERY_COMPLETED
        elif target == DeliveryStatusEnum.CANCELLED:
            extra_fields["cancelled_at"] = now
            audit_action = AUDIT_DELIVERY_CANCELLED
        elif target == DeliveryStatusEnum.ON_THE_WAY:
            extra_fields["started_at"] = now
            audit_action = AUDIT_DELIVERY_STARTED
        else:
            audit_action = AUDIT_DELIVERY_STARTED

        # Conditional UPDATE: only succeeds if status hasn't changed since we read it
        updated = await self.deliveries.update_status(
            delivery_id=delivery_id,
            driver_id=driver_id,
            new_status=target.value,
            expected_current_status=current.value,  # ← race condition guard
            **extra_fields,
        )

        if updated is None:
            # Status changed between our read and the UPDATE — race condition
            raise ConflictError(
                code=ErrorCodes.INVALID_STATE_TRANSITION,
                message=f"Delivery status changed concurrently — please retry",
                details={
                    "expected_status": current.value,
                    "target_status": target.value,
                },
            )

        await self.audit.record(
            action=audit_action,
            user_id=driver_id,
            entity_type="DELIVERY",
            entity_id=delivery_id,
            ip_address=ip_address,
        )

        response = _to_response(updated or delivery)

        # === Cache the response under the idempotency key ===
        if idempotency_key and request_payload is not None:
            await self.idempotency.store(
                key=idempotency_key,
                user_id=driver_id,
                endpoint=f"PATCH /deliveries/{delivery_id}/status",
                request_payload=request_payload,
                response=response.model_dump(mode="json"),
                status_code=200,
            )

        return response

    # === Internals ===

    async def _require_delivery(
        self, delivery_id: UUID, driver_id: UUID
    ) -> Delivery:
        delivery = await self.deliveries.get_by_id(delivery_id, driver_id)
        if delivery is None:
            raise NotFoundError(
                code=ErrorCodes.DELIVERY_NOT_FOUND,
                message="Delivery not found",
                details={"delivery_id": str(delivery_id)},
            )
        return delivery


def _to_response(delivery: Delivery) -> DeliveryResponse:
    """Convert a Delivery ORM instance to a DeliveryResponse schema."""
    customer_response: CustomerResponse | None = None
    if delivery.customer is not None:
        customer_response = CustomerResponse.model_validate(delivery.customer)

    return DeliveryResponse(
        id=delivery.id,
        customer_id=delivery.customer_id,
        customer=customer_response,
        status=DeliveryStatusEnum(delivery.status),
        created_at=delivery.created_at,
        started_at=delivery.started_at,
        arrived_at=delivery.arrived_at,
        completed_at=delivery.completed_at,
        cancelled_at=delivery.cancelled_at,
    )
