package com.fk.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Text styles in the FK type ramp.
 *
 * Conceptually aligned with iOS `FKThemeTextStyle`.
 */
enum class FkTextStyle {
  LargeTitle,
  Title1,
  Title2,
  Title3,
  Headline,
  Body,
  Callout,
  Subheadline,
  Footnote,
  Caption1,
  Caption2,
}

/**
 * Font ramp for a theme snapshot.
 *
 * Base sizes match iOS Large content-size defaults. Compose scales via
 * [androidx.compose.ui.unit.TextUnit] / system font scale automatically.
 *
 * Conceptually aligned with iOS `FKThemeTypography`.
 */
@Immutable
data class FkThemeTypography(
  val styles: Map<FkTextStyle, TextStyle>,
) {
  /** Returns the [TextStyle] for [style], falling back to [Body]. */
  fun textStyle(style: FkTextStyle): TextStyle =
    styles[style] ?: styles.getValue(FkTextStyle.Body)

  companion object {
    /** Built-in ramp matching iOS `FKThemeDefaultFactory` base fonts. */
    fun default(): FkThemeTypography = FkThemeTypography(
      styles = mapOf(
        FkTextStyle.LargeTitle to TextStyle(
          fontSize = 34.sp,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 41.sp,
        ),
        FkTextStyle.Title1 to TextStyle(
          fontSize = 28.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 34.sp,
        ),
        FkTextStyle.Title2 to TextStyle(
          fontSize = 22.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 28.sp,
        ),
        FkTextStyle.Title3 to TextStyle(
          fontSize = 20.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 25.sp,
        ),
        FkTextStyle.Headline to TextStyle(
          fontSize = 17.sp,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 22.sp,
        ),
        FkTextStyle.Body to TextStyle(
          fontSize = 17.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 22.sp,
        ),
        FkTextStyle.Callout to TextStyle(
          fontSize = 16.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 21.sp,
        ),
        FkTextStyle.Subheadline to TextStyle(
          fontSize = 15.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 20.sp,
        ),
        FkTextStyle.Footnote to TextStyle(
          fontSize = 13.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 18.sp,
        ),
        FkTextStyle.Caption1 to TextStyle(
          fontSize = 12.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 16.sp,
        ),
        FkTextStyle.Caption2 to TextStyle(
          fontSize = 11.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 13.sp,
        ),
      ),
    )
  }
}
