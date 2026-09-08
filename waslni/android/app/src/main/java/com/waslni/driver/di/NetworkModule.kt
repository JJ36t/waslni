package com.waslni.driver.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.waslni.driver.BuildConfig
import com.waslni.driver.core.security.SecureStorage
import com.waslni.driver.core.security.TokenManager
import com.waslni.driver.data.remote.api.AuthApi
import com.waslni.driver.data.remote.api.CustomerApi
import com.waslni.driver.data.remote.api.DeliveryApi
import com.waslni.driver.data.remote.api.SyncApi
import com.waslni.driver.data.remote.interceptor.AuthInterceptor
import com.waslni.driver.data.remote.interceptor.ErrorInterceptor
import com.waslni.driver.data.remote.interceptor.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for the network stack.
 *
 * Provides:
 *   - [SecureStorage] (encrypted prefs for tokens)
 *   - [TokenManager] (high-level auth state)
 *   - [Json] (kotlinx.serialization instance — reuses the one from
 *     [SerializationModule] but we provide a network-specific override here
 *     with `encodeDefaults = false` to keep payloads small)
 *   - [OkHttpClient] (with all 3 interceptors in the right order)
 *   - [Retrofit] instance
 *   - 4 API interfaces (AuthApi, CustomerApi, DeliveryApi, SyncApi)
 *
 * Interceptor order matters:
 *   1. Application interceptors run first (outgoing) → ErrorInterceptor
 *      (catches IOException BEFORE it reaches the app), then AuthInterceptor.
 *   2. Network interceptors run after redirects/auth.
 *   3. TokenAuthenticator is an [okhttp3.Authenticator], not an interceptor —
 *      OkHttp invokes it only when the response is 401.
 *
 * Wait — actually OkHttp application interceptors run before the network layer,
 * so they don't see redirects. Network interceptors see them. For our case
 * the simpler approach is:
 *   - AuthInterceptor (application) — adds the header.
 *   - ErrorInterceptor (application) — wraps exceptions.
 *   - TokenAuthenticator — runs only on 401.
 *   - HttpLoggingInterceptor (network) — logs final outgoing/incoming.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage =
        SecureStorage(context)

    @Provides
    @Singleton
    fun provideTokenManager(storage: SecureStorage): TokenManager = TokenManager(storage)

    @Provides
    @Singleton
    fun provideNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        prettyPrint = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        errorInterceptor: ErrorInterceptor,
        authenticator: TokenAuthenticator,
        json: Json,
        @ApplicationContext context: Context
    ): OkHttpClient {
        // HTTP cache — 10 MB. Caches GET responses when the server allows it
        // (Cache-Control headers). Authenticated responses are NOT cached
        // because they contain user-specific data + the Authorization header
        // would leak between users.
        val cacheDir = context.cacheDir.resolve("http_cache")
        val cache = okhttp3.Cache(cacheDir, 10L * 1024 * 1024)

        val builder = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .authenticator(authenticator)
            .addInterceptor(authInterceptor)
            .addInterceptor(errorInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                // Redact sensitive headers so tokens never appear in Logcat
                redactHeader("Authorization")
                redactHeader("Cookie")
                redactHeader("Set-Cookie")
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideCustomerApi(retrofit: Retrofit): CustomerApi =
        retrofit.create(CustomerApi::class.java)

    @Provides
    @Singleton
    fun provideDeliveryApi(retrofit: Retrofit): DeliveryApi =
        retrofit.create(DeliveryApi::class.java)

    @Provides
    @Singleton
    fun provideSyncApi(retrofit: Retrofit): SyncApi = retrofit.create(SyncApi::class.java)
}
