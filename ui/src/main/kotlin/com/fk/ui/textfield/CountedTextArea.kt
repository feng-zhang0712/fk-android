package com.fk.ui.textfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Multiline text area with an optional character counter.
 *
 * Conceptually aligned with iOS `FKCountTextView`.
 */
@Composable
fun CountedTextArea(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  configuration: CountedTextConfiguration = CountedTextConfiguration(),
  label: String? = null,
  placeholder: String? = null,
  enabled: Boolean = true,
  isError: Boolean = false,
) {
  val metrics = fkMetrics()
  val maxLength = configuration.maxLength
  val atLimit = maxLength != null && value.length >= maxLength
  val counterColor = when {
    isError || atLimit -> fkStatusColor(FkStatusSemantic.Error)
    else -> fkColor(FkColorRole.OnSurfaceSecondary)
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(metrics.spacingXxs),
    horizontalAlignment = Alignment.End,
  ) {
    OutlinedTextField(
      value = value,
      onValueChange = { incoming ->
        val next = if (maxLength != null) incoming.truncate(maxLength) else incoming
        onValueChange(next)
      },
      modifier = Modifier.fillMaxWidth(),
      enabled = enabled,
      label = label?.let { { Text(it) } },
      placeholder = placeholder?.let { { Text(it) } },
      // At-limit is expected (enforced); do not paint the field as error solely for that.
      isError = isError,
      minLines = configuration.minLines.coerceAtLeast(1),
      maxLines = configuration.maxLines.coerceAtLeast(configuration.minLines),
    )
    if (configuration.showCounter && maxLength != null) {
      Text(
        text = "${value.length} / $maxLength",
        style = fkTextStyle(FkTextStyle.Caption2),
        color = counterColor,
        textAlign = TextAlign.End,
      )
    }
  }
}
