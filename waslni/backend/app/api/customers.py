"""Customer endpoints: GET, POST, GET/{id}, PATCH/{id}, DELETE/{id}.

Wire format matches docs/04-api-contract.md §5.

Authorization:
  - All endpoints require a valid access token (ActiveUser).
  - The user's `id` is used as the `driver_id` for all queries — a driver
    can never read or modify another driver's customers.
"""
from uuid import UUID

from fastapi import APIRouter, Query, Request, status

from app.api.deps import ActiveUser, CustomerServiceDep, get_client_ip
from app.schemas.common import PaginatedResponse, PaginationMeta
from app.schemas.customer import (
    CustomerCreate,
    CustomerResponse,
    CustomerUpdate,
)

router = APIRouter(prefix="/customers", tags=["customers"])


@router.get("", response_model=PaginatedResponse[CustomerResponse])
async def list_customers(
    service: CustomerServiceDep,
    user: ActiveUser,
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=50, ge=1, le=200),
    search: str | None = Query(default=None, max_length=100),
) -> PaginatedResponse[CustomerResponse]:
    """Paginated list of the current driver's customers.

    `search` matches name OR phone (case-insensitive prefix).
    """
    rows, total = await service.list(
        driver_id=user.id, page=page, limit=limit, search=search
    )
    total_pages = (total + limit - 1) // limit if total > 0 else 0
    return PaginatedResponse[CustomerResponse](
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
    response_model=CustomerResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_customer(
    body: CustomerCreate,
    request: Request,
    service: CustomerServiceDep,
    user: ActiveUser,
) -> CustomerResponse:
    """Add a new customer.

    Raises:
        409 DUPLICATE_PHONE: phone already registered for this driver.
        422: validation error (bad name/phone/lat/lng).
    """
    return await service.create(
        driver_id=user.id,
        data=body,
        ip_address=get_client_ip(request),
    )


@router.get("/{customer_id}", response_model=CustomerResponse)
async def get_customer(
    customer_id: UUID,
    service: CustomerServiceDep,
    user: ActiveUser,
) -> CustomerResponse:
    """Fetch a single customer by ID.

    Raises:
        404 CUSTOMER_NOT_FOUND: doesn't exist OR belongs to another driver
            (same error to avoid information leak).
    """
    return await service.get(customer_id=customer_id, driver_id=user.id)


@router.patch("/{customer_id}", response_model=CustomerResponse)
async def update_customer(
    customer_id: UUID,
    body: CustomerUpdate,
    request: Request,
    service: CustomerServiceDep,
    user: ActiveUser,
) -> CustomerResponse:
    """Update a customer's fields. Only non-null fields are applied.

    Raises:
        404 CUSTOMER_NOT_FOUND
        409 DUPLICATE_PHONE: new phone conflicts with another customer.
    """
    return await service.update(
        customer_id=customer_id,
        driver_id=user.id,
        data=body,
        ip_address=get_client_ip(request),
    )


@router.delete("/{customer_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_customer(
    customer_id: UUID,
    request: Request,
    service: CustomerServiceDep,
    user: ActiveUser,
) -> None:
    """Delete a customer.

    Raises:
        404 CUSTOMER_NOT_FOUND
        409 CUSTOMER_HAS_ACTIVE_DELIVERY: customer has ON_THE_WAY or ARRIVED delivery.
    """
    await service.delete(
        customer_id=customer_id,
        driver_id=user.id,
        ip_address=get_client_ip(request),
    )
    return None
