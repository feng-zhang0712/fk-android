package com.fk.business.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fk.business.R
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Like / Reply / More strip shared by Standard action bar and Compact meta rail.
 *
 * Conceptually aligned with iOS `FKCommentActionBarView` /
 * `FKCommentCompactActionRailView`.
 */
@Composable
internal fun CommentActionControls(
  item: CommentItem,
  configuration: CommentConfiguration,
  onLike: () -> Unit,
  onReply: () -> Unit,
  onMore: () -> Unit,
  modifier: Modifier = Modifier,
  spacingDp: Int = 16,
  iconTitleSpacingDp: Int = 6,
  minimumHeightDp: Int = 30,
  showsReplyTitle: Boolean = true,
) {
  val strings = configuration.strings
  val tint = fkColor(FkColorRole.OnSurfaceSecondary)
  val likedTint = fkStatusColor(FkStatusSemantic.Error)

  Row(
    modifier = modifier.heightIn(min = minimumHeightDp.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(spacingDp.dp),
  ) {
    if (configuration.showsLikeAction) {
      Row(
        modifier = Modifier.clickable(role = Role.Button, onClick = onLike),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(iconTitleSpacingDp.dp),
      ) {
        Icon(
          painter = painterResource(
            if (item.isLiked) R.drawable.fk_ic_heart_fill else R.drawable.fk_ic_heart_outline,
          ),
          contentDescription = if (item.isLiked) strings.unlike else strings.like,
          modifier = Modifier.size(18.dp),
          tint = if (item.isLiked) likedTint else tint,
        )
        if (item.likeCount > 0 || item.likeCountText != null) {
          Text(
            text = item.formattedLikeCount(),
            style = fkTextStyle(FkTextStyle.Footnote),
            color = if (item.isLiked) likedTint else tint,
          )
        }
      }
    }
    if (configuration.showsReplyAction) {
      Row(
        modifier = Modifier.clickable(role = Role.Button, onClick = onReply),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(iconTitleSpacingDp.dp),
      ) {
        Icon(
          painter = painterResource(R.drawable.fk_ic_chat),
          contentDescription = strings.reply,
          modifier = Modifier.size(18.dp),
          tint = tint,
        )
        if (showsReplyTitle) {
          Text(
            text = strings.reply,
            style = fkTextStyle(FkTextStyle.Footnote),
            color = tint,
          )
        }
      }
    }
    if (configuration.showsMoreAction) {
      Icon(
        painter = painterResource(R.drawable.fk_ic_more_horizontal),
        contentDescription = strings.more,
        modifier = Modifier
          .size(18.dp)
          .clickable(role = Role.Button, onClick = onMore),
        tint = tint,
      )
    }
  }
}
