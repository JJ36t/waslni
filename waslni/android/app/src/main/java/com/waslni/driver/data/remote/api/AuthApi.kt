package com.waslni.driver.data.remote.api

import com.waslni.driver.data.remote.dto.LoginRequestDto
import com.waslni.driver.data.remote.dto.LogoutRequestDto
import com.waslni.driver.data.remote.dto.RefreshRequestDto
import com.waslni.driver.data.remote.dto.TokenResponseDto
import com.waslni.driver.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<TokenResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): Response<TokenResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto): Response<Unit>

    @GET("auth/me")
    suspend fun getMe(): Response<UserDto>
}
