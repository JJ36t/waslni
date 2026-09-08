package com.waslni.driver.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [NavigationUpdate] computed properties + [NavigationInstruction.shortText].
 *
 * Pure functions — no Android, no SDK.
 */
class NavigationUpdateTest {

    @Test
    fun `formattedDistanceToManeuver shows m for < 1000m`() {
        val update = NavigationUpdate(
            currentInstruction = null,
            nextInstruction = null,
            distanceToNextManeuverMeters = 500.0,
            durationRemainingSeconds = 0,
            distanceRemainingMeters = 0.0,
            isArrived = false,
            isDeviating = false,
            isRerouting = false
        )
        assertEquals("500 m", update.formattedDistanceToManeuver)
    }

    @Test
    fun `formattedDistanceToManeuver shows km for >= 1000m`() {
        val update = NavigationUpdate(
            currentInstruction = null,
            nextInstruction = null,
            distanceToNextManeuverMeters = 1200.0,
            durationRemainingSeconds = 0,
            distanceRemainingMeters = 0.0,
            isArrived = false,
            isDeviating = false,
            isRerouting = false
        )
        assertEquals("1.2 km", update.formattedDistanceToManeuver)
    }

    @Test
    fun `formattedDurationRemaining shows min for < 60 min`() {
        val update = NavigationUpdate(
            currentInstruction = null,
            nextInstruction = null,
            distanceToNextManeuverMeters = 0.0,
            durationRemainingSeconds = 540,  // 9 min
            distanceRemainingMeters = 0.0,
            isArrived = false,
            isDeviating = false,
            isRerouting = false
        )
        assertEquals("9 min", update.formattedDurationRemaining)
    }

    @Test
    fun `formattedDurationRemaining shows hr + min for >= 60 min`() {
        val update = NavigationUpdate(
            currentInstruction = null,
            nextInstruction = null,
            distanceToNextManeuverMeters = 0.0,
            durationRemainingSeconds = 5400,  // 90 min
            distanceRemainingMeters = 0.0,
            isArrived = false,
            isDeviating = false,
            isRerouting = false
        )
        assertEquals("1 hr 30 min", update.formattedDurationRemaining)
    }

    @Test
    fun `formattedDurationRemaining min 1 for very short durations`() {
        val update = NavigationUpdate(
            currentInstruction = null,
            nextInstruction = null,
            distanceToNextManeuverMeters = 0.0,
            durationRemainingSeconds = 10,  // < 1 min → should show "1 min"
            distanceRemainingMeters = 0.0,
            isArrived = false,
            isDeviating = false,
            isRerouting = false
        )
        assertEquals("1 min", update.formattedDurationRemaining)
    }
}

class NavigationInstructionTest {

    @Test
    fun `shortText for depart returns Arabic`() {
        val instr = NavigationInstruction(
            text = "Head north",
            maneuverType = "depart",
            modifier = null,
            distanceMeters = 100.0,
            durationSeconds = 10
        )
        assertEquals("ابدأ القيادة", instr.shortText)
    }

    @Test
    fun `shortText for arrive returns Arabic`() {
        val instr = NavigationInstruction(
            text = "You have arrived",
            maneuverType = "arrive",
            modifier = null,
            distanceMeters = 0.0,
            durationSeconds = 0
        )
        assertEquals("وصلت إلى وجهتك", instr.shortText)
    }

    @Test
    fun `shortText for turn left returns Arabic`() {
        val instr = NavigationInstruction(
            text = "Turn left onto Main St",
            maneuverType = "turn",
            modifier = "left",
            distanceMeters = 200.0,
            durationSeconds = 15
        )
        assertEquals("انعطف يسارًا", instr.shortText)
    }

    @Test
    fun `shortText for turn right returns Arabic`() {
        val instr = NavigationInstruction(
            text = "Turn right",
            maneuverType = "turn",
            modifier = "right",
            distanceMeters = 150.0,
            durationSeconds = 12
        )
        assertEquals("انعطف يمينًا", instr.shortText)
    }

    @Test
    fun `shortText for uturn returns Arabic`() {
        val instr = NavigationInstruction(
            text = "Make a U-turn",
            maneuverType = "turn",
            modifier = "uturn",
            distanceMeters = 100.0,
            durationSeconds = 20
        )
        assertEquals("انعطف للخلف", instr.shortText)
    }

    @Test
    fun `shortText for continue returns Arabic`() {
        val instr = NavigationInstruction(
            text = "Continue straight",
            maneuverType = "continue",
            modifier = null,
            distanceMeters = 500.0,
            durationSeconds = 30
        )
        assertEquals("تابع بشكل مستقيم", instr.shortText)
    }

    @Test
    fun `shortText for merge returns Arabic`() {
        val instr = NavigationInstruction(
            text = "Merge onto highway",
            maneuverType = "merge",
            modifier = null,
            distanceMeters = 300.0,
            durationSeconds = 25
        )
        assertEquals("ادمج مع الطريق", instr.shortText)
    }

    @Test
    fun `shortText for roundabout returns Arabic`() {
        val instr = NavigationInstruction(
            text = "Enter roundabout",
            maneuverType = "roundabout",
            modifier = null,
            distanceMeters = 50.0,
            durationSeconds = 10
        )
        assertEquals("ادخل الدوّار", instr.shortText)
    }

    @Test
    fun `shortText for exit_roundabout returns Arabic`() {
        val instr = NavigationInstruction(
            text = "Exit roundabout",
            maneuverType = "exit_roundabout",
            modifier = null,
            distanceMeters = 30.0,
            durationSeconds = 5
        )
        assertEquals("اخرج من الدوّار", instr.shortText)
    }

    @Test
    fun `shortText for unknown maneuver falls back to text`() {
        val instr = NavigationInstruction(
            text = "Custom instruction",
            maneuverType = "custom_type",
            modifier = null,
            distanceMeters = 100.0,
            durationSeconds = 10
        )
        assertEquals("Custom instruction", instr.shortText)
    }
}
