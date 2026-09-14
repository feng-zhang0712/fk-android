package com.fk.business.comment

/**
 * Supplies comment pages, reply expansions, and submit operations.
 *
 * Implement in the app layer — CommentKit never performs networking.
 *
 * Conceptually aligned with iOS `FKCommentListDataSource` (suspend instead of completion).
 */
interface CommentListDataSource {
  suspend fun loadInitial(): CommentListPage

  suspend fun loadMore(): CommentListPage

  suspend fun loadReplies(parentId: String): List<CommentItem>

  suspend fun submit(request: CommentSubmitRequest): CommentItem
}

/**
 * Optional hooks for row interactions and submit outcomes.
 *
 * Conceptually aligned with iOS `FKCommentListDelegate` (narrow Compose port).
 * All methods have no-op defaults.
 */
interface CommentListListener {
  fun onToggleLike(
    item: CommentItem,
    previousIsLiked: Boolean,
    previousLikeCount: Int,
    previousLikeCountText: String?,
  ) = Unit

  fun onTapReply(item: CommentItem) = Unit

  fun onTapAvatar(item: CommentItem) = Unit

  fun onTapAuthor(item: CommentItem) = Unit

  fun onTapComment(item: CommentItem) = Unit

  fun onSelectMore(action: CommentMoreAction, item: CommentItem) = Unit

  fun onExpandReplies(parentId: String, inserted: List<CommentItem>, error: Throwable?) = Unit

  fun onCollapseReplies(parentId: String) = Unit

  fun onSubmit(item: CommentItem) = Unit

  fun onFailSubmit(request: CommentSubmitRequest, error: Throwable) = Unit
}
