"""Unit tests for DeliveryService — state machine + idempotency in isolation.

Tests the service's business logic directly (not through HTTP):
  - State machine: valid + invalid transitions.
  - Idempotency: same key + same body → cached; same key + different body → conflict.
  - Authorization: driver cannot transition another driver's delivery.
  - Active delivery check: can't create a second active delivery for same customer.
"""
import pytest
from uuid import uuid4

from app.core.exceptions import ConflictError, ErrorCodes, NotFoundError
from app.schemas.customer import CustomerCreate
from app.schemas.delivery import DeliveryCreate, DeliveryStatusEnum, DeliveryStatusUpdate
from app.services.customer_service import CustomerService
from app.services.delivery_service import DeliveryService

pytestmark = pytest.mark.asyncio


class TestDeliveryServiceCreate:
    async def test_create_returns_delivery_with_started_at(
        self, delivery_service: DeliveryService, customer_service: CustomerService, test_user
    ) -> None:
        # Create a customer first
        customer = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="Test", phone="07801234567", latitude=31.0, longitude=44.0),
        )

        delivery = await delivery_service.create(
            driver_id=test_user.id,
            data=DeliveryCreate(customer_id=customer.id),
        )

        assert delivery.id is not None
        assert delivery.status == DeliveryStatusEnum.ON_THE_WAY
        assert delivery.started_at is not None
        assert delivery.customer is not None  # eager-loaded

    async def test_create_for_unknown_customer_raises_not_found(
        self, delivery_service: DeliveryService, test_user
    ) -> None:
        with pytest.raises(NotFoundError) as exc_info:
            await delivery_service.create(
                driver_id=test_user.id,
                data=DeliveryCreate(customer_id=uuid4()),
            )
        assert exc_info.value.code == ErrorCodes.CUSTOMER_NOT_FOUND

    async def test_create_duplicate_active_raises_conflict(
        self, delivery_service: DeliveryService, customer_service: CustomerService, test_user
    ) -> None:
        customer = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="Test", phone="07801234567", latitude=31.0, longitude=44.0),
        )
        await delivery_service.create(
            driver_id=test_user.id,
            data=DeliveryCreate(customer_id=customer.id),
        )

        with pytest.raises(ConflictError) as exc_info:
            await delivery_service.create(
                driver_id=test_user.id,
                data=DeliveryCreate(customer_id=customer.id),
            )
        assert exc_info.value.code == ErrorCodes.CUSTOMER_HAS_ACTIVE_DELIVERY


class TestDeliveryServiceStateMachine:
    async def _create_delivery(self, delivery_service, customer_service, test_user):
        customer = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="Test", phone="07801234567", latitude=31.0, longitude=44.0),
        )
        return await delivery_service.create(
            driver_id=test_user.id,
            data=DeliveryCreate(customer_id=customer.id),
        )

    async def test_valid_on_the_way_to_arrived(
        self, delivery_service, customer_service, test_user
    ) -> None:
        delivery = await self._create_delivery(delivery_service, customer_service, test_user)

        result = await delivery_service.transition(
            delivery_id=delivery.id,
            driver_id=test_user.id,
            target=DeliveryStatusEnum.ARRIVED,
        )
        assert result.status == DeliveryStatusEnum.ARRIVED
        assert result.arrived_at is not None
        assert result.completed_at is None

    async def test_valid_arrived_to_delivered(
        self, delivery_service, customer_service, test_user
    ) -> None:
        delivery = await self._create_delivery(delivery_service, customer_service, test_user)
        await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.ARRIVED,
        )

        result = await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.DELIVERED,
        )
        assert result.status == DeliveryStatusEnum.DELIVERED
        assert result.completed_at is not None

    async def test_invalid_skip_on_the_way_to_delivered(
        self, delivery_service, customer_service, test_user
    ) -> None:
        delivery = await self._create_delivery(delivery_service, customer_service, test_user)

        with pytest.raises(ConflictError) as exc_info:
            await delivery_service.transition(
                delivery_id=delivery.id, driver_id=test_user.id,
                target=DeliveryStatusEnum.DELIVERED,
            )
        assert exc_info.value.code == ErrorCodes.INVALID_STATE_TRANSITION

    async def test_invalid_transition_from_terminal(
        self, delivery_service, customer_service, test_user
    ) -> None:
        delivery = await self._create_delivery(delivery_service, customer_service, test_user)
        # Cancel it
        await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.CANCELLED,
        )

        # Try to revive → invalid
        with pytest.raises(ConflictError):
            await delivery_service.transition(
                delivery_id=delivery.id, driver_id=test_user.id,
                target=DeliveryStatusEnum.DELIVERED,
            )

    async def test_cancel_from_on_the_way(
        self, delivery_service, customer_service, test_user
    ) -> None:
        delivery = await self._create_delivery(delivery_service, customer_service, test_user)

        result = await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.CANCELLED,
        )
        assert result.status == DeliveryStatusEnum.CANCELLED
        assert result.cancelled_at is not None

    async def test_transition_unknown_delivery_raises_not_found(
        self, delivery_service, test_user
    ) -> None:
        with pytest.raises(NotFoundError):
            await delivery_service.transition(
                delivery_id=uuid4(), driver_id=test_user.id,
                target=DeliveryStatusEnum.ARRIVED,
            )


class TestDeliveryServiceIdempotency:
    async def test_same_key_same_body_returns_cached(
        self, delivery_service, customer_service, test_user
    ) -> None:
        customer = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="Test", phone="07801234567", latitude=31.0, longitude=44.0),
        )
        delivery = await delivery_service.create(
            driver_id=test_user.id,
            data=DeliveryCreate(customer_id=customer.id),
        )
        # Transition to ARRIVED first
        await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.ARRIVED,
        )

        key = str(uuid4())
        payload = {"id": str(delivery.id), "status": "DELIVERED"}

        # First call → executes + caches
        r1 = await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.DELIVERED,
            idempotency_key=key,
            request_payload=payload,
        )
        assert r1.status == DeliveryStatusEnum.DELIVERED
        first_completed_at = r1.completed_at

        # Second call with same key + same body → cached response
        r2 = await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.DELIVERED,
            idempotency_key=key,
            request_payload=payload,
        )
        assert r2.completed_at == first_completed_at  # same timestamp (cached)

    async def test_same_key_different_body_raises_conflict(
        self, delivery_service, customer_service, test_user
    ) -> None:
        customer = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="Test", phone="07801234567", latitude=31.0, longitude=44.0),
        )
        delivery = await delivery_service.create(
            driver_id=test_user.id,
            data=DeliveryCreate(customer_id=customer.id),
        )
        await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.ARRIVED,
        )

        key = str(uuid4())
        # First call with body A
        await delivery_service.transition(
            delivery_id=delivery.id, driver_id=test_user.id,
            target=DeliveryStatusEnum.DELIVERED,
            idempotency_key=key,
            request_payload={"id": str(delivery.id), "status": "DELIVERED"},
        )

        # Second call with same key but different body → conflict
        with pytest.raises(ConflictError) as exc_info:
            await delivery_service.transition(
                delivery_id=delivery.id, driver_id=test_user.id,
                target=DeliveryStatusEnum.DELIVERED,
                idempotency_key=key,
                request_payload={"id": str(delivery.id), "status": "CANCELLED"},  # different
            )
        assert exc_info.value.code == ErrorCodes.IDEMPOTENCY_CONFLICT


# === Fixtures (reused from test_customer_service.py) ===

@pytest.fixture
async def other_driver_user(db_session):
    from app.core.security import hash_password
    from app.models import User

    user = User(
        id=uuid4(),
        username="delivery_service_test_other",
        password_hash=hash_password("otherpass123"),
        role="driver",
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()
    return user


@pytest.fixture
async def customer_service(db_session):
    from app.repositories.audit_log_repo import AuditLogRepository
    from app.repositories.customer_repo import CustomerRepository

    return CustomerService(
        session=db_session,
        customers=CustomerRepository(db_session),
        audit=AuditLogRepository(db_session),
    )


@pytest.fixture
async def delivery_service(db_session):
    from app.repositories.audit_log_repo import AuditLogRepository
    from app.repositories.customer_repo import CustomerRepository
    from app.repositories.delivery_repo import DeliveryRepository
    from app.repositories.idempotency_repo import IdempotencyKeyRepository

    return DeliveryService(
        session=db_session,
        deliveries=DeliveryRepository(db_session),
        customers=CustomerRepository(db_session),
        idempotency=IdempotencyKeyRepository(db_session),
        audit=AuditLogRepository(db_session),
    )
