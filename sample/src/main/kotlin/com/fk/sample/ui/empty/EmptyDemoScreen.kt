package com.fk.sample.ui.empty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.empty.Empty
import com.fk.ui.empty.EmptyAction
import com.fk.ui.empty.EmptyConfiguration
import com.fk.ui.empty.EmptyContent
import com.fk.ui.empty.EmptyInputs
import com.fk.ui.empty.EmptyPhase
import com.fk.ui.empty.EmptyScenario
import com.fk.ui.empty.EmptyStateHost
import com.fk.ui.empty.EmptyType
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Smoke demo for Phase D2 empty-state (loading / empty / error overlays).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyDemoScreen(
  onBack: () -> Unit,
) {
  val metrics = fkMetrics()
  var mode by remember { mutableStateOf(DemoMode.Loading) }
  var lastAction by remember { mutableStateOf("—") }

  val configuration = remember(mode) { configurationFor(mode) }
  val illustrationIcon = remember(mode) { iconFor(mode) }

  Scaffold(
    topBar = { SampleTopBar(title = "Empty", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(metrics.spacingM),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
    ) {
      Text("Empty package v${Empty.VERSION}", style = fkTextStyle(FkTextStyle.Footnote))
      Text(
        "phase=${phaseLabel(configuration.phase)} · type=${configuration.type.analyticsId}",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      Text(
        "Last action: $lastAction",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      HorizontalDivider()

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      ) {
        DemoMode.entries.forEach { entry ->
          FilterChip(
            selected = mode == entry,
            onClick = { mode = entry },
            label = { Text(entry.label) },
          )
        }
      }

      EmptyStateHost(
        configuration = configuration,
        onAction = { action: EmptyAction ->
          lastAction = "${action.id} · ${action.title}"
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(360.dp),
        illustration = {
          Icon(
            imageVector = illustrationIcon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = fkColor(FkColorRole.OnSurfaceSecondary),
          )
        },
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Text(
            "Host content visible when phase is Content",
            modifier = Modifier.padding(metrics.spacingM),
            style = fkTextStyle(FkTextStyle.Body),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
          )
        }
      }
    }
  }
}

private enum class DemoMode(val label: String) {
  Loading("Loading"),
  Empty("Empty"),
  Error("Error"),
  Offline("Offline"),
  NoResults("No results"),
  Content("Content"),
  Resolved("Resolver"),
}

private fun configurationFor(mode: DemoMode): EmptyConfiguration = when (mode) {
  DemoMode.Loading -> EmptyConfiguration(
    phase = EmptyPhase.Loading,
    type = EmptyType.Loading,
    content = EmptyContent(loadingMessage = "Loading…"),
  )
  DemoMode.Empty -> EmptyConfiguration.scenario(EmptyScenario.NoMessages)
  DemoMode.Error -> EmptyConfiguration.scenario(EmptyScenario.LoadFailed)
  DemoMode.Offline -> EmptyConfiguration.scenario(EmptyScenario.NoNetwork)
  DemoMode.NoResults -> EmptyConfiguration.scenario(EmptyScenario.NoSearchResult)
  DemoMode.Content -> EmptyConfiguration(phase = EmptyPhase.Content)
  DemoMode.Resolved -> EmptyConfiguration.resolved(
    EmptyInputs(
      dataLength = 0,
      isLoading = false,
      searchQuery = "kotlin",
    ),
  )
}

private fun iconFor(mode: DemoMode): ImageVector = when (mode) {
  DemoMode.Loading -> Icons.Outlined.Inbox
  DemoMode.Empty -> Icons.Outlined.Inbox
  DemoMode.Error -> Icons.Outlined.ErrorOutline
  DemoMode.Offline -> Icons.Outlined.CloudOff
  DemoMode.NoResults -> Icons.Outlined.SearchOff
  DemoMode.Content -> Icons.Outlined.Inbox
  DemoMode.Resolved -> Icons.Outlined.SearchOff
}

private fun phaseLabel(phase: EmptyPhase): String = when (phase) {
  EmptyPhase.Content -> "content"
  EmptyPhase.Loading -> "loading"
  EmptyPhase.Empty -> "empty"
  EmptyPhase.Error -> "error"
  is EmptyPhase.Custom -> "custom(${phase.id})"
}
