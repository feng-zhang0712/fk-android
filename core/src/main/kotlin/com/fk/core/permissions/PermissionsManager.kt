package com.fk.core.permissions

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.fk.core.pluggable.core.ObservationToken
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Unified check / request façade over Android runtime permissions.
 *
 * Conceptually aligned with iOS `FKPermissions`. Prefer injecting this over a process singleton.
 */
class PermissionsManager(
  context: Context,
  private var prePromptHandler: PermissionPrePromptHandler? = null,
) {
  private val appContext: Context = context.applicationContext
  private val tracker = PermissionRequestTracker(appContext)
  private val lock = Any()
  private val observers = CopyOnWriteArrayList<Pair<UUID, (PermissionKind, PermissionStatus) -> Unit>>()
  private val statusCache = LinkedHashMap<PermissionKind, PermissionStatus>()
  private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

  /** Installs or clears the optional pre-prompt UI handler. */
  fun setPrePromptHandler(handler: PermissionPrePromptHandler?) {
    prePromptHandler = handler
  }

  /**
   * Returns the current status for [kind] without showing a system dialog.
   *
   * Pass [activity] when available so [PermissionStatus.PermanentlyDenied] can be detected
   * via `shouldShowRequestPermissionRationale`.
   */
  fun status(kind: PermissionKind, activity: ComponentActivity? = null): PermissionStatus {
    if (!PermissionManifest.isCapabilityAvailable(appContext, kind)) {
      return PermissionStatus.Unavailable
    }

    val permissions = PermissionManifest.permissionsFor(kind)
    if (permissions.isEmpty()) {
      return PermissionStatus.Granted
    }

    if (isKindGranted(kind)) return PermissionStatus.Granted

    val anyRequested = permissions.any { tracker.hasRequested(it) }
    if (!anyRequested) return PermissionStatus.NotDetermined

    if (activity != null) {
      val deniedPermissions = permissions.filter { !PermissionRuntime.isGranted(appContext, it) }
      val canShowRationale = deniedPermissions.any {
        PermissionRuntime.shouldShowRationale(activity, it)
      }
      if (deniedPermissions.isNotEmpty() && !canShowRationale) {
        return PermissionStatus.PermanentlyDenied
      }
    }
    return PermissionStatus.Denied
  }

  /** Requests a single permission, optionally showing a host pre-prompt first. */
  suspend fun request(
    activity: ComponentActivity,
    request: PermissionRequest,
  ): PermissionResult {
    val current = status(request.kind, activity)
    when (current) {
      PermissionStatus.Granted ->
        return PermissionResult(request.kind, PermissionStatus.Granted)
      PermissionStatus.Unavailable ->
        return PermissionResult(
          request.kind,
          PermissionStatus.Unavailable,
          PermissionError.Unavailable,
        )
      PermissionStatus.PermanentlyDenied ->
        return PermissionResult(request.kind, PermissionStatus.PermanentlyDenied)
      else -> Unit
    }

    if (!showPrePromptIfNeeded(request.prePrompt)) {
      return PermissionResult(
        kind = request.kind,
        status = status(request.kind, activity),
        error = PermissionError.PrePromptCancelled,
      )
    }

    when (request.kind) {
      PermissionKind.LocationAlways -> requestLocationAlways(activity)
      else -> {
        val permissions = PermissionManifest.permissionsFor(request.kind)
        if (permissions.isNotEmpty()) {
          PermissionRuntime.requestManifestPermissions(activity, permissions.toTypedArray())
          tracker.markRequested(permissions)
        }
      }
    }

    val next = status(request.kind, activity)
    notifyIfChanged(request.kind, next)
    return PermissionResult(request.kind, next)
  }

  /** Convenience overload without a [PermissionRequest] wrapper. */
  suspend fun request(
    activity: ComponentActivity,
    kind: PermissionKind,
    prePrompt: PermissionPrePrompt? = null,
  ): PermissionResult = request(activity, PermissionRequest(kind, prePrompt))

  /**
   * Requests multiple kinds in one system dialog when possible (union of Manifest permissions),
   * then returns per-kind results.
   *
   * [PermissionKind.LocationAlways] is handled in a second step after foreground location,
   * per Android platform rules.
   */
  suspend fun request(
    activity: ComponentActivity,
    kinds: List<PermissionKind>,
  ): Map<PermissionKind, PermissionResult> {
    if (kinds.isEmpty()) return emptyMap()

    val distinct = kinds.distinct()
    val includeAlways = PermissionKind.LocationAlways in distinct
    val standard = distinct.filter { it != PermissionKind.LocationAlways }

    val allPermissions = standard
      .filter { PermissionManifest.isCapabilityAvailable(appContext, it) }
      .flatMap { PermissionManifest.permissionsFor(it) }
      .distinct()

    if (allPermissions.isNotEmpty()) {
      PermissionRuntime.requestManifestPermissions(activity, allPermissions.toTypedArray())
      tracker.markRequested(allPermissions)
    }

    if (includeAlways) {
      requestLocationAlways(activity)
    }

    return distinct.associateWith { kind ->
      val next = status(kind, activity)
      notifyIfChanged(kind, next)
      val error = when (next) {
        PermissionStatus.Unavailable -> PermissionError.Unavailable
        else -> null
      }
      PermissionResult(kind, next, error)
    }
  }

  /**
   * Requests each [PermissionRequest] sequentially (honors per-item pre-prompts).
   */
  suspend fun requestEach(
    activity: ComponentActivity,
    requests: List<PermissionRequest>,
  ): Map<PermissionKind, PermissionResult> {
    val output = LinkedHashMap<PermissionKind, PermissionResult>()
    for (request in requests) {
      output[request.kind] = request(activity, request)
    }
    return output
  }

  /**
   * Opens the application details page in system Settings.
   *
   * @return `true` when an activity was started.
   */
  fun openAppSettings(context: Context = appContext): Boolean {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }

  /**
   * Observes status changes for [kinds].
   *
   * Invokes [handler] immediately with current statuses, then again when an Activity resumes
   * (e.g. returning from Settings) or after a [request].
   *
   * @param activity Optional; improves [PermissionStatus.PermanentlyDenied] detection.
   */
  fun observeStatusChanges(
    kinds: Collection<PermissionKind> = PermissionKind.entries,
    activity: ComponentActivity? = null,
    handler: (PermissionKind, PermissionStatus) -> Unit,
  ): ObservationToken {
    val id = UUID.randomUUID()
    val watched = kinds.toList()
    observers += id to handler

    watched.forEach { kind ->
      val current = status(kind, activity)
      synchronized(lock) { statusCache[kind] = current }
      handler(kind, current)
    }

    ensureLifecycleObserver()

    return ObservationToken {
      observers.removeAll { it.first == id }
      if (observers.isEmpty()) {
        removeLifecycleObserver()
      }
    }
  }

  /** Re-reads statuses and notifies observers of changes (e.g. from a Compose `ON_RESUME`). */
  fun refreshObservedStatuses(activity: ComponentActivity? = null) {
    val kinds = synchronized(lock) { statusCache.keys.toList() }
      .ifEmpty { PermissionKind.entries }
    kinds.forEach { kind ->
      notifyIfChanged(kind, status(kind, activity))
    }
  }

  private suspend fun showPrePromptIfNeeded(prompt: PermissionPrePrompt?): Boolean {
    if (prompt == null) return true
    val handler = prePromptHandler ?: return true
    return handler.show(prompt)
  }

  /**
   * Android requires foreground location before [Manifest.permission.ACCESS_BACKGROUND_LOCATION]
   * can be requested (API 29+).
   */
  private suspend fun requestLocationAlways(activity: ComponentActivity) {
    val foreground = PermissionManifest.foregroundLocationPermissions()
    if (!foreground.any { PermissionRuntime.isGranted(appContext, it) }) {
      PermissionRuntime.requestManifestPermissions(activity, foreground.toTypedArray())
      tracker.markRequested(foreground)
    }
    if (!foreground.any { PermissionRuntime.isGranted(appContext, it) }) {
      return
    }

    val background = PermissionManifest.backgroundLocationPermissions()
    if (background.isNotEmpty() &&
      background.any { !PermissionRuntime.isGranted(appContext, it) }
    ) {
      PermissionRuntime.requestManifestPermissions(activity, background.toTypedArray())
      tracker.markRequested(background)
    }
  }

  private fun isKindGranted(kind: PermissionKind): Boolean {
    val permissions = PermissionManifest.permissionsFor(kind)
    if (permissions.isEmpty()) return true
    return when (kind) {
      PermissionKind.LocationWhenInUse ->
        PermissionManifest.foregroundLocationPermissions()
          .any { PermissionRuntime.isGranted(appContext, it) }
      PermissionKind.LocationAlways -> {
        val foreground = PermissionManifest.foregroundLocationPermissions()
          .any { PermissionRuntime.isGranted(appContext, it) }
        val background = PermissionManifest.backgroundLocationPermissions()
        val backgroundOk = background.isEmpty() ||
          background.all { PermissionRuntime.isGranted(appContext, it) }
        foreground && backgroundOk
      }
      else -> permissions.all { PermissionRuntime.isGranted(appContext, it) }
    }
  }

  private fun notifyIfChanged(kind: PermissionKind, next: PermissionStatus) {
    val previous = synchronized(lock) {
      val old = statusCache[kind]
      statusCache[kind] = next
      old
    }
    if (previous == next) return
    observers.forEach { (_, handler) ->
      runCatching { handler(kind, next) }
    }
  }

  private fun ensureLifecycleObserver() {
    if (lifecycleCallbacks != null) return
    val application = appContext as? Application ?: return
    val callbacks = object : Application.ActivityLifecycleCallbacks {
      override fun onActivityResumed(activity: android.app.Activity) {
        refreshObservedStatuses(activity as? ComponentActivity)
      }

      override fun onActivityCreated(a: android.app.Activity, b: Bundle?) = Unit
      override fun onActivityStarted(a: android.app.Activity) = Unit
      override fun onActivityPaused(a: android.app.Activity) = Unit
      override fun onActivityStopped(a: android.app.Activity) = Unit
      override fun onActivitySaveInstanceState(a: android.app.Activity, b: Bundle) = Unit
      override fun onActivityDestroyed(a: android.app.Activity) = Unit
    }
    lifecycleCallbacks = callbacks
    application.registerActivityLifecycleCallbacks(callbacks)
  }

  private fun removeLifecycleObserver() {
    val callbacks = lifecycleCallbacks ?: return
    (appContext as? Application)?.unregisterActivityLifecycleCallbacks(callbacks)
    lifecycleCallbacks = null
  }
}
