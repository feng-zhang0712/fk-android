package com.fk.sample.core.logging

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.logging.Logging
import com.fk.core.logging.LogLevel
import com.fk.sample.ui.SampleTopBar
import java.io.File

/**
 * Smoke demo for Phase A4 logging (Logcat + small file sink).
 */
@Composable
fun LoggingDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val logDir = remember {
    File(context.applicationContext.filesDir, "fk_sample_logs")
  }
  val logger = remember {
    Logging.logcatAndFileLogger(
      context = context,
      tag = "FkSampleLog",
      minimumLevel = LogLevel.Verbose,
      logDirectory = logDir,
      maxFileBytes = 64L * 1024,
      maxTotalBytes = 256L * 1024,
    )
  }
  var status by remember { mutableStateOf("Idle — check Logcat tag FkSampleLog") }
  var filePreview by remember { mutableStateOf("(empty)") }
  var fileCount by remember { mutableStateOf(0) }

  fun refreshFilePreview() {
    val sink = logger.fileSink
    fileCount = sink?.logFiles()?.size ?: 0
    val recent = sink?.readRecent(800).orEmpty()
    filePreview = recent.ifBlank { "(empty)" }
  }

  Scaffold(
    topBar = { SampleTopBar(title = "Logging", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Logging package v${Logging.VERSION}")
      Text("Sinks: LogcatSink + FileLogSink (caps 64KiB / 256KiB)")
      Text("Dir: ${logDir.absolutePath}")
      HorizontalDivider()
      Text("Status: $status")
      Text("File count: $fileCount")
      Text("File preview:\n$filePreview")

      Button(
        onClick = {
          logger.verbose(fields = mapOf("step" to "1")) { "Verbose trace" }
          logger.debug(fields = mapOf("feature" to "logging")) { "Debug detail" }
          logger.info(fields = mapOf("userId" to "demo")) { "Info event" }
          logger.warning { "Warning condition" }
          logger.error(
            fields = mapOf("code" to "E42"),
            throwable = IllegalStateException("demo failure"),
          ) { "Error with throwable" }
          status = "Wrote 5 levels to Logcat + file"
          logger.fileSink?.awaitIdle()
          refreshFilePreview()
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Emit sample logs")
      }

      Button(
        onClick = {
          refreshFilePreview()
          status = "Refreshed file preview"
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Refresh file preview")
      }

      Button(
        onClick = {
          logger.fileSink?.clear()
          refreshFilePreview()
          status = "Cleared log files"
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Clear log files")
      }
    }
  }
}
