package com.fk.sample.catalog

/**
 * Declarative catalog of demo scenarios for the sample app.
 *
 * Navigation is two-level:
 * 1. Home → Core / UI / Business
 * 2. Group → component demos for that module
 *
 * Add a [SampleDestination] under the matching [SampleGroup] when encapsulating
 * a new component. Keep route ids stable — they are used by Navigation Compose.
 */
enum class SampleGroup(
  val title: String,
  val subtitle: String,
) {
  Core(
    title = "Core",
    subtitle = "Foundation contracts and infrastructure",
  ),
  Ui(
    title = "UI",
    subtitle = "Design system and Compose components",
  ),
  Business(
    title = "Business",
    subtitle = "Business composites and feature kits",
  ),
  ;

  /** Navigation route for this group's list screen. */
  val route: String get() = "group/${name.lowercase()}"
}

/**
 * A single navigable demo entry.
 *
 * @property route Navigation route id (stable).
 * @property group Parent module group.
 * @property title List row title.
 * @property description One-line summary shown under the title.
 * @property available When false, the row is shown as coming soon and not navigable.
 */
data class SampleDestination(
  val route: String,
  val group: SampleGroup,
  val title: String,
  val description: String,
  val available: Boolean = true,
) {
  companion object {
    const val HOME = "home"
    const val PLUGGABLE = "core/pluggable"
    const val NETWORK = "core/network"
    const val STORAGE = "core/storage"
    const val LOGGING = "core/logging"
    const val MAPPING = "core/mapping"
    const val SECURITY = "core/security"
    const val I18N = "core/i18n"
    const val PERMISSIONS = "core/permissions"
    const val BIOMETRIC = "core/biometric"
    const val BACKGROUND = "core/background"
    const val NOTIFICATION = "core/notification"
    const val FILE = "core/file"
    const val IMAGE = "core/image"
    const val APP = "core/app"
    const val THEME = "ui/theme"
    const val EMPTY = "ui/empty"
    const val SKELETON = "ui/skeleton"
    const val TOAST = "ui/toast"
    const val LIST = "ui/list"
    const val TEXTFIELD = "ui/textfield"
  }
}

/** Full catalog of component demos. */
object SampleCatalog {
  val destinations: List<SampleDestination> = listOf(
    SampleDestination(
      route = SampleDestination.PLUGGABLE,
      group = SampleGroup.Core,
      title = "Pluggable",
      description = "DI contracts, mocks, session / storage / API smoke demo",
    ),
    SampleDestination(
      route = SampleDestination.NETWORK,
      group = SampleGroup.Core,
      title = "Network",
      description = "OkHttp ApiClient façade over Pluggable contracts",
    ),
    SampleDestination(
      route = SampleDestination.STORAGE,
      group = SampleGroup.Core,
      title = "Storage",
      description = "DataStore key-value + Keystore-encrypted TypedStore",
    ),
    SampleDestination(
      route = SampleDestination.LOGGING,
      group = SampleGroup.Core,
      title = "Logging",
      description = "Logcat + file sink with structured fields",
    ),
    SampleDestination(
      route = SampleDestination.MAPPING,
      group = SampleGroup.Core,
      title = "Mapping",
      description = "JSON conventions + business envelope helpers",
    ),
    SampleDestination(
      route = SampleDestination.SECURITY,
      group = SampleGroup.Core,
      title = "Security",
      description = "Hash / AES / RSA / HMAC + Keystore key store",
    ),
    SampleDestination(
      route = SampleDestination.I18N,
      group = SampleGroup.Core,
      title = "I18n",
      description = "Runtime locale switch + dictionary + formatters",
    ),
    SampleDestination(
      route = SampleDestination.PERMISSIONS,
      group = SampleGroup.Core,
      title = "Permissions",
      description = "Unified check / request over runtime permissions",
    ),
    SampleDestination(
      route = SampleDestination.BIOMETRIC,
      group = SampleGroup.Core,
      title = "Biometric",
      description = "BiometricPrompt façade: capability / policy / errors",
    ),
    SampleDestination(
      route = SampleDestination.BACKGROUND,
      group = SampleGroup.Core,
      title = "Background",
      description = "WorkManager refresh / processing schedule + observe",
    ),
    SampleDestination(
      route = SampleDestination.NOTIFICATION,
      group = SampleGroup.Core,
      title = "Notification",
      description = "Local notification channels + schedule / cancel",
    ),
    SampleDestination(
      route = SampleDestination.FILE,
      group = SampleGroup.Core,
      title = "File",
      description = "Resumable download / upload + pause / resume",
    ),
    SampleDestination(
      route = SampleDestination.IMAGE,
      group = SampleGroup.Core,
      title = "Image",
      description = "ImageLoading contract + Coil adapter",
    ),
    SampleDestination(
      route = SampleDestination.APP,
      group = SampleGroup.Core,
      title = "App",
      description = "Version / deeplink / lifecycle / analytics / startup",
    ),
    SampleDestination(
      route = SampleDestination.THEME,
      group = SampleGroup.Ui,
      title = "Theme",
      description = "Design tokens + FkTheme (color / type / spacing / shadow)",
    ),
    SampleDestination(
      route = SampleDestination.EMPTY,
      group = SampleGroup.Ui,
      title = "Empty",
      description = "Loading / empty / error overlays + resolver",
    ),
    SampleDestination(
      route = SampleDestination.SKELETON,
      group = SampleGroup.Ui,
      title = "Skeleton",
      description = "Shimmer placeholders + list / card presets",
    ),
    SampleDestination(
      route = SampleDestination.TOAST,
      group = SampleGroup.Ui,
      title = "Toast",
      description = "Unified toast / HUD / snackbar queue",
    ),
    SampleDestination(
      route = SampleDestination.LIST,
      group = SampleGroup.Ui,
      title = "List",
      description = "Refresh / load-more / empty-skeleton orchestration",
    ),
    SampleDestination(
      route = SampleDestination.TEXTFIELD,
      group = SampleGroup.Ui,
      title = "TextField",
      description = "Formatting / validation / OTP / counters",
    ),
    SampleDestination(
      route = "business/comment",
      group = SampleGroup.Business,
      title = "Comment",
      description = "Comment list + composer (coming soon)",
      available = false,
    ),
  )

  fun destinationsIn(group: SampleGroup): List<SampleDestination> =
    destinations.filter { it.group == group }

  fun groupOrNull(route: String): SampleGroup? =
    SampleGroup.entries.firstOrNull { it.route == route }
}
