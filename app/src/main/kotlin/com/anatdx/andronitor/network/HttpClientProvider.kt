package com.anatdx.andronitor.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object HttpClientProvider {
    private val sharedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun client(connectTimeoutMillis: Long, readTimeoutMillis: Long): OkHttpClient =
        sharedClient.newBuilder()
            .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
            .build()
}
