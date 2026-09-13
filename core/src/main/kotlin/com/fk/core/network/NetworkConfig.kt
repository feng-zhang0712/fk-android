package com.fk.core.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Configuration for [OkHttpApiClient].
 *
 * @property baseUrl Optional base used to resolve relative [com.fk.core.pluggable.networking.ApiRequest.url] values.
 * @property connectTimeoutMs TCP connect timeout.
 * @property readTimeoutMs Response body read timeout.
 * @property writeTimeoutMs Request body write timeout.
 * @property callTimeoutMs Overall call timeout (`0` = none).
 * @property defaultHeaders Headers merged into every request (request headers win on conflict).
 * @property enableHttpLogging When true, attaches an OkHttp [HttpLoggingInterceptor] at BASIC level.
 */
data class NetworkConfig(
  val baseUrl: String? = null,
  val connectTimeoutMs: Long = 10_000,
  val readTimeoutMs: Long = 30_000,
  val writeTimeoutMs: Long = 30_000,
  val callTimeoutMs: Long = 0,
  val defaultHeaders: Map<String, String> = emptyMap(),
  val enableHttpLogging: Boolean = false,
) {
  /**
   * Builds a configured [OkHttpClient].
   *
   * Pass [additionalInterceptors] for auth, tracing, etc. (OkHttp interceptor chain).
   */
  fun buildClient(
    additionalInterceptors: List<okhttp3.Interceptor> = emptyList(),
  ): OkHttpClient {
    val builder = OkHttpClient.Builder()
      .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
      .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
      .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
      .callTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)

    additionalInterceptors.forEach { builder.addInterceptor(it) }

    if (enableHttpLogging) {
      builder.addInterceptor(
        HttpLoggingInterceptor().apply {
          level = HttpLoggingInterceptor.Level.BASIC
        },
      )
    }

    return builder.build()
  }
}
