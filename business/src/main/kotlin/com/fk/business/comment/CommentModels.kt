package com.fk.business.comment

/**
 * Comment kit package hub — models, contracts, list mutations, Compose UI.
 *
 * Conceptually aligned with iOS `FKCommentKit` (no networking).
 */
object CommentKit {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.3.0"
}

/**
 * Row / list layout skeleton (iOS `FKCommentLayoutPreset`).
 */
enum class CommentLayoutPreset {
  /** Feed-style: author + time header, reply-to line, body, bottom action bar. */
  Standard,

  /** Denser social-style: author ▸ reply-to + trailing time, body, meta action rail. */
  Compact,
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
 *
 * Use [standard] / [compact] for preset-matched defaults (iOS
 * `FKCommentKitConfiguration.configuration(for:)`).
 */
data class CommentConfiguration(
  val layoutPreset: CommentLayoutPreset = CommentLayoutPreset.Standard,
  val showsComposer: Boolean = true,
  val showsLikeAction: Boolean = true,
  val showsReplyAction: Boolean = true,
  val showsMoreAction: Boolean = true,
  val showsCopyAction: Boolean = true,
  val showsReportAction: Boolean = true,
  /**
   * When `true` (default), [CommentListHost] presents the built-in more bottom sheet.
   * When `false`, the host only invokes the `onMore` callback.
   */
  val presentsDefaultMoreMenu: Boolean = true,
  val beginsReplyOnRowTap: Boolean = true,
  val maxVisualDepth: Int = 1,
  val indentWidthDp: Int = 24,
  val maxExpandedReplies: Int = 50,
  val expandAffordanceMinimumReplyCount: Int = 1,
  val showsSeparators: Boolean = true,
  val showsThreadLine: Boolean = true,
  val bodyMaxLines: Int? = 6,
  val usesExpandableBody: Boolean = true,
  /** Composer chrome tokens (iOS `FKCommentComposerConfiguration`). */
  val composer: CommentComposerConfiguration = CommentComposerConfiguration(),
  val strings: CommentStrings = CommentStrings(),
) {
  companion object {
    /** Standard feed-style defaults. */
    fun standard(): CommentConfiguration = CommentConfiguration()

    /** Compact social-style defaults (iOS `FKCommentCompactDefaults`). */
    fun compact(): CommentConfiguration = CommentConfiguration(
      layoutPreset = CommentLayoutPreset.Compact,
      indentWidthDp = 42,
      showsSeparators = false,
      showsThreadLine = false,
      bodyMaxLines = 8,
      composer = CommentComposerConfiguration.compact(),
      strings = CommentStrings.compact(),
    )
  }
}

/**
 * When the composer chrome is visible inside [CommentListHost].
 *
 * [CommentConfiguration.showsComposer] remains the master switch for read-only lists.
 * Conceptually aligned with iOS `FKCommentComposerPresentationMode`.
 */
enum class CommentComposerPresentationMode {
  /** Composer bar stays visible whenever the list shows a composer. */
  Always,

  /** Shown while composing (focused or active reply target); hidden after blur reset. */
  OnDemand,

  /**
   * Like [OnDemand], but stays visible after blur when text or preserved drafts remain
   * so the user can resume; hides when idle with no drafts.
   */
  Automatic,
}

/**
 * Appearance and behavior for [CommentComposer].
 *
 * Conceptually aligned with iOS `FKCommentComposerConfiguration`.
 */
data class CommentComposerConfiguration(
  val maxCharacterCount: Int? = 2000,
  val usesCapsuleInput: Boolean = false,
  val showsSendButton: Boolean = true,
  /** When `true` (default), shows the reply-target stripe above the input. */
  val showsReplyTargetBanner: Boolean = true,
  /** When `true` (default), shows Cancel on the reply-target stripe. */
  val showsCancelReplyButton: Boolean = true,
  val presentationMode: CommentComposerPresentationMode = CommentComposerPresentationMode.Always,
  /**
   * When `true` (default), losing focus clears the reply stripe and visible text
   * (see [presentationMode] for automatic text retention) while optionally preserving drafts.
   */
  val clearsCompositionOnBlur: Boolean = true,
  /**
   * When `true` (default), non-empty input is stored per reply target (and top-level) on blur /
   * retarget and restored the next time that target is composed.
   */
  val preservesDrafts: Boolean = true,
) {
  companion object {
    fun compact(): CommentComposerConfiguration = CommentComposerConfiguration(
      usesCapsuleInput = true,
    )
  }
}

/**
 * English default strings (apps replace for localization).
 *
 * Format placeholders use `%s` / `%d` (Android-style).
 */
data class CommentStrings(
  val reply: String = "Reply",
  val like: String = "Like",
  val unlike: String = "Unlike",
  val more: String = "More",
  val send: String = "Send",
  val composerPlaceholder: String = "Add a comment…",
  /** Composer / standard reply-to line: `Replying to %s`. */
  val replyToFormat: String = "Replying to %s",
  val cancelReply: String = "Cancel",
  val viewReplies: String = "View %d replies",
  val hideReplies: String = "Hide replies",
  val expandMore: String = "Expand more",
  val expandBody: String = "Read more",
  val collapseBody: String = "Show less",
  val loadingReplies: String = "Loading…",
  val copy: String = "Copy",
  val delete: String = "Delete",
  val report: String = "Report",
  val cancel: String = "Cancel",
  val emptyTitle: String = "No comments yet",
  val emptyDescription: String = "Be the first to comment.",
) {
  fun replyToText(displayName: String): String =
    replyToFormat.replace("%s", displayName)

  fun viewRepliesText(count: Int): String =
    viewReplies.replace("%d", count.toString())

  companion object {
    fun compact(): CommentStrings = CommentStrings(
      composerPlaceholder = "Say something nice…",
      replyToFormat = "%s",
      viewReplies = "Expand %d replies",
      hideReplies = "Collapse",
    )
  }
}
