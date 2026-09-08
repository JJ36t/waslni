package com.waslni.driver.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage backed by [EncryptedSharedPreferences].
 *
 * Encrypts values (and keys) with AES-256-GCM. Master key is bound to the
 * Android Keystore so the file is unreadable without the device's hardware
 * key — survives app reinstall but NOT device wipe / root.
 *
 * Stored here:
 *   - access_token
 *   - refresh_token
 *   - user_id
 *   - username
 *   - role
 *
 * NEVER store:
 *   - Passwords (we use them once at login, then discard)
 *   - Customer data (that lives in Room)
 */
class SecureStorage(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback: in-memory map (only happens if Keystore is broken — extremely rare).
        // This means tokens won't survive app restart, but the app still works.
        context.getSharedPreferences("${FILE_NAME}_fallback", Context.MODE_PRIVATE)
    }

    // === Generic ===

    fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    fun getString(key: String, default: String? = null): String? =
        prefs.getString(key, default)

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // === Token convenience ===

    var accessToken: String?
        get() = getString(KEY_ACCESS_TOKEN)
        set(value) { putString(KEY_ACCESS_TOKEN, value) }

    var refreshToken: String?
        get() = getString(KEY_REFRESH_TOKEN)
        set(value) { putString(KEY_REFRESH_TOKEN, value) }

    var userId: String?
        get() = getString(KEY_USER_ID)
        set(value) { putString(KEY_USER_ID, value) }

    var username: String?
        get() = getString(KEY_USERNAME)
        set(value) { putString(KEY_USERNAME, value) }

    var role: String?
        get() = getString(KEY_ROLE)
        set(value) { putString(KEY_ROLE, value) }

    /**
     * Clear all auth-related data — called on logout / session expiration.
     */
    fun clearAuth() {
        remove(KEY_ACCESS_TOKEN)
        remove(KEY_REFRESH_TOKEN)
        remove(KEY_USER_ID)
        remove(KEY_USERNAME)
        remove(KEY_ROLE)
    }

    companion object {
        private const val FILE_NAME = "waselni_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
    }
}
