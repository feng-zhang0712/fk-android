package com.fk.ui.textfield

/**
 * Form text-field package hub — formatting, validation, OTP, counters.
 *
 * Conceptually aligned with iOS `FKTextField` / `FKCodeTextField` / `FKCountTextView`
 * (Compose form capabilities — not a plain Material TextField wrapper).
 */
object FkTextFields {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"
}

/**
 * Built-in formatting / filtering strategies.
 *
 * Conceptually aligned with iOS `FKTextFieldFormatType`.
 */
sealed class TextFieldFormat {
  /** Digits with 3-4-4 display grouping (CN mobile default, max 11). */
  data object PhoneNumber : TextFieldFormat()

  /** Digits + optional trailing `X`; 6-4-4-4 / 6-4-5 grouping. */
  data object IdCard : TextFieldFormat()

  /** Digits grouped by 4 (max 24). */
  data object BankCard : TextFieldFormat()

  /** Fixed-length verification code (not the slot OTP UI — see [OtpTextField]). */
  data class VerificationCode(
    val length: Int = 6,
    val allowsAlphabet: Boolean = false,
  ) : TextFieldFormat()

  /** Password with optional strength rule. */
  data class Password(
    val minLength: Int = 8,
    val maxLength: Int = 32,
    val validatesStrength: Boolean = false,
  ) : TextFieldFormat()

  /** Amount with optional thousands grouping and decimal scale. */
  data class Amount(
    val maxIntegerDigits: Int = 12,
    val decimalDigits: Int = 2,
  ) : TextFieldFormat()

  data object Email : TextFieldFormat()
  data object Numeric : TextFieldFormat()
  data object Alphabetic : TextFieldFormat()
  data object AlphaNumeric : TextFieldFormat()

  /** Per-character allowlist regex + optional display grouping. */
  data class Custom(
    val characterRegex: Regex,
    val maxLength: Int? = null,
    val separator: Char? = null,
    val groupPattern: List<Int> = emptyList(),
  ) : TextFieldFormat()

  /** No built-in filtering beyond optional [TextFieldConfiguration.maxLength]. */
  data object Plain : TextFieldFormat()
}

/**
 * Visual / semantic status for supporting text and borders.
 *
 * Conceptually aligned with iOS `FKTextFieldStatus` (narrow subset).
 */
enum class TextFieldStatus {
  Normal,
  Error,
  Success,
  Disabled,
  ReadOnly,
}

/**
 * When to run the sync validator.
 *
 * Conceptually aligned with iOS `FKTextFieldValidationTrigger`.
 */
enum class TextFieldValidationTrigger {
  OnChange,
  OnBlur,
  OnSubmit,
}

/**
 * Helper / success / error message channels.
 *
 * Conceptually aligned with iOS `FKTextFieldMessages`.
 */
data class TextFieldMessages(
  val helper: String? = null,
  val success: String? = null,
  val error: String? = null,
)

/**
 * Result of formatting a user edit into raw + display text.
 */
data class TextFieldFormatResult(
  val rawText: String,
  val formattedText: String,
)

/**
 * Sync validation outcome.
 */
data class TextFieldValidationResult(
  val isValid: Boolean,
  val message: String? = null,
) {
  companion object {
    val Valid: TextFieldValidationResult = TextFieldValidationResult(isValid = true)
  }
}

/**
 * Aggregate configuration for [FkTextField].
 */
data class TextFieldConfiguration(
  val format: TextFieldFormat = TextFieldFormat.Plain,
  val validationTrigger: TextFieldValidationTrigger = TextFieldValidationTrigger.OnBlur,
  val messages: TextFieldMessages = TextFieldMessages(),
  /** Hard cap on raw length (also applied by some formats). */
  val maxLength: Int? = null,
  val minLength: Int? = null,
  /** Show `current / max` when [maxLength] is set. */
  val showCounter: Boolean = false,
  val singleLine: Boolean = true,
)

/**
 * OTP slot style.
 *
 * Conceptually aligned with iOS `FKCodeTextField.SlotStyle`.
 */
enum class OtpSlotStyle {
  Boxes,
  Underlines,
}

/**
 * OTP field configuration.
 */
data class OtpConfiguration(
  val length: Int = 6,
  val slotStyle: OtpSlotStyle = OtpSlotStyle.Boxes,
  val allowsAlphabet: Boolean = false,
)

/**
 * Multiline counted text area configuration.
 *
 * Conceptually aligned with iOS `FKCountTextView.Configuration`.
 */
data class CountedTextConfiguration(
  val maxLength: Int? = 200,
  val showCounter: Boolean = true,
  val minLines: Int = 3,
  val maxLines: Int = 6,
)
