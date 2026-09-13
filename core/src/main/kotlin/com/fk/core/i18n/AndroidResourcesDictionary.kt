package com.fk.core.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Resolves Android `string` resources under an in-app [languageCode] via
 * [Context.createConfigurationContext], independent of the system locale.
 *
 * Resource entry names cannot contain `.`; keys such as `demo.title` are looked up
 * as `demo_title`. Returns `null` when the name is missing (never the raw key).
 */
class AndroidResourcesDictionary(
  context: Context,
  private val packageName: String = context.packageName,
) : I18nDictionary {
  private val appContext: Context = context.applicationContext

  override fun translate(key: String, languageCode: String, table: String?): String? {
    val locale = Locale.forLanguageTag(LocaleMatcher.normalize(languageCode))
    val configuration = Configuration(appContext.resources.configuration)
    configuration.setLocale(locale)
    val localized = appContext.createConfigurationContext(configuration)
    val resourceName = key.replace('.', '_')
    val resId = localized.resources.getIdentifier(resourceName, "string", packageName)
    if (resId == 0) return null
    return localized.getString(resId)
  }
}
