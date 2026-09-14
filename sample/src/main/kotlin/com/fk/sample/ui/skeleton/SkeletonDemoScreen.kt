package com.fk.sample.ui.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.fk.ui.skeleton.Skeleton
import com.fk.ui.skeleton.SkeletonAnimationMode
import com.fk.ui.skeleton.SkeletonConfiguration
import com.fk.ui.skeleton.SkeletonContainer
import com.fk.ui.skeleton.SkeletonListPlaceholder
import com.fk.ui.skeleton.SkeletonPresets
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Smoke demo for Phase D3 skeleton (list placeholder + presets).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkeletonDemoScreen(
  onBack: () -> Unit,
) {
  val metrics = fkMetrics()
  var mode by remember { mutableStateOf(DemoMode.ListRows) }
  var animation by remember { mutableStateOf(SkeletonAnimationMode.Shimmer) }
  val configuration = remember(animation) {
    SkeletonConfiguration(animationMode = animation)
  }

  Scaffold(
    topBar = { SampleTopBar(title = "Skeleton", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(metrics.spacingM),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
    ) {
      Text("Skeleton package v${Skeleton.VERSION}", style = fkTextStyle(FkTextStyle.Footnote))
      Text(
        "Shimmer / pulse placeholders · FkTheme colors",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      HorizontalDivider()

      Text("Preset", style = fkTextStyle(FkTextStyle.Subheadline))
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

      Text("Animation", style = fkTextStyle(FkTextStyle.Subheadline))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      ) {
        SkeletonAnimationMode.entries.forEach { entry ->
          FilterChip(
            selected = animation == entry,
            onClick = { animation = entry },
            label = { Text(entry.name) },
          )
        }
      }

      HorizontalDivider()

      SkeletonContainer(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        configuration = configuration,
      ) {
        when (mode) {
          DemoMode.ListRows -> {
            SkeletonListPlaceholder(
              count = 8,
              modifier = Modifier.fillMaxSize(),
              configuration = configuration,
            )
          }
          DemoMode.TextBlock -> {
            SkeletonPresets.TextBlock(
              modifier = Modifier.padding(metrics.spacingS),
              lines = 5,
              configuration = configuration,
            )
          }
          DemoMode.Card -> {
            SkeletonPresets.Card(
              modifier = Modifier.padding(metrics.spacingS),
              configuration = configuration,
            )
          }
          DemoMode.Grid -> {
            FlowRow(
              modifier = Modifier.padding(metrics.spacingS),
              horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
              verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
            ) {
              repeat(4) {
                SkeletonPresets.GridCell(configuration = configuration)
              }
            }
          }
        }
      }
    }
  }
}

private enum class DemoMode(val label: String) {
  ListRows("List rows"),
  TextBlock("Text"),
  Card("Card"),
  Grid("Grid"),
}
