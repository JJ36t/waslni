package com.waslni.driver.domain.model

import java.util.UUID

/**
 * Domain model representing a customer in the Waselni app.
 *
 * IMPORTANT: This is a pure Kotlin class — no Android imports, no Room annotations,
 * no Retrofit annotations. The `domain` layer is framework-agnostic so we can test
 * business logic without an emulator.
 *
 * Identity:
 *   - `id` is generated on the Android side as a UUID string at creation time.
 *   - This allows offline-first: the customer exists locally with a stable ID
 *     even before the backend has acknowledged it.
 *
 * Timestamps:
 *   - Stored as epoch millis (Long) for cheap sorting, easy Room indexing,
 *     and timezone-independent comparisons.
 *   - Backend uses ISO 8601 strings; the API mapper converts both ways.
 *
 * The customer's "address" IS the geographic coordinate. There is no street,
 * house number, or description field by design (see PRD section 1.1).
 *
 * @property id           UUID string, primary key.
 * @property name         2-120 chars, trimmed.
 * @property phone        7-30 chars, digits + optional leading +.
 * @property latitude     Decimal degrees, -90..90.
 * @property longitude    Decimal degrees, -180..180.
 * @property accuracy     GPS accuracy in meters. Nullable because backend may
 *                        return null for legacy records.
 * @property createdAt    Epoch millis at first persistence (local or remote).
 * @property updatedAt    Epoch millis of last modification. Used by sync
 *                        conflict resolution (latest-write-wins).
 */
data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(name.length in 2..120) { "Name length must be 2..120, was ${name.length}" }
        require(phone.isNotBlank()) { "Phone cannot be blank" }
        require(phone.length in 7..30) { "Phone length must be 7..30, was ${phone.length}" }
        require(latitude in -90.0..90.0) { "Latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude out of range: $longitude" }
        require(accuracy == null || accuracy >= 0f) { "Accuracy cannot be negative: $accuracy" }
    }

    /**
     * Returns the customer's coordinate as a [LatLng] for use with the maps layer.
     */
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}
