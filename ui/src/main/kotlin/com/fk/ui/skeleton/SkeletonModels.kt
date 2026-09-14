package com.fk.ui.skeleton

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Skeleton package hub — shimmer / placeholder patterns.
 *
 * Conceptually aligned with iOS `FKUIKit` Skeleton (composable + presets).
 * Auto UIView tree-scanning is intentionally not ported.
 */
object Skeleton {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.0"

  /** Default configuration (theme-derived colors applied at composition time when null). */
  val DefaultConfiguration: SkeletonConfiguration = SkeletonConfiguration()
}

/** Skeleton animation style. */
enum class SkeletonAnimationMode {
  /** Moving highlight band (default). */
  Shimmer,

  /** Opacity pulse. */
  Pulse,

  /** Static placeholder fill. */
  None,
}

/** Direction of the shimmer highlight. */
enum class SkeletonShimmerDirection {
  LeftToRight,
  RightToLeft,
  TopToBottom,
  BottomToTop,
}

/** Avatar shape used by [SkeletonPresets.ListRow]. */
enum class SkeletonAvatarStyle {
  Circle,
  Rounded,
}

/**
 * Visual configuration for skeleton blocks.
 *
 * Conceptually aligned with iOS `FKSkeletonConfiguration`.
 *
 * When [baseColor] / [highlightColor] are null, Compose surfaces derive them from
 * [com.fk.ui.theme.FkTheme] at draw time.
 */
@Immutable
data class SkeletonConfiguration(
  val baseColor: Color? = null,
  val highlightColor: Color? = null,
  val cornerRadius: Dp = 6.dp,
  val animationMode: SkeletonAnimationMode = SkeletonAnimationMode.Shimmer,
  val shimmerDirection: SkeletonShimmerDirection = SkeletonShimmerDirection.LeftToRight,
  val animationDurationMs: Int = 1_400,
  val pulseMinAlpha: Float = 0.45f,
  val lineSpacing: Dp = 8.dp,
  val lineHeight: Dp = 12.dp,
  val itemSpacing: Dp = 12.dp,
)
