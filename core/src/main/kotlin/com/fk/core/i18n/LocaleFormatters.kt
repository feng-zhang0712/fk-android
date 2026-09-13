package com.fk.core.i18n

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/**
 * Locale-aware formatter factory bound to an in-app [locale].
 *
 * Conceptually aligned with iOS `FKI18nFormatterProvider`.
 */
class LocaleFormatters(
  val locale: Locale,
) {
  /** Decimal / currency / percent number formatters for [locale]. */
  fun number(style: NumberStyle = NumberStyle.Decimal): NumberFormat =
    when (style) {
      NumberStyle.Decimal -> NumberFormat.getNumberInstance(locale)
      NumberStyle.Currency -> NumberFormat.getCurrencyInstance(locale)
      NumberStyle.Percent -> NumberFormat.getPercentInstance(locale)
      NumberStyle.Integer -> NumberFormat.getIntegerInstance(locale)
    }

  /** Formats [value] with [style]. */
  fun formatNumber(value: Number, style: NumberStyle = NumberStyle.Decimal): String =
    number(style).format(value)

  /**
   * Date (± time) formatters for [locale].
   *
   * Pass `null` for [dateStyle] or [timeStyle] to omit that part
   * (Java [DateFormat] has no `NONE` constant).
   */
  fun date(
    dateStyle: Int? = DateFormat.MEDIUM,
    timeStyle: Int? = null,
  ): DateFormat =
    when {
      timeStyle == null && dateStyle != null ->
        DateFormat.getDateInstance(dateStyle, locale)
      dateStyle == null && timeStyle != null ->
        DateFormat.getTimeInstance(timeStyle, locale)
      dateStyle != null && timeStyle != null ->
        DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale)
      else ->
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    }

  /** Formats [date] with [dateStyle] / [timeStyle]. */
  fun formatDate(
    date: Date,
    dateStyle: Int? = DateFormat.MEDIUM,
    timeStyle: Int? = null,
  ): String = this.date(dateStyle, timeStyle).format(date)
}

/** Number formatter styles exposed by [LocaleFormatters]. */
enum class NumberStyle {
  Decimal,
  Currency,
  Percent,
  Integer,
}
