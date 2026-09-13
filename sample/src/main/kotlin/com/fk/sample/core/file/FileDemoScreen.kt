package com.fk.sample.core.file

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.file.DownloadRequest
import com.fk.core.file.FileTransferError
import com.fk.core.file.FileTransfers
import com.fk.core.file.TransferState
import com.fk.sample.ui.SampleTopBar
import kotlinx.coroutines.launch
import java.io.File

/**
 * Smoke demo for Phase C1 file transfers (download + pause / resume).
 */
@Composable
fun FileDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val transfers = remember { FileTransfers.create(context.applicationContext) }
  val downloadDir = remember {
    File(context.cacheDir, "fk-sample-downloads").also { it.mkdirs() }
  }

  var status by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }
  var taskId by remember { mutableIntStateOf(-1) }
  var progress by remember { mutableFloatStateOf(0f) }
  var stateLabel by remember { mutableStateOf("—") }

  LaunchedEffect(taskId) {
    if (taskId < 0) return@LaunchedEffect
    transfers.observe(taskId).collect { snapshot ->
      if (snapshot == null) return@collect
      stateLabel = snapshot.state.name
      progress = when {
        snapshot.totalBytes > 0L ->
          (snapshot.completedBytes.toFloat() / snapshot.totalBytes.toFloat()).coerceIn(0f, 1f)
        else -> progress
      }
      detail = buildString {
        append("taskId=").append(snapshot.id).append('\n')
        append("state=").append(snapshot.state).append('\n')
        append("bytes=").append(snapshot.completedBytes).append('/').append(snapshot.totalBytes)
        append('\n')
        append("path=").append(snapshot.destinationPath ?: "—")
        snapshot.errorMessage?.let { append('\n').append("error=").append(it) }
      }
      if (snapshot.state == TransferState.Completed) {
        status = "Completed"
      } else if (snapshot.state == TransferState.Failed) {
        status = "Failed"
      } else if (snapshot.state == TransferState.Paused) {
        status = "Paused"
      }
    }
  }

  Scaffold(
    topBar = { SampleTopBar(title = "File", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("FileTransfers package v${FileTransfers.VERSION}")
      Text("Resumable download · pause / resume · transfer persistence")
      HorizontalDivider()
      Text("Status: $status")
      Text("State: $stateLabel")
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
      )
      Text("Detail:\n$detail")

      Button(
        onClick = {
          scope.launch {
            try {
              status = "Starting download…"
              progress = 0f
              taskId = transfers.download(
                DownloadRequest(
                  sourceUrl = SAMPLE_URL,
                  destinationDirectory = downloadDir,
                  fileName = "sample.bin",
                ),
              )
              status = "Downloading"
            } catch (e: FileTransferError) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Start download (~1MB)")
      }

      Button(
        onClick = {
          scope.launch {
            if (taskId < 0) {
              status = "No task"
              return@launch
            }
            transfers.pauseDownload(taskId)
            status = "Pause requested"
          }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = taskId >= 0,
      ) {
        Text("Pause download")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              if (taskId < 0) {
                status = "No task"
                return@launch
              }
              transfers.resumeDownload(taskId)
              status = "Resuming"
            } catch (e: FileTransferError) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = taskId >= 0,
      ) {
        Text("Resume download")
      }

      Button(
        onClick = {
          scope.launch {
            if (taskId < 0) return@launch
            transfers.cancel(taskId)
            status = "Cancelled"
          }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = taskId >= 0,
      ) {
        Text("Cancel")
      }

      Button(
        onClick = {
          scope.launch {
            val pending = transfers.persistedTransfers()
            status = "Persisted (${pending.size})"
            detail = if (pending.isEmpty()) {
              "No persisted transfers"
            } else {
              pending.joinToString("\n") { p ->
                "#${p.id} ${p.state} ${p.completedBytes}/${p.totalBytes}\n  ${p.sourceUrl}"
              }
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("List persisted transfers")
      }
    }
  }
}

/** Public ~1MB payload that typically supports Range for pause/resume demos. */
private const val SAMPLE_URL: String = "https://httpbin.org/bytes/1048576"
