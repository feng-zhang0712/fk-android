package com.fk.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Built-in default palette / snapshot factory.
 *
 * Color values approximate iOS system semantics used by
 * `FKThemeDefaultFactory` (explicit ARGB — not dynamic UIColor refs).
 */
internal object FkThemeDefaults {
  const val DefaultId: String = "fk.theme.default"
  const val DefaultDarkId: String = "fk.theme.default-dark"

  fun makeDefault(): FkTheme = makeTheme(DefaultId)

  fun makeDefaultDark(): FkTheme = makeTheme(DefaultDarkId)

  private fun makeTheme(id: String): FkTheme = FkTheme(
    id = id,
    colors = makeDefaultPalette(),
    typography = FkThemeTypography.default(),
    metrics = FkThemeMetrics(),
    shadows = FkThemeShadows.default(),
  )

  private fun makeDefaultPalette(): FkThemeColors = FkThemeColors(
    primary = FkThemeColor(light = Color(0xFF007AFF), dark = Color(0xFF0A84FF)),
    onPrimary = FkThemeColor(fixed = Color.White),
    secondary = FkThemeColor(light = Color(0x28787880), dark = Color(0x52787880)),
    onSecondary = FkThemeColor(light = Color(0xFF000000), dark = Color(0xFFFFFFFF)),
    destructive = FkThemeColor(light = Color(0xFFFF3B30), dark = Color(0xFFFF453A)),
    onDestructive = FkThemeColor(fixed = Color.White),
    background = FkThemeColor(light = Color(0xFFFFFFFF), dark = Color(0xFF000000)),
    surface = FkThemeColor(light = Color(0xFFF2F2F7), dark = Color(0xFF1C1C1E)),
    surfaceElevated = FkThemeColor(light = Color(0xFFFFFFFF), dark = Color(0xFF2C2C2E)),
    onSurface = FkThemeColor(light = Color(0xFF000000), dark = Color(0xFFFFFFFF)),
    onSurfaceSecondary = FkThemeColor(light = Color(0x993C3C43), dark = Color(0x99EBEBF5)),
    outline = FkThemeColor(light = Color(0x4A3C3C43), dark = Color(0x99545458)),
    scrim = FkThemeColor(
      light = Color.Black.copy(alpha = 0.45f),
      dark = Color.Black.copy(alpha = 0.55f),
    ),
    statusSuccess = FkThemeColor(light = Color(0xFF34C759), dark = Color(0xFF30D158)),
    statusWarning = FkThemeColor(light = Color(0xFFFF9500), dark = Color(0xFFFF9F0A)),
    statusError = FkThemeColor(light = Color(0xFFFF3B30), dark = Color(0xFFFF453A)),
    statusInfo = FkThemeColor(light = Color(0xFF007AFF), dark = Color(0xFF0A84FF)),
    statusNeutral = FkThemeColor(light = Color(0x993C3C43), dark = Color(0x99EBEBF5)),
  )
}
