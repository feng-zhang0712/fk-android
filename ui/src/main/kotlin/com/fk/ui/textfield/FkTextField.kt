package com.fk.ui.textfield

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Formatted / validated outlined text field.
 *
 * Controlled by [value] / [onValueChange] (formatted display). Canonical raw text
 * (no grouping separators) is reported via [onRawChange].
 *
 * Conceptually aligned with iOS `FKTextField` (narrow Compose port).
 *
 * @param value Formatted display text (source of truth from the caller).
 * @param onValueChange Invoked with the new **formatted** display string after filtering.
 * @param onRawChange Invoked with the canonical raw string after each edit.
 * @param visualTransformation Ignored when [TextFieldFormat.Password] (visibility toggle owns masking).
 */
@Composable
fun FkTextField(
  value: String,
  onValueChange: (formatted: String) -> Unit,
  modifier: Modifier = Modifier,
  configuration: TextFieldConfiguration = TextFieldConfiguration(),
  label: String? = null,
  placeholder: String? = null,
  status: TextFieldStatus = TextFieldStatus.Normal,
  enabled: Boolean = status != TextFieldStatus.Disabled,
  readOnly: Boolean = status == TextFieldStatus.ReadOnly,
  validator: ((raw: String) -> TextFieldValidationResult)? = null,
  onValidation: ((TextFieldValidationResult) -> Unit)? = null,
  onRawChange: ((raw: String) -> Unit)? = null,
  keyboardOptions: KeyboardOptions = keyboardOptionsFor(configuration.format),
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  visualTransformation: VisualTransformation = visualTransformationFor(configuration.format),
) {
  var focused by remember { mutableStateOf(false) }
  var validationMessage by remember { mutableStateOf<String?>(null) }
  var localStatus by remember { mutableStateOf(TextFieldStatus.Normal) }
  val isPassword = configuration.format is TextFieldFormat.Password
  var passwordVisible by remember { mutableStateOf(false) }

  fun runValidation(raw: String): TextFieldValidationResult {
    val result = validator?.invoke(raw)
      ?: TextFieldValidator.validate(
        rawText = raw,
        format = configuration.format,
        minLength = configuration.minLength,
        maxLength = configuration.maxLength,
      )
    validationMessage = result.message
    localStatus = when {
      !result.isValid -> TextFieldStatus.Error
      result.isValid && raw.isNotEmpty() && configuration.messages.success != null ->
        TextFieldStatus.Success
      else -> TextFieldStatus.Normal
    }
    onValidation?.invoke(result)
    return result
  }

  // Explicit parent status (error from server, disabled, …) wins over local validation chrome.
  val effectiveStatus = when {
    status != TextFieldStatus.Normal -> status
    else -> localStatus
  }

  val supporting = supportingText(
    status = effectiveStatus,
    messages = configuration.messages,
    validationMessage = validationMessage,
    rawLength = TextFieldFormatter.format(value, configuration.format, configuration.maxLength).rawText.length,
    maxLength = configuration.maxLength,
    showCounter = configuration.showCounter,
  )

  val colors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = when (effectiveStatus) {
      TextFieldStatus.Error -> fkStatusColor(FkStatusSemantic.Error)
      TextFieldStatus.Success -> fkStatusColor(FkStatusSemantic.Success)
      else -> fkColor(FkColorRole.Primary)
    },
    unfocusedBorderColor = when (effectiveStatus) {
      TextFieldStatus.Error -> fkStatusColor(FkStatusSemantic.Error)
      TextFieldStatus.Success -> fkStatusColor(FkStatusSemantic.Success)
      else -> fkColor(FkColorRole.Outline)
    },
    errorBorderColor = fkStatusColor(FkStatusSemantic.Error),
    focusedLabelColor = when (effectiveStatus) {
      TextFieldStatus.Error -> fkStatusColor(FkStatusSemantic.Error)
      else -> fkColor(FkColorRole.Primary)
    },
  )

  val effectiveTransformation = when {
    isPassword && !passwordVisible -> PasswordVisualTransformation()
    isPassword && passwordVisible -> VisualTransformation.None
    else -> visualTransformation
  }

  OutlinedTextField(
    value = value,
    onValueChange = { incoming ->
      if (readOnly) return@OutlinedTextField
      val result = TextFieldFormatter.format(
        text = incoming,
        format = configuration.format,
        maxLengthOverride = configuration.maxLength,
      )
      onValueChange(result.formattedText)
      onRawChange?.invoke(result.rawText)
      if (configuration.validationTrigger == TextFieldValidationTrigger.OnChange) {
        runValidation(result.rawText)
      } else if (localStatus == TextFieldStatus.Error || localStatus == TextFieldStatus.Success) {
        // Clear stale blur/submit chrome while editing; next trigger re-validates.
        localStatus = TextFieldStatus.Normal
        validationMessage = null
      }
    },
    modifier = modifier.onFocusChanged { state ->
      val wasFocused = focused
      focused = state.isFocused
      if (wasFocused && !state.isFocused &&
        configuration.validationTrigger == TextFieldValidationTrigger.OnBlur
      ) {
        val raw = TextFieldFormatter.format(value, configuration.format, configuration.maxLength).rawText
        runValidation(raw)
      }
    },
    enabled = enabled,
    readOnly = readOnly,
    label = label?.let { { Text(it) } },
    placeholder = placeholder?.let { { Text(it) } },
    supportingText = supporting?.let { msg ->
      {
        Text(
          text = msg,
          style = fkTextStyle(FkTextStyle.Caption1),
          color = when (effectiveStatus) {
            TextFieldStatus.Error -> fkStatusColor(FkStatusSemantic.Error)
            TextFieldStatus.Success -> fkStatusColor(FkStatusSemantic.Success)
            else -> fkColor(FkColorRole.OnSurfaceSecondary)
          },
        )
      }
    },
    trailingIcon = if (isPassword && enabled && !readOnly) {
      {
        TextButton(onClick = { passwordVisible = !passwordVisible }) {
          Text(if (passwordVisible) "Hide" else "Show")
        }
      }
    } else {
      null
    },
    isError = effectiveStatus == TextFieldStatus.Error,
    singleLine = configuration.singleLine,
    keyboardOptions = keyboardOptions.copy(
      imeAction = if (configuration.validationTrigger == TextFieldValidationTrigger.OnSubmit) {
        ImeAction.Done
      } else {
        keyboardOptions.imeAction
      },
    ),
    keyboardActions = KeyboardActions(
      onDone = {
        if (configuration.validationTrigger == TextFieldValidationTrigger.OnSubmit) {
          val raw = TextFieldFormatter.format(value, configuration.format, configuration.maxLength).rawText
          runValidation(raw)
        }
        keyboardActions.onDone?.invoke(this)
      },
      onGo = keyboardActions.onGo,
      onNext = keyboardActions.onNext,
      onPrevious = keyboardActions.onPrevious,
      onSearch = keyboardActions.onSearch,
      onSend = keyboardActions.onSend,
    ),
    visualTransformation = effectiveTransformation,
    colors = colors,
  )
}

/**
 * Runs the default (or custom) validator for a formatted field value.
 *
 * Useful for form submit outside [TextFieldValidationTrigger] timing.
 */
fun validateTextField(
  formattedValue: String,
  configuration: TextFieldConfiguration,
  validator: ((raw: String) -> TextFieldValidationResult)? = null,
): TextFieldValidationResult {
  val raw = TextFieldFormatter.format(
    text = formattedValue,
    format = configuration.format,
    maxLengthOverride = configuration.maxLength,
  ).rawText
  return validator?.invoke(raw)
    ?: TextFieldValidator.validate(
      rawText = raw,
      format = configuration.format,
      minLength = configuration.minLength,
      maxLength = configuration.maxLength,
    )
}

private fun supportingText(
  status: TextFieldStatus,
  messages: TextFieldMessages,
  validationMessage: String?,
  rawLength: Int,
  maxLength: Int?,
  showCounter: Boolean,
): String? {
  val statusMessage = when (status) {
    TextFieldStatus.Error -> validationMessage ?: messages.error
    TextFieldStatus.Success -> messages.success
    else -> messages.helper
  }
  val counter = if (showCounter && maxLength != null) "$rawLength / $maxLength" else null
  return when {
    statusMessage != null && counter != null -> "$statusMessage · $counter"
    statusMessage != null -> statusMessage
    else -> counter
  }
}

internal fun keyboardOptionsFor(format: TextFieldFormat): KeyboardOptions =
  when (format) {
    TextFieldFormat.PhoneNumber,
    TextFieldFormat.BankCard,
    TextFieldFormat.Numeric,
    -> KeyboardOptions(keyboardType = KeyboardType.Number)
    is TextFieldFormat.VerificationCode -> KeyboardOptions(
      keyboardType = if (format.allowsAlphabet) KeyboardType.Ascii else KeyboardType.Number,
    )
    is TextFieldFormat.Amount -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
    TextFieldFormat.Email -> KeyboardOptions(keyboardType = KeyboardType.Email)
    is TextFieldFormat.Password -> KeyboardOptions(keyboardType = KeyboardType.Password)
    TextFieldFormat.IdCard,
    TextFieldFormat.Alphabetic,
    TextFieldFormat.AlphaNumeric,
    is TextFieldFormat.Custom,
    TextFieldFormat.Plain,
    -> KeyboardOptions(keyboardType = KeyboardType.Ascii)
  }

internal fun visualTransformationFor(format: TextFieldFormat): VisualTransformation =
  when (format) {
    is TextFieldFormat.Password -> PasswordVisualTransformation()
    else -> VisualTransformation.None
  }
