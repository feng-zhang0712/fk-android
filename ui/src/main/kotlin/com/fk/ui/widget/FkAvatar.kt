package com.fk.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkStatusColor

/**
 * Profile / list avatar with initials fallback, optional URL image, verified badge, and presence.
 *
 * Conceptually aligned with iOS `FKAvatar` (Compose).
 *
 * Initials stay under the image so loading / failed URL loads still show a readable fallback.
 */
@Composable
fun FkAvatar(
  displayName: String?,
  modifier: Modifier = Modifier,
  imageUrl: String? = null,
  size: AvatarSize = AvatarSize.M,
  shape: AvatarShape = AvatarShape.Circle,
  verified: Boolean = false,
  presence: PresenceState? = null,
  contentDescription: String? = displayName,
  onClick: (() -> Unit)? = null,
) {
  val diameter = size.diameter
  val clipShape = avatarShape(shape, diameter)
  val initials = remember(displayName) { avatarInitials(displayName) }
  val placeholderColor = remember(displayName) { avatarPlaceholderColor(displayName) }

  Box(modifier = modifier.size(diameter)) {
    val bodyModifier = Modifier
      .fillMaxSize()
      .clip(clipShape)
      .background(placeholderColor)
      .then(
        if (onClick != null) {
          Modifier.clickable(onClick = onClick)
        } else {
          Modifier
        },
      )
    Box(
      modifier = bodyModifier,
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initials.ifEmpty { "?" },
        color = Color.White,
        fontSize = (diameter.value * 0.36f).sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
      )
      if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    if (verified) {
      val badge = (diameter * 0.28f).coerceAtLeast(8.dp)
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = 1.dp, y = 1.dp)
          .size(badge)
          .clip(CircleShape)
          .background(fkStatusColor(FkStatusSemantic.Info))
          .border(1.5.dp, fkColor(FkColorRole.Surface), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "✓",
          color = Color.White,
          fontSize = (badge.value * 0.55f).sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
      }
    }

    presence?.let { state ->
      FkPresenceDot(
        state = state,
        size = when (size) {
          AvatarSize.Xs, AvatarSize.S -> PresenceDotSize.S
          AvatarSize.M, AvatarSize.L -> PresenceDotSize.M
          AvatarSize.Xl -> PresenceDotSize.L
        },
        modifier = Modifier
          .align(Alignment.BottomStart)
          .offset(x = (-1).dp, y = 1.dp),
      )
    }
  }
}

/**
 * Standalone presence indicator (iOS `FKPresenceIndicator`).
 */
@Composable
fun FkPresenceDot(
  state: PresenceState,
  modifier: Modifier = Modifier,
  size: PresenceDotSize = PresenceDotSize.M,
) {
  Box(
    modifier = modifier
      .size(size.diameter)
      .clip(CircleShape)
      .background(presenceColor(state))
      .border(1.dp, fkColor(FkColorRole.Surface), CircleShape),
  )
}

internal fun avatarInitials(displayName: String?): String {
  val name = displayName?.trim().orEmpty()
  if (name.isEmpty()) return ""
  val parts = name.split(Regex("\\s+")).filter { it.isNotEmpty() }
  val first = parts.first()
  val firstChar = first.first()
  return if (firstChar.code < 128 && parts.size >= 2) {
    buildString {
      append(parts[0].first().uppercaseChar())
      append(parts[1].first().uppercaseChar())
    }
  } else {
    firstChar.toString().uppercase()
  }
}

private fun avatarPlaceholderColor(displayName: String?): Color {
  val palette = listOf(
    Color(0xFF5B8DEF),
    Color(0xFF6FCF97),
    Color(0xFFF2C94C),
    Color(0xFFEB5757),
    Color(0xFFBB6BD9),
    Color(0xFF56CCF2),
  )
  val key = displayName?.trim().orEmpty()
  if (key.isEmpty()) return palette[0]
  val index = (key.hashCode().and(Int.MAX_VALUE)) % palette.size
  return palette[index]
}

private fun avatarShape(shape: AvatarShape, diameter: Dp): Shape =
  when (shape) {
    AvatarShape.Circle -> CircleShape
    AvatarShape.RoundedRect -> RoundedCornerShape(diameter * 0.22f)
  }

@Composable
private fun presenceColor(state: PresenceState): Color =
  when (state) {
    PresenceState.Online -> fkStatusColor(FkStatusSemantic.Success)
    PresenceState.Away -> fkStatusColor(FkStatusSemantic.Warning)
    PresenceState.Busy -> fkStatusColor(FkStatusSemantic.Error)
    PresenceState.Offline -> fkStatusColor(FkStatusSemantic.Neutral)
  }
