package com.fk.sample.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.sheet.FkBottomSheet
import com.fk.ui.sheet.FkCenterSheet
import com.fk.ui.sheet.FkSheet
import com.fk.ui.sheet.SheetConfiguration
import com.fk.ui.sheet.SheetDetent
import com.fk.ui.sheet.rememberFkBottomSheetState
import com.fk.ui.sheet.rememberSheetController
import com.fk.ui.sheet.shouldSkipPartiallyExpanded
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Smoke demo for Phase E3 sheet (multi-detent bottom + center card).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SheetDemoScreen(
  onBack: () -> Unit,
) {
  val metrics = fkMetrics()
  var bottomVisible by remember { mutableStateOf(false) }
  var centerVisible by remember { mutableStateOf(false) }
  var preset by remember { mutableStateOf(BottomPreset.MediumLarge) }

  val bottomConfig = when (preset) {
    BottomPreset.MediumLarge -> SheetConfiguration.BottomSheetDefault
    BottomPreset.LargeOnly -> SheetConfiguration.BottomSheetLarge
    BottomPreset.Fit -> SheetConfiguration.BottomSheetFit
    BottomPreset.Fraction -> SheetConfiguration(
      detents = listOf(SheetDetent.Fraction(0.35f), SheetDetent.Large),
    )
  }

  Scaffold(
    topBar = { SampleTopBar(title = "Sheet", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(metrics.spacingM),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
    ) {
      Text("Sheet package v${FkSheet.VERSION}", style = fkTextStyle(FkTextStyle.Footnote))
      Text(
        "Multi-detent bottom sheet · center card",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      HorizontalDivider()

      Text("Bottom preset", style = fkTextStyle(FkTextStyle.Subheadline))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      ) {
        BottomPreset.entries.forEach { entry ->
          FilterChip(
            selected = preset == entry,
            onClick = { preset = entry },
            label = { Text(entry.label) },
          )
        }
      }

      Button(
        onClick = { bottomVisible = true },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Show bottom sheet")
      }
      Button(
        onClick = { centerVisible = true },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Show center card")
      }
    }
  }

  if (bottomVisible) {
    // State must live inside the visible branch so Hidden is not reused on reopen.
    val sheetState = rememberFkBottomSheetState(bottomConfig)
    val skipPartial = shouldSkipPartiallyExpanded(bottomConfig.detents)
    val controller = rememberSheetController(sheetState, skipPartiallyExpanded = skipPartial)

    FkBottomSheet(
      onDismissRequest = { bottomVisible = false },
      configuration = bottomConfig,
      sheetState = sheetState,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(metrics.spacingM),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        Text("Bottom sheet", style = fkTextStyle(FkTextStyle.Headline))
        Text(
          "Drag handle · swipe · scrim dismiss. Try Expand / Partial when available.",
          style = fkTextStyle(FkTextStyle.Body),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        if (!skipPartial) {
          TextButton(onClick = { controller.partialExpand() }) {
            Text("Partial expand")
          }
        }
        TextButton(onClick = { controller.expand() }) {
          Text("Expand")
        }
        Button(
          onClick = {
            controller.hide { bottomVisible = false }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Close")
        }
      }
    }
  }

  if (centerVisible) {
    FkCenterSheet(
      onDismissRequest = { centerVisible = false },
      configuration = SheetConfiguration.CenterCard,
    ) {
      Column(
        modifier = Modifier.padding(metrics.spacingL),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        Text("Center card", style = fkTextStyle(FkTextStyle.Headline))
        Text(
          "Dim backdrop · tap outside to dismiss.",
          style = fkTextStyle(FkTextStyle.Body),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        Button(
          onClick = { centerVisible = false },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("OK")
        }
      }
    }
  }
}

private enum class BottomPreset(val label: String) {
  MediumLarge("Medium+Large"),
  LargeOnly("Large"),
  Fit("Fit"),
  Fraction("35%+Large"),
}
