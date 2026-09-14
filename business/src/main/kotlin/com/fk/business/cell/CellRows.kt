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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle
import com.fk.ui.widget.AvatarSize
import com.fk.ui.widget.FkAvatar
import com.fk.ui.widget.FkStatusPill
import com.fk.ui.widget.FkTag
import com.fk.ui.widget.PresenceState
import com.fk.ui.widget.TagVariant

/**
 * Person / contact row: avatar, name, subtitle, optional role tag and timestamp.
 *
 * Conceptually aligned with iOS `FKUserListCell` (Compose — not UITableViewCell).
 *
 * @param avatar Optional leading content that replaces the default [FkAvatar]
 *   (which already loads [UserListItem.avatarUrl] via Coil when present).
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
        FkAvatar(
          displayName = item.displayName,
          imageUrl = item.avatarUrl,
          size = AvatarSize.M,
          verified = item.isVerified,
          presence = item.presence?.toPresenceState(),
          onClick = onTapAvatar,
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
        if (item.isVerified && avatar != null) {
          Text(
            text = "✓",
            style = fkTextStyle(FkTextStyle.Caption2),
            color = fkColor(FkColorRole.Primary),
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
        FkTag(title = tag.title, variant = tag.style.toTagVariant())
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
    FkAvatar(
      displayName = item.title,
      size = AvatarSize.S,
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
      FkTag(title = titleText, variant = TagVariant.Neutral)
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
      FkStatusPill(title = item.statusPill.title, style = item.statusPill.style.toStatusSemantic(), showsDot = item.statusPill.showsDot)
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


private fun CellChromeStyle.toTagVariant(): TagVariant =
  when (this) {
    CellChromeStyle.Neutral -> TagVariant.Neutral
    CellChromeStyle.Success -> TagVariant.Success
    CellChromeStyle.Warning -> TagVariant.Warning
    CellChromeStyle.Error -> TagVariant.Error
    CellChromeStyle.Info -> TagVariant.Brand
  }

private fun CellChromeStyle.toStatusSemantic(): FkStatusSemantic =
  when (this) {
    CellChromeStyle.Neutral -> FkStatusSemantic.Neutral
    CellChromeStyle.Success -> FkStatusSemantic.Success
    CellChromeStyle.Warning -> FkStatusSemantic.Warning
    CellChromeStyle.Error -> FkStatusSemantic.Error
    CellChromeStyle.Info -> FkStatusSemantic.Info
  }

private fun CellPresence.toPresenceState(): PresenceState =
  when (this) {
    CellPresence.Online -> PresenceState.Online
    CellPresence.Away -> PresenceState.Away
    CellPresence.Busy -> PresenceState.Busy
    CellPresence.Offline -> PresenceState.Offline
  }
