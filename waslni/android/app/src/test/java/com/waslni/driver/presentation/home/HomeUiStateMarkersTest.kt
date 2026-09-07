package com.waslni.driver.presentation.home

import com.waslni.driver.core.maps.model.MarkerType
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.LocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [HomeUiState.markers] derivation logic.
 *
 * This is the heart of the map rendering: given the current state
 * (customers + active deliveries + driver location), produce the
 * correct list of [MapMarker]s.
 *
 * Pure function — no Compose, no Hilt, no Android — so we can test it
 * directly with plain JUnit.
 */
class HomeUiStateMarkersTest {

    private fun customer(
        id: String = "c1",
        name: String = "محمد",
        phone: String = "07801234567",
        lat: Double = 31.978942,
        lng: Double = 44.940127
    ) = Customer(id = id, name = name, phone = phone, latitude = lat, longitude = lng, accuracy = 4f)

    private fun driverLoc() = LocationResult(
        latitude = 31.985, longitude = 44.942, accuracy = 5f, timestamp = 1L
    )

    @Test
    fun `empty state produces no markers`() {
        val state = HomeUiState()
        assertTrue(state.markers().isEmpty())
    }

    @Test
    fun `driver location produces a DRIVER marker`() {
        val state = HomeUiState(driverLocation = driverLoc())

        val markers = state.markers()
        assertEquals(1, markers.size)
        assertEquals(MarkerType.DRIVER, markers[0].type)
        assertEquals(HomeUiState.MARKER_DRIVER_ID, markers[0].id)
    }

    @Test
    fun `single customer with no active delivery produces CUSTOMER marker`() {
        val state = HomeUiState(customers = listOf(customer()))

        val markers = state.markers()
        assertEquals(1, markers.size)
        assertEquals(MarkerType.CUSTOMER, markers[0].type)
        assertEquals("customer-c1", markers[0].id)
        assertEquals("c1", markers[0].data)
    }

    @Test
    fun `customer with active delivery produces ACTIVE marker`() {
        val state = HomeUiState(
            customers = listOf(customer(id = "c1")),
            activeCustomerIds = setOf("c1")
        )

        val markers = state.markers()
        assertEquals(1, markers.size)
        assertEquals(MarkerType.ACTIVE, markers[0].type)
    }

    @Test
    fun `customer not in active set stays CUSTOMER`() {
        val state = HomeUiState(
            customers = listOf(customer(id = "c1"), customer(id = "c2")),
            activeCustomerIds = setOf("c2")
        )

        val markers = state.markers()
        assertEquals(2, markers.size)
        val c1 = markers.first { it.id == "customer-c1" }
        val c2 = markers.first { it.id == "customer-c2" }
        assertEquals(MarkerType.CUSTOMER, c1.type)
        assertEquals(MarkerType.ACTIVE, c2.type)
    }

    @Test
    fun `driver marker appears first in list`() {
        val state = HomeUiState(
            customers = listOf(customer(id = "c1")),
            driverLocation = driverLoc()
        )

        val markers = state.markers()
        assertEquals(2, markers.size)
        assertEquals(MarkerType.DRIVER, markers[0].type)
        assertEquals(MarkerType.CUSTOMER, markers[1].type)
    }

    @Test
    fun `marker positions match customer coordinates`() {
        val state = HomeUiState(
            customers = listOf(customer(id = "c1", lat = 31.5, lng = 44.2))
        )

        val markers = state.markers()
        assertEquals(31.5, markers[0].position.latitude, 0.000001)
        assertEquals(44.2, markers[0].position.longitude, 0.000001)
    }

    @Test
    fun `many customers produce many markers in order`() {
        val customers = (1..5).map { customer(id = "c$it") }
        val state = HomeUiState(customers = customers)

        val markers = state.markers()
        assertEquals(5, markers.size)
        assertEquals("customer-c1", markers[0].id)
        assertEquals("customer-c5", markers[4].id)
    }

    @Test
    fun `active set with unknown id does not affect markers`() {
        val state = HomeUiState(
            customers = listOf(customer(id = "c1")),
            activeCustomerIds = setOf("c-ghost")  // not in customer list
        )

        val markers = state.markers()
        assertEquals(1, markers.size)
        assertEquals(MarkerType.CUSTOMER, markers[0].type)
    }

    @Test
    fun `driver marker has no data payload`() {
        val state = HomeUiState(driverLocation = driverLoc())

        val markers = state.markers()
        assertNull(markers[0].data)
    }

    @Test
    fun `customer marker carries customerId as data`() {
        val state = HomeUiState(customers = listOf(customer(id = "abc-123")))

        val markers = state.markers()
        assertNotNull(markers[0].data)
        assertEquals("abc-123", markers[0].data)
    }
}
