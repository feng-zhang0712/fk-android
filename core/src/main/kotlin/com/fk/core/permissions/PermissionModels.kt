package com.fk.core.permissions

/**
 * High-level permission domains exposed by the façade.
 *
 * Each kind maps to one or more Android Manifest permissions
 * (see [Permissions.manifestPermissions]). Conceptually aligned with iOS `FKPermissionKind`.
 */
enum class PermissionKind {
  Camera,
  Microphone,
  PhotoLibrary,
  LocationWhenInUse,
  LocationAlways,
  Notifications,
  Bluetooth,
  Calendar,
}

/**
 * Unified authorization state across Android runtime permissions.
 *
 * - [NotDetermined]: never requested (tracked locally; Android itself collapses this with deny)
 * - [Granted]: access is available (or no runtime permission is required on this API)
 * - [Denied]: denied but the system dialog may still be shown
 * - [PermanentlyDenied]: denied and the system will not show the dialog again
 * - [Unavailable]: required hardware / feature is missing on this device
 */
enum class PermissionStatus {
  NotDetermined,
  Granted,
  Denied,
  PermanentlyDenied,
  Unavailable,
}

/**
 * Errors produced by a permission request flow.
 */
sealed class PermissionError {
  /** User dismissed an optional pre-prompt before the system dialog. */
  data object PrePromptCancelled : PermissionError()

  /** Required capability is unavailable on this device. */
  data object Unavailable : PermissionError()

  /** Host-provided custom failure. */
  data class Custom(val message: String) : PermissionError()
}

/**
 * Optional education UI content shown before the system permission dialog.
 *
 * The library does not render UI; hosts supply a [PermissionPrePromptHandler].
 */
data class PermissionPrePrompt(
  val title: String,
  val message: String,
  val confirmTitle: String = "Continue",
  val cancelTitle: String = "Not now",
)

/**
 * Input for a single permission request.
 */
data class PermissionRequest(
  val kind: PermissionKind,
  val prePrompt: PermissionPrePrompt? = null,
)

/**
 * Outcome of checking or requesting a permission.
 */
data class PermissionResult(
  val kind: PermissionKind,
  val status: PermissionStatus,
  val error: PermissionError? = null,
) {
  /** Whether the status grants usable access. */
  val isGranted: Boolean
    get() = status == PermissionStatus.Granted
}

/**
 * Host callback that presents [PermissionPrePrompt] UI.
 *
 * @return `true` to continue with the system dialog; `false` to cancel.
 */
fun interface PermissionPrePromptHandler {
  suspend fun show(prompt: PermissionPrePrompt): Boolean
}
