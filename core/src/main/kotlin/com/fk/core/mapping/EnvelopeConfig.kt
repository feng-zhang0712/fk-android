package com.fk.core.mapping

/**
 * Field names and success rules for `{ code, message, data }` style envelopes.
 *
 * @property payloadKey JSON key holding the business payload (used when
 *   [nestedPayloadPath] is null).
 * @property codeKey Business status code key.
 * @property messageKey Human-readable business message key.
 * @property successCodes Codes treated as success when the success-bool key is
 *   absent or not present in the payload.
 * @property successBoolKey Optional boolean success flag (e.g. `"success"`).
 *   When the key is present, its value decides success; when missing, [successCodes]
 *   are used instead.
 * @property nestedPayloadPath Optional dotted path for nested payloads
 *   (e.g. `"result.items"`). When set, overrides [payloadKey].
 */
data class EnvelopeConfig(
  val payloadKey: String = "data",
  val codeKey: String = "code",
  val messageKey: String = "message",
  val successCodes: Set<Int> = setOf(0),
  val successBoolKey: String? = null,
  val nestedPayloadPath: String? = null,
) {
  companion object {
    /** Standard `{ code, message, data }` with success code `0`. */
    val Standard: EnvelopeConfig = EnvelopeConfig()

    /** `{ success, result, code?, message? }` style envelope. */
    val SuccessFlag: EnvelopeConfig = EnvelopeConfig(
      payloadKey = "result",
      successBoolKey = "success",
    )
  }

  /** Effective payload path: [nestedPayloadPath] or [payloadKey]. */
  val effectivePayloadPath: String
    get() = nestedPayloadPath?.takeIf { it.isNotBlank() } ?: payloadKey
}

/**
 * Intermediate result after unwrapping an envelope, before decoding [payload].
 */
data class EnvelopeResult(
  val payload: kotlinx.serialization.json.JsonElement,
  val businessCode: Int?,
  val businessMessage: String?,
)
