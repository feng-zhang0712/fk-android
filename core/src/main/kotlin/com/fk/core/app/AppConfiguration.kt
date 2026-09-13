package com.fk.core.app

/**
 * Runtime knobs for [App.create] collaborators.
 *
 * @property channel Distribution channel stamped into [AppInfo] / analytics.
 * @property environment Override build environment when non-null.
 * @property analyticsBatchSize Max events per [AnalyticsTracking.flush] upload.
 * @property analyticsMaxBuffer Soft cap on in-memory pending events.
 */
data class AppConfiguration(
  val channel: String = "default",
  val environment: AppBuildEnvironment? = null,
  val analyticsBatchSize: Int = 20,
  val analyticsMaxBuffer: Int = 500,
)

/**
 * Bundled app-infra collaborators for composition roots and samples.
 *
 * Prefer injecting individual interfaces into feature modules over passing this bag.
 */
data class AppServices(
  val info: AppInfo,
  val versionChecker: VersionChecking,
  val deeplinks: DeeplinkRouter,
  val analytics: AnalyticsTracking,
  val startup: StartupTaskManaging,
  val lifecycle: com.fk.core.pluggable.core.AppLifecycleObserver,
)
