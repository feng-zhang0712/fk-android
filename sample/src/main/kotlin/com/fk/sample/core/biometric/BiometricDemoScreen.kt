package com.fk.sample.core.biometric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.fk.core.biometric.Biometric
import com.fk.core.biometric.BiometricError
import com.fk.core.biometric.BiometricPolicy
import com.fk.core.biometric.BiometricReason
import com.fk.sample.ui.SampleTopBar
import kotlinx.coroutines.launch

/**
 * Smoke demo for Phase B4 biometric (capability probe + BiometricPrompt).
 */
@Composable
fun BiometricDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val activity = context as FragmentActivity
  val scope = rememberCoroutineScope()
  val biometric = remember { Biometric.create(context.applicationContext) }

  var status by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }

  Scaffold(
    topBar = { SampleTopBar(title = "Biometric", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Biometric package v${Biometric.VERSION}")
      Text("Capability probe · BiometricPrompt · typed errors")
      HorizontalDivider()
      Text("Status: $status")
      Text("Detail:\n$detail")

      Button(
        onClick = {
          val capability = biometric.capability(BiometricPolicy.BiometricsOrDeviceCredential)
          status = if (capability.canAuthenticate) "Ready" else "Not ready"
          detail = buildString {
            append("canAuthenticate=").append(capability.canAuthenticate).append('\n')
            append("type=").append(capability.biometryType).append('\n')
            append("enrolled=").append(capability.isBiometryEnrolled).append('\n')
            append("deviceCredential=").append(capability.isDeviceCredentialSet).append('\n')
            append("policy=").append(capability.evaluatedPolicy).append('\n')
            append("probeError=").append(capability.probeError ?: "—")
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Probe capability")
      }

      Button(
        onClick = {
          val opened = biometric.openBiometricSettings()
          status = if (opened) "Opened settings" else "Failed to open settings"
          detail = "Enroll biometrics or set a device credential, then probe again."
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Open biometric / security settings")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              biometric.authenticate(
                activity = activity,
                reason = BiometricReason.unlockApp(),
                policy = BiometricPolicy.BiometricsOrDeviceCredential,
              )
              status = "Authenticated"
              detail = "Success"
            } catch (e: BiometricError) {
              status = e::class.simpleName ?: "BiometricError"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Authenticate (biometrics or device credential)")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              biometric.authenticate(
                activity = activity,
                reason = BiometricReason.confirmAction(),
                policy = BiometricPolicy.BiometricsOnly,
              )
              status = "Authenticated"
              detail = "Biometrics-only success"
            } catch (e: BiometricError) {
              status = e::class.simpleName ?: "BiometricError"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Authenticate (biometrics only)")
      }

      Button(
        onClick = {
          biometric.cancelAuthentication()
          status = "Cancel requested"
          detail = "In-flight prompt should end as AppCancelled"
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Cancel in-flight authentication")
      }
    }
  }
}
