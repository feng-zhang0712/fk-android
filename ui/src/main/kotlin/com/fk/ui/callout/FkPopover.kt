package com.fk.ui.callout

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

/**
 * Richer anchored panel with popover defaults (light card, manual dismiss).
 *
 * Supports title/body, actions, menus, coach marks, and custom content.
 */
object FkPopover {
  var defaultConfiguration: CalloutConfiguration
    get() = Callout.popoverConfiguration
    set(value) {
      Callout.popoverConfiguration = value
    }

  var menuConfiguration: CalloutConfiguration
    get() = Callout.menuConfiguration
    set(value) {
      Callout.menuConfiguration = value
    }

  fun isPresenting(controller: CalloutController): Boolean = controller.isPresenting

  fun show(
    controller: CalloutController,
    title: String,
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
      CalloutKind.Popover,
    )
    return FkCallout.show(
      controller = controller,
      content = CalloutContent.TitleSubtitle(title, message),
      anchor = anchor,
      sourceRectInWindow = sourceRectInWindow,
      configuration = resolved,
      hooks = hooks,
    )
  }

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
      CalloutKind.Popover,
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
    header: CalloutHeaderPanel,
    body: String,
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
      CalloutKind.Popover,
    )
    return FkCallout.show(
      controller = controller,
      content = CalloutContent.HeaderPanel(header, body),
      anchor = anchor,
      sourceRectInWindow = sourceRectInWindow,
      configuration = resolved,
      hooks = hooks,
    )
  }

  fun show(
    controller: CalloutController,
    message: String,
    actions: List<CalloutAction>,
    actionHandlers: Map<String, () -> Unit>,
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
      CalloutKind.Popover,
    )
    return FkCallout.show(
      controller = controller,
      content = CalloutContent.MessageWithActions(message, actions),
      anchor = anchor,
      sourceRectInWindow = sourceRectInWindow,
      configuration = resolved,
      hooks = hooks,
      actionHandlers = actionHandlers,
    )
  }

  fun show(
    controller: CalloutController,
    customContent: @Composable () -> Unit,
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
      CalloutKind.Popover,
    )
    return FkCallout.show(
      controller = controller,
      content = CalloutContent.Custom(customContent),
      anchor = anchor,
      sourceRectInWindow = sourceRectInWindow,
      configuration = resolved,
      hooks = hooks,
    )
  }

  fun showCoachMark(
    controller: CalloutController,
    content: CalloutCoachMarkContent,
    anchor: CalloutAnchorState,
    sourceRectInWindow: Rect? = null,
    placement: CalloutPlacement = CalloutPlacement.Bottom,
    primaryAction: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    configuration: CalloutConfiguration? = null,
    hooks: CalloutLifecycleHooks = CalloutLifecycleHooks(),
  ): CalloutHandle {
    var resolved = CalloutConfiguration.resolvingPreset(
      configuration,
      defaultConfiguration,
      placement,
      CalloutKind.Popover,
    )
    if (configuration == null) {
      resolved = resolved.copy(
        backdrop = CalloutBackdropStyle(showsDimmedBackdrop = true, spotlightsAnchor = true),
      )
    }
    val handlers = primaryAction?.let { mapOf(content.primaryActionTitle to it) }.orEmpty()
    return FkCallout.show(
      controller = controller,
      content = CalloutContent.CoachMark(content),
      anchor = anchor,
      sourceRectInWindow = sourceRectInWindow,
      configuration = resolved,
      hooks = hooks,
      actionHandlers = handlers,
      closeHandler = onClose,
    )
  }

  fun showMenu(
    controller: CalloutController,
    menu: CalloutMenu,
    anchor: CalloutAnchorState,
    sourceRectInWindow: Rect? = null,
    placement: CalloutPlacement = CalloutPlacement.BottomStart,
    onSelect: ((CalloutMenuItem) -> Unit)? = null,
    configuration: CalloutConfiguration? = null,
    hooks: CalloutLifecycleHooks = CalloutLifecycleHooks(),
  ): CalloutHandle {
    val resolved = CalloutConfiguration.resolvingPreset(
      configuration,
      menuConfiguration,
      placement,
      CalloutKind.Popover,
    )
    return FkCallout.show(
      controller = controller,
      content = CalloutContent.Menu(menu),
      anchor = anchor,
      sourceRectInWindow = sourceRectInWindow,
      configuration = resolved,
      hooks = hooks,
      menuSelectionHandler = onSelect,
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
