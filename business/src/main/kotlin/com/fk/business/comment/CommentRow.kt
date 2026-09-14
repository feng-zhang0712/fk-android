package com.fk.business.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fk.business.R
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkTextStyle
import com.fk.ui.widget.AvatarSize
import com.fk.ui.widget.FkAvatar

/**
 * Comment row that dispatches to Standard or Compact layout from [CommentConfiguration.layoutPreset].
 *
 * Conceptually aligned with iOS `FKCommentRowCell` / `FKCommentCompactRowCell`.
 */
@Composable
fun CommentRow(
  item: CommentItem,
  configuration: CommentConfiguration,
  modifier: Modifier = Modifier,
  isExpanding: Boolean = false,
  onLike: () -> Unit = {},
  onReply: () -> Unit = {},
  onMore: () -> Unit = {},
  onExpandReplies: () -> Unit = {},
  onTapAvatar: () -> Unit = {},
  onTapAuthor: () -> Unit = {},
  onTapBody: () -> Unit = {},
) {
  when (configuration.layoutPreset) {
    CommentLayoutPreset.Standard -> CommentStandardRow(
      item = item,
      configuration = configuration,
      modifier = modifier,
      isExpanding = isExpanding,
      onLike = onLike,
      onReply = onReply,
      onMore = onMore,
      onExpandReplies = onExpandReplies,
      onTapAvatar = onTapAvatar,
      onTapAuthor = onTapAuthor,
      onTapBody = onTapBody,
    )
    CommentLayoutPreset.Compact -> CommentCompactRow(
      item = item,
      configuration = configuration,
      modifier = modifier,
      isExpanding = isExpanding,
      onLike = onLike,
      onReply = onReply,
      onMore = onMore,
      onExpandReplies = onExpandReplies,
      onTapAvatar = onTapAvatar,
      onTapAuthor = onTapAuthor,
      onTapBody = onTapBody,
    )
  }
}

@Composable
private fun CommentStandardRow(
  item: CommentItem,
  configuration: CommentConfiguration,
  modifier: Modifier,
  isExpanding: Boolean,
  onLike: () -> Unit,
  onReply: () -> Unit,
  onMore: () -> Unit,
  onExpandReplies: () -> Unit,
  onTapAvatar: () -> Unit,
  onTapAuthor: () -> Unit,
  onTapBody: () -> Unit,
) {
  val visualDepth = item.depth.coerceAtMost(configuration.maxVisualDepth)
  val indent = (visualDepth * configuration.indentWidthDp).dp
  val strings = configuration.strings
  val threadColor = fkColor(FkColorRole.Outline).copy(alpha = 0.45f)
  val showThread = visualDepth > 0 && configuration.showsThreadLine

  // iOS Standard: contentInsets 16 / 10, sectionSpacing 4 (+ action bar min 30).
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp)
      .padding(start = indent)
      .then(
        if (showThread) {
          Modifier.drawBehind {
            val x = -indent.toPx() + 8.dp.toPx()
            drawLine(
              color = threadColor,
              start = Offset(x, 0f),
              end = Offset(x, size.height),
              strokeWidth = 2.dp.toPx(),
            )
          }
        } else {
          Modifier
        },
      ),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    FkAvatar(
      displayName = item.authorName,
      imageUrl = item.avatarUrl,
      size = AvatarSize.S,
      verified = item.isVerified,
      onClick = onTapAvatar,
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = item.authorName,
          style = fkTextStyle(FkTextStyle.Subheadline).copy(fontWeight = FontWeight.SemiBold),
          color = fkColor(FkColorRole.OnSurface),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f, fill = false)
            .clickable(onClick = onTapAuthor),
        )
        item.timestampText?.takeIf { it.isNotBlank() }?.let { time ->
          Text(
            text = time,
            style = fkTextStyle(FkTextStyle.Caption2),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
            maxLines = 1,
          )
        }
      }
      item.replyTo?.let { target ->
        Text(
          text = strings.replyToText(target.displayName),
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      CommentExpandableBody(
        text = item.body,
        configuration = configuration,
        onTapBody = onTapBody,
      )
      CommentActionControls(
        item = item,
        configuration = configuration,
        onLike = onLike,
        onReply = onReply,
        onMore = onMore,
        spacingDp = 16,
        iconTitleSpacingDp = 6,
        minimumHeightDp = 30,
        showsReplyTitle = true,
      )
      CommentExpandRepliesRow(
        item = item,
        configuration = configuration,
        isExpanding = isExpanding,
        compact = false,
        onExpandReplies = onExpandReplies,
      )
    }
  }
}

@Composable
private fun CommentCompactRow(
  item: CommentItem,
  configuration: CommentConfiguration,
  modifier: Modifier,
  isExpanding: Boolean,
  onLike: () -> Unit,
  onReply: () -> Unit,
  onMore: () -> Unit,
  onExpandReplies: () -> Unit,
  onTapAvatar: () -> Unit,
  onTapAuthor: () -> Unit,
  onTapBody: () -> Unit,
) {
  val visualDepth = item.depth.coerceAtMost(configuration.maxVisualDepth)
  val indent = (visualDepth * configuration.indentWidthDp).dp
  val strings = configuration.strings
  val showReplyTo = item.replyTo != null && visualDepth > 0

  // iOS Compact: contentInsets 12 / 10, sectionSpacing 4, action rail min 24.
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .padding(start = indent),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    FkAvatar(
      displayName = item.authorName,
      imageUrl = item.avatarUrl,
      size = AvatarSize.S,
      verified = item.isVerified,
      onClick = onTapAvatar,
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = item.authorName,
          style = fkTextStyle(FkTextStyle.Subheadline),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.clickable(onClick = onTapAuthor),
        )
        if (showReplyTo) {
          Icon(
            painter = painterResource(R.drawable.fk_ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(10.dp),
            tint = fkColor(FkColorRole.OnSurfaceSecondary),
          )
          Text(
            text = item.replyTo!!.displayName,
            style = fkTextStyle(FkTextStyle.Subheadline),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
        }
        Box(modifier = Modifier.weight(1f))
        item.timestampText?.takeIf { it.isNotBlank() }?.let { time ->
          Text(
            text = time,
            style = fkTextStyle(FkTextStyle.Caption2),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
            maxLines = 1,
          )
        }
      }
      CommentExpandableBody(
        text = item.body,
        configuration = configuration,
        onTapBody = onTapBody,
      )
      CommentActionControls(
        item = item,
        configuration = configuration,
        onLike = onLike,
        onReply = onReply,
        onMore = onMore,
        spacingDp = 12,
        iconTitleSpacingDp = 4,
        minimumHeightDp = 24,
        showsReplyTitle = true,
      )
      CommentExpandRepliesRow(
        item = item,
        configuration = configuration,
        isExpanding = isExpanding,
        compact = true,
        onExpandReplies = onExpandReplies,
      )
    }
  }
}

@Composable
private fun CommentExpandableBody(
  text: String,
  configuration: CommentConfiguration,
  onTapBody: () -> Unit,
) {
  if (text.isEmpty()) return
  val maxLines = configuration.bodyMaxLines
  val expandable = configuration.usesExpandableBody && maxLines != null
  var expanded by remember(text) { mutableStateOf(false) }
  var overflowed by remember(text) { mutableStateOf(false) }
  val strings = configuration.strings

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = text,
      style = fkTextStyle(FkTextStyle.Body),
      color = fkColor(FkColorRole.OnSurface),
      maxLines = if (expandable && !expanded) maxLines!! else Int.MAX_VALUE,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.clickable(onClick = onTapBody),
      onTextLayout = { result ->
        if (expandable && !expanded) {
          overflowed = result.hasVisualOverflow
        }
      },
    )
    if (expandable && (expanded || overflowed)) {
      Text(
        text = if (expanded) strings.collapseBody else strings.expandBody,
        style = fkTextStyle(FkTextStyle.Footnote),
        color = fkColor(FkColorRole.Primary),
        modifier = Modifier.clickable(role = Role.Button) { expanded = !expanded },
      )
    }
  }
}

@Composable
private fun CommentExpandRepliesRow(
  item: CommentItem,
  configuration: CommentConfiguration,
  isExpanding: Boolean,
  compact: Boolean,
  onExpandReplies: () -> Unit,
) {
  if (item.replyCount < configuration.expandAffordanceMinimumReplyCount) return
  val strings = configuration.strings
  val title = when {
    item.areRepliesExpanded -> strings.hideReplies
    item.replyCount > 0 -> strings.viewRepliesText(item.replyCount)
    else -> strings.expandMore
  }
  val chevron = if (item.areRepliesExpanded) {
    R.drawable.fk_ic_chevron_up
  } else {
    R.drawable.fk_ic_chevron_down
  }

  Row(
    modifier = Modifier
      .clickable(enabled = !isExpanding, role = Role.Button, onClick = onExpandReplies),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (isExpanding) {
      CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        strokeWidth = 2.dp,
      )
    }
    Text(
      text = title,
      style = fkTextStyle(FkTextStyle.Footnote),
      color = fkColor(FkColorRole.Primary),
    )
    if (compact) {
      Icon(
        painter = painterResource(chevron),
        contentDescription = null,
        modifier = Modifier.size(10.dp),
        tint = fkColor(FkColorRole.Primary),
      )
    }
  }
}
