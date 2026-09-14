package com.fk.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Light / dark color pair, resolved against the active appearance.
 *
 * Conceptually aligned with iOS `FKThemeColor`.
 */
@Immutable
data class FkThemeColor(
  val light: Color,
  val dark: Color,
) {
  /** Same color in light and dark. */
  constructor(fixed: Color) : this(light = fixed, dark = fixed)

  /** Picks [dark] when [darkTheme] is true, otherwise [light]. */
  fun resolve(darkTheme: Boolean): Color = if (darkTheme) dark else light
}

/**
 * Semantic color roles in [FkThemeColors].
 *
 * Conceptually aligned with iOS `FKThemeColorRole`.
 */
enum class FkColorRole {
  Primary,
  OnPrimary,
  Secondary,
  OnSecondary,
  Destructive,
  OnDestructive,
  Background,
  Surface,
  SurfaceElevated,
  OnSurface,
  OnSurfaceSecondary,
  Outline,
  Scrim,
  StatusSuccess,
  StatusWarning,
  StatusError,
  StatusInfo,
  StatusNeutral,
}

/**
 * Workflow status semantics (shared with future status widgets).
 *
 * Conceptually aligned with iOS `FKWidgetStatusSemantic`.
 */
enum class FkStatusSemantic {
  Success,
  Warning,
  Error,
  Info,
  Neutral,
}
