package com.fk.sample.ui.callout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.callout.Callout
import com.fk.ui.callout.CalloutAnchorState
import com.fk.ui.callout.CalloutController
import com.fk.ui.callout.CalloutHost
import com.fk.ui.callout.calloutAnchor
import com.fk.ui.callout.rememberCalloutController
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

@Composable
internal fun CalloutScenarioScaffold(
  title: String,
  description: String,
  onBack: () -> Unit,
  controller: CalloutController = rememberCalloutController(),
  content: @Composable ColumnScope.(
    controller: CalloutController,
    log: (String) -> Unit,
    lastLog: String,
  ) -> Unit,
) {
  var lastLog by remember { mutableStateOf("—") }
  val metrics = fkMetrics()

  CalloutHost(controller = controller) {
    Scaffold(
      topBar = { SampleTopBar(title = title, onBack = onBack) },
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(metrics.spacingM),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        Text(
          "Callout v${Callout.VERSION}",
          style = fkTextStyle(FkTextStyle.Footnote),
        )
        Text(
          description,
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        Text(
          "Log: $lastLog",
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        content(controller, { lastLog = it }, lastLog)
      }
    }
  }
}

@Composable
internal fun DemoAnchorButton(
  label: String,
  anchor: CalloutAnchorState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    onClick = onClick,
    modifier = modifier.calloutAnchor(anchor),
  ) {
    Text(label)
  }
}

@Composable
internal fun DemoAnchorChip(
  label: String,
  anchor: CalloutAnchorState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val metrics = fkMetrics()
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.calloutAnchor(anchor),
  ) {
    Text(label, modifier = Modifier.padding(horizontal = metrics.spacingXs))
  }
}

@Composable
internal fun DismissRow(controller: CalloutController) {
  TextButton(onClick = { controller.dismissActive() }) {
    Text("Dismiss active")
  }
}

@Composable
internal fun SectionLabel(text: String) {
  Text(
    text = text,
    style = fkTextStyle(FkTextStyle.Subheadline),
    color = fkColor(FkColorRole.OnSurface),
    modifier = Modifier.padding(top = fkMetrics().spacingS),
  )
}

@Composable
internal fun CenterStage(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(fkColor(FkColorRole.Surface))
      .border(1.dp, fkColor(FkColorRole.Outline).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
      .padding(fkMetrics().spacingL),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}
