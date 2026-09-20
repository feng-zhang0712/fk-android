package com.fk.business.cell

/**
 * Selective business list-row kit (item models + Compose rows).
 *
 * Conceptually aligned with iOS `FKCellKit` — **not** a dump of every cell.
 * Prefer CommentKit for threaded comments with actions.
 */
object CellKit {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.2"
}

/** Lightweight tag shown in trailing / inline chrome. */
data class CellTag(
  val title: String,
  val style: CellChromeStyle = CellChromeStyle.Neutral,
)

/** Workflow status pill for order / ticket rows. */
data class CellStatusPill(
  val title: String,
  val style: CellChromeStyle = CellChromeStyle.Neutral,
  val showsDot: Boolean = false,
)

/** Semantic chrome for tags and status pills. */
enum class CellChromeStyle {
  Neutral,
  Success,
  Warning,
  Error,
  Info,
}

/** Optional presence indicator on user avatars. */
enum class CellPresence {
  Online,
  Away,
  Busy,
  Offline,
}

/** Contiguous title segment with optional search-hit emphasis. */
data class SearchHighlightSegment(
  val text: String,
  val isHighlighted: Boolean = false,
)

/** View model for [UserListRow]. */
data class UserListItem(
  val id: String,
  val displayName: String,
  val subtitle: String? = null,
  val avatarUrl: String? = null,
  val presence: CellPresence? = null,
  val unreadCount: Int = 0,
  val roleTag: CellTag? = null,
  val timestampText: String? = null,
  val isVerified: Boolean = false,
) {
  init {
    require(unreadCount >= 0) { "unreadCount must be >= 0" }
  }
}

/** View model for [NotificationListRow]. */
data class NotificationListItem(
  val id: String,
  val title: String,
  val summary: String? = null,
  val timestampText: String? = null,
  val isUnread: Boolean = false,
)

/** View model for [SearchResultRow]. */
data class SearchResultItem(
  val id: String,
  val titleSegments: List<SearchHighlightSegment>,
  val breadcrumbText: String? = null,
  val categoryTagTitle: String? = null,
) {
  companion object {
    /**
     * Builds title segments with the first case-insensitive match of [query] highlighted.
     */
    fun highlight(
      id: String,
      title: String,
      query: String?,
      breadcrumbText: String? = null,
      categoryTagTitle: String? = null,
    ): SearchResultItem {
      val segments = highlightSegments(title, query)
      return SearchResultItem(
        id = id,
        titleSegments = segments,
        breadcrumbText = breadcrumbText,
        categoryTagTitle = categoryTagTitle,
      )
    }

    fun highlightSegments(title: String, query: String?): List<SearchHighlightSegment> {
      if (query.isNullOrEmpty()) return listOf(SearchHighlightSegment(title))
      val index = title.indexOf(query, ignoreCase = true)
      if (index < 0) return listOf(SearchHighlightSegment(title))
      val before = title.substring(0, index)
      val match = title.substring(index, index + query.length)
      val after = title.substring(index + query.length)
      return buildList {
        if (before.isNotEmpty()) add(SearchHighlightSegment(before))
        add(SearchHighlightSegment(match, isHighlighted = true))
        if (after.isNotEmpty()) add(SearchHighlightSegment(after))
      }
    }
  }
}

/** View model for [OrderListRow]. */
data class OrderListItem(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val displayOrderNumber: String,
  val fullOrderNumber: String? = null,
  val statusPill: CellStatusPill,
  val showsCopyChip: Boolean = true,
) {
  /** Value passed to copy callbacks. */
  val copyableOrderNumber: String get() = fullOrderNumber ?: displayOrderNumber
}

/** View model for [InlineToggleRow]. */
data class InlineToggleItem(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val isOn: Boolean,
  val isEnabled: Boolean = true,
)
