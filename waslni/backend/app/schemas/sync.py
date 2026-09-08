"""Pydantic schemas for the /sync endpoint.

Wire format matches docs/04-api-contract.md §7.

The sync endpoint accepts a batch of pending operations from the Android
client and returns:
  1. Per-operation results (SUCCESS / CONFLICT / FAILED / IGNORED).
  2. Server-side changes that happened since the client's last sync
     (download direction — for multi-device support).
  3. A new `latest_sync_timestamp` the client stores and sends next time.
"""
from datetime import datetime
from enum import Enum
from typing import Any
from uuid import UUID

from pydantic import BaseModel, Field


# === Request ===

class SyncOperationType(str, Enum):
    """The kind of mutation being synced."""

    CREATE_CUSTOMER = "CREATE_CUSTOMER"
    UPDATE_CUSTOMER = "UPDATE_CUSTOMER"
    DELETE_CUSTOMER = "DELETE_CUSTOMER"
    CREATE_DELIVERY = "CREATE_DELIVERY"
    UPDATE_DELIVERY = "UPDATE_DELIVERY"
    COMPLETE_DELIVERY = "COMPLETE_DELIVERY"
    CANCEL_DELIVERY = "CANCEL_DELIVERY"


class SyncEntityType(str, Enum):
    CUSTOMER = "CUSTOMER"
    DELIVERY = "DELIVERY"


class SyncOperationRequest(BaseModel):
    """A single pending operation sent from the client.

    `id` is the client-side operation id (UUID generated when the op was
    enqueued). The server uses it to correlate results back to the client.

    `payload` is the request body that would have been sent to the
    equivalent REST endpoint. For DELETE operations it contains only
    `{"id": "..."}`.

    `idempotency_key` is set only for COMPLETE_DELIVERY (and any future
    operation that requires idempotency).
    """

    id: str = Field(description="Client-generated operation id (UUID).")
    entity_type: SyncEntityType
    operation: SyncOperationType
    payload: dict[str, Any] = Field(
        description="Pre-serialized request body for this operation."
    )
    idempotency_key: str | None = None


class SyncRequest(BaseModel):
    """POST /sync body."""

    operations: list[SyncOperationRequest] = Field(
        max_length=500,
        description="Pending operations to process. Max 500 per batch.",
    )
    latest_sync_timestamp: datetime | None = Field(
        default=None,
        description=(
            "The server-issued timestamp from the client's last successful sync. "
            "Used to compute server_changes (download direction). "
            "Null on first sync → returns all of the client's data."
        ),
    )


# === Response ===

class SyncResultStatus(str, Enum):
    """Per-operation outcome."""

    SUCCESS = "SUCCESS"
    CONFLICT = "CONFLICT"
    FAILED = "FAILED"
    IGNORED = "IGNORED"


class SyncOperationResult(BaseModel):
    """Result of processing a single operation in the batch."""

    operation_id: str = Field(description="Matches the client's operation id.")
    status: SyncResultStatus
    entity_id: str | None = None
    server_state: dict[str, Any] | None = Field(
        default=None,
        description=(
            "For SUCCESS: the persisted entity. "
            "For CONFLICT: the server's current version (so the client can adopt it)."
        ),
    )
    error: dict[str, Any] | None = None


class ServerChange(BaseModel):
    """A server-side change the client should apply (download direction).

    The client reconciles these against its local Room database:
      - CREATE/UPDATE → upsert into Room with syncState=SYNCED.
      - DELETE → delete from Room.
    """

    entity_type: SyncEntityType
    entity_id: UUID
    operation: SyncOperationType
    payload: dict[str, Any]


class SyncResponse(BaseModel):
    """POST /sync response."""

    results: list[SyncOperationResult]
    server_changes: list[ServerChange] = Field(
        default_factory=list,
        description="Server-side changes since the client's latest_sync_timestamp.",
    )
    latest_sync_timestamp: datetime = Field(
        description=(
            "The client must store this and send it as latest_sync_timestamp "
            "in its next /sync call."
        ),
    )
