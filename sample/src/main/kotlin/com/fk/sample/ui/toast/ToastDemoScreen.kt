package com.fk.sample.ui.toast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle
import com.fk.ui.toast.Hud
import com.fk.ui.toast.Snackbar
import com.fk.ui.toast.Toast
import com.fk.ui.toast.ToastHost
import com.fk.ui.toast.ToastStyle
import com.fk.ui.toast.rememberToastController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Smoke demo for Phase D4 toast (replace-active / HUD / snackbar).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToastDemoScreen(
  onBack: () -> Unit,
) {
  val metrics = fkMetrics()
  val scope = rememberCoroutineScope()
  val controller = rememberToastController()
  var lastHandle by remember { mutableStateOf("—") }

  ToastHost(controller = controller) {
    Scaffold(
      topBar = { SampleTopBar(title = "Toast", onBack = onBack) },
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(metrics.spacingM),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        Text("Toast package v${Toast.VERSION}", style = fkTextStyle(FkTextStyle.Footnote))
        Text(
          "Single-slot replace-active · HUD · snackbar",
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        Text(
          "Last handle: $lastHandle · active=${controller.current != null}",
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        HorizontalDivider()

        Button(
          onClick = {
            scope.launch {
              repeat(5) { index ->
                val handle = controller.show(
                  message = "Burst #${index + 1} (replaces previous)",
                  style = ToastStyle.Info,
                )
                lastHandle = handle.id.take(8)
                delay(120)
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Burst 5 toasts (replace-active)")
        }

        Button(
          onClick = {
            val handle = Hud.showLoading(controller, message = "Saving…")
            lastHandle = handle.id.take(8)
            scope.launch {
              delay(1_800)
              controller.dismiss(handle.id)
              Hud.showSuccess(controller, "Saved")
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("HUD loading → success")
        }

        Button(
          onClick = {
            val handle = Snackbar.show(
              controller = controller,
              message = "Item archived",
              actionLabel = "Undo",
              onAction = { lastHandle = "undo" },
              style = ToastStyle.Normal,
            )
            lastHandle = handle.id.take(8)
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Snackbar with action")
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
          verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
        ) {
          listOf(
            ToastStyle.Success to "Success",
            ToastStyle.Error to "Error",
            ToastStyle.Warning to "Warning",
            ToastStyle.Info to "Info",
          ).forEach { (style, label) ->
            FilterChip(
              selected = false,
              onClick = {
                val handle = controller.show(message = "$label toast", style = style)
                lastHandle = handle.id.take(8)
              },
              label = { Text(label) },
            )
          }
        }

        Button(
          onClick = { controller.clear() },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Clear")
        }
      }
    }
  }
}
