package com.waslni.driver.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides the application-wide kotlinx.serialization [Json] instance.
 *
 * Configuration:
 *   - ignoreUnknownKeys: backend may add fields; we don't want to break.
 *   - explicitNulls = false: omit null fields from outgoing JSON.
 *   - encodeDefaults = true: include default-valued fields (useful for sync payloads).
 */
@Module
@InstallIn(SingletonComponent::class)
object SerializationModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = false
    }
}
