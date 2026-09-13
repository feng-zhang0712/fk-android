package com.fk.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Maps [PermissionKind] to Manifest permission names for the running API level.
 */
internal object PermissionManifest {
  fun permissionsFor(kind: PermissionKind): List<String> =
    when (kind) {
      PermissionKind.Camera -> listOf(Manifest.permission.CAMERA)
      PermissionKind.Microphone -> listOf(Manifest.permission.RECORD_AUDIO)
      PermissionKind.PhotoLibrary -> photoLibraryPermissions()
      PermissionKind.LocationWhenInUse -> foregroundLocationPermissions()
      PermissionKind.LocationAlways -> buildList {
        addAll(foregroundLocationPermissions())
        addAll(backgroundLocationPermissions())
      }
      PermissionKind.Notifications -> notificationPermissions()
      PermissionKind.Bluetooth -> bluetoothPermissions()
      PermissionKind.Calendar -> listOf(Manifest.permission.READ_CALENDAR)
    }

  fun foregroundLocationPermissions(): List<String> = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
  )

  fun backgroundLocationPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= 29) {
      listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
      emptyList()
    }

  /**
   * Whether the device exposes the hardware / feature needed for [kind].
   *
   * Kinds without a clear system feature check return `true`.
   */
  fun isCapabilityAvailable(context: Context, kind: PermissionKind): Boolean {
    val pm = context.packageManager
    return when (kind) {
      PermissionKind.Camera ->
        pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
          pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
      PermissionKind.Microphone ->
        pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
      PermissionKind.Bluetooth ->
        pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
      else -> true
    }
  }

  private fun photoLibraryPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= 33) {
      listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
      listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

  private fun notificationPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= 33) {
      listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      emptyList() // install-time / no runtime prompt
    }

  private fun bluetoothPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= 31) {
      listOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
      )
    } else {
      emptyList()
    }
}
