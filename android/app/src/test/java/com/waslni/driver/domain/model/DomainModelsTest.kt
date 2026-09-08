package com.waslni.driver.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerTest {

    @Test
    fun valid_customer_creates_successfully() {
        val c = Customer(
            id = "x",
            name = "محمد أحمد",
            phone = "07801234567",
            latitude = 31.978942,
            longitude = 44.940127,
            accuracy = 4.2f
        )
        assertEquals("محمد أحمد", c.name)
        assertEquals(31.978942, c.latitude, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blank_name_throws() {
        Customer(name = "", phone = "07801234567", latitude = 0.0, longitude = 0.0, accuracy = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun too_short_name_throws() {
        Customer(name = "م", phone = "07801234567", latitude = 0.0, longitude = 0.0, accuracy = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun too_long_name_throws() {
        Customer(
            name = "x".repeat(121),
            phone = "07801234567",
            latitude = 0.0,
            longitude = 0.0,
            accuracy = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun too_short_phone_throws() {
        Customer(name = "محمد", phone = "123", latitude = 0.0, longitude = 0.0, accuracy = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun latitude_above_90_throws() {
        Customer(name = "محمد", phone = "07801234567", latitude = 91.0, longitude = 0.0, accuracy = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun longitude_below_minus_180_throws() {
        Customer(name = "محمد", phone = "07801234567", latitude = 0.0, longitude = -181.0, accuracy = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negative_accuracy_throws() {
        Customer(name = "محمد", phone = "07801234567", latitude = 0.0, longitude = 0.0, accuracy = -1f)
    }

    @Test
    fun null_accuracy_is_allowed() {
        val c = Customer(name = "محمد", phone = "07801234567", latitude = 0.0, longitude = 0.0, accuracy = null)
        assertNull(c.accuracy)
    }

    @Test
    fun toLatLng_returns_correct_pair() {
        val c = Customer(name = "محمد", phone = "07801234567", latitude = 31.5, longitude = 44.2, accuracy = null)
        val ll = c.toLatLng()
        assertEquals(31.5, ll.latitude, 0.0)
        assertEquals(44.2, ll.longitude, 0.0)
    }
}

class DeliveryStatusTest {

    @Test
    fun delivered_and_cancelled_are_terminal() {
        assertTrue(DeliveryStatus.DELIVERED.isTerminal)
        assertTrue(DeliveryStatus.CANCELLED.isTerminal)
    }

    @Test
    fun pending_is_not_terminal() {
        assertFalse(DeliveryStatus.PENDING.isTerminal)
    }

    @Test
    fun on_the_way_and_arrived_are_active() {
        assertTrue(DeliveryStatus.ON_THE_WAY.isActive)
        assertTrue(DeliveryStatus.ARRIVED.isActive)
    }

    @Test
    fun pending_is_not_active() {
        assertFalse(DeliveryStatus.PENDING.isActive)
    }

    @Test
    fun pending_can_transition_to_assigned_or_cancelled() {
        val allowed = DeliveryStatus.PENDING.allowedNextStates()
        assertTrue(allowed.contains(DeliveryStatus.ASSIGNED))
        assertTrue(allowed.contains(DeliveryStatus.CANCELLED))
    }

    @Test
    fun delivered_cannot_transition_anywhere() {
        assertTrue(DeliveryStatus.DELIVERED.allowedNextStates().isEmpty())
        assertFalse(DeliveryStatus.DELIVERED.canTransitionTo(DeliveryStatus.CANCELLED))
    }

    @Test
    fun cannot_skip_states_pending_to_delivered() {
        assertFalse(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.DELIVERED))
    }

    @Test
    fun can_transition_on_the_way_to_arrived() {
        assertTrue(DeliveryStatus.ON_THE_WAY.canTransitionTo(DeliveryStatus.ARRIVED))
    }

    @Test
    fun can_transition_arrived_to_delivered() {
        assertTrue(DeliveryStatus.ARRIVED.canTransitionTo(DeliveryStatus.DELIVERED))
    }

    @Test
    fun fromString_parses_case_insensitively() {
        assertEquals(DeliveryStatus.ON_THE_WAY, DeliveryStatus.fromString("on_the_way"))
        assertEquals(DeliveryStatus.ARRIVED, DeliveryStatus.fromString("ARRIVED"))
    }
}

class LatLngTest {

    @Test
    fun distanceTo_returns_zero_for_same_point() {
        val p = LatLng(31.978942, 44.940127)
        assertEquals(0.0, p.distanceTo(p), 0.001)
    }

    @Test
    fun distanceTo_returns_positive_for_different_points() {
        val a = LatLng(31.978942, 44.940127)
        val b = LatLng(31.979000, 44.940200)
        val d = a.distanceTo(b)
        assertTrue("Distance should be > 0, was $d", d > 0)
        assertTrue("Distance should be < 20m, was $d", d < 20)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalid_latitude_throws() {
        LatLng(100.0, 0.0)
    }
}

class LocationResultTest {

    @Test
    fun acceptable_accuracy_returns_true_when_within_threshold() {
        val r = LocationResult(0.0, 0.0, accuracy = 5f)
        assertTrue(r.isAcceptable(thresholdMeters = 10f))
    }

    @Test
    fun acceptable_accuracy_returns_false_when_exceeds_threshold() {
        val r = LocationResult(0.0, 0.0, accuracy = 70f)
        assertFalse(r.isAcceptable(thresholdMeters = 10f))
    }

    @Test
    fun default_threshold_is_10m() {
        val r = LocationResult(0.0, 0.0, accuracy = 9f)
        assertTrue(r.isAcceptable())
    }

    @Test(expected = IllegalArgumentException::class)
    fun negative_accuracy_throws() {
        LocationResult(0.0, 0.0, accuracy = -1f)
    }
}
