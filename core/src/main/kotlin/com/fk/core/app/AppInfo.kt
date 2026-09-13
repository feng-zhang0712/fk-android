package com.fk.core.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * App / device identity for version checks and analytics common parameters.
 *
 * Conceptually aligned with iOS `FKBusinessInfoProviding`.
 */
interface AppInfo {
  val packageName: String
  val versionName: String
  val versionCode: Long
  val sdkInt: Int
  val deviceModel: String
  val channel: String
  val environment: AppBuildEnvironment

  /** Human-readable label, e.g. `1.2.3 (42)`. */
  fun versionLabel(): String = "$versionName ($versionCode)"

  /** Snapshot used by [VersionChecking]. */
  fun metadata(): AppMetadata =
    AppMetadata(
      packageName = packageName,
      versionName = versionName,
      versionCode = versionCode,
      channel = channel,
      environment = environment,
    )

  /** Default analytics common parameters. */
  fun commonParameters(): Map<String, String> =
    mapOf(
      "package_name" to packageName,
      "app_version" to versionName,
      "build" to versionCode.toString(),
      "os" to "Android",
      "os_version" to sdkInt.toString(),
      "device_model" to deviceModel,
      "channel" to channel,
      "env" to environment.name.lowercase(),
    )
}

/** Default [AppInfo] from [PackageManager]. */
class AndroidAppInfo(
  context: Context,
  override val channel: String = "default",
  environmentOverride: AppBuildEnvironment? = null,
) : AppInfo {
  private val appContext = context.applicationContext
  private val packageInfo = run {
    val pm = appContext.packageManager
    val name = appContext.packageName
    if (Build.VERSION.SDK_INT >= 33) {
      pm.getPackageInfo(name, PackageManager.PackageInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      pm.getPackageInfo(name, 0)
    }
  }

  override val packageName: String = appContext.packageName

  override val versionName: String = packageInfo.versionName ?: "0"

  override val versionCode: Long =
    if (Build.VERSION.SDK_INT >= 28) {
      packageInfo.longVersionCode
    } else {
      @Suppress("DEPRECATION")
      packageInfo.versionCode.toLong()
    }

  override val sdkInt: Int = Build.VERSION.SDK_INT

  override val deviceModel: String = Build.MODEL

  override val environment: AppBuildEnvironment = environmentOverride
    ?: if ((appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      AppBuildEnvironment.Debug
    } else {
      AppBuildEnvironment.Release
    }
}
