"""Tests for /deliveries endpoints.

Covers:
  - Create delivery happy path + active delivery conflict
  - Get single delivery
  - List with pagination + filters
  - State machine: valid transitions, invalid transitions, terminal states
  - Idempotency-Key: cache hit on retry, conflict on different body
  - Authorization: driver A cannot access driver B's deliveries
"""
from uuid import uuid4

import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio


async def _create_customer(
    client: AsyncClient,
    headers: dict,
    name: str = "محمد",
    phone: str = "07801234567",
) -> dict:
    response = await client.post("/api/v1/customers", json={
        "name": name, "phone": phone,
        "latitude": 31.978942, "longitude": 44.940127, "accuracy": 4.2,
    }, headers=headers)
    return response.json()


async def _create_delivery(
    client: AsyncClient,
    headers: dict,
    customer_id: str,
    status: str = "ON_THE_WAY",
) -> dict:
    response = await client.post("/api/v1/deliveries", json={
        "customer_id": customer_id, "status": status,
    }, headers=headers)
    return response.json()


class TestCreateDelivery:
    async def test_create_success(self, client, auth_headers) -> None:
        customer = await _create_customer(client, auth_headers)
        body = await _create_delivery(client, auth_headers, customer["id"])

        assert body["status"] == "ON_THE_WAY"
        assert body["customer_id"] == customer["id"]
        assert body["started_at"] is not None
        assert body["completed_at"] is None
        # Customer nested in response
        assert body["customer"]["name"] == "محمد"

    async def test_create_for_unknown_customer_returns_404(
        self, client, auth_headers
    ) -> None:
        response = await client.post("/api/v1/deliveries", json={
            "customer_id": str(uuid4()), "status": "ON_THE_WAY",
        }, headers=auth_headers)
        assert response.status_code == 404
        assert response.json()["error"]["code"] == "CUSTOMER_NOT_FOUND"

    async def test_create_duplicate_active_delivery_conflict(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        await _create_delivery(client, auth_headers, customer["id"])

        # Second active delivery for same customer must fail
        response = await client.post("/api/v1/deliveries", json={
            "customer_id": customer["id"], "status": "ON_THE_WAY",
        }, headers=auth_headers)
        assert response.status_code == 409
        assert response.json()["error"]["code"] == "CUSTOMER_HAS_ACTIVE_DELIVERY"

    async def test_create_after_previous_delivered_succeeds(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        d1 = await _create_delivery(client, auth_headers, customer["id"])
        # Transition to DELIVERED via ARRIVED
        await client.patch(
            f"/api/v1/deliveries/{d1['id']}/status",
            json={"status": "ARRIVED"},
            headers=auth_headers,
        )
        await client.patch(
            f"/api/v1/deliveries/{d1['id']}/status",
            json={"status": "DELIVERED"},
            headers=auth_headers,
        )

        # Now a new delivery should be allowed
        response = await client.post("/api/v1/deliveries", json={
            "customer_id": customer["id"], "status": "ON_THE_WAY",
        }, headers=auth_headers)
        assert response.status_code == 201


class TestGetDelivery:
    async def test_get_success(self, client, auth_headers) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])
        response = await client.get(
            f"/api/v1/deliveries/{delivery['id']}", headers=auth_headers
        )
        assert response.status_code == 200
        assert response.json()["id"] == delivery["id"]

    async def test_get_unknown_returns_404(self, client, auth_headers) -> None:
        response = await client.get(
            f"/api/v1/deliveries/{uuid4()}", headers=auth_headers
        )
        assert response.status_code == 404
        assert response.json()["error"]["code"] == "DELIVERY_NOT_FOUND"


class TestListDeliveries:
    async def test_list_empty(self, client, auth_headers) -> None:
        response = await client.get("/api/v1/deliveries", headers=auth_headers)
        assert response.status_code == 200
        assert response.json()["data"] == []

    async def test_list_filter_by_status(self, client, auth_headers) -> None:
        customer = await _create_customer(client, auth_headers)
        d1 = await _create_delivery(client, auth_headers, customer["id"])
        # d1 is ON_THE_WAY

        # Create + complete another
        customer2 = await _create_customer(
            client, auth_headers, name="Second", phone="07701112233"
        )
        d2 = await _create_delivery(client, auth_headers, customer2["id"])
        await client.patch(
            f"/api/v1/deliveries/{d2['id']}/status",
            json={"status": "ARRIVED"},
            headers=auth_headers,
        )
        await client.patch(
            f"/api/v1/deliveries/{d2['id']}/status",
            json={"status": "DELIVERED"},
            headers=auth_headers,
        )

        # Filter by status=DELIVERED
        response = await client.get(
            "/api/v1/deliveries?status=DELIVERED", headers=auth_headers
        )
        body = response.json()
        assert len(body["data"]) == 1
        assert body["data"][0]["status"] == "DELIVERED"


class TestStateMachine:
    async def test_valid_transition_on_the_way_to_arrived(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])

        response = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "ARRIVED"},
            headers=auth_headers,
        )
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "ARRIVED"
        assert body["arrived_at"] is not None
        assert body["completed_at"] is None

    async def test_valid_transition_arrived_to_delivered(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])
        await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "ARRIVED"}, headers=auth_headers,
        )
        response = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "DELIVERED"}, headers=auth_headers,
        )
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "DELIVERED"
        assert body["completed_at"] is not None

    async def test_invalid_skip_transition_returns_409(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])
        # Direct ON_THE_WAY → DELIVERED is not allowed
        response = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "DELIVERED"}, headers=auth_headers,
        )
        assert response.status_code == 409
        assert response.json()["error"]["code"] == "INVALID_STATE_TRANSITION"

    async def test_transition_from_terminal_returns_409(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])
        # Cancel it
        await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "CANCELLED"}, headers=auth_headers,
        )
        # Try to revive — should fail
        response = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "DELIVERED"}, headers=auth_headers,
        )
        assert response.status_code == 409
        assert response.json()["error"]["code"] == "INVALID_STATE_TRANSITION"

    async def test_cancel_from_on_the_way(self, client, auth_headers) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])

        response = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "CANCELLED"}, headers=auth_headers,
        )
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "CANCELLED"
        assert body["cancelled_at"] is not None


class TestIdempotency:
    async def test_idempotent_retry_returns_cached_response(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])

        key = str(uuid4())
        # First call — completes the delivery
        r1 = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "ARRIVED"},
            headers={**auth_headers, "Idempotency-Key": key},
        )
        assert r1.status_code == 200
        assert r1.json()["status"] == "ARRIVED"
        first_completed_at = r1.json()["arrived_at"]

        # Second call with same key + same body → cached response
        r2 = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "ARRIVED"},
            headers={**auth_headers, "Idempotency-Key": key},
        )
        assert r2.status_code == 200
        assert r2.json()["status"] == "ARRIVED"
        # Same arrived_at (cached, not re-executed)
        assert r2.json()["arrived_at"] == first_completed_at

    async def test_idempotency_conflict_on_different_body(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])

        key = str(uuid4())
        # First call: ARRIVED
        await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "ARRIVED"},
            headers={**auth_headers, "Idempotency-Key": key},
        )

        # Second call with same key but DIFFERENT body → 409
        r2 = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "CANCELLED"},  # different body
            headers={**auth_headers, "Idempotency-Key": key},
        )
        assert r2.status_code == 409
        assert r2.json()["error"]["code"] == "IDEMPOTENCY_CONFLICT"

    async def test_no_idempotency_key_allows_re_execution(
        self, client, auth_headers
    ) -> None:
        """Without Idempotency-Key, every call attempts the transition."""
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])

        # First transition to ARRIVED
        r1 = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "ARRIVED"}, headers=auth_headers,
        )
        assert r1.status_code == 200

        # Second call to ARRIVED again — fails (already ARRIVED → ARRIVED is invalid)
        r2 = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "ARRIVED"}, headers=auth_headers,
        )
        assert r2.status_code == 409
        assert r2.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


class TestAuthorization:
    async def test_other_driver_cannot_get_delivery(
        self, client, auth_headers, other_auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])

        response = await client.get(
            f"/api/v1/deliveries/{delivery['id']}", headers=other_auth_headers
        )
        assert response.status_code == 404  # not 403 — no info leak

    async def test_other_driver_cannot_transition(
        self, client, auth_headers, other_auth_headers
    ) -> None:
        customer = await _create_customer(client, auth_headers)
        delivery = await _create_delivery(client, auth_headers, customer["id"])

        response = await client.patch(
            f"/api/v1/deliveries/{delivery['id']}/status",
            json={"status": "ARRIVED"}, headers=other_auth_headers,
        )
        assert response.status_code == 404


# === Fixtures reused from test_customers.py ===
@pytest.fixture
async def other_driver(db_session) -> object:
    from app.core.security import hash_password
    from app.models import User

    user = User(
        id=uuid4(),
        username="other_driver_deliveries",
        password_hash=hash_password("otherpassword123"),
        role="driver",
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()
    return user


@pytest.fixture
async def other_auth_headers(other_driver) -> dict[str, str]:
    from app.core.security import create_access_token

    token = create_access_token(user_id=other_driver.id, role=other_driver.role)
    return {"Authorization": f"Bearer {token}"}
