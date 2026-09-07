"""Tests for the unified error model + format_error_response helper."""
import pytest

from app.core.exceptions import (
    AppError,
    BadRequestError,
    ConflictError,
    ErrorCodes,
    ForbiddenError,
    InternalError,
    NotFoundError,
    RateLimitError,
    ServiceUnavailableError,
    UnauthorizedError,
    ValidationError,
    format_error_response,
)


class TestErrorHierarchy:
    def test_all_errors_inherit_from_app_error(self) -> None:
        for cls in (
            BadRequestError,
            UnauthorizedError,
            ForbiddenError,
            NotFoundError,
            ConflictError,
            ValidationError,
            RateLimitError,
            InternalError,
            ServiceUnavailableError,
        ):
            assert issubclass(cls, AppError)

    def test_status_codes_are_correct(self) -> None:
        assert BadRequestError("X").status_code == 400
        assert UnauthorizedError("X").status_code == 401
        assert ForbiddenError("X").status_code == 403
        assert NotFoundError("X").status_code == 404
        assert ConflictError("X").status_code == 409
        assert ValidationError("X").status_code == 422
        assert RateLimitError("X").status_code == 429
        assert InternalError("X").status_code == 500
        assert ServiceUnavailableError("X").status_code == 503

    def test_error_carries_code_message_details(self) -> None:
        err = NotFoundError(
            code=ErrorCodes.CUSTOMER_NOT_FOUND,
            message="Customer not found",
            details={"customer_id": "abc-123"},
        )
        assert err.code == "CUSTOMER_NOT_FOUND"
        assert err.message == "Customer not found"
        assert err.details == {"customer_id": "abc-123"}


class TestFormatErrorResponse:
    def test_minimal_response(self) -> None:
        body = format_error_response("FOO", "bar")
        assert body == {"error": {"code": "FOO", "message": "bar"}}

    def test_response_with_details(self) -> None:
        body = format_error_response(
            "FOO",
            "bar",
            details={"field": "name", "issue": "too short"},
        )
        assert body == {
            "error": {
                "code": "FOO",
                "message": "bar",
                "details": {"field": "name", "issue": "too short"},
            }
        }

    def test_response_with_none_details_omits_field(self) -> None:
        body = format_error_response("FOO", "bar", details=None)
        assert "details" not in body["error"]


class TestErrorCodes:
    def test_all_codes_are_uppercase_strings(self) -> None:
        codes = [
            ErrorCodes.INVALID_CREDENTIALS,
            ErrorCodes.TOKEN_EXPIRED,
            ErrorCodes.TOKEN_INVALID,
            ErrorCodes.REFRESH_TOKEN_INVALID,
            ErrorCodes.UNAUTHORIZED,
            ErrorCodes.FORBIDDEN,
            ErrorCodes.ACCOUNT_DISABLED,
            ErrorCodes.RESOURCE_NOT_FOUND,
            ErrorCodes.CUSTOMER_NOT_FOUND,
            ErrorCodes.DELIVERY_NOT_FOUND,
            ErrorCodes.VALIDATION_ERROR,
            ErrorCodes.DUPLICATE_PHONE,
            ErrorCodes.INVALID_STATE_TRANSITION,
            ErrorCodes.CUSTOMER_HAS_ACTIVE_DELIVERIES,
            ErrorCodes.CUSTOMER_HAS_ACTIVE_DELIVERY,
            ErrorCodes.IDEMPOTENCY_CONFLICT,
            ErrorCodes.STALE_UPDATE,
            ErrorCodes.RATE_LIMIT_EXCEEDED,
            ErrorCodes.INTERNAL_ERROR,
            ErrorCodes.SERVICE_UNAVAILABLE,
        ]
        for code in codes:
            assert isinstance(code, str)
            assert code == code.upper()
            assert code.replace("_", "").isalnum()
