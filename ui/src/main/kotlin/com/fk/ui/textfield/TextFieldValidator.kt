package com.fk.ui.textfield

/**
 * Sync validation for [FkTextField] formats.
 *
 * Conceptually aligned with iOS `FKTextFieldDefaultValidator`.
 * Empty input is treated as valid (required checks belong to the form layer).
 */
object TextFieldValidator {
  fun validate(
    rawText: String,
    format: TextFieldFormat,
    minLength: Int? = null,
    maxLength: Int? = null,
  ): TextFieldValidationResult {
    if (rawText.isEmpty()) return TextFieldValidationResult.Valid

    if (minLength != null && rawText.length < minLength) {
      return TextFieldValidationResult(false, "Must be at least $minLength characters")
    }
    if (maxLength != null && rawText.length > maxLength) {
      return TextFieldValidationResult(false, "Must be at most $maxLength characters")
    }

    return when (format) {
      TextFieldFormat.PhoneNumber -> {
        val ok = rawText.length == 11 && rawText.all { it.isDigit() }
        TextFieldValidationResult(ok, if (ok) null else "Enter an 11-digit phone number")
      }
      TextFieldFormat.IdCard -> {
        val ok = validateIdCard(rawText)
        TextFieldValidationResult(ok, if (ok) null else "Enter a valid ID card number")
      }
      TextFieldFormat.BankCard -> {
        val ok = rawText.length in 12..24 && rawText.all { it.isDigit() }
        TextFieldValidationResult(ok, if (ok) null else "Enter a valid bank card number")
      }
      is TextFieldFormat.VerificationCode -> {
        val ok = rawText.length == format.length
        TextFieldValidationResult(ok, if (ok) null else "Enter the ${format.length}-character code")
      }
      is TextFieldFormat.Password -> {
        if (rawText.length < format.minLength) {
          return TextFieldValidationResult(false, "Password must be at least ${format.minLength} characters")
        }
        if (format.validatesStrength) {
          val strong = rawText.any { it.isUpperCase() } &&
            rawText.any { it.isLowerCase() } &&
            rawText.any { it.isDigit() }
          return TextFieldValidationResult(
            strong,
            if (strong) null else "Use upper, lower, and a digit",
          )
        }
        TextFieldValidationResult.Valid
      }
      is TextFieldFormat.Amount -> {
        val pattern = Regex("^\\d+(\\.\\d{0,${format.decimalDigits.coerceAtLeast(0)}})?$")
        val ok = pattern.matches(rawText)
        TextFieldValidationResult(ok, if (ok) null else "Enter a valid amount")
      }
      TextFieldFormat.Email -> {
        val ok = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(rawText)
        TextFieldValidationResult(ok, if (ok) null else "Enter a valid email")
      }
      TextFieldFormat.Numeric -> {
        val ok = rawText.all { it.isDigit() }
        TextFieldValidationResult(ok, if (ok) null else "Digits only")
      }
      TextFieldFormat.Alphabetic -> {
        val ok = rawText.all { it.isLetter() }
        TextFieldValidationResult(ok, if (ok) null else "Letters only")
      }
      TextFieldFormat.AlphaNumeric -> {
        val ok = rawText.all { it.isLetterOrDigit() }
        TextFieldValidationResult(ok, if (ok) null else "Letters and digits only")
      }
      is TextFieldFormat.Custom -> {
        val ok = rawText.all { format.characterRegex.matches(it.toString()) }
        TextFieldValidationResult(ok, if (ok) null else "Invalid characters")
      }
      TextFieldFormat.Plain -> TextFieldValidationResult.Valid
    }
  }

  /** Chinese Resident Identity Card (15 numeric or 18 with checksum). */
  fun validateIdCard(id: String): Boolean {
    if (id.length == 15) return id.all { it.isDigit() }
    if (id.length != 18) return false
    val body = id.take(17)
    val check = id.takeLast(1).uppercase()
    if (!body.all { it.isDigit() }) return false
    val factors = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    val parity = arrayOf("1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2")
    val sum = body.mapIndexed { i, c -> c.digitToInt() * factors[i] }.sum()
    return parity[sum % 11] == check
  }
}
