package com.fk.ui.callout

import androidx.compose.ui.geometry.Rect

/**
 * Short-lived anchored hint with tooltip defaults (compact dark bubble, auto-dismiss).
 */
object FkTooltip {
  var defaultConfiguration: CalloutConfiguration
    get() = Callout.tooltipConfiguration
    set(value) {
      Callout.tooltipConfiguration = value
    }

  fun isPresenting(controller: CalloutController): Boolean = controller.isPresenting

  fun show(
    controller: CalloutController,
    message: String,
    anchor: CalloutAnchorState,
    sourceRectInWindow: Rect? = null,
    placement: CalloutPlacement = CalloutPlacement.Automatic,
    configuration: CalloutConfiguration? = null,
    hooks: CalloutLifecycleHooks = CalloutLifecycleHooks(),
  ): CalloutHandle {
    val resolved = CalloutConfiguration.resolvingPreset(
      configuration,
      defaultConfiguration,
      placement,
      CalloutKind.Tooltip,
    )
    return FkCallout.show(
      controller = controller,
      content = CalloutContent.Message(message),
      anchor = anchor,
      sourceRectInWindow = sourceRectInWindow,
      configuration = resolved,
      hooks = hooks,
    )
  }

  fun show(
    controller: CalloutController,
    icon: CalloutIcon,
    message: String,
    anchor: CalloutAnchorState,
    sourceRectInWindow: Rect? = null,
    placement: CalloutPlacement = CalloutPlacement.Automatic,
    configuration: CalloutConfiguration? = null,
    hooks: CalloutLifecycleHooks = CalloutLifecycleHooks(),
  ): CalloutHandle {
    val resolved = CalloutConfiguration.resolvingPreset(
      configuration,
      defaultConfiguration,
      placement,
      CalloutKind.Tooltip,
    )
    return FkCallout.show(
      controller = controller,
      content = CalloutContent.IconMessage(icon, message),
      anchor = anchor,
      sourceRectInWindow = sourceRectInWindow,
      configuration = resolved,
      hooks = hooks,
    )
  }

  fun dismissActive(
    controller: CalloutController,
    reason: CalloutDismissReason = CalloutDismissReason.Manual,
  ) {
    controller.dismissActive(reason)
  }

  fun dismiss(
    controller: CalloutController,
    handle: CalloutHandle,
    reason: CalloutDismissReason = CalloutDismissReason.Manual,
  ) {
    controller.dismiss(handle, reason)
  }
}
