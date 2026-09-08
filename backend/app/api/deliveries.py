"""Delivery endpoints: GET, POST, GET/{id}, PATCH/{id}/status.

Wire format matches docs/04-api-contract.md §6.

Authorization:
  - All endpoints require a valid access token (ActiveUser).
  - The user's `id` is used as the `driver_id` for all queries.

Idempotency:
  - PATCH /{id}/status accepts an Idempotency-Key header.
  - The service caches the response keyed by (key, user_id).
  - Retry with same key + same body → cached response.
  - Retry with same key + different body → 409 IDEMPOTENCY_CONFLICT.
"""
from datetime import datetime
from uuid import UUID

from fastapi import APIRouter, Header, Query, Request, status

from app.api.deps import ActiveUser, DeliveryServiceDep, get_client_ip
from app.schemas.common import PaginatedResponse, PaginationMeta
from app.schemas.delivery import (
    DeliveryCreate,
    DeliveryResponse,
    DeliveryStatusEnum,
    DeliveryStatusUpdate,
)

router = APIRouter(prefix="/deliveries", tags=["deliveries"])


@router.get("", response_model=PaginatedResponse[DeliveryResponse])
async def list_deliveries(
    service: DeliveryServiceDep,
    user: ActiveUser,
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=50, ge=1, le=200),
    status_filter: DeliveryStatusEnum | None = Query(default=None, alias="status"),
    from_date: datetime | None = Query(default=None),
    to_date: datetime | None = Query(default=None),
) -> PaginatedResponse[DeliveryResponse]:
    """Paginated list of the current driver's deliveries with optional filters."""
    rows, total = await service.list(
        driver_id=user.id,
        page=page,
        limit=limit,
        status=status_filter,
        from_date=from_date,
        to_date=to_date,
    )
    total_pages = (total + limit - 1) // limit if total > 0 else 0
    return PaginatedResponse[DeliveryResponse](
        data=rows,
        pagination=PaginationMeta(
            page=page,
            limit=limit,
            total=total,
            total_pages=total_pages,
            has_next=page < total_pages,
            has_prev=page > 1,
        ),
    )


@router.post(
    "",
    response_model=DeliveryResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_delivery(
    body: DeliveryCreate,
    request: Request,
    service: DeliveryServiceDep,
    user: ActiveUser,
) -> DeliveryResponse:
    """Create a new delivery for a customer.

    Raises:
        404 CUSTOMER_NOT_FOUND
        409 CUSTOMER_HAS_ACTIVE_DELIVERY: customer already has an active delivery.
    """
    return await service.create(
        driver_id=user.id,
        data=body,
        ip_address=get_client_ip(request),
    )


@router.get("/{delivery_id}", response_model=DeliveryResponse)
async def get_delivery(
    delivery_id: UUID,
    service: DeliveryServiceDep,
    user: ActiveUser,
) -> DeliveryResponse:
    """Fetch a single delivery by ID.

    Raises:
        404 DELIVERY_NOT_FOUND
    """
    return await service.get(delivery_id=delivery_id, driver_id=user.id)


@router.patch("/{delivery_id}/status", response_model=DeliveryResponse)
async def update_delivery_status(
    delivery_id: UUID,
    body: DeliveryStatusUpdate,
    request: Request,
    service: DeliveryServiceDep,
    user: ActiveUser,
    idempotency_key: str | None = Header(default=None, alias="Idempotency-Key"),
) -> DeliveryResponse:
    """Transition a delivery to a new status.

    State machine:
        PENDING → ASSIGNED → ON_THE_WAY → ARRIVED → DELIVERED
                                            │
                                            └──→ CANCELLED (from any non-terminal state)

    Idempotency:
        Send an `Idempotency-Key` header (any UUID) to make this call safe to retry.
        The same key + same body returns the cached response.
        The same key + different body returns 409 IDEMPOTENCY_CONFLICT.

    Raises:
        404 DELIVERY_NOT_FOUND
        409 INVALID_STATE_TRANSITION: e.g. PENDING → DELIVERED is not allowed.
        409 IDEMPOTENCY_CONFLICT: reused key with a different body.
    """
    return await service.transition(
        delivery_id=delivery_id,
        driver_id=user.id,
        target=body.status,
        idempotency_key=idempotency_key,
        request_payload=body.model_dump(mode="json"),
        ip_address=get_client_ip(request),
    )
