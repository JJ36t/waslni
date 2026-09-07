package com.waslni.driver.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [computeAccuracyTier].
 *
 * The function is pure (no Compose dependencies), so we can test it directly
 * without Robolectric. The visual rendering is verified manually on-device.
 *
 * Bucket boundaries (with default threshold 10m):
 *   - 0..5   → EXCELLENT
 *   - 5..10  → GOOD
 *   - >10    → POOR
 */
class AccuracyIndicatorTest {

    @Test
    fun `accuracy of 0 returns EXCELLENT`() {
        assertEquals(AccuracyTier.EXCELLENT, computeAccuracyTier(0f, thresholdMeters = 10f))
    }

    @Test
    fun `accuracy of 5 returns EXCELLENT`() {
        assertEquals(AccuracyTier.EXCELLENT, computeAccuracyTier(5f, thresholdMeters = 10f))
    }

    @Test
    fun `accuracy just above 5 returns GOOD`() {
        assertEquals(AccuracyTier.GOOD, computeAccuracyTier(5.1f, thresholdMeters = 10f))
    }

    @Test
    fun `accuracy at exact threshold returns GOOD`() {
        assertEquals(AccuracyTier.GOOD, computeAccuracyTier(10f, thresholdMeters = 10f))
    }

    @Test
    fun `accuracy just above threshold returns POOR`() {
        assertEquals(AccuracyTier.POOR, computeAccuracyTier(10.1f, thresholdMeters = 10f))
    }

    @Test
    fun `accuracy of 70 returns POOR`() {
        assertEquals(AccuracyTier.POOR, computeAccuracyTier(70f, thresholdMeters = 10f))
    }

    @Test
    fun `lowering threshold reclassifies previous GOOD as POOR`() {
        // At default threshold 10m: accuracy=8 is GOOD
        assertEquals(AccuracyTier.GOOD, computeAccuracyTier(8f, thresholdMeters = 10f))
        // Lower threshold to 5m: accuracy=8 becomes POOR
        assertEquals(AccuracyTier.POOR, computeAccuracyTier(8f, thresholdMeters = 5f))
    }

    @Test
    fun `raising threshold keeps more readings acceptable`() {
        // At default threshold 10m: accuracy=15 is POOR
        assertEquals(AccuracyTier.POOR, computeAccuracyTier(15f, thresholdMeters = 10f))
        // Raise threshold to 20m: accuracy=15 becomes GOOD
        assertEquals(AccuracyTier.GOOD, computeAccuracyTier(15f, thresholdMeters = 20f))
    }

    @Test
    fun `EXCELLENT tier is independent of threshold`() {
        // 3m is always EXCELLENT regardless of threshold
        assertEquals(AccuracyTier.EXCELLENT, computeAccuracyTier(3f, thresholdMeters = 5f))
        assertEquals(AccuracyTier.EXCELLENT, computeAccuracyTier(3f, thresholdMeters = 10f))
        assertEquals(AccuracyTier.EXCELLENT, computeAccuracyTier(3f, thresholdMeters = 50f))
    }
}
