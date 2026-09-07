package com.waslni.driver.data.remote.api

import com.waslni.driver.data.remote.dto.DeliveryCreateDto
import com.waslni.driver.data.remote.dto.DeliveryResponseDto
import com.waslni.driver.data.remote.dto.DeliveryStatusUpdateDto
import com.waslni.driver.data.remote.dto.PaginatedResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DeliveryApi {

    @GET("deliveries")
    suspend fun listDeliveries(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null
    ): Response<PaginatedResponse<DeliveryResponseDto>>

    @POST("deliveries")
    suspend fun createDelivery(@Body body: DeliveryCreateDto): Response<DeliveryResponseDto>

    @GET("deliveries/{id}")
    suspend fun getDelivery(@Path("id") id: String): Response<DeliveryResponseDto>

    @PATCH("deliveries/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body body: DeliveryStatusUpdateDto,
        @Header("Idempotency-Key") idempotencyKey: String
    ): Response<DeliveryResponseDto>
}
