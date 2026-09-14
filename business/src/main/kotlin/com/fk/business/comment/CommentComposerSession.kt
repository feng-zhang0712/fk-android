package com.fk.business.comment

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * In-memory composer session: visible text, focus, and per-target drafts.
 *
 * Conceptually aligned with draft / blur behavior of iOS `FKCommentComposerView`.
 */
@Stable
class CommentComposerSession {
  /** Visible input text. */
  var text: String by mutableStateOf("")
    private set

  /** Whether the text field currently holds focus. */
  var isFocused: Boolean by mutableStateOf(false)
    internal set

  /** Draft key used for the most recent blur / retarget (`""` = top-level). */
  var lastDraftKey: String by mutableStateOf(TOP_LEVEL_DRAFT_KEY)
    private set

  private val drafts = mutableStateMapOf<String, Draft>()

  /** Whether any preserved draft has non-empty trimmed text. */
  val hasPreservedDrafts: Boolean
    get() = drafts.values.any { it.text.trim().isNotEmpty() }

  fun draftKey(target: CommentReplyTarget?): String =
    target?.id ?: TOP_LEVEL_DRAFT_KEY

  fun setText(value: String, maxCharacterCount: Int?) {
    text = when {
      maxCharacterCount == null -> value
      else -> value.take(maxCharacterCount.coerceAtLeast(1))
    }
  }

  fun saveDraft(target: CommentReplyTarget?, preservesDrafts: Boolean) {
    if (!preservesDrafts) return
    val key = draftKey(target)
    val trimmed = text.trim()
    if (trimmed.isEmpty()) {
      drafts.remove(key)
    } else {
      drafts[key] = Draft(text = text, replyTarget = target)
    }
    lastDraftKey = key
  }

  fun loadDraft(target: CommentReplyTarget?, preservesDrafts: Boolean) {
    if (!preservesDrafts) {
      text = ""
      return
    }
    val key = draftKey(target)
    text = drafts[key]?.text.orEmpty()
  }

  fun discardDraft(key: String) {
    drafts.remove(key)
    if (draftKey(null) == key || lastDraftKey == key) {
      // Caller decides whether to clear visible text for the active target.
    }
  }

  fun discardDraftForTarget(target: CommentReplyTarget?) {
    drafts.remove(draftKey(target))
  }

  fun discardAllDrafts() {
    drafts.clear()
  }

  fun clearVisibleText() {
    text = ""
  }

  /**
   * Whether list hosts should keep the composer chrome visible.
   *
   * Conceptually aligned with iOS `FKCommentComposerView.shouldDisplayChrome`.
   */
  fun shouldDisplayChrome(
    showsComposer: Boolean,
    replyTarget: CommentReplyTarget?,
    configuration: CommentComposerConfiguration,
  ): Boolean {
    if (!showsComposer) return false
    return when (configuration.presentationMode) {
      CommentComposerPresentationMode.Always -> true
      CommentComposerPresentationMode.OnDemand ->
        isFocused || replyTarget != null
      CommentComposerPresentationMode.Automatic ->
        isFocused || replyTarget != null || text.isNotBlank() || hasPreservedDrafts
    }
  }

  /**
   * Applies blur / external dismiss reset.
   *
   * @return whether the host should clear [CommentListController.replyTarget].
   */
  fun applyBlurReset(
    replyTarget: CommentReplyTarget?,
    configuration: CommentComposerConfiguration,
  ): BlurResetResult {
    if (!configuration.clearsCompositionOnBlur) {
      return BlurResetResult(clearReplyTarget = false, keepVisibleText = true)
    }
    lastDraftKey = draftKey(replyTarget)
    saveDraft(replyTarget, configuration.preservesDrafts)
    val keepVisibleText =
      configuration.presentationMode == CommentComposerPresentationMode.Automatic &&
        text.isNotBlank()
    if (!keepVisibleText) {
      text = ""
    }
    return BlurResetResult(clearReplyTarget = true, keepVisibleText = keepVisibleText)
  }

  data class BlurResetResult(
    val clearReplyTarget: Boolean,
    val keepVisibleText: Boolean,
  )

  private data class Draft(
    val text: String,
    val replyTarget: CommentReplyTarget?,
  )

  companion object {
    const val TOP_LEVEL_DRAFT_KEY: String = ""
  }
}
