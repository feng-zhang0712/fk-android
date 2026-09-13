package com.fk.sample.core.app

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.app.App
import com.fk.core.app.AppError
import com.fk.core.app.PatternRouteHandler
import com.fk.core.app.RemoteVersionInfo
import com.fk.core.app.RouteHandlingResult
import com.fk.core.app.StartupTask
import com.fk.core.app.StartupTaskPriority
import com.fk.sample.ui.SampleTopBar
import kotlinx.coroutines.launch

/**
 * Smoke demo for Phase C3 app infra (version string + deeplink parse).
 */
@Composable
fun AppDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val app = remember { App.create(context.applicationContext) }

  var status by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }
  var lifecycleLabel by remember { mutableStateOf(app.lifecycle.state.name) }

  DisposableEffect(app) {
    val token = app.lifecycle.observe { lifecycleLabel = it.name }
    onDispose { token.cancel() }
  }

  Scaffold(
    topBar = { SampleTopBar(title = "App", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("App package v${App.VERSION}")
      Text("Version · deeplink · lifecycle · analytics · startup")
      HorizontalDivider()
      Text("Status: $status")
      Text("Lifecycle: $lifecycleLabel")
      Text("Detail:\n$detail")

      Button(
        onClick = {
          val info = app.info
          status = "Version"
          detail = buildString {
            append("label=").append(info.versionLabel()).append('\n')
            append("versionName=").append(info.versionName).append('\n')
            append("versionCode=").append(info.versionCode).append('\n')
            append("package=").append(info.packageName).append('\n')
            append("channel=").append(info.channel).append('\n')
            append("env=").append(info.environment).append('\n')
            append("device=").append(info.deviceModel).append('\n')
            append("sdk=").append(info.sdkInt)
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Show version / AppInfo")
      }

      Button(
        onClick = {
          try {
            val route = app.deeplinks.parse(SAMPLE_DEEPLINK)
            status = "Parsed"
            detail = buildString {
              append("uri=").append(route.uri).append('\n')
              append("host=").append(route.host).append('\n')
              append("path=").append(route.path).append('\n')
              append("segments=").append(route.pathSegments).append('\n')
              append("query=").append(route.query)
            }
          } catch (e: AppError) {
            status = "Error"
            detail = e.message ?: e.toString()
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Parse sample deeplink")
      }

      Button(
        onClick = {
          app.deeplinks.register(
            PatternRouteHandler(
              id = "product",
              pathPattern = "/product/*",
            ) { ctx ->
              status = "Routed"
              detail = "Handled product id=${ctx.pathSegments.getOrNull(1)} ref=${ctx.query["ref"]}"
              RouteHandlingResult.Handled
            },
          )
          val result = app.deeplinks.open(SAMPLE_DEEPLINK)
          if (result is RouteHandlingResult.NotHandled) {
            status = "Not handled"
            detail = result.toString()
          } else if (result is RouteHandlingResult.Failed) {
            status = "Failed"
            detail = result.message
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Register route + open")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              val result = app.versionChecker.checkForUpdate {
                RemoteVersionInfo(
                  versionName = "9.9.9",
                  versionCode = app.info.versionCode + 1,
                  isForceUpdate = false,
                  releaseNotes = "Sample remote version",
                )
              }
              status = "Version check"
              detail = buildString {
                append("local=").append(result.local.versionName)
                append(" (").append(result.local.versionCode).append(')').append('\n')
                append("remote=").append(result.remote.versionName).append('\n')
                append("decision=").append(result.decision)
              }
            } catch (e: Exception) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Check update (mock remote)")
      }

      Button(
        onClick = {
          app.analytics.trackPageView("app_demo")
          app.analytics.trackClick("show_version", page = "app_demo")
          status = "Analytics"
          detail = app.analytics.pendingEvents().joinToString("\n") { e ->
            "${e.type} ${e.name} ${e.parameters}"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Track page + click")
      }

      Button(
        onClick = {
          scope.launch {
            val log = StringBuilder()
            app.startup.register(
              StartupTask(id = "task-high", priority = StartupTaskPriority.High) {
                log.appendLine("high")
              },
            )
            app.startup.register(
              StartupTask(id = "task-normal", delayMs = 50L) {
                log.appendLine("normal")
              },
            )
            app.startup.runAll()
            status = "Startup done"
            detail = log.toString().ifBlank { "(no output)" }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Run startup tasks")
      }
    }
  }
}

private const val SAMPLE_DEEPLINK: String = "https://example.com/product/42?ref=ad"
