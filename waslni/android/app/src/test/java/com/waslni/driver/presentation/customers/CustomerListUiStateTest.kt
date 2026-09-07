package com.waslni.driver.presentation.customers

import com.waslni.driver.domain.model.Customer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [CustomerListUiState] computed properties.
 *
 * Pure state — no flows, no Android.
 */
class CustomerListUiStateTest {

    private fun customer(id: String = "c1", name: String = "محمد") = Customer(
        id = id, name = name, phone = "07801234567",
        latitude = 31.0, longitude = 44.0, accuracy = 4f
    )

    @Test
    fun `isEmpty is true when no customers and no query`() {
        val state = CustomerListUiState(customers = emptyList(), query = "")
        assertTrue(state.isEmpty)
    }

    @Test
    fun `isEmpty is false when there are customers`() {
        val state = CustomerListUiState(customers = listOf(customer()), query = "")
        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty is false when query is non-blank`() {
        val state = CustomerListUiState(customers = emptyList(), query = "محمد")
        assertFalse(state.isEmpty)
    }

    @Test
    fun `isSearching is true when query is non-blank`() {
        val state = CustomerListUiState(query = "محمد")
        assertTrue(state.isSearching)
    }

    @Test
    fun `isSearching is false when query is blank`() {
        val state = CustomerListUiState(query = "")
        assertFalse(state.isSearching)
    }

    @Test
    fun `isNoResults is true when query non-blank and customers empty`() {
        val state = CustomerListUiState(customers = emptyList(), query = "xyz")
        assertTrue(state.isNoResults)
    }

    @Test
    fun `isNoResults is false when query non-blank and customers match`() {
        val state = CustomerListUiState(customers = listOf(customer()), query = "محمد")
        assertFalse(state.isNoResults)
    }

    @Test
    fun `isNoResults is false when query is blank`() {
        val state = CustomerListUiState(customers = emptyList(), query = "")
        assertFalse(state.isNoResults)
    }

    @Test
    fun `total starts at 0 by default`() {
        val state = CustomerListUiState()
        assertEquals(0, state.total)
    }
}
