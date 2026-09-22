package com.fk.core.mapping

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * Mapping — kotlinx.serialization conventions and business envelope helpers.
 *
 * Conceptually aligned with iOS `FKCoreKit` ModelMapping; Android-shaped APIs.
 */
object Mapping {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"

  /** Default mapper using [MappingJson.Api]. */
  fun apiMapper(): JsonMapper = JsonMapper(MappingJson.Api)

  /** Lenient mapper for loosely typed API payloads. */
  fun lenientMapper(): JsonMapper = JsonMapper(MappingJson.LenientApi)

  /** Strict mapper that rejects unknown keys. */
  fun strictMapper(): JsonMapper = JsonMapper(MappingJson.Strict)
}

/**
 * Shared [Json] presets for API and DTO mapping.
 *
 * Distinct from [com.fk.core.pluggable.storage.PluggableJson] (storage-oriented).
 */
@OptIn(ExperimentalSerializationApi::class)
object MappingJson {
  /**
   * Typical REST API: snake_case keys, ignore unknowns, omit nulls on encode.
   */
  val Api: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = false
    explicitNulls = false
    namingStrategy = JsonNamingStrategy.SnakeCase
  }

  /**
   * Lenient REST API: same as [Api] plus [JsonBuilder.isLenient] for soft scalars.
   */
  val LenientApi: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    explicitNulls = false
    namingStrategy = JsonNamingStrategy.SnakeCase
  }

  /**
   * Strict JSON: unknown keys fail; nulls encoded explicitly; default property names.
   */
  val Strict: Json = Json {
    ignoreUnknownKeys = false
    encodeDefaults = true
    isLenient = false
    explicitNulls = true
  }
}
