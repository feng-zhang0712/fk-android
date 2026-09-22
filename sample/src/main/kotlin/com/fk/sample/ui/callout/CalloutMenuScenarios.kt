package com.fk.sample.ui.callout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fk.ui.callout.CalloutAppearance
import com.fk.ui.callout.CalloutConfiguration
import com.fk.ui.callout.CalloutContent
import com.fk.ui.callout.CalloutMenu
import com.fk.ui.callout.CalloutMenuItem
import com.fk.ui.callout.CalloutMenuSection
import com.fk.ui.callout.CalloutPlacement
import com.fk.ui.callout.FkCallout
import com.fk.ui.callout.FkPopover
import com.fk.ui.callout.rememberCalloutAnchorState
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkTextStyle

@Composable
internal fun CalloutActionMenuScreen(onBack: () -> Unit) {
  val menuAnchor = rememberCalloutAnchorState("action-menu")
  val frostedAnchor = rememberCalloutAnchorState("frosted-menu")

  CalloutScenarioScaffold(
    title = "Action menu",
    description = "FkPopover.showMenu · sectioned rows, header, destructive / disabled items",
    onBack = onBack,
  ) { controller, log, _ ->
    DemoAnchorChip(
      label = "Sectioned action menu",
      anchor = menuAnchor,
      onClick = {
        FkPopover.showMenu(
          controller = controller,
          menu = CalloutMenu(
            header = "Workspace actions",
            sections = listOf(
              CalloutMenuSection(
                items = listOf(
                  CalloutMenuItem(id = "edit", title = "Edit", icon = Icons.Outlined.Edit),
                  CalloutMenuItem(id = "share", title = "Share", subtitle = "Copy link", icon = Icons.Outlined.Share),
                ),
              ),
              CalloutMenuSection(
                items = listOf(
                  CalloutMenuItem(
                    id = "archive",
                    title = "Archive",
                    isEnabled = false,
                  ),
                  CalloutMenuItem(
                    id = "delete",
                    title = "Delete",
                    icon = Icons.Outlined.Delete,
                    isDestructive = true,
                  ),
                ),
              ),
            ),
          ),
          anchor = menuAnchor,
          onSelect = { item -> log("menu select · ${item.title}") },
        )
        log("FkPopover.showMenu · sections")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DemoAnchorChip(
      label = "Frosted appearance flag",
      anchor = frostedAnchor,
      onClick = {
        val config = CalloutConfiguration.menuDefault().copy(
          appearance = CalloutConfiguration.menuDefault().appearance.copy(
            usesFrostedGlassBackground = true,
            style = CalloutAppearance.Style.Light,
          ),
        )
        FkPopover.showMenu(
          controller = controller,
          menu = CalloutMenu(
            sections = listOf(
              CalloutMenuSection(
                items = listOf(
                  CalloutMenuItem(title = "Option A"),
                  CalloutMenuItem(title = "Option B"),
                ),
              ),
            ),
          ),
          anchor = frostedAnchor,
          configuration = config,
          onSelect = { item -> log("frosted · ${item.title}") },
        )
        log("FkPopover.showMenu · frosted flag")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DismissRow(controller)
  }
}

@Composable
internal fun CalloutSelectMenuScreen(onBack: () -> Unit) {
  val anchor = rememberCalloutAnchorState("select-menu")
  var selectedId by remember { mutableStateOf("pro") }

  CalloutScenarioScaffold(
    title = "Select menu",
    description = "Content-sized menu with trailing checkmark; selection updates the trigger",
    onBack = onBack,
  ) { controller, log, _ ->
    val plans = listOf(
      CalloutMenuItem(id = "free", title = "Free", subtitle = "Personal", isSelected = selectedId == "free"),
      CalloutMenuItem(id = "pro", title = "Pro", subtitle = "Team", isSelected = selectedId == "pro"),
      CalloutMenuItem(id = "enterprise", title = "Enterprise", subtitle = "Org", isSelected = selectedId == "enterprise"),
    )
    val selectedTitle = plans.firstOrNull { it.id == selectedId }?.title ?: "Select"

    CenterStage {
      DemoAnchorButton(
        label = "Plan: $selectedTitle",
        anchor = anchor,
        onClick = {
          if (FkPopover.isPresenting(controller)) {
            FkPopover.dismissActive(controller)
            log("toggle dismiss")
            return@DemoAnchorButton
          }
          FkPopover.showMenu(
            controller = controller,
            menu = CalloutMenu(sections = listOf(CalloutMenuSection(plans))),
            anchor = anchor,
            placement = CalloutPlacement.BottomStart,
            onSelect = { item ->
              selectedId = item.id
              log("selected · ${item.title}")
            },
          )
          log("FkPopover.showMenu · select")
        },
      )
    }
    DismissRow(controller)
  }
}

@Composable
internal fun CalloutCustomContentScreen(onBack: () -> Unit) {
  val accountAnchor = rememberCalloutAnchorState("account")
  val scrollAnchor = rememberCalloutAnchorState("scroll")

  CalloutScenarioScaffold(
    title = "Custom content",
    description = "FkPopover / FkCallout custom composable panels (account card, scrollable list)",
    onBack = onBack,
  ) { controller, log, _ ->
    DemoAnchorChip(
      label = "Account panel",
      anchor = accountAnchor,
      onClick = {
        FkPopover.show(
          controller = controller,
          customContent = {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            ) {
              Text("Alex Rivera", style = fkTextStyle(FkTextStyle.Headline), fontWeight = FontWeight.SemiBold)
              Text("alex@example.com", style = fkTextStyle(FkTextStyle.Caption1))
              HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
              Text("Switch workspace", style = fkTextStyle(FkTextStyle.Subheadline))
              Text("Personal · Acme Inc.", style = fkTextStyle(FkTextStyle.Caption1))
            }
          },
          anchor = accountAnchor,
          placement = CalloutPlacement.BottomStart,
        )
        log("FkPopover.show(custom)")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DemoAnchorChip(
      label = "Scrollable panel (max height)",
      anchor = scrollAnchor,
      onClick = {
        val config = CalloutConfiguration.popoverDefault(CalloutPlacement.Bottom).copy(
          maxContentHeight = 180.dp,
          maxWidth = 280.dp,
        )
        FkCallout.show(
          controller = controller,
          content = CalloutContent.Custom {
            Column {
              repeat(20) { index ->
                Text(
                  "Row ${index + 1} · scrollable interior",
                  modifier = Modifier.padding(vertical = 6.dp),
                )
              }
            }
          },
          anchor = scrollAnchor,
          configuration = config,
        )
        log("FkCallout custom · maxContentHeight")
      },
      modifier = Modifier.fillMaxWidth(),
    )

    DismissRow(controller)
  }
}
