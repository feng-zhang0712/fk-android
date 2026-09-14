package com.fk.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Platform-agnostic shadow / elevation style.
 *
 * Compose has no CALayer shadow API; consumers may map these tokens to
 * [androidx.compose.ui.draw.shadow] elevation or a custom draw.
 *
 * Conceptually aligned with iOS `FKLayerShadowStyle.custom`.
 */
@Immutable
data class FkShadowStyle(
  val color: Color = Color.Black,
  val alpha: Float = 0.12f,
  val radius: Dp = 4.dp,
  val offsetX: Dp = 0.dp,
  val offsetY: Dp = 2.dp,
) {
  companion object {
    /** No shadow. */
    val None: FkShadowStyle = FkShadowStyle(alpha = 0f, radius = 0.dp, offsetY = 0.dp)
  }
}

/**
 * Elevation presets for a theme snapshot.
 *
 * Conceptually aligned with iOS `FKThemeShadowTokens`.
 */
@Immutable
data class FkThemeShadows(
  val elevationLow: FkShadowStyle,
  val elevationMedium: FkShadowStyle,
  val elevationHigh: FkShadowStyle,
) {
  companion object {
    /** Defaults matching iOS `FKThemeDefaultFactory` shadow presets. */
    fun default(): FkThemeShadows = FkThemeShadows(
      elevationLow = FkShadowStyle(
        color = Color.Black,
        alpha = 0.12f,
        radius = 4.dp,
        offsetY = 2.dp,
      ),
      elevationMedium = FkShadowStyle(
        color = Color.Black,
        alpha = 0.16f,
        radius = 8.dp,
        offsetY = 4.dp,
      ),
      elevationHigh = FkShadowStyle(
        color = Color.Black,
        alpha = 0.22f,
        radius = 12.dp,
        offsetY = 6.dp,
      ),
    )
  }
}
