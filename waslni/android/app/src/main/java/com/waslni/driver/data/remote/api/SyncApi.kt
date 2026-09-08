package com.waslni.driver.data.remote.api

import com.waslni.driver.data.remote.dto.SyncRequestDto
import com.waslni.driver.data.remote.dto.SyncResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SyncApi {

    @POST("sync")
    suspend fun sync(
        @Body body: SyncRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): Response<SyncResponseDto>
}
