package com.fk.ui.toast

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.UUID

/**
 * Toast package hub — unified queue for toast / HUD / snackbar.
 *
 * Conceptually aligned with iOS `FKUIKit` Toast (`FKToast` / `FKHUD` / `FKSnackbar`).
 */
object Toast {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.0"

  val DefaultConfiguration: ToastConfiguration = ToastConfiguration()
}

/** Presentation kind. */
enum class ToastKind {
  /** Brief centered (or positioned) message. */
  Toast,

  /** Blocking / centered status or loading HUD. */
  Hud,

  /** Bottom bar with optional action. */
  Snackbar,
}

/** Visual / semantic style. */
enum class ToastStyle {
  Normal,
  Success,
  Error,
  Warning,
  Info,
  Loading,
}

/** On-screen anchor. */
enum class ToastPosition {
  Top,
  Center,
  Bottom,
}

/** How a new item interacts with the active display. */
enum class ToastPresentationStrategy {
  /** Finish current, then show next (default). */
  Sequential,

  /** Replace the currently visible item immediately. */
  ReplaceActive,
}

/** Queue knobs for [ToastController]. */
@Immutable
data class ToastQueueConfiguration(
  val maxConcurrent: Int = 1,
  val presentationStrategy: ToastPresentationStrategy = ToastPresentationStrategy.Sequential,
)

/**
 * Per-item presentation options.
 *
 * @property durationMs Auto-dismiss delay; `0` means sticky until dismiss / timeout.
 * @property timeoutMs Hard timeout for sticky items (e.g. HUD loading); `null` = none.
 * @property interceptTouches When true (typical HUD), blocks interaction behind the toast.
 */
@Immutable
data class ToastConfiguration(
  val kind: ToastKind = ToastKind.Toast,
  val style: ToastStyle = ToastStyle.Normal,
  val position: ToastPosition? = null,
  val durationMs: Long = 2_000L,
  val timeoutMs: Long? = null,
  val interceptTouches: Boolean = false,
  val maxWidth: Dp = 320.dp,
) {
  fun resolvedPosition(): ToastPosition =
    position ?: when (kind) {
      ToastKind.Snackbar -> ToastPosition.Bottom
      ToastKind.Hud, ToastKind.Toast -> ToastPosition.Center
    }
}

/**
 * Enqueue payload.
 *
 * [onAction] is excluded from equals/hashCode (callback identity).
 */
@Stable
class ToastRequest(
  val message: String,
  val title: String? = null,
  val actionLabel: String? = null,
  val configuration: ToastConfiguration = Toast.DefaultConfiguration,
  val id: String = UUID.randomUUID().toString(),
  val onAction: (() -> Unit)? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ToastRequest) return false
    return id == other.id &&
      message == other.message &&
      title == other.title &&
      actionLabel == other.actionLabel &&
      configuration == other.configuration
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + message.hashCode()
    result = 31 * result + (title?.hashCode() ?: 0)
    result = 31 * result + (actionLabel?.hashCode() ?: 0)
    result = 31 * result + configuration.hashCode()
    return result
  }
}

/** Handle returned by [ToastController.show] / [ToastController.enqueue]. */
@Immutable
data class ToastHandle(
  val id: String,
)

/** Currently visible toast snapshot for the host. */
@Immutable
data class ToastEntry(
  val request: ToastRequest,
  val presentedAtEpochMs: Long = System.currentTimeMillis(),
)
