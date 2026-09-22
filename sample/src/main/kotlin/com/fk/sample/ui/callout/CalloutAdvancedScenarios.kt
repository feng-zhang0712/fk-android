package com.fk.sample.ui.callout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fk.ui.callout.CalloutAnimationStyle
import com.fk.ui.callout.CalloutConfiguration
import com.fk.ui.callout.CalloutContent
import com.fk.ui.callout.CalloutHandle
import com.fk.ui.callout.CalloutLifecycleHooks
import com.fk.ui.callout.CalloutPlacement
import com.fk.ui.callout.CalloutPresentationPolicy
import com.fk.ui.callout.CalloutRequest
import com.fk.ui.callout.FkCallout
import com.fk.ui.callout.FkPopover
import com.fk.ui.callout.FkTooltip
import com.fk.ui.callout.rememberCalloutAnchorState
import com.fk.ui.theme.fkMetrics

@Composable
internal fun CalloutAdvancedScreen(onBack: () -> Unit) {
  val metrics = fkMetrics()
  val primary = rememberCalloutAnchorState("adv-primary")
  val secondary = rememberCalloutAnchorState("adv-secondary")
  val updateAnchor = rememberCalloutAnchorState("adv-update")
  var handle by remember { mutableStateOf<CalloutHandle?>(null) }
  var updateCount by remember { mutableIntStateOf(0) }
  var concurrent by remember { mutableStateOf(false) }

  CalloutScenarioScaffold(
    title = "FkCallout advanced",
    description = "showOrUpdate, concurrent policy, lifecycle hooks, dismiss by handle",
    onBack = onBack,
  ) { controller, log, _ ->
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Allow concurrent", modifier = Modifier.weight(1f))
      Switch(checked = concurrent, onCheckedChange = { concurrent = it })
    }

    DemoAnchorChip(
      label = "Show with lifecycle hooks",
      anchor = primary,
      onClick = {
        val config = CalloutConfiguration.popoverDefault(CalloutPlacement.Bottom).copy(
          presentationPolicy = if (concurrent) {
            CalloutPresentationPolicy.AllowConcurrent
          } else {
            CalloutPresentationPolicy.ReplaceActive
          },
        )
        handle = FkCallout.show(
          controller = controller,
          content = CalloutContent.TitleSubtitle(
            title = "Hooks",
            message = "willShow / didShow / willDismiss / didDismiss",
          ),
          anchor = primary,
          configuration = config,
          hooks = CalloutLifecycleHooks(
            willShow = { log("willShow · $it") },
            didShow = { log("didShow · $it") },
            willDismiss = { id, reason -> log("willDismiss · $id · $reason") },
            didDismiss = { id, reason -> log("didDismiss · $id · $reason") },
          ),
        )
        log("FkCallout.show · handle=${handle?.id?.take(8)}")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DemoAnchorChip(
      label = "Second bubble (concurrent test)",
      anchor = secondary,
      onClick = {
        val config = CalloutConfiguration.tooltipDefault(CalloutPlacement.Top).copy(
          presentationPolicy = if (concurrent) {
            CalloutPresentationPolicy.AllowConcurrent
          } else {
            CalloutPresentationPolicy.ReplaceActive
          },
          autoDismissDurationMs = null,
          tapOutsideToDismiss = true,
        )
        FkTooltip.show(
          controller = controller,
          message = "Second session",
          anchor = secondary,
          placement = CalloutPlacement.Top,
          configuration = config,
        )
        log("second session · concurrent=$concurrent")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    HorizontalDivider()
    SectionLabel("showOrUpdate")
    DemoAnchorChip(
      label = "Show or update same anchor",
      anchor = updateAnchor,
      onClick = {
        updateCount += 1
        val request = CalloutRequest(
          content = CalloutContent.Message("Updated in place · #$updateCount"),
          anchorId = updateAnchor.id,
          configuration = CalloutConfiguration.popoverDefault(CalloutPlacement.Top),
        )
        controller.registerAnchor(updateAnchor)
        handle = FkCallout.showOrUpdate(controller, request)
        log("showOrUpdate · #$updateCount")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    TextButton(
      onClick = {
        val current = handle
        if (current != null) {
          FkCallout.dismiss(controller, current)
          log("dismiss(handle)")
        } else {
          log("no handle")
        }
      },
    ) {
      Text("Dismiss via handle")
    }

    DismissRow(controller)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalloutPlaygroundScreen(onBack: () -> Unit) {
  val metrics = fkMetrics()
  val anchor = rememberCalloutAnchorState("playground")
  var kindTooltip by remember { mutableStateOf(false) }
  var placement by remember { mutableStateOf(CalloutPlacement.Automatic) }
  var animation by remember { mutableStateOf(CalloutAnimationStyle.FadeScale) }
  var tapOutside by remember { mutableStateOf(true) }
  var passThrough by remember { mutableStateOf(true) }
  var flip by remember { mutableStateOf(true) }

  CalloutScenarioScaffold(
    title = "Interactive playground",
    description = "Live toggles for kind, placement, animation, and dismiss interaction",
    onBack = onBack,
  ) { controller, log, _ ->
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text("Use tooltip preset", modifier = Modifier.weight(1f))
      Switch(checked = kindTooltip, onCheckedChange = { kindTooltip = it })
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text("Tap outside to dismiss", modifier = Modifier.weight(1f))
      Switch(checked = tapOutside, onCheckedChange = { tapOutside = it })
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text("Pass-through outside touches", modifier = Modifier.weight(1f))
      Switch(checked = passThrough, onCheckedChange = { passThrough = it })
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text("Flip when needed", modifier = Modifier.weight(1f))
      Switch(checked = flip, onCheckedChange = { flip = it })
    }

    SectionLabel("Placement")
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
    ) {
      listOf(
        CalloutPlacement.Automatic,
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

    SectionLabel("Animation")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs)) {
      CalloutAnimationStyle.entries.forEach { option ->
        FilterChip(
          selected = animation == option,
          onClick = { animation = option },
          label = { Text(option.name) },
        )
      }
    }

    HorizontalDivider()
    CenterStage {
      DemoAnchorButton(
        label = "Present",
        anchor = anchor,
        onClick = {
          val base = if (kindTooltip) {
            CalloutConfiguration.tooltipDefault(placement)
          } else {
            CalloutConfiguration.popoverDefault(placement)
          }
          val config = base.copy(
            animationStyle = animation,
            tapOutsideToDismiss = tapOutside,
            passesThroughOutsideTouches = passThrough,
            flipsPlacementWhenNeeded = flip,
            autoDismissDurationMs = if (kindTooltip) 3_000L else null,
          )
          if (kindTooltip) {
            FkTooltip.show(
              controller = controller,
              message = "Playground tooltip",
              anchor = anchor,
              placement = placement,
              configuration = config,
            )
          } else {
            FkPopover.show(
              controller = controller,
              title = "Playground",
              message = "Tweaked configuration from the controls above.",
              anchor = anchor,
              placement = placement,
              configuration = config,
            )
          }
          log("playground · tooltip=$kindTooltip · ${placement.name}")
        },
      )
    }
    DismissRow(controller)
  }
}
