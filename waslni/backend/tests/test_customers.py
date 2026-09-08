"""Tests for /customers endpoints.

Covers:
  - CRUD happy path
  - Pagination
  - Search by name and phone
  - Duplicate phone detection (create + update)
  - Authorization (driver A cannot access driver B's customers)
  - Delete blocked by active delivery
  - Validation errors
"""
import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio


@pytest.fixture
async def other_driver(db_session) -> object:
    """A second driver user, for authorization tests."""
    from uuid import uuid4

    from app.core.security import hash_password
    from app.models import User

    user = User(
        id=uuid4(),
        username="other_driver",
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


async def _create_customer(
    client: AsyncClient,
    headers: dict,
    name: str = "محمد أحمد",
    phone: str = "07801234567",
    lat: float = 31.978942,
    lng: float = 44.940127,
) -> dict:
    """Helper — POST /customers and return the response body."""
    response = await client.post("/api/v1/customers", json={
        "name": name, "phone": phone, "latitude": lat, "longitude": lng,
        "accuracy": 4.2,
    }, headers=headers)
    assert response.status_code == 201, response.text
    return response.json()


class TestCreateCustomer:
    async def test_create_success(self, client, auth_headers) -> None:
        body = await _create_customer(client, auth_headers)
        assert body["name"] == "محمد أحمد"
        assert body["phone"] == "07801234567"
        assert body["latitude"] == 31.978942
        assert body["accuracy"] == 4.2
        assert "id" in body
        assert "created_at" in body
        assert "updated_at" in body
        # password_hash should never appear
        assert "password_hash" not in body

    async def test_create_strips_phone_formatting(self, client, auth_headers) -> None:
        """Phone is normalized: spaces, dashes, parens stripped."""
        response = await client.post("/api/v1/customers", json={
            "name": "Test", "phone": "0780-123-4567",
            "latitude": 31.0, "longitude": 44.0,
        }, headers=auth_headers)
        assert response.status_code == 201
        assert response.json()["phone"] == "07801234567"

    async def test_create_duplicate_phone(self, client, auth_headers) -> None:
        await _create_customer(client, auth_headers, phone="07801234567")
        response = await client.post("/api/v1/customers", json={
            "name": "Another", "phone": "07801234567",
            "latitude": 31.0, "longitude": 44.0,
        }, headers=auth_headers)
        assert response.status_code == 409
        assert response.json()["error"]["code"] == "DUPLICATE_PHONE"

    async def test_create_validation_short_name(self, client, auth_headers) -> None:
        response = await client.post("/api/v1/customers", json={
            "name": "a", "phone": "07801234567",
            "latitude": 31.0, "longitude": 44.0,
        }, headers=auth_headers)
        assert response.status_code == 422

    async def test_create_validation_bad_latitude(self, client, auth_headers) -> None:
        response = await client.post("/api/v1/customers", json={
            "name": "Test", "phone": "07801234567",
            "latitude": 95.0, "longitude": 44.0,
        }, headers=auth_headers)
        assert response.status_code == 422

    async def test_create_requires_auth(self, client) -> None:
        response = await client.post("/api/v1/customers", json={
            "name": "Test", "phone": "07801234567",
            "latitude": 31.0, "longitude": 44.0,
        })
        assert response.status_code == 401


class TestGetCustomer:
    async def test_get_success(self, client, auth_headers) -> None:
        created = await _create_customer(client, auth_headers)
        response = await client.get(f"/api/v1/customers/{created['id']}", headers=auth_headers)
        assert response.status_code == 200
        assert response.json()["id"] == created["id"]

    async def test_get_unknown_returns_404(self, client, auth_headers) -> None:
        from uuid import uuid4

        response = await client.get(f"/api/v1/customers/{uuid4()}", headers=auth_headers)
        assert response.status_code == 404
        assert response.json()["error"]["code"] == "CUSTOMER_NOT_FOUND"

    async def test_get_other_drivers_customer_returns_404(
        self, client, auth_headers, other_auth_headers
    ) -> None:
        """Driver A creates a customer; driver B cannot fetch it."""
        created = await _create_customer(client, auth_headers)
        response = await client.get(
            f"/api/v1/customers/{created['id']}", headers=other_auth_headers
        )
        assert response.status_code == 404  # NOT 403 — no information leak


class TestListCustomers:
    async def test_list_empty(self, client, auth_headers) -> None:
        response = await client.get("/api/v1/customers", headers=auth_headers)
        assert response.status_code == 200
        body = response.json()
        assert body["data"] == []
        assert body["pagination"]["total"] == 0

    async def test_list_with_customers(self, client, auth_headers) -> None:
        await _create_customer(client, auth_headers, name="Zeinab")
        await _create_customer(client, auth_headers, name="Ahmad", phone="07701112233")
        response = await client.get("/api/v1/customers", headers=auth_headers)
        assert response.status_code == 200
        body = response.json()
        assert len(body["data"]) == 2
        # Sorted by name ASC
        assert body["data"][0]["name"] == "Ahmad"
        assert body["data"][1]["name"] == "Zeinab"
        assert body["pagination"]["total"] == 2
        assert body["pagination"]["total_pages"] == 1

    async def test_list_pagination(self, client, auth_headers) -> None:
        # Create 5 customers
        for i in range(5):
            await _create_customer(
                client, auth_headers, name=f"C{i}", phone=f"0780{i}0000000"
            )
        response = await client.get(
            "/api/v1/customers?page=1&limit=2", headers=auth_headers
        )
        body = response.json()
        assert len(body["data"]) == 2
        assert body["pagination"]["total"] == 5
        assert body["pagination"]["total_pages"] == 3
        assert body["pagination"]["has_next"] is True
        assert body["pagination"]["has_prev"] is False

    async def test_list_search_by_name(self, client, auth_headers) -> None:
        await _create_customer(client, auth_headers, name="محمد أحمد")
        await _create_customer(client, auth_headers, name="علي حسن", phone="07701112233")
        response = await client.get(
            "/api/v1/customers?search=محمد", headers=auth_headers
        )
        body = response.json()
        assert len(body["data"]) == 1
        assert body["data"][0]["name"] == "محمد أحمد"

    async def test_list_search_by_phone(self, client, auth_headers) -> None:
        await _create_customer(client, auth_headers, phone="07801234567")
        await _create_customer(client, auth_headers, name="Other", phone="07701112233")
        response = await client.get(
            "/api/v1/customers?search=0780", headers=auth_headers
        )
        body = response.json()
        assert len(body["data"]) == 1
        assert body["data"][0]["phone"] == "07801234567"


class TestUpdateCustomer:
    async def test_update_name(self, client, auth_headers) -> None:
        created = await _create_customer(client, auth_headers)
        response = await client.patch(
            f"/api/v1/customers/{created['id']}",
            json={"name": "محمد علي أحمد"},
            headers=auth_headers,
        )
        assert response.status_code == 200
        assert response.json()["name"] == "محمد علي أحمد"

    async def test_update_phone_to_duplicate_fails(
        self, client, auth_headers
    ) -> None:
        c1 = await _create_customer(client, auth_headers, phone="07801110000")
        await _create_customer(client, auth_headers, phone="07802220000")
        response = await client.patch(
            f"/api/v1/customers/{c1['id']}",
            json={"phone": "07802220000"},
            headers=auth_headers,
        )
        assert response.status_code == 409
        assert response.json()["error"]["code"] == "DUPLICATE_PHONE"

    async def test_update_partial_only_changes_provided_fields(
        self, client, auth_headers
    ) -> None:
        created = await _create_customer(client, auth_headers)
        original_phone = created["phone"]

        response = await client.patch(
            f"/api/v1/customers/{created['id']}",
            json={"name": "New Name Only"},
            headers=auth_headers,
        )
        body = response.json()
        assert body["name"] == "New Name Only"
        # Phone unchanged
        assert body["phone"] == original_phone

    async def test_update_unknown_returns_404(self, client, auth_headers) -> None:
        from uuid import uuid4

        response = await client.patch(
            f"/api/v1/customers/{uuid4()}",
            json={"name": "X"},
            headers=auth_headers,
        )
        assert response.status_code == 404


class TestDeleteCustomer:
    async def test_delete_success(self, client, auth_headers) -> None:
        created = await _create_customer(client, auth_headers)
        response = await client.delete(
            f"/api/v1/customers/{created['id']}", headers=auth_headers
        )
        assert response.status_code == 204

        # Verify it's gone
        get_resp = await client.get(
            f"/api/v1/customers/{created['id']}", headers=auth_headers
        )
        assert get_resp.status_code == 404

    async def test_delete_unknown_returns_404(self, client, auth_headers) -> None:
        from uuid import uuid4

        response = await client.delete(
            f"/api/v1/customers/{uuid4()}", headers=auth_headers
        )
        assert response.status_code == 404

    async def test_delete_blocked_by_active_delivery(
        self, client, auth_headers
    ) -> None:
        # Create customer + delivery
        customer = await _create_customer(client, auth_headers)
        delivery_resp = await client.post(
            "/api/v1/deliveries",
            json={"customer_id": customer["id"], "status": "ON_THE_WAY"},
            headers=auth_headers,
        )
        assert delivery_resp.status_code == 201

        # Try to delete customer — should fail
        response = await client.delete(
            f"/api/v1/customers/{customer['id']}", headers=auth_headers
        )
        assert response.status_code == 409
        assert response.json()["error"]["code"] == "CUSTOMER_HAS_ACTIVE_DELIVERY"

    async def test_delete_other_drivers_customer_returns_404(
        self, client, auth_headers, other_auth_headers
    ) -> None:
        created = await _create_customer(client, auth_headers)
        response = await client.delete(
            f"/api/v1/customers/{created['id']}", headers=other_auth_headers
        )
        assert response.status_code == 404
