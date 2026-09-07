package com.waslni.driver.presentation.customers

import com.waslni.driver.core.location.GpsDisabledException
import com.waslni.driver.core.location.LocationPermissionException
import com.waslni.driver.core.location.LocationTimeoutException
import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.model.LocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for the validation + error-mapping helpers used by Add/Edit screens.
 *
 * Pure functions — no Android, no coroutines — so they run as plain JUnit.
 */
class ValidationHelpersTest {

    // === validateName ===

    @Test
    fun `validateName returns null for valid name`() {
        assertNull(validateName("محمد أحمد"))
        assertNull(validateName("ab"))
        assertNull(validateName("a".repeat(120)))
    }

    @Test
    fun `validateName returns error for empty input`() {
        assertEquals("الاسم مطلوب", validateName(""))
        assertEquals("الاسم مطلوب", validateName("   "))
    }

    @Test
    fun `validateName returns error for too-short input`() {
        assertEquals("الاسم قصير جدًا", validateName("a"))
    }

    @Test
    fun `validateName returns error for too-long input`() {
        assertEquals("الاسم طويل جدًا", validateName("a".repeat(121)))
    }

    // === validatePhone ===

    @Test
    fun `validatePhone returns null for valid phone`() {
        assertNull(validatePhone("07801234567"))
        assertNull(validatePhone("+9647801234567"))
        assertNull(validatePhone("1234567"))
        assertNull(validatePhone("123456789012345678901234567890"))  // 30 chars
    }

    @Test
    fun `validatePhone returns error for empty input`() {
        assertEquals("رقم الموبايل مطلوب", validatePhone(""))
    }

    @Test
    fun `validatePhone returns error for too-short input`() {
        assertEquals("الرقم قصير جدًا", validatePhone("123456"))
    }

    @Test
    fun `validatePhone returns error for too-long input`() {
        assertEquals("الرقم طويل جدًا", validatePhone("1234567890123456789012345678901"))  // 31 chars
    }

    @Test
    fun `validatePhone returns error for non-digit non-plus characters`() {
        assertEquals("الرقم يجب أن يحتوي أرقامًا فقط", validatePhone("0780-1234567"))
        assertEquals("الرقم يجب أن يحتوي أرقامًا فقط", validatePhone("0780 1234"))
        assertEquals("الرقم يجب أن يحتوي أرقامًا فقط", validatePhone("abc12345"))
    }

    // === Throwable.toUserMessage ===

    @Test
    fun `toUserMessage maps permission exception`() {
        val msg = LocationPermissionException(permanentlyDenied = false).toUserMessage()
        assertEquals("تم رفض صلاحية الموقع", msg)
    }

    @Test
    fun `toUserMessage maps permanently-denied permission exception`() {
        val msg = LocationPermissionException(permanentlyDenied = true).toUserMessage()
        assertEquals("تم رفض صلاحية الموقع بشكل دائم. افتح الإعدادات لمنحها.", msg)
    }

    @Test
    fun `toUserMessage maps gps disabled exception`() {
        val msg = GpsDisabledException().toUserMessage()
        assertEquals("الـGPS معطل. فعّل خدمات الموقع من الإعدادات.", msg)
    }

    @Test
    fun `toUserMessage maps timeout exception`() {
        val msg = LocationTimeoutException(timeoutMillis = 10_000L).toUserMessage()
        assertEquals("تعذر تحديد الموقع. تأكد من أنك في مكان مفتوح.", msg)
    }

    @Test
    fun `toUserMessage falls back to throwable message`() {
        val msg = RuntimeException("custom error").toUserMessage()
        assertEquals("custom error", msg)
    }

    @Test
    fun `toUserMessage falls back to generic message when throwable message is null`() {
        val msg = object : Throwable() {}.toUserMessage()
        assertEquals("حدث خطأ غير متوقع", msg)
    }
}

/**
 * Tests for [AddCustomerUiState] computed properties.
 */
class AddCustomerUiStateTest {

    private fun location() = LocationResult(31.0, 44.0, accuracy = 4f, timestamp = 1L)

    @Test
    fun `canSave is false when name is too short`() {
        val state = AddCustomerUiState(
            name = "a",
            phone = "07801234567",
            location = location()
        )
        assertEquals(false, state.canSave)
    }

    @Test
    fun `canSave is false when phone is too short`() {
        val state = AddCustomerUiState(
            name = "محمد",
            phone = "123",
            location = location()
        )
        assertEquals(false, state.canSave)
    }

    @Test
    fun `canSave is false when location is null`() {
        val state = AddCustomerUiState(
            name = "محمد",
            phone = "07801234567",
            location = null
        )
        assertEquals(false, state.canSave)
    }

    @Test
    fun `canSave is false when isSaving`() {
        val state = AddCustomerUiState(
            name = "محمد",
            phone = "07801234567",
            location = location(),
            isSaving = true
        )
        assertEquals(false, state.canSave)
    }

    @Test
    fun `canSave is false when isCapturingLocation`() {
        val state = AddCustomerUiState(
            name = "محمد",
            phone = "07801234567",
            location = location(),
            isCapturingLocation = true
        )
        assertEquals(false, state.canSave)
    }

    @Test
    fun `canSave is true when all fields valid and not saving or capturing`() {
        val state = AddCustomerUiState(
            name = "محمد أحمد",
            phone = "07801234567",
            location = location()
        )
        assertEquals(true, state.canSave)
    }
}
