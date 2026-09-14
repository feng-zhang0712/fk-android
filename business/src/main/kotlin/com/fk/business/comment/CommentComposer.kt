package com.fk.business.comment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Comment composer with optional reply banner and send control.
 *
 * Conceptually aligned with iOS `FKCommentComposerView`.
 */
@Composable
fun CommentComposer(
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  modifier: Modifier = Modifier,
  replyTarget: CommentReplyTarget? = null,
  onCancelReply: () -> Unit = {},
  isSending: Boolean = false,
  strings: CommentStrings = CommentStrings(),
  enabled: Boolean = true,
) {
  val metrics = fkMetrics()
  val canSend = enabled && !isSending && value.trim().isNotEmpty()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(metrics.spacingM),
    verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
  ) {
    replyTarget?.let { target ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${strings.replyingTo} ${target.displayName}",
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onCancelReply, enabled = !isSending) {
          Text(strings.cancelReply)
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
    ) {
      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.weight(1f),
        enabled = enabled && !isSending,
        placeholder = { Text(strings.composerPlaceholder) },
        singleLine = true,
      )
      Button(
        onClick = onSend,
        enabled = canSend,
      ) {
        Text(if (isSending) "…" else strings.send)
      }
    }
  }
}
