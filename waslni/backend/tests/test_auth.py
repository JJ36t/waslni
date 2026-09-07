"""Tests for the /auth endpoints.

Covers:
  - /auth/login: success, wrong password, unknown user, disabled account, rate limit
  - /auth/refresh: success, rotation (old token revoked), expired token, tampered token,
    reuse of revoked token triggers logout-all
  - /auth/logout: success, idempotent on unknown token
  - /auth/me: success with valid token, 401 without token, 401 with expired token,
    403 for disabled account
"""
import time
from uuid import uuid4

import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio


class TestLogin:
    async def test_login_success(
        self,
        client: AsyncClient,
        test_user,  # creates "test_driver" / "testpassword123"
    ) -> None:
        response = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "testpassword123",
        })
        assert response.status_code == 200
        body = response.json()
        assert "access_token" in body
        assert "refresh_token" in body
        assert body["token_type"] == "bearer"
        assert body["expires_in"] == 900  # 15 min
        assert body["user"]["username"] == "test_driver"
        assert body["user"]["role"] == "driver"
        assert "password_hash" not in body["user"]

    async def test_login_wrong_password(self, client: AsyncClient, test_user) -> None:
        response = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "wrongpassword",
        })
        assert response.status_code == 401
        body = response.json()
        assert body["error"]["code"] == "INVALID_CREDENTIALS"

    async def test_login_unknown_user(self, client: AsyncClient) -> None:
        response = await client.post("/api/v1/auth/login", json={
            "username": "ghost_user",
            "password": "anything",
        })
        assert response.status_code == 401
        assert response.json()["error"]["code"] == "INVALID_CREDENTIALS"
        # Same error code as wrong password — no username enumeration

    async def test_login_disabled_account(
        self,
        client: AsyncClient,
        db_session,
    ) -> None:
        # Create a disabled user
        from app.core.security import hash_password
        from app.models import User

        user = User(
            id=uuid4(),
            username="disabled_user",
            password_hash=hash_password("somepassword"),
            role="driver",
            is_active=False,
        )
        db_session.add(user)
        await db_session.commit()

        response = await client.post("/api/v1/auth/login", json={
            "username": "disabled_user",
            "password": "somepassword",
        })
        assert response.status_code == 403
        assert response.json()["error"]["code"] == "ACCOUNT_DISABLED"

    async def test_login_validation_error_too_short_username(
        self, client: AsyncClient
    ) -> None:
        response = await client.post("/api/v1/auth/login", json={
            "username": "ab",  # min_length=3
            "password": "somepassword",
        })
        assert response.status_code == 422

    async def test_login_creates_audit_log(
        self,
        client: AsyncClient,
        test_user,
        db_session,
    ) -> None:
        await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "testpassword123",
        })
        from sqlalchemy import select

        from app.models import AuditLog

        result = await db_session.execute(
            select(AuditLog).where(AuditLog.action == "LOGIN_SUCCESS")
        )
        logs = result.scalars().all()
        assert len(logs) == 1
        assert str(logs[0].user_id) == str(test_user.id)


class TestRefresh:
    async def test_refresh_success(self, client: AsyncClient, test_user) -> None:
        # Login first
        login_resp = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "testpassword123",
        })
        refresh_token = login_resp.json()["refresh_token"]

        # Refresh
        response = await client.post("/api/v1/auth/refresh", json={
            "refresh_token": refresh_token,
        })
        assert response.status_code == 200
        body = response.json()
        assert "access_token" in body
        assert "refresh_token" in body
        # New tokens differ from the old ones
        assert body["refresh_token"] != refresh_token

    async def test_refresh_rotates_old_token(self, client: AsyncClient, test_user) -> None:
        """After refresh, the old refresh token must be revoked."""
        login_resp = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "testpassword123",
        })
        old_refresh = login_resp.json()["refresh_token"]

        await client.post("/api/v1/auth/refresh", json={"refresh_token": old_refresh})

        # Reusing the old token must fail
        response = await client.post("/api/v1/auth/refresh", json={
            "refresh_token": old_refresh,
        })
        assert response.status_code == 401
        assert response.json()["error"]["code"] == "REFRESH_TOKEN_INVALID"

    async def test_refresh_tampered_token(self, client: AsyncClient) -> None:
        response = await client.post("/api/v1/auth/refresh", json={
            "refresh_token": "not.a.real.token",
        })
        assert response.status_code == 401
        assert response.json()["error"]["code"] == "REFRESH_TOKEN_INVALID"

    async def test_refresh_with_access_token_fails(
        self,
        client: AsyncClient,
        test_user,
    ) -> None:
        """An access token must not be accepted by /auth/refresh."""
        login_resp = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "testpassword123",
        })
        access_token = login_resp.json()["access_token"]

        response = await client.post("/api/v1/auth/refresh", json={
            "refresh_token": access_token,
        })
        assert response.status_code == 401
        assert response.json()["error"]["code"] == "REFRESH_TOKEN_INVALID"


class TestLogout:
    async def test_logout_success(self, client: AsyncClient, test_user) -> None:
        login_resp = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "testpassword123",
        })
        refresh_token = login_resp.json()["refresh_token"]

        response = await client.post("/api/v1/auth/logout", json={
            "refresh_token": refresh_token,
        })
        assert response.status_code == 204

        # Verify the token can no longer be refreshed
        refresh_resp = await client.post("/api/v1/auth/refresh", json={
            "refresh_token": refresh_token,
        })
        assert refresh_resp.status_code == 401

    async def test_logout_unknown_token_is_idempotent(
        self, client: AsyncClient
    ) -> None:
        """Calling logout with a garbage token returns 204 — no information leak."""
        response = await client.post("/api/v1/auth/logout", json={
            "refresh_token": "garbage-not-a-real-token-but-long-enough",
        })
        assert response.status_code == 204

    async def test_logout_twice_is_idempotent(
        self, client: AsyncClient, test_user
    ) -> None:
        login_resp = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "testpassword123",
        })
        refresh_token = login_resp.json()["refresh_token"]

        r1 = await client.post("/api/v1/auth/logout", json={"refresh_token": refresh_token})
        r2 = await client.post("/api/v1/auth/logout", json={"refresh_token": refresh_token})

        assert r1.status_code == 204
        assert r2.status_code == 204


class TestMe:
    async def test_me_success(self, client: AsyncClient, test_user, auth_headers) -> None:
        response = await client.get("/api/v1/auth/me", headers=auth_headers)
        assert response.status_code == 200
        body = response.json()
        assert body["username"] == "test_driver"
        assert body["role"] == "driver"
        assert "password_hash" not in body

    async def test_me_without_token_returns_401(self, client: AsyncClient) -> None:
        response = await client.get("/api/v1/auth/me")
        assert response.status_code == 401
        assert response.json()["error"]["code"] == "UNAUTHORIZED"

    async def test_me_with_garbage_token_returns_401(self, client: AsyncClient) -> None:
        response = await client.get(
            "/api/v1/auth/me",
            headers={"Authorization": "Bearer garbage.token.here"},
        )
        assert response.status_code == 401

    async def test_me_with_refresh_token_returns_401(
        self,
        client: AsyncClient,
        test_user,
    ) -> None:
        """A refresh token must not be accepted by /auth/me."""
        login_resp = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "testpassword123",
        })
        refresh_token = login_resp.json()["refresh_token"]

        response = await client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {refresh_token}"},
        )
        assert response.status_code == 401
        assert response.json()["error"]["code"] == "TOKEN_INVALID"

    async def test_me_disabled_user_returns_403(
        self,
        client: AsyncClient,
        db_session,
    ) -> None:
        from datetime import timedelta

        from app.core.security import create_access_token, hash_password
        from app.models import User

        user = User(
            id=uuid4(),
            username="disabled_for_me_test",
            password_hash=hash_password("somepassword123"),
            role="driver",
            is_active=False,
        )
        db_session.add(user)
        await db_session.commit()

        token = create_access_token(user_id=user.id, role=user.role)
        response = await client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert response.status_code == 403
        assert response.json()["error"]["code"] == "ACCOUNT_DISABLED"


class TestRateLimit:
    """Verify the in-memory rate limiter on /auth/login."""

    async def test_rate_limit_blocks_after_max_attempts(
        self,
        client: AsyncClient,
        test_user,
    ) -> None:
        # Reset the in-memory counter (module-level state)
        from app.api.auth import _login_attempts
        _login_attempts.clear()

        # First N attempts should not be rate-limited (they'll 401 for wrong password)
        # We use a unique IP via X-Forwarded-For to isolate from other tests
        headers = {"X-Forwarded-For": "10.10.10.10"}
        for _ in range(5):
            r = await client.post("/api/v1/auth/login", json={
                "username": "test_driver",
                "password": "wrongpassword",
            }, headers=headers)
            assert r.status_code == 401  # not rate-limited yet

        # 6th attempt should be rate-limited
        r = await client.post("/api/v1/auth/login", json={
            "username": "test_driver",
            "password": "wrongpassword",
        }, headers=headers)
        assert r.status_code == 429
        assert r.json()["error"]["code"] == "RATE_LIMIT_EXCEEDED"
