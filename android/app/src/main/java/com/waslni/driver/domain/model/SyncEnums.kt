package com.waslni.driver.domain.model

/**
 * Sync state of a local entity.
 *
 * - SYNCED:  The entity matches the backend's version.
 * - PENDING: A local change has not yet been pushed to the backend.
 * - FAILED:  The sync worker gave up after MAX_RETRIES (Phase 12).
 *
 * This is stored on every entity (customers, deliveries) so the UI can show
 * a "pending sync" badge without joining to the sync_operations table.
 */
enum class SyncState {
    SYNCED,
    PENDING,
    FAILED;

    companion object {
        fun fromString(value: String): SyncState = valueOf(value.uppercase())
    }
}

/**
 * The kind of mutation a [SyncOperation] represents.
 *
 * Used by the sync worker (Phase 12) to decide which API endpoint and HTTP
 * method to call.
 *
 * COMPLETE is delivery-specific — it maps to PATCH /deliveries/{id}/status
 * with status=DELIVERED. We keep it separate from UPDATE because it needs
 * an Idempotency-Key header (see API contract §6.4).
 */
enum class SyncOperation {
    CREATE_CUSTOMER,
    UPDATE_CUSTOMER,
    DELETE_CUSTOMER,
    CREATE_DELIVERY,
    UPDATE_DELIVERY,
    COMPLETE_DELIVERY,
    CANCEL_DELIVERY;

    companion object {
        fun fromString(value: String): SyncOperation = valueOf(value.uppercase())
    }
}
