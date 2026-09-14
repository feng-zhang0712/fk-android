package com.fk.sample.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle
import com.fk.ui.widget.AvatarSize
import com.fk.ui.widget.ChipItem
import com.fk.ui.widget.ChipMode
import com.fk.ui.widget.ChipSelectionMode
import com.fk.ui.widget.ChipSize
import com.fk.ui.widget.FkAvatar
import com.fk.ui.widget.FkChip
import com.fk.ui.widget.FkChipGroup
import com.fk.ui.widget.FkPresenceDot
import com.fk.ui.widget.FkStatusPill
import com.fk.ui.widget.FkTag
import com.fk.ui.widget.PresenceState
import com.fk.ui.widget.StatusPillSize
import com.fk.ui.widget.TagVariant
import com.fk.ui.widget.WidgetKit

/**
 * Demo for Phase G3 widgets (Avatar / Chip / Tag / StatusPill).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetDemoScreen(
  onBack: () -> Unit,
) {
  val metrics = fkMetrics()
  var selected by remember { mutableStateOf(setOf("all")) }
  var tokens by remember {
    mutableStateOf(
      listOf(
        ChipItem(id = "android", title = "Android", removable = true),
        ChipItem(id = "compose", title = "Compose", removable = true),
      ),
    )
  }

  Scaffold(
    topBar = { SampleTopBar(title = "Widget", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(metrics.spacingM),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingM),
    ) {
      Text(
        text = "WidgetKit ${WidgetKit.VERSION}",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )

      Section("Avatar")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingM),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        FkAvatar(displayName = "Ada Lovelace", size = AvatarSize.S, presence = PresenceState.Online)
        FkAvatar(
          displayName = "Grace Hopper",
          size = AvatarSize.M,
          verified = true,
          presence = PresenceState.Away,
        )
        FkAvatar(displayName = "张三", size = AvatarSize.L, presence = PresenceState.Busy)
        FkAvatar(displayName = null, size = AvatarSize.M)
      }
      Text(
        text = "Presence dots",
        style = fkTextStyle(FkTextStyle.Caption2),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(metrics.spacingS)) {
        PresenceState.entries.forEach { state ->
          FkPresenceDot(state = state)
        }
      }

      HorizontalDivider(color = fkColor(FkColorRole.Outline))
      Section("StatusPill")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        FkStatusPill("Shipped", style = FkStatusSemantic.Success, showsDot = true)
        FkStatusPill("Pending", style = FkStatusSemantic.Warning, showsDot = true)
        FkStatusPill("Failed", style = FkStatusSemantic.Error, showsDot = true)
        FkStatusPill("Processing", style = FkStatusSemantic.Info, showsDot = true)
        FkStatusPill("Draft", style = FkStatusSemantic.Neutral)
        FkStatusPill("Large", style = FkStatusSemantic.Success, size = StatusPillSize.M, showsDot = true)
      }

      HorizontalDivider(color = fkColor(FkColorRole.Outline))
      Section("Tag")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        TagVariant.entries.forEach { variant ->
          FkTag(title = variant.name, variant = variant)
        }
      }

      HorizontalDivider(color = fkColor(FkColorRole.Outline))
      Section("Chip (Material façade)")
      Text(
        text = "FkChip wraps FilterChip / InputChip / SuggestionChip with FK colors. " +
          "Prefer Material directly for one-offs; use FkChipGroup for selection rules.",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        FkChip(title = "Unselected", mode = ChipMode.Filter, selected = false, onSelectedChange = {})
        FkChip(title = "Selected", mode = ChipMode.Filter, selected = true, onSelectedChange = {})
        FkChip(title = "Try Compose", mode = ChipMode.Suggestion, onClick = {})
      }
      FlowRow(horizontalArrangement = Arrangement.spacedBy(metrics.spacingS)) {
        tokens.forEach { token ->
          FkChip(
            title = token.title,
            mode = ChipMode.Input,
            size = ChipSize.S,
            onRemove = { tokens = tokens.filterNot { it.id == token.id } },
          )
        }
      }

      HorizontalDivider(color = fkColor(FkColorRole.Outline))
      Section("ChipGroup (single)")
      FkChipGroup(
        chips = listOf(
          ChipItem("all", "All"),
          ChipItem("sale", "Sale"),
          ChipItem("new", "New"),
          ChipItem("gift", "Gift", enabled = false),
        ),
        selectedIds = selected,
        onSelectionChange = { selected = it },
        selectionMode = ChipSelectionMode.Single,
        chipSize = ChipSize.M,
        modifier = Modifier.fillMaxWidth(),
      )
      Text(
        text = "selected=$selected",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
    }
  }
}

@Composable
private fun Section(title: String) {
  Text(
    text = title,
    style = fkTextStyle(FkTextStyle.Title3).copy(fontWeight = FontWeight.SemiBold),
    color = fkColor(FkColorRole.OnSurface),
  )
}
