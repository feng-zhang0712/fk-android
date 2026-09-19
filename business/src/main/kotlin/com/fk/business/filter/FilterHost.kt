package com.fk.business.filter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fk.business.R
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkTextStyle

/** Ambient appearance for panel bodies under [FilterHost]. */
internal val LocalFilterAppearance = staticCompositionLocalOf { FilterAppearance() }

/**
 * Filter strip + expandable panel host over [content].
 *
 * Conceptually aligned with iOS `FKTabBarFilterController` (Compose overlay,
 * not UIKit anchored sheet). Panel enter/exit uses **vertical offset**
 * (`slideInVertically` / `slideOutVertically`) clipped to the region below the
 * strip, so the panel emerges from / retreats into the strip bottom edge.
 *
 * Place this at a screen-root container so the scrim covers the list / page
 * body, not only the strip.
 *
 * @param panelContents Host-owned panel payloads keyed by tab id.
 * @param onPanelContentChange Called when a built-in panel mutates its model.
 * @param onSelection Fired after model update (and after controller auto-collapse for single-select).
 * @param customPanel Optional composable for [FilterPanelContent.Custom] tabs.
 * @param collapsedChevron Optional painter for collapsed tab chevron (defaults to triangle asset).
 * @param expandedChevron Optional painter for expanded tab chevron.
 */
@Composable
fun FilterHost(
  controller: FilterController,
  panelContents: Map<String, FilterPanelContent>,
  onPanelContentChange: (tabId: String, content: FilterPanelContent) -> Unit,
  modifier: Modifier = Modifier,
  onSelection: (FilterSelectionContext) -> Unit = {},
  customPanel: (@Composable (tabId: String) -> Unit)? = null,
  collapsedChevron: Painter? = null,
  expandedChevron: Painter? = null,
  content: @Composable () -> Unit,
) {
  val appearance = controller.configuration.appearance
  val strings = controller.configuration.strings
  val expandedId = controller.expandedTabId
  val expandedTab = controller.tabs.firstOrNull { it.id == expandedId }
  // Keep the last expanded tab while exit animation runs (otherwise content
  // becomes null immediately and the panel appears to vanish).
  var presentedTab by remember { mutableStateOf<FilterTab?>(null) }
  if (expandedTab != null) {
    presentedTab = expandedTab
  }

  val defaultCollapsed = painterResource(R.drawable.fk_ic_arrow_triangle_down)
  val defaultExpanded = painterResource(R.drawable.fk_ic_arrow_triangle_up)
  val chevronCollapsed = collapsedChevron ?: defaultCollapsed
  val chevronExpanded = expandedChevron ?: defaultExpanded

  BackHandler(enabled = controller.isExpanded) {
    controller.collapse(FilterDismissReason.SystemBack)
  }

  CompositionLocalProvider(LocalFilterAppearance provides appearance) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
      val hostHeight = maxHeight

      Column(modifier = Modifier.fillMaxSize()) {
        FilterTabStrip(
          controller = controller,
          appearance = appearance,
          collapsedChevron = chevronCollapsed,
          expandedChevron = chevronExpanded,
          modifier = Modifier.fillMaxWidth(),
        )
        if (appearance.showsStripDivider) {
          HorizontalDivider(color = fkColor(FkColorRole.Outline))
        }
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
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .fillMaxWidth()
              .clipToBounds(),
          ) {
            androidx.compose.animation.AnimatedVisibility(
              visible = expanded,
              enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
              exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            ) {
              val tab = presentedTab
              if (tab != null) {
                val panelContent = panelContents[tab.id]
                val heightModifier = panelHeightModifier(
                  content = panelContent,
                  appearance = appearance,
                  hostHeight = hostHeight,
                  fallbackMaxFraction = controller.configuration.panelMaxHeightFraction,
                )
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .then(heightModifier)
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
                        modifier = Modifier.padding(16.dp),
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
                        modifier = Modifier.padding(16.dp),
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
}

private fun panelHeightModifier(
  content: FilterPanelContent?,
  appearance: FilterAppearance,
  hostHeight: Dp,
  fallbackMaxFraction: Float,
): Modifier {
  val policy = when (content) {
    is FilterPanelContent.Hierarchy,
    is FilterPanelContent.DualHierarchy,
    -> appearance.directoryHeight
    is FilterPanelContent.SingleList -> appearance.listHeight
    is FilterPanelContent.Tags -> appearance.tagsHeight
    is FilterPanelContent.Custom, null -> appearance.tagsHeight
  }
  val maxFraction = policy.maxScreenFraction
    .takeIf { it > 0f }
    ?: fallbackMaxFraction
  val cappedMax = (hostHeight * maxFraction.coerceIn(0.1f, 1f)).coerceAtMost(hostHeight)
  val minHeight = maxOf(
    policy.minDp.dp,
    hostHeight * policy.minScreenFraction.coerceAtLeast(0f),
  ).coerceAtMost(cappedMax)

  return if (policy.fillToMinimum) {
    Modifier.height(cappedMax)
  } else {
    Modifier.heightIn(min = minHeight, max = cappedMax)
  }
}

@Composable
private fun FilterTabStrip(
  controller: FilterController,
  appearance: FilterAppearance,
  collapsedChevron: Painter,
  expandedChevron: Painter,
  modifier: Modifier = Modifier,
) {
  val stripHeight = appearance.stripHeightDp.dp
  val horizontalPad = appearance.stripHorizontalPaddingDp.dp
  val fillEqually = appearance.tabWidthMode == FilterTabWidthMode.FillEqually

  Row(
    modifier = modifier
      .height(stripHeight)
      .background(Color(0xFFF7F7F7))
      .then(
        if (fillEqually) {
          Modifier.fillMaxWidth()
        } else {
          Modifier.horizontalScroll(rememberScrollState())
        },
      )
      .padding(horizontal = horizontalPad),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    controller.tabs.forEach { tab ->
      val expanded = controller.expandedTabId == tab.id
      val title = controller.displayTitle(tab)
      val tint = if (expanded) {
        fkColor(FkColorRole.Primary)
      } else {
        fkColor(FkColorRole.OnSurface)
      }
      Row(
        modifier = Modifier
          .then(if (fillEqually) Modifier.weight(1f) else Modifier)
          .fillMaxHeight()
          .clickable { controller.toggle(tab.id) }
          .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          text = title,
          style = fkTextStyle(FkTextStyle.Subheadline),
          fontWeight = FontWeight.Medium,
          color = tint,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          painter = if (expanded) expandedChevron else collapsedChevron,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}
