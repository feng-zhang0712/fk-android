package com.fk.ui.theme

import androidx.compose.runtime.Immutable

/**
 * Semantic color palette for a theme snapshot.
 *
 * Conceptually aligned with iOS `FKThemeColorPalette` (18 roles).
 */
@Immutable
data class FkThemeColors(
  val primary: FkThemeColor,
  val onPrimary: FkThemeColor,
  val secondary: FkThemeColor,
  val onSecondary: FkThemeColor,
  val destructive: FkThemeColor,
  val onDestructive: FkThemeColor,
  val background: FkThemeColor,
  val surface: FkThemeColor,
  val surfaceElevated: FkThemeColor,
  val onSurface: FkThemeColor,
  val onSurfaceSecondary: FkThemeColor,
  val outline: FkThemeColor,
  val scrim: FkThemeColor,
  val statusSuccess: FkThemeColor,
  val statusWarning: FkThemeColor,
  val statusError: FkThemeColor,
  val statusInfo: FkThemeColor,
  val statusNeutral: FkThemeColor,
) {
  /** Returns the token for [role]. */
  fun color(role: FkColorRole): FkThemeColor = when (role) {
    FkColorRole.Primary -> primary
    FkColorRole.OnPrimary -> onPrimary
    FkColorRole.Secondary -> secondary
    FkColorRole.OnSecondary -> onSecondary
    FkColorRole.Destructive -> destructive
    FkColorRole.OnDestructive -> onDestructive
    FkColorRole.Background -> background
    FkColorRole.Surface -> surface
    FkColorRole.SurfaceElevated -> surfaceElevated
    FkColorRole.OnSurface -> onSurface
    FkColorRole.OnSurfaceSecondary -> onSurfaceSecondary
    FkColorRole.Outline -> outline
    FkColorRole.Scrim -> scrim
    FkColorRole.StatusSuccess -> statusSuccess
    FkColorRole.StatusWarning -> statusWarning
    FkColorRole.StatusError -> statusError
    FkColorRole.StatusInfo -> statusInfo
    FkColorRole.StatusNeutral -> statusNeutral
  }

  /** Returns the status foreground token for [semantic]. */
  fun color(semantic: FkStatusSemantic): FkThemeColor = when (semantic) {
    FkStatusSemantic.Success -> statusSuccess
    FkStatusSemantic.Warning -> statusWarning
    FkStatusSemantic.Error -> statusError
    FkStatusSemantic.Info -> statusInfo
    FkStatusSemantic.Neutral -> statusNeutral
  }
}
