package com.fk.core.permissions

import android.content.Context
import android.content.SharedPreferences

/**
 * Remembers which Manifest permissions have been requested at least once,
 * so [PermissionStatus.NotDetermined] can be distinguished from permanent deny.
 */
internal class PermissionRequestTracker(
  context: Context,
  preferencesName: String = "fk_permissions",
) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

  fun hasRequested(permission: String): Boolean =
    prefs.getBoolean(key(permission), false)

  fun markRequested(permissions: Collection<String>) {
    if (permissions.isEmpty()) return
    prefs.edit().apply {
      permissions.forEach { putBoolean(key(it), true) }
      apply()
    }
  }

  private fun key(permission: String): String = "requested:$permission"
}
