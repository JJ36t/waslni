package com.waslni.driver.domain.usecase.customer

import com.waslni.driver.domain.model.Customer
import com.waslni.driver.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a reactive stream of all customers ordered by name.
 */
class ObserveCustomersUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    operator fun invoke(): Flow<List<Customer>> = repository.observeAll()
}
