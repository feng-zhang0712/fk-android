package com.fk.business.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkTextStyle
import kotlinx.coroutines.delay

/**
 * Bottom composer with optional reply-target banner, text input, and send control.
 *
 * Owns per-target drafts, blur reset, and presentation hints via
 * [CommentComposerConfiguration]. Conceptually aligned with iOS `FKCommentComposerView`.
 *
 * @param session In-memory text + drafts for this host.
 * @param focusToken Increments when the host begins composition; requests IME focus.
 * @param onBlurComposition Fired after blur reset when the reply stripe should clear.
 * @param onCompositionStateChange Fired when focus / text / drafts may affect chrome visibility.
 */
@Composable
fun CommentComposer(
  session: CommentComposerSession,
  replyTarget: CommentReplyTarget?,
  onSend: (String) -> Unit,
  onCancelReply: () -> Unit,
  modifier: Modifier = Modifier,
  isSending: Boolean = false,
  configuration: CommentConfiguration = CommentConfiguration.standard(),
  enabled: Boolean = true,
  focusToken: Int = 0,
  onBlurComposition: () -> Unit = {},
  onCompositionStateChange: () -> Unit = {},
) {
  val composerConfig = configuration.composer
  val strings = configuration.strings
  val trimmed = session.text.trim()
  val canSend = enabled && !isSending && trimmed.isNotEmpty()
  val usesCapsule = composerConfig.usesCapsuleInput
  val focusRequester = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current
  /** Last reply id we loaded a draft for (`""` = top-level). */
  var boundDraftKey by remember { mutableStateOf<String?>(null) }
  var lastHandledFocusToken by remember { mutableIntStateOf(0) }
  var suppressBlurReset by remember { mutableStateOf(false) }

  fun applyTargetDraft(target: CommentReplyTarget?) {
    val key = session.draftKey(target)
    if (boundDraftKey != null && boundDraftKey != key) {
      // Save previous visible text under the previous key before swapping.
      val previousTarget = boundDraftKey!!
        .takeIf { it.isNotEmpty() }
        ?.let { CommentReplyTarget(id = it, displayName = "") }
      session.saveDraft(previousTarget, composerConfig.preservesDrafts)
    }
    session.loadDraft(target, composerConfig.preservesDrafts)
    boundDraftKey = key
  }

  // beginComposition / retarget while composing.
  LaunchedEffect(focusToken, replyTarget?.id) {
    if (focusToken > lastHandledFocusToken) {
      lastHandledFocusToken = focusToken
      suppressBlurReset = true
      applyTargetDraft(replyTarget)
      onCompositionStateChange()
      delay(16)
      focusRequester.requestFocus()
      keyboard?.show()
      delay(64)
      focusRequester.requestFocus()
      keyboard?.show()
      suppressBlurReset = false
      return@LaunchedEffect
    }
    // Retarget while already focused (tap another row without blurring).
    if (session.isFocused) {
      val key = session.draftKey(replyTarget)
      if (boundDraftKey != key) {
        applyTargetDraft(replyTarget)
        onCompositionStateChange()
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        if (usesCapsule) {
          fkColor(FkColorRole.Surface)
        } else {
          fkColor(FkColorRole.SurfaceElevated)
        },
      )
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    val showBanner = composerConfig.showsReplyTargetBanner && replyTarget != null
    if (showBanner) {
      val target = replyTarget!!
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = strings.replyToText(target.displayName),
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          modifier = Modifier.weight(1f),
        )
        if (composerConfig.showsCancelReplyButton) {
          TextButton(
            onClick = {
              suppressBlurReset = true
              session.saveDraft(replyTarget, composerConfig.preservesDrafts)
              session.clearVisibleText()
              boundDraftKey = null
              keyboard?.hide()
              onCancelReply()
              onCompositionStateChange()
              suppressBlurReset = false
            },
            enabled = !isSending,
          ) {
            Text(
              text = strings.cancelReply,
              color = fkColor(FkColorRole.Primary),
            )
          }
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .background(
            color = if (usesCapsule) {
              fkColor(FkColorRole.SurfaceElevated)
            } else {
              fkColor(FkColorRole.Surface)
            },
            shape = RoundedCornerShape(if (usesCapsule) 18.dp else 8.dp),
          )
          .padding(horizontal = 14.dp, vertical = 10.dp),
      ) {
        ComposerTextField(
          value = session.text,
          onValueChange = { value ->
            session.setText(value, composerConfig.maxCharacterCount)
            onCompositionStateChange()
          },
          enabled = enabled && !isSending,
          placeholder = strings.composerPlaceholder,
          focusRequester = focusRequester,
          onFocusChanged = { focused ->
            val wasFocused = session.isFocused
            session.isFocused = focused
            if (focused) {
              onCompositionStateChange()
              return@ComposerTextField
            }
            if (!wasFocused || suppressBlurReset) {
              onCompositionStateChange()
              return@ComposerTextField
            }
            val result = session.applyBlurReset(replyTarget, composerConfig)
            boundDraftKey = null
            if (result.clearReplyTarget) {
              onBlurComposition()
            }
            onCompositionStateChange()
          },
        )
      }
      if (composerConfig.showsSendButton) {
        TextButton(
          onClick = { onSend(trimmed) },
          enabled = canSend,
        ) {
          Text(
            text = if (isSending) "…" else strings.send,
            style = fkTextStyle(FkTextStyle.Headline).copy(fontWeight = FontWeight.SemiBold),
            color = if (canSend) {
              fkColor(FkColorRole.Primary)
            } else {
              fkColor(FkColorRole.OnSurfaceSecondary)
            },
          )
        }
      }
    }
  }
}

/**
 * Collapsed affordance when [CommentComposerPresentationMode.OnDemand] / [Automatic]
 * hides the full composer chrome.
 */
@Composable
fun CommentComposerCollapsedHint(
  placeholder: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  usesCapsule: Boolean = false,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(fkColor(FkColorRole.Surface))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
      )
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .background(
          color = fkColor(FkColorRole.SurfaceElevated),
          shape = RoundedCornerShape(if (usesCapsule) 18.dp else 8.dp),
        )
        .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Text(
        text = placeholder,
        style = fkTextStyle(FkTextStyle.Body),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
    }
  }
}

@Composable
private fun ComposerTextField(
  value: String,
  onValueChange: (String) -> Unit,
  enabled: Boolean,
  placeholder: String,
  focusRequester: FocusRequester,
  onFocusChanged: (Boolean) -> Unit,
) {
  val textColor = fkColor(FkColorRole.OnSurface)
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
    singleLine = true,
    textStyle = fkTextStyle(FkTextStyle.Body).copy(color = textColor),
    cursorBrush = SolidColor(fkColor(FkColorRole.Primary)),
    modifier = Modifier
      .fillMaxWidth()
      .focusRequester(focusRequester)
      .onFocusChanged { onFocusChanged(it.isFocused) },
    decorationBox = { inner ->
      Box {
        if (value.isEmpty()) {
          Text(
            text = placeholder,
            style = fkTextStyle(FkTextStyle.Body),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
          )
        }
        inner()
      }
    },
  )
}
