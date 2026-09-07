package com.waslni.driver.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.waslni.driver.domain.model.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Production implementation of [LocationProvider] using Google Play Services'
 * [FusedLocationProviderClient].
 *
 * Why FusedLocationProviderClient over raw LocationManager:
 *   - Better battery: combines GPS, Wi-Fi, cell, accelerometer.
 *   - Single API across Android versions.
 *   - Handles Play Services availability transparently.
 *
 * Trade-off: requires Play Services on the device. Almost all modern Android
 * devices in our target market have it; if not, we'd add a fallback to
 * LocationManager in a separate implementation.
 */
class FusedLocationProvider(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient
) : LocationProvider {

    override fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    override fun isLocationEnabled(): Boolean {
        // Works on API 28+ via LocationManager.isLocationEnabled.
        // For older APIs, fall back to checking any provider.
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            // Permission was revoked between checks — treat as disabled.
            false
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(
        timeoutMillis: Long,
        accuracyThresholdMeters: Float,
        maxAgeMillis: Long
    ): LocationResult {
        // 1. Permission gate
        if (!hasLocationPermission()) {
            throw LocationPermissionException(permanentlyDenied = false)
        }

        // 2. GPS enabled gate
        if (!isLocationEnabled()) {
            throw GpsDisabledException()
        }

        // 3. Try last known location — instant and free.
        val lastKnown = getLastKnownLocationSafely()
        if (lastKnown != null && isFreshAndAccurate(lastKnown, maxAgeMillis, accuracyThresholdMeters)) {
            return lastKnown
        }

        // 4. Request a fresh fix with timeout.
        val fresh = withTimeoutOrNull(timeoutMillis) {
            requestSingleHighAccuracyFix(accuracyThresholdMeters)
        } ?: throw LocationTimeoutException(timeoutMillis)

        return fresh
    }

    @SuppressLint("MissingPermission")
    override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationResult> =
        callbackFlow {
            if (!hasLocationPermission()) {
                close(LocationPermissionException())
                return@callbackFlow
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setMinUpdateIntervalMillis(intervalMillis / 2)
                .setWaitForAccurateLocation(true)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: GmsLocationResult) {
                    result.lastLocation?.let { trySend(it.toDomain()) }
                }
            }

            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

            awaitClose {
                fusedClient.removeLocationUpdates(callback)
            }
        }

    // === Internals ===

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocationSafely(): LocationResult? =
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(location.toDomain())
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resume(null) // last-known is best-effort — fall through to fresh request
                }
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleHighAccuracyFix(
        accuracyThresholdMeters: Float
    ): LocationResult = suspendCancellableCoroutine { cont ->

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateDelayMillis(0L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: GmsLocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    val domain = loc.toDomain()
                    // Stop as soon as we have an acceptable fix.
                    if (domain.accuracy <= accuracyThresholdMeters) {
                        fusedClient.removeLocationUpdates(this)
                        cont.resume(domain)
                    }
                    // Otherwise keep listening — the withTimeoutOrNull wrapper
                    // will eventually expire.
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        cont.invokeOnCancellation {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    private fun isFreshAndAccurate(
        loc: LocationResult,
        maxAgeMillis: Long,
        accuracyThresholdMeters: Float
    ): Boolean {
        val age = System.currentTimeMillis() - loc.timestamp
        return age <= maxAgeMillis && loc.accuracy <= accuracyThresholdMeters
    }

    /**
     * Convert a Play Services [android.location.Location] to our domain
     * [LocationResult]. Pulled out as an extension so tests can reuse it.
     */
    private fun android.location.Location.toDomain(): LocationResult = LocationResult(
        latitude = latitude,
        longitude = longitude,
        accuracy = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
        timestamp = time
    )
}
