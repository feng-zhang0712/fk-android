package com.fk.ui.callout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.fk.ui.callout.CalloutLayoutEngine.BeakEdge
import com.fk.ui.callout.CalloutLayoutEngine.ResolvedBeakOffset
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Nearest [CalloutController] from [CalloutHost]. */
val LocalCalloutController = compositionLocalOf<CalloutController> {
  error("No CalloutController provided. Wrap content in CalloutHost { }.")
}

/**
 * Hosts [content] and draws active callout overlays from [controller].
 *
 * Attach anchors with [Modifier.calloutAnchor] inside this host so window bounds stay in sync.
 */
@Composable
fun CalloutHost(
  controller: CalloutController = rememberCalloutController(),
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  CompositionLocalProvider(LocalCalloutController provides controller) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
      val containerSize = Size(
        width = constraints.maxWidth.toFloat(),
        height = constraints.maxHeight.toFloat(),
      )
      var hostOriginInWindow by remember { mutableStateOf(Offset.Zero) }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .onGloballyPositioned { coords ->
            hostOriginInWindow = coords.positionInWindow()
          },
      ) {
        content()
        CalloutOverlay(
          controller = controller,
          hostOriginInWindow = hostOriginInWindow,
          containerSize = containerSize,
        )
      }
    }
  }
}

@Composable
fun BoxScope.CalloutOverlay(
  controller: CalloutController,
  hostOriginInWindow: Offset,
  containerSize: Size,
) {
  controller.entries.forEach { entry ->
    key(entry.request.id) {
      CalloutSessionOverlay(
        controller = controller,
        entry = entry,
        hostOriginInWindow = hostOriginInWindow,
        containerSize = containerSize,
      )
    }
  }
}

@Composable
private fun CalloutSessionOverlay(
  controller: CalloutController,
  entry: CalloutEntry,
  hostOriginInWindow: Offset,
  containerSize: Size,
) {
  val request = entry.request
  val config = request.configuration
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current

  LaunchedEffect(request.id) {
    if (request.sourceRectInWindow != null) return@LaunchedEffect
    // Allow a layout pass before treating a missing anchor as unavailable.
    kotlinx.coroutines.delay(64)
    val bounds = controller.anchorBounds(request.anchorId)
    if (bounds == null || bounds == Rect.Zero) {
      controller.dismiss(request.id, CalloutDismissReason.AnchorUnavailable)
    }
  }

  val anchorState = controller.anchorState(request.anchorId)
  // Read mutable state so bound changes recompose the overlay.
  val liveBounds = anchorState?.boundsInWindow
  val anchorAttached = anchorState?.isAttached == true
  val anchorInWindow = request.sourceRectInWindow
    ?: liveBounds?.takeIf { anchorAttached && it != Rect.Zero }
  if (anchorInWindow == null || anchorInWindow == Rect.Zero) return

  val anchorInHost = Rect(
    left = anchorInWindow.left - hostOriginInWindow.x,
    top = anchorInWindow.top - hostOriginInWindow.y,
    right = anchorInWindow.right - hostOriginInWindow.x,
    bottom = anchorInWindow.bottom - hostOriginInWindow.y,
  )

  var bubbleSize by remember(request.id) { mutableStateOf(IntSize.Zero) }
  val measured = bubbleSize.width > 0 && bubbleSize.height > 0

  val beakWidthPx = with(density) { config.appearance.beakWidth.toPx() }
  val cornerRadiusPx = with(density) { config.appearance.cornerRadius.toPx() }
  val beakCornerInsetPx = with(density) { config.appearance.beakCornerInset.toPx() }
  val anchorSpacingPx = with(density) { config.anchorSpacing.toPx() }
  val screenEdgeMarginPx = with(density) { config.screenEdgeMargin.toPx() }
  val minWidthPx = config.minWidth?.let { with(density) { it.toPx() } }
  val matchedMinWidthPx = if (config.matchesAnchorWidth) {
    max(anchorInHost.width, minWidthPx ?: 0f)
  } else {
    minWidthPx
  }

  val resolvedBeakOffset = when (val offset = config.beakOffset) {
    CalloutBeakOffset.Automatic -> ResolvedBeakOffset.Automatic
    is CalloutBeakOffset.Fraction -> ResolvedBeakOffset.Fraction(offset.value, offset.reference)
    is CalloutBeakOffset.Fixed -> ResolvedBeakOffset.Fixed(
      valuePx = with(density) { offset.value.toPx() },
      reference = offset.reference,
    )
  }

  val imeBottom = WindowInsets.ime.getBottom(density).toFloat()
  LaunchedEffect(imeBottom, request.id, config.keyboardAvoidance) {
    if (config.keyboardAvoidance == CalloutKeyboardAvoidance.Dismiss && imeBottom > 0f) {
      controller.dismiss(request.id, CalloutDismissReason.Manual)
    }
  }
  val bottomObstruction =
    if (config.keyboardAvoidance == CalloutKeyboardAvoidance.Relayout) imeBottom else 0f

  // Prefer real measured size only — never layout with a fake placeholder size, or the
  // bubble will jump once onSizeChanged fires (visible as a slide toward the final frame).
  val layout = if (measured) {
    CalloutLayoutEngine.layout(
      anchorRect = anchorInHost,
      bubbleSize = Size(bubbleSize.width.toFloat(), bubbleSize.height.toFloat()),
      placement = config.placement,
      anchorSpacing = anchorSpacingPx,
      anchorAlignment = config.anchorAlignment,
      beakOffset = resolvedBeakOffset,
      beakWidth = beakWidthPx,
      cornerRadius = cornerRadiusPx,
      beakCornerInset = beakCornerInsetPx,
      layoutDirection = layoutDirection,
      containerBounds = Rect(Offset.Zero, containerSize),
      screenEdgeMargin = screenEdgeMarginPx,
      flipsWhenNeeded = config.flipsPlacementWhenNeeded,
      bottomObstruction = bottomObstruction,
    )
  } else {
    null
  }

  val (_, onSurface, secondary) = resolvedBubbleColors(config.appearance)
  val announcement = config.accessibilityAnnouncementOverride
    ?: accessibilityText(request.content)

  val interceptOutside = config.backdrop.showsDimmedBackdrop ||
    (config.tapOutsideToDismiss && !config.passesThroughOutsideTouches)

  if (config.backdrop.showsDimmedBackdrop) {
    CalloutBackdrop(
      anchorRect = anchorInHost,
      style = config.backdrop,
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(request.id, config.tapOutsideToDismiss) {
          if (!config.tapOutsideToDismiss) return@pointerInput
          detectTapGestures {
            controller.dismiss(request.id, CalloutDismissReason.TapOutside)
          }
        },
    )
  } else if (config.tapOutsideToDismiss && layout != null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(request.id, interceptOutside, layout.frame) {
          detectTapGestures { offset ->
            val frame = layout.frame
            val inside = offset.x in frame.left..frame.right &&
              offset.y in frame.top..frame.bottom
            if (!inside) {
              controller.dismiss(request.id, CalloutDismissReason.TapOutside)
            }
          }
        },
    )
  }

  val alpha = remember(request.id) { Animatable(0f) }
  val scale = remember(request.id) {
    Animatable(if (config.animationStyle == CalloutAnimationStyle.FadeScale) 0.94f else 1f)
  }
  // Gate reveal so the first painted frame is already at the final layout position.
  var reveal by remember(request.id) { mutableStateOf(false) }

  LaunchedEffect(measured, request.id, config.animationStyle, config.animationDurationMs) {
    if (!measured) {
      reveal = false
      alpha.snapTo(0f)
      scale.snapTo(if (config.animationStyle == CalloutAnimationStyle.FadeScale) 0.94f else 1f)
      return@LaunchedEffect
    }
    reveal = true
    alpha.snapTo(0f)
    if (config.animationStyle == CalloutAnimationStyle.FadeScale) {
      scale.snapTo(0.94f)
    } else {
      scale.snapTo(1f)
    }
    // One frame at the correct offset with alpha 0, then animate in place.
    kotlinx.coroutines.yield()
    val spec = tween<Float>(config.animationDurationMs)
    launch { alpha.animateTo(1f, spec) }
    if (config.animationStyle == CalloutAnimationStyle.FadeScale) {
      launch { scale.animateTo(1f, tween(config.animationDurationMs)) }
    }
  }

  val minW = matchedMinWidthPx?.let { with(density) { it.toDp() } } ?: 0.dp
  val transformOrigin = remember(layout, bubbleSize) {
    beakTransformOrigin(layout, bubbleSize)
  }
  // Read animatables in composition so each tick recomposes / updates the layer.
  val alphaValue = alpha.value
  val scaleValue = scale.value

  Box(
    modifier = Modifier
      .offset {
        if (layout != null && reveal) {
          IntOffset(layout.frame.left.roundToInt(), layout.frame.top.roundToInt())
        } else {
          // Off-screen measure pass — keeps the bubble out of view until sized.
          IntOffset(-10_000, -10_000)
        }
      }
      .graphicsLayer {
        this.alpha = if (reveal) alphaValue else 0f
        val s = if (reveal) scaleValue else 0.94f
        scaleX = s
        scaleY = s
        this.transformOrigin = transformOrigin
      }
      .widthIn(min = minW, max = config.maxWidth)
      .wrapContentSize()
      .onSizeChanged { size ->
        if (size.width > 0 && size.height > 0 && size != bubbleSize) {
          bubbleSize = size
        }
      }
      .then(
        if (config.accessibilityAnnouncementEnabled && announcement != null) {
          Modifier.semantics { contentDescription = announcement }
        } else {
          Modifier
        },
      ),
  ) {
    CalloutBubble(
      placement = layout?.placement ?: config.placement.resolvedForMeasure(),
      beakCenterAlongEdge = layout?.beakCenterAlongEdge
        ?: (bubbleSize.width / 2f).coerceAtLeast(0f),
      appearance = config.appearance,
      contentPadding = config.contentPadding,
    ) {
      CalloutContentBody(
        content = request.content,
        configuration = config,
        onSurface = onSurface,
        secondary = secondary,
        onAction = { actionId -> controller.invokeAction(request.id, actionId) },
        onClose = { controller.invokeClose(request.id) },
        onMenuSelect = { item -> controller.invokeMenuSelection(request.id, item) },
      )
    }
  }
}

/** Prefer a cardinal placement while measuring so beak padding matches the eventual edge. */
private fun CalloutPlacement.resolvedForMeasure(): CalloutPlacement =
  if (this == CalloutPlacement.Automatic) CalloutPlacement.Bottom else this

private fun beakTransformOrigin(
  layout: CalloutLayoutEngine.Result?,
  bubbleSize: IntSize,
): TransformOrigin {
  if (layout == null || bubbleSize.width <= 0 || bubbleSize.height <= 0) {
    return TransformOrigin.Center
  }
  val edge = with(CalloutLayoutEngine) { layout.placement.beakEdge() }
  val px = layout.beakCenterAlongEdge
  return when (edge) {
    BeakEdge.Top -> TransformOrigin(
      pivotFractionX = (px / bubbleSize.width).coerceIn(0f, 1f),
      pivotFractionY = 0f,
    )
    BeakEdge.Bottom -> TransformOrigin(
      pivotFractionX = (px / bubbleSize.width).coerceIn(0f, 1f),
      pivotFractionY = 1f,
    )
    BeakEdge.Start -> TransformOrigin(
      pivotFractionX = 0f,
      pivotFractionY = (px / bubbleSize.height).coerceIn(0f, 1f),
    )
    BeakEdge.End -> TransformOrigin(
      pivotFractionX = 1f,
      pivotFractionY = (px / bubbleSize.height).coerceIn(0f, 1f),
    )
  }
}

private fun accessibilityText(content: CalloutContent): String? = when (content) {
  is CalloutContent.Message -> content.text
  is CalloutContent.TitleSubtitle -> "${content.title}. ${content.message}"
  is CalloutContent.IconMessage -> content.message
  is CalloutContent.MessageWithActions -> content.message
  is CalloutContent.HeaderPanel -> "${content.header.title}. ${content.body}"
  is CalloutContent.CoachMark -> "${content.content.title}. ${content.content.message}"
  is CalloutContent.Menu -> content.menu.header
  is CalloutContent.Custom -> null
}
