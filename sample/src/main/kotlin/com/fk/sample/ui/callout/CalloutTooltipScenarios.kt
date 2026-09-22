package com.fk.sample.ui.callout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fk.ui.callout.CalloutAppearance
import com.fk.ui.callout.CalloutConfiguration
import com.fk.ui.callout.CalloutIcon
import com.fk.ui.callout.CalloutPlacement
import com.fk.ui.callout.FkTooltip
import com.fk.ui.callout.rememberCalloutAnchorState
import com.fk.ui.theme.fkMetrics

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalloutTooltipBasicsScreen(onBack: () -> Unit) {
  val metrics = fkMetrics()
  val textAnchor = rememberCalloutAnchorState("tooltip-text")
  val iconAnchor = rememberCalloutAnchorState("tooltip-icon")
  var placement by remember { mutableStateOf(CalloutPlacement.Top) }
  var style by remember { mutableStateOf(CalloutAppearance.Style.Dark) }

  CalloutScenarioScaffold(
    title = "Tooltip basics",
    description = "FkTooltip · placements, multiline, iconMessage, light & dark styles",
    onBack = onBack,
  ) { controller, log, _ ->
    SectionLabel("Placement")
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
    ) {
      listOf(
        CalloutPlacement.Top,
        CalloutPlacement.Bottom,
        CalloutPlacement.Start,
        CalloutPlacement.End,
        CalloutPlacement.Automatic,
      ).forEach { option ->
        FilterChip(
          selected = placement == option,
          onClick = { placement = option },
          label = { Text(option.name) },
        )
      }
    }

    SectionLabel("Style")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs)) {
      CalloutAppearance.Style.entries.forEach { option ->
        FilterChip(
          selected = style == option,
          onClick = { style = option },
          label = { Text(option.name) },
        )
      }
    }

    HorizontalDivider()
    CenterStage {
      DemoAnchorButton(
        label = "Show tooltip",
        anchor = textAnchor,
        onClick = {
          val config = CalloutConfiguration.tooltipDefault(placement).copy(
            appearance = CalloutAppearance(
              style = style,
              showsShadow = style == CalloutAppearance.Style.Light,
            ),
          )
          FkTooltip.show(
            controller = controller,
            message = "Short hint near the control.\nSupports multiline copy.",
            anchor = textAnchor,
            placement = placement,
            configuration = config,
          )
          log("FkTooltip.show · ${placement.name} · ${style.name}")
        },
      )
    }

    DemoAnchorChip(
      label = "Icon + message",
      anchor = iconAnchor,
      onClick = {
        FkTooltip.show(
          controller = controller,
          icon = CalloutIcon(imageVector = Icons.Outlined.Info),
          message = "Icon message tooltip with compact padding.",
          anchor = iconAnchor,
          placement = placement,
        )
        log("FkTooltip.show(iconMessage)")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DismissRow(controller)
  }
}
