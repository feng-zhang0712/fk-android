package com.fk.ui.callout

/**
 * Anchored speech-bubble overlays (tooltip / popover) for Compose.
 *
 * Recommended entry points: [FkTooltip], [FkPopover]. Use [FkCallout] for advanced
 * content combinations, in-place updates, or concurrent presentation policies.
 */
object Callout {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"

  /** Baseline configuration for advanced [FkCallout.show] calls. */
  var defaultConfiguration: CalloutConfiguration = CalloutConfiguration.popoverDefault()

  /** Global tooltip preset store. */
  var tooltipConfiguration: CalloutConfiguration = CalloutConfiguration.tooltipDefault()

  /** Global popover preset store. */
  var popoverConfiguration: CalloutConfiguration = CalloutConfiguration.popoverDefault()

  /** Global menu / select preset store. */
  var menuConfiguration: CalloutConfiguration = CalloutConfiguration.menuDefault()
}
