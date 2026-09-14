package com.fk.sample.ui.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.flow.FlowKit
import com.fk.ui.flow.FlowStepItem
import com.fk.ui.flow.FlowStepState
import com.fk.ui.flow.FkStepIndicator
import com.fk.ui.flow.FkTimeline
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Demo for Phase G5 flow visualization (StepIndicator / Timeline).
 */
@Composable
fun FlowDemoScreen(
  onBack: () -> Unit,
) {
  val metrics = fkMetrics()
  var checkoutIndex by remember { mutableIntStateOf(1) }

  Scaffold(
    topBar = { SampleTopBar(title = "Flow", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(metrics.spacingM),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingM),
    ) {
      Text(
        text = "FlowKit ${FlowKit.VERSION}",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )

      Section("StepIndicator (checkout)")
      Text(
        text = "Tap a step to move currentIndex. Special states stay explicit.",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      FkStepIndicator(
        items = listOf(
          FlowStepItem("cart", "Cart"),
          FlowStepItem("address", "Address"),
          FlowStepItem("pay", "Payment"),
          FlowStepItem("done", "Done"),
        ),
        currentStepIndex = checkoutIndex,
        onStepClick = { id ->
          val next = listOf("cart", "address", "pay", "done").indexOf(id)
          if (next >= 0) checkoutIndex = next
        },
        modifier = Modifier.fillMaxWidth(),
      )
      Text(
        text = "currentStepIndex=$checkoutIndex",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )

      HorizontalDivider(color = fkColor(FkColorRole.Outline))
      Section("Timeline (logistics)")
      FkTimeline(
        items = listOf(
          FlowStepItem(
            id = "placed",
            title = "Order placed",
            caption = "Warehouse accepted",
            timestampText = "Mon 09:12",
            state = FlowStepState.Completed,
          ),
          FlowStepItem(
            id = "shipped",
            title = "Shipped",
            subtitle = "Left Shenzhen hub",
            timestampText = "Mon 18:40",
            state = FlowStepState.Completed,
          ),
          FlowStepItem(
            id = "transit",
            title = "In transit",
            subtitle = "ETA Wed",
            timestampText = "Tue 08:05",
            state = FlowStepState.Current,
          ),
          FlowStepItem(
            id = "deliver",
            title = "Out for delivery",
            state = FlowStepState.Upcoming,
          ),
        ),
        modifier = Modifier.fillMaxWidth(),
      )

      HorizontalDivider(color = fkColor(FkColorRole.Outline))
      Section("Timeline (error + skipped)")
      FkTimeline(
        items = listOf(
          FlowStepItem("kyc", "Identity", state = FlowStepState.Completed, timestampText = "Day 1"),
          FlowStepItem("review", "Manual review", state = FlowStepState.Error, subtitle = "Document blurry"),
          FlowStepItem("extra", "Extra check", state = FlowStepState.Skipped),
          FlowStepItem("done", "Approved", state = FlowStepState.Upcoming),
        ),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun Section(title: String) {
  Text(
    text = title,
    style = fkTextStyle(FkTextStyle.Title3).copy(fontWeight = FontWeight.SemiBold),
    color = fkColor(FkColorRole.OnSurface),
  )
}
