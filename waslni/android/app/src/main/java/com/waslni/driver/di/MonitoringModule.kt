package com.waslni.driver.di

import com.waslni.driver.core.monitoring.CrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for monitoring components.
 *
 * Phase 28 will replace this with Firebase Crashlytics bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object MonitoringModule {

    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter = CrashReporter()
}
