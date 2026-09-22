package com.fk.ui.rating

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Rating package hub — read-only and interactive star scoring with fractional steps.
 */
object RatingKit {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"
}

/** Whether the user can change the score. */
enum class RatingInteractionMode {
  /** Displays the current value; ignores touch and drag. */
  ReadOnly,
  /** Allows tap and drag to update the value. */
  Interactive,
}

/** Discrete increments applied after user input. */
sealed class RatingStep {
  /** Whole-number steps (for example `1.0` on a five-item control). */
  data object Whole : RatingStep()

  /** Half-step increments (for example `0.5`). */
  data object Half : RatingStep()

  /** Custom positive increment. */
  data class Custom(val value: Double) : RatingStep()

  /** Resolved step size used for snapping. */
  val increment: Double
    get() = when (this) {
      Whole -> 1.0
      Half -> 0.5
      is Custom -> maxOf(0.01, value)
    }
}

/** Placement of an optional value caption. */
enum class RatingLabelPlacement {
  None,
  Trailing,
  Bottom,
}

/**
 * Icon source for empty and filled states.
 *
 * Partial fills use a horizontal clip over the filled glyph (works for any painter).
 */
@Immutable
sealed class RatingIconStyle {
  /** Bundled star outline / fill drawables. */
  data object Star : RatingIconStyle()

  /** Host-provided painters (same shape recommended for clean half fills). */
  data class Painters(
    val empty: Painter,
    val filled: Painter,
  ) : RatingIconStyle()
}

/** Geometry for rating items and optional caption placement. */
@Immutable
data class RatingLayoutConfiguration(
  val itemCount: Int = 5,
  val itemSize: Dp = 28.dp,
  val itemSpacing: Dp = 6.dp,
  val contentPadding: Dp = 0.dp,
  val labelPlacement: RatingLabelPlacement = RatingLabelPlacement.None,
  val labelSpacing: Dp = 6.dp,
) {
  init {
    require(itemCount >= 1) { "itemCount must be >= 1" }
  }
}

/** Colors and caption formatting for [FkRating]. */
@Immutable
data class RatingAppearanceConfiguration(
  val iconStyle: RatingIconStyle = RatingIconStyle.Star,
  val emptyColor: Color = Color(0xFFC7C7CC),
  val filledColor: Color = Color(0xFFFFCC00),
  val labelColor: Color = Color(0xFF8E8E93),
  /** Optional formatter for the caption; when null a one-decimal default is used. */
  val valueFormatter: ((Double) -> String)? = null,
)

/** Touch behavior and snapping for [FkRating]. */
@Immutable
data class RatingInteractionConfiguration(
  val mode: RatingInteractionMode = RatingInteractionMode.Interactive,
  val step: RatingStep = RatingStep.Whole,
  val allowsDragSelection: Boolean = true,
  /** Opacity multiplier while the control is disabled. */
  val disabledAlpha: Float = 0.45f,
  val enableHaptics: Boolean = false,
)

/** Optional caption formatting shown beside or below the icon row. */
@Immutable
data class RatingLabelConfiguration(
  /** When non-null and non-blank, used instead of the numeric value text. */
  val customText: String? = null,
  val valuePrefix: String = "",
  val valueSuffix: String = "",
)

/** Grouped style and behavior settings for [FkRating]. */
@Immutable
data class RatingConfiguration(
  val layout: RatingLayoutConfiguration = RatingLayoutConfiguration(),
  val appearance: RatingAppearanceConfiguration = RatingAppearanceConfiguration(),
  val interaction: RatingInteractionConfiguration = RatingInteractionConfiguration(),
  val label: RatingLabelConfiguration = RatingLabelConfiguration(),
  /** Accessibility label describing the control (for example "Rating"). */
  val contentDescription: String? = "Rating",
  /** Format for TalkBack value text; `%1$s` = value, `%2$s` = maximum. */
  val accessibilityValueFormat: String = "%1\$s of %2\$s",
)

internal object RatingMath {
  fun snap(
    raw: Double,
    minimumValue: Double,
    maximumValue: Double,
    step: RatingStep,
  ): Double {
    val clamped = raw.coerceIn(minimumValue, maximumValue)
    val increment = step.increment
    if (increment <= 0.0) return clamped
    val offset = clamped - minimumValue
    val steps = kotlin.math.round(offset / increment)
    val snapped = minimumValue + steps * increment
    return snapped.coerceIn(minimumValue, maximumValue)
  }

  fun fillFraction(
    index: Int,
    value: Double,
    minimumValue: Double,
    maximumValue: Double,
    itemCount: Int,
  ): Float {
    if (itemCount <= 0 || maximumValue <= minimumValue) return 0f
    val span = (maximumValue - minimumValue) / itemCount.toDouble()
    val lowerBound = minimumValue + index.toDouble() * span
    val fraction = (value - lowerBound) / span
    return fraction.toFloat().coerceIn(0f, 1f)
  }

  fun valueAtFraction(
    fraction: Float,
    minimumValue: Double,
    maximumValue: Double,
  ): Double {
    val f = fraction.coerceIn(0f, 1f).toDouble()
    return minimumValue + f * (maximumValue - minimumValue)
  }

  fun formatLabel(
    value: Double,
    configuration: RatingConfiguration,
  ): String? {
    if (configuration.layout.labelPlacement == RatingLabelPlacement.None) return null
    val custom = configuration.label.customText
    if (!custom.isNullOrBlank()) return custom
    val numeric = configuration.appearance.valueFormatter?.invoke(value) ?: defaultFormat(value)
    return configuration.label.valuePrefix + numeric + configuration.label.valueSuffix
  }

  fun formatAccessibilityValue(
    value: Double,
    maximumValue: Double,
    configuration: RatingConfiguration,
  ): String {
    val valueText = configuration.appearance.valueFormatter?.invoke(value) ?: defaultFormat(value)
    val maxText = configuration.appearance.valueFormatter?.invoke(maximumValue) ?: defaultFormat(maximumValue)
    return try {
      configuration.accessibilityValueFormat.format(valueText, maxText)
    } catch (_: Exception) {
      "$valueText of $maxText"
    }
  }

  private fun defaultFormat(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) {
      asLong.toString()
    } else {
      String.format("%.1f", value)
    }
  }
}
