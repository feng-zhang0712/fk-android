package com.fk.sample.ui.textfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.textfield.CountedTextArea
import com.fk.ui.textfield.CountedTextConfiguration
import com.fk.ui.textfield.FkTextField
import com.fk.ui.textfield.FkTextFields
import com.fk.ui.textfield.OtpConfiguration
import com.fk.ui.textfield.OtpSlotStyle
import com.fk.ui.textfield.OtpTextField
import com.fk.ui.textfield.TextFieldConfiguration
import com.fk.ui.textfield.TextFieldFormat
import com.fk.ui.textfield.TextFieldMessages
import com.fk.ui.textfield.TextFieldValidationTrigger
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Smoke demo for Phase E2 textfield (OTP + validated formats + counter).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextFieldDemoScreen(
  onBack: () -> Unit,
) {
  val metrics = fkMetrics()
  var demo by remember { mutableStateOf(DemoTab.Validated) }
  var phone by remember { mutableStateOf("") }
  var phoneRaw by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var otp by remember { mutableStateOf("") }
  var otpDone by remember { mutableStateOf<String?>(null) }
  var otpStyle by remember { mutableStateOf(OtpSlotStyle.Boxes) }
  var bio by remember { mutableStateOf("") }

  Scaffold(
    topBar = { SampleTopBar(title = "TextField", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(metrics.spacingM)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
    ) {
      Text("TextField package v${FkTextFields.VERSION}", style = fkTextStyle(FkTextStyle.Footnote))
      Text(
        "Formatting · validation · OTP · counters",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      HorizontalDivider()

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      ) {
        DemoTab.entries.forEach { tab ->
          FilterChip(
            selected = demo == tab,
            onClick = { demo = tab },
            label = { Text(tab.label) },
          )
        }
      }
      HorizontalDivider()

      when (demo) {
        DemoTab.Validated -> {
          Text("Phone (3-4-4, validate on blur)", style = fkTextStyle(FkTextStyle.Subheadline))
          FkTextField(
            value = phone,
            onValueChange = { phone = it },
            onRawChange = { phoneRaw = it },
            modifier = Modifier.fillMaxWidth(),
            configuration = TextFieldConfiguration(
              format = TextFieldFormat.PhoneNumber,
              validationTrigger = TextFieldValidationTrigger.OnBlur,
              messages = TextFieldMessages(helper = "CN mobile number"),
            ),
            label = "Phone",
            placeholder = "138 1234 5678",
          )
          Text(
            "raw=$phoneRaw",
            style = fkTextStyle(FkTextStyle.Caption1),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
          )

          Text("Email (on change)", style = fkTextStyle(FkTextStyle.Subheadline))
          FkTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            configuration = TextFieldConfiguration(
              format = TextFieldFormat.Email,
              validationTrigger = TextFieldValidationTrigger.OnChange,
              messages = TextFieldMessages(helper = "Lowercased automatically"),
            ),
            label = "Email",
          )

          Text("Password (strength)", style = fkTextStyle(FkTextStyle.Subheadline))
          FkTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            configuration = TextFieldConfiguration(
              format = TextFieldFormat.Password(
                minLength = 8,
                maxLength = 32,
                validatesStrength = true,
              ),
              validationTrigger = TextFieldValidationTrigger.OnBlur,
              messages = TextFieldMessages(helper = "Upper + lower + digit"),
              showCounter = true,
              maxLength = 32,
            ),
            label = "Password",
          )
        }
        DemoTab.Otp -> {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
          ) {
            OtpSlotStyle.entries.forEach { style ->
              FilterChip(
                selected = otpStyle == style,
                onClick = { otpStyle = style },
                label = { Text(style.name) },
              )
            }
          }
          OtpTextField(
            value = otp,
            onValueChange = {
              otp = it
              otpDone = null
            },
            configuration = OtpConfiguration(length = 6, slotStyle = otpStyle),
            onCompleted = { otpDone = it },
          )
          Text(
            text = otpDone?.let { "Completed: $it" } ?: "code=$otp",
            style = fkTextStyle(FkTextStyle.Caption1),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
          )
        }
        DemoTab.Counter -> {
          CountedTextArea(
            value = bio,
            onValueChange = { bio = it },
            configuration = CountedTextConfiguration(maxLength = 80),
            label = "Bio",
            placeholder = "Tell us about yourself",
          )
        }
      }
    }
  }
}

private enum class DemoTab(val label: String) {
  Validated("Validated"),
  Otp("OTP"),
  Counter("Counter"),
}
