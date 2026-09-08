"""Tests for security primitives — password hashing and JWT."""
import time
from uuid import uuid4

import pytest

from app.core.security import (
    TokenInvalidError,
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)


class TestPasswordHashing:
    def test_hash_password_returns_argon2_string(self) -> None:
        hashed = hash_password("mypassword")
        # Argon2 hashes start with $argon2id$
        assert hashed.startswith("$argon2")

    def test_hash_password_is_different_each_time(self) -> None:
        # Salt is random — same password → different hashes
        h1 = hash_password("mypassword")
        h2 = hash_password("mypassword")
        assert h1 != h2

    def test_verify_password_accepts_correct_password(self) -> None:
        hashed = hash_password("mypassword")
        assert verify_password("mypassword", hashed) is True

    def test_verify_password_rejects_wrong_password(self) -> None:
        hashed = hash_password("mypassword")
        assert verify_password("wrongpassword", hashed) is False

    def test_verify_password_returns_false_on_garbage_input(self) -> None:
        # Never raise — return False to avoid timing info via exceptions
        assert verify_password("anything", "not-a-real-hash") is False


class TestAccessTokens:
    def test_create_and_decode_access_token(self) -> None:
        user_id = uuid4()
        token = create_access_token(user_id=user_id, role="driver")

        payload = decode_token(token)
        assert payload["sub"] == str(user_id)
        assert payload["role"] == "driver"
        assert payload["type"] == "access"
        assert "exp" in payload
        assert "iat" in payload

    def test_decode_token_rejects_garbage(self) -> None:
        with pytest.raises(TokenInvalidError):
            decode_token("not.a.real.token")

    def test_decode_token_rejects_tampered_token(self) -> None:
        user_id = uuid4()
        token = create_access_token(user_id=user_id, role="driver")
        # Flip a character in the signature
        tampered = token[:-2] + ("AA" if token[-2:] != "AA" else "BB")
        with pytest.raises(TokenInvalidError):
            decode_token(tampered)


class TestRefreshTokens:
    def test_create_and_decode_refresh_token(self) -> None:
        user_id = uuid4()
        jti = uuid4()
        token = create_refresh_token(user_id=user_id, jti=jti)

        payload = decode_token(token)
        assert payload["sub"] == str(user_id)
        assert payload["jti"] == str(jti)
        assert payload["type"] == "refresh"

    def test_refresh_token_has_longer_expiry_than_access(self) -> None:
        user_id = uuid4()
        jti = uuid4()
        access = create_access_token(user_id=user_id, role="driver")
        refresh = create_refresh_token(user_id=user_id, jti=jti)

        access_payload = decode_token(access)
        refresh_payload = decode_token(refresh)

        # Refresh should expire later than access (default 30 days vs 15 min)
        assert refresh_payload["exp"] > access_payload["exp"]


class TestTokenExpiry:
    def test_expired_token_raises_token_expired(self) -> None:
        from datetime import timedelta

        user_id = uuid4()
        # Create a token that expired 1 second ago
        token = create_access_token(
            user_id=user_id,
            role="driver",
            expires_delta=timedelta(seconds=-1),
        )

        from app.core.security import TokenExpiredError as SecurityExpired

        with pytest.raises(SecurityExpired):
            decode_token(token)
