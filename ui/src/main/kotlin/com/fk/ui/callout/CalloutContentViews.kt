package com.fk.ui.callout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.fkColor

@Composable
internal fun CalloutContentBody(
  content: CalloutContent,
  configuration: CalloutConfiguration,
  onSurface: Color,
  secondary: Color,
  onAction: (String) -> Unit,
  onClose: () -> Unit,
  onMenuSelect: (CalloutMenuItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scroll = rememberScrollState()
  val heightCap = configuration.maxContentHeight
  val scrollModifier = if (heightCap != null) {
    Modifier
      .heightIn(max = heightCap)
      .verticalScroll(scroll)
  } else {
    Modifier
  }

  Column(modifier = modifier.then(scrollModifier)) {
    when (content) {
      is CalloutContent.Message -> {
        Text(
          text = content.text,
          color = onSurface,
          fontSize = configuration.bodyTextSize,
        )
      }
      is CalloutContent.TitleSubtitle -> {
        Text(
          text = content.title,
          color = onSurface,
          fontSize = configuration.titleTextSize,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = content.message,
          color = secondary,
          fontSize = configuration.bodyTextSize,
        )
      }
      is CalloutContent.IconMessage -> {
        Row(verticalAlignment = Alignment.Top) {
          content.icon.imageVector?.let { vector ->
            Icon(
              imageVector = vector,
              contentDescription = content.icon.contentDescription,
              tint = content.icon.tint ?: onSurface,
              modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
          }
          Text(
            text = content.message,
            color = onSurface,
            fontSize = configuration.bodyTextSize,
            modifier = Modifier.weight(1f, fill = false),
          )
        }
      }
      is CalloutContent.MessageWithActions -> {
        Text(
          text = content.message,
          color = onSurface,
          fontSize = configuration.bodyTextSize,
        )
        Spacer(Modifier.height(10.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          content.actions.forEach { action ->
            TextButton(
              onClick = { onAction(action.id) },
              modifier = Modifier.semantics {
                contentDescription = action.accessibilityLabel ?: action.title
              },
            ) {
              Text(
                text = action.title,
                fontWeight = if (action.style == CalloutAction.Style.Primary) {
                  FontWeight.SemiBold
                } else {
                  FontWeight.Normal
                },
              )
            }
          }
        }
      }
      is CalloutContent.HeaderPanel -> HeaderPanelBody(
        header = content.header,
        body = content.body,
        configuration = configuration,
        onSurface = onSurface,
        secondary = secondary,
      )
      is CalloutContent.CoachMark -> CoachMarkBody(
        content = content.content,
        configuration = configuration,
        onSurface = onSurface,
        secondary = secondary,
        onPrimary = { onAction(content.content.primaryActionTitle) },
        onClose = onClose,
      )
      is CalloutContent.Menu -> MenuBody(
        menu = content.menu,
        onSurface = onSurface,
        secondary = secondary,
        onSelect = onMenuSelect,
      )
      is CalloutContent.Custom -> content.content()
    }
  }
}

@Composable
private fun HeaderPanelBody(
  header: CalloutHeaderPanel,
  body: String,
  configuration: CalloutConfiguration,
  onSurface: Color,
  secondary: Color,
) {
  val headerBg = header.backgroundColor ?: fkColor(FkColorRole.Surface).copy(alpha = 0.9f)
  val headerFg = header.textColor ?: onSurface
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = header.title,
      color = headerFg,
      fontSize = configuration.titleTextSize,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(headerBg)
        .padding(horizontal = 12.dp, vertical = 10.dp),
    )
    Spacer(Modifier.height(10.dp))
    Text(
      text = body,
      color = secondary,
      fontSize = configuration.bodyTextSize,
    )
  }
}

@Composable
private fun CoachMarkBody(
  content: CalloutCoachMarkContent,
  configuration: CalloutConfiguration,
  onSurface: Color,
  secondary: Color,
  onPrimary: () -> Unit,
  onClose: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = content.title,
        color = onSurface,
        fontSize = configuration.titleTextSize,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (content.showsCloseButton) {
        TextButton(onClick = onClose) {
          Text("✕", color = secondary)
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = content.message,
      color = secondary,
      fontSize = configuration.bodyTextSize,
    )
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onPrimary) {
      Text(content.primaryActionTitle, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun MenuBody(
  menu: CalloutMenu,
  onSurface: Color,
  secondary: Color,
  onSelect: (CalloutMenuItem) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    menu.header?.let { header ->
      Text(
        text = header,
        color = secondary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      HorizontalDivider(color = secondary.copy(alpha = 0.2f))
    }
    menu.sections.forEachIndexed { sectionIndex, section ->
      if (sectionIndex > 0) {
        HorizontalDivider(
          color = secondary.copy(alpha = 0.2f),
          modifier = Modifier.padding(vertical = 4.dp),
        )
      }
      section.items.forEach { item ->
        MenuRow(item = item, onSurface = onSurface, secondary = secondary, onSelect = onSelect)
      }
    }
  }
}

@Composable
private fun MenuRow(
  item: CalloutMenuItem,
  onSurface: Color,
  secondary: Color,
  onSelect: (CalloutMenuItem) -> Unit,
) {
  val tint = when {
    !item.isEnabled -> secondary.copy(alpha = 0.4f)
    item.isDestructive -> fkColor(FkColorRole.Destructive)
    item.tint != null -> item.tint
    else -> onSurface
  }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable(enabled = item.isEnabled) { onSelect(item) }
      .padding(horizontal = 12.dp, vertical = 10.dp),
  ) {
    item.icon?.let { icon: ImageVector ->
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(20.dp),
      )
      Spacer(Modifier.width(10.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(text = item.title, color = tint, fontWeight = FontWeight.Medium)
      item.subtitle?.let {
        Spacer(Modifier.height(2.dp))
        Text(text = it, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
    if (item.isSelected) {
      Text("✓", color = tint, modifier = Modifier.padding(start = 8.dp))
    }
  }
}
