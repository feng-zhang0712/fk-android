package com.fk.core.i18n

/**
 * `{token}` interpolation helpers for localization templates.
 *
 * Conceptually aligned with iOS `FKI18nMessageFormat` (token map path).
 * Android `String.format` / ICU MessageFormat remain available for apps that need them;
 * this helper matches the iOS `{name}` dictionary style used across FKKit.
 */
object MessageFormat {
  /**
   * Replaces `{token}` placeholders with [variables]. Unknown tokens stay unchanged.
   */
  fun interpolate(template: String, variables: Map<String, String>): String {
    if (!template.contains('{') || variables.isEmpty()) return template
    var result = template
    for ((key, value) in variables) {
      result = result.replace("{$key}", value)
    }
    return result
  }

  /**
   * Simple English-style plural pick for dictionary templates that use `|` separators:
   * `one|other` → picks by [count] (`1` → first segment, else second).
   *
   * Prefer full ICU / Android plurals in `res` when shipping production copy.
   */
  fun pluralSimple(template: String, count: Int): String {
    val parts = template.split('|')
    if (parts.size < 2) return template
    return if (count == 1) parts[0] else parts[1]
  }
}
