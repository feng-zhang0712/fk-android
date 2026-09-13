package com.fk.core.mapping

import com.fk.core.pluggable.networking.ApiResponse
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

/**
 * Encode/decode helper over a configured [Json] instance.
 *
 * Prefer injecting this (or the underlying [Json]) into feature modules.
 */
class JsonMapper(
  val json: Json = MappingJson.Api,
  private val envelopeConfig: EnvelopeConfig = EnvelopeConfig.Standard,
) {
  private val envelopeProcessor = EnvelopeProcessor(envelopeConfig, json)

  /** Decodes [bytes] into [T]. */
  fun <T> decode(bytes: ByteArray, deserializer: DeserializationStrategy<T>): T {
    return try {
      json.decodeFromString(deserializer, bytes.toString(Charsets.UTF_8))
    } catch (e: MappingException) {
      throw e
    } catch (e: Exception) {
      throw MappingException.DecodingFailed(e)
    }
  }

  /** Decodes [ApiResponse.data] into [T]. */
  fun <T> decode(response: ApiResponse, deserializer: DeserializationStrategy<T>): T =
    decode(response.data, deserializer)

  /** Encodes [value] to UTF-8 JSON bytes. */
  fun <T> encodeToBytes(value: T, serializer: SerializationStrategy<T>): ByteArray {
    return try {
      json.encodeToString(serializer, value).toByteArray(Charsets.UTF_8)
    } catch (e: Exception) {
      throw MappingException.EncodingFailed(e)
    }
  }

  /** Encodes [value] to a JSON string. */
  fun <T> encodeToString(value: T, serializer: SerializationStrategy<T>): String {
    return try {
      json.encodeToString(serializer, value)
    } catch (e: Exception) {
      throw MappingException.EncodingFailed(e)
    }
  }

  /**
   * Unwraps a business envelope then decodes the payload as [T].
   *
   * JSON `null` payloads decode to `null` when [deserializer] is nullable;
   * non-nullable types surface [MappingException.DecodingFailed].
   */
  fun <T> decodeEnvelope(bytes: ByteArray, deserializer: DeserializationStrategy<T>): T {
    val result = envelopeProcessor.process(bytes)
    return try {
      json.decodeFromJsonElement(deserializer, result.payload)
    } catch (e: MappingException) {
      throw e
    } catch (e: Exception) {
      throw MappingException.DecodingFailed(e)
    }
  }

  /** Unwraps [ApiResponse.data] as an envelope then decodes the payload. */
  fun <T> decodeEnvelope(
    response: ApiResponse,
    deserializer: DeserializationStrategy<T>,
  ): T = decodeEnvelope(response.data, deserializer)

  /**
   * Returns a copy that uses [config] for envelope helpers.
   *
   * The underlying [json] instance is reused.
   */
  fun withEnvelope(config: EnvelopeConfig): JsonMapper =
    JsonMapper(json = json, envelopeConfig = config)
}
