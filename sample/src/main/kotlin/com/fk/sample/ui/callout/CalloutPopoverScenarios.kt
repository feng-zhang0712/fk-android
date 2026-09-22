@file:OptIn(ExperimentalLayoutApi::class)

package com.fk.sample.ui.callout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fk.ui.callout.CalloutAction
import com.fk.ui.callout.CalloutAppearance
import com.fk.ui.callout.CalloutCoachMarkContent
import com.fk.ui.callout.CalloutConfiguration
import com.fk.ui.callout.CalloutHeaderPanel
import com.fk.ui.callout.CalloutPlacement
import com.fk.ui.callout.FkPopover
import com.fk.ui.callout.rememberCalloutAnchorState
import com.fk.ui.theme.fkMetrics

@Composable
internal fun CalloutPopoverContentScreen(onBack: () -> Unit) {
  val metrics = fkMetrics()
  val messageAnchor = rememberCalloutAnchorState("pop-message")
  val titleAnchor = rememberCalloutAnchorState("pop-title")
  val headerAnchor = rememberCalloutAnchorState("pop-header")
  var placement by remember { mutableStateOf(CalloutPlacement.Bottom) }
  var style by remember { mutableStateOf(CalloutAppearance.Style.Light) }

  CalloutScenarioScaffold(
    title = "Popover content",
    description = "FkPopover · message, titleSubtitle, headerPanel, appearance styles",
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
      ).forEach { option ->
        FilterChip(
          selected = placement == option,
          onClick = { placement = option },
          label = { Text(option.name) },
        )
      }
    }

    SectionLabel("Card style")
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

    DemoAnchorChip(
      label = "Message only",
      anchor = messageAnchor,
      onClick = {
        FkPopover.show(
          controller = controller,
          message = "Plain message content inside a popover bubble.",
          anchor = messageAnchor,
          placement = placement,
          configuration = styledPopover(placement, style),
        )
        log("FkPopover.show(message)")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DemoAnchorChip(
      label = "Title + subtitle",
      anchor = titleAnchor,
      onClick = {
        FkPopover.show(
          controller = controller,
          title = "Popover card",
          message = "Body copy inside the card with manual dismiss.",
          anchor = titleAnchor,
          placement = placement,
          configuration = styledPopover(placement, style),
        )
        log("FkPopover.show(title, message)")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DemoAnchorChip(
      label = "Header panel",
      anchor = headerAnchor,
      onClick = {
        FkPopover.show(
          controller = controller,
          header = CalloutHeaderPanel(
            title = "Featured",
            backgroundColor = Color(0xFF1B6EF3).copy(alpha = 0.12f),
            textColor = Color(0xFF1B6EF3),
          ),
          body = "Colored header strip with supporting body text beneath.",
          anchor = headerAnchor,
          placement = placement,
          configuration = styledPopover(placement, style),
        )
        log("FkPopover.show(header, body)")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DismissRow(controller)
  }
}

@Composable
internal fun CalloutCoachMarkScreen(onBack: () -> Unit) {
  val anchor = rememberCalloutAnchorState("coach")
  var placement by remember { mutableStateOf(CalloutPlacement.Bottom) }

  CalloutScenarioScaffold(
    title = "Coach mark",
    description = "FkPopover.showCoachMark with dimmed backdrop, close, and primary action",
    onBack = onBack,
  ) { controller, log, _ ->
    SectionLabel("Placement")
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(fkMetrics().spacingXs),
    ) {
      listOf(
        CalloutPlacement.Top,
        CalloutPlacement.Bottom,
        CalloutPlacement.Start,
        CalloutPlacement.End,
      ).forEach { option ->
        FilterChip(
          selected = placement == option,
          onClick = { placement = option },
          label = { Text(option.name) },
        )
      }
    }

    HorizontalDivider()
    CenterStage {
      DemoAnchorButton(
        label = "Show coach mark",
        anchor = anchor,
        onClick = {
          FkPopover.showCoachMark(
            controller = controller,
            content = CalloutCoachMarkContent(
              title = "Tap to switch profiles",
              message = "Switch between your profiles for unique app experiences.",
              primaryActionTitle = "Got it",
              showsCloseButton = true,
            ),
            anchor = anchor,
            placement = placement,
            primaryAction = { log("coach mark · primary") },
            onClose = { log("coach mark · close") },
          )
          log("FkPopover.showCoachMark · ${placement.name}")
        },
      )
    }
    DismissRow(controller)
  }
}

@Composable
internal fun CalloutFooterActionsScreen(onBack: () -> Unit) {
  val anchor = rememberCalloutAnchorState("actions")

  CalloutScenarioScaffold(
    title = "Footer actions",
    description = "FkPopover.show(message, actions, actionHandlers) maps handlers by action id",
    onBack = onBack,
  ) { controller, log, _ ->
    CenterStage {
      DemoAnchorButton(
        label = "Message with actions",
        anchor = anchor,
        onClick = {
          val learnMore = CalloutAction(id = "learn", title = "Learn more", style = CalloutAction.Style.Default)
          val gotIt = CalloutAction(id = "got_it", title = "Got it", style = CalloutAction.Style.Primary)
          FkPopover.show(
            controller = controller,
            message = "You can attach one or more footer actions to a popover message.",
            actions = listOf(learnMore, gotIt),
            actionHandlers = mapOf(
              learnMore.id to { log("action · learn more") },
              gotIt.id to { log("action · got it") },
            ),
            anchor = anchor,
            placement = CalloutPlacement.Top,
          )
          log("FkPopover messageWithActions")
        },
      )
    }
    DismissRow(controller)
  }
}

private fun styledPopover(
  placement: CalloutPlacement,
  style: CalloutAppearance.Style,
): CalloutConfiguration {
  val base = CalloutConfiguration.popoverDefault(placement)
  return base.copy(
    appearance = base.appearance.copy(style = style),
  )
}
