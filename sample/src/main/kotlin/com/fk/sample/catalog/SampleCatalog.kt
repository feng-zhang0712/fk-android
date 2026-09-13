package com.fk.sample.catalog

/**
 * Declarative catalog of demo scenarios for the sample app.
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
}

/**
 * A single navigable demo entry.
 *
 * @property route Navigation route id (stable).
 * @property group Catalog section.
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
  }
}

/** Full catalog shown on the home hub. */
object SampleCatalog {
  val destinations: List<SampleDestination> = listOf(
    SampleDestination(
      route = SampleDestination.PLUGGABLE,
      group = SampleGroup.Core,
      title = "Pluggable",
      description = "DI contracts, mocks, session / storage / API smoke demo",
    ),
    // Placeholders help show grouping before later packages land.
    SampleDestination(
      route = "core/network",
      group = SampleGroup.Core,
      title = "Network",
      description = "OkHttp client façade (coming soon)",
      available = false,
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

  fun grouped(): List<Pair<SampleGroup, List<SampleDestination>>> =
    SampleGroup.entries.mapNotNull { group ->
      val items = destinations.filter { it.group == group }
      if (items.isEmpty()) null else group to items
    }
}
