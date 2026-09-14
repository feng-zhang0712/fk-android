package com.fk.business.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Orchestrates filter strip expand / collapse and tab title overrides.
 *
 * Conceptually aligned with iOS `FKTabBarFilterController` state machine
 * (Compose state holder — not a ViewController).
 */
@Stable
class FilterController(
  tabs: List<FilterTab>,
  val configuration: FilterConfiguration = FilterConfiguration(),
) {
  var tabs: List<FilterTab> by mutableStateOf(tabs)
    private set

  /** Currently expanded tab id, or null when collapsed. */
  var expandedTabId: String? by mutableStateOf(null)
    private set

  private var titleOverrides: Map<String, String> by mutableStateOf(emptyMap())

  /** Last dismiss reason (for host analytics if needed). */
  var lastDismissReason: FilterDismissReason? by mutableStateOf(null)
    private set

  val isExpanded: Boolean get() = expandedTabId != null

  /** Display title for a tab (override wins when present). */
  fun displayTitle(tab: FilterTab): String =
    titleOverrides[tab.id] ?: tab.title

  fun replaceTabs(newTabs: List<FilterTab>) {
    tabs = newTabs
    titleOverrides = emptyMap()
    if (expandedTabId != null && newTabs.none { it.id == expandedTabId }) {
      collapse(FilterDismissReason.Programmatic)
    }
  }

  fun clearTitleOverride(tabId: String) {
    titleOverrides = titleOverrides - tabId
  }

  fun clearAllTitleOverrides() {
    titleOverrides = emptyMap()
  }

  fun expand(tabId: String) {
    if (tabs.none { it.id == tabId }) return
    if (expandedTabId == tabId) return
    if (expandedTabId != null && expandedTabId != tabId) {
      lastDismissReason = FilterDismissReason.SwitchingTab
    }
    expandedTabId = tabId
  }

  fun collapse(reason: FilterDismissReason = FilterDismissReason.Programmatic) {
    if (expandedTabId == null) return
    lastDismissReason = reason
    expandedTabId = null
  }

  fun toggle(tabId: String) {
    if (tabs.none { it.id == tabId }) return
    if (expandedTabId == tabId) {
      collapse(FilterDismissReason.UserToggledSameTab)
    } else {
      expand(tabId)
    }
  }

  /**
   * Applies single-select title override + auto-collapse after a panel selection.
   * Call from the host after updating panel models.
   */
  fun onPanelSelection(context: FilterSelectionContext) {
    if (context.selection.effectiveMode == FilterSelectionMode.Single) {
      titleOverrides = titleOverrides + (context.tabId to context.selection.item.title)
      collapse(FilterDismissReason.SingleSelectAuto)
    }
  }
}

@Composable
fun rememberFilterController(
  tabs: List<FilterTab>,
  configuration: FilterConfiguration = FilterConfiguration(),
): FilterController =
  remember(tabs.map { it.id }) {
    FilterController(tabs = tabs, configuration = configuration)
  }
