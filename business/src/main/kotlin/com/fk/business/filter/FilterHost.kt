package com.fk.business.filter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkSpacingToken
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Filter strip + expandable panel host over [content].
 *
 * Conceptually aligned with iOS `FKTabBarFilterController` (Compose overlay,
 * not UIKit anchored sheet). Place this at a screen-root container so the
 * scrim covers the list / page body, not only the strip.
 *
 * @param panelContents Host-owned panel payloads keyed by tab id.
 * @param onPanelContentChange Called when a built-in panel mutates its model.
 * @param onSelection Fired after model update (and after controller auto-collapse for single-select).
 * @param customPanel Optional composable for [FilterPanelContent.Custom] tabs.
 */
@Composable
fun FilterHost(
  controller: FilterController,
  panelContents: Map<String, FilterPanelContent>,
  onPanelContentChange: (tabId: String, content: FilterPanelContent) -> Unit,
  modifier: Modifier = Modifier,
  onSelection: (FilterSelectionContext) -> Unit = {},
  customPanel: (@Composable (tabId: String) -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val metrics = fkMetrics()
  val strings = controller.configuration.strings
  val expandedId = controller.expandedTabId
  val expandedTab = controller.tabs.firstOrNull { it.id == expandedId }

  BackHandler(enabled = controller.isExpanded) {
    controller.collapse(FilterDismissReason.SystemBack)
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val maxPanelHeight = maxHeight * controller.configuration.panelMaxHeightFraction

    Column(modifier = Modifier.fillMaxSize()) {
      FilterTabStrip(
        controller = controller,
        modifier = Modifier.fillMaxWidth(),
      )
      HorizontalDivider(color = fkColor(FkColorRole.Outline))
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        content()
        val expanded = expandedTab != null
        androidx.compose.animation.AnimatedVisibility(
          visible = expanded,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(fkColor(FkColorRole.Scrim).copy(alpha = 0.4f))
              .semantics { contentDescription = strings.dismissScrim }
              .clickable { controller.collapse(FilterDismissReason.Backdrop) },
          )
        }
        Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
          androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
          ) {
            val tab = expandedTab
            if (tab != null) {
              val panelContent = panelContents[tab.id]
              val usesFixedTwoColumnHeight = panelContent is FilterPanelContent.Hierarchy ||
                panelContent is FilterPanelContent.DualHierarchy
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .then(
                    if (usesFixedTwoColumnHeight) {
                      Modifier.height(maxPanelHeight)
                    } else {
                      Modifier.heightIn(max = maxPanelHeight)
                    },
                  )
                  .background(fkColor(FkColorRole.Surface)),
              ) {
                when {
                  panelContent is FilterPanelContent.Custom && customPanel != null ->
                    customPanel(tab.id)
                  panelContent is FilterPanelContent.Custom ->
                    Text(
                      text = strings.emptyPanel,
                      style = fkTextStyle(FkTextStyle.Body),
                      color = fkColor(FkColorRole.OnSurfaceSecondary),
                      modifier = Modifier.padding(metrics.spacing(FkSpacingToken.L)),
                    )
                  panelContent != null ->
                    FilterPanelBody(
                      content = panelContent,
                      allowsMultipleSelection = tab.allowsMultipleSelection,
                      emptyLabel = strings.emptyPanel,
                      onContentChange = { updated ->
                        onPanelContentChange(tab.id, updated)
                      },
                      onSelection = { selection ->
                        val context = FilterSelectionContext(
                          tabId = tab.id,
                          panelKind = tab.panelKind,
                          selection = selection,
                        )
                        onSelection(context)
                        controller.onPanelSelection(context)
                      },
                      modifier = Modifier.fillMaxWidth(),
                    )
                  else ->
                    Text(
                      text = strings.emptyPanel,
                      style = fkTextStyle(FkTextStyle.Body),
                      color = fkColor(FkColorRole.OnSurfaceSecondary),
                      modifier = Modifier.padding(metrics.spacing(FkSpacingToken.L)),
                    )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FilterTabStrip(
  controller: FilterController,
  modifier: Modifier = Modifier,
) {
  val metrics = fkMetrics()
  Row(
    modifier = modifier
      .background(fkColor(FkColorRole.Surface))
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = metrics.spacing(FkSpacingToken.S)),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    controller.tabs.forEach { tab ->
      val expanded = controller.expandedTabId == tab.id
      val title = controller.displayTitle(tab)
      Column(
        modifier = Modifier
          .clickable { controller.toggle(tab.id) }
          .padding(
            horizontal = metrics.spacing(FkSpacingToken.M),
            vertical = metrics.spacing(FkSpacingToken.M),
          ),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            style = fkTextStyle(FkTextStyle.Subheadline),
            fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Medium,
            color = if (expanded) {
              fkColor(FkColorRole.Primary)
            } else {
              fkColor(FkColorRole.OnSurface)
            },
          )
          Text(
            text = if (expanded) " ▲" else " ▼",
            style = fkTextStyle(FkTextStyle.Caption2),
            color = if (expanded) {
              fkColor(FkColorRole.Primary)
            } else {
              fkColor(FkColorRole.OnSurfaceSecondary)
            },
          )
        }
        tab.subtitle?.let { subtitle ->
          Text(
            text = subtitle,
            style = fkTextStyle(FkTextStyle.Caption2),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
            modifier = Modifier.padding(top = 2.dp),
          )
        }
      }
    }
  }
}
