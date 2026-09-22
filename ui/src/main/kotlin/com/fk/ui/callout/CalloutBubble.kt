package com.fk.ui.callout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.fk.ui.callout.CalloutLayoutEngine.BeakEdge
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.fkColor
import kotlin.math.sqrt

@Composable
internal fun resolvedBubbleColors(appearance: CalloutAppearance): Triple<Color, Color, Color> {
  val surface = appearance.backgroundColor ?: when (appearance.style) {
    CalloutAppearance.Style.Light -> fkColor(FkColorRole.SurfaceElevated)
    CalloutAppearance.Style.Dark -> Color(0xFF141414)
  }
  val onSurface = appearance.textColor ?: when (appearance.style) {
    CalloutAppearance.Style.Light -> fkColor(FkColorRole.OnSurface)
    CalloutAppearance.Style.Dark -> Color(0xFFFAFAFA)
  }
  val secondary = appearance.secondaryTextColor ?: when (appearance.style) {
    CalloutAppearance.Style.Light -> fkColor(FkColorRole.OnSurfaceSecondary)
    CalloutAppearance.Style.Dark -> Color(0xFFD0D0D0)
  }
  return Triple(surface, onSurface, secondary)
}

/**
 * Renders bubble chrome (rounded body + beak) and hosts [content] inside the body padding.
 */
@Composable
internal fun CalloutBubble(
  placement: CalloutPlacement,
  beakCenterAlongEdge: Float,
  appearance: CalloutAppearance,
  contentPadding: PaddingValuesDp,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val density = LocalDensity.current
  val beakHeightPx = with(density) { appearance.beakHeight.toPx() }
  val beakWidthPx = with(density) { appearance.beakWidth.toPx() }
  val cornerRadiusPx = with(density) { appearance.cornerRadius.toPx() }
  val edge = with(CalloutLayoutEngine) { placement.beakEdge() }
  val (fill, _, _) = resolvedBubbleColors(appearance)
  val borderColor = appearance.borderColor ?: if (appearance.borderWidth > 0.dp) {
    fkColor(FkColorRole.Outline).copy(alpha = 0.45f)
  } else {
    Color.Transparent
  }
  val bodyPadding = when (edge) {
    BeakEdge.Top -> PaddingValues(
      top = appearance.beakHeight + contentPadding.top,
      start = contentPadding.start,
      end = contentPadding.end,
      bottom = contentPadding.bottom,
    )
    BeakEdge.Bottom -> PaddingValues(
      top = contentPadding.top,
      start = contentPadding.start,
      end = contentPadding.end,
      bottom = appearance.beakHeight + contentPadding.bottom,
    )
    BeakEdge.Start -> PaddingValues(
      top = contentPadding.top,
      start = appearance.beakHeight + contentPadding.start,
      end = contentPadding.end,
      bottom = contentPadding.bottom,
    )
    BeakEdge.End -> PaddingValues(
      top = contentPadding.top,
      start = contentPadding.start,
      end = appearance.beakHeight + contentPadding.end,
      bottom = contentPadding.bottom,
    )
  }

  val shape = remember(edge, beakCenterAlongEdge, beakWidthPx, beakHeightPx, cornerRadiusPx, appearance.beakStyle) {
    GenericShape { size, _ ->
      addPath(
        buildBubblePath(
          size = size,
          edge = edge,
          beakCenter = beakCenterAlongEdge,
          beakWidth = beakWidthPx,
          beakHeight = beakHeightPx,
          cornerRadius = cornerRadiusPx,
          beakStyle = appearance.beakStyle,
        ),
      )
    }
  }

  val elevation = if (appearance.showsShadow) appearance.shadowRadius else 0.dp
  Box(
    modifier = modifier
      .then(
        if (appearance.showsShadow) {
          Modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = appearance.shadowOpacity),
            spotColor = Color.Black.copy(alpha = appearance.shadowOpacity),
          )
        } else {
          Modifier
        },
      )
      .clip(shape)
      .background(fill, shape)
      .then(
        if (appearance.borderWidth > 0.dp && borderColor != Color.Transparent) {
          Modifier.border(appearance.borderWidth, borderColor, shape)
        } else {
          Modifier
        },
      )
      .padding(bodyPadding),
  ) {
    content()
  }
}

internal fun buildBubblePath(
  size: Size,
  edge: BeakEdge,
  beakCenter: Float,
  beakWidth: Float,
  beakHeight: Float,
  cornerRadius: Float,
  beakStyle: CalloutBeakStyle,
): Path {
  val path = Path()
  val body = when (edge) {
    BeakEdge.Top -> RoundRect(
      rect = Rect(0f, beakHeight, size.width, size.height),
      cornerRadius = CornerRadius(cornerRadius, cornerRadius),
    )
    BeakEdge.Bottom -> RoundRect(
      rect = Rect(0f, 0f, size.width, size.height - beakHeight),
      cornerRadius = CornerRadius(cornerRadius, cornerRadius),
    )
    BeakEdge.Start -> RoundRect(
      rect = Rect(beakHeight, 0f, size.width, size.height),
      cornerRadius = CornerRadius(cornerRadius, cornerRadius),
    )
    BeakEdge.End -> RoundRect(
      rect = Rect(0f, 0f, size.width - beakHeight, size.height),
      cornerRadius = CornerRadius(cornerRadius, cornerRadius),
    )
  }
  path.addRoundRect(body)

  val half = beakWidth * 0.5f
  val tipOffset = when (beakStyle) {
    is CalloutBeakStyle.RightAngle -> {
      val t = beakStyle.apexAlongBase.coerceIn(0f, 1f)
      -half + beakWidth * t
    }
    else -> 0f
  }
  val resolvedHeight = when (beakStyle) {
    CalloutBeakStyle.Equilateral -> beakWidth * sqrt(3f) / 2f
    else -> beakHeight
  }

  if (beakStyle is CalloutBeakStyle.Polygon && beakStyle.vertices.size >= 3) {
    val poly = Path()
    beakStyle.vertices.forEachIndexed { index, point ->
      val nx = point.x.coerceIn(0f, 1f)
      val ny = point.y.coerceIn(0f, 1f)
      val mapped = when (edge) {
        BeakEdge.Top -> Offset(
          x = (beakCenter - half) + beakWidth * nx,
          y = resolvedHeight * (1f - ny),
        )
        BeakEdge.Bottom -> Offset(
          x = (beakCenter - half) + beakWidth * nx,
          y = (size.height - resolvedHeight) + resolvedHeight * ny,
        )
        BeakEdge.Start -> Offset(
          x = resolvedHeight * (1f - ny),
          y = (beakCenter - half) + beakWidth * nx,
        )
        BeakEdge.End -> Offset(
          x = (size.width - resolvedHeight) + resolvedHeight * ny,
          y = (beakCenter - half) + beakWidth * nx,
        )
      }
      if (index == 0) poly.moveTo(mapped.x, mapped.y) else poly.lineTo(mapped.x, mapped.y)
    }
    poly.close()
    path.addPath(poly)
    return path
  }

  val beak = Path()
  when (edge) {
    BeakEdge.Top -> {
      val cx = beakCenter.coerceIn(half, size.width - half)
      beak.moveTo(cx - half, beakHeight)
      beak.lineTo(cx + tipOffset, 0f)
      beak.lineTo(cx + half, beakHeight)
      beak.close()
    }
    BeakEdge.Bottom -> {
      val cx = beakCenter.coerceIn(half, size.width - half)
      val baseY = size.height - beakHeight
      beak.moveTo(cx - half, baseY)
      beak.lineTo(cx + tipOffset, size.height)
      beak.lineTo(cx + half, baseY)
      beak.close()
    }
    BeakEdge.Start -> {
      val cy = beakCenter.coerceIn(half, size.height - half)
      beak.moveTo(beakHeight, cy - half)
      beak.lineTo(0f, cy + tipOffset)
      beak.lineTo(beakHeight, cy + half)
      beak.close()
    }
    BeakEdge.End -> {
      val cy = beakCenter.coerceIn(half, size.height - half)
      val baseX = size.width - beakHeight
      beak.moveTo(baseX, cy - half)
      beak.lineTo(size.width, cy + tipOffset)
      beak.lineTo(baseX, cy + half)
      beak.close()
    }
  }
  path.addPath(beak)
  return path
}

@Composable
internal fun CalloutBackdrop(
  anchorRect: Rect,
  style: CalloutBackdropStyle,
  modifier: Modifier = Modifier,
) {
  if (!style.showsDimmedBackdrop) return
  val dim = style.dimColor ?: fkColor(FkColorRole.Scrim).copy(alpha = 0.45f)
  val radiusPx = with(LocalDensity.current) { style.spotlightCornerRadius.toPx() }
  Canvas(modifier = modifier.fillMaxSize()) {
    val full = Path().apply { addRect(Rect(Offset.Zero, size)) }
    if (style.spotlightsAnchor && anchorRect.width > 0f && anchorRect.height > 0f) {
      val hole = Path().apply {
        addRoundRect(
          RoundRect(
            rect = anchorRect,
            cornerRadius = CornerRadius(radiusPx, radiusPx),
          ),
        )
      }
      val combined = Path().apply {
        op(full, hole, PathOperation.Difference)
      }
      drawPath(combined, dim)
    } else {
      drawRect(dim)
    }
  }
}
