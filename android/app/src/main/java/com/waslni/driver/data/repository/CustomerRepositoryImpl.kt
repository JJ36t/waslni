package com.waslni.driver.data.repository

import com.waslni.driver.data.local.dao.CustomerDao
import com.waslni.driver.data.local.dao.DeliveryDao
import com.waslni.driver.data.local.dao.SyncOperationDao
import com.waslni.driver.data.local.mapper.toDomain
import com.waslni.driver.data.local.mapper.toEntity
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.SyncOperation
import com.waslni.driver.domain.model.SyncOperation as SyncOp
import com.waslni.driver.domain.model.SyncOperationStatus
import com.waslni.driver.domain.model.SyncState
import com.waslni.driver.domain.repository.CustomerRepository
import com.waslni.driver.domain.usecase.sync.ScheduleSyncUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [CustomerRepository].
 *
 * - All reads come from Room.
 * - All writes go to Room AND enqueue a [SyncOperation].
 * - The remote API is the sync worker's concern (Phase 12), not this class.
 *
 * Duplicate phone detection:
 *   - We block inserts/updates that would introduce a duplicate phone for
 *     the same driver. (The backend enforces this too via UNIQUE(driver_id, phone).)
 *
 * Active delivery check on delete:
 *   - We block deletion of a customer that has an active delivery (ON_THE_WAY
 *     or ARRIVED). This matches the backend's 409 response.
 */
@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val deliveryDao: DeliveryDao,
    private val syncDao: SyncOperationDao,
    private val json: Json,
    private val scheduleSync: ScheduleSyncUseCase
) : CustomerRepository {

    override fun observeAll(): Flow<List<Customer>> =
        customerDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Customer>> =
        customerDao.search(query).map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Customer?> =
        customerDao.observeById(id).map { row -> row?.toDomain() }

    override suspend fun getCustomer(id: String): Customer? =
        customerDao.getById(id)?.toDomain()

    override suspend fun getCustomerByPhone(phone: String): Customer? =
        customerDao.getByPhone(phone)?.toDomain()

    override fun observeCount(): Flow<Int> = customerDao.observeCount()

    override suspend fun addCustomer(customer: Customer): Customer {
        // Duplicate phone check
        val existing = customerDao.getByPhone(customer.phone)
        if (existing != null && existing.id != customer.id) {
            throw IllegalArgumentException("Duplicate phone: ${customer.phone}")
        }

        val now = System.currentTimeMillis()
        val toSave = customer.copy(
            createdAt = now,
            updatedAt = now
        )

        // Persist to Room with syncState=PENDING
        customerDao.insert(toSave.toEntity(SyncState.PENDING))

        // Enqueue a sync operation
        enqueueSyncOperation(
            entityId = toSave.id,
            operation = SyncOp.CREATE_CUSTOMER,
            payload = json.encodeToString(CustomerSyncPayload.serializer(), toSave.toPayload())
        )

        return toSave
    }

    override suspend fun updateCustomer(customer: Customer): Customer {
        val existing = customerDao.getById(customer.id)
            ?: throw IllegalArgumentException("Customer not found: ${customer.id}")

        // Duplicate phone check (in case phone is being changed)
        val byPhone = customerDao.getByPhone(customer.phone)
        if (byPhone != null && byPhone.id != customer.id) {
            throw IllegalArgumentException("Duplicate phone: ${customer.phone}")
        }

        val now = System.currentTimeMillis()
        val toSave = customer.copy(updatedAt = now)

        customerDao.update(toSave.toEntity(SyncState.PENDING))

        enqueueSyncOperation(
            entityId = toSave.id,
            operation = SyncOp.UPDATE_CUSTOMER,
            payload = json.encodeToString(CustomerSyncPayload.serializer(), toSave.toPayload())
        )

        return toSave
    }

    override suspend fun deleteCustomer(id: String) {
        // Block deletion if active delivery exists
        val active = deliveryDao.getActiveForCustomer(id)
        if (active != null) {
            throw IllegalStateException("Customer has active delivery: ${active.id}")
        }

        val existing = customerDao.getById(id)
            ?: return // already deleted — idempotent

        customerDao.deleteById(id)

        enqueueSyncOperation(
            entityId = id,
            operation = SyncOp.DELETE_CUSTOMER,
            payload = json.encodeToString(
                CustomerSyncPayload.serializer(),
                CustomerSyncPayload(id = id, name = "", phone = "", latitude = 0.0, longitude = 0.0, accuracy = null, createdAt = 0, updatedAt = 0)
            )
        )
    }

    private suspend fun enqueueSyncOperation(
        entityId: String,
        operation: SyncOp,
        payload: String
    ) {
        val op = SyncOperation(
            entityId = entityId,
            entityType = "CUSTOMER",
            operation = operation,
            payload = payload,
            status = SyncOperationStatus.PENDING
        )
        syncDao.insert(op.toEntity())
        // Schedule an immediate sync so the operation drains ASAP when online.
        scheduleSync()
    }
}

/**
 * Wire format for customer sync payloads.
 *
 * This must stay in sync with the backend's `SyncOperationRequest.payload`
 * schema (see docs/04-api-contract.md §7.1).
 */
@kotlinx.serialization.Serializable
data class CustomerSyncPayload(
    val id: String,
    val name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Convenience mapper from domain Customer to sync payload.
 */
private fun Customer.toPayload(): CustomerSyncPayload = CustomerSyncPayload(
    id = id,
    name = name,
    phone = phone,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    createdAt = createdAt,
    updatedAt = updatedAt
)
