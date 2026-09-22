package com.fk.sample.ui.callout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.callout.Callout
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

private enum class CalloutDemoDestination {
  Hub,
  TooltipBasics,
  PopoverContent,
  CoachMark,
  FooterActions,
  ActionMenu,
  SelectMenu,
  CustomContent,
  PlacementGrid,
  BeakStyles,
  LayoutBehavior,
  Advanced,
  Playground,
}

private data class CalloutDemoRow(
  val title: String,
  val subtitle: String,
  val destination: CalloutDemoDestination,
)

private data class CalloutDemoSection(
  val title: String,
  val rows: List<CalloutDemoRow>,
)

/**
 * Callout sample hub with grouped scenarios covering tooltip, popover, layout, and advanced APIs.
 */
@Composable
fun CalloutDemoScreen(
  onBack: () -> Unit,
) {
  var destination by remember { mutableStateOf(CalloutDemoDestination.Hub) }
  when (destination) {
    CalloutDemoDestination.Hub -> CalloutDemoHub(
      onBack = onBack,
      onOpen = { destination = it },
    )
    CalloutDemoDestination.TooltipBasics -> CalloutTooltipBasicsScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.PopoverContent -> CalloutPopoverContentScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.CoachMark -> CalloutCoachMarkScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.FooterActions -> CalloutFooterActionsScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.ActionMenu -> CalloutActionMenuScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.SelectMenu -> CalloutSelectMenuScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.CustomContent -> CalloutCustomContentScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.PlacementGrid -> CalloutPlacementGridScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.BeakStyles -> CalloutBeakStylesScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.LayoutBehavior -> CalloutLayoutBehaviorScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.Advanced -> CalloutAdvancedScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
    CalloutDemoDestination.Playground -> CalloutPlaygroundScreen(
      onBack = { destination = CalloutDemoDestination.Hub },
    )
  }
}

@Composable
private fun CalloutDemoHub(
  onBack: () -> Unit,
  onOpen: (CalloutDemoDestination) -> Unit,
) {
  val metrics = fkMetrics()
  val sections = remember {
    listOf(
      CalloutDemoSection(
        title = "FkTooltip",
        rows = listOf(
          CalloutDemoRow(
            title = "Tooltip basics",
            subtitle = "Placements, multiline, iconMessage, light & dark styles",
            destination = CalloutDemoDestination.TooltipBasics,
          ),
        ),
      ),
      CalloutDemoSection(
        title = "FkPopover · Content",
        rows = listOf(
          CalloutDemoRow(
            title = "Popover content",
            subtitle = "message, titleSubtitle, headerPanel, appearance styles",
            destination = CalloutDemoDestination.PopoverContent,
          ),
          CalloutDemoRow(
            title = "Coach mark",
            subtitle = "showCoachMark with close, primary action, and spotlight",
            destination = CalloutDemoDestination.CoachMark,
          ),
          CalloutDemoRow(
            title = "Footer actions",
            subtitle = "message + actions with handlers keyed by action id",
            destination = CalloutDemoDestination.FooterActions,
          ),
        ),
      ),
      CalloutDemoSection(
        title = "FkPopover · Menus",
        rows = listOf(
          CalloutDemoRow(
            title = "Action menu",
            subtitle = "Sectioned menu, header, destructive / disabled rows",
            destination = CalloutDemoDestination.ActionMenu,
          ),
          CalloutDemoRow(
            title = "Select menu",
            subtitle = "Trailing checkmark; selection updates the trigger",
            destination = CalloutDemoDestination.SelectMenu,
          ),
          CalloutDemoRow(
            title = "Custom content",
            subtitle = "Account panel and scrollable custom composables",
            destination = CalloutDemoDestination.CustomContent,
          ),
        ),
      ),
      CalloutDemoSection(
        title = "Layout & chrome",
        rows = listOf(
          CalloutDemoRow(
            title = "Placements & beak offset",
            subtitle = "All twelve placements and beak offset modes",
            destination = CalloutDemoDestination.PlacementGrid,
          ),
          CalloutDemoRow(
            title = "Beak styles",
            subtitle = "Isosceles, equilateral, and right-angle pointers",
            destination = CalloutDemoDestination.BeakStyles,
          ),
          CalloutDemoRow(
            title = "Layout behavior",
            subtitle = "sourceRect, maxContentHeight, keyboard, edge flip",
            destination = CalloutDemoDestination.LayoutBehavior,
          ),
        ),
      ),
      CalloutDemoSection(
        title = "Custom & advanced",
        rows = listOf(
          CalloutDemoRow(
            title = "FkCallout advanced",
            subtitle = "showOrUpdate, concurrent policy, hooks, dismiss by handle",
            destination = CalloutDemoDestination.Advanced,
          ),
          CalloutDemoRow(
            title = "Interactive playground",
            subtitle = "Live switches for kind, animation, and dismiss policy",
            destination = CalloutDemoDestination.Playground,
          ),
        ),
      ),
    )
  }

  Scaffold(
    topBar = {
      SampleTopBar(
        title = "Callout v${Callout.VERSION}",
        onBack = onBack,
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState()),
    ) {
      Text(
        text = "Anchored tooltip / popover overlays",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
        modifier = Modifier.padding(
          horizontal = metrics.spacingM,
          vertical = metrics.spacingS,
        ),
      )
      sections.forEach { section ->
        Text(
          text = section.title,
          style = fkTextStyle(FkTextStyle.Footnote).copy(fontWeight = FontWeight.SemiBold),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          modifier = Modifier.padding(
            horizontal = metrics.spacingM,
            vertical = metrics.spacingS,
          ),
        )
        section.rows.forEach { row ->
          ListItem(
            headlineContent = { Text(row.title) },
            supportingContent = { Text(row.subtitle) },
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpen(row.destination) },
          )
          HorizontalDivider()
        }
      }
    }
  }
}
