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
      route = "ui/theme",
      group = SampleGroup.Ui,
      title = "Theme",
      description = "Design tokens + FkTheme (coming soon)",
      available = false,
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
