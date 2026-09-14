package com.fk.ui.sheet

/**
 * Product-level sheet package hub — multi-detent bottom sheet + center card.
 *
 * Conceptually aligned with a narrow subset of iOS `FKSheetPresentationController`
 * (Compose façade over Material3 — not a UIKit presentation engine port).
 */
object FkSheet {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.0"
}

/**
 * Height strategy for bottom sheets.
 *
 * Conceptually aligned with iOS `FKSheetPresentationDetent` (narrow subset).
 *
 * Material3 [androidx.compose.material3.ModalBottomSheet] supports at most two
 * visible anchors (partially expanded + expanded). Product detents map onto that:
 * - two or more snap-friendly detents → partial + expanded
 * - a single tall detent → expanded only (`skipPartiallyExpanded`)
 */
sealed class SheetDetent {
  /** About half of the available height (Material partial expand). */
  data object Medium : SheetDetent()

  /** Near-full sheet (Material expanded). */
  data object Large : SheetDetent()

  /** Maximum height allowed by the sheet host. */
  data object Full : SheetDetent()

  /** Cap content height to [fraction] of the screen (0f..1f). */
  data class Fraction(val fraction: Float) : SheetDetent() {
    init {
      require(fraction in 0.05f..1f) { "fraction must be in 0.05..1" }
    }
  }

  /** Prefer intrinsic content height (still swipe-dismissible). */
  data object FitContent : SheetDetent()
}

/**
 * Backdrop / scrim style.
 *
 * Conceptually aligned with iOS `FKBackdropStyle` (dim / none only).
 */
sealed class SheetBackdrop {
  data object None : SheetBackdrop()
  data class Dim(val alpha: Float = 0.32f) : SheetBackdrop() {
    init {
      require(alpha in 0f..1f) { "alpha must be in 0..1" }
    }
  }
}

/**
 * Which presentation surface to use.
 */
enum class SheetPresentation {
  /** Bottom-attached modal sheet. */
  BottomSheet,

  /** Centered floating card (dialog-style). */
  Center,
}

/**
 * Aggregate configuration for [FkBottomSheet] / [FkCenterSheet].
 *
 * Conceptually aligned with a narrow subset of iOS `FKSheetPresentationConfiguration`.
 */
data class SheetConfiguration(
  val presentation: SheetPresentation = SheetPresentation.BottomSheet,
  /**
   * Ordered detents for bottom sheets. First entry is the preferred initial height
   * when the sheet opens (mapped to Material partial/expanded).
   */
  val detents: List<SheetDetent> = listOf(SheetDetent.Medium, SheetDetent.Large),
  val backdrop: SheetBackdrop = SheetBackdrop.Dim(),
  val dismissOnBack: Boolean = true,
  /**
   * When `false`, outside taps do not dismiss.
   *
   * Honored by [FkCenterSheet]. For [FkBottomSheet], Material3 routes both scrim taps and
   * swipe-to-dismiss through `onDismissRequest` — the host decides whether to close.
   */
  val dismissOnClickOutside: Boolean = true,
  val showDragHandle: Boolean = true,
) {
  init {
    require(detents.isNotEmpty()) { "detents must not be empty" }
  }

  companion object {
    /** Common feed / action sheet: medium ↔ large. */
    val BottomSheetDefault: SheetConfiguration = SheetConfiguration()

    /** Tall sheet that opens fully (skips partial). */
    val BottomSheetLarge: SheetConfiguration = SheetConfiguration(
      detents = listOf(SheetDetent.Large),
      showDragHandle = true,
    )

    /** Content-sized sheet capped at 90% height. */
    val BottomSheetFit: SheetConfiguration = SheetConfiguration(
      detents = listOf(SheetDetent.FitContent),
      showDragHandle = true,
    )

    /** Centered card with dim backdrop. */
    val CenterCard: SheetConfiguration = SheetConfiguration(
      presentation = SheetPresentation.Center,
      detents = listOf(SheetDetent.FitContent),
      showDragHandle = false,
      backdrop = SheetBackdrop.Dim(alpha = 0.4f),
    )
  }
}
