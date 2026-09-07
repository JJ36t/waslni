package com.waslni.driver.core.maps

import android.content.Context
import com.waslni.driver.core.maps.mapbox.MapboxMapProvider
import com.waslni.driver.core.maps.mapbox.MapboxRoutingEngine
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
 * - Binds [MapboxRoutingEngine] as the singleton [RoutingEngine].
 *
 * Phase 15 will add NavigationEngine.
 */
@Module
@InstallIn(SingletonComponent::class)
object MapsModule {

    @Provides
    @Singleton
    fun provideMapProvider(
        @ApplicationContext context: Context
    ): MapProvider = MapboxMapProvider(context)

    @Provides
    @Singleton
    fun provideRoutingEngine(
        @ApplicationContext context: Context
    ): RoutingEngine = MapboxRoutingEngine(context)
}
