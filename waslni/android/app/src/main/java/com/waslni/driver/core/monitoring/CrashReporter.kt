package com.waslni.driver.core.monitoring

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash + error reporter backed by Firebase Crashlytics.
 *
 * In production: delegates to FirebaseCrashlytics.
 * In development: also logs to Logcat for easy debugging.
 *
 * Usage:
 *   crashReporter.reportException(e, "Failed to sync customer")
 *   crashReporter.logEvent("delivery_completed")
 */
@Singleton
class CrashReporter @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics? = try {
        FirebaseCrashlytics.getInstance()
    } catch (e: Exception) {
        null  // Firebase not initialized (unit tests without google-services.json)
    }

    init {
        crashlytics?.isCrashlyticsCollectionEnabled = true
    }

    fun reportException(
        exception: Throwable,
        message: String? = null,
        customKeys: Map<String, String> = emptyMap()
    ) {
        if (message != null) {
            Log.e(TAG, message, exception)
            crashlytics?.log(message)
        } else {
            Log.e(TAG, "Exception reported", exception)
        }

        customKeys.forEach { (key, value) ->
            Log.d(TAG, "customKey: $key=$value")
            crashlytics?.setCustomKey(key, value)
        }

        crashlytics?.recordException(exception)
    }

    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        val paramString = if (params.isEmpty()) "" else " $params"
        Log.i(TAG, "event: $eventName$paramString")
        crashlytics?.log("event: $eventName$paramString")
    }

    fun setUserId(userId: String) {
        Log.i(TAG, "user_id set: $userId")
        crashlytics?.setUserId(userId)
    }

    fun clearUserId() {
        Log.i(TAG, "user_id cleared")
        crashlytics?.setUserId("")
    }

    companion object {
        private const val TAG = "WaselniCrash"
    }
}
