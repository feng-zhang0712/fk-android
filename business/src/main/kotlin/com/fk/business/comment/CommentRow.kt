package com.fk.business.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Standard comment row: avatar, author, body, like / reply / more, expand replies.
 *
 * Conceptually aligned with iOS `FKCommentRowCell` (standard skeleton).
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
  val metrics = fkMetrics()
  val visualDepth = item.depth.coerceAtMost(configuration.maxVisualDepth)
  val indent = (visualDepth * configuration.indentWidthDp).dp
  val strings = configuration.strings

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = indent)
      .padding(horizontal = metrics.spacingM, vertical = metrics.spacingS),
    verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
      verticalAlignment = Alignment.Top,
    ) {
      Surface(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .clickable(onClick = onTapAvatar),
        color = fkColor(FkColorRole.Secondary),
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = item.authorName.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
            style = fkTextStyle(FkTextStyle.Footnote),
            color = fkColor(FkColorRole.OnSecondary),
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
            text = item.authorName,
            style = fkTextStyle(FkTextStyle.Subheadline).copy(fontWeight = FontWeight.SemiBold),
            color = fkColor(FkColorRole.OnSurface),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onTapAuthor),
          )
          if (item.isVerified) {
            Text(
              text = "✓",
              style = fkTextStyle(FkTextStyle.Caption1),
              color = fkStatusColor(FkStatusSemantic.Info),
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
        item.replyTo?.let { target ->
          Text(
            text = "↳ ${target.displayName}",
            style = fkTextStyle(FkTextStyle.Caption1),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
          )
        }
        Text(
          text = item.body,
          style = fkTextStyle(FkTextStyle.Body),
          color = fkColor(FkColorRole.OnSurface),
          modifier = Modifier.clickable(onClick = onTapBody),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(metrics.spacingXxs),
        ) {
          if (configuration.showsLikeAction) {
            TextButton(onClick = onLike) {
              val label = if (item.likeCount > 0) {
                "${strings.like} ${item.formattedLikeCount()}"
              } else {
                strings.like
              }
              Text(
                text = label,
                color = if (item.isLiked) {
                  fkStatusColor(FkStatusSemantic.Error)
                } else {
                  fkColor(FkColorRole.OnSurfaceSecondary)
                },
              )
            }
          }
          if (configuration.showsReplyAction) {
            TextButton(onClick = onReply) {
              Text(strings.reply, color = fkColor(FkColorRole.OnSurfaceSecondary))
            }
          }
          if (configuration.showsMoreAction) {
            TextButton(onClick = onMore) {
              Text(strings.more, color = fkColor(FkColorRole.OnSurfaceSecondary))
            }
          }
        }
        if (item.replyCount > 0) {
          TextButton(onClick = onExpandReplies, enabled = !isExpanding) {
            Text(
              text = if (item.areRepliesExpanded) {
                strings.hideReplies
              } else {
                strings.viewReplies.replace("%d", item.replyCount.toString())
              },
              color = fkColor(FkColorRole.Primary),
            )
          }
        }
      }
    }
  }
}
