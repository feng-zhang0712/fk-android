package com.fk.business.comment

/**
 * Comment kit package hub — models, contracts, list mutations, Compose UI.
 *
 * Conceptually aligned with iOS `FKCommentKit` (no networking).
 */
object CommentKit {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.0"
}

/**
 * Target comment referenced by a reply (“Replying to …”).
 */
data class CommentReplyTarget(
  val id: String,
  val displayName: String,
)

/**
 * View model for a comment list row.
 *
 * Flat list + optional [parentId] / [replyTo] / [depth] for threading.
 */
data class CommentItem(
  val id: String,
  val authorName: String,
  val body: String,
  val avatarUrl: String? = null,
  val timestampText: String? = null,
  val likeCount: Int = 0,
  val likeCountText: String? = null,
  val isLiked: Boolean = false,
  val replyCount: Int = 0,
  val parentId: String? = null,
  val replyTo: CommentReplyTarget? = null,
  val depth: Int = 0,
  val isVerified: Boolean = false,
  val isOwnedByCurrentUser: Boolean = false,
  val isDeletable: Boolean = false,
  val areRepliesExpanded: Boolean = false,
) {
  init {
    require(likeCount >= 0) { "likeCount must be >= 0" }
    require(replyCount >= 0) { "replyCount must be >= 0" }
    require(depth >= 0) { "depth must be >= 0" }
  }

  /** Effective deletable flag (owned rows are deletable by default). */
  val canDelete: Boolean get() = isDeletable || isOwnedByCurrentUser

  /** Display like count string. */
  fun formattedLikeCount(): String = likeCountText ?: likeCount.toString()
}

/**
 * One page of comments for pagination.
 */
data class CommentListPage(
  val items: List<CommentItem>,
  val hasMore: Boolean,
)

/**
 * Payload for submitting a new comment or reply.
 */
data class CommentSubmitRequest(
  val text: String,
  val replyToCommentId: String? = null,
)

/**
 * Built-in “more” menu actions.
 */
enum class CommentMoreAction {
  Copy,
  Delete,
  Report,
}

/**
 * Feature flags and copy for the comment host.
 */
data class CommentConfiguration(
  val showsComposer: Boolean = true,
  val showsLikeAction: Boolean = true,
  val showsReplyAction: Boolean = true,
  val showsMoreAction: Boolean = true,
  val beginsReplyOnRowTap: Boolean = true,
  val maxVisualDepth: Int = 1,
  val indentWidthDp: Int = 16,
  val maxExpandedReplies: Int = 50,
  val strings: CommentStrings = CommentStrings(),
)

/**
 * English default strings (apps replace for localization).
 */
data class CommentStrings(
  val reply: String = "Reply",
  val like: String = "Like",
  val more: String = "More",
  val send: String = "Send",
  val composerPlaceholder: String = "Add a comment…",
  val replyingTo: String = "Replying to",
  val cancelReply: String = "Cancel",
  val viewReplies: String = "View %d replies",
  val hideReplies: String = "Hide replies",
  val copy: String = "Copy",
  val delete: String = "Delete",
  val report: String = "Report",
  val emptyTitle: String = "No comments yet",
  val emptyDescription: String = "Be the first to comment.",
)
