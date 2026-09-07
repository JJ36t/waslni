"""Common Pydantic schemas shared across endpoints."""
from typing import Any, Generic, TypeVar

from pydantic import BaseModel


T = TypeVar("T")


class PaginatedResponse(BaseModel, Generic[T]):
    """Generic paginated list response.

    Used by GET /customers, GET /deliveries, etc.
    """

    data: list[T]
    pagination: "PaginationMeta"


class PaginationMeta(BaseModel):
    page: int
    limit: int
    total: int
    total_pages: int
    has_next: bool
    has_prev: bool


class ErrorResponse(BaseModel):
    """Matches the unified error model from docs/04-api-contract.md §2."""

    error: "ErrorBody"


class ErrorBody(BaseModel):
    code: str
    message: str
    details: dict[str, Any] | None = None
