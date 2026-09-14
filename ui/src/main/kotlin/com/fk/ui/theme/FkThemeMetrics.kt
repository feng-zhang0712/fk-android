package com.fk.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Named spacing tokens in [FkThemeMetrics].
 *
 * Conceptually aligned with iOS `FKThemeSpacingToken`.
 */
enum class FkSpacingToken {
  Xxs,
  Xs,
  S,
  M,
  L,
  Xl,
}

/**
 * Spacing, corner radii, and layout constants for a theme.
 *
 * Defaults match iOS `FKThemeMetrics`. [minimumHitTarget] is 44.dp for
 * cross-platform parity; Material touch targets often prefer 48.dp.
 *
 * Conceptually aligned with iOS `FKThemeMetrics`.
 */
@Immutable
data class FkThemeMetrics(
  val spacingXxs: Dp = 4.dp,
  val spacingXs: Dp = 8.dp,
  val spacingS: Dp = 12.dp,
  val spacingM: Dp = 16.dp,
  val spacingL: Dp = 24.dp,
  val spacingXl: Dp = 32.dp,
  val radiusSmall: Dp = 8.dp,
  val radiusMedium: Dp = 12.dp,
  val radiusLarge: Dp = 16.dp,
  val radiusFull: Dp = 10_000.dp,
  val minimumHitTarget: Dp = 44.dp,
  val hairline: Dp = 1.dp,
) {
  /** Returns spacing for [token]. */
  fun spacing(token: FkSpacingToken): Dp = when (token) {
    FkSpacingToken.Xxs -> spacingXxs
    FkSpacingToken.Xs -> spacingXs
    FkSpacingToken.S -> spacingS
    FkSpacingToken.M -> spacingM
    FkSpacingToken.L -> spacingL
    FkSpacingToken.Xl -> spacingXl
  }

  /** Corner shape for the small radius token. */
  val shapeSmall: Shape get() = RoundedCornerShape(radiusSmall)

  /** Corner shape for the medium radius token. */
  val shapeMedium: Shape get() = RoundedCornerShape(radiusMedium)

  /** Corner shape for the large radius token. */
  val shapeLarge: Shape get() = RoundedCornerShape(radiusLarge)

  /** Pill / circle shape (maps [radiusFull]). */
  val shapeFull: Shape get() = CircleShape
}
