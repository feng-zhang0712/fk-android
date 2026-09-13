package com.fk.core.mapping

import com.fk.core.pluggable.networking.ResponseInterceptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * [ResponseInterceptor] that unwraps a business envelope for HTTP 2xx responses.
 *
 * Non-2xx responses are returned unchanged so callers can inspect HTTP errors.
 * Business failures throw [MappingException.BusinessFailure].
 */
class EnvelopeResponseInterceptor(
  config: EnvelopeConfig = EnvelopeConfig.Standard,
  private val json: Json = MappingJson.Api,
) : ResponseInterceptor {
  private val processor = EnvelopeProcessor(config, json)

  override fun intercept(
    data: ByteArray,
    statusCode: Int?,
    headers: Map<String, String>,
  ): ByteArray {
    if (statusCode != null && statusCode !in 200..299) {
      return data
    }
    if (data.isEmpty()) return data
    val result = processor.process(data)
    return json.encodeToString(JsonElement.serializer(), result.payload)
      .toByteArray(Charsets.UTF_8)
  }
}
