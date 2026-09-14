package com.fk.business.comment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Orchestrates comment list state, mutations, composer reply target, and data-source calls.
 *
 * Conceptually aligned with iOS `FKCommentListViewController` load / mutation surface
 * (Compose state holder — not a ViewController).
 */
@Stable
class CommentListController(
  val configuration: CommentConfiguration = CommentConfiguration(),
  private val scope: CoroutineScope,
) {
  var items: List<CommentItem> by mutableStateOf(emptyList())
    private set

  var hasMore: Boolean by mutableStateOf(false)
    private set

  var isInitialLoading: Boolean by mutableStateOf(false)
    private set

  var isLoadingMore: Boolean by mutableStateOf(false)
    private set

  var isSending: Boolean by mutableStateOf(false)
    private set

  var replyTarget: CommentReplyTarget? by mutableStateOf(null)
    private set

  var expandingParentId: String? by mutableStateOf(null)
    private set

  var loadError: String? by mutableStateOf(null)
    private set

  /** Increments after a successful submit so the host can clear the composer draft. */
  var composerResetToken: Int by mutableStateOf(0)
    private set

  private var dataSource: CommentListDataSource? = null
  private var listener: CommentListListener = NoOpCommentListListener
  private var disposed = false
  private var loadJob: Job? = null
  private var likePendingIds = mutableSetOf<String>()

  fun bind(
    dataSource: CommentListDataSource,
    listener: CommentListListener = NoOpCommentListListener,
  ): CommentListController {
    this.dataSource = dataSource
    this.listener = listener
    return this
  }

  fun loadInitial() {
    val source = dataSource ?: return
    if (disposed) return
    loadJob?.cancel()
    isInitialLoading = items.isEmpty()
    loadError = null
    loadJob = scope.launch {
      try {
        val page = source.loadInitial()
        if (disposed) return@launch
        items = page.items
        hasMore = page.hasMore
        likePendingIds.clear()
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        if (disposed) return@launch
        loadError = error.message ?: "Failed to load comments"
      } finally {
        if (!disposed) isInitialLoading = false
      }
    }
  }

  fun loadMore() {
    val source = dataSource ?: return
    if (disposed || !hasMore || isLoadingMore || isInitialLoading) return
    isLoadingMore = true
    scope.launch {
      try {
        val page = source.loadMore()
        if (disposed) return@launch
        items = CommentListMutation.appendingUnique(page.items, onto = items)
        hasMore = page.hasMore
      } catch (error: CancellationException) {
        throw error
      } catch (_: Throwable) {
        // Keep existing content; host may retry.
      } finally {
        if (!disposed) isLoadingMore = false
      }
    }
  }

  fun toggleLike(item: CommentItem) {
    toggleLike(item.id)
  }

  /** Optimistic like toggle using the latest row for [id]. */
  fun toggleLike(id: String) {
    if (disposed) return
    val item = items.firstOrNull { it.id == id } ?: return
    val previousIsLiked = item.isLiked
    val previousLikeCount = item.likeCount
    val previousLikeCountText = item.likeCountText
    val updated = CommentLikeOptimistic.toggled(item)
    items = CommentListMutation.replacing(updated, items)
    // While a persistence attempt is in flight, keep updating UI but do not re-notify.
    if (id !in likePendingIds) {
      likePendingIds.add(id)
      listener.onToggleLike(
        item = updated,
        previousIsLiked = previousIsLiked,
        previousLikeCount = previousLikeCount,
        previousLikeCountText = previousLikeCountText,
      )
    }
  }

  /**
   * Clears the in-flight like marker after the app successfully persists the toggle.
   *
   * Call this on success; call [rollbackLike] on failure.
   */
  fun acknowledgeLike(id: String) {
    likePendingIds.remove(id)
  }

  fun rollbackLike(
    id: String,
    previousIsLiked: Boolean,
    previousLikeCount: Int,
    previousLikeCountText: String? = null,
  ) {
    val current = items.firstOrNull { it.id == id } ?: return
    val restored = CommentLikeOptimistic.rolledBack(
      item = current,
      previousIsLiked = previousIsLiked,
      previousLikeCount = previousLikeCount,
      previousLikeCountText = previousLikeCountText,
    )
    items = CommentListMutation.replacing(restored, items)
    likePendingIds.remove(id)
  }

  fun beginReply(item: CommentItem) {
    replyTarget = CommentReplyTarget(id = item.id, displayName = item.authorName)
    listener.onTapReply(item)
  }

  fun cancelReply() {
    replyTarget = null
  }

  fun expandOrCollapseReplies(item: CommentItem) {
    expandOrCollapseReplies(item.id)
  }

  /** Expand or collapse replies using the latest row for [id]. */
  fun expandOrCollapseReplies(id: String) {
    val item = items.firstOrNull { it.id == id } ?: return
    if (item.areRepliesExpanded) {
      items = CommentListMutation.collapsingReplies(parentId = item.id, inItems = items)
      listener.onCollapseReplies(item.id)
      return
    }
    if (item.replyCount <= 0) return
    val source = dataSource ?: return
    if (expandingParentId != null) return
    expandingParentId = item.id
    scope.launch {
      try {
        val replies = source.loadReplies(item.id)
        if (disposed) return@launch
        items = CommentListMutation.insertingReplies(
          replies = replies,
          parentId = item.id,
          into = items,
          maxCount = configuration.maxExpandedReplies,
        )
        listener.onExpandReplies(item.id, replies, null)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        if (!disposed) {
          listener.onExpandReplies(item.id, emptyList(), error)
        }
      } finally {
        if (!disposed) expandingParentId = null
      }
    }
  }

  fun submit(text: String) {
    val source = dataSource ?: return
    val trimmed = text.trim()
    if (trimmed.isEmpty() || isSending || disposed) return
    val request = CommentSubmitRequest(
      text = trimmed,
      replyToCommentId = replyTarget?.id,
    )
    isSending = true
    scope.launch {
      try {
        val created = source.submit(request)
        if (disposed) return@launch
        val withReplyMeta = if (replyTarget != null) {
          created.copy(
            parentId = created.parentId ?: replyTarget?.id,
            replyTo = created.replyTo ?: replyTarget,
          )
        } else {
          created
        }
        items = CommentListMutation.insertingSubmitted(withReplyMeta, into = items)
        replyTarget = null
        composerResetToken += 1
        listener.onSubmit(withReplyMeta)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        if (!disposed) {
          listener.onFailSubmit(request, error)
        }
      } finally {
        if (!disposed) isSending = false
      }
    }
  }

  /** Inserts a comment (submit / realtime) with the same placement rules as [submit]. */
  fun insertComment(item: CommentItem) {
    items = CommentListMutation.insertingSubmitted(item, into = items)
  }

  /** Replaces a row by id (e.g. edited body). */
  fun updateComment(item: CommentItem) {
    items = CommentListMutation.replacing(item, items)
  }

  fun removeComment(id: String) {
    items = CommentListMutation.removing(id = id, from = items)
    likePendingIds.remove(id)
  }

  fun replaceComments(newItems: List<CommentItem>, hasMorePages: Boolean = hasMore) {
    items = newItems
    hasMore = hasMorePages
    likePendingIds.clear()
  }

  /** Applies a built-in more action (delete removes locally) and notifies the listener. */
  fun selectMore(action: CommentMoreAction, item: CommentItem) {
    if (action == CommentMoreAction.Delete) {
      removeComment(item.id)
    }
    listener.onSelectMore(action, item)
  }

  fun notifyTapAvatar(item: CommentItem) = listener.onTapAvatar(item)

  fun notifyTapAuthor(item: CommentItem) = listener.onTapAuthor(item)

  fun notifyTapComment(item: CommentItem) = listener.onTapComment(item)

  fun dispose() {
    if (disposed) return
    disposed = true
    loadJob?.cancel()
    dataSource = null
  }
}

/** No-op [CommentListListener] for hosts that only need the list/composer UI. */
object NoOpCommentListListener : CommentListListener

@Composable
fun rememberCommentListController(
  configuration: CommentConfiguration = CommentConfiguration(),
  dataSource: CommentListDataSource,
  listener: CommentListListener = NoOpCommentListListener,
  loadOnStart: Boolean = true,
): CommentListController {
  val scope = rememberCoroutineScope()
  val controller = remember(configuration) {
    CommentListController(configuration = configuration, scope = scope)
  }
  DisposableEffect(controller) {
    onDispose { controller.dispose() }
  }
  LaunchedEffect(controller, dataSource, listener, loadOnStart) {
    controller.bind(dataSource, listener)
    if (loadOnStart) controller.loadInitial()
  }
  return controller
}
