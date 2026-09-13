package com.fk.core.permissions

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Low-level Manifest permission helpers used by [PermissionsManager].
 */
internal object PermissionRuntime {
  fun isGranted(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

  fun shouldShowRationale(activity: ComponentActivity, permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

  /**
   * Launches the system multi-permission dialog via [ComponentActivity.activityResultRegistry].
   *
   * Uses dynamic registration so library code can request permissions without requiring
   * the host to call `registerForActivityResult` in `onCreate`.
   */
  suspend fun requestManifestPermissions(
    activity: ComponentActivity,
    permissions: Array<String>,
  ): Map<String, Boolean> {
    if (permissions.isEmpty()) return emptyMap()
    return suspendCancellableCoroutine { continuation ->
      val key = "fk.permissions.${UUID.randomUUID()}"
      lateinit var launcher: ActivityResultLauncher<Array<String>>
      launcher = activity.activityResultRegistry.register(
        key,
        ActivityResultContracts.RequestMultiplePermissions(),
      ) { result ->
        runCatching { launcher.unregister() }
        if (continuation.isActive) {
          continuation.resume(result)
        }
      }
      continuation.invokeOnCancellation {
        runCatching { launcher.unregister() }
      }
      launcher.launch(permissions)
    }
  }
}
