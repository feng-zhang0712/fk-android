package com.fk.ui.empty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Empty-state package hub — loading / empty / error overlays.
 *
 * Conceptually aligned with iOS `FKUIKit` EmptyState.
 */
object Empty {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.0"

  /** Minimum [EmptyLayout.maxContentWidth] applied when rendering (iOS clamp parity). */
  val MinContentWidth: Dp = 180.dp
}

/**
 * Inline empty-state content (column layout).
 *
 * Renders nothing when [EmptyConfiguration.phase] is [EmptyPhase.Content].
 *
 * @param configuration Aggregate empty-state configuration.
 * @param onAction Invoked when an action button is pressed (route by [EmptyAction.id]).
 * @param illustration Optional leading illustration / icon slot.
 * @param isRefreshing Host pull-to-refresh active; may suppress loading chrome.
 */
@Composable
fun EmptyStateContent(
  configuration: EmptyConfiguration,
  onAction: (EmptyAction) -> Unit = {},
  modifier: Modifier = Modifier,
  illustration: (@Composable ColumnScope.() -> Unit)? = null,
  isRefreshing: Boolean = false,
) {
  val config = configuration.withEnforcedRetry()
  if (shouldHide(config, isRefreshing)) return

  val metrics = fkMetrics()
  val density = config.layout.density
  val spacing = config.layout.verticalSpacing
    ?: (metrics.spacingM * density.spacingScale())
  val titleStyle = scaledStyle(fkTextStyle(FkTextStyle.Headline), density.typeScale())
  val bodyStyle = scaledStyle(fkTextStyle(FkTextStyle.Subheadline), density.typeScale())
  val onSurface = fkColor(FkColorRole.OnSurface)
  val onSecondary = fkColor(FkColorRole.OnSurfaceSecondary)
  val maxWidth = config.layout.maxContentWidth.coerceAtLeast(Empty.MinContentWidth)

  Column(
    modifier = modifier
      .widthIn(max = maxWidth)
      .padding(config.layout.contentPadding)
      .offset(y = config.layout.verticalOffset),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    when (config.phase) {
      EmptyPhase.Loading -> {
        CircularProgressIndicator(
          modifier = Modifier.size(36.dp),
          color = onSecondary,
          strokeWidth = 3.dp,
        )
        val message = config.content.loadingMessage
          ?: config.content.title
        if (!message.isNullOrBlank()) {
          Text(
            text = message,
            style = bodyStyle,
            color = onSecondary,
            textAlign = TextAlign.Center,
          )
        }
      }
      else -> {
        if (illustration != null) {
          illustration()
        }
        val title = config.content.title
        if (!title.isNullOrBlank()) {
          Text(
            text = title,
            style = titleStyle,
            color = onSurface,
            textAlign = TextAlign.Center,
            maxLines = config.presentation.maxTitleLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
          )
        }
        val description = config.content.description
        if (!description.isNullOrBlank()) {
          val display = truncateDescription(description, config.presentation)
          Text(
            text = display,
            style = bodyStyle,
            color = onSecondary,
            textAlign = TextAlign.Center,
            maxLines = config.presentation.maxDescriptionLines,
            overflow = TextOverflow.Ellipsis,
            modifier = if (display != description) {
              Modifier.semantics { contentDescription = description }
            } else {
              Modifier
            },
          )
        }
        if (!config.actions.isEmpty) {
          ActionColumn(
            actions = config.actions,
            onAction = onAction,
            spacing = metrics.spacingXs,
          )
        }
      }
    }
  }
}

/**
 * Full-bleed overlay that centers [EmptyStateContent] over a host.
 *
 * Place inside a [Box]; hidden when phase is [EmptyPhase.Content].
 */
@Composable
fun BoxScope.EmptyStateOverlay(
  configuration: EmptyConfiguration,
  onAction: (EmptyAction) -> Unit = {},
  modifier: Modifier = Modifier,
  illustration: (@Composable ColumnScope.() -> Unit)? = null,
  isRefreshing: Boolean = false,
) {
  val config = configuration.withEnforcedRetry()
  if (shouldHide(config, isRefreshing)) return

  val alignment = when (config.layout.alignment) {
    EmptyContentAlignment.Center -> Alignment.Center
    EmptyContentAlignment.Top -> Alignment.TopCenter
  }
  val dim = config.presentation.blockingOverlayAlpha.coerceIn(0f, 1f)

  Box(
    modifier = modifier
      .matchParentSize()
      .background(fkColor(FkColorRole.Background)),
    contentAlignment = alignment,
  ) {
    if (dim > 0f) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(Color.Black.copy(alpha = dim)),
      )
    }
    EmptyStateContent(
      configuration = config,
      onAction = onAction,
      illustration = illustration,
      isRefreshing = isRefreshing,
    )
  }
}

/**
 * Host helper: draws [content] and overlays empty-state when not [EmptyPhase.Content].
 */
@Composable
fun EmptyStateHost(
  configuration: EmptyConfiguration,
  onAction: (EmptyAction) -> Unit = {},
  modifier: Modifier = Modifier,
  illustration: (@Composable ColumnScope.() -> Unit)? = null,
  isRefreshing: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(modifier = modifier) {
    content()
    EmptyStateOverlay(
      configuration = configuration,
      onAction = onAction,
      illustration = illustration,
      isRefreshing = isRefreshing,
    )
  }
}

@Composable
private fun ActionColumn(
  actions: EmptyActionSet,
  onAction: (EmptyAction) -> Unit,
  spacing: Dp,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    actions.primary?.let { action ->
      SlotButton(action = action, slot = ActionSlot.Primary, onAction = onAction)
    }
    actions.secondary?.let { action ->
      SlotButton(action = action, slot = ActionSlot.Secondary, onAction = onAction)
    }
    actions.tertiary?.let { action ->
      SlotButton(action = action, slot = ActionSlot.Tertiary, onAction = onAction)
    }
  }
}

private enum class ActionSlot {
  Primary,
  Secondary,
  Tertiary,
}

@Composable
private fun SlotButton(
  action: EmptyAction,
  slot: ActionSlot,
  onAction: (EmptyAction) -> Unit,
) {
  val enabled = action.isEnabled && !action.isLoading
  val useLink = action.kind == EmptyActionKind.Link
  when {
    useLink || slot == ActionSlot.Tertiary -> TextButton(
      onClick = { onAction(action) },
      enabled = enabled,
    ) {
      ActionLabel(action)
    }
    slot == ActionSlot.Secondary -> OutlinedButton(
      onClick = { onAction(action) },
      enabled = enabled,
      modifier = Modifier.fillMaxWidth(),
    ) {
      ActionLabel(action)
    }
    else -> Button(
      onClick = { onAction(action) },
      enabled = enabled,
      modifier = Modifier.fillMaxWidth(),
    ) {
      ActionLabel(action)
    }
  }
}

@Composable
private fun ActionLabel(action: EmptyAction) {
  if (action.isLoading) {
    CircularProgressIndicator(
      modifier = Modifier.size(18.dp),
      strokeWidth = 2.dp,
    )
  } else {
    Text(action.title)
  }
}

internal fun shouldHide(configuration: EmptyConfiguration, isRefreshing: Boolean): Boolean {
  if (configuration.phase is EmptyPhase.Content) return true
  return configuration.phase is EmptyPhase.Loading &&
    configuration.presentation.skipsWhileRefreshing &&
    isRefreshing
}

internal fun truncateDescription(text: String, presentation: EmptyPresentation): String {
  val limit = presentation.maxDescriptionChars
  if (limit <= 0 || text.length <= limit) return text
  val suffix = presentation.truncationSuffix
  val keep = (limit - suffix.length).coerceAtLeast(0)
  return text.take(keep) + suffix
}

private fun scaledStyle(base: TextStyle, scale: Float): TextStyle {
  if (scale == 1f) return base
  val size = base.fontSize
  return if (size.isSp) {
    base.copy(fontSize = (size.value * scale).sp)
  } else {
    base
  }
}
