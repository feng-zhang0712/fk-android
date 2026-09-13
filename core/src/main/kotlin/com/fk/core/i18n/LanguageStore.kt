package com.fk.core.i18n

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistence for the in-app language selection.
 *
 * Keys are supplied by the caller (typically [I18nConfig.storageKey]) so
 * [I18nManager.configure] can rebind persistence without reconstructing the store.
 */
interface LanguageStore {
  /** Previously saved BCP-47 code for [key], or `null`. */
  fun read(key: String): String?

  /** Persists [code] under [key], or clears when [code] is null/blank. */
  fun write(key: String, code: String?)
}

/**
 * [LanguageStore] backed by a private SharedPreferences file.
 */
class SharedPrefsLanguageStore(
  context: Context,
  preferencesName: String = "fk_i18n",
) : LanguageStore {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

  override fun read(key: String): String? = prefs.getString(key, null)

  override fun write(key: String, code: String?) {
    prefs.edit().apply {
      if (code.isNullOrBlank()) remove(key) else putString(key, code)
      commit()
    }
  }
}
