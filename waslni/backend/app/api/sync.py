"""Sync endpoint: POST /sync.

Accepts a batch of pending operations from the Android client and returns:
  1. Per-operation results (SUCCESS / CONFLICT / FAILED / IGNORED).
  2. Server-side changes since the client's last sync (multi-device support).
  3. A new latest_sync_timestamp the client stores for its next sync.

Wire format: docs/04-api-contract.md §7.
"""
from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from app.api.deps import ActiveUser, SyncServiceDep, get_client_ip
from app.schemas.sync import SyncRequest, SyncResponse

router = APIRouter(prefix="/sync", tags=["sync"])


@router.post("", response_model=SyncResponse)
async def sync(
    body: SyncRequest,
    request: Request,
    service: SyncServiceDep,
    user: ActiveUser,
) -> SyncResponse:
    """Process a batch of pending operations from the client.

    The batch is processed atomically — either all operations commit or
    none do (per-op failures are still reported individually so the client
    knows which ones to retry).

    Operations supported:
      - CREATE_CUSTOMER / UPDATE_CUSTOMER / DELETE_CUSTOMER
      - CREATE_DELIVERY / UPDATE_DELIVERY / COMPLETE_DELIVERY / CANCEL_DELIVERY

    Conflict resolution:
      - Customers: latest-write-wins based on updated_at. If the server has
        a newer version, the operation returns CONFLICT with server_state.
      - Delivery completion: idempotency_key prevents double-execution on retry.

    The response also includes `server_changes` — entities owned by this
    driver that were modified since the client's `latest_sync_timestamp`.
    This enables multi-device sync: a change made on Device A propagates
    to Device B on its next sync.
    """
    return await service.process(
        driver_id=user.id,
        request=body,
        ip_address=get_client_ip(request),
    )
