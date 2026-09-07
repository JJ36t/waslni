"""Unified error model for the API.

Every error response follows this shape (see docs/04-api-contract.md §2):

    {
      "error": {
        "code": "CUSTOMER_NOT_FOUND",
        "message": "Customer not found",
        "details": { ... }
      }
    }

Custom exceptions:
  - Each carries a `code` (machine-readable) and `message` (user-readable).
  - FastAPI exception handlers convert them to HTTP responses.

Usage in services / routes:
    raise NotFoundError("CUSTOMER_NOT_FOUND", "Customer not found",
                        details={"customer_id": customer_id})
"""
from typing import Any


class AppError(Exception):
    """Base class for all API errors.

    Subclasses set `status_code` to control the HTTP response code.
    """

    status_code: int = 500
    default_message: str = "Internal error"

    def __init__(
        self,
        code: str,
        message: str | None = None,
        details: dict[str, Any] | None = None,
    ) -> None:
        self.code = code
        self.message = message or self.default_message
        self.details = details
        super().__init__(self.message)


# === 400 Bad Request ===
class BadRequestError(AppError):
    status_code = 400
    default_message = "Bad request"


# === 401 Unauthorized ===
class UnauthorizedError(AppError):
    status_code = 401
    default_message = "Unauthorized"


class TokenExpiredError(AppError):
    status_code = 401
    default_message = "Token has expired"


# === 403 Forbidden ===
class ForbiddenError(AppError):
    status_code = 403
    default_message = "Forbidden"


# === 404 Not Found ===
class NotFoundError(AppError):
    status_code = 404
    default_message = "Resource not found"


# === 409 Conflict ===
class ConflictError(AppError):
    status_code = 409
    default_message = "Conflict"


# === 422 Validation Error ===
class ValidationError(AppError):
    status_code = 422
    default_message = "Validation failed"


# === 429 Too Many Requests ===
class RateLimitError(AppError):
    status_code = 429
    default_message = "Too many requests"


# === 500 Internal ===
class InternalError(AppError):
    status_code = 500
    default_message = "Internal server error"


# === 503 Service Unavailable ===
class ServiceUnavailableError(AppError):
    status_code = 503
    default_message = "Service unavailable"


# === Common error codes (centralized for consistency) ===
class ErrorCodes:
    """String constants for error codes used across the codebase."""

    # Auth
    INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    TOKEN_EXPIRED = "TOKEN_EXPIRED"
    TOKEN_INVALID = "TOKEN_INVALID"
    REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID"
    UNAUTHORIZED = "UNAUTHORIZED"
    FORBIDDEN = "FORBIDDEN"
    ACCOUNT_DISABLED = "ACCOUNT_DISABLED"

    # Resources
    RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND"
    CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND"
    DELIVERY_NOT_FOUND = "DELIVERY_NOT_FOUND"

    # Validation
    VALIDATION_ERROR = "VALIDATION_ERROR"

    # Conflicts
    DUPLICATE_PHONE = "DUPLICATE_PHONE"
    INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION"
    CUSTOMER_HAS_ACTIVE_DELIVERIES = "CUSTOMER_HAS_ACTIVE_DELIVERIES"
    CUSTOMER_HAS_ACTIVE_DELIVERY = "CUSTOMER_HAS_ACTIVE_DELIVERY"
    IDEMPOTENCY_CONFLICT = "IDEMPOTENCY_CONFLICT"
    STALE_UPDATE = "STALE_UPDATE"

    # Limits
    RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED"

    # Server
    INTERNAL_ERROR = "INTERNAL_ERROR"
    SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE"


def format_error_response(
    code: str,
    message: str,
    details: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Build the unified error response body.

    Used by exception handlers AND by tests that assert on response shape.
    """
    body: dict[str, Any] = {"code": code, "message": message}
    if details is not None:
        body["details"] = details
    return {"error": body}
