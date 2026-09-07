package com.waslni.driver.data.local.mapper

import com.waslni.driver.data.local.entity.CustomerEntity
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.SyncState

/**
 * Map a Room [CustomerEntity] to the domain [Customer].
 *
 * Domain models never expose `syncState` — that's a data-layer concern.
 * The repository reads syncState separately when it needs to (e.g. to
 * decide whether to enqueue a sync operation).
 */
fun CustomerEntity.toDomain(): Customer = Customer(
    id = id,
    name = name,
    phone = phone,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Map a domain [Customer] to a Room [CustomerEntity] for persistence.
 *
 * @param syncState The sync state to write. Defaults to PENDING because the
 *                  common case is "the user just created/edited this row".
 *                  The repository sets SYNCED when pulling from the backend.
 */
fun Customer.toEntity(syncState: SyncState = SyncState.PENDING): CustomerEntity = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = syncState.name
)

/**
 * Read the syncState of an entity as a typed [SyncState].
 */
fun CustomerEntity.syncState(): SyncState = SyncState.fromString(syncState)
