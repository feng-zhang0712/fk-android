package com.fk.core.mapping

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Unwraps common API envelopes and validates business success.
 */
class EnvelopeProcessor(
  private val config: EnvelopeConfig = EnvelopeConfig.Standard,
  private val json: Json = MappingJson.Api,
) {
  /**
   * Parses [bytes] as a JSON object, validates success, and returns the payload element.
   */
  fun process(bytes: ByteArray): EnvelopeResult {
    val root = parseObject(bytes)
    val businessCode = root[config.codeKey]?.asIntOrNull()
    val businessMessage = root[config.messageKey]?.asStringOrNull()

    validateSuccess(root, businessCode, businessMessage)

    val path = config.effectivePayloadPath
    val payload = resolvePath(root, path)
      ?: throw MappingException.KeyNotFound(path)

    return EnvelopeResult(
      payload = payload,
      businessCode = businessCode,
      businessMessage = businessMessage,
    )
  }

  private fun validateSuccess(
    root: JsonObject,
    businessCode: Int?,
    businessMessage: String?,
  ) {
    val successBoolKey = config.successBoolKey
    if (successBoolKey != null && root.containsKey(successBoolKey)) {
      val flag = root[successBoolKey]?.asBooleanOrNull() ?: false
      if (!flag) {
        throw MappingException.BusinessFailure(
          code = businessCode ?: -1,
          businessMessage = businessMessage,
        )
      }
      return
    }

    if (businessCode != null && businessCode !in config.successCodes) {
      throw MappingException.BusinessFailure(
        code = businessCode,
        businessMessage = businessMessage,
      )
    }
  }

  private fun parseObject(bytes: ByteArray): JsonObject {
    val text = bytes.toString(Charsets.UTF_8)
    val element = try {
      json.parseToJsonElement(text)
    } catch (e: Exception) {
      throw MappingException.InvalidJson(e)
    }
    return try {
      element.jsonObject
    } catch (e: Exception) {
      throw MappingException.InvalidJson(e)
    }
  }
}

/**
 * Resolves a dotted path (`a.b.c`) against [root].
 *
 * Missing segments yield `null`. A present JSON `null` yields [JsonNull].
 */
internal fun resolvePath(root: JsonObject, path: String): JsonElement? {
  val segments = path.split('.').filter { it.isNotEmpty() }
  if (segments.isEmpty()) return null
  var current: JsonElement = root
  for (segment in segments) {
    val obj = current as? JsonObject ?: return null
    current = obj[segment] ?: return null
  }
  return current
}

private fun JsonElement.asIntOrNull(): Int? {
  if (this is JsonNull) return null
  val primitive = this as? JsonPrimitive ?: return null
  return primitive.intOrNull
    ?: primitive.contentOrNull?.toIntOrNull()
}

private fun JsonElement.asStringOrNull(): String? {
  if (this is JsonNull) return null
  val primitive = this as? JsonPrimitive ?: return null
  return primitive.contentOrNull
}

private fun JsonElement.asBooleanOrNull(): Boolean? {
  if (this is JsonNull) return null
  val primitive = this as? JsonPrimitive ?: return null
  return primitive.booleanOrNull
    ?: when (primitive.contentOrNull?.lowercase()) {
      "true", "1", "yes" -> true
      "false", "0", "no" -> false
      else -> null
    }
}
