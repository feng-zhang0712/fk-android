package com.fk.business.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fk.ui.empty.EmptyActionSet
import com.fk.ui.empty.EmptyConfiguration
import com.fk.ui.empty.EmptyContent
import com.fk.ui.empty.EmptyPhase
import com.fk.ui.empty.EmptyStateContent
import com.fk.ui.empty.EmptyType
import com.fk.ui.sheet.FkBottomSheet
import com.fk.ui.sheet.SheetConfiguration
import com.fk.ui.sheet.rememberFkBottomSheetState
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Comment list + composer host.
 *
 * Conceptually aligned with iOS `FKCommentListViewController` content surface.
 *
 * @param onMore Called when More is tapped and
 *   [CommentConfiguration.presentsDefaultMoreMenu] is `false`.
 *   When the default menu is enabled, built-in Copy / Report / Delete are presented
 *   in a bottom sheet and [CommentListController.selectMore] is invoked automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentListHost(
  controller: CommentListController,
  modifier: Modifier = Modifier,
  onMore: (CommentItem) -> Unit = {},
) {
  val configuration = controller.configuration
  val composerConfig = configuration.composer
  val metrics = fkMetrics()
  val composerSession = remember { CommentComposerSession() }
  var chromeEpoch by remember { mutableStateOf(0) }
  val listState = rememberLazyListState()
  val focusManager = LocalFocusManager.current
  val keyboard = LocalSoftwareKeyboardController.current
  val clipboard = LocalClipboardManager.current
  var moreTarget by remember { mutableStateOf<CommentItem?>(null) }
  var pendingSubmitDraftKey by remember { mutableStateOf<String?>(null) }

  fun refreshChrome() {
    chromeEpoch += 1
  }

  fun dismissComposition() {
    keyboard?.hide()
    if (composerSession.isFocused) {
      // Blur path in CommentComposer saves the draft and clears the stripe.
      focusManager.clearFocus(force = true)
      return
    }
    if (controller.replyTarget != null || composerSession.text.isNotBlank()) {
      val result = composerSession.applyBlurReset(controller.replyTarget, composerConfig)
      if (result.clearReplyTarget) {
        controller.cancelReply()
      }
      refreshChrome()
    }
  }

  val showComposerChrome = run {
    // chromeEpoch forces recompute after blur / cancel / submit.
    @Suppress("UNUSED_EXPRESSION")
    chromeEpoch
    composerSession.shouldDisplayChrome(
      showsComposer = configuration.showsComposer,
      replyTarget = controller.replyTarget,
      configuration = composerConfig,
    )
  }

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
      pendingSubmitDraftKey?.let { composerSession.discardDraft(it) }
      pendingSubmitDraftKey = null
      composerSession.clearVisibleText()
      refreshChrome()
    }
  }
  LaunchedEffect(listState, controller.composerFocusToken) {
    if (controller.composerFocusToken > 0) {
      delay(300)
    }
    snapshotFlow { listState.isScrollInProgress }
      .distinctUntilChanged()
      .filter { it }
      .collect { dismissComposition() }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .imePadding(),
  ) {
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
            modifier = Modifier
              .align(Alignment.Center)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissComposition() },
              ),
          )
        }
        else -> {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissComposition() },
              ),
            state = listState,
          ) {
            items(controller.items, key = { it.id }) { item ->
              CommentRow(
                item = item,
                configuration = configuration,
                isExpanding = controller.expandingParentId == item.id,
                onLike = { controller.toggleLike(item.id) },
                onReply = { controller.beginReply(item) },
                onMore = {
                  if (configuration.presentsDefaultMoreMenu) {
                    dismissComposition()
                    moreTarget = item
                  } else {
                    onMore(item)
                  }
                },
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
              if (configuration.showsSeparators) {
                HorizontalDivider(color = fkColor(FkColorRole.Outline).copy(alpha = 0.35f))
              }
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
      if (showComposerChrome) {
        CommentComposer(
          session = composerSession,
          replyTarget = controller.replyTarget,
          onSend = { text ->
            pendingSubmitDraftKey = composerSession.draftKey(controller.replyTarget)
            controller.submit(text)
          },
          onCancelReply = {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
            controller.cancelReply()
            refreshChrome()
          },
          isSending = controller.isSending,
          configuration = configuration,
          focusToken = controller.composerFocusToken,
          onBlurComposition = {
            controller.cancelReply()
            refreshChrome()
          },
          onCompositionStateChange = { refreshChrome() },
        )
      } else {
        CommentComposerCollapsedHint(
          placeholder = configuration.strings.composerPlaceholder,
          usesCapsule = composerConfig.usesCapsuleInput,
          onClick = { controller.beginTopLevelComment() },
        )
      }
    }
  }

  moreTarget?.let { item ->
    val sheetState = rememberFkBottomSheetState(SheetConfiguration.BottomSheetFit)
    val strings = configuration.strings
    FkBottomSheet(
      onDismissRequest = { moreTarget = null },
      configuration = SheetConfiguration.BottomSheetFit,
      sheetState = sheetState,
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (configuration.showsCopyAction && item.body.isNotBlank()) {
          CommentMoreSheetRow(
            title = strings.copy,
            onClick = {
              clipboard.setText(AnnotatedString(item.body))
              controller.selectMore(CommentMoreAction.Copy, item)
              moreTarget = null
            },
          )
        }
        if (configuration.showsReportAction) {
          CommentMoreSheetRow(
            title = strings.report,
            onClick = {
              controller.selectMore(CommentMoreAction.Report, item)
              moreTarget = null
            },
          )
        }
        if (item.canDelete) {
          CommentMoreSheetRow(
            title = strings.delete,
            destructive = true,
            onClick = {
              controller.selectMore(CommentMoreAction.Delete, item)
              moreTarget = null
            },
          )
        }
      }
    }
  }
}

@Composable
private fun CommentMoreSheetRow(
  title: String,
  onClick: () -> Unit,
  destructive: Boolean = false,
) {
  val metrics = fkMetrics()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = metrics.spacingM),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = fkTextStyle(FkTextStyle.Body).copy(fontWeight = FontWeight.Normal),
      color = if (destructive) {
        fkStatusColor(FkStatusSemantic.Error)
      } else {
        fkColor(FkColorRole.OnSurface)
      },
    )
  }
}
