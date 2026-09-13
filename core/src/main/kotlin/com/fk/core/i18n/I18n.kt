package com.fk.core.i18n

import android.content.Context
import java.util.Locale

/**
 * I18n — runtime locale switch, typed keys, dictionaries, and locale formatters.
 *
 * Goes beyond static `res/values` by keeping an in-app language independent of the
 * system locale (dictionary and/or configuration-context resources).
 *
 * Conceptually aligned with iOS `FKCoreKit` I18n; Android-shaped APIs.
 */
object I18n {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.0"

  /** Default SharedPreferences key for persisted language selection. */
  const val DEFAULT_STORAGE_KEY: String = "com.fk.i18n.language"

  /**
   * Builds an [I18nManager] backed by an in-memory dictionary (ideal for samples/tests).
   *
   * @param attachAndroidResources When true and [context] is non-null, missing dictionary
   *   keys fall through to [AndroidResourcesDictionary].
   */
  fun dictionaryManager(
    flatDictionary: Map<String, Map<String, String>>,
    config: I18nConfig = I18nConfig(
      defaultLanguageCode = "en",
      supportedLanguageCodes = flatDictionary.keys.toList(),
    ),
    context: Context? = null,
    attachAndroidResources: Boolean = false,
  ): I18nManager {
    return I18nManager(
      config = config,
      dictionary = MapDictionary.ofFlat(flatDictionary),
      resources = if (attachAndroidResources && context != null) {
        AndroidResourcesDictionary(context)
      } else {
        null
      },
      persistence = context?.let { SharedPrefsLanguageStore(it) },
    )
  }
}

/**
 * Runtime configuration for [I18nManager].
 *
 * @property defaultLanguageCode Fallback / initial language (BCP-47).
 * @property supportedLanguageCodes Languages exposed for in-app switching.
 * @property fallbackLanguageCodes Extra lookup fallbacks after the active code (resources path).
 * @property persistSelection Whether to read/write the selection under [storageKey].
 * @property storageKey Persistence key (SharedPreferences).
 * @property enforceSupportedLanguages When true, reject unsupported [I18nManager.setLanguageCode]
 *   if the supported list is non-empty.
 */
data class I18nConfig(
  val defaultLanguageCode: String = "en",
  val supportedLanguageCodes: List<String> = listOf("en"),
  val fallbackLanguageCodes: List<String> = emptyList(),
  val persistSelection: Boolean = true,
  val storageKey: String = I18n.DEFAULT_STORAGE_KEY,
  val enforceSupportedLanguages: Boolean = true,
)

/**
 * Selectable in-app language (BCP-47 [code]).
 */
data class I18nLanguage(
  val code: String,
  val displayNameOverride: String? = null,
) {
  /** [Locale] derived from [code]. */
  val locale: Locale
    get() = Locale.forLanguageTag(code.replace('_', '-'))

  /** User-facing label for pickers. */
  fun displayName(using: Locale? = null): String {
    if (!displayNameOverride.isNullOrBlank()) return displayNameOverride
    val target = using ?: locale
    return locale.getDisplayName(target).ifBlank { code }
  }
}

/**
 * Typed localization key to avoid scattering free-form strings.
 */
@JvmInline
value class I18nKey(val rawValue: String) {
  override fun toString(): String = rawValue
}
