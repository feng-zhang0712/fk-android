package com.fk.ui.textfield

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkStatusColor

/**
 * Slot-based OTP / verification-code field.
 *
 * Conceptually aligned with iOS `FKCodeTextField`.
 *
 * @param value Digits (or alphanumeric) code string; truncated to [OtpConfiguration.length].
 * @param onValueChange Called on each change with the sanitized code.
 * @param onCompleted Called when the code newly reaches [OtpConfiguration.length].
 */
@Composable
fun OtpTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  configuration: OtpConfiguration = OtpConfiguration(),
  isError: Boolean = false,
  enabled: Boolean = true,
  onCompleted: ((String) -> Unit)? = null,
  slotSize: Dp = 44.dp,
) {
  val length = configuration.length.coerceAtLeast(1)
  val sanitized = remember(value, configuration) {
    TextFieldFormatter.format(
      text = value,
      format = TextFieldFormat.VerificationCode(
        length = length,
        allowsAlphabet = configuration.allowsAlphabet,
      ),
    ).rawText
  }
  val focusRequester = remember { FocusRequester() }
  val metrics = fkMetrics()
  val outline = when {
    isError -> fkStatusColor(FkStatusSemantic.Error)
    else -> fkColor(FkColorRole.Outline)
  }
  val active = fkColor(FkColorRole.Primary)
  val onSurface = fkColor(FkColorRole.OnSurface)
  val density = LocalDensity.current
  var lastCompleted by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(sanitized, length) {
    if (sanitized.length == length) {
      if (sanitized != lastCompleted) {
        lastCompleted = sanitized
        onCompleted?.invoke(sanitized)
      }
    } else {
      lastCompleted = null
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clickable(enabled = enabled) { focusRequester.requestFocus() },
  ) {
    BasicTextField(
      value = sanitized,
      onValueChange = { incoming ->
        if (!enabled) return@BasicTextField
        val next = TextFieldFormatter.format(
          text = incoming,
          format = TextFieldFormat.VerificationCode(
            length = length,
            allowsAlphabet = configuration.allowsAlphabet,
          ),
        ).rawText
        onValueChange(next)
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(slotSize)
        .alpha(0.01f)
        .focusRequester(focusRequester),
      enabled = enabled,
      textStyle = TextStyle(color = Color.Transparent),
      keyboardOptions = KeyboardOptions(
        keyboardType = if (configuration.allowsAlphabet) {
          KeyboardType.Ascii
        } else {
          KeyboardType.NumberPassword
        },
      ),
      singleLine = true,
      cursorBrush = SolidColor(Color.Transparent),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingS, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(length) { index ->
        val char = sanitized.getOrNull(index)?.toString().orEmpty()
        val isActive = sanitized.length == index
        when (configuration.slotStyle) {
          OtpSlotStyle.Boxes -> {
            Box(
              modifier = Modifier
                .size(slotSize)
                .border(
                  width = 1.dp,
                  color = when {
                    isError -> outline
                    isActive -> active
                    else -> outline
                  },
                  shape = RoundedCornerShape(metrics.radiusSmall),
                ),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = char,
                style = TextStyle(
                  fontSize = 18.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = onSurface,
                  textAlign = TextAlign.Center,
                ),
              )
            }
          }
          OtpSlotStyle.Underlines -> {
            val underlineHeight = with(density) { 2.dp.toPx() }
            Box(
              modifier = Modifier
                .width(slotSize)
                .height(slotSize)
                .drawBehind {
                  drawLine(
                    color = when {
                      isError -> outline
                      isActive -> active
                      else -> outline
                    },
                    start = Offset(0f, size.height - underlineHeight / 2f),
                    end = Offset(size.width, size.height - underlineHeight / 2f),
                    strokeWidth = underlineHeight,
                  )
                },
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = char,
                style = TextStyle(
                  fontSize = 18.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = onSurface,
                  textAlign = TextAlign.Center,
                ),
              )
            }
          }
        }
      }
    }
  }
}
