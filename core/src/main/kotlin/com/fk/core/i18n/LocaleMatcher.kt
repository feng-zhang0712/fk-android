package com.fk.core.i18n

/**
 * Normalizes BCP-47 language tags and builds lookup candidate chains.
 *
 * Conceptually aligned with iOS `FKI18nLocaleMatcher`.
 */
object LocaleMatcher {
  private val languageAliases: Map<String, String> = mapOf(
    "zh-CN" to "zh-Hans",
    "zh-Hans-CN" to "zh-Hans",
    "zh-SG" to "zh-Hans",
    "zh-TW" to "zh-Hant",
    "zh-HK" to "zh-Hant",
    "zh-MO" to "zh-Hant",
    "zh-Hant-TW" to "zh-Hant",
    "zh-Hant-HK" to "zh-Hant",
  )

  /** Trims and replaces `_` with `-`. */
  fun normalize(code: String): String =
    code.trim().replace('_', '-')

  /** Normalizes and maps known Chinese region aliases to script codes. */
  fun canonicalize(code: String): String {
    val normalized = normalize(code)
    return languageAliases[normalized] ?: normalized
  }

  /**
   * Progressive fallbacks for resource / dictionary lookup.
   *
   * Example: `zh-Hans-CN` → `zh-Hans-CN`, `zh-Hans`, `zh`, then [additionalFallbacks].
   */
  fun fallbackCandidates(
    languageCode: String,
    additionalFallbacks: List<String> = emptyList(),
  ): List<String> {
    val result = LinkedHashSet<String>()
    fun append(raw: String) {
      val n = canonicalize(raw)
      if (n.isNotEmpty()) result += n
    }

    append(languageCode)
    val parts = normalize(languageCode).split('-').filter { it.isNotEmpty() }
    if (parts.size > 1) {
      for (i in parts.size - 1 downTo 1) {
        append(parts.subList(0, i).joinToString("-"))
      }
    }
    parts.firstOrNull()?.let { append(it) }
    additionalFallbacks.forEach { append(it) }
    return result.toList()
  }

  /** First-seen order, de-duplicated after canonicalize. */
  fun uniqueLanguageCodes(codes: List<String>): List<String> {
    val result = LinkedHashSet<String>()
    codes.forEach { code ->
      val c = canonicalize(code)
      if (c.isNotEmpty()) result += c
    }
    return result.toList()
  }

  /**
   * Picks the best supported language for [preferredLanguageCodes].
   */
  fun bestSupportedLanguage(
    preferredLanguageCodes: List<String>,
    supportedLanguageCodes: List<String>,
    fallback: String,
  ): String {
    if (supportedLanguageCodes.isEmpty()) return canonicalize(fallback)
    val supported = uniqueLanguageCodes(supportedLanguageCodes)
    val supportedSet = supported.toSet()

    for (preferred in uniqueLanguageCodes(preferredLanguageCodes)) {
      if (preferred in supportedSet) return preferred
      for (candidate in fallbackCandidates(preferred)) {
        if (candidate in supportedSet) return candidate
      }
    }
    val canonicalFallback = canonicalize(fallback)
    return if (canonicalFallback in supportedSet) canonicalFallback else supported.first()
  }
}
