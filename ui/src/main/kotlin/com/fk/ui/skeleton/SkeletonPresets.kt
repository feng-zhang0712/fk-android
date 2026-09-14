package com.fk.ui.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.fkMetrics

/**
 * Opinionated skeleton layouts.
 *
 * Conceptually aligned with iOS `FKSkeletonPresets`.
 */
object SkeletonPresets {
  /**
   * List row: avatar + title + subtitle lines.
   *
   * Recommended LazyColumn item height ≈ 68.dp (matches iOS list placeholder guidance).
   */
  @Composable
  fun ListRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 44.dp,
    avatarStyle: SkeletonAvatarStyle = SkeletonAvatarStyle.Circle,
    configuration: SkeletonConfiguration = LocalSkeletonConfiguration.current,
  ) {
    val metrics = fkMetrics()
    Row(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = metrics.spacingM, vertical = metrics.spacingS),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(configuration.itemSpacing),
    ) {
      when (avatarStyle) {
        SkeletonAvatarStyle.Circle ->
          SkeletonCircle(size = avatarSize, configuration = configuration)
        SkeletonAvatarStyle.Rounded ->
          SkeletonBlock(
            width = avatarSize,
            height = avatarSize,
            shape = RoundedCornerShape(configuration.cornerRadius),
            configuration = configuration,
          )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(configuration.lineSpacing),
      ) {
        SkeletonBlock(
          modifier = Modifier.fillMaxWidth(0.72f),
          height = configuration.lineHeight + 2.dp,
          configuration = configuration,
        )
        SkeletonBlock(
          modifier = Modifier.fillMaxWidth(0.48f),
          height = configuration.lineHeight,
          configuration = configuration,
        )
      }
    }
  }

  /** Multi-line text column. */
  @Composable
  fun TextBlock(
    modifier: Modifier = Modifier,
    lines: Int = 3,
    configuration: SkeletonConfiguration = LocalSkeletonConfiguration.current,
  ) {
    Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(configuration.lineSpacing),
    ) {
      repeat(lines.coerceAtLeast(1)) { index ->
        val fraction = when {
          index == lines - 1 && lines > 1 -> 0.6f
          else -> 1f
        }
        SkeletonBlock(
          modifier = Modifier.fillMaxWidth(fraction),
          height = configuration.lineHeight,
          configuration = configuration,
        )
      }
    }
  }

  /** Card: banner + two text lines. */
  @Composable
  fun Card(
    modifier: Modifier = Modifier,
    bannerHeight: Dp = 120.dp,
    configuration: SkeletonConfiguration = LocalSkeletonConfiguration.current,
  ) {
    val metrics = fkMetrics()
    Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(configuration.itemSpacing),
    ) {
      SkeletonBlock(
        modifier = Modifier.fillMaxWidth(),
        height = bannerHeight,
        shape = RoundedCornerShape(configuration.cornerRadius),
        configuration = configuration,
      )
      Column(
        modifier = Modifier.padding(horizontal = metrics.spacingXs),
        verticalArrangement = Arrangement.spacedBy(configuration.lineSpacing),
      ) {
        SkeletonBlock(
          modifier = Modifier.fillMaxWidth(0.85f),
          height = configuration.lineHeight + 2.dp,
          configuration = configuration,
        )
        SkeletonBlock(
          modifier = Modifier.fillMaxWidth(0.55f),
          height = configuration.lineHeight,
          configuration = configuration,
        )
      }
    }
  }

  /** Grid tile: square media + label. */
  @Composable
  fun GridCell(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    configuration: SkeletonConfiguration = LocalSkeletonConfiguration.current,
  ) {
    Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(configuration.lineSpacing),
    ) {
      SkeletonBlock(
        width = size,
        height = size,
        shape = RoundedCornerShape(configuration.cornerRadius),
        configuration = configuration,
      )
      SkeletonBlock(
        width = size * 0.75f,
        height = configuration.lineHeight,
        configuration = configuration,
      )
    }
  }
}

/**
 * Lazy list of [SkeletonPresets.ListRow] placeholders.
 *
 * Used by Phase E1 `list` for [com.fk.ui.list.ListPresentationState.InitialLoading]
 * (`presetRows`-style placeholders).
 */
@Composable
fun SkeletonListPlaceholder(
  count: Int = 6,
  modifier: Modifier = Modifier,
  avatarStyle: SkeletonAvatarStyle = SkeletonAvatarStyle.Circle,
  configuration: SkeletonConfiguration = LocalSkeletonConfiguration.current,
) {
  val rows = (0 until count.coerceAtLeast(0)).toList()
  LazyColumn(modifier = modifier) {
    items(rows, key = { it }) {
      SkeletonPresets.ListRow(
        avatarStyle = avatarStyle,
        configuration = configuration,
      )
    }
  }
}
