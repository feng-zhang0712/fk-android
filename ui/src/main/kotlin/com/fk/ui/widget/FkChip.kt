package com.fk.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Thin façade over Material 3 chips with FK theme colors.
 *
 * Prefer Material `FilterChip` / `InputChip` / `SuggestionChip` directly when a
 * one-off screen does not need FK visual defaults or [FkChipGroup] selection rules.
 * Use this API for cross-app consistency and iOS `FKChip` mode parity.
 *
 * | [ChipMode] | Material backing |
 * |------------|------------------|
 * | Filter / Choice | [FilterChip] |
 * | Input | [InputChip] |
 * | Suggestion | [SuggestionChip] |
 */
@Composable
fun FkChip(
  title: String,
  modifier: Modifier = Modifier,
  mode: ChipMode = ChipMode.Filter,
  selected: Boolean = false,
  enabled: Boolean = true,
  size: ChipSize = ChipSize.M,
  onClick: (() -> Unit)? = null,
  onSelectedChange: ((Boolean) -> Unit)? = null,
  onRemove: (() -> Unit)? = null,
) {
  val label: @Composable () -> Unit = {
    Text(
      text = title,
      style = chipLabelStyle(size).copy(
        fontWeight = if (selected && mode != ChipMode.Input) FontWeight.SemiBold else FontWeight.Medium,
      ),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }

  when (mode) {
    ChipMode.Filter, ChipMode.Choice -> {
      val primary = fkColor(FkColorRole.Primary)
      FilterChip(
        selected = selected,
        onClick = {
          when {
            onSelectedChange != null -> onSelectedChange(!selected)
            onClick != null -> onClick()
          }
        },
        label = label,
        enabled = enabled,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = primary.copy(alpha = 0.12f),
          selectedLabelColor = primary,
          selectedLeadingIconColor = primary,
          containerColor = fkColor(FkColorRole.Surface),
          labelColor = fkColor(FkColorRole.OnSurface),
          disabledContainerColor = fkColor(FkColorRole.SurfaceElevated),
          disabledLabelColor = fkColor(FkColorRole.OnSurfaceSecondary),
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = enabled,
          selected = selected,
          borderColor = fkColor(FkColorRole.Outline),
          selectedBorderColor = primary,
          disabledBorderColor = fkColor(FkColorRole.Outline),
          disabledSelectedBorderColor = fkColor(FkColorRole.Outline),
        ),
      )
    }
    ChipMode.Input -> {
      InputChip(
        selected = false,
        onClick = { onClick?.invoke() },
        label = label,
        enabled = enabled,
        modifier = modifier,
        trailingIcon = if (onRemove != null && enabled) {
          {
            Text(
              text = "×",
              style = fkTextStyle(FkTextStyle.Subheadline),
              color = fkColor(FkColorRole.OnSurfaceSecondary),
              modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(horizontal = 2.dp),
            )
          }
        } else {
          null
        },
        colors = InputChipDefaults.inputChipColors(
          containerColor = fkColor(FkColorRole.Surface),
          labelColor = fkColor(FkColorRole.OnSurface),
          disabledContainerColor = fkColor(FkColorRole.SurfaceElevated),
          disabledLabelColor = fkColor(FkColorRole.OnSurfaceSecondary),
        ),
        border = InputChipDefaults.inputChipBorder(
          enabled = enabled,
          selected = false,
          borderColor = fkColor(FkColorRole.Outline),
          disabledBorderColor = fkColor(FkColorRole.Outline),
        ),
      )
    }
    ChipMode.Suggestion -> {
      SuggestionChip(
        onClick = { onClick?.invoke() },
        label = label,
        enabled = enabled,
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(
          containerColor = fkColor(FkColorRole.Surface),
          labelColor = fkColor(FkColorRole.OnSurface),
          disabledContainerColor = fkColor(FkColorRole.SurfaceElevated),
          disabledLabelColor = fkColor(FkColorRole.OnSurfaceSecondary),
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
          enabled = enabled,
          borderColor = fkColor(FkColorRole.Outline),
          disabledBorderColor = fkColor(FkColorRole.Outline),
        ),
      )
    }
  }
}

/**
 * Read-only metadata / promo / role tag (iOS `FKTag`).
 *
 * Material has no dedicated Tag; do not use disabled [FilterChip] for this.
 * Not for workflow status — use [FkStatusPill].
 */
@Composable
fun FkTag(
  title: String,
  modifier: Modifier = Modifier,
  variant: TagVariant = TagVariant.Neutral,
  size: ChipSize = ChipSize.S,
) {
  val metrics = fkMetrics()
  val (bg, border, fg) = tagColors(variant)
  Text(
    text = title,
    style = chipLabelStyle(size).copy(fontWeight = FontWeight.Medium),
    color = fg,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
      .heightIn(min = size.height)
      .clip(metrics.shapeFull)
      .background(bg)
      .border(1.dp, border, metrics.shapeFull)
      .padding(
        horizontal = if (size == ChipSize.Xs) 8.dp else 10.dp,
        vertical = if (size == ChipSize.Xs) 2.dp else 4.dp,
      ),
  )
}

/**
 * Flow layout + selection orchestration (iOS `FKChipGroup`).
 *
 * This is the main reason to keep a Chip façade: Material has no group controller.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FkChipGroup(
  chips: List<ChipItem>,
  selectedIds: Set<String>,
  onSelectionChange: (Set<String>) -> Unit,
  modifier: Modifier = Modifier,
  selectionMode: ChipSelectionMode = ChipSelectionMode.Single,
  chipMode: ChipMode = ChipMode.Filter,
  chipSize: ChipSize = ChipSize.M,
  onSelectionLimitReached: (() -> Unit)? = null,
  onPrimaryAction: ((String) -> Unit)? = null,
  onRemove: ((String) -> Unit)? = null,
) {
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    chips.forEach { item ->
      val selected = item.id in selectedIds
      FkChip(
        title = item.title,
        mode = chipMode,
        selected = selected,
        enabled = item.enabled,
        size = chipSize,
        onClick = {
          when (chipMode) {
            ChipMode.Suggestion -> onPrimaryAction?.invoke(item.id)
            ChipMode.Input -> onPrimaryAction?.invoke(item.id)
            else -> Unit
          }
        },
        onSelectedChange = if (chipMode == ChipMode.Filter || chipMode == ChipMode.Choice) {
          {
            onSelectionChange(
              nextSelection(
                current = selectedIds,
                id = item.id,
                mode = selectionMode,
                onLimit = onSelectionLimitReached,
              ),
            )
          }
        } else {
          null
        },
        onRemove = if (chipMode == ChipMode.Input && item.removable) {
          { onRemove?.invoke(item.id) }
        } else {
          null
        },
      )
    }
  }
}

internal fun nextSelection(
  current: Set<String>,
  id: String,
  mode: ChipSelectionMode,
  onLimit: (() -> Unit)?,
): Set<String> =
  when (mode) {
    ChipSelectionMode.None -> current
    ChipSelectionMode.Single ->
      if (id in current) emptySet() else setOf(id)
    is ChipSelectionMode.Multiple -> {
      if (id in current) {
        current - id
      } else {
        val max = mode.max
        if (max != null && current.size >= max) {
          onLimit?.invoke()
          current
        } else {
          current + id
        }
      }
    }
  }

@Composable
private fun chipLabelStyle(size: ChipSize) =
  when (size) {
    ChipSize.Xs -> fkTextStyle(FkTextStyle.Caption2)
    ChipSize.S -> fkTextStyle(FkTextStyle.Caption1)
    ChipSize.M -> fkTextStyle(FkTextStyle.Subheadline)
  }

@Composable
private fun tagColors(variant: TagVariant): Triple<Color, Color, Color> {
  val outline = fkColor(FkColorRole.Outline)
  return when (variant) {
    TagVariant.Neutral -> Triple(
      fkColor(FkColorRole.SurfaceElevated),
      outline,
      fkColor(FkColorRole.OnSurfaceSecondary),
    )
    TagVariant.Brand -> {
      val c = fkColor(FkColorRole.Primary)
      Triple(c.copy(alpha = 0.12f), c, c)
    }
    TagVariant.Success -> semanticTag(FkStatusSemantic.Success)
    TagVariant.Warning -> semanticTag(FkStatusSemantic.Warning)
    TagVariant.Error -> semanticTag(FkStatusSemantic.Error)
    TagVariant.Outline -> Triple(
      Color.Transparent,
      outline,
      fkColor(FkColorRole.OnSurface),
    )
  }
}

@Composable
private fun semanticTag(semantic: FkStatusSemantic): Triple<Color, Color, Color> {
  val c = fkStatusColor(semantic)
  return Triple(c.copy(alpha = 0.12f), c.copy(alpha = 0.4f), c)
}
