package com.waslni.driver.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity state.
 *
 * Exposes:
 *   - [isOnline]: StateFlow<Boolean> — true when connected to any network
 *     with internet capability.
 *   - [observe()]: Flow<NetworkState> for fine-grained monitoring.
 *
 * Used by the UI to show "🟢 Online" / "🟠 Offline" indicator and to decide
 * whether to enable certain actions (e.g. live navigation requires online).
 *
 * Note: even when offline, the app works fully — all writes go to Room and
 * the sync queue drains when connectivity returns.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /**
     * Start monitoring network changes.
     *
     * Returns a Flow that emits the current network state and continues
     * emitting on every change. The flow completes when the caller stops
     * collecting (which unregisters the callback).
     */
    fun observe(): Flow<NetworkState> = callbackFlow {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                trySend(NetworkState.ONLINE)
            }

            override fun onLost(network: Network) {
                _isOnline.value = checkCurrentConnectivity()
                if (!_isOnline.value) {
                    trySend(NetworkState.OFFLINE)
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                _isOnline.value = hasInternet
                trySend(if (hasInternet) NetworkState.ONLINE else NetworkState.OFFLINE)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        manager.registerNetworkCallback(request, callback)

        // Emit the current state immediately
        trySend(if (_isOnline.value) NetworkState.ONLINE else NetworkState.OFFLINE)

        awaitClose {
            manager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Synchronous connectivity check — useful for one-shot queries.
     */
    fun isCurrentlyOnline(): Boolean = checkCurrentConnectivity()

    private fun checkCurrentConnectivity(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false

        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

enum class NetworkState { ONLINE, OFFLINE }
