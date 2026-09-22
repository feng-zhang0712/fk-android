package com.fk.ui.callout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Interaction category stored on the configuration. */
enum class CalloutKind {
  /** Short, transient hint with conservative width and auto-dismiss defaults. */
  Tooltip,

  /** Richer panel that stays until the user dismisses it or replaces it. */
  Popover,
}

/** Entrance and exit motion styles. */
enum class CalloutAnimationStyle {
  /** Opacity only. */
  Fade,

  /** Opacity with a subtle scale. */
  FadeScale,
}

/**
 * Edge-relative placement of a callout bubble next to its anchor.
 *
 * The case name describes where the **bubble** sits relative to the anchor.
 * The beak points toward the anchor on the opposite edge.
 */
enum class CalloutPlacement {
  /** Chooses among top, bottom, start, and end based on available space. */
  Automatic,

  /** Bubble above the anchor; beak centered on the bottom edge. */
  Top,

  /** Bubble above the anchor; beak near the start of the bottom edge. */
  TopStart,

  /** Bubble above the anchor; beak near the end of the bottom edge. */
  TopEnd,

  /** Bubble below the anchor; beak centered on the top edge. */
  Bottom,

  /** Bubble below the anchor; beak toward the start of the top edge. */
  BottomStart,

  /** Bubble below the anchor; beak toward the end of the top edge. */
  BottomEnd,

  /** Bubble before the anchor; beak centered on the end edge. */
  Start,

  /** Bubble before the anchor; beak toward the top of the end edge. */
  StartTop,

  /** Bubble before the anchor; beak toward the bottom of the end edge. */
  StartBottom,

  /** Bubble after the anchor; beak centered on the start edge. */
  End,

  /** Bubble after the anchor; beak toward the top of the start edge. */
  EndTop,

  /** Bubble after the anchor; beak toward the bottom of the start edge. */
  EndBottom,
}

/** Aligns the bubble relative to the anchor along the axis perpendicular to the beak. */
enum class CalloutAnchorAlignment {
  Center,
  Start,
  End,
}

/** Coordinate system used by [CalloutBeakOffset]. */
enum class CalloutBeakOffsetReference {
  /** Positions along the bubble beak edge from its start. */
  BubbleEdge,

  /** Positions relative to the anchor point projected onto the beak edge. */
  Anchor,
}

/** Controls where the beak sits along its edge. */
sealed class CalloutBeakOffset {
  /** Placement-driven defaults (center vs corner). */
  data object Automatic : CalloutBeakOffset()

  /** Fraction `0…1` along the usable beak edge. */
  data class Fraction(
    val value: Float,
    val reference: CalloutBeakOffsetReference = CalloutBeakOffsetReference.BubbleEdge,
  ) : CalloutBeakOffset()

  /** Fixed distance from the reference origin on the usable beak edge. */
  data class Fixed(
    val value: Dp,
    val reference: CalloutBeakOffsetReference = CalloutBeakOffsetReference.BubbleEdge,
  ) : CalloutBeakOffset()
}

/** Triangular (or polygon) pointer rendered on a callout bubble edge. */
sealed class CalloutBeakStyle {
  /** Symmetric triangle using [CalloutAppearance.beakWidth] / [CalloutAppearance.beakHeight]. */
  data object Isosceles : CalloutBeakStyle()

  /** Equilateral triangle; height is derived from beak width when height is not overridden. */
  data object Equilateral : CalloutBeakStyle()

  /**
   * Right triangle with the right angle on the bubble edge.
   *
   * @param corner Which end of the beak base hosts the right angle.
   * @param apexAlongBase Tip position along the base (`0` = start, `1` = end).
   */
  data class RightAngle(
    val corner: CalloutBeakRightAngleCorner,
    val apexAlongBase: Float = 1f,
  ) : CalloutBeakStyle()

  /**
   * Custom polygon in normalized beak space.
   *
   * `x` is `0…1` along the bubble edge; `y` is `0` on the edge and `1` at the outward tip.
   * Use [androidx.compose.ui.geometry.Offset] values in that unit range.
   */
  data class Polygon(
    val vertices: List<androidx.compose.ui.geometry.Offset>,
  ) : CalloutBeakStyle()
}

enum class CalloutBeakRightAngleCorner {
  Start,
  End,
}

/** How a visible callout reacts when the software keyboard changes layout. */
enum class CalloutKeyboardAvoidance {
  None,
  Relayout,
  Dismiss,
}

/** Whether a new callout replaces existing ones or can be shown concurrently. */
enum class CalloutPresentationPolicy {
  ReplaceActive,
  AllowConcurrent,
}

/** Optional dimmed backdrop and anchor spotlight. */
@Immutable
data class CalloutBackdropStyle(
  val showsDimmedBackdrop: Boolean = false,
  val spotlightsAnchor: Boolean = true,
  val dimColor: Color? = null,
  val spotlightCornerRadius: Dp = 8.dp,
)

/** Visual chrome for callout bubbles. */
@Immutable
data class CalloutAppearance(
  val style: Style = Style.Light,
  val backgroundColor: Color? = null,
  val textColor: Color? = null,
  val secondaryTextColor: Color? = null,
  val cornerRadius: Dp = 8.dp,
  val beakWidth: Dp = 14.dp,
  val beakHeight: Dp = 7.dp,
  val beakStyle: CalloutBeakStyle = CalloutBeakStyle.Isosceles,
  val beakCornerInset: Dp = 12.dp,
  val showsShadow: Boolean = true,
  val shadowOpacity: Float = 0.12f,
  val shadowRadius: Dp = 10.dp,
  val shadowOffset: DpOffset = DpOffset(0.dp, 4.dp),
  val borderColor: Color? = null,
  val borderWidth: Dp = 0.dp,
  val usesFrostedGlassBackground: Boolean = false,
) {
  enum class Style {
    /** Elevated light surface, dark text. */
    Light,

    /** Near-black surface, light text. */
    Dark,
  }
}

/**
 * Per-request configuration for anchored callouts.
 *
 * Prefer [tooltipDefault], [popoverDefault], or [menuDefault] over the bare constructor
 * unless you need fully custom defaults.
 */
@Immutable
data class CalloutConfiguration(
  val kind: CalloutKind = CalloutKind.Tooltip,
  val placement: CalloutPlacement = CalloutPlacement.Automatic,
  val appearance: CalloutAppearance = CalloutAppearance(),
  val anchorSpacing: Dp = 8.dp,
  val maxWidth: Dp = 280.dp,
  val contentPadding: PaddingValuesDp = PaddingValuesDp(10.dp, 12.dp),
  val bodyTextSize: TextUnit = 14.sp,
  val titleTextSize: TextUnit = 16.sp,
  val animationDurationMs: Int = 220,
  val animationStyle: CalloutAnimationStyle = CalloutAnimationStyle.FadeScale,
  /** Auto-dismiss delay; `null` keeps the callout until manual dismissal. */
  val autoDismissDurationMs: Long? = null,
  val tapOutsideToDismiss: Boolean = true,
  /** When true, touches outside the bubble reach views behind the overlay. */
  val passesThroughOutsideTouches: Boolean = true,
  val accessibilityAnnouncementEnabled: Boolean = true,
  val accessibilityAnnouncementOverride: String? = null,
  val flipsPlacementWhenNeeded: Boolean = true,
  val anchorAlignment: CalloutAnchorAlignment = CalloutAnchorAlignment.Center,
  val beakOffset: CalloutBeakOffset = CalloutBeakOffset.Automatic,
  val matchesAnchorWidth: Boolean = false,
  val minWidth: Dp? = null,
  val screenEdgeMargin: Dp = 12.dp,
  val maxContentHeight: Dp? = null,
  val keyboardAvoidance: CalloutKeyboardAvoidance = CalloutKeyboardAvoidance.Relayout,
  val presentationPolicy: CalloutPresentationPolicy = CalloutPresentationPolicy.ReplaceActive,
  val backdrop: CalloutBackdropStyle = CalloutBackdropStyle(),
) {
  companion object {
    /** Narrower width, short auto-dismiss, dark compact chrome. */
    fun tooltipDefault(
      placement: CalloutPlacement = CalloutPlacement.Automatic,
    ): CalloutConfiguration =
      CalloutConfiguration(
        kind = CalloutKind.Tooltip,
        placement = placement,
        appearance = CalloutAppearance(style = CalloutAppearance.Style.Dark, showsShadow = false),
        anchorSpacing = 6.dp,
        maxWidth = 240.dp,
        contentPadding = PaddingValuesDp(vertical = 8.dp, horizontal = 10.dp),
        autoDismissDurationMs = 3_000L,
        tapOutsideToDismiss = false,
        passesThroughOutsideTouches = true,
        keyboardAvoidance = CalloutKeyboardAvoidance.None,
      )

    /** Wider surface, manual dismiss, light card style. */
    fun popoverDefault(
      placement: CalloutPlacement = CalloutPlacement.Automatic,
    ): CalloutConfiguration =
      CalloutConfiguration(
        kind = CalloutKind.Popover,
        placement = placement,
        appearance = CalloutAppearance(
          style = CalloutAppearance.Style.Light,
          cornerRadius = 12.dp,
          beakWidth = 16.dp,
          beakHeight = 8.dp,
          showsShadow = true,
          shadowOpacity = 0.16f,
          shadowRadius = 16.dp,
          shadowOffset = DpOffset(0.dp, 8.dp),
          borderWidth = 0.5.dp,
        ),
        anchorSpacing = 10.dp,
        maxWidth = 320.dp,
        contentPadding = PaddingValuesDp(vertical = 14.dp, horizontal = 16.dp),
        autoDismissDurationMs = null,
        tapOutsideToDismiss = true,
        passesThroughOutsideTouches = true,
      )

    /** Aligns to anchor start and matches anchor width when possible. */
    fun menuDefault(
      placement: CalloutPlacement = CalloutPlacement.BottomStart,
    ): CalloutConfiguration =
      popoverDefault(placement).copy(
        anchorAlignment = CalloutAnchorAlignment.Start,
        matchesAnchorWidth = true,
        contentPadding = PaddingValuesDp(vertical = 8.dp, horizontal = 8.dp),
      )

    internal fun resolvingPreset(
      configuration: CalloutConfiguration?,
      default: CalloutConfiguration,
      placement: CalloutPlacement,
      kind: CalloutKind,
    ): CalloutConfiguration {
      var resolved = configuration ?: default
      if (placement != CalloutPlacement.Automatic) {
        resolved = resolved.copy(placement = placement)
      }
      return resolved.copy(kind = kind)
    }
  }
}

/** Compact padding holder so configuration stays free of Compose [androidx.compose.foundation.layout.PaddingValues]. */
@Immutable
data class PaddingValuesDp(
  val top: Dp,
  val start: Dp,
  val bottom: Dp,
  val end: Dp,
) {
  constructor(vertical: Dp, horizontal: Dp) : this(vertical, horizontal, vertical, horizontal)

  constructor(all: Dp) : this(all, all, all, all)
}
