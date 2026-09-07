package com.waslni.driver.core.maps

import android.content.Context
import com.waslni.driver.core.maps.mapbox.MapboxMapProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the maps subsystem.
 *
 * - Binds [MapboxMapProvider] as the singleton [MapProvider].
 *
 * When we add the routing/navigation engines in Phase 14/15 we'll add their
 * providers here too.
 */
@Module
@InstallIn(SingletonComponent::class)
object MapsModule {

    @Provides
    @Singleton
    fun provideMapProvider(
        @ApplicationContext context: Context
    ): MapProvider = MapboxMapProvider(context)
}
