package com.fk.core.app

import android.net.Uri

/** Build / distribution environment label. */
enum class AppBuildEnvironment {
  Debug,
  Release,
}

/**
 * Local app identity used for version checks and analytics common params.
 *
 * Conceptually aligned with iOS `FKAppMetadata` / `FKBusinessInfoProviding`.
 */
data class AppMetadata(
  val packageName: String,
  val versionName: String,
  val versionCode: Long,
  val channel: String = "default",
  val environment: AppBuildEnvironment = AppBuildEnvironment.Release,
)

/** Remote version payload from a host-provided provider. */
data class RemoteVersionInfo(
  val versionName: String,
  val versionCode: Long? = null,
  val isForceUpdate: Boolean = false,
  val releaseNotes: String? = null,
  val updateUri: Uri? = null,
)

/** Outcome of comparing local vs remote versions. */
enum class UpdateDecision {
  UpToDate,
  OptionalUpdate,
  ForceUpdate,
}

/** Result of [VersionChecking.checkForUpdate]. */
data class VersionCheckResult(
  val local: AppMetadata,
  val remote: RemoteVersionInfo,
  val decision: UpdateDecision,
)

/** Deeplink intake source. */
enum class DeeplinkSource {
  Intent,
  Notification,
  Manual,
  Other,
}

/**
 * Parsed deeplink context.
 *
 * Conceptually aligned with iOS `FKRouteContext` / `FKDeeplinkContext`.
 */
data class RouteContext(
  val uri: Uri,
  val host: String?,
  val path: String,
  val pathSegments: List<String>,
  val query: Map<String, String>,
  val source: DeeplinkSource = DeeplinkSource.Other,
  val extras: Map<String, String> = emptyMap(),
)

/** Result of attempting to handle a route. */
sealed class RouteHandlingResult {
  data object Handled : RouteHandlingResult()
  data object NotHandled : RouteHandlingResult()
  data class Failed(val message: String) : RouteHandlingResult()
}

/** Analytics event kinds. */
enum class AnalyticsEventType {
  PageView,
  Click,
  Custom,
}

/** Buffered analytics event. */
data class AnalyticsEvent(
  val id: String,
  val type: AnalyticsEventType,
  val name: String,
  val parameters: Map<String, String>,
  val timestampEpochMs: Long,
)

/** Startup task priority (higher runs first). */
enum class StartupTaskPriority {
  High,
  Normal,
  Low,
}

/**
 * One launch-time unit of work.
 *
 * Conceptually aligned with iOS `FKStartupTask`.
 */
data class StartupTask(
  val id: String,
  val priority: StartupTaskPriority = StartupTaskPriority.Normal,
  val delayMs: Long = 0L,
  val work: suspend () -> Unit,
)

/** Stable errors for app-infra operations. */
sealed class AppError(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause) {
  class InvalidDeeplink(value: String) :
    AppError("Invalid deeplink: $value")

  class VersionCheckFailed(detail: String, cause: Throwable? = null) :
    AppError("Version check failed: $detail", cause)

  class Unknown(detail: String, cause: Throwable? = null) :
    AppError(detail, cause)
}
