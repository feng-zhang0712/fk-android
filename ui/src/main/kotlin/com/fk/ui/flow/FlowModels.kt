package com.fk.ui.flow

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Flow visualization: horizontal step indicator and vertical timeline.
 *
 * Conceptually aligned with iOS FKUIKit `FlowVisualization` (Compose, lean API).
 */
object FlowKit {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"
}

/**
 * Semantic state of a single step / timeline node (iOS `FKFlowStepState`).
 */
enum class FlowStepState {
  /** Finished; connector toward the next step is filled. */
  Completed,
  /** Active step; emphasized chrome. */
  Current,
  /** Not yet reached. */
  Upcoming,
  /** Failed or blocked. */
  Error,
  /** Intentionally bypassed. */
  Skipped,
  /** Visible but not interactive. */
  Disabled,
}

/**
 * Node diameter presets (iOS `FKFlowNodeSize`).
 */
enum class FlowNodeSize(val diameter: Dp) {
  S(20.dp),
  M(28.dp),
  L(36.dp),
}

/**
 * A single node for [FkStepIndicator] or [FkTimeline] (iOS `FKFlowStepItem`).
 *
 * Prefer host-formatted [timestampText] over raw dates so locale stays in the app layer.
 *
 * @param interactive When non-null, overrides the default tap policy
 *   (default: tappable iff a click handler is set and state is not [FlowStepState.Disabled]
 *   or [FlowStepState.Skipped]).
 */
data class FlowStepItem(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val caption: String? = null,
  val timestampText: String? = null,
  val state: FlowStepState = FlowStepState.Upcoming,
  val interactive: Boolean? = null,
)

/** Whether this item should accept taps given an optional click handler. */
fun FlowStepItem.canSelect(hasClickHandler: Boolean): Boolean {
  if (!hasClickHandler) return false
  interactive?.let { return it }
  return state != FlowStepState.Disabled && state != FlowStepState.Skipped
}

/**
 * Derives the active step index from explicit item states (iOS `FKFlowProgressResolver`).
 */
fun activeFlowIndex(items: List<FlowStepItem>): Int? {
  items.indexOfFirst { it.state == FlowStepState.Current }.takeIf { it >= 0 }?.let { return it }
  items.indexOfFirst { it.state == FlowStepState.Upcoming }.takeIf { it >= 0 }?.let { return it }
  items.indexOfLast { it.state == FlowStepState.Completed }.takeIf { it >= 0 }?.let { return it }
  return null
}

/**
 * Applies [currentStepIndex] to produce completed / current / upcoming states.
 *
 * Explicit [FlowStepState.Error], [FlowStepState.Skipped], and [FlowStepState.Disabled]
 * on an item are preserved (iOS parity: special states win).
 */
fun resolveFlowItems(
  items: List<FlowStepItem>,
  currentStepIndex: Int?,
): List<FlowStepItem> {
  if (currentStepIndex == null) return items
  val clamped = currentStepIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
  return items.mapIndexed { index, item ->
    when (item.state) {
      FlowStepState.Error, FlowStepState.Skipped, FlowStepState.Disabled -> item
      else -> item.copy(
        state = when {
          index < clamped -> FlowStepState.Completed
          index == clamped -> FlowStepState.Current
          else -> FlowStepState.Upcoming
        },
      )
    }
  }
}
