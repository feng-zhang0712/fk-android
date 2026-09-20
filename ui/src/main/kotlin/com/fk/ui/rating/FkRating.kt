package com.fk.ui.rating

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.fk.ui.R
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkTextStyle

/**
 * Read-only or interactive rating control with whole / half / custom step snapping.
 *
 * Partial item fills use a horizontal clip over the filled glyph so half-star (and
 * other fractional) values render without a dedicated half asset.
 *
 * @param value Current snapped rating value.
 * @param onValueChange Invoked after user input snaps to the configured step.
 * @param configuration Layout, appearance, interaction, and caption settings.
 * @param minimumValue Lower bound of the rating scale.
 * @param maximumValue Upper bound (defaults to [RatingLayoutConfiguration.itemCount]).
 * @param enabled When false, interaction is blocked and [RatingInteractionConfiguration.disabledAlpha] applies.
 */
@Composable
fun FkRating(
  value: Double,
  onValueChange: (Double) -> Unit,
  modifier: Modifier = Modifier,
  configuration: RatingConfiguration = RatingConfiguration(),
  minimumValue: Double = 0.0,
  maximumValue: Double = configuration.layout.itemCount.toDouble(),
  enabled: Boolean = true,
) {
  val layout = configuration.layout
  val appearance = configuration.appearance
  val interaction = configuration.interaction
  val interactive = enabled && interaction.mode == RatingInteractionMode.Interactive
  val haptic = LocalHapticFeedback.current
  val layoutDirection = LocalLayoutDirection.current
  val latestOnValueChange by rememberUpdatedState(onValueChange)
  val latestValue by rememberUpdatedState(value)

  val (emptyPainter, filledPainter) = resolvePainters(appearance.iconStyle)
  val labelText = remember(value, configuration) {
    RatingMath.formatLabel(value, configuration)
  }
  val a11yValue = remember(value, maximumValue, configuration) {
    RatingMath.formatAccessibilityValue(value, maximumValue, configuration)
  }

  var rowWidthPx by remember { mutableFloatStateOf(0f) }

  fun commitFromLocalX(localX: Float) {
    if (!interactive || rowWidthPx <= 0f) return
    val rawX = if (layoutDirection == LayoutDirection.Rtl) {
      rowWidthPx - localX
    } else {
      localX
    }
    val fraction = (rawX / rowWidthPx).coerceIn(0f, 1f)
    val raw = RatingMath.valueAtFraction(fraction, minimumValue, maximumValue)
    val snapped = RatingMath.snap(raw, minimumValue, maximumValue, interaction.step)
    if (snapped != latestValue) {
      if (interaction.enableHaptics) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      latestOnValueChange(snapped)
    }
  }

  val alpha = if (enabled) 1f else interaction.disabledAlpha.coerceIn(0.1f, 1f)

  val semanticsModifier = Modifier.semantics(mergeDescendants = true) {
    configuration.contentDescription?.let { contentDescription = it }
    stateDescription = a11yValue
    progressBarRangeInfo = ProgressBarRangeInfo(
      current = value.toFloat(),
      range = minimumValue.toFloat()..maximumValue.toFloat(),
      steps = accessibilitySteps(minimumValue, maximumValue, interaction.step),
    )
    if (interactive) {
      setProgress { target ->
        val snapped = RatingMath.snap(
          target.toDouble(),
          minimumValue,
          maximumValue,
          interaction.step,
        )
        latestOnValueChange(snapped)
        true
      }
    }
  }

  val iconsRow: @Composable () -> Unit = {
    Row(
      modifier = Modifier
        .onSizeChanged { rowWidthPx = it.width.toFloat() }
        .then(
          if (interactive) {
            Modifier
              .pointerInput(interaction, minimumValue, maximumValue, layoutDirection) {
                detectTapGestures { offset -> commitFromLocalX(offset.x) }
              }
              .pointerInput(interaction, minimumValue, maximumValue, layoutDirection) {
                if (!interaction.allowsDragSelection) return@pointerInput
                detectHorizontalDragGestures(
                  onDragStart = { offset -> commitFromLocalX(offset.x) },
                  onHorizontalDrag = { change, _ ->
                    change.consume()
                    commitFromLocalX(change.position.x)
                  },
                )
              }
          } else {
            Modifier
          },
        ),
      horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(layout.itemCount) { index ->
        RatingItem(
          fillFraction = RatingMath.fillFraction(
            index = index,
            value = value,
            minimumValue = minimumValue,
            maximumValue = maximumValue,
            itemCount = layout.itemCount,
          ),
          emptyPainter = emptyPainter,
          filledPainter = filledPainter,
          emptyColor = appearance.emptyColor,
          filledColor = appearance.filledColor,
          size = layout.itemSize,
        )
      }
    }
  }

  val caption: (@Composable () -> Unit)? = labelText?.let { text ->
    {
      Text(
        text = text,
        style = fkTextStyle(FkTextStyle.Subheadline),
        color = appearance.labelColor,
        maxLines = 1,
      )
    }
  }

  Box(
    modifier = modifier
      .alpha(alpha)
      .padding(layout.contentPadding)
      .then(semanticsModifier),
  ) {
    when (layout.labelPlacement) {
      RatingLabelPlacement.None -> iconsRow()
      RatingLabelPlacement.Trailing -> {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(layout.labelSpacing),
        ) {
          iconsRow()
          caption?.invoke()
        }
      }
      RatingLabelPlacement.Bottom -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(layout.labelSpacing),
        ) {
          iconsRow()
          caption?.invoke()
        }
      }
    }
  }
}

/**
 * Read-only star rating convenience.
 */
@Composable
fun FkRatingReadOnly(
  value: Double,
  modifier: Modifier = Modifier,
  itemCount: Int = 5,
  step: RatingStep = RatingStep.Half,
  configuration: RatingConfiguration = RatingConfiguration(
    layout = RatingLayoutConfiguration(itemCount = itemCount),
    interaction = RatingInteractionConfiguration(
      mode = RatingInteractionMode.ReadOnly,
      step = step,
    ),
  ),
) {
  FkRating(
    value = value,
    onValueChange = {},
    modifier = modifier,
    configuration = configuration.copy(
      layout = configuration.layout.copy(itemCount = itemCount),
      interaction = configuration.interaction.copy(
        mode = RatingInteractionMode.ReadOnly,
        step = step,
      ),
    ),
    maximumValue = itemCount.toDouble(),
  )
}

/**
 * Interactive star rating convenience (defaults to half-step snapping).
 */
@Composable
fun FkRatingInteractive(
  value: Double,
  onValueChange: (Double) -> Unit,
  modifier: Modifier = Modifier,
  itemCount: Int = 5,
  step: RatingStep = RatingStep.Half,
  configuration: RatingConfiguration = RatingConfiguration(
    layout = RatingLayoutConfiguration(itemCount = itemCount),
    interaction = RatingInteractionConfiguration(
      mode = RatingInteractionMode.Interactive,
      step = step,
    ),
  ),
) {
  FkRating(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    configuration = configuration.copy(
      layout = configuration.layout.copy(itemCount = itemCount),
      interaction = configuration.interaction.copy(
        mode = RatingInteractionMode.Interactive,
        step = step,
      ),
    ),
    maximumValue = itemCount.toDouble(),
  )
}

@Composable
private fun RatingItem(
  fillFraction: Float,
  emptyPainter: Painter,
  filledPainter: Painter,
  emptyColor: Color,
  filledColor: Color,
  size: Dp,
) {
  val fraction = fillFraction.coerceIn(0f, 1f)
  Box(modifier = Modifier.size(size)) {
    Icon(
      painter = emptyPainter,
      contentDescription = null,
      tint = emptyColor,
      modifier = Modifier.size(size),
    )
    if (fraction > 0.001f) {
      Icon(
        painter = filledPainter,
        contentDescription = null,
        tint = filledColor,
        // Keep intrinsic layout size; clip the draw pass so half-stars crop
        // instead of shrinking the glyph into the left slot.
        modifier = Modifier
          .size(size)
          .drawWithContent {
            val clipWidth = this.size.width * fraction
            clipRect(right = clipWidth) {
              this@drawWithContent.drawContent()
            }
          },
      )
    }
  }
}

@Composable
private fun resolvePainters(style: RatingIconStyle): Pair<Painter, Painter> {
  return when (style) {
    // Same silhouette for empty + filled so tints stack/clip without a size mismatch
    // (outline stroke would otherwise read larger than the fill path).
    RatingIconStyle.Star -> {
      val star = painterResource(R.drawable.fk_ic_star_fill)
      star to star
    }
    is RatingIconStyle.Painters -> style.empty to style.filled
  }
}

private fun accessibilitySteps(
  minimumValue: Double,
  maximumValue: Double,
  step: RatingStep,
): Int {
  val span = maximumValue - minimumValue
  val increment = step.increment
  if (span <= 0.0 || increment <= 0.0) return 0
  val count = (span / increment).toInt()
  return maxOf(0, count - 1)
}
