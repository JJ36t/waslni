"""Sync service — processes a batch of pending operations from Android.

Architecture:
  - Each operation in the batch is processed INDEPENDENTLY.
    A failure on one operation does NOT abort the others — the response
    carries per-op results.
  - The service delegates to CustomerService / DeliveryService for the
    actual mutation, then wraps the outcome in a SyncOperationResult.
  - Conflict resolution:
      * Customers (name/phone/location): latest-write-wins based on
        the `updated_at` in the payload vs. the server's `updated_at`.
        If the server has a newer version, we return CONFLICT + server_state
        so the client adopts the server's version.
      * Delivery completion: idempotency_key prevents double-execution.
  - After processing the batch, the service computes `server_changes` —
    all entities owned by the driver that were modified since the client's
    `latest_sync_timestamp` (multi-device sync).
  - The new `latest_sync_timestamp` is "now" — the client stores it and
    sends it on its next sync.

Why batch instead of one endpoint per operation:
  - Reduces HTTP round-trips (mobile networks are slow).
  - Lets the client dump its whole queue in one call.
  - Server can return download-direction changes in the same response.
"""
from datetime import datetime, timedelta, timezone
from typing import Any
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import AppError, ConflictError, ErrorCodes, NotFoundError
from app.models import AuditLog, Customer, Delivery
from app.repositories.audit_log_repo import AuditLogRepository
from app.repositories.customer_repo import CustomerRepository
from app.repositories.delivery_repo import DeliveryRepository
from app.repositories.idempotency_repo import IdempotencyKeyRepository
from app.schemas.customer import CustomerCreate, CustomerResponse, CustomerUpdate
from app.schemas.delivery import (
    DeliveryCreate,
    DeliveryResponse,
    DeliveryStatusEnum,
)
from app.schemas.sync import (
    ServerChange,
    SyncEntityType,
    SyncOperationRequest,
    SyncOperationResult,
    SyncOperationType,
    SyncRequest,
    SyncResponse,
    SyncResultStatus,
)
from app.services.customer_service import CustomerService
from app.services.delivery_service import DeliveryService


# Audit action for sync batch itself
AUDIT_SYNC_PERFORMED = "SYNC_PERFORMED"


class SyncService:
    """Processes batch sync operations from Android clients."""

    # Sync timestamp is "now minus a small grace period" so that writes
    # happening concurrently with the sync call are not missed on the
    # next sync. 5 seconds is generous; the client re-syncs on app foreground.
    _TIMESTAMP_GRACE = timedelta(seconds=5)

    def __init__(
        self,
        session: AsyncSession,
        customers_repo: CustomerRepository,
        deliveries_repo: DeliveryRepository,
        idempotency_repo: IdempotencyKeyRepository,
        audit_repo: AuditLogRepository,
        customer_service_factory: "CustomerServiceFactory",
        delivery_service_factory: "DeliveryServiceFactory",
    ) -> None:
        self.session = session
        self.customers_repo = customers_repo
        self.deliveries_repo = deliveries_repo
        self.idempotency_repo = idempotency_repo
        self.audit_repo = audit_repo
        self.customer_service_factory = customer_service_factory
        self.delivery_service_factory = delivery_service_factory

    async def process(
        self,
        driver_id: UUID,
        request: SyncRequest,
        ip_address: str | None = None,
    ) -> SyncResponse:
        """Process a batch of operations and return per-op results + server changes."""
        results: list[SyncOperationResult] = []

        for op in request.operations:
            result = await self._process_one(driver_id, op, ip_address)
            results.append(result)

        # Compute server-side changes since the client's last sync
        server_changes = await self._compute_server_changes(
            driver_id=driver_id,
            since=request.latest_sync_timestamp,
        )

        # The new sync timestamp — client stores this for next time
        new_timestamp = datetime.now(timezone.utc) - self._TIMESTAMP_GRACE

        await self.audit_repo.record(
            action=AUDIT_SYNC_PERFORMED,
            user_id=driver_id,
            metadata={
                "operations_count": len(request.operations),
                "success_count": sum(1 for r in results if r.status == SyncResultStatus.SUCCESS),
                "conflict_count": sum(1 for r in results if r.status == SyncResultStatus.CONFLICT),
                "failed_count": sum(1 for r in results if r.status == SyncResultStatus.FAILED),
            },
            ip_address=ip_address,
        )

        return SyncResponse(
            results=results,
            server_changes=server_changes,
            latest_sync_timestamp=new_timestamp,
        )

    # === Per-operation processing ===

    async def _process_one(
        self,
        driver_id: UUID,
        op: SyncOperationRequest,
        ip_address: str | None,
    ) -> SyncOperationResult:
        """Process a single operation, mapping exceptions to result statuses.

        Any exception is caught — sync NEVER aborts the batch.
        """
        try:
            return await self._dispatch(driver_id, op, ip_address)
        except ConflictError as e:
            # Conflict — return server_state if we can determine it
            server_state = await self._lookup_server_state(driver_id, op)
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.CONFLICT,
                entity_id=op.payload.get("id"),
                server_state=server_state,
                error={"code": e.code, "message": e.message, "details": e.details},
            )
        except NotFoundError as e:
            # Likely a DELETE for an already-deleted entity — treat as IGNORED
            if op.operation in (
                SyncOperationType.DELETE_CUSTOMER,
                SyncOperationType.UPDATE_CUSTOMER,
                SyncOperationType.UPDATE_DELIVERY,
                SyncOperationType.CANCEL_DELIVERY,
            ):
                return SyncOperationResult(
                    operation_id=op.id,
                    status=SyncResultStatus.IGNORED,
                    entity_id=op.payload.get("id"),
                    error={"code": e.code, "message": e.message},
                )
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.FAILED,
                entity_id=op.payload.get("id"),
                error={"code": e.code, "message": e.message},
            )
        except AppError as e:
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.FAILED,
                entity_id=op.payload.get("id"),
                error={"code": e.code, "message": e.message, "details": e.details},
            )
        except Exception as e:
            # Don't leak internals
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.FAILED,
                entity_id=op.payload.get("id"),
                error={"code": ErrorCodes.INTERNAL_ERROR, "message": "Internal error"},
            )

    async def _dispatch(
        self,
        driver_id: UUID,
        op: SyncOperationRequest,
        ip_address: str | None,
    ) -> SyncOperationResult:
        """Route the operation to the right service method."""
        if op.entity_type == SyncEntityType.CUSTOMER:
            return await self._process_customer_op(driver_id, op, ip_address)
        elif op.entity_type == SyncEntityType.DELIVERY:
            return await self._process_delivery_op(driver_id, op, ip_address)
        else:
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.FAILED,
                error={"code": "UNKNOWN_ENTITY_TYPE", "message": f"Unknown entity type: {op.entity_type}"},
            )

    # === Customer operations ===

    async def _process_customer_op(
        self,
        driver_id: UUID,
        op: SyncOperationRequest,
        ip_address: str | None,
    ) -> SyncOperationResult:
        service = self.customer_service_factory(driver_id)

        if op.operation == SyncOperationType.CREATE_CUSTOMER:
            data = CustomerCreate(**op.payload)
            result = await service.create(driver_id=driver_id, data=data, ip_address=ip_address)
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.SUCCESS,
                entity_id=str(result.id),
                server_state=result.model_dump(mode="json"),
            )

        elif op.operation == SyncOperationType.UPDATE_CUSTOMER:
            # Conflict check: latest-write-wins based on updated_at
            entity_id = UUID(op.payload["id"])
            existing = await self.customers_repo.get_by_id(entity_id, driver_id)
            if existing is None:
                raise NotFoundError(
                    code=ErrorCodes.CUSTOMER_NOT_FOUND,
                    message="Customer not found",
                )

            # Parse updated_at from payload (ISO 8601 string from Android)
            payload_updated_str = op.payload.get("updated_at")
            if payload_updated_str:
                payload_updated = _parse_iso8601(payload_updated_str)
                if existing.updated_at > payload_updated:
                    # Server has newer version → CONFLICT
                    return SyncOperationResult(
                        operation_id=op.id,
                        status=SyncResultStatus.CONFLICT,
                        entity_id=str(entity_id),
                        server_state=CustomerResponse.model_validate(existing).model_dump(mode="json"),
                        error={
                            "code": ErrorCodes.STALE_UPDATE,
                            "message": "Server has a newer version of this customer",
                        },
                    )

            # Apply the update (only name/phone/lat/lng/accuracy — not id/driver_id/timestamps)
            update_data = CustomerUpdate(
                name=op.payload.get("name"),
                phone=op.payload.get("phone"),
                latitude=op.payload.get("latitude"),
                longitude=op.payload.get("longitude"),
                accuracy=op.payload.get("accuracy"),
            )
            result = await service.update(
                customer_id=entity_id,
                driver_id=driver_id,
                data=update_data,
                ip_address=ip_address,
            )
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.SUCCESS,
                entity_id=str(result.id),
                server_state=result.model_dump(mode="json"),
            )

        elif op.operation == SyncOperationType.DELETE_CUSTOMER:
            entity_id = UUID(op.payload["id"])
            await service.delete(
                customer_id=entity_id,
                driver_id=driver_id,
                ip_address=ip_address,
            )
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.SUCCESS,
                entity_id=str(entity_id),
            )

        else:
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.FAILED,
                error={"code": "UNKNOWN_OPERATION", "message": f"Unknown customer op: {op.operation}"},
            )

    # === Delivery operations ===

    async def _process_delivery_op(
        self,
        driver_id: UUID,
        op: SyncOperationRequest,
        ip_address: str | None,
    ) -> SyncOperationResult:
        service = self.delivery_service_factory(driver_id)

        if op.operation == SyncOperationType.CREATE_DELIVERY:
            data = DeliveryCreate(
                customer_id=UUID(op.payload["customer_id"]),
                status=DeliveryStatusEnum(op.payload.get("status", "ON_THE_WAY")),
            )
            result = await service.create(
                driver_id=driver_id, data=data, ip_address=ip_address
            )
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.SUCCESS,
                entity_id=str(result.id),
                server_state=result.model_dump(mode="json"),
            )

        elif op.operation == SyncOperationType.COMPLETE_DELIVERY:
            entity_id = UUID(op.payload["id"])
            result = await service.transition(
                delivery_id=entity_id,
                driver_id=driver_id,
                target=DeliveryStatusEnum.DELIVERED,
                idempotency_key=op.idempotency_key,
                request_payload=op.payload,
                ip_address=ip_address,
            )
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.SUCCESS,
                entity_id=str(result.id),
                server_state=result.model_dump(mode="json"),
            )

        elif op.operation == SyncOperationType.CANCEL_DELIVERY:
            entity_id = UUID(op.payload["id"])
            result = await service.transition(
                delivery_id=entity_id,
                driver_id=driver_id,
                target=DeliveryStatusEnum.CANCELLED,
                idempotency_key=op.idempotency_key,
                request_payload=op.payload,
                ip_address=ip_address,
            )
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.SUCCESS,
                entity_id=str(result.id),
                server_state=result.model_dump(mode="json"),
            )

        elif op.operation == SyncOperationType.UPDATE_DELIVERY:
            # Generic update — used for ARRIVED transition
            entity_id = UUID(op.payload["id"])
            target_str = op.payload.get("status")
            if not target_str:
                return SyncOperationResult(
                    operation_id=op.id,
                    status=SyncResultStatus.FAILED,
                    error={"code": "MISSING_STATUS", "message": "UPDATE_DELIVERY requires 'status' in payload"},
                )
            result = await service.transition(
                delivery_id=entity_id,
                driver_id=driver_id,
                target=DeliveryStatusEnum(target_str),
                idempotency_key=op.idempotency_key,
                request_payload=op.payload,
                ip_address=ip_address,
            )
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.SUCCESS,
                entity_id=str(result.id),
                server_state=result.model_dump(mode="json"),
            )

        else:
            return SyncOperationResult(
                operation_id=op.id,
                status=SyncResultStatus.FAILED,
                error={"code": "UNKNOWN_OPERATION", "message": f"Unknown delivery op: {op.operation}"},
            )

    # === Server-side changes (download direction) ===

    async def _compute_server_changes(
        self,
        driver_id: UUID,
        since: datetime | None,
    ) -> list[ServerChange]:
        """Find entities owned by this driver that changed since `since`.

        Used for multi-device sync: if the user makes a change on Device A,
        Device B receives it as a server_change on its next sync.
        """
        if since is None:
            # First sync — return nothing; the client already has its own
            # local data and we don't want to flood it.
            return []

        changes: list[ServerChange] = []

        # Customers modified since `since`
        cust_result = await self.session.execute(
            select(Customer).where(
                Customer.driver_id == driver_id,
                Customer.updated_at > since,
            )
        )
        for c in cust_result.scalars().all():
            # Heuristic: if created_at ≈ updated_at → CREATE, else UPDATE
            op = (
                SyncOperationType.CREATE_CUSTOMER
                if abs((c.updated_at - c.created_at).total_seconds()) < 1
                else SyncOperationType.UPDATE_CUSTOMER
            )
            changes.append(ServerChange(
                entity_type=SyncEntityType.CUSTOMER,
                entity_id=c.id,
                operation=op,
                payload=CustomerResponse.model_validate(c).model_dump(mode="json"),
            ))

        # Deliveries modified since `since`
        del_result = await self.session.execute(
            select(Delivery).where(
                Delivery.driver_id == driver_id,
                # Deliveries don't have updated_at; use completed_at / cancelled_at / arrived_at
                # OR created_at as the "modified" timestamp.
            )
        )
        for d in del_result.scalars().all():
            modified_at = max(
                ts for ts in [
                    d.created_at, d.started_at, d.arrived_at, d.completed_at, d.cancelled_at
                ] if ts is not None
            )
            if modified_at <= since:
                continue

            # Determine operation type
            if d.status == "DELIVERED":
                op = SyncOperationType.COMPLETE_DELIVERY
            elif d.status == "CANCELLED":
                op = SyncOperationType.CANCEL_DELIVERY
            elif abs((modified_at - d.created_at).total_seconds()) < 1:
                op = SyncOperationType.CREATE_DELIVERY
            else:
                op = SyncOperationType.UPDATE_DELIVERY

            # Build the payload — include the customer_id and status
            payload: dict[str, Any] = {
                "id": str(d.id),
                "customer_id": str(d.customer_id),
                "status": d.status,
                "created_at": d.created_at.isoformat() if d.created_at else None,
                "started_at": d.started_at.isoformat() if d.started_at else None,
                "arrived_at": d.arrived_at.isoformat() if d.arrived_at else None,
                "completed_at": d.completed_at.isoformat() if d.completed_at else None,
                "cancelled_at": d.cancelled_at.isoformat() if d.cancelled_at else None,
            }
            changes.append(ServerChange(
                entity_type=SyncEntityType.DELIVERY,
                entity_id=d.id,
                operation=op,
                payload=payload,
            ))

        return changes

    # === Helpers ===

    async def _lookup_server_state(
        self, driver_id: UUID, op: SyncOperationRequest
    ) -> dict[str, Any] | None:
        """For CONFLICT results — return the server's current version of the entity."""
        try:
            entity_id = UUID(op.payload["id"])
        except (KeyError, ValueError, TypeError):
            return None

        if op.entity_type == SyncEntityType.CUSTOMER:
            customer = await self.customers_repo.get_by_id(entity_id, driver_id)
            if customer is not None:
                return CustomerResponse.model_validate(customer).model_dump(mode="json")
        elif op.entity_type == SyncEntityType.DELIVERY:
            delivery = await self.deliveries_repo.get_by_id(entity_id, driver_id)
            if delivery is not None:
                # Use the same _to_response logic from DeliveryService
                from app.services.delivery_service import _to_response
                return _to_response(delivery).model_dump(mode="json")

        return None


# === Service factory type aliases ===

# These let us construct per-request CustomerService / DeliveryService
# instances bound to the same session as the SyncService.

CustomerServiceFactory = "Callable[[UUID], CustomerService]"
DeliveryServiceFactory = "Callable[[UUID], DeliveryService]"


def _parse_iso8601(value: str) -> datetime:
    """Parse an ISO 8601 timestamp from the client.

    Handles both with and without trailing 'Z'.
    """
    if value.endswith("Z"):
        value = value[:-1] + "+00:00"
    return datetime.fromisoformat(value)
