package com.fk.ui.empty

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout density for empty-state chrome.
 *
 * Scales fallback spacing and type; explicit [EmptyLayout.verticalSpacing] is not scaled.
 * Conceptually aligned with iOS `FKEmptyStateDensity`.
 */
enum class EmptyDensity {
  Compact,
  Regular,
  Comfortable,
}

/**
 * Vertical alignment of empty-state content within its host.
 */
enum class EmptyContentAlignment {
  Center,
  Top,
}

/**
 * Layout hints for empty-state surfaces.
 *
 * Conceptually aligned with a simplified iOS `FKEmptyStateLayoutConfiguration`.
 */
data class EmptyLayout(
  val density: EmptyDensity = EmptyDensity.Regular,
  val alignment: EmptyContentAlignment = EmptyContentAlignment.Center,
  val verticalSpacing: Dp? = null,
  val contentPadding: Dp = 24.dp,
  val maxContentWidth: Dp = 320.dp,
  val verticalOffset: Dp = 0.dp,
)

/**
 * Overlay / loading presentation options.
 *
 * Conceptually aligned with a simplified iOS `FKEmptyStatePresentationConfiguration`.
 */
data class EmptyPresentation(
  /** When true, suppress loading chrome while a host refresh indicator is active. */
  val skipsWhileRefreshing: Boolean = true,
  /** Dim alpha applied behind overlay content (0 = none). */
  val blockingOverlayAlpha: Float = 0f,
  /** Max lines for the title. */
  val maxTitleLines: Int = 2,
  /** Max lines for the description. */
  val maxDescriptionLines: Int = 4,
  /**
   * Soft character cap for description before ellipsis (0 = unlimited).
   * Full text remains available to accessibility when truncated.
   */
  val maxDescriptionChars: Int = 200,
  /** Suffix appended when [maxDescriptionChars] truncates the description. */
  val truncationSuffix: String = "…",
)

/**
 * Copy and loading subtitle for an empty-state surface.
 */
data class EmptyContent(
  val title: String? = null,
  val description: String? = null,
  val loadingMessage: String? = null,
)
