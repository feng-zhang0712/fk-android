package com.fk.ui.callout

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

/**
 * Advanced presenter for anchored callout bubbles.
 *
 * Prefer [FkTooltip] / [FkPopover] for typical call sites; use this when you need arbitrary
 * [CalloutContent], [showOrUpdate], or fine-grained session control.
 */
object FkCallout {
  var defaultConfiguration: CalloutConfiguration
    get() = Callout.defaultConfiguration
    set(value) {
      Callout.defaultConfiguration = value
    }

  fun isPresenting(controller: CalloutController): Boolean = controller.isPresenting

  fun show(
    controller: CalloutController,
    request: CalloutRequest,
  ): CalloutHandle = controller.show(request)

  fun show(
    controller: CalloutController,
    content: CalloutContent,
    anchor: CalloutAnchorState,
    sourceRectInWindow: Rect? = null,
    configuration: CalloutConfiguration = defaultConfiguration,
    hooks: CalloutLifecycleHooks = CalloutLifecycleHooks(),
    actionHandlers: Map<String, () -> Unit> = emptyMap(),
    menuSelectionHandler: ((CalloutMenuItem) -> Unit)? = null,
    closeHandler: (() -> Unit)? = null,
  ): CalloutHandle {
    controller.registerAnchor(anchor)
    return controller.show(
      CalloutRequest(
        content = content,
        anchorId = anchor.id,
        sourceRectInWindow = sourceRectInWindow,
        configuration = configuration,
        hooks = hooks,
        actionHandlers = actionHandlers,
        menuSelectionHandler = menuSelectionHandler,
        closeHandler = closeHandler,
      ),
    )
  }

  fun showOrUpdate(
    controller: CalloutController,
    request: CalloutRequest,
  ): CalloutHandle = controller.showOrUpdate(request)

  fun dismiss(
    controller: CalloutController,
    id: String,
    reason: CalloutDismissReason = CalloutDismissReason.Manual,
  ) {
    controller.dismiss(id, reason)
  }

  fun dismiss(
    controller: CalloutController,
    handle: CalloutHandle,
    reason: CalloutDismissReason = CalloutDismissReason.Manual,
  ) {
    controller.dismiss(handle, reason)
  }

  fun dismissActive(
    controller: CalloutController,
    reason: CalloutDismissReason = CalloutDismissReason.Manual,
  ) {
    controller.dismissActive(reason)
  }

  fun update(
    controller: CalloutController,
    id: String,
    content: CalloutContent,
    configuration: CalloutConfiguration? = null,
  ): Boolean = controller.update(id, content, configuration)

  fun update(
    controller: CalloutController,
    handle: CalloutHandle,
    content: CalloutContent,
    configuration: CalloutConfiguration? = null,
  ): Boolean = controller.update(handle, content, configuration)
}
