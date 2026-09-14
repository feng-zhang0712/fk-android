package com.fk.ui.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkTextStyle

/**
 * Vertical event timeline for logistics, audit history, activity feeds, and similar rails.
 *
 * Conceptually aligned with iOS `FKTimeline` (Compose, lean).
 *
 * Pass [currentStepIndex] to derive completed/current/upcoming, or set each
 * [FlowStepItem.state] explicitly.
 */
@Composable
fun FkTimeline(
  items: List<FlowStepItem>,
  modifier: Modifier = Modifier,
  currentStepIndex: Int? = null,
  nodeSize: FlowNodeSize = FlowNodeSize.M,
  onStepClick: ((String) -> Unit)? = null,
) {
  val resolved = resolveFlowItems(items, currentStepIndex)
  if (resolved.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth()) {
    resolved.forEachIndexed { index, item ->
      key(item.id) {
        val interactive = item.canSelect(hasClickHandler = onStepClick != null)
        val isLast = index == resolved.lastIndex
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .semantics {
              contentDescription = timelineAccessibilityLabel(item)
              if (interactive) role = Role.Button
            }
            .then(
              if (interactive && onStepClick != null) {
                Modifier.clickable { onStepClick(item.id) }
              } else {
                Modifier
              },
            ),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .width(nodeSize.diameter)
              .fillMaxHeight(),
          ) {
            FlowNode(item = item, index = index, size = nodeSize)
            if (!isLast) {
              Box(
                modifier = Modifier
                  .padding(top = 4.dp)
                  .width(2.dp)
                  .weight(1f)
                  .background(connectorColor(item.state)),
              )
            }
          }
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(bottom = if (isLast) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top,
            ) {
              Text(
                text = item.title,
                style = fkTextStyle(FkTextStyle.Subheadline).copy(
                  fontWeight = if (item.state == FlowStepState.Current) {
                    FontWeight.SemiBold
                  } else {
                    FontWeight.Medium
                  },
                ),
                color = labelColor(item.state),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              item.timestampText?.let { time ->
                Text(
                  text = time,
                  style = fkTextStyle(FkTextStyle.Caption2),
                  color = fkColor(FkColorRole.OnSurfaceSecondary),
                  maxLines = 1,
                  modifier = Modifier.padding(start = 8.dp),
                )
              }
            }
            item.subtitle?.let { subtitle ->
              Text(
                text = subtitle,
                style = fkTextStyle(FkTextStyle.Footnote),
                color = fkColor(FkColorRole.OnSurfaceSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            item.caption?.let { caption ->
              Text(
                text = caption,
                style = fkTextStyle(FkTextStyle.Caption1),
                color = fkColor(FkColorRole.OnSurfaceSecondary),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

private fun timelineAccessibilityLabel(item: FlowStepItem): String = buildString {
  append(item.title)
  append(", ")
  append(item.state.name.lowercase())
  item.timestampText?.let {
    append(", ")
    append(it)
  }
  item.subtitle?.let {
    append(", ")
    append(it)
  }
  item.caption?.let {
    append(", ")
    append(it)
  }
}
