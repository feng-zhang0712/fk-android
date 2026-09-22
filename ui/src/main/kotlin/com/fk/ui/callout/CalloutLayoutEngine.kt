package com.fk.ui.callout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.min

/** Pure layout math for anchored callout bubbles (pixel units). */
internal object CalloutLayoutEngine {

  data class Result(
    val frame: Rect,
    val placement: CalloutPlacement,
    /** Beak center along its edge, measured from the edge start in pixels. */
    val beakCenterAlongEdge: Float,
  )

  private val automaticCandidates = listOf(
    CalloutPlacement.Top,
    CalloutPlacement.Bottom,
    CalloutPlacement.Start,
    CalloutPlacement.End,
  )

  fun layout(
    anchorRect: Rect,
    bubbleSize: Size,
    placement: CalloutPlacement,
    anchorSpacing: Float,
    anchorAlignment: CalloutAnchorAlignment,
    beakOffset: ResolvedBeakOffset,
    beakWidth: Float,
    cornerRadius: Float,
    beakCornerInset: Float,
    layoutDirection: LayoutDirection,
    containerBounds: Rect,
    screenEdgeMargin: Float,
    flipsWhenNeeded: Boolean,
    bottomObstruction: Float = 0f,
  ): Result {
    val safeLayoutBounds = layoutBounds(
      containerBounds = containerBounds,
      screenEdgeMargin = screenEdgeMargin,
      bottomObstruction = bottomObstruction,
    )
    val candidates = candidatePlacements(placement, flipsWhenNeeded)
    var best: Result? = null
    var bestScore = Float.POSITIVE_INFINITY

    for (candidate in candidates) {
      val proposed = proposedFrame(
        anchorRect = anchorRect,
        bubbleSize = bubbleSize,
        placement = candidate,
        anchorSpacing = anchorSpacing,
        anchorAlignment = anchorAlignment,
        layoutDirection = layoutDirection,
      )
      val clamped = clampFrame(proposed, safeLayoutBounds)
      val beakCenter = resolvedBeakCenter(
        anchorRect = anchorRect,
        bubbleFrame = clamped,
        placement = candidate,
        beakOffset = beakOffset,
        beakWidth = beakWidth,
        cornerRadius = cornerRadius,
        beakCornerInset = beakCornerInset,
        layoutDirection = layoutDirection,
      )
      val score = placementScore(
        frame = clamped,
        layoutBounds = safeLayoutBounds,
        placement = candidate,
        requestedPlacement = placement,
        anchorRect = anchorRect,
        bubbleSize = bubbleSize,
        anchorSpacing = anchorSpacing,
      )
      if (score < bestScore) {
        bestScore = score
        best = Result(clamped, candidate, beakCenter)
      }
    }

    val fallbackPlacement = if (placement == CalloutPlacement.Automatic) {
      val topSpace = availableSpace(anchorRect, CalloutPlacement.Top, safeLayoutBounds, anchorSpacing)
      val bottomSpace = availableSpace(anchorRect, CalloutPlacement.Bottom, safeLayoutBounds, anchorSpacing)
      if (bottomSpace >= topSpace) CalloutPlacement.Bottom else CalloutPlacement.Top
    } else {
      placement
    }
    val fallbackFrame = clampFrame(
      proposedFrame(
        anchorRect = anchorRect,
        bubbleSize = bubbleSize,
        placement = fallbackPlacement,
        anchorSpacing = anchorSpacing,
        anchorAlignment = anchorAlignment,
        layoutDirection = layoutDirection,
      ),
      safeLayoutBounds,
    )
    val resolved = best ?: Result(
      frame = fallbackFrame,
      placement = fallbackPlacement,
      beakCenterAlongEdge = resolvedBeakCenter(
        anchorRect = anchorRect,
        bubbleFrame = fallbackFrame,
        placement = fallbackPlacement,
        beakOffset = beakOffset,
        beakWidth = beakWidth,
        cornerRadius = cornerRadius,
        beakCornerInset = beakCornerInset,
        layoutDirection = layoutDirection,
      ),
    )
    val clampedFrame = clampFrame(resolved.frame, safeLayoutBounds)
    return Result(
      frame = clampedFrame,
      placement = resolved.placement,
      beakCenterAlongEdge = resolvedBeakCenter(
        anchorRect = anchorRect,
        bubbleFrame = clampedFrame,
        placement = resolved.placement,
        beakOffset = beakOffset,
        beakWidth = beakWidth,
        cornerRadius = cornerRadius,
        beakCornerInset = beakCornerInset,
        layoutDirection = layoutDirection,
      ),
    )
  }

  fun layoutBounds(
    containerBounds: Rect,
    screenEdgeMargin: Float,
    bottomObstruction: Float = 0f,
  ): Rect {
    var rect = Rect(
      left = containerBounds.left + screenEdgeMargin,
      top = containerBounds.top + screenEdgeMargin,
      right = containerBounds.right - screenEdgeMargin,
      bottom = containerBounds.bottom - screenEdgeMargin,
    )
    if (bottomObstruction > 0f) {
      rect = rect.copy(bottom = max(rect.top, rect.bottom - bottomObstruction))
    }
    return rect
  }

  internal sealed class ResolvedBeakOffset {
    data object Automatic : ResolvedBeakOffset()
    data class Fraction(val value: Float, val reference: CalloutBeakOffsetReference) : ResolvedBeakOffset()
    data class Fixed(val valuePx: Float, val reference: CalloutBeakOffsetReference) : ResolvedBeakOffset()
  }

  internal enum class BeakEdge { Top, Bottom, Start, End }
  internal enum class BeakAlignment { Center, Start, End }

  internal fun CalloutPlacement.beakEdge(): BeakEdge = when (this) {
    CalloutPlacement.Automatic,
    CalloutPlacement.Top,
    CalloutPlacement.TopStart,
    CalloutPlacement.TopEnd,
    -> BeakEdge.Bottom

    CalloutPlacement.Bottom,
    CalloutPlacement.BottomStart,
    CalloutPlacement.BottomEnd,
    -> BeakEdge.Top

    CalloutPlacement.Start,
    CalloutPlacement.StartTop,
    CalloutPlacement.StartBottom,
    -> BeakEdge.End

    CalloutPlacement.End,
    CalloutPlacement.EndTop,
    CalloutPlacement.EndBottom,
    -> BeakEdge.Start
  }

  internal fun CalloutPlacement.flipped(): CalloutPlacement = when (this) {
    CalloutPlacement.Automatic -> CalloutPlacement.Automatic
    CalloutPlacement.Top -> CalloutPlacement.Bottom
    CalloutPlacement.TopStart -> CalloutPlacement.BottomStart
    CalloutPlacement.TopEnd -> CalloutPlacement.BottomEnd
    CalloutPlacement.Bottom -> CalloutPlacement.Top
    CalloutPlacement.BottomStart -> CalloutPlacement.TopStart
    CalloutPlacement.BottomEnd -> CalloutPlacement.TopEnd
    CalloutPlacement.Start -> CalloutPlacement.End
    CalloutPlacement.StartTop -> CalloutPlacement.EndTop
    CalloutPlacement.StartBottom -> CalloutPlacement.EndBottom
    CalloutPlacement.End -> CalloutPlacement.Start
    CalloutPlacement.EndTop -> CalloutPlacement.StartTop
    CalloutPlacement.EndBottom -> CalloutPlacement.StartBottom
  }

  private fun CalloutPlacement.preferredBeakAlignment(): BeakAlignment = when (this) {
    CalloutPlacement.Automatic,
    CalloutPlacement.Top,
    CalloutPlacement.Bottom,
    CalloutPlacement.Start,
    CalloutPlacement.End,
    -> BeakAlignment.Center

    CalloutPlacement.TopStart,
    CalloutPlacement.BottomStart,
    CalloutPlacement.StartTop,
    CalloutPlacement.EndTop,
    -> BeakAlignment.Start

    CalloutPlacement.TopEnd,
    CalloutPlacement.BottomEnd,
    CalloutPlacement.StartBottom,
    CalloutPlacement.EndBottom,
    -> BeakAlignment.End
  }

  private fun candidatePlacements(
    placement: CalloutPlacement,
    flipsWhenNeeded: Boolean,
  ): List<CalloutPlacement> {
    if (placement == CalloutPlacement.Automatic) return automaticCandidates
    return if (flipsWhenNeeded) listOf(placement, placement.flipped()) else listOf(placement)
  }

  private fun placementScore(
    frame: Rect,
    layoutBounds: Rect,
    placement: CalloutPlacement,
    requestedPlacement: CalloutPlacement,
    anchorRect: Rect,
    bubbleSize: Size,
    anchorSpacing: Float,
  ): Float {
    val overflow = overflowPenalty(frame, layoutBounds)
    if (requestedPlacement == CalloutPlacement.Automatic) {
      val available = availableSpace(anchorRect, placement, layoutBounds, anchorSpacing)
      val required = requiredSpace(placement, bubbleSize)
      val deficit = max(0f, required - available)
      return overflow + deficit * 1_000f - available
    }
    if (placement == requestedPlacement) return overflow
    if (placement == requestedPlacement.flipped()) return overflow + 1f
    return overflow + 1_000f
  }

  private fun resolvedBeakCenter(
    anchorRect: Rect,
    bubbleFrame: Rect,
    placement: CalloutPlacement,
    beakOffset: ResolvedBeakOffset,
    beakWidth: Float,
    cornerRadius: Float,
    beakCornerInset: Float,
    layoutDirection: LayoutDirection,
  ): Float {
    val edge = placement.beakEdge()
    val isHorizontalEdge = edge == BeakEdge.Top || edge == BeakEdge.Bottom
    val edgeLength = if (isHorizontalEdge) bubbleFrame.width else bubbleFrame.height
    val margin = maxOf(beakWidth * 0.5f + 2f, cornerRadius + 2f, beakCornerInset + beakWidth * 0.5f)
    val usableLength = max(edgeLength - margin * 2f, 1f)

    return when (beakOffset) {
      ResolvedBeakOffset.Automatic -> clampBeakCenter(
        automaticBeakCenter(
          anchorRect, bubbleFrame, placement, margin, edgeLength, isHorizontalEdge, layoutDirection,
        ),
        edgeLength,
        margin,
      )
      is ResolvedBeakOffset.Fraction -> {
        val clamped = beakOffset.value.coerceIn(0f, 1f)
        val center = when (beakOffset.reference) {
          CalloutBeakOffsetReference.BubbleEdge -> margin + usableLength * clamped
          CalloutBeakOffsetReference.Anchor -> {
            val origin = anchorReferenceOnEdge(
              anchorRect, bubbleFrame, placement, margin, edgeLength, isHorizontalEdge, layoutDirection,
            )
            origin + (edgeLength - margin - origin) * clamped
          }
        }
        clampBeakCenter(center, edgeLength, margin)
      }
      is ResolvedBeakOffset.Fixed -> {
        val center = when (beakOffset.reference) {
          CalloutBeakOffsetReference.BubbleEdge -> margin + beakOffset.valuePx
          CalloutBeakOffsetReference.Anchor -> {
            val origin = anchorReferenceOnEdge(
              anchorRect, bubbleFrame, placement, margin, edgeLength, isHorizontalEdge, layoutDirection,
            )
            origin + beakOffset.valuePx
          }
        }
        clampBeakCenter(center, edgeLength, margin)
      }
    }
  }

  private fun automaticBeakCenter(
    anchorRect: Rect,
    bubbleFrame: Rect,
    placement: CalloutPlacement,
    margin: Float,
    edgeLength: Float,
    isHorizontalEdge: Boolean,
    layoutDirection: LayoutDirection,
  ): Float = clampBeakCenter(
    projectedAnchorCoordinateOnBeakEdge(
      anchorRect, bubbleFrame, placement, placement.preferredBeakAlignment(),
      isHorizontalEdge, layoutDirection,
    ),
    edgeLength,
    margin,
  )

  private fun anchorReferenceOnEdge(
    anchorRect: Rect,
    bubbleFrame: Rect,
    placement: CalloutPlacement,
    margin: Float,
    edgeLength: Float,
    isHorizontalEdge: Boolean,
    layoutDirection: LayoutDirection,
  ): Float = clampBeakCenter(
    projectedAnchorCoordinateOnBeakEdge(
      anchorRect, bubbleFrame, placement, placement.preferredBeakAlignment(),
      isHorizontalEdge, layoutDirection,
    ),
    edgeLength,
    margin,
  )

  private fun projectedAnchorCoordinateOnBeakEdge(
    anchorRect: Rect,
    bubbleFrame: Rect,
    placement: CalloutPlacement,
    beakAlignment: BeakAlignment,
    isHorizontalEdge: Boolean,
    layoutDirection: LayoutDirection,
  ): Float {
    when (beakAlignment) {
      BeakAlignment.Center -> {
        val center = Offset(anchorRect.center.x, anchorRect.center.y)
        return when (placement.beakEdge()) {
          BeakEdge.Top, BeakEdge.Bottom -> center.x - bubbleFrame.left
          BeakEdge.Start, BeakEdge.End -> center.y - bubbleFrame.top
        }
      }
      BeakAlignment.Start -> {
        if (isHorizontalEdge) {
          val anchorX = if (layoutDirection == LayoutDirection.Rtl) anchorRect.right else anchorRect.left
          return anchorX - bubbleFrame.left
        }
        return anchorRect.top - bubbleFrame.top
      }
      BeakAlignment.End -> {
        if (isHorizontalEdge) {
          val anchorX = if (layoutDirection == LayoutDirection.Rtl) anchorRect.left else anchorRect.right
          return anchorX - bubbleFrame.left
        }
        return anchorRect.bottom - bubbleFrame.top
      }
    }
  }

  private fun clampBeakCenter(proposed: Float, edgeLength: Float, margin: Float): Float =
    min(max(proposed, margin), edgeLength - margin)

  private fun proposedFrame(
    anchorRect: Rect,
    bubbleSize: Size,
    placement: CalloutPlacement,
    anchorSpacing: Float,
    anchorAlignment: CalloutAnchorAlignment,
    layoutDirection: LayoutDirection,
  ): Rect {
    val alignment = effectiveAnchorAlignment(placement, anchorAlignment)
    val gap = anchorSpacing
    val (start, end) = absoluteHorizontal(anchorRect, layoutDirection)
    val originX: Float
    val originY: Float
    when (placement) {
      CalloutPlacement.Automatic,
      CalloutPlacement.Top,
      CalloutPlacement.TopStart,
      CalloutPlacement.TopEnd,
      -> {
        originY = anchorRect.top - gap - bubbleSize.height
        originX = alignedOrigin(start, end, bubbleSize.width, alignment)
      }
      CalloutPlacement.Bottom,
      CalloutPlacement.BottomStart,
      CalloutPlacement.BottomEnd,
      -> {
        originY = anchorRect.bottom + gap
        originX = alignedOrigin(start, end, bubbleSize.width, alignment)
      }
      CalloutPlacement.Start,
      CalloutPlacement.StartTop,
      CalloutPlacement.StartBottom,
      -> {
        originX = if (layoutDirection == LayoutDirection.Ltr) {
          anchorRect.left - gap - bubbleSize.width
        } else {
          anchorRect.right + gap
        }
        originY = alignedOrigin(anchorRect.top, anchorRect.bottom, bubbleSize.height, alignment)
      }
      CalloutPlacement.End,
      CalloutPlacement.EndTop,
      CalloutPlacement.EndBottom,
      -> {
        originX = if (layoutDirection == LayoutDirection.Ltr) {
          anchorRect.right + gap
        } else {
          anchorRect.left - gap - bubbleSize.width
        }
        originY = alignedOrigin(anchorRect.top, anchorRect.bottom, bubbleSize.height, alignment)
      }
    }
    return Rect(offset = Offset(originX, originY), size = bubbleSize)
  }

  private fun absoluteHorizontal(
    anchorRect: Rect,
    layoutDirection: LayoutDirection,
  ): Pair<Float, Float> =
    if (layoutDirection == LayoutDirection.Ltr) {
      anchorRect.left to anchorRect.right
    } else {
      anchorRect.right to anchorRect.left
    }

  private fun effectiveAnchorAlignment(
    placement: CalloutPlacement,
    anchorAlignment: CalloutAnchorAlignment,
  ): CalloutAnchorAlignment = when (placement) {
    CalloutPlacement.TopStart, CalloutPlacement.BottomStart -> CalloutAnchorAlignment.Start
    CalloutPlacement.TopEnd, CalloutPlacement.BottomEnd -> CalloutAnchorAlignment.End
    CalloutPlacement.StartTop, CalloutPlacement.EndTop -> CalloutAnchorAlignment.Start
    CalloutPlacement.StartBottom, CalloutPlacement.EndBottom -> CalloutAnchorAlignment.End
    else -> anchorAlignment
  }

  private fun availableSpace(
    anchorRect: Rect,
    placement: CalloutPlacement,
    layoutBounds: Rect,
    anchorSpacing: Float,
  ): Float = when (placement) {
    CalloutPlacement.Top, CalloutPlacement.TopStart, CalloutPlacement.TopEnd ->
      max(0f, anchorRect.top - layoutBounds.top - anchorSpacing)
    CalloutPlacement.Bottom, CalloutPlacement.BottomStart, CalloutPlacement.BottomEnd ->
      max(0f, layoutBounds.bottom - anchorRect.bottom - anchorSpacing)
    CalloutPlacement.Start, CalloutPlacement.StartTop, CalloutPlacement.StartBottom ->
      max(0f, anchorRect.left - layoutBounds.left - anchorSpacing)
    CalloutPlacement.End, CalloutPlacement.EndTop, CalloutPlacement.EndBottom ->
      max(0f, layoutBounds.right - anchorRect.right - anchorSpacing)
    CalloutPlacement.Automatic -> 0f
  }

  private fun requiredSpace(placement: CalloutPlacement, bubbleSize: Size): Float = when (placement) {
    CalloutPlacement.Top, CalloutPlacement.TopStart, CalloutPlacement.TopEnd,
    CalloutPlacement.Bottom, CalloutPlacement.BottomStart, CalloutPlacement.BottomEnd,
    -> bubbleSize.height
    CalloutPlacement.Start, CalloutPlacement.StartTop, CalloutPlacement.StartBottom,
    CalloutPlacement.End, CalloutPlacement.EndTop, CalloutPlacement.EndBottom,
    -> bubbleSize.width
    CalloutPlacement.Automatic -> 0f
  }

  private fun alignedOrigin(
    anchorStart: Float,
    anchorEnd: Float,
    bubbleLength: Float,
    alignment: CalloutAnchorAlignment,
  ): Float = when (alignment) {
    CalloutAnchorAlignment.Center -> ((anchorStart + anchorEnd) * 0.5f) - bubbleLength * 0.5f
    CalloutAnchorAlignment.Start -> min(anchorStart, anchorEnd)
    CalloutAnchorAlignment.End -> max(anchorStart, anchorEnd) - bubbleLength
  }

  private fun clampFrame(frame: Rect, layoutBounds: Rect): Rect {
    if (layoutBounds.width <= 0f || layoutBounds.height <= 0f) return frame
    var left = frame.left
    var top = frame.top
    if (frame.width <= layoutBounds.width) {
      if (left < layoutBounds.left) left = layoutBounds.left
      if (left + frame.width > layoutBounds.right) left = layoutBounds.right - frame.width
    } else {
      left = layoutBounds.left
    }
    if (frame.height <= layoutBounds.height) {
      if (top < layoutBounds.top) top = layoutBounds.top
      if (top + frame.height > layoutBounds.bottom) top = layoutBounds.bottom - frame.height
    } else {
      top = layoutBounds.top
    }
    return Rect(offset = Offset(left, top), size = Size(frame.width, frame.height))
  }

  private fun overflowPenalty(frame: Rect, layoutBounds: Rect): Float {
    var penalty = 0f
    if (frame.left < layoutBounds.left) penalty += layoutBounds.left - frame.left
    if (frame.right > layoutBounds.right) penalty += frame.right - layoutBounds.right
    if (frame.top < layoutBounds.top) penalty += layoutBounds.top - frame.top
    if (frame.bottom > layoutBounds.bottom) penalty += frame.bottom - layoutBounds.bottom
    return penalty
  }
}
