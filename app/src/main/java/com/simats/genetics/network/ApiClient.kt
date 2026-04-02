package com.simats.genetics.network

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // MUST end with /api/
    const val BASE_URL = "http:180.235.121.245:8008"

    private var retrofit: Retrofit? = null

    private fun createClient(context: Context): OkHttpClient {

        // Logging (for debugging)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Token Interceptor
        val authInterceptor = Interceptor { chain ->

            val original: Request = chain.request()
            val token = TokenManager.getToken(context)

            Log.d("AUTH_DEBUG", "URL: ${original.url}")
            Log.d("AUTH_DEBUG", "Token: $token")

            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")

            // Add token only if exists
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Token $token")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    fun getApi(context: Context): ApiService {

        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createClient(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }
}