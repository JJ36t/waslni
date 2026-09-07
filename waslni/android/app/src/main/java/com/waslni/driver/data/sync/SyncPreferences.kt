package com.waslni.driver.data.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferences
import androidx.datastore.preferences.core.stringPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed preferences for the sync subsystem.
 *
 * Stores:
 *   - `latest_sync_timestamp` (epoch millis) — the server-issued watermark
 *     for incremental sync. Null on first sync.
 *   - `last_sync_attempt_at` (epoch millis) — for the UI "last synced X ago" label.
 *   - `last_sync_success_at` (epoch millis) — for retry backoff decisions.
 *
 * We use DataStore (not EncryptedSharedPreferences) because these values are
 * NOT sensitive — they're just timestamps.
 */
private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "waselni_sync")

@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val store get() = context.syncDataStore

    /** The latest_sync_timestamp returned by the server (epoch millis), or null on first sync. */
    val latestSyncTimestamp: Flow<Long?> = store.data.map { it[LATEST_SYNC_KEY] }

    /** When the last sync attempt happened (success or failure). */
    val lastSyncAttemptAt: Flow<Long?> = store.data.map { it[LAST_ATTEMPT_KEY] }

    /** When the last successful sync happened. */
    val lastSyncSuccessAt: Flow<Long?> = store.data.map { it[LAST_SUCCESS_KEY] }

    /** Persist the new watermark after a successful sync. */
    suspend fun setLatestSyncTimestamp(timestampMillis: Long) {
        store.edit { it[LATEST_SYNC_KEY] = timestampMillis }
    }

    /** Record that a sync attempt just happened. */
    suspend fun setLastSyncAttemptAt(timestampMillis: Long) {
        store.edit { it[LAST_ATTEMPT_KEY] = timestampMillis }
    }

    /** Record that a sync just succeeded. */
    suspend fun setLastSyncSuccessAt(timestampMillis: Long) {
        store.edit { it[LAST_SUCCESS_KEY] = timestampMillis }
    }

    /** Clear all sync state — used on logout. */
    suspend fun clear() {
        store.edit { it.clear() }
    }

    companion object {
        private val LATEST_SYNC_KEY = longPreferences("latest_sync_timestamp")
        private val LAST_ATTEMPT_KEY = longPreferences("last_sync_attempt_at")
        private val LAST_SUCCESS_KEY = longPreferences("last_sync_success_at")
    }
}
