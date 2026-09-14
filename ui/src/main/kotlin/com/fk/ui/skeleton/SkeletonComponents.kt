package com.fk.ui.skeleton

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.fkColor
import kotlin.math.max

/** Shared shimmer phase (0f..1f). Null = no container clock. */
internal val LocalSkeletonShimmerPhase = compositionLocalOf<Float?> { null }

/** Configuration inherited from the nearest [SkeletonContainer], if any. */
val LocalSkeletonConfiguration = compositionLocalOf { Skeleton.DefaultConfiguration }

/**
 * Resolves placeholder colors from [config] or the current [com.fk.ui.theme.FkTheme].
 */
@Composable
fun rememberSkeletonColors(
  config: SkeletonConfiguration = LocalSkeletonConfiguration.current,
): Pair<Color, Color> {
  val surface = fkColor(FkColorRole.Surface)
  val onSurface = fkColor(FkColorRole.OnSurface)
  val base = config.baseColor ?: surface.copy(alpha = 0.92f).compositeOverSurface(onSurface)
  val highlight = config.highlightColor
    ?: onSurface.copy(alpha = 0.08f).compositeOver(base)
  return base to highlight
}

private fun Color.compositeOverSurface(onSurface: Color): Color =
  if (alpha >= 0.99f) {
    onSurface.copy(alpha = 0.06f).compositeOver(this)
  } else {
    this
  }

private fun Color.compositeOver(background: Color): Color {
  val a = alpha
  val r = red * a + background.red * (1f - a)
  val g = green * a + background.green * (1f - a)
  val b = blue * a + background.blue * (1f - a)
  return Color(red = r, green = g, blue = b, alpha = 1f)
}

/**
 * Provides a shared shimmer clock (and default [SkeletonConfiguration]) for children.
 *
 * Conceptually aligned with iOS `FKSkeletonContainerView.usesUnifiedShimmer`.
 */
@Composable
fun SkeletonContainer(
  modifier: Modifier = Modifier,
  configuration: SkeletonConfiguration = Skeleton.DefaultConfiguration,
  content: @Composable BoxScope.() -> Unit,
) {
  val phase = rememberSkeletonPhase(configuration)
  CompositionLocalProvider(
    LocalSkeletonShimmerPhase provides phase,
    LocalSkeletonConfiguration provides configuration,
  ) {
    Box(
      modifier = modifier.semantics {
        contentDescription = "Loading"
      },
      content = content,
    )
  }
}

/**
 * Single placeholder block (rectangle / rounded / circle via [shape]).
 *
 * Conceptually aligned with iOS `FKSkeletonView`.
 *
 * When [width] is null, [height] is still applied so fraction-based widths
 * (e.g. `Modifier.fillMaxWidth(0.7f)`) keep a visible line height.
 */
@Composable
fun SkeletonBlock(
  modifier: Modifier = Modifier,
  width: Dp? = null,
  height: Dp = 12.dp,
  shape: Shape? = null,
  configuration: SkeletonConfiguration = LocalSkeletonConfiguration.current,
) {
  val resolvedShape = shape ?: RoundedCornerShape(configuration.cornerRadius)
  val sized = if (width != null) {
    modifier.size(width = width, height = height)
  } else {
    modifier.height(height)
  }
  Box(
    modifier = sized
      .clip(resolvedShape)
      .skeletonFill(configuration)
      .semantics { invisibleToUser() },
  )
}

/** Circular avatar-sized skeleton. */
@Composable
fun SkeletonCircle(
  size: Dp,
  modifier: Modifier = Modifier,
  configuration: SkeletonConfiguration = LocalSkeletonConfiguration.current,
) {
  SkeletonBlock(
    modifier = modifier,
    width = size,
    height = size,
    shape = CircleShape,
    configuration = configuration,
  )
}

/**
 * Applies a skeleton fill to an existing layout (overlay-style placeholder).
 *
 * When [enabled] is false, returns [this] unchanged.
 */
@Composable
fun Modifier.skeleton(
  enabled: Boolean = true,
  configuration: SkeletonConfiguration = LocalSkeletonConfiguration.current,
  shape: Shape? = null,
): Modifier {
  if (!enabled) return this
  val resolvedShape = shape ?: RoundedCornerShape(configuration.cornerRadius)
  return this
    .clip(resolvedShape)
    .skeletonFill(configuration)
    .semantics { invisibleToUser() }
}

@Composable
internal fun rememberSkeletonPhase(configuration: SkeletonConfiguration): Float {
  val transition = rememberInfiniteTransition(label = "fk-skeleton")
  val duration = configuration.animationDurationMs.coerceAtLeast(300)
  val animated by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = duration, easing = LinearEasing),
      repeatMode = if (configuration.animationMode == SkeletonAnimationMode.Pulse) {
        RepeatMode.Reverse
      } else {
        RepeatMode.Restart
      },
    ),
    label = "fk-skeleton-phase",
  )
  return if (configuration.animationMode == SkeletonAnimationMode.None) 0f else animated
}

@Composable
private fun Modifier.skeletonFill(configuration: SkeletonConfiguration): Modifier {
  val (base, highlight) = rememberSkeletonColors(configuration)
  val sharedPhase = LocalSkeletonShimmerPhase.current
  // Always compose the local clock (Compose rules); prefer shared phase when present.
  val localPhase = rememberSkeletonPhase(configuration)
  val phase = sharedPhase ?: localPhase

  return when (configuration.animationMode) {
    SkeletonAnimationMode.None -> background(base)
    SkeletonAnimationMode.Pulse -> {
      val alpha = configuration.pulseMinAlpha +
        (1f - configuration.pulseMinAlpha) * phase
      background(base.copy(alpha = alpha.coerceIn(0.2f, 1f)))
    }
    SkeletonAnimationMode.Shimmer -> {
      val density = LocalDensity.current
      val brush = remember(phase, base, highlight, configuration.shimmerDirection) {
        shimmerBrush(
          phase = phase,
          base = base,
          highlight = highlight,
          direction = configuration.shimmerDirection,
          travelPx = with(density) { 240.dp.toPx() },
        )
      }
      background(brush)
    }
  }
}

private fun shimmerBrush(
  phase: Float,
  base: Color,
  highlight: Color,
  direction: SkeletonShimmerDirection,
  travelPx: Float,
): Brush {
  val travel = max(travelPx, 1f)
  val startOffset = -travel + phase * travel * 2f
  val (start, end) = when (direction) {
    SkeletonShimmerDirection.LeftToRight ->
      Offset(startOffset, 0f) to Offset(startOffset + travel, 0f)
    SkeletonShimmerDirection.RightToLeft ->
      Offset(travel - startOffset, 0f) to Offset(-startOffset, 0f)
    SkeletonShimmerDirection.TopToBottom ->
      Offset(0f, startOffset) to Offset(0f, startOffset + travel)
    SkeletonShimmerDirection.BottomToTop ->
      Offset(0f, travel - startOffset) to Offset(0f, -startOffset)
  }
  return Brush.linearGradient(
    colors = listOf(base, highlight, base),
    start = start,
    end = end,
  )
}
