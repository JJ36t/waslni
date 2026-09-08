"""Unit tests for CustomerService — business logic in isolation.

Unlike the API tests in test_customers.py (which go through the HTTP layer),
these tests call CustomerService methods directly. This lets us verify the
service's internal logic (duplicate detection, active delivery check, audit
logging) without the HTTP/Pydantic/validation overhead.

We use the real database (via the conftest fixtures) because the service's
logic is tightly coupled to the repository's SQL queries — mocking the
repository would test nothing meaningful.
"""
import pytest
from uuid import uuid4

from app.core.exceptions import ConflictError, ErrorCodes, NotFoundError
from app.schemas.customer import CustomerCreate, CustomerUpdate
from app.services.customer_service import CustomerService

pytestmark = pytest.mark.asyncio


class TestCustomerServiceCreate:
    async def test_create_returns_customer_with_id(
        self, customer_service: CustomerService, test_user
    ) -> None:
        result = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(
                name="محمد أحمد",
                phone="07801234567",
                latitude=31.978942,
                longitude=44.940127,
                accuracy=4.2,
            ),
        )
        assert result.id is not None
        assert result.name == "محمد أحمد"
        assert result.phone == "07801234567"
        assert result.accuracy == 4.2

    async def test_create_strips_whitespace_from_name(
        self, customer_service: CustomerService, test_user
    ) -> None:
        result = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(
                name="  محمد  ",
                phone="07801234567",
                latitude=31.0,
                longitude=44.0,
            ),
        )
        assert result.name == "محمد"

    async def test_create_strips_formatting_from_phone(
        self, customer_service: CustomerService, test_user
    ) -> None:
        result = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(
                name="Test",
                phone="0780-123-4567",
                latitude=31.0,
                longitude=44.0,
            ),
        )
        assert result.phone == "07801234567"

    async def test_create_duplicate_phone_raises_conflict(
        self, customer_service: CustomerService, test_user
    ) -> None:
        # First create succeeds
        await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(
                name="First",
                phone="07801234567",
                latitude=31.0,
                longitude=44.0,
            ),
        )

        # Second create with same phone → ConflictError
        with pytest.raises(ConflictError) as exc_info:
            await customer_service.create(
                driver_id=test_user.id,
                data=CustomerCreate(
                    name="Second",
                    phone="07801234567",
                    latitude=31.0,
                    longitude=44.0,
                ),
            )
        assert exc_info.value.code == ErrorCodes.DUPLICATE_PHONE

    async def test_create_same_phone_different_drivers_succeeds(
        self, customer_service: CustomerService, test_user, other_driver_user
    ) -> None:
        # Driver A creates with phone X
        await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(
                name="A's Customer",
                phone="07801234567",
                latitude=31.0,
                longitude=44.0,
            ),
        )

        # Driver B creates with same phone → OK (UNIQUE is per-driver)
        result = await customer_service.create(
            driver_id=other_driver_user.id,
            data=CustomerCreate(
                name="B's Customer",
                phone="07801234567",
                latitude=31.0,
                longitude=44.0,
            ),
        )
        assert result.name == "B's Customer"


class TestCustomerServiceGet:
    async def test_get_returns_customer(
        self, customer_service: CustomerService, test_user
    ) -> None:
        created = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(
                name="Test", phone="07801234567",
                latitude=31.0, longitude=44.0,
            ),
        )

        result = await customer_service.get(created.id, test_user.id)
        assert result.id == created.id
        assert result.name == "Test"

    async def test_get_unknown_raises_not_found(
        self, customer_service: CustomerService, test_user
    ) -> None:
        with pytest.raises(NotFoundError):
            await customer_service.get(uuid4(), test_user.id)

    async def test_get_other_drivers_customer_raises_not_found(
        self, customer_service: CustomerService, test_user, other_driver_user
    ) -> None:
        created = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(
                name="A's", phone="07801234567",
                latitude=31.0, longitude=44.0,
            ),
        )

        # Driver B tries to get → NotFoundError (not ForbiddenError — no info leak)
        with pytest.raises(NotFoundError):
            await customer_service.get(created.id, other_driver_user.id)


class TestCustomerServiceUpdate:
    async def test_update_name_only(
        self, customer_service: CustomerService, test_user
    ) -> None:
        created = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(
                name="Old Name", phone="07801234567",
                latitude=31.0, longitude=44.0,
            ),
        )

        updated = await customer_service.update(
            customer_id=created.id,
            driver_id=test_user.id,
            data=CustomerUpdate(name="New Name"),
        )
        assert updated.name == "New Name"
        assert updated.phone == "07801234567"  # unchanged

    async def test_update_phone_to_duplicate_raises_conflict(
        self, customer_service: CustomerService, test_user
    ) -> None:
        c1 = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="C1", phone="07801110000", latitude=31.0, longitude=44.0),
        )
        await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="C2", phone="07802220000", latitude=31.0, longitude=44.0),
        )

        with pytest.raises(ConflictError) as exc_info:
            await customer_service.update(
                customer_id=c1.id,
                driver_id=test_user.id,
                data=CustomerUpdate(phone="07802220000"),
            )
        assert exc_info.value.code == ErrorCodes.DUPLICATE_PHONE

    async def test_update_phone_to_same_value_succeeds(
        self, customer_service: CustomerService, test_user
    ) -> None:
        """Updating phone to the same value should NOT trigger duplicate."""
        created = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="Test", phone="07801234567", latitude=31.0, longitude=44.0),
        )

        updated = await customer_service.update(
            customer_id=created.id,
            driver_id=test_user.id,
            data=CustomerUpdate(phone="07801234567"),  # same phone
        )
        assert updated.phone == "07801234567"


class TestCustomerServiceDelete:
    async def test_delete_removes_customer(
        self, customer_service: CustomerService, test_user
    ) -> None:
        created = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="Test", phone="07801234567", latitude=31.0, longitude=44.0),
        )

        await customer_service.delete(created.id, test_user.id)

        with pytest.raises(NotFoundError):
            await customer_service.get(created.id, test_user.id)

    async def test_delete_with_active_delivery_raises_conflict(
        self, customer_service: CustomerService, delivery_service, test_user
    ) -> None:
        from app.schemas.delivery import DeliveryCreate

        created = await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="Test", phone="07801234567", latitude=31.0, longitude=44.0),
        )
        # Create an active delivery for this customer
        await delivery_service.create(
            driver_id=test_user.id,
            data=DeliveryCreate(customer_id=created.id),
        )

        with pytest.raises(ConflictError) as exc_info:
            await customer_service.delete(created.id, test_user.id)
        assert exc_info.value.code == ErrorCodes.CUSTOMER_HAS_ACTIVE_DELIVERY


class TestCustomerServiceList:
    async def test_list_returns_paginated_results(
        self, customer_service: CustomerService, test_user
    ) -> None:
        for i in range(5):
            await customer_service.create(
                driver_id=test_user.id,
                data=CustomerCreate(
                    name=f"Customer {i}",
                    phone=f"0780{i:07d}",
                    latitude=31.0,
                    longitude=44.0,
                ),
            )

        rows, total = await customer_service.list(
            driver_id=test_user.id, page=1, limit=3
        )
        assert len(rows) == 3
        assert total == 5

    async def test_list_search_by_name(
        self, customer_service: CustomerService, test_user
    ) -> None:
        await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="محمد أحمد", phone="07801110001", latitude=31.0, longitude=44.0),
        )
        await customer_service.create(
            driver_id=test_user.id,
            data=CustomerCreate(name="علي حسن", phone="07801110002", latitude=31.0, longitude=44.0),
        )

        rows, total = await customer_service.list(
            driver_id=test_user.id, search="محمد"
        )
        assert len(rows) == 1
        assert rows[0].name == "محمد أحمد"


# === Fixtures ===

@pytest.fixture
async def other_driver_user(db_session):
    from app.core.security import hash_password
    from app.models import User

    user = User(
        id=uuid4(),
        username="service_test_other_driver",
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
    from app.services.delivery_service import DeliveryService

    return DeliveryService(
        session=db_session,
        deliveries=DeliveryRepository(db_session),
        customers=CustomerRepository(db_session),
        idempotency=IdempotencyKeyRepository(db_session),
        audit=AuditLogRepository(db_session),
    )
