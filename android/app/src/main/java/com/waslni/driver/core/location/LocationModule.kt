package com.waslni.driver.core.location

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the location subsystem.
 *
 * - Binds [FusedLocationProvider] as the singleton [LocationProvider].
 * - Provides [GpsSettingsHelper] for opening the system location settings
 *   page (used by the UI when [GpsDisabledException] is thrown).
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationProvider(
        @ApplicationContext context: Context
    ): LocationProvider = FusedLocationProvider(
        context = context,
        fusedClient = LocationServices.getFusedLocationProviderClient(context)
    )

    @Provides
    @Singleton
    fun provideGpsSettingsHelper(
        @ApplicationContext context: Context
    ): GpsSettingsHelper = GpsSettingsHelper(context)
}

/**
 * Helper to launch system screens related to location.
 *
 * Wrapped in a class (not called inline) so we can mock/fake it in tests
 * and so the calling code doesn't need a [Context] reference.
 */
class GpsSettingsHelper(private val context: Context) {

    /**
     * Open the system "Location" settings page where the user can toggle
     * GPS / location services.
     */
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Open the app's details page in system Settings, scrolled to the
     * Permissions section. Used when the user has permanently denied
     * location permission and we can no longer prompt them via the
     * runtime permission dialog.
     */
    fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
