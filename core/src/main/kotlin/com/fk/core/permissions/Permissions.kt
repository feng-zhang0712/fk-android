package com.fk.core.permissions

import android.content.Context

/**
 * Permissions — unified check / request façade over Android runtime permissions.
 *
 * Conceptually aligned with iOS `FKCoreKit` Permissions; Android-shaped Activity Result APIs.
 */
object Permissions {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.2"

  /**
   * Builds a [PermissionsManager].
   *
   * @param context Used for status checks, Settings intent, and request tracking.
   * @param prePromptHandler Optional host UI for [PermissionPrePrompt] before system dialogs.
   */
  fun create(
    context: Context,
    prePromptHandler: PermissionPrePromptHandler? = null,
  ): PermissionsManager =
    PermissionsManager(
      context = context,
      prePromptHandler = prePromptHandler,
    )

  /**
   * Manifest permission names a host should declare for [kind] on the current API level.
   *
   * Empty when the kind needs no runtime permission (e.g. notifications below API 33).
   */
  fun manifestPermissions(kind: PermissionKind): List<String> =
    PermissionManifest.permissionsFor(kind)
}
