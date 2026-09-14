package com.fk.business.cell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Person / contact row: avatar, name, subtitle, optional role tag and timestamp.
 *
 * Conceptually aligned with iOS `FKUserListCell` (Compose — not UITableViewCell).
 *
 * @param avatar Optional leading content (e.g. Coil `AsyncImage` for [UserListItem.avatarUrl]).
 *   When null, a letter avatar is shown.
 */
@Composable
fun UserListRow(
  item: UserListItem,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
  onTapAvatar: () -> Unit = {},
  avatar: (@Composable () -> Unit)? = null,
) {
  val metrics = fkMetrics()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = metrics.spacingM, vertical = metrics.spacingS),
    horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box {
      if (avatar != null) {
        Box(modifier = Modifier.clickable(onClick = onTapAvatar)) {
          avatar()
        }
      } else {
        CellLetterAvatar(
          label = item.displayName,
          onClick = onTapAvatar,
        )
      }
      item.presence?.let { presence ->
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 1.dp, y = 1.dp)
            .size(10.dp)
            .clip(CircleShape)
            .background(presenceColor(presence)),
        )
      }
      if (item.unreadCount > 0) {
        Text(
          text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
          style = fkTextStyle(FkTextStyle.Caption2),
          color = fkColor(FkColorRole.OnDestructive),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 4.dp, y = (-4).dp)
            .clip(CircleShape)
            .background(fkColor(FkColorRole.Destructive))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        )
      }
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXxs),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      ) {
        Text(
          text = item.displayName,
          style = fkTextStyle(FkTextStyle.Subheadline).copy(fontWeight = FontWeight.SemiBold),
          color = fkColor(FkColorRole.OnSurface),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (item.isVerified) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(fkStatusColor(FkStatusSemantic.Info)),
          )
        }
      }
      item.subtitle?.let { subtitle ->
        Text(
          text = subtitle,
          style = fkTextStyle(FkTextStyle.Footnote),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXxs),
    ) {
      item.timestampText?.let { time ->
        Text(
          text = time,
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
      }
      item.roleTag?.let { tag ->
        CellTagChip(tag = tag)
      }
    }
  }
}

/**
 * Inbox notification row with unread affordance.
 *
 * Conceptually aligned with iOS `FKNotificationListCell`.
 */
@Composable
fun NotificationListRow(
  item: NotificationListItem,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val metrics = fkMetrics()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = metrics.spacingM, vertical = metrics.spacingS),
    horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
    verticalAlignment = Alignment.Top,
  ) {
    CellLetterAvatar(
      label = item.title,
      size = 36.dp,
      onClick = onClick,
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXxs),
    ) {
      Text(
        text = item.title,
        style = fkTextStyle(FkTextStyle.Subheadline).copy(
          fontWeight = if (item.isUnread) FontWeight.SemiBold else FontWeight.Normal,
        ),
        color = fkColor(FkColorRole.OnSurface),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      item.summary?.let { summary ->
        Text(
          text = summary,
          style = fkTextStyle(FkTextStyle.Footnote),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      item.timestampText?.let { time ->
        Text(
          text = time,
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
      }
    }
    if (item.isUnread) {
      Box(
        modifier = Modifier
          .padding(top = 6.dp)
          .size(8.dp)
          .clip(CircleShape)
          .background(fkColor(FkColorRole.Primary)),
      )
    }
  }
}

/**
 * Search hit row with optional query highlight segments.
 *
 * Conceptually aligned with iOS `FKSearchResultCell`.
 */
@Composable
fun SearchResultRow(
  item: SearchResultItem,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val metrics = fkMetrics()
  val highlightColor = fkColor(FkColorRole.Primary)
  val normalColor = fkColor(FkColorRole.OnSurface)
  val title = buildAnnotatedString {
    item.titleSegments.forEach { segment ->
      withStyle(
        SpanStyle(
          color = if (segment.isHighlighted) highlightColor else normalColor,
          fontWeight = if (segment.isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
        ),
      ) {
        append(segment.text)
      }
    }
  }
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = metrics.spacingM, vertical = metrics.spacingS),
    horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXxs),
    ) {
      Text(
        text = title,
        style = fkTextStyle(FkTextStyle.Body),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      item.breadcrumbText?.let { crumb ->
        Text(
          text = crumb,
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    item.categoryTagTitle?.let { titleText ->
      CellTagChip(tag = CellTag(title = titleText))
    }
  }
}

/**
 * Order / ticket row with status pill and optional copy chip.
 *
 * Conceptually aligned with iOS `FKOrderListCell`.
 */
@Composable
fun OrderListRow(
  item: OrderListItem,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
  onCopyOrderNumber: (String) -> Unit = {},
) {
  val metrics = fkMetrics()
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = metrics.spacingM, vertical = metrics.spacingS),
    verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = item.title,
        style = fkTextStyle(FkTextStyle.Subheadline).copy(fontWeight = FontWeight.SemiBold),
        color = fkColor(FkColorRole.OnSurface),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      CellStatusPillChip(pill = item.statusPill)
    }
    item.subtitle?.let { subtitle ->
      Text(
        text = subtitle,
        style = fkTextStyle(FkTextStyle.Footnote),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
    ) {
      Text(
        text = item.displayOrderNumber,
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      if (item.showsCopyChip) {
        Text(
          text = "Copy",
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.Primary),
          modifier = Modifier
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = false),
              onClick = { onCopyOrderNumber(item.copyableOrderNumber) },
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        )
      }
    }
  }
}

/**
 * Settings-style row with a trailing switch.
 *
 * Conceptually aligned with iOS `FKInlineToggleCell` (Compose callback, not handler registry).
 * The title / subtitle area is also toggleable when [InlineToggleItem.isEnabled].
 */
@Composable
fun InlineToggleRow(
  item: InlineToggleItem,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val metrics = fkMetrics()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .toggleable(
        value = item.isOn,
        enabled = item.isEnabled,
        role = Role.Switch,
        onValueChange = onCheckedChange,
      )
      .padding(horizontal = metrics.spacingM, vertical = metrics.spacingS),
    horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingXxs),
    ) {
      Text(
        text = item.title,
        style = fkTextStyle(FkTextStyle.Body),
        color = if (item.isEnabled) {
          fkColor(FkColorRole.OnSurface)
        } else {
          fkColor(FkColorRole.OnSurfaceSecondary)
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      item.subtitle?.let { subtitle ->
        Text(
          text = subtitle,
          style = fkTextStyle(FkTextStyle.Footnote),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Switch(
      checked = item.isOn,
      onCheckedChange = null,
      enabled = item.isEnabled,
    )
  }
}

@Composable
private fun CellLetterAvatar(
  label: String,
  onClick: () -> Unit,
  size: Dp = 40.dp,
) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(fkColor(FkColorRole.Secondary))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
      style = fkTextStyle(FkTextStyle.Subheadline),
      color = fkColor(FkColorRole.OnSecondary),
    )
  }
}

@Composable
private fun CellTagChip(tag: CellTag) {
  val color = chromeColor(tag.style)
  Text(
    text = tag.title,
    style = fkTextStyle(FkTextStyle.Caption1),
    color = color,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier
      .clip(fkMetrics().shapeSmall)
      .background(color.copy(alpha = 0.12f))
      .padding(horizontal = 8.dp, vertical = 2.dp),
  )
}

@Composable
private fun CellStatusPillChip(pill: CellStatusPill) {
  val color = chromeColor(pill.style)
  Row(
    modifier = Modifier
      .clip(fkMetrics().shapeFull)
      .background(color.copy(alpha = 0.12f))
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    if (pill.showsDot) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(color),
      )
    }
    Text(
      text = pill.title,
      style = fkTextStyle(FkTextStyle.Caption1),
      color = color,
      maxLines = 1,
    )
  }
}

@Composable
private fun chromeColor(style: CellChromeStyle): Color =
  when (style) {
    CellChromeStyle.Neutral -> fkStatusColor(FkStatusSemantic.Neutral)
    CellChromeStyle.Success -> fkStatusColor(FkStatusSemantic.Success)
    CellChromeStyle.Warning -> fkStatusColor(FkStatusSemantic.Warning)
    CellChromeStyle.Error -> fkStatusColor(FkStatusSemantic.Error)
    CellChromeStyle.Info -> fkStatusColor(FkStatusSemantic.Info)
  }

@Composable
private fun presenceColor(presence: CellPresence): Color =
  when (presence) {
    CellPresence.Online -> fkStatusColor(FkStatusSemantic.Success)
    CellPresence.Away -> fkStatusColor(FkStatusSemantic.Warning)
    CellPresence.Busy -> fkStatusColor(FkStatusSemantic.Error)
    CellPresence.Offline -> fkStatusColor(FkStatusSemantic.Neutral)
  }
