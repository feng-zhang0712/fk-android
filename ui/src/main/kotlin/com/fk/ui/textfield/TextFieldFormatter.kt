package com.fk.ui.textfield

/**
 * Formats and sanitizes text for [FkTextField].
 *
 * Conceptually aligned with iOS `FKTextFieldDefaultFormatter` (deterministic, locale-free).
 */
object TextFieldFormatter {
  /**
   * Sanitizes [text] into canonical [TextFieldFormatResult.rawText] and a display string.
   */
  fun format(
    text: String,
    format: TextFieldFormat,
    maxLengthOverride: Int? = null,
  ): TextFieldFormatResult =
    when (format) {
      TextFieldFormat.PhoneNumber -> {
        val limit = maxLengthOverride ?: 11
        val raw = text.digitsOnly().truncate(limit)
        TextFieldFormatResult(raw, raw.grouped(listOf(3, 4, 4)))
      }
      TextFieldFormat.IdCard -> {
        val limit = maxLengthOverride ?: 18
        val raw = normalizeIdCard(text, limit)
        val pattern = if (raw.length > 15) listOf(6, 4, 4, 4) else listOf(6, 4, 5)
        TextFieldFormatResult(raw, raw.grouped(pattern))
      }
      TextFieldFormat.BankCard -> {
        val limit = maxLengthOverride ?: 24
        val raw = text.digitsOnly().truncate(limit)
        TextFieldFormatResult(raw, raw.grouped(listOf(4)))
      }
      is TextFieldFormat.VerificationCode -> {
        val limit = maxLengthOverride ?: format.length.coerceAtLeast(0)
        val raw = if (format.allowsAlphabet) {
          text.filter { it.isLetterOrDigit() }.uppercase().truncate(limit)
        } else {
          text.digitsOnly().truncate(limit)
        }
        TextFieldFormatResult(raw, raw)
      }
      is TextFieldFormat.Password -> {
        val limit = maxLengthOverride ?: format.maxLength
        val raw = text.truncate(limit.coerceAtLeast(0))
        TextFieldFormatResult(raw, raw)
      }
      is TextFieldFormat.Amount -> formatAmount(text, format)
      TextFieldFormat.Email -> {
        val raw = text.filterNot { it.isWhitespace() }.lowercase()
          .let { if (maxLengthOverride != null) it.truncate(maxLengthOverride) else it }
        TextFieldFormatResult(raw, raw)
      }
      TextFieldFormat.Numeric -> {
        val raw = text.digitsOnly().let {
          if (maxLengthOverride != null) it.truncate(maxLengthOverride) else it
        }
        TextFieldFormatResult(raw, raw)
      }
      TextFieldFormat.Alphabetic -> {
        val raw = text.filter { it.isLetter() }.let {
          if (maxLengthOverride != null) it.truncate(maxLengthOverride) else it
        }
        TextFieldFormatResult(raw, raw)
      }
      TextFieldFormat.AlphaNumeric -> {
        val raw = text.filter { it.isLetterOrDigit() }.let {
          if (maxLengthOverride != null) it.truncate(maxLengthOverride) else it
        }
        TextFieldFormatResult(raw, raw)
      }
      is TextFieldFormat.Custom -> {
        val filtered = text.filter { format.characterRegex.matches(it.toString()) }
        val limited = filtered.let {
          val cap = maxLengthOverride ?: format.maxLength
          if (cap != null) it.truncate(cap) else it
        }
        val display = if (format.separator != null && format.groupPattern.isNotEmpty()) {
          limited.grouped(format.groupPattern, format.separator)
        } else {
          limited
        }
        TextFieldFormatResult(limited, display)
      }
      TextFieldFormat.Plain -> {
        val raw = if (maxLengthOverride != null) text.truncate(maxLengthOverride) else text
        TextFieldFormatResult(raw, raw)
      }
    }

  private fun formatAmount(text: String, format: TextFieldFormat.Amount): TextFieldFormatResult {
    val filtered = text.filter { it.isDigit() || it == '.' }
    val hasDot = filtered.contains('.')
    val parts = filtered.split('.', limit = 2)
    val integerRaw = parts.getOrElse(0) { "" }.digitsOnly()
      .truncate(format.maxIntegerDigits.coerceAtLeast(1))
    val decimalRaw = if (parts.size > 1) {
      parts[1].digitsOnly().truncate(format.decimalDigits.coerceAtLeast(0))
    } else {
      ""
    }
    // Keep a trailing '.' while the user is typing decimals (e.g. "12.").
    val raw = when {
      hasDot && decimalRaw.isEmpty() -> "$integerRaw."
      decimalRaw.isEmpty() -> integerRaw
      else -> "$integerRaw.$decimalRaw"
    }
    val groupedInteger = groupThousands(integerRaw)
    val formatted = when {
      hasDot && decimalRaw.isEmpty() -> "$groupedInteger."
      decimalRaw.isEmpty() -> groupedInteger
      else -> "$groupedInteger.$decimalRaw"
    }
    return TextFieldFormatResult(raw, formatted)
  }

  /** Digits for body; optional single trailing `X` checksum (18-digit cards). */
  private fun normalizeIdCard(text: String, limit: Int): String {
    val upper = text.uppercase()
    val body = StringBuilder()
    var checksum: Char? = null
    for (ch in upper) {
      when {
        ch.isDigit() && body.length < 17 -> body.append(ch)
        ch == 'X' && body.length == 17 && checksum == null -> checksum = 'X'
      }
    }
    val combined = if (checksum != null) body.toString() + checksum else body.toString()
    return combined.truncate(limit)
  }

  private fun groupThousands(digits: String): String {
    if (digits.length <= 3) return digits
    val chars = ArrayList<Char>(digits.length + digits.length / 3)
    var counter = 0
    for (ch in digits.reversed()) {
      if (counter > 0 && counter % 3 == 0) chars.add(',')
      chars.add(ch)
      counter++
    }
    return chars.reversed().joinToString("")
  }
}

internal fun String.digitsOnly(): String = filter { it.isDigit() }

internal fun String.truncate(max: Int): String =
  if (max <= 0 || length <= max) this else substring(0, max)

/**
 * Inserts [separator] between groups defined by [pattern].
 *
 * A single-element pattern (e.g. `[4]`) repeats for the whole string.
 * Remaining characters after exhausting a multi-element pattern use the last group size.
 */
internal fun String.grouped(pattern: List<Int>, separator: Char = ' '): String {
  if (isEmpty() || pattern.isEmpty()) return this
  val out = StringBuilder()
  var index = 0
  var groupIndex = 0
  while (index < length) {
    val size = if (pattern.size == 1) {
      pattern[0]
    } else {
      pattern.getOrElse(groupIndex) { pattern.last() }
    }.coerceAtLeast(1)
    if (out.isNotEmpty()) out.append(separator)
    val end = (index + size).coerceAtMost(length)
    out.append(substring(index, end))
    index = end
    groupIndex++
  }
  return out.toString()
}
