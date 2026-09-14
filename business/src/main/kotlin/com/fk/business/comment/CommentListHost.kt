package com.fk.business.comment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fk.ui.empty.EmptyActionSet
import com.fk.ui.empty.EmptyConfiguration
import com.fk.ui.empty.EmptyContent
import com.fk.ui.empty.EmptyPhase
import com.fk.ui.empty.EmptyStateContent
import com.fk.ui.empty.EmptyType
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Comment list + composer host.
 *
 * Conceptually aligned with iOS `FKCommentListViewController` content surface.
 *
 * @param onMore Present a more menu for [CommentItem]; typically call
 *   [CommentListController.selectMore] when the user picks an action.
 */
@Composable
fun CommentListHost(
  controller: CommentListController,
  modifier: Modifier = Modifier,
  onMore: (CommentItem) -> Unit = {},
) {
  val configuration = controller.configuration
  val metrics = fkMetrics()
  var draft by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val shouldLoadMore by remember {
    derivedStateOf {
      val info = listState.layoutInfo
      val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
      last >= info.totalItemsCount - 3
    }
  }
  LaunchedEffect(shouldLoadMore, controller.hasMore, controller.isLoadingMore) {
    if (shouldLoadMore && controller.hasMore && !controller.isLoadingMore) {
      controller.loadMore()
    }
  }
  LaunchedEffect(controller.composerResetToken) {
    if (controller.composerResetToken > 0) {
      draft = ""
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
    ) {
      when {
        controller.isInitialLoading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        controller.loadError != null && controller.items.isEmpty() -> {
          EmptyStateContent(
            configuration = EmptyConfiguration(
              phase = EmptyPhase.Error,
              type = EmptyType.Error,
              content = EmptyContent(
                title = "Couldn't load comments",
                description = controller.loadError,
              ),
              actions = EmptyActionSet.primary(
                title = EmptyConfiguration.DefaultRetryTitle,
                id = "retry",
              ),
            ),
            onAction = { controller.loadInitial() },
            modifier = Modifier.align(Alignment.Center),
          )
        }
        controller.items.isEmpty() -> {
          EmptyStateContent(
            configuration = EmptyConfiguration.of(
              phase = EmptyPhase.Empty,
              title = configuration.strings.emptyTitle,
              description = configuration.strings.emptyDescription,
            ),
            modifier = Modifier.align(Alignment.Center),
          )
        }
        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
          ) {
            items(controller.items, key = { it.id }) { item ->
              CommentRow(
                item = item,
                configuration = configuration,
                isExpanding = controller.expandingParentId == item.id,
                onLike = { controller.toggleLike(item.id) },
                onReply = { controller.beginReply(item) },
                onMore = { onMore(item) },
                onExpandReplies = { controller.expandOrCollapseReplies(item.id) },
                onTapAvatar = { controller.notifyTapAvatar(item) },
                onTapAuthor = { controller.notifyTapAuthor(item) },
                onTapBody = {
                  if (configuration.beginsReplyOnRowTap) {
                    controller.beginReply(item)
                  } else {
                    controller.notifyTapComment(item)
                  }
                },
              )
              HorizontalDivider(color = fkColor(FkColorRole.Outline).copy(alpha = 0.35f))
            }
            if (controller.isLoadingMore) {
              item(key = "comment_loading_more") {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(metrics.spacingM),
                  contentAlignment = Alignment.Center,
                ) {
                  CircularProgressIndicator()
                }
              }
            }
          }
        }
      }
      // Non-empty list with a soft load error (e.g. refresh failed) — keep content visible.
      if (controller.loadError != null && controller.items.isNotEmpty()) {
        Text(
          text = controller.loadError.orEmpty(),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(metrics.spacingS),
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
      }
    }

    if (configuration.showsComposer) {
      HorizontalDivider()
      CommentComposer(
        value = draft,
        onValueChange = { draft = it },
        onSend = { controller.submit(draft) },
        replyTarget = controller.replyTarget,
        onCancelReply = { controller.cancelReply() },
        isSending = controller.isSending,
        strings = configuration.strings,
      )
    }
  }
}
