package com.fk.ui.callout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

/** Reason reported when a callout finishes dismissal. */
enum class CalloutDismissReason {
  Manual,
  TapOutside,
  Timeout,
  Replaced,
  AnchorUnavailable,
  MenuSelection,
  ActionTriggered,
  CloseButton,
}

/** Stable reference to one presented callout. */
@Immutable
data class CalloutHandle(
  val id: String,
)

/** Lifecycle callbacks for one callout request. */
data class CalloutLifecycleHooks(
  val willShow: ((String) -> Unit)? = null,
  val didShow: ((String) -> Unit)? = null,
  val willDismiss: ((String, CalloutDismissReason) -> Unit)? = null,
  val didDismiss: ((String, CalloutDismissReason) -> Unit)? = null,
)

/** Action button shown inside a callout footer. */
@Immutable
data class CalloutAction(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val style: Style = Style.Default,
  val accessibilityLabel: String? = null,
) {
  enum class Style {
    Default,
    Primary,
  }
}

/** Icon descriptor for [CalloutContent.IconMessage]. */
@Immutable
data class CalloutIcon(
  val imageVector: ImageVector? = null,
  val tint: Color? = null,
  val contentDescription: String? = null,
)

/** Colored header strip for split popovers. */
@Immutable
data class CalloutHeaderPanel(
  val title: String,
  val backgroundColor: Color? = null,
  val textColor: Color? = null,
)

/** Onboarding / coach-mark payload. */
@Immutable
data class CalloutCoachMarkContent(
  val title: String,
  val message: String,
  val primaryActionTitle: String = "Got it",
  val showsCloseButton: Boolean = true,
)

/** One selectable or actionable row in a callout menu. */
@Immutable
data class CalloutMenuItem(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val subtitle: String? = null,
  val icon: ImageVector? = null,
  val isSelected: Boolean = false,
  val tint: Color? = null,
  val isEnabled: Boolean = true,
  val isDestructive: Boolean = false,
)

/** A group of menu rows separated from adjacent sections by a divider. */
@Immutable
data class CalloutMenuSection(
  val items: List<CalloutMenuItem>,
)

/** Menu payload for dropdown / action popovers. */
@Immutable
data class CalloutMenu(
  val header: String? = null,
  val sections: List<CalloutMenuSection>,
)

/** Text or custom UI hosted inside a callout bubble. */
sealed interface CalloutContent {
  data class Message(val text: String) : CalloutContent

  data class TitleSubtitle(
    val title: String,
    val message: String,
  ) : CalloutContent

  data class IconMessage(
    val icon: CalloutIcon,
    val message: String,
  ) : CalloutContent

  data class MessageWithActions(
    val message: String,
    val actions: List<CalloutAction>,
  ) : CalloutContent

  data class HeaderPanel(
    val header: CalloutHeaderPanel,
    val body: String,
  ) : CalloutContent

  data class CoachMark(
    val content: CalloutCoachMarkContent,
  ) : CalloutContent

  data class Menu(
    val menu: CalloutMenu,
  ) : CalloutContent

  /**
   * Fully custom bubble interior.
   *
   * The composable is invoked by [CalloutHost] while the session is active.
   */
  class Custom(
    val content: @Composable () -> Unit,
  ) : CalloutContent
}

/**
 * Bundles anchor, content, configuration, lifecycle hooks, and interaction handlers.
 */
data class CalloutRequest(
  val content: CalloutContent,
  val anchorId: String,
  val sourceRectInWindow: androidx.compose.ui.geometry.Rect? = null,
  val configuration: CalloutConfiguration = CalloutConfiguration(),
  val hooks: CalloutLifecycleHooks = CalloutLifecycleHooks(),
  val actionHandlers: Map<String, () -> Unit> = emptyMap(),
  val menuSelectionHandler: ((CalloutMenuItem) -> Unit)? = null,
  val closeHandler: (() -> Unit)? = null,
  val id: String = UUID.randomUUID().toString(),
)

/** Currently visible callout session snapshot for the host. */
@Immutable
data class CalloutEntry(
  val request: CalloutRequest,
  val presentedAtEpochMs: Long = System.currentTimeMillis(),
)
