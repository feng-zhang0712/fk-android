package com.fk.business.filter

/**
 * Filter kit package hub — tab strip + expandable panels (hierarchy / dual / tags / list).
 *
 * Conceptually aligned with iOS `FKTabBarFilter` (Compose, not UIKit sheet anchoring).
 */
object FilterKit {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"
}

/** Stable identifier for filter rows, sections, and categories. */
@JvmInline
value class FilterId(val raw: String) {
  override fun toString(): String = raw
}

/** Single vs multiple selection for a section. */
enum class FilterSelectionMode {
  Single,
  Multiple,
  ;

  companion object {
    /**
     * Effective mode = multiple only when both the tab and the section allow it.
     */
    fun effective(
      requested: FilterSelectionMode,
      allowsMultipleFromTab: Boolean,
    ): FilterSelectionMode =
      if (allowsMultipleFromTab && requested == Multiple) Multiple else Single
  }
}

/** Built-in panel recipes (maps to iOS `FKTabBarFilterPanelKind`). */
enum class FilterPanelKind {
  Hierarchy,
  DualHierarchy,
  Tags,
  SingleList,
  Custom,
}

/** Why the expanded panel collapsed. */
enum class FilterDismissReason {
  UserToggledSameTab,
  Backdrop,
  SystemBack,
  Programmatic,
  SwitchingTab,
  SingleSelectAuto,
}

/**
 * How single-select clears other rows in a two-column panel.
 */
enum class TwoColumnSingleSelectionScope {
  /** Only the tapped section’s selection changes. */
  WithinSection,
  /** One selected row across all right-hand sections. */
  GlobalAcrossSections,
}

/**
 * Whether repeated taps that leave selection unchanged invoke callbacks.
 */
enum class TwoColumnReselectBehavior {
  EveryTap,
  OnlyWhenChanged,
  ;

  fun shouldFire(selectionChanged: Boolean): Boolean =
    when (this) {
      EveryTap -> true
      OnlyWhenChanged -> selectionChanged
    }
}

/**
 * How taps on a titled right-hand section header behave.
 */
enum class TwoColumnRightHeaderBehavior {
  /** Title only; not interactive. */
  Standard,
  /** Tap toggles [FilterSection.isCollapsed]. */
  ToggleCollapse,
  /** Tap selects the section as a synthetic header item (always single). */
  SelectableHeader,
}

/** One selectable row, chip, or grid cell. */
data class FilterOptionItem(
  val id: FilterId,
  val title: String,
  val subtitle: String? = null,
  val isSelected: Boolean = false,
  val isEnabled: Boolean = true,
)

/** A titled group of options inside a panel. */
data class FilterSection(
  val id: FilterId,
  val title: String? = null,
  val selectionMode: FilterSelectionMode,
  val items: List<FilterOptionItem>,
  val isCollapsed: Boolean = false,
)

/** Left column categories and right-hand sections keyed by category id. */
data class TwoColumnFilterModel(
  val categories: List<Category>,
  val sectionsByCategoryId: Map<FilterId, List<FilterSection>>,
  /**
   * Section whose header is selected ([TwoColumnRightHeaderBehavior.SelectableHeader]).
   * Cleared when a category or option item is picked.
   */
  val selectedHeaderSectionId: FilterId? = null,
) {
  data class Category(
    val id: FilterId,
    val title: String,
    val isSelected: Boolean = false,
  )

  val selectedCategoryId: FilterId?
    get() = categories.firstOrNull { it.isSelected }?.id
      ?: categories.firstOrNull()?.id
}

/** Payload emitted by a panel when selection changes. */
data class FilterPanelSelection(
  val sectionId: FilterId?,
  val item: FilterOptionItem,
  val effectiveMode: FilterSelectionMode,
)

/** Full selection event for a strip tab. */
data class FilterSelectionContext(
  val tabId: String,
  val panelKind: FilterPanelKind,
  val selection: FilterPanelSelection,
)

/** One filter strip tab. */
data class FilterTab(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val panelKind: FilterPanelKind,
  val allowsMultipleSelection: Boolean = false,
)

/** Configuration for two-column panels. */
data class TwoColumnPanelConfig(
  val singleSelectionScope: TwoColumnSingleSelectionScope = TwoColumnSingleSelectionScope.WithinSection,
  val rightHeaderBehavior: TwoColumnRightHeaderBehavior = TwoColumnRightHeaderBehavior.Standard,
  val reselectBehavior: TwoColumnReselectBehavior = TwoColumnReselectBehavior.EveryTap,
)

/** Host-owned panel payload for a tab. */
sealed class FilterPanelContent {
  data class Hierarchy(
    val model: TwoColumnFilterModel,
    val config: TwoColumnPanelConfig = TwoColumnPanelConfig(
      singleSelectionScope = TwoColumnSingleSelectionScope.WithinSection,
    ),
  ) : FilterPanelContent()

  data class DualHierarchy(
    val model: TwoColumnFilterModel,
    val config: TwoColumnPanelConfig = TwoColumnPanelConfig(
      singleSelectionScope = TwoColumnSingleSelectionScope.GlobalAcrossSections,
      rightHeaderBehavior = TwoColumnRightHeaderBehavior.SelectableHeader,
    ),
  ) : FilterPanelContent()

  data class Tags(
    val sections: List<FilterSection>,
    val columns: Int = 4,
  ) : FilterPanelContent()

  data class SingleList(
    val section: FilterSection,
  ) : FilterPanelContent()

  data class Custom(
    val key: String = "custom",
  ) : FilterPanelContent()
}

/** How filter strip tabs share horizontal space. */
enum class FilterTabWidthMode {
  /** Equal width, no horizontal scroll (iOS `fillEqually`). */
  FillEqually,
  /** Intrinsic widths with horizontal scroll. */
  IntrinsicScrollable,
}

/**
 * Chip / pill visual recipe (tags vs directory grids).
 *
 * Defaults align with SACTrain / iOS `FKFilterAppearance` pill styles.
 */
data class FilterPillStyle(
  val cornerRadiusDp: Float = 6f,
  val horizontalPaddingDp: Float = 8f,
  val verticalPaddingDp: Float = 10f,
  /** Unselected: 1dp muted border (tags / platform chips). */
  val showNormalBorder: Boolean = true,
  /** Selected: solid primary fill + onPrimary text (tags). */
  val selectedFillPrimary: Boolean = true,
  /** Selected: primary text only, clear fill/border (directory). */
  val selectedTextOnly: Boolean = false,
  /** Directory / tags grid column count. */
  val columns: Int = 3,
)

/**
 * Panel height policy relative to the host content area height.
 *
 * - [minDp] / [minScreenFraction]: lower bound (whichever is larger wins)
 * - [maxScreenFraction]: upper bound
 * - When [fillToMinimum] is true (directory), height is pinned to at least the
 *   lower bound even if content is shorter (iOS `screenMinimumFraction`).
 */
data class FilterPanelHeightPolicy(
  val minDp: Float = 80f,
  val minScreenFraction: Float = 0f,
  val maxScreenFraction: Float = 0.55f,
  val fillToMinimum: Boolean = false,
) {
  companion object {
    /** Tags / sort / platform: wrap content up to ~55% screen. */
    val Adaptive: FilterPanelHeightPolicy = FilterPanelHeightPolicy(
      minDp = 80f,
      minScreenFraction = 0f,
      maxScreenFraction = 0.55f,
      fillToMinimum = false,
    )

    /** Course / knowledge directory: ~45–55% host height (scroll inside; not full screen). */
    val Directory: FilterPanelHeightPolicy = FilterPanelHeightPolicy(
      minDp = 200f,
      minScreenFraction = 0.45f,
      maxScreenFraction = 0.55f,
      fillToMinimum = true,
    )
  }
}

/**
 * Visual / layout appearance for [FilterHost].
 *
 * Defaults match iOS equal-width strip + SACTrain pill / height recipes.
 */
data class FilterAppearance(
  val tabWidthMode: FilterTabWidthMode = FilterTabWidthMode.FillEqually,
  val stripHeightDp: Float = 50f,
  val showsStripDivider: Boolean = true,
  val stripHorizontalPaddingDp: Float = 4f,
  val tagsPillStyle: FilterPillStyle = FilterPillStyle(
    showNormalBorder = true,
    selectedFillPrimary = true,
    selectedTextOnly = false,
    columns = 3,
  ),
  val directoryPillStyle: FilterPillStyle = FilterPillStyle(
    showNormalBorder = false,
    selectedFillPrimary = false,
    selectedTextOnly = true,
    columns = 2,
    verticalPaddingDp = 10f,
  ),
  val listTextCentered: Boolean = true,
  val tagsHeight: FilterPanelHeightPolicy = FilterPanelHeightPolicy.Adaptive,
  val directoryHeight: FilterPanelHeightPolicy = FilterPanelHeightPolicy.Directory,
  val listHeight: FilterPanelHeightPolicy = FilterPanelHeightPolicy.Adaptive,
)

/** Feature flags and copy for the filter host. */
data class FilterConfiguration(
  /**
   * Fallback max height fraction when [appearance] height policies are unused.
   * Prefer [FilterAppearance] height policies.
   */
  val panelMaxHeightFraction: Float = 0.55f,
  val appearance: FilterAppearance = FilterAppearance(),
  val strings: FilterStrings = FilterStrings(),
)

/** English default strings (apps replace for localization). */
data class FilterStrings(
  val emptyPanel: String = "No options",
  val dismissScrim: String = "Dismiss filter",
)
