package com.fk.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Read-only workflow / order status capsule.
 *
 * Conceptually aligned with iOS `FKStatusPill`. Distinct from [FkTag] (marketing metadata).
 */
@Composable
fun FkStatusPill(
  title: String,
  modifier: Modifier = Modifier,
  style: StatusPillStyle = FkStatusSemantic.Neutral,
  showsDot: Boolean = false,
  size: StatusPillSize = StatusPillSize.S,
) {
  val color = fkStatusColor(style)
  val metrics = fkMetrics()
  Row(
    modifier = modifier
      .heightIn(min = size.height)
      .clip(metrics.shapeFull)
      .background(color.copy(alpha = 0.14f))
      .padding(horizontal = 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (showsDot) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(color),
      )
    }
    Text(
      text = title,
      style = when (size) {
        StatusPillSize.S -> fkTextStyle(FkTextStyle.Caption1)
        StatusPillSize.M -> fkTextStyle(FkTextStyle.Subheadline)
      }.copy(fontWeight = FontWeight.SemiBold),
      color = color,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
