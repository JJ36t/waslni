package com.waslni.driver.core.monitoring

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight crash + error reporter.
 *
 * In production, this delegates to Firebase Crashlytics (or Sentry).
 * In development, it logs to Logcat.
 *
 * The abstraction lets us swap reporting backends without touching the
 * rest of the app. Call sites use [reportException] / [logEvent] and
 * don't know (or care) where the data ends up.
 *
 * Usage:
 *   @Inject lateinit var crashReporter: CrashReporter
 *   crashReporter.reportException(e, "Failed to sync customer")
 *   crashReporter.logEvent("delivery_completed", mapOf("delivery_id" to id))
 *
 * Phase 28 will wire this to Firebase Crashlytics.
 */
@Singleton
class CrashReporter @Inject constructor() {

    fun reportException(
        exception: Throwable,
        message: String? = null,
        customKeys: Map<String, String> = emptyMap()
    ) {
        if (message != null) {
            Log.e(TAG, message, exception)
        } else {
            Log.e(TAG, "Exception reported", exception)
        }
        customKeys.forEach { (key, value) ->
            Log.d(TAG, "customKey: $key=$value")
        }
        // TODO Phase 28: FirebaseCrashlytics.getInstance().recordException(exception)
    }

    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        val paramString = if (params.isEmpty()) "" else " $params"
        Log.i(TAG, "event: $eventName$paramString")
        // TODO Phase 28: FirebaseCrashlytics.getInstance().log("$eventName $params")
    }

    fun setUserId(userId: String) {
        Log.i(TAG, "user_id set: $userId")
        // TODO Phase 28: FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    fun clearUserId() {
        Log.i(TAG, "user_id cleared")
        // TODO Phase 28: FirebaseCrashlytics.getInstance().setUserId("")
    }

    companion object {
        private const val TAG = "WaselniCrash"
    }
}
