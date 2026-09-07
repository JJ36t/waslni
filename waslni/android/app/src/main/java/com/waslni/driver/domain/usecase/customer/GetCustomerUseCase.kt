package com.waslni.driver.domain.usecase.customer

import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCustomerUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    /** Reactive stream for UI consumption. */
    operator fun invoke(customerId: String): Flow<Customer?> =
        repository.observeById(customerId)

    /** One-shot fetch for use cases that don't need reactivity. */
    suspend fun getOnce(customerId: String): Customer? =
        repository.getCustomer(customerId)
}
