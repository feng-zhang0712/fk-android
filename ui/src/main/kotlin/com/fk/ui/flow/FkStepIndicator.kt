package com.fk.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Horizontal step progress for wizards, checkout headers, and multi-step forms.
 *
 * Conceptually aligned with iOS `FKStepIndicator` (Compose, lean).
 *
 * Pass [currentStepIndex] to derive completed/current/upcoming, or set each
 * [FlowStepItem.state] explicitly. Special states (error / skipped / disabled) win.
 */
@Composable
fun FkStepIndicator(
  items: List<FlowStepItem>,
  modifier: Modifier = Modifier,
  currentStepIndex: Int? = null,
  nodeSize: FlowNodeSize = FlowNodeSize.M,
  onStepClick: ((String) -> Unit)? = null,
) {
  val resolved = resolveFlowItems(items, currentStepIndex)
  if (resolved.isEmpty()) return

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
  ) {
    resolved.forEachIndexed { index, item ->
      key(item.id) {
        val interactive = item.canSelect(hasClickHandler = onStepClick != null)
        Column(
          modifier = Modifier
            .weight(1f)
            .semantics {
              contentDescription = stepAccessibilityLabel(
                index = index,
                count = resolved.size,
                item = item,
              )
              if (interactive) role = Role.Button
            }
            .then(
              if (interactive && onStepClick != null) {
                Modifier.clickable { onStepClick(item.id) }
              } else {
                Modifier
              },
            ),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (index > 0) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(2.dp)
                  .background(connectorColor(resolved[index - 1].state)),
              )
            } else {
              Box(modifier = Modifier.weight(1f))
            }
            FlowNode(item = item, index = index, size = nodeSize)
            if (index < resolved.lastIndex) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(2.dp)
                  .background(connectorColor(item.state)),
              )
            } else {
              Box(modifier = Modifier.weight(1f))
            }
          }
          Text(
            text = item.title,
            style = fkTextStyle(FkTextStyle.Caption1).copy(
              fontWeight = if (item.state == FlowStepState.Current) {
                FontWeight.SemiBold
              } else {
                FontWeight.Medium
              },
            ),
            color = labelColor(item.state),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
          )
        }
      }
    }
  }
}

@Composable
internal fun FlowNode(
  item: FlowStepItem,
  index: Int,
  size: FlowNodeSize,
) {
  val diameter = size.diameter
  val (fill, border, content) = nodeChrome(item.state)
  Box(
    modifier = Modifier
      .size(diameter)
      .clip(CircleShape)
      .background(fill)
      .border(1.5.dp, border, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = nodeGlyph(item.state, index),
      color = content,
      fontSize = (diameter.value * 0.42f).sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
internal fun nodeChrome(state: FlowStepState): Triple<Color, Color, Color> {
  val primary = fkColor(FkColorRole.Primary)
  val onPrimary = fkColor(FkColorRole.OnPrimary)
  val outline = fkColor(FkColorRole.Outline)
  val surface = fkColor(FkColorRole.Surface)
  val muted = fkColor(FkColorRole.OnSurfaceSecondary)
  val error = fkStatusColor(FkStatusSemantic.Error)
  return when (state) {
    FlowStepState.Completed -> Triple(primary, primary, onPrimary)
    FlowStepState.Current -> Triple(primary.copy(alpha = 0.12f), primary, primary)
    FlowStepState.Upcoming -> Triple(surface, outline, muted)
    FlowStepState.Error -> Triple(error.copy(alpha = 0.12f), error, error)
    FlowStepState.Skipped, FlowStepState.Disabled ->
      Triple(surface, outline.copy(alpha = 0.6f), muted.copy(alpha = 0.7f))
  }
}

@Composable
internal fun connectorColor(fromState: FlowStepState): Color =
  when (fromState) {
    FlowStepState.Completed -> fkColor(FkColorRole.Primary)
    FlowStepState.Current -> fkColor(FkColorRole.Primary).copy(alpha = 0.35f)
    FlowStepState.Error -> fkStatusColor(FkStatusSemantic.Error).copy(alpha = 0.45f)
    else -> fkColor(FkColorRole.Outline)
  }

@Composable
internal fun labelColor(state: FlowStepState): Color =
  when (state) {
    FlowStepState.Completed, FlowStepState.Current -> fkColor(FkColorRole.OnSurface)
    FlowStepState.Error -> fkStatusColor(FkStatusSemantic.Error)
    FlowStepState.Upcoming -> fkColor(FkColorRole.OnSurfaceSecondary)
    FlowStepState.Skipped, FlowStepState.Disabled ->
      fkColor(FkColorRole.OnSurfaceSecondary).copy(alpha = 0.7f)
  }

internal fun nodeGlyph(state: FlowStepState, index: Int): String =
  when (state) {
    FlowStepState.Completed -> "✓"
    FlowStepState.Error -> "!"
    FlowStepState.Skipped -> "–"
    else -> (index + 1).toString()
  }

internal fun stepAccessibilityLabel(index: Int, count: Int, item: FlowStepItem): String =
  "Step ${index + 1} of $count, ${item.title}, ${item.state.name.lowercase()}"
