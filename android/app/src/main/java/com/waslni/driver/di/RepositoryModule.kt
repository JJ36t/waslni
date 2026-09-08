package com.waslni.driver.di

import com.waslni.driver.data.repository.AuthRepositoryImpl
import com.waslni.driver.data.repository.CustomerRepositoryImpl
import com.waslni.driver.data.repository.DeliveryRepositoryImpl
import com.waslni.driver.data.repository.SyncRepositoryImpl
import com.waslni.driver.domain.repository.AuthRepository
import com.waslni.driver.domain.repository.CustomerRepository
import com.waslni.driver.domain.repository.DeliveryRepository
import com.waslni.driver.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository interfaces to their implementations.
 *
 * Using @Binds instead of @Provides because we're just mapping an interface
 * to an impl — Hilt generates the boilerplate.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryRepository(impl: DeliveryRepositoryImpl): DeliveryRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
