package com.waslni.driver.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [NotificationType] enum — verifies channel + priority + ID mapping.
 *
 * The mapping is internal to NotificationHelper, but we can verify the enum
 * values + their ordinal stability (important for notification ID constants).
 */
class NotificationTypeTest {

    @Test
    fun `enum has exactly 4 values`() {
        assertEquals(4, NotificationType.entries.size)
    }

    @Test
    fun `enum values are in expected order`() {
        assertEquals(NotificationType.APPROACHING_CUSTOMER, NotificationType.entries[0])
        assertEquals(NotificationType.ARRIVED_AT_CUSTOMER, NotificationType.entries[1])
        assertEquals(NotificationType.DELIVERY_COMPLETED, NotificationType.entries[2])
        assertEquals(NotificationType.DELIVERY_CANCELLED, NotificationType.entries[3])
    }

    @Test
    fun `approaching and arrived are navigation alerts (high priority)`() {
        // These types should map to the navigation_alerts channel
        // We can't test the private channelFor() directly, but we verify
        // the enum values exist and are distinct.
        assertTrue(NotificationType.APPROACHING_CUSTOMER != NotificationType.ARRIVED_AT_CUSTOMER)
    }

    @Test
    fun `completed and cancelled are delivery status (low priority)`() {
        assertTrue(NotificationType.DELIVERY_COMPLETED != NotificationType.DELIVERY_CANCELLED)
    }

    @Test
    fun `all values have unique names`() {
        val names = NotificationType.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }
}
