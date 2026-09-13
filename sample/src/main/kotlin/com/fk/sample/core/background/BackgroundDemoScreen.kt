package com.fk.sample.core.background

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.background.Background
import com.fk.core.background.BackgroundAppRefreshRequest
import com.fk.core.background.BackgroundProcessingRequest
import com.fk.core.background.BackgroundTaskConfiguration
import com.fk.core.background.BackgroundTaskError
import com.fk.core.background.BackgroundTaskKind
import com.fk.core.background.BackgroundTaskRegistration
import com.fk.core.background.BackgroundWorkInfo
import com.fk.core.background.BackgroundWorkState
import com.fk.sample.ui.SampleTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Smoke demo for Phase B5 background (enqueue + observe WorkManager workers).
 */
@Composable
fun BackgroundDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val manager = remember {
    Background.create(
      context.applicationContext,
      BackgroundTaskConfiguration(
        allowsMultipleInstall = true,
        logScheduling = true,
      ),
    ).also { bg ->
      bg.registerAppRefresh(REFRESH_ID) { handle ->
        delay(400)
        !handle.isExpired
      }
      bg.registerProcessing(PROCESSING_ID) { handle ->
        delay(600)
        !handle.isExpired
      }
      bg.installRegistrations(
        listOf(
          BackgroundTaskRegistration(REFRESH_ID, BackgroundTaskKind.AppRefresh),
          BackgroundTaskRegistration(PROCESSING_ID, BackgroundTaskKind.Processing),
        ),
      )
    }
  }

  var status by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }
  var refreshInfo by remember { mutableStateOf<BackgroundWorkInfo?>(null) }
  var processingInfo by remember { mutableStateOf<BackgroundWorkInfo?>(null) }
  var shortWorkNote by remember { mutableStateOf("—") }

  LaunchedEffect(manager) {
    manager.observeWorkInfo(REFRESH_ID).collect { refreshInfo = it }
  }
  LaunchedEffect(manager) {
    manager.observeWorkInfo(PROCESSING_ID).collect { processingInfo = it }
  }

  Scaffold(
    topBar = { SampleTopBar(title = "Background", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Background package v${Background.VERSION}")
      Text("WorkManager refresh / processing · observe · short in-process work")
      HorizontalDivider()
      Text("Status: $status")
      Text("Refresh: ${refreshInfo.format()}")
      Text("Processing: ${processingInfo.format()}")
      Text("Short work: $shortWorkNote")
      Text("Detail:\n$detail")

      Button(
        onClick = {
          scope.launch {
            try {
              status = "Scheduling refresh…"
              manager.scheduleAppRefresh(
                BackgroundAppRefreshRequest(
                  identifier = REFRESH_ID,
                  earliestBeginEpochMs = System.currentTimeMillis() + 2_000L,
                ),
              )
              val pending = manager.pendingSummaries()
              status = "Refresh enqueued"
              detail = pending.joinToString("\n") { summary ->
                "${summary.identifier} (${summary.kind}) state=${summary.state}"
              }.ifBlank { "No pending summaries yet" }
            } catch (e: BackgroundTaskError) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Enqueue app refresh (+2s)")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              status = "Scheduling processing…"
              manager.scheduleProcessing(
                BackgroundProcessingRequest(
                  identifier = PROCESSING_ID,
                  earliestBeginEpochMs = System.currentTimeMillis() + 2_000L,
                  requiresNetworkConnectivity = false,
                  requiresCharging = false,
                ),
              )
              val pending = manager.pendingSummaries()
              status = "Processing enqueued"
              detail = pending.joinToString("\n") { summary ->
                "${summary.identifier} (${summary.kind}) state=${summary.state}"
              }.ifBlank { "No pending summaries yet" }
            } catch (e: BackgroundTaskError) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Enqueue processing (+2s)")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              manager.cancelScheduledTask(REFRESH_ID)
              manager.cancelScheduledTask(PROCESSING_ID)
              status = "Cancelled"
              detail = "Cancelled refresh + processing unique work"
            } catch (e: BackgroundTaskError) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Cancel scheduled tasks")
      }

      Button(
        onClick = {
          scope.launch {
            shortWorkNote = "Running…"
            val token = manager.beginBackgroundWork(name = "sample-flush") {
              delay(500)
            }
            status = "Short work started"
            detail = "token.isValid=${token.isValid} (no OS background budget on Android)"
            delay(600)
            shortWorkNote = if (token.isValid) "Still running" else "Completed (in-process)"
            token.end()
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("beginBackgroundWork (in-process)")
      }

      Button(
        onClick = {
          scope.launch {
            val pending = manager.pendingSummaries()
            status = "Pending"
            detail = if (pending.isEmpty()) {
              "No pending / running unique work"
            } else {
              pending.joinToString("\n") { s ->
                buildString {
                  append(s.identifier).append(" kind=").append(s.kind)
                  append(" state=").append(s.state)
                  append(" network=").append(s.requiresNetworkConnectivity)
                  append(" charging=").append(s.requiresCharging)
                }
              }
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("List pending summaries")
      }
    }
  }
}

private const val REFRESH_ID: String = "com.fk.sample.background.refresh"
private const val PROCESSING_ID: String = "com.fk.sample.background.processing"

private fun BackgroundWorkInfo?.format(): String {
  if (this == null) return "—"
  return "${state.name} attempts=$attemptCount id=${workId ?: "—"}"
}
