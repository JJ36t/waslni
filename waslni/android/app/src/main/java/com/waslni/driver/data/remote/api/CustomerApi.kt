package com.waslni.driver.data.remote.api

import com.waslni.driver.data.remote.dto.CustomerCreateDto
import com.waslni.driver.data.remote.dto.CustomerResponseDto
import com.waslni.driver.data.remote.dto.CustomerUpdateDto
import com.waslni.driver.data.remote.dto.PaginatedResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CustomerApi {

    @GET("customers")
    suspend fun listCustomers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("search") search: String? = null
    ): Response<PaginatedResponse<CustomerResponseDto>>

    @POST("customers")
    suspend fun createCustomer(@Body body: CustomerCreateDto): Response<CustomerResponseDto>

    @GET("customers/{id}")
    suspend fun getCustomer(@Path("id") id: String): Response<CustomerResponseDto>

    @PATCH("customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: String,
        @Body body: CustomerUpdateDto
    ): Response<CustomerResponseDto>

    @DELETE("customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: String): Response<Unit>
}
