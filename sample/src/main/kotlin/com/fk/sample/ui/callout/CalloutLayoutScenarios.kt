package com.fk.sample.ui.callout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.fk.ui.callout.CalloutBeakOffset
import com.fk.ui.callout.CalloutBeakRightAngleCorner
import com.fk.ui.callout.CalloutBeakStyle
import com.fk.ui.callout.CalloutConfiguration
import com.fk.ui.callout.CalloutKeyboardAvoidance
import com.fk.ui.callout.CalloutMenu
import com.fk.ui.callout.CalloutMenuItem
import com.fk.ui.callout.CalloutMenuSection
import com.fk.ui.callout.CalloutPlacement
import com.fk.ui.callout.FkPopover
import com.fk.ui.callout.rememberCalloutAnchorState
import com.fk.ui.theme.fkMetrics

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalloutPlacementGridScreen(onBack: () -> Unit) {
  val metrics = fkMetrics()
  val placements = CalloutPlacement.entries.filter { it != CalloutPlacement.Automatic }
  var selected by remember { mutableStateOf(CalloutPlacement.Top) }
  val anchor = rememberCalloutAnchorState("placement-grid")
  var beakMode by remember { mutableStateOf(BeakOffsetMode.Automatic) }

  CalloutScenarioScaffold(
    title = "Placements & beak offset",
    description = "All twelve CalloutPlacement values plus CalloutBeakOffset modes",
    onBack = onBack,
  ) { controller, log, _ ->
    SectionLabel("Placement")
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
    ) {
      placements.forEach { option ->
        FilterChip(
          selected = selected == option,
          onClick = { selected = option },
          label = { Text(option.name) },
        )
      }
    }

    SectionLabel("Beak offset")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs)) {
      BeakOffsetMode.entries.forEach { mode ->
        FilterChip(
          selected = beakMode == mode,
          onClick = { beakMode = mode },
          label = { Text(mode.label) },
        )
      }
    }

    HorizontalDivider()
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(220.dp),
      contentAlignment = Alignment.Center,
    ) {
      DemoAnchorButton(
        label = "Show · ${selected.name}",
        anchor = anchor,
        onClick = {
          val config = CalloutConfiguration.popoverDefault(selected).copy(
            beakOffset = when (beakMode) {
              BeakOffsetMode.Automatic -> CalloutBeakOffset.Automatic
              BeakOffsetMode.FractionStart -> CalloutBeakOffset.Fraction(0.2f)
              BeakOffsetMode.FractionEnd -> CalloutBeakOffset.Fraction(0.8f)
              BeakOffsetMode.Fixed -> CalloutBeakOffset.Fixed(24.dp)
            },
          )
          FkPopover.show(
            controller = controller,
            title = selected.name,
            message = "Beak offset: ${beakMode.label}",
            anchor = anchor,
            placement = selected,
            configuration = config,
          )
          log("placement · ${selected.name} · ${beakMode.label}")
        },
      )
    }
    DismissRow(controller)
  }
}

private enum class BeakOffsetMode(val label: String) {
  Automatic("Automatic"),
  FractionStart("Fraction 0.2"),
  FractionEnd("Fraction 0.8"),
  Fixed("Fixed 24dp"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalloutBeakStylesScreen(onBack: () -> Unit) {
  val metrics = fkMetrics()
  val anchor = rememberCalloutAnchorState("beak-style")
  var style by remember { mutableStateOf(BeakDemoStyle.Isosceles) }

  CalloutScenarioScaffold(
    title = "Beak styles",
    description = "CalloutBeakStyle presets: isosceles, equilateral, right-angle",
    onBack = onBack,
  ) { controller, log, _ ->
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
    ) {
      BeakDemoStyle.entries.forEach { option ->
        FilterChip(
          selected = style == option,
          onClick = { style = option },
          label = { Text(option.label) },
        )
      }
    }

    HorizontalDivider()
    CenterStage {
      DemoAnchorButton(
        label = "Show beak style",
        anchor = anchor,
        onClick = {
          val base = CalloutConfiguration.popoverDefault(CalloutPlacement.Top)
          val config = base.copy(
            appearance = base.appearance.copy(beakStyle = style.toBeakStyle()),
          )
          FkPopover.show(
            controller = controller,
            title = style.label,
            message = "Pointer shape applied to the bubble chrome.",
            anchor = anchor,
            placement = CalloutPlacement.Top,
            configuration = config,
          )
          log("beak · ${style.label}")
        },
      )
    }
    DismissRow(controller)
  }
}

private enum class BeakDemoStyle(val label: String) {
  Isosceles("Isosceles"),
  Equilateral("Equilateral"),
  RightAngleStart("Right · start"),
  RightAngleEnd("Right · end"),
  ;

  fun toBeakStyle(): CalloutBeakStyle = when (this) {
    Isosceles -> CalloutBeakStyle.Isosceles
    Equilateral -> CalloutBeakStyle.Equilateral
    RightAngleStart -> CalloutBeakStyle.RightAngle(CalloutBeakRightAngleCorner.Start, apexAlongBase = 0f)
    RightAngleEnd -> CalloutBeakStyle.RightAngle(CalloutBeakRightAngleCorner.End, apexAlongBase = 1f)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalloutLayoutBehaviorScreen(onBack: () -> Unit) {
  val metrics = fkMetrics()
  val sourceAnchor = rememberCalloutAnchorState("source-rect")
  val flipAnchor = rememberCalloutAnchorState("edge-flip")
  val menuAnchor = rememberCalloutAnchorState("menu-height")
  val keyboardAnchor = rememberCalloutAnchorState("keyboard")
  var sourceBounds by remember { mutableStateOf(Rect.Zero) }
  var avoidance by remember { mutableStateOf(CalloutKeyboardAvoidance.Relayout) }
  var text by remember { mutableStateOf("") }

  CalloutScenarioScaffold(
    title = "Layout behavior",
    description = "sourceRect, maxContentHeight, keyboardAvoidance, edge flip near screen bounds",
    onBack = onBack,
  ) { controller, log, _ ->
    SectionLabel("Partial sourceRect (leading half)")
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { coords ->
          val pos = coords.positionInWindow()
          val size = coords.size
          sourceBounds = Rect(
            left = pos.x,
            top = pos.y,
            right = pos.x + size.width,
            bottom = pos.y + size.height,
          )
        },
    ) {
      DemoAnchorChip(
        label = "Anchor · leading half source",
        anchor = sourceAnchor,
        onClick = {
          val half = if (sourceBounds.width > 0f) {
            sourceBounds.copy(right = sourceBounds.left + sourceBounds.width / 2f)
          } else {
            null
          }
          FkPopover.show(
            controller = controller,
            title = "sourceRect",
            message = "Bubble aims at the leading half of the anchor.",
            anchor = sourceAnchor,
            sourceRectInWindow = half,
            placement = CalloutPlacement.Bottom,
          )
          log("sourceRect · leading half")
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }

    SectionLabel("Near top edge (flip)")
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      DemoAnchorChip(
        label = "Prefer Top · may flip",
        anchor = flipAnchor,
        onClick = {
          val config = CalloutConfiguration.popoverDefault(CalloutPlacement.Top).copy(
            flipsPlacementWhenNeeded = true,
          )
          FkPopover.show(
            controller = controller,
            title = "Edge flip",
            message = "When Top does not fit, the engine flips to Bottom.",
            anchor = flipAnchor,
            placement = CalloutPlacement.Top,
            configuration = config,
          )
          log("flip · Top preferred")
        },
      )
    }

    SectionLabel("Menu maxContentHeight")
    DemoAnchorChip(
      label = "Tall menu · 160dp max",
      anchor = menuAnchor,
      onClick = {
        val items = (1..12).map { CalloutMenuItem(id = "row-$it", title = "Option $it") }
        FkPopover.showMenu(
          controller = controller,
          menu = CalloutMenu(sections = listOf(CalloutMenuSection(items))),
          anchor = menuAnchor,
          configuration = CalloutConfiguration.menuDefault().copy(maxContentHeight = 160.dp),
          onSelect = { log("menu · ${it.title}") },
        )
        log("menu · maxContentHeight 160")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    SectionLabel("Keyboard avoidance")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs)) {
      CalloutKeyboardAvoidance.entries.forEach { option ->
        FilterChip(
          selected = avoidance == option,
          onClick = { avoidance = option },
          label = { Text(option.name) },
        )
      }
    }
    OutlinedTextField(
      value = text,
      onValueChange = { text = it },
      label = { Text("Focus to open keyboard") },
      modifier = Modifier.fillMaxWidth(),
    )
    DemoAnchorChip(
      label = "Show near field",
      anchor = keyboardAnchor,
      onClick = {
        val config = CalloutConfiguration.popoverDefault(CalloutPlacement.Top).copy(
          keyboardAvoidance = avoidance,
        )
        FkPopover.show(
          controller = controller,
          title = "Keyboard",
          message = "Avoidance mode: ${avoidance.name}",
          anchor = keyboardAnchor,
          placement = CalloutPlacement.Top,
          configuration = config,
        )
        log("keyboardAvoidance · ${avoidance.name}")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DismissRow(controller)
  }
}
