package com.fk.ui.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkShadows
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/** Nearest [ToastController] from [ToastHost]. */
val LocalToastController = compositionLocalOf<ToastController> {
  error("No ToastController provided. Wrap content in ToastHost { }.")
}

/**
 * Hosts [content] and draws the toast / HUD / snackbar overlay from [controller].
 *
 * Conceptually aligned with iOS `FKToast` window presenter (Compose overlay form).
 */
@Composable
fun ToastHost(
  controller: ToastController = rememberToastController(),
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  CompositionLocalProvider(LocalToastController provides controller) {
    Box(modifier = modifier.fillMaxSize()) {
      content()
      ToastOverlay(controller = controller)
    }
  }
}

@Composable
fun BoxScope.ToastOverlay(
  controller: ToastController,
) {
  val entries = controller.visible.toList()
  val blocking = entries.any { it.request.configuration.interceptTouches }
  if (blocking) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(fkColor(FkColorRole.Scrim).copy(alpha = 0.35f))
        .clickable(
          indication = null,
          interactionSource = remember { MutableInteractionSource() },
          onClick = {},
        ),
    )
  }

  entries.forEach { entry ->
    val position = entry.request.configuration.resolvedPosition()
    val alignment = when (position) {
      ToastPosition.Top -> Alignment.TopCenter
      ToastPosition.Center -> Alignment.Center
      ToastPosition.Bottom -> Alignment.BottomCenter
    }
    val insetModifier = when (position) {
      ToastPosition.Top -> Modifier.statusBarsPadding()
      ToastPosition.Bottom -> Modifier.navigationBarsPadding()
      ToastPosition.Center -> Modifier
    }
    AnimatedVisibility(
      visible = true,
      enter = when (entry.request.configuration.kind) {
        ToastKind.Snackbar -> slideInVertically { it } + fadeIn()
        else -> fadeIn() + scaleIn(initialScale = 0.92f)
      },
      modifier = Modifier
        .align(alignment)
        .then(insetModifier)
        .padding(fkMetrics().spacingM),
    ) {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        ToastCard(
          entry = entry,
          onDismiss = { controller.dismiss(entry.request.id) },
          onAction = { controller.invokeAction(entry.request.id) },
        )
      }
    }
  }
}

@Composable
private fun ToastCard(
  entry: ToastEntry,
  onDismiss: () -> Unit,
  onAction: () -> Unit,
) {
  val request = entry.request
  val config = request.configuration
  val metrics = fkMetrics()
  val shadow = fkShadows().elevationMedium
  val (container, onContainer) = toastColors(config.style)
  val shape = RoundedCornerShape(
    when (config.kind) {
      ToastKind.Snackbar -> metrics.radiusMedium
      else -> metrics.radiusLarge
    },
  )

  val dismissOnTap =
    config.kind != ToastKind.Snackbar &&
      !(config.kind == ToastKind.Hud && config.style == ToastStyle.Loading)

  val cardModifier = Modifier
    .then(
      if (config.kind == ToastKind.Snackbar) {
        Modifier.fillMaxWidth()
      } else {
        Modifier.widthIn(max = config.maxWidth)
      },
    )
    .shadow(
      elevation = shadow.radius,
      shape = shape,
      ambientColor = shadow.color.copy(alpha = shadow.alpha),
      spotColor = shadow.color.copy(alpha = shadow.alpha),
    )
    .clip(shape)
    .background(container)
    .then(
      if (dismissOnTap) {
        Modifier.clickable(
          indication = null,
          interactionSource = remember { MutableInteractionSource() },
          onClick = onDismiss,
        )
      } else {
        Modifier
      },
    )
    .padding(metrics.spacingM)
    .semantics {
      contentDescription = buildString {
        request.title?.let { append(it).append(". ") }
        append(request.message)
      }
    }

  when (config.kind) {
    ToastKind.Snackbar -> {
      Row(
        modifier = cardModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          request.title?.let {
            Text(
              text = it,
              style = fkTextStyle(FkTextStyle.Subheadline),
              color = onContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = request.message,
            style = fkTextStyle(FkTextStyle.Footnote),
            color = onContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        request.actionLabel?.let { label ->
          TextButton(onClick = onAction) {
            Text(label, color = onContainer)
          }
        }
      }
    }
    ToastKind.Hud, ToastKind.Toast -> {
      Column(
        modifier = cardModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        if (config.style == ToastStyle.Loading) {
          CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = onContainer,
            strokeWidth = 2.dp,
          )
        }
        request.title?.let {
          Text(
            text = it,
            style = fkTextStyle(FkTextStyle.Headline),
            color = onContainer,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = request.message,
          style = fkTextStyle(FkTextStyle.Subheadline),
          color = onContainer,
          textAlign = TextAlign.Center,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun toastColors(style: ToastStyle): Pair<Color, Color> {
  val elevated = fkColor(FkColorRole.SurfaceElevated)
  val onSurface = fkColor(FkColorRole.OnSurface)
  return when (style) {
    ToastStyle.Normal, ToastStyle.Loading -> elevated to onSurface
    ToastStyle.Success -> contrastingOn(fkStatusColor(FkStatusSemantic.Success))
    ToastStyle.Error -> contrastingOn(fkStatusColor(FkStatusSemantic.Error))
    ToastStyle.Warning -> contrastingOn(fkStatusColor(FkStatusSemantic.Warning))
    ToastStyle.Info -> contrastingOn(fkStatusColor(FkStatusSemantic.Info))
  }
}

private fun contrastingOn(background: Color): Pair<Color, Color> {
  val on = if (background.luminance() > 0.5f) Color.Black else Color.White
  return background to on
}
