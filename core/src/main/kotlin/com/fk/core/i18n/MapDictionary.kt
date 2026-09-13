package com.fk.core.i18n

/**
 * Optional dictionary / remote-copy backend consulted before Android resources.
 */
fun interface I18nDictionary {
  /**
   * @return Localized value for [key] in [languageCode], or `null` when missing.
   */
  fun translate(key: String, languageCode: String, table: String?): String?
}

/**
 * Nested map dictionary: `language → table → key → value`.
 *
 * Lookup is exact [languageCode] then [fallbackLanguageCode] (progressive locale
 * fallback is owned by [I18nManager] for resource backends).
 *
 * Prefer [ofFlat] for simple `language → key → value` maps (stored under [DEFAULT_TABLE]).
 */
class MapDictionary(
  nestedDictionary: Map<String, Map<String, Map<String, String>>>,
  fallbackLanguageCode: String = "en",
) : I18nDictionary {
  private val dictionary: Map<String, Map<String, Map<String, String>>> =
    nestedDictionary.mapKeys { LocaleMatcher.canonicalize(it.key) }

  private val fallback: String = LocaleMatcher.canonicalize(fallbackLanguageCode)

  override fun translate(key: String, languageCode: String, table: String?): String? {
    val tableName = table ?: DEFAULT_TABLE
    val primary = LocaleMatcher.canonicalize(languageCode)
    dictionary[primary]?.get(tableName)?.get(key)?.let { return it }
    if (primary != fallback) {
      dictionary[fallback]?.get(tableName)?.get(key)?.let { return it }
    }
    return null
  }

  companion object {
    /** Default table name for flat dictionaries (iOS `Localizable` parity). */
    const val DEFAULT_TABLE: String = "Localizable"

    /**
     * Flat `languageCode → key → value` stored under [DEFAULT_TABLE].
     */
    fun ofFlat(
      flatDictionary: Map<String, Map<String, String>>,
      fallbackLanguageCode: String = "en",
    ): MapDictionary =
      MapDictionary(
        nestedDictionary = flatDictionary.mapValues { (_, values) ->
          mapOf(DEFAULT_TABLE to values)
        },
        fallbackLanguageCode = fallbackLanguageCode,
      )
  }
}
