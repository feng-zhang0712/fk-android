package com.fk.sample.core.permissions

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.permissions.PermissionKind
import com.fk.core.permissions.PermissionPrePrompt
import com.fk.core.permissions.PermissionPrePromptHandler
import com.fk.core.permissions.PermissionRequest
import com.fk.core.permissions.PermissionStatus
import com.fk.core.permissions.Permissions
import com.fk.sample.ui.SampleTopBar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Smoke demo for Phase B3 permissions (check → request → settings).
 */
@Composable
fun PermissionsDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val activity = context as ComponentActivity
  val scope = rememberCoroutineScope()
  val manager = remember { Permissions.create(context.applicationContext) }

  var statusText by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }
  var pendingPrompt by remember {
    mutableStateOf<Pair<PermissionPrePrompt, CompletableDeferred<Boolean>>?>(null)
  }

  DisposableEffect(manager) {
    manager.setPrePromptHandler(
      PermissionPrePromptHandler { prompt ->
        val deferred = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main.immediate) {
          pendingPrompt = prompt to deferred
        }
        try {
          deferred.await()
        } finally {
          withContext(Dispatchers.Main.immediate) {
            pendingPrompt = null
          }
        }
      },
    )
    onDispose { manager.setPrePromptHandler(null) }
  }

  pendingPrompt?.let { (prompt, deferred) ->
    AlertDialog(
      onDismissRequest = { deferred.complete(false) },
      title = { Text(prompt.title) },
      text = { Text(prompt.message) },
      confirmButton = {
        TextButton(onClick = { deferred.complete(true) }) {
          Text(prompt.confirmTitle)
        }
      },
      dismissButton = {
        TextButton(onClick = { deferred.complete(false) }) {
          Text(prompt.cancelTitle)
        }
      },
    )
  }

  fun showStatus(kind: PermissionKind, label: String) {
    val status = manager.status(kind, activity)
    statusText = label
    detail = "$kind → $status (granted=${status == PermissionStatus.Granted})"
  }

  Scaffold(
    topBar = { SampleTopBar(title = "Permissions", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Permissions package v${Permissions.VERSION}")
      Text("Check → request (with optional pre-prompt) → Settings")
      HorizontalDivider()
      Text("Status: $statusText")
      Text("Detail:\n$detail")

      Button(
        onClick = { showStatus(PermissionKind.Camera, "Camera status") },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Check camera status")
      }

      Button(
        onClick = {
          scope.launch {
            val result = manager.request(
              activity,
              PermissionRequest(
                kind = PermissionKind.Camera,
                prePrompt = PermissionPrePrompt(
                  title = "Camera access",
                  message = "Sample needs the camera to demonstrate the permission façade.",
                ),
              ),
            )
            statusText = if (result.isGranted) "Camera granted" else "Camera not granted"
            detail = buildString {
              append("status=").append(result.status)
              result.error?.let { append("\nerror=").append(it) }
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Request camera (with pre-prompt)")
      }

      Button(
        onClick = {
          scope.launch {
            val result = manager.request(activity, PermissionKind.Notifications)
            statusText = if (result.isGranted) "Notifications granted" else "Notifications not granted"
            detail = "status=${result.status}"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Request notifications")
      }

      Button(
        onClick = {
          val opened = manager.openAppSettings()
          statusText = if (opened) "Opened Settings" else "Failed to open Settings"
          detail = "Use the system screen to grant/revoke, then return and re-check."
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Open app settings")
      }
    }
  }
}
