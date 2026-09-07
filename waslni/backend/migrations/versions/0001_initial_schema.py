"""initial schema

Revision ID: 0001
Revises:
Create Date: 2026-09-08
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

# revision identifiers
revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # === users ===
    op.create_table(
        "users",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True,
                  server_default=sa.text("gen_random_uuid()")),
        sa.Column("username", sa.String(50), nullable=False, unique=True),
        sa.Column("password_hash", sa.String(255), nullable=False),
        sa.Column("role", sa.String(20), nullable=False, server_default="driver"),
        sa.Column("is_active", sa.Boolean, nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.Column("last_login_at", sa.DateTime(timezone=True), nullable=True),
        sa.CheckConstraint("role IN ('driver', 'admin')", name="chk_user_role"),
    )
    op.create_index("idx_users_username", "users", ["username"])

    # === customers ===
    op.create_table(
        "customers",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("driver_id", postgresql.UUID(as_uuid=True),
                  sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("name", sa.String(120), nullable=False),
        sa.Column("phone", sa.String(30), nullable=False),
        sa.Column("latitude", postgresql.NUMERIC(10, 7), nullable=False),
        sa.Column("longitude", postgresql.NUMERIC(10, 7), nullable=False),
        sa.Column("accuracy", sa.Float, nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.UniqueConstraint("driver_id", "phone", name="uq_driver_phone"),
        sa.CheckConstraint("char_length(name) >= 2", name="chk_customer_name_length"),
        sa.CheckConstraint("char_length(phone) >= 7", name="chk_customer_phone_length"),
        sa.CheckConstraint("latitude >= -90 AND latitude <= 90", name="chk_latitude"),
        sa.CheckConstraint("longitude >= -180 AND longitude <= 180", name="chk_longitude"),
        sa.CheckConstraint("accuracy IS NULL OR accuracy >= 0", name="chk_accuracy_non_negative"),
    )
    op.create_index("idx_customers_driver_name", "customers", ["driver_id", "name"])
    op.create_index("idx_customers_driver_phone", "customers", ["driver_id", "phone"])

    # === deliveries ===
    op.create_table(
        "deliveries",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("customer_id", postgresql.UUID(as_uuid=True),
                  sa.ForeignKey("customers.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("driver_id", postgresql.UUID(as_uuid=True),
                  sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("status", sa.String(20), nullable=False, server_default="PENDING"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.Column("started_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("arrived_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("cancelled_at", sa.DateTime(timezone=True), nullable=True),
        sa.CheckConstraint(
            "status IN ('PENDING', 'ASSIGNED', 'ON_THE_WAY', 'ARRIVED', 'DELIVERED', 'CANCELLED')",
            name="chk_delivery_status",
        ),
        sa.CheckConstraint(
            """
            (status = 'PENDING' AND started_at IS NULL AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'ASSIGNED' AND started_at IS NULL AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'ON_THE_WAY' AND started_at IS NOT NULL AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'ARRIVED' AND started_at IS NOT NULL AND arrived_at IS NOT NULL AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'DELIVERED' AND started_at IS NOT NULL AND completed_at IS NOT NULL AND cancelled_at IS NULL)
            OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
            """,
            name="chk_delivery_times",
        ),
    )
    op.create_index("idx_deliveries_driver_status", "deliveries", ["driver_id", "status"])
    op.create_index("idx_deliveries_driver_created", "deliveries", ["driver_id", "created_at"])
    op.create_index("idx_deliveries_customer", "deliveries", ["customer_id"])

    # === refresh_tokens ===
    op.create_table(
        "refresh_tokens",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True,
                  server_default=sa.text("gen_random_uuid()")),
        sa.Column("user_id", postgresql.UUID(as_uuid=True),
                  sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("token_hash", sa.String(255), nullable=False, unique=True),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("revoked", sa.Boolean, nullable=False, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.Column("device_info", sa.String(255), nullable=True),
    )
    op.create_index("idx_refresh_tokens_user", "refresh_tokens", ["user_id"])
    op.create_index("idx_refresh_tokens_hash", "refresh_tokens", ["token_hash"])

    # === audit_logs ===
    op.create_table(
        "audit_logs",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True,
                  server_default=sa.text("gen_random_uuid()")),
        sa.Column("user_id", postgresql.UUID(as_uuid=True),
                  sa.ForeignKey("users.id", ondelete="SET NULL"), nullable=True),
        sa.Column("action", sa.String(50), nullable=False),
        sa.Column("entity_type", sa.String(30), nullable=True),
        sa.Column("entity_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("metadata", postgresql.JSONB, nullable=True),
        sa.Column("ip_address", sa.String(45), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
    )
    op.create_index("idx_audit_user_date", "audit_logs", ["user_id", "created_at"])
    op.create_index("idx_audit_action", "audit_logs", ["action"])

    # === idempotency_keys ===
    op.create_table(
        "idempotency_keys",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True,
                  server_default=sa.text("gen_random_uuid()")),
        sa.Column("key", sa.String(255), nullable=False, unique=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True),
                  sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("endpoint", sa.String(100), nullable=False),
        sa.Column("request_hash", sa.String(64), nullable=False),
        sa.Column("response", postgresql.JSONB, nullable=False),
        sa.Column("status_code", sa.Integer, nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now()")),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False,
                  server_default=sa.text("now() + interval '24 hours'")),
    )
    op.create_index("idx_idempotency_key_user", "idempotency_keys", ["key", "user_id"])
    op.create_index("idx_idempotency_expires", "idempotency_keys", ["expires_at"])


def downgrade() -> None:
    op.drop_table("idempotency_keys")
    op.drop_table("audit_logs")
    op.drop_table("refresh_tokens")
    op.drop_table("deliveries")
    op.drop_table("customers")
    op.drop_table("users")
