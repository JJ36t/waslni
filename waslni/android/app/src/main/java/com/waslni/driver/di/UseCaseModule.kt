package com.waslni.driver.di

import com.waslni.driver.core.location.LocationProvider
import com.waslni.driver.domain.repository.AuthRepository
import com.waslni.driver.domain.repository.DeliveryRepository
import com.waslni.driver.domain.usecase.auth.HasSessionUseCase
import com.waslni.driver.domain.usecase.auth.LoginUseCase
import com.waslni.driver.domain.usecase.auth.LogoutUseCase
import com.waslni.driver.domain.usecase.auth.VerifySessionUseCase
import com.waslni.driver.domain.usecase.customer.AddCustomerUseCase
import com.waslni.driver.domain.usecase.customer.CheckDuplicatePhoneUseCase
import com.waslni.driver.domain.usecase.customer.DeleteCustomerUseCase
import com.waslni.driver.domain.usecase.customer.GetCustomerUseCase
import com.waslni.driver.domain.usecase.customer.ObserveCustomersUseCase
import com.waslni.driver.domain.usecase.customer.SearchCustomersUseCase
import com.waslni.driver.domain.usecase.customer.UpdateCustomerLocationUseCase
import com.waslni.driver.domain.usecase.customer.UpdateCustomerUseCase
import com.waslni.driver.domain.usecase.delivery.ObserveActiveDeliveryCustomerIdsUseCase
import com.waslni.driver.domain.usecase.location.GetCurrentLocationUseCase
import com.waslni.driver.domain.usecase.sync.ObservePendingSyncCountUseCase
import com.waslni.driver.domain.usecase.sync.ScheduleSyncUseCase
import com.waslni.driver.data.sync.SyncScheduler
import com.waslni.driver.domain.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides use cases.
 *
 * Use cases are stateless and cheap to construct, so we don't strictly need
 * to scope them as singletons. We provide them explicitly so view models
 * can @Inject them by class instead of using @Inject constructor on every
 * use case (which would also work — this is just a style preference).
 *
 * As the project grows, this module will be split into per-feature modules
 * (CustomerUseCaseModule, DeliveryUseCaseModule, etc.) for clarity.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides fun provideAddCustomerUseCase(
        repo: com.waslni.driver.domain.repository.CustomerRepository
    ) = AddCustomerUseCase(repo)

    @Provides fun provideUpdateCustomerUseCase(
        repo: com.waslni.driver.domain.repository.CustomerRepository
    ) = UpdateCustomerUseCase(repo)

    @Provides fun provideUpdateCustomerLocationUseCase(
        repo: com.waslni.driver.domain.repository.CustomerRepository
    ) = UpdateCustomerLocationUseCase(repo)

    @Provides fun provideDeleteCustomerUseCase(
        repo: com.waslni.driver.domain.repository.CustomerRepository
    ) = DeleteCustomerUseCase(repo)

    @Provides fun provideObserveCustomersUseCase(
        repo: com.waslni.driver.domain.repository.CustomerRepository
    ) = ObserveCustomersUseCase(repo)

    @Provides fun provideSearchCustomersUseCase(
        repo: com.waslni.driver.domain.repository.CustomerRepository
    ) = SearchCustomersUseCase(repo)

    @Provides fun provideGetCustomerUseCase(
        repo: com.waslni.driver.domain.repository.CustomerRepository
    ) = GetCustomerUseCase(repo)

    @Provides fun provideCheckDuplicatePhoneUseCase(
        repo: com.waslni.driver.domain.repository.CustomerRepository
    ) = CheckDuplicatePhoneUseCase(repo)

    @Provides fun provideGetCurrentLocationUseCase(
        provider: LocationProvider
    ) = GetCurrentLocationUseCase(provider)

    @Provides fun provideObserveActiveDeliveryCustomerIdsUseCase(
        repo: DeliveryRepository
    ) = ObserveActiveDeliveryCustomerIdsUseCase(repo)

    // === Auth ===
    @Provides fun provideLoginUseCase(repo: AuthRepository) = LoginUseCase(repo)

    @Provides fun provideLogoutUseCase(repo: AuthRepository) = LogoutUseCase(repo)

    @Provides fun provideHasSessionUseCase(repo: AuthRepository) = HasSessionUseCase(repo)

    @Provides fun provideVerifySessionUseCase(repo: AuthRepository) = VerifySessionUseCase(repo)

    // === Sync ===
    @Provides fun provideScheduleSyncUseCase(scheduler: SyncScheduler) =
        ScheduleSyncUseCase(scheduler)

    @Provides fun provideObservePendingSyncCountUseCase(repo: SyncRepository) =
        ObservePendingSyncCountUseCase(repo)
}
