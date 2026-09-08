package com.waslni.driver.data.remote.mapper

import com.waslni.driver.data.remote.dto.CustomerResponseDto
import com.waslni.driver.data.remote.dto.DeliveryResponseDto
import com.waslni.driver.data.remote.dto.UserDto
import com.waslni.driver.domain.model.DeliveryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for DTO → domain model mappers.
 *
 * Pure functions — no Android, no network — so plain JUnit.
 */
class DtoMappersTest {

    @Test
    fun `UserDto maps to User domain`() {
        val dto = UserDto(
            id = "abc-123",
            username = "driver_01",
            role = "driver",
            isActive = true,
            lastLoginAt = "2026-09-08T10:30:00Z"
        )

        val user = dto.toDomain()

        assertEquals("abc-123", user.id)
        assertEquals("driver_01", user.username)
        assertEquals("driver", user.role)
        assertEquals(true, user.isActive)
        assertEquals(1780_000_000_000L, user.lastLoginAt!!)  // rough check
    }

    @Test
    fun `UserDto with null lastLoginAt maps to null lastLoginAt`() {
        val dto = UserDto(
            id = "abc",
            username = "u",
            role = "driver",
            isActive = true,
            lastLoginAt = null
        )

        assertNull(dto.toDomain().lastLoginAt)
    }

    @Test
    fun `UserDto with malformed lastLoginAt maps to null lastLoginAt`() {
        val dto = UserDto(
            id = "abc",
            username = "u",
            role = "driver",
            isActive = true,
            lastLoginAt = "not-a-date"
        )

        assertNull(dto.toDomain().lastLoginAt)
    }

    @Test
    fun `CustomerResponseDto maps to Customer domain`() {
        val dto = CustomerResponseDto(
            id = "cust-1",
            name = "محمد أحمد",
            phone = "07801234567",
            latitude = 31.978942,
            longitude = 44.940127,
            accuracy = 4.2f,
            createdAt = "2026-09-08T10:30:00Z",
            updatedAt = "2026-09-08T11:00:00Z"
        )

        val customer = dto.toDomain()

        assertEquals("cust-1", customer.id)
        assertEquals("محمد أحمد", customer.name)
        assertEquals("07801234567", customer.phone)
        assertEquals(31.978942, customer.latitude, 0.0000001)
        assertEquals(44.940127, customer.longitude, 0.0000001)
        assertEquals(4.2f, customer.accuracy!!, 0.01f)
        assertEquals(1780_000_000_000L, customer.createdAt)  // rough check
        assertEquals(1780_000_000_000L, customer.updatedAt)
    }

    @Test
    fun `CustomerResponseDto with null accuracy maps to null accuracy`() {
        val dto = CustomerResponseDto(
            id = "cust-1",
            name = "X",
            phone = "07801234567",
            latitude = 31.0,
            longitude = 44.0,
            accuracy = null,
            createdAt = "2026-09-08T10:30:00Z",
            updatedAt = "2026-09-08T10:30:00Z"
        )

        assertNull(dto.toDomain().accuracy)
    }

    @Test
    fun `DeliveryResponseDto with all timestamps maps correctly`() {
        val dto = DeliveryResponseDto(
            id = "del-1",
            customerId = "cust-1",
            customer = null,
            status = "DELIVERED",
            createdAt = "2026-09-08T10:00:00Z",
            startedAt = "2026-09-08T10:01:00Z",
            arrivedAt = "2026-09-08T10:15:00Z",
            completedAt = "2026-09-08T10:16:00Z",
            cancelledAt = null
        )

        val delivery = dto.toDomain()

        assertEquals("del-1", delivery.id)
        assertEquals("cust-1", delivery.customerId)
        assertEquals(DeliveryStatus.DELIVERED, delivery.status)
        assertEquals(1780_000_000_000L, delivery.createdAt)  // rough
        assertEquals(1780_000_000_000L, delivery.startedAt!!)  // rough
        assertEquals(1780_000_000_000L, delivery.arrivedAt!!)
        assertEquals(1780_000_000_000L, delivery.completedAt!!)
        assertNull(delivery.cancelledAt)
    }

    @Test
    fun `DeliveryResponseDto with CANCELLED status maps correctly`() {
        val dto = DeliveryResponseDto(
            id = "del-1",
            customerId = "cust-1",
            customer = null,
            status = "CANCELLED",
            createdAt = "2026-09-08T10:00:00Z",
            startedAt = "2026-09-08T10:01:00Z",
            arrivedAt = null,
            completedAt = null,
            cancelledAt = "2026-09-08T10:05:00Z"
        )

        val delivery = dto.toDomain()

        assertEquals(DeliveryStatus.CANCELLED, delivery.status)
        assertNull(delivery.arrivedAt)
        assertNull(delivery.completedAt)
        assertEquals(1780_000_000_000L, delivery.cancelledAt!!)
    }

    @Test
    fun `parseIso8601ToMillis returns 0 for malformed input`() {
        assertEquals(0L, parseIso8601ToMillis("not-a-date"))
        assertEquals(0L, parseIso8601ToMillis(""))
    }

    @Test
    fun `parseIso8601ToMillis parses valid ISO 8601 with Z`() {
        // 2026-01-01T00:00:00Z = 1767225600 seconds = 1767225600000 millis
        assertEquals(1767225600000L, parseIso8601ToMillis("2026-01-01T00:00:00Z"))
    }

    @Test
    fun `formatMillisToIso8601 round-trips with parseIso8601ToMillis`() {
        val original = 1767225600000L
        val formatted = formatMillisToIso8601(original)
        val parsed = parseIso8601ToMillis(formatted)
        assertEquals(original, parsed)
    }
}
