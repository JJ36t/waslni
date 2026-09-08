"""Query performance optimization helpers.

These utilities help identify and fix slow queries:

  - [explain_query] — runs EXPLAIN ANALYZE on a query and returns the plan.
    Use during development to verify indexes are being used.
  - [SLOW_QUERY_THRESHOLD_MS] — queries slower than this are logged as warnings.

Common optimizations applied in the codebase:
  1. Eager loading: DeliveryRepository.get_by_id uses selectinload(Delivery.customer)
     to avoid N+1 queries when the response includes nested customer data.
  2. Composite indexes: idx_customers_driver_name (driver_id, name) for search +
     idx_deliveries_driver_status (driver_id, status) for status filtering.
  3. Count queries use SELECT COUNT(*) (not SELECT * + len()) — the DB optimizes.
  4. Pagination uses OFFSET + LIMIT (not fetch-all + slice).
  5. Search uses ILIKE with prefix match (not full-text search — overkill for our scale).
"""
import logging
import time
from typing import Any

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

logger = logging.getLogger(__name__)

SLOW_QUERY_THRESHOLD_MS = 100  # log queries slower than 100ms


async def explain_query(session: AsyncSession, sql: str, params: dict[str, Any] | None = None) -> str:
    """Run EXPLAIN ANALYZE on a query and return the execution plan.

    Usage (development only):
        plan = await explain_query(session, "SELECT * FROM customers WHERE driver_id = :did", {"did": uuid})
        print(plan)

    Look for:
      - "Seq Scan" → bad (full table scan, no index used).
      - "Index Scan" or "Index Only Scan" → good (index is being used).
      - "Execution Time: X ms" → should be < SLOW_QUERY_THRESHOLD_MS.
    """
    result = await session.execute(
        text(f"EXPLAIN ANALYZE {sql}"),
        params or {}
    )
    rows = result.fetchall()
    return "\n".join(row[0] for row in rows)


class QueryTimer:
    """Context manager that logs query execution time.

    Usage:
        async with QueryTimer("get_customer_by_id"):
            customer = await session.execute(...)

    Logs a warning if the query takes longer than SLOW_QUERY_THRESHOLD_MS.
    """

    def __init__(self, label: str) -> None:
        self.label = label
        self.start: float = 0

    def __enter__(self) -> "QueryTimer":
        self.start = time.monotonic()
        return self

    def __exit__(self, *args: Any) -> None:
        elapsed_ms = (time.monotonic() - self.start) * 1000
        if elapsed_ms > SLOW_QUERY_THRESHOLD_MS:
            logger.warning(
                "Slow query detected",
                extra={
                    "query": self.label,
                    "elapsed_ms": round(elapsed_ms, 2),
                    "threshold_ms": SLOW_QUERY_THRESHOLD_MS,
                }
            )
        else:
            logger.debug(
                "Query executed",
                extra={"query": self.label, "elapsed_ms": round(elapsed_ms, 2)}
            )


# === Index verification queries ===
# Run these after migrations to verify indexes exist:

VERIFY_INDEXES_SQL = """
SELECT
    t.tablename,
    i.indexname,
    i.indexdef
FROM pg_indexes i
JOIN pg_tables t ON i.schemaname = t.schemaname AND i.tablename = t.tablename
WHERE t.tablename IN ('users', 'customers', 'deliveries', 'sync_operations',
                      'refresh_tokens', 'audit_logs', 'idempotency_keys')
ORDER BY t.tablename, i.indexname;
"""

# Expected indexes (from the initial migration):
EXPECTED_INDEXES = {
    "users": ["idx_users_username"],
    "customers": [
        "idx_customers_driver_name",
        "idx_customers_driver_phone",
        "uq_driver_phone",
    ],
    "deliveries": [
        "idx_deliveries_driver_status",
        "idx_deliveries_driver_created",
        "idx_deliveries_customer",
    ],
    "refresh_tokens": [
        "idx_refresh_tokens_user",
        "idx_refresh_tokens_hash",
    ],
    "audit_logs": [
        "idx_audit_user_date",
        "idx_audit_action",
    ],
    "idempotency_keys": [
        "idx_idempotency_key_user",
        "idx_idempotency_expires",
    ],
}
