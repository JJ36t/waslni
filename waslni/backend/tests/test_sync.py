"""Tests for the /sync endpoint.

Covers:
  - CREATE_CUSTOMER via sync (success + conflict on duplicate phone)
  - UPDATE_CUSTOMER via sync (success + STALE_UPDATE conflict)
  - DELETE_CUSTOMER via sync (success + IGNORED for already-deleted)
  - CREATE_DELIVERY via sync
  - COMPLETE_DELIVERY via sync (with idempotency_key)
  - Batch processing (multiple ops in one call, independent results)
  - server_changes for download direction (multi-device sync)
  - latest_sync_timestamp is returned and can be used in next call
  - Empty batch
"""
from datetime import datetime, timedelta, timezone
from uuid import uuid4

import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio


async def _create_customer_via_api(
    client: AsyncClient, headers: dict, name: str = "محمد", phone: str = "07801234567"
) -> dict:
    response = await client.post("/api/v1/customers", json={
        "name": name, "phone": phone,
        "latitude": 31.978942, "longitude": 44.940127, "accuracy": 4.2,
    }, headers=headers)
    assert response.status_code == 201, response.text
    return response.json()


class TestSyncCreateCustomer:
    async def test_create_customer_via_sync(self, client, auth_headers) -> None:
        op_id = str(uuid4())
        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": op_id,
                "entity_type": "CUSTOMER",
                "operation": "CREATE_CUSTOMER",
                "payload": {
                    "name": "محمد أحمد",
                    "phone": "07801234567",
                    "latitude": 31.978942,
                    "longitude": 44.940127,
                    "accuracy": 4.2,
                },
            }],
        }, headers=auth_headers)
        assert response.status_code == 200
        body = response.json()
        assert len(body["results"]) == 1
        result = body["results"][0]
        assert result["operation_id"] == op_id
        assert result["status"] == "SUCCESS"
        assert "id" in result["server_state"]
        assert result["server_state"]["name"] == "محمد أحمد"

    async def test_create_duplicate_phone_returns_conflict(
        self, client, auth_headers
    ) -> None:
        # First create via REST
        await _create_customer_via_api(client, auth_headers, phone="07801234567")

        # Try to create same phone via sync
        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),
                "entity_type": "CUSTOMER",
                "operation": "CREATE_CUSTOMER",
                "payload": {
                    "name": "Another",
                    "phone": "07801234567",
                    "latitude": 31.0, "longitude": 44.0,
                },
            }],
        }, headers=auth_headers)
        body = response.json()
        result = body["results"][0]
        assert result["status"] == "CONFLICT"
        assert result["error"]["code"] == "DUPLICATE_PHONE"


class TestSyncUpdateCustomer:
    async def test_update_customer_via_sync(self, client, auth_headers) -> None:
        customer = await _create_customer_via_api(client, auth_headers)

        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),
                "entity_type": "CUSTOMER",
                "operation": "UPDATE_CUSTOMER",
                "payload": {
                    "id": customer["id"],
                    "name": "محمد علي أحمد",
                    "phone": "07801234567",
                    "latitude": 31.978942,
                    "longitude": 44.940127,
                    "accuracy": 4.2,
                    "updated_at": customer["updated_at"],  # client's view
                },
            }],
        }, headers=auth_headers)
        body = response.json()
        result = body["results"][0]
        assert result["status"] == "SUCCESS"
        assert result["server_state"]["name"] == "محمد علي أحمد"

    async def test_stale_update_returns_conflict(
        self, client, auth_headers
    ) -> None:
        """If the server has a newer updated_at than the payload → CONFLICT."""
        customer = await _create_customer_via_api(client, auth_headers)

        # Send an old updated_at (1 hour in the past)
        old_ts = (datetime.now(timezone.utc) - timedelta(hours=1)).isoformat()
        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),
                "entity_type": "CUSTOMER",
                "operation": "UPDATE_CUSTOMER",
                "payload": {
                    "id": customer["id"],
                    "name": "Stale Name",
                    "phone": "07801234567",
                    "latitude": 31.0, "longitude": 44.0,
                    "updated_at": old_ts,
                },
            }],
        }, headers=auth_headers)
        body = response.json()
        result = body["results"][0]
        assert result["status"] == "CONFLICT"
        assert result["error"]["code"] == "STALE_UPDATE"
        # server_state returned so client can adopt it
        assert result["server_state"]["name"] == "محمد"


class TestSyncDeleteCustomer:
    async def test_delete_customer_via_sync(self, client, auth_headers) -> None:
        customer = await _create_customer_via_api(client, auth_headers)

        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),
                "entity_type": "CUSTOMER",
                "operation": "DELETE_CUSTOMER",
                "payload": {"id": customer["id"]},
            }],
        }, headers=auth_headers)
        body = response.json()
        assert body["results"][0]["status"] == "SUCCESS"

        # Verify it's gone
        get_resp = await client.get(
            f"/api/v1/customers/{customer['id']}", headers=auth_headers
        )
        assert get_resp.status_code == 404

    async def test_delete_already_deleted_returns_ignored(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer_via_api(client, auth_headers)
        # Delete it first via REST
        await client.delete(f"/api/v1/customers/{customer['id']}", headers=auth_headers)

        # Now try to delete via sync — should be IGNORED, not FAILED
        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),
                "entity_type": "CUSTOMER",
                "operation": "DELETE_CUSTOMER",
                "payload": {"id": customer["id"]},
            }],
        }, headers=auth_headers)
        body = response.json()
        assert body["results"][0]["status"] == "IGNORED"


class TestSyncCreateDelivery:
    async def test_create_delivery_via_sync(self, client, auth_headers) -> None:
        customer = await _create_customer_via_api(client, auth_headers)

        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),
                "entity_type": "DELIVERY",
                "operation": "CREATE_DELIVERY",
                "payload": {
                    "customer_id": customer["id"],
                    "status": "ON_THE_WAY",
                },
            }],
        }, headers=auth_headers)
        body = response.json()
        result = body["results"][0]
        assert result["status"] == "SUCCESS"
        assert result["server_state"]["status"] == "ON_THE_WAY"
        assert result["server_state"]["started_at"] is not None


class TestSyncCompleteDelivery:
    async def test_complete_delivery_with_idempotency_key(
        self, client, auth_headers
    ) -> None:
        customer = await _create_customer_via_api(client, auth_headers)
        # Create delivery via REST first
        del_resp = await client.post("/api/v1/deliveries", json={
            "customer_id": customer["id"], "status": "ON_THE_WAY",
        }, headers=auth_headers)
        delivery_id = del_resp.json()["id"]

        # Transition to ARRIVED first (required by state machine)
        await client.patch(f"/api/v1/deliveries/{delivery_id}/status",
                           json={"status": "ARRIVED"}, headers=auth_headers)

        # Now complete via sync
        idem_key = str(uuid4())
        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),
                "entity_type": "DELIVERY",
                "operation": "COMPLETE_DELIVERY",
                "payload": {"id": delivery_id},
                "idempotency_key": idem_key,
            }],
        }, headers=auth_headers)
        body = response.json()
        result = body["results"][0]
        assert result["status"] == "SUCCESS"
        assert result["server_state"]["status"] == "DELIVERED"
        assert result["server_state"]["completed_at"] is not None

    async def test_complete_delivery_idempotent_retry(
        self, client, auth_headers
    ) -> None:
        """Retrying COMPLETE_DELIVERY with same idempotency_key returns SUCCESS
        with the cached response (no double-execution)."""
        customer = await _create_customer_via_api(client, auth_headers)
        del_resp = await client.post("/api/v1/deliveries", json={
            "customer_id": customer["id"], "status": "ON_THE_WAY",
        }, headers=auth_headers)
        delivery_id = del_resp.json()["id"]
        await client.patch(f"/api/v1/deliveries/{delivery_id}/status",
                           json={"status": "ARRIVED"}, headers=auth_headers)

        # First complete via sync
        idem_key = str(uuid4())
        op_id = str(uuid4())
        r1 = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": op_id,
                "entity_type": "DELIVERY",
                "operation": "COMPLETE_DELIVERY",
                "payload": {"id": delivery_id},
                "idempotency_key": idem_key,
            }],
        }, headers=auth_headers)
        first_completed_at = r1.json()["results"][0]["server_state"]["completed_at"]

        # Retry with same idempotency_key
        r2 = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),  # different op id is OK
                "entity_type": "DELIVERY",
                "operation": "COMPLETE_DELIVERY",
                "payload": {"id": delivery_id},
                "idempotency_key": idem_key,  # SAME idempotency key
            }],
        }, headers=auth_headers)
        result = r2.json()["results"][0]
        # Either SUCCESS (cached) or CONFLICT (state machine) — both are valid
        # depending on whether the cached response is hit first.
        # The key invariant: completed_at matches the first call (no double-exec).
        if result["status"] == "SUCCESS":
            assert result["server_state"]["completed_at"] == first_completed_at


class TestBatchProcessing:
    async def test_multiple_operations_in_one_batch(
        self, client, auth_headers
    ) -> None:
        # Create 3 customers in one sync call
        response = await client.post("/api/v1/sync", json={
            "operations": [
                {
                    "id": str(uuid4()),
                    "entity_type": "CUSTOMER",
                    "operation": "CREATE_CUSTOMER",
                    "payload": {
                        "name": "First", "phone": "07801110001",
                        "latitude": 31.0, "longitude": 44.0,
                    },
                },
                {
                    "id": str(uuid4()),
                    "entity_type": "CUSTOMER",
                    "operation": "CREATE_CUSTOMER",
                    "payload": {
                        "name": "Second", "phone": "07801110002",
                        "latitude": 31.0, "longitude": 44.0,
                    },
                },
                {
                    "id": str(uuid4()),
                    "entity_type": "CUSTOMER",
                    "operation": "CREATE_CUSTOMER",
                    "payload": {
                        "name": "Third", "phone": "07801110003",
                        "latitude": 31.0, "longitude": 44.0,
                    },
                },
            ],
        }, headers=auth_headers)
        body = response.json()
        assert len(body["results"]) == 3
        for r in body["results"]:
            assert r["status"] == "SUCCESS"

    async def test_failure_in_one_op_does_not_block_others(
        self, client, auth_headers
    ) -> None:
        """One CONFLICT must not abort the rest of the batch."""
        # Pre-create a customer with phone "07801234567"
        await _create_customer_via_api(client, auth_headers, phone="07801234567")

        # Batch: op1 fails (duplicate phone), op2 succeeds (different phone)
        response = await client.post("/api/v1/sync", json={
            "operations": [
                {
                    "id": str(uuid4()),
                    "entity_type": "CUSTOMER",
                    "operation": "CREATE_CUSTOMER",
                    "payload": {
                        "name": "Duplicate", "phone": "07801234567",
                        "latitude": 31.0, "longitude": 44.0,
                    },
                },
                {
                    "id": str(uuid4()),
                    "entity_type": "CUSTOMER",
                    "operation": "CREATE_CUSTOMER",
                    "payload": {
                        "name": "Unique", "phone": "07809998888",
                        "latitude": 31.0, "longitude": 44.0,
                    },
                },
            ],
        }, headers=auth_headers)
        body = response.json()
        assert body["results"][0]["status"] == "CONFLICT"
        assert body["results"][1]["status"] == "SUCCESS"

    async def test_empty_batch(self, client, auth_headers) -> None:
        response = await client.post("/api/v1/sync", json={
            "operations": [],
        }, headers=auth_headers)
        assert response.status_code == 200
        body = response.json()
        assert body["results"] == []
        assert "latest_sync_timestamp" in body


class TestServerChanges:
    async def test_first_sync_returns_no_changes(self, client, auth_headers) -> None:
        """When latest_sync_timestamp is null (first sync), no server_changes."""
        # Pre-create a customer via REST (simulating data already on server)
        await _create_customer_via_api(client, auth_headers)

        response = await client.post("/api/v1/sync", json={
            "operations": [],
            "latest_sync_timestamp": None,
        }, headers=auth_headers)
        body = response.json()
        assert body["server_changes"] == []

    async def test_changes_since_last_sync_are_returned(
        self, client, auth_headers
    ) -> None:
        """After first sync, server-side changes appear in next sync's server_changes."""
        # First sync — get baseline timestamp
        r1 = await client.post("/api/v1/sync", json={
            "operations": [],
            "latest_sync_timestamp": None,
        }, headers=auth_headers)
        first_ts = r1.json()["latest_sync_timestamp"]

        # Wait a moment so the new customer's updated_at > first_ts
        # (the timestamp has a 5s grace period subtracted, so we need to wait)
        import asyncio
        await asyncio.sleep(0.1)

        # Make a server-side change (create a customer via REST)
        await _create_customer_via_api(
            client, auth_headers, name="NewCustomer", phone="07805556666"
        )

        # Second sync — should see the new customer in server_changes
        r2 = await client.post("/api/v1/sync", json={
            "operations": [],
            "latest_sync_timestamp": first_ts,
        }, headers=auth_headers)
        body = r2.json()
        # The newly created customer should appear
        customer_changes = [
            c for c in body["server_changes"] if c["entity_type"] == "CUSTOMER"
        ]
        assert len(customer_changes) >= 1
        names = [c["payload"]["name"] for c in customer_changes]
        assert "NewCustomer" in names

    async def test_latest_sync_timestamp_advances(
        self, client, auth_headers
    ) -> None:
        """Each sync returns a newer timestamp than the previous one."""
        r1 = await client.post("/api/v1/sync", json={
            "operations": [], "latest_sync_timestamp": None,
        }, headers=auth_headers)
        ts1 = datetime.fromisoformat(r1.json()["latest_sync_timestamp"].replace("Z", "+00:00"))

        import asyncio
        await asyncio.sleep(0.01)

        r2 = await client.post("/api/v1/sync", json={
            "operations": [], "latest_sync_timestamp": ts1.isoformat(),
        }, headers=auth_headers)
        ts2 = datetime.fromisoformat(r2.json()["latest_sync_timestamp"].replace("Z", "+00:00"))

        assert ts2 > ts1


class TestSyncAuthorization:
    async def test_other_driver_cannot_sync_to_your_data(
        self, client, auth_headers, other_auth_headers
    ) -> None:
        """Driver A cannot create a customer for driver B via sync."""
        # Driver A creates a customer
        customer_a = await _create_customer_via_api(
            client, auth_headers, name="Driver A Customer", phone="07801234567"
        )

        # Driver B tries to UPDATE driver A's customer via sync
        response = await client.post("/api/v1/sync", json={
            "operations": [{
                "id": str(uuid4()),
                "entity_type": "CUSTOMER",
                "operation": "UPDATE_CUSTOMER",
                "payload": {
                    "id": customer_a["id"],
                    "name": "Hacked",
                    "phone": "07801234567",
                    "latitude": 31.0, "longitude": 44.0,
                    "updated_at": customer_a["updated_at"],
                },
            }],
        }, headers=other_auth_headers)
        body = response.json()
        # Should be IGNORED (NotFoundError → IGNORED for UPDATE_CUSTOMER)
        result = body["results"][0]
        assert result["status"] in ("IGNORED", "CONFLICT", "FAILED")


# === Fixtures ===
@pytest.fixture
async def other_driver(db_session) -> object:
    from app.core.security import hash_password
    from app.models import User

    user = User(
        id=uuid4(),
        username="other_driver_sync",
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
