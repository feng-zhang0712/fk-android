package com.fk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Immutable design-token snapshot for FK UI surfaces.
 *
 * Conceptually aligned with iOS `FKTheme`. Prefer reading tokens through
 * [LocalFkTheme] / [fkColor] / [fkTextStyle] inside Compose; use [resolveColor]
 * outside composition.
 */
@Immutable
data class FkTheme(
  val id: String,
  val colors: FkThemeColors,
  val typography: FkThemeTypography,
  val metrics: FkThemeMetrics,
  val shadows: FkThemeShadows,
) {
  /** Resolves [role] for the given appearance (non-Compose). */
  fun resolveColor(role: FkColorRole, darkTheme: Boolean): Color =
    colors.color(role).resolve(darkTheme)

  /** Resolves a workflow status color for the given appearance (non-Compose). */
  fun resolveStatusColor(semantic: FkStatusSemantic, darkTheme: Boolean): Color =
    colors.color(semantic).resolve(darkTheme)

  /** Returns a text style from this theme's ramp. */
  fun textStyle(style: FkTextStyle): TextStyle =
    typography.textStyle(style)

  companion object {
    /** Stable identifier for [Default]. */
    const val DefaultId: String = FkThemeDefaults.DefaultId

    /** Stable identifier for [DefaultDark]. */
    const val DefaultDarkId: String = FkThemeDefaults.DefaultDarkId

    /** Package semantic version (keep in sync with library version when publishing). */
    const val VERSION: String = "0.1.2"

    /**
     * Built-in theme using adaptive semantic colors.
     * Same token values as [DefaultDark]; only [id] differs (iOS parity).
     */
    val Default: FkTheme = FkThemeDefaults.makeDefault()

    /**
     * Built-in preset with the same adaptive tokens as [Default] and a distinct [id].
     */
    val DefaultDark: FkTheme = FkThemeDefaults.makeDefaultDark()
  }
}

/** CompositionLocal holding the active [FkTheme] snapshot. */
val LocalFkTheme = staticCompositionLocalOf { FkTheme.Default }

/** CompositionLocal for whether dark appearance is active for token resolution. */
val LocalFkDarkTheme = staticCompositionLocalOf { false }

/** True when the call site is already inside an [FkTheme] provider. */
private val LocalFkThemeActive = staticCompositionLocalOf { false }

/**
 * Root theme for FK UI surfaces.
 *
 * Provides [LocalFkTheme] / [LocalFkDarkTheme] and bridges semantic tokens into
 * [MaterialTheme] so Scaffold / Button / etc. inherit the palette.
 *
 * Nested calls inherit the parent [theme] and appearance unless overridden —
 * matching MaterialTheme's "default to current" behavior.
 *
 * @param theme Token snapshot (defaults to the ambient [LocalFkTheme]).
 * @param darkTheme When true, resolves each [FkThemeColor] to its dark variant.
 *   Defaults to the ambient appearance inside an existing [FkTheme], otherwise
 *   [isSystemInDarkTheme].
 */
@Composable
fun FkTheme(
  theme: FkTheme = LocalFkTheme.current,
  darkTheme: Boolean = if (LocalFkThemeActive.current) {
    LocalFkDarkTheme.current
  } else {
    isSystemInDarkTheme()
  },
  content: @Composable () -> Unit,
) {
  val colorScheme = theme.toColorScheme(darkTheme)
  val typography = theme.toMaterialTypography()
  val shapes = theme.toMaterialShapes()

  CompositionLocalProvider(
    LocalFkThemeActive provides true,
    LocalFkTheme provides theme,
    LocalFkDarkTheme provides darkTheme,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = shapes,
      content = content,
    )
  }
}

/** Resolves [role] from the active theme for the active appearance. */
@Composable
@ReadOnlyComposable
fun fkColor(role: FkColorRole): Color =
  LocalFkTheme.current.resolveColor(role, LocalFkDarkTheme.current)

/** Resolves a workflow status color from the active theme. */
@Composable
@ReadOnlyComposable
fun fkStatusColor(semantic: FkStatusSemantic): Color =
  LocalFkTheme.current.resolveStatusColor(semantic, LocalFkDarkTheme.current)

/** Returns a text style from the active theme ramp. */
@Composable
@ReadOnlyComposable
fun fkTextStyle(style: FkTextStyle): TextStyle =
  LocalFkTheme.current.textStyle(style)

/** Active theme metrics (spacing / radii). */
@Composable
@ReadOnlyComposable
fun fkMetrics(): FkThemeMetrics = LocalFkTheme.current.metrics

/** Active theme shadow tokens. */
@Composable
@ReadOnlyComposable
fun fkShadows(): FkThemeShadows = LocalFkTheme.current.shadows

internal fun FkTheme.toColorScheme(darkTheme: Boolean): ColorScheme {
  val c = colors
  fun role(role: FkColorRole): Color = resolveColor(role, darkTheme)
  val primary = role(FkColorRole.Primary)
  val onPrimary = role(FkColorRole.OnPrimary)
  val secondary = role(FkColorRole.Secondary)
  val onSecondary = role(FkColorRole.OnSecondary)
  val error = role(FkColorRole.Destructive)
  val onError = role(FkColorRole.OnDestructive)
  val background = role(FkColorRole.Background)
  val onBackground = role(FkColorRole.OnSurface)
  val surface = role(FkColorRole.Surface)
  val onSurface = role(FkColorRole.OnSurface)
  val onSurfaceVariant = role(FkColorRole.OnSurfaceSecondary)
  val surfaceElevated = role(FkColorRole.SurfaceElevated)
  val outline = role(FkColorRole.Outline)
  val scrim = role(FkColorRole.Scrim)
  return if (darkTheme) {
    darkColorScheme(
      primary = primary,
      onPrimary = onPrimary,
      secondary = secondary,
      onSecondary = onSecondary,
      error = error,
      onError = onError,
      background = background,
      onBackground = onBackground,
      surface = surface,
      onSurface = onSurface,
      onSurfaceVariant = onSurfaceVariant,
      surfaceContainerHigh = surfaceElevated,
      outline = outline,
      scrim = scrim,
    )
  } else {
    lightColorScheme(
      primary = primary,
      onPrimary = onPrimary,
      secondary = secondary,
      onSecondary = onSecondary,
      error = error,
      onError = onError,
      background = background,
      onBackground = onBackground,
      surface = surface,
      onSurface = onSurface,
      onSurfaceVariant = onSurfaceVariant,
      surfaceContainerHigh = surfaceElevated,
      outline = outline,
      scrim = scrim,
    )
  }
}

internal fun FkTheme.toMaterialTypography(): Typography {
  val t = typography
  return Typography(
    displayLarge = t.textStyle(FkTextStyle.LargeTitle),
    headlineLarge = t.textStyle(FkTextStyle.Title1),
    headlineMedium = t.textStyle(FkTextStyle.Title2),
    headlineSmall = t.textStyle(FkTextStyle.Title3),
    titleMedium = t.textStyle(FkTextStyle.Headline),
    bodyLarge = t.textStyle(FkTextStyle.Body),
    bodyMedium = t.textStyle(FkTextStyle.Callout),
    bodySmall = t.textStyle(FkTextStyle.Subheadline),
    labelLarge = t.textStyle(FkTextStyle.Footnote),
    labelMedium = t.textStyle(FkTextStyle.Caption1),
    labelSmall = t.textStyle(FkTextStyle.Caption2),
  )
}

internal fun FkTheme.toMaterialShapes(): Shapes {
  val m = metrics
  return Shapes(
    small = RoundedCornerShape(m.radiusSmall),
    medium = RoundedCornerShape(m.radiusMedium),
    large = RoundedCornerShape(m.radiusLarge),
  )
}
