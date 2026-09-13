package com.fk.core.i18n

import android.os.Build
import android.os.LocaleList
import com.fk.core.pluggable.core.ObservationToken
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-app localization manager: language switch, dictionary → resources lookup,
 * observers, and formatters.
 *
 * Conceptually aligned with iOS `FKI18nManager`. Prefer injecting this over a process singleton.
 */
class I18nManager(
  private var config: I18nConfig,
  private var dictionary: I18nDictionary? = null,
  private var resources: I18nDictionary? = null,
  private val persistence: LanguageStore? = null,
) {
  private val lock = Any()
  private val observers = CopyOnWriteArrayList<Pair<UUID, (I18nLanguage) -> Unit>>()

  @Volatile
  private var languageCode: String =
    resolveInitialLanguageCode(config, persistence)

  /** Active language descriptor. */
  val currentLanguage: I18nLanguage
    get() = I18nLanguage(code = languageCode)

  /** Active BCP-47 language code. */
  val currentLanguageCode: String
    get() = languageCode

  /** Locale for the active in-app language. */
  val currentLocale: Locale
    get() = currentLanguage.locale

  /** Whether the active language is typically laid out RTL. */
  val isRightToLeft: Boolean
    get() =
      android.text.TextUtils.getLayoutDirectionFromLocale(currentLocale) ==
        android.util.LayoutDirection.RTL

  /** Locale-aware number / date formatters for [currentLocale]. */
  val formatters: LocaleFormatters
    get() = LocaleFormatters(currentLocale)

  /** Languages listed in [I18nConfig.supportedLanguageCodes]. */
  fun supportedLanguages(): List<I18nLanguage> =
    LocaleMatcher.uniqueLanguageCodes(config.supportedLanguageCodes)
      .map { I18nLanguage(code = it) }

  /** Replaces configuration and re-resolves the active language. */
  fun configure(newConfig: I18nConfig) {
    synchronized(lock) {
      config = newConfig
    }
    setLanguageCode(resolveInitialLanguageCode(newConfig, persistence))
  }

  /** Installs or clears the dictionary backend (consulted before [resources]). */
  fun setDictionary(newDictionary: I18nDictionary?) {
    synchronized(lock) {
      dictionary = newDictionary
    }
  }

  /** Installs or clears the Android resources / secondary backend. */
  fun setResources(newResources: I18nDictionary?) {
    synchronized(lock) {
      resources = newResources
    }
  }

  /**
   * Updates the active language and notifies observers when the code changes.
   *
   * Blank codes are ignored. When [I18nConfig.enforceSupportedLanguages] is true and
   * the supported list is non-empty, unsupported codes throw [IllegalArgumentException].
   */
  fun setLanguageCode(code: String) {
    val canonical = LocaleMatcher.canonicalize(code)
    if (canonical.isEmpty()) return

    val snapshot = synchronized(lock) { config }
    if (snapshot.enforceSupportedLanguages) {
      val supported = LocaleMatcher.uniqueLanguageCodes(snapshot.supportedLanguageCodes)
      if (supported.isNotEmpty()) {
        require(canonical in supported.toSet()) {
          "Unsupported language code: $canonical (supported=$supported)"
        }
      }
    }
    synchronized(lock) {
      if (languageCode == canonical) return
      languageCode = canonical
    }
    if (snapshot.persistSelection) {
      persistence?.write(snapshot.storageKey, canonical)
    }
    val language = I18nLanguage(code = canonical)
    observers.forEach { (_, handler) ->
      runCatching { handler(language) }
    }
  }

  /**
   * Clears persisted selection and switches to [I18nConfig.defaultLanguageCode].
   */
  fun resetLanguageSelection() {
    val snapshot = synchronized(lock) { config }
    persistence?.write(snapshot.storageKey, null)
    setLanguageCode(snapshot.defaultLanguageCode)
  }

  /**
   * Resolves [key]: dictionary (active language) → resources (progressive fallbacks) → [key].
   */
  fun localized(key: String, table: String? = null): String {
    val code = languageCode
    val snapshotConfig: I18nConfig
    val snapshotDictionary: I18nDictionary?
    val snapshotResources: I18nDictionary?
    synchronized(lock) {
      snapshotConfig = config
      snapshotDictionary = dictionary
      snapshotResources = resources
    }

    snapshotDictionary?.translate(key, code, table)?.let { return it }

    if (snapshotResources != null) {
      val fallbacks = snapshotConfig.fallbackLanguageCodes + snapshotConfig.defaultLanguageCode
      for (candidate in LocaleMatcher.fallbackCandidates(code, fallbacks)) {
        snapshotResources.translate(key, candidate, table)?.let { return it }
      }
    }
    return key
  }

  /** Typed-key overload of [localized]. */
  fun localized(key: I18nKey, table: String? = null): String =
    localized(key.rawValue, table)

  /** Resolves [key] then interpolates `{token}` placeholders. */
  fun localized(
    key: String,
    variables: Map<String, String>,
    table: String? = null,
  ): String = MessageFormat.interpolate(localized(key, table), variables)

  /** Typed-key overload with `{token}` interpolation. */
  fun localized(
    key: I18nKey,
    variables: Map<String, String>,
    table: String? = null,
  ): String = localized(key.rawValue, variables, table)

  /**
   * Resolves [key] and applies simple `one|other` plural splitting via [MessageFormat.pluralSimple].
   */
  fun localizedPlural(key: String, count: Int, table: String? = null): String =
    MessageFormat.pluralSimple(localized(key, table), count)

  /**
   * Observes language changes. Invokes [handler] immediately with the current language.
   */
  fun observeLanguageChange(handler: (I18nLanguage) -> Unit): ObservationToken {
    val id = UUID.randomUUID()
    observers += id to handler
    handler(currentLanguage)
    return ObservationToken {
      observers.removeAll { it.first == id }
    }
  }

  companion object {
    internal fun resolveInitialLanguageCode(
      config: I18nConfig,
      persistence: LanguageStore?,
    ): String {
      val supported = LocaleMatcher.uniqueLanguageCodes(config.supportedLanguageCodes)

      if (config.persistSelection) {
        val stored = persistence?.read(config.storageKey)?.takeIf { it.isNotBlank() }
        if (stored != null) {
          val canonical = LocaleMatcher.canonicalize(stored)
          if (!config.enforceSupportedLanguages ||
            supported.isEmpty() ||
            canonical in supported.toSet()
          ) {
            return canonical
          }
        }
      }

      val preferred = LocaleMatcher.uniqueLanguageCodes(
        systemPreferredLanguageCodes() + config.defaultLanguageCode,
      )
      return LocaleMatcher.bestSupportedLanguage(
        preferredLanguageCodes = preferred,
        supportedLanguageCodes = supported.ifEmpty { listOf(config.defaultLanguageCode) },
        fallback = config.defaultLanguageCode,
      )
    }

    private fun systemPreferredLanguageCodes(): List<String> {
      if (Build.VERSION.SDK_INT >= 24) {
        val list = LocaleList.getDefault()
        return (0 until list.size()).map { index -> list[index].toLanguageTag() }
      }
      @Suppress("DEPRECATION")
      return listOf(Locale.getDefault().toLanguageTag())
    }
  }
}
