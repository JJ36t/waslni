"""add concurrency safety: partial unique index + soft delete + tombstones

Revision ID: 0002
Revises: 0001
Create Date: 2026-09-08

Changes:
  1. Partial unique index on deliveries(customer_id) WHERE status IN ('ON_THE_WAY', 'ARRIVED')
     → prevents race condition creating two active deliveries for same customer.
  2. Soft-delete columns: customers.deleted_at, deliveries.deleted_at
     → enables multi-device sync of deletions via server_changes.
  3. sync_tombstones table
     → records all deletions for download-direction sync (Device B learns about
        deletions made on Device A).
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # === 1. Partial unique index — prevents two active deliveries for same customer ===
    op.create_index(
        "idx_deliveries_one_active_per_customer",
        "deliveries",
        ["customer_id"],
        unique=True,
        postgresql_where=sa.text("status IN ('ON_THE_WAY', 'ARRIVED')"),
    )

    # === 2. Soft-delete columns ===
    op.add_column("customers", sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column("deliveries", sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=True))

    # === 3. sync_tombstones — records deletions for multi-device sync ===
    op.create_table(
        "sync_tombstones",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("driver_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("entity_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("entity_type", sa.String(30), nullable=False),  # CUSTOMER | DELIVERY
        sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )
    op.create_index("idx_tombstones_driver", "sync_tombstones", ["driver_id"])
    op.create_index("idx_tombstones_entity", "sync_tombstones", ["entity_type", "entity_id"])
    op.create_index("idx_tombstones_deleted_at", "sync_tombstones", ["deleted_at"])


def downgrade() -> None:
    op.drop_index("idx_tombstones_deleted_at", table_name="sync_tombstones")
    op.drop_index("idx_tombstones_entity", table_name="sync_tombstones")
    op.drop_index("idx_tombstones_driver", table_name="sync_tombstones")
    op.drop_table("sync_tombstones")

    op.drop_column("deliveries", "deleted_at")
    op.drop_column("customers", "deleted_at")

    op.drop_index("idx_deliveries_one_active_per_customer", table_name="deliveries")
