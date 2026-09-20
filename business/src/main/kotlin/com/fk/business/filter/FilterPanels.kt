package com.fk.business.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkSpacingToken
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Renders the panel for [content] and routes selection mutations back to the host.
 */
@Composable
internal fun FilterPanelBody(
  content: FilterPanelContent,
  allowsMultipleSelection: Boolean,
  emptyLabel: String,
  onContentChange: (FilterPanelContent) -> Unit,
  onSelection: (FilterPanelSelection) -> Unit,
  modifier: Modifier = Modifier,
) {
  val appearance = LocalFilterAppearance.current
  when (content) {
    is FilterPanelContent.Hierarchy ->
      TwoColumnListPanel(
        model = content.model,
        config = content.config,
        allowsMultipleSelection = allowsMultipleSelection,
        emptyLabel = emptyLabel,
        onModelChange = { onContentChange(content.copy(model = it)) },
        onSelection = onSelection,
        modifier = modifier,
        gridStyle = false,
        pillStyle = appearance.directoryPillStyle,
      )
    is FilterPanelContent.DualHierarchy ->
      TwoColumnListPanel(
        model = content.model,
        config = content.config,
        allowsMultipleSelection = allowsMultipleSelection,
        emptyLabel = emptyLabel,
        onModelChange = { onContentChange(content.copy(model = it)) },
        onSelection = onSelection,
        modifier = modifier,
        gridStyle = true,
        pillStyle = appearance.directoryPillStyle,
      )
    is FilterPanelContent.Tags ->
      TagsPanel(
        sections = content.sections,
        columns = content.columns.takeIf { it > 0 } ?: appearance.tagsPillStyle.columns,
        allowsMultipleSelection = allowsMultipleSelection,
        emptyLabel = emptyLabel,
        onSectionsChange = { onContentChange(content.copy(sections = it)) },
        onSelection = onSelection,
        modifier = modifier,
        pillStyle = appearance.tagsPillStyle,
      )
    is FilterPanelContent.SingleList ->
      SingleListPanel(
        section = content.section,
        allowsMultipleSelection = allowsMultipleSelection,
        emptyLabel = emptyLabel,
        onSectionChange = { onContentChange(content.copy(section = it)) },
        onSelection = onSelection,
        modifier = modifier,
        textCentered = appearance.listTextCentered,
      )
    is FilterPanelContent.Custom -> {
      // Host renders [FilterHost] customPanel; this branch is a safe fallback only.
      Box(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
          text = emptyLabel,
          style = fkTextStyle(FkTextStyle.Body),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
      }
    }
  }
}

@Composable
private fun TwoColumnListPanel(
  model: TwoColumnFilterModel,
  config: TwoColumnPanelConfig,
  allowsMultipleSelection: Boolean,
  emptyLabel: String,
  onModelChange: (TwoColumnFilterModel) -> Unit,
  onSelection: (FilterPanelSelection) -> Unit,
  modifier: Modifier = Modifier,
  gridStyle: Boolean,
  pillStyle: FilterPillStyle,
) {
  val metrics = fkMetrics()
  val categoryId = model.selectedCategoryId
  val sections = categoryId?.let { model.sectionsByCategoryId[it].orEmpty() }.orEmpty()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .fillMaxHeight(),
  ) {
    LazyColumn(
      modifier = Modifier
        .width(112.dp)
        .fillMaxHeight()
        .background(fkColor(FkColorRole.SurfaceElevated)),
    ) {
      items(model.categories, key = { it.id.raw }) { category ->
        val selected = category.isSelected || category.id == categoryId
        Text(
          text = category.title,
          style = fkTextStyle(FkTextStyle.Subheadline),
          fontWeight = FontWeight.Normal,
          color = if (selected) {
            fkColor(FkColorRole.Primary)
          } else {
            fkColor(FkColorRole.OnSurface)
          },
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              val result = FilterSelection.selectCategory(model, category.id)
              onModelChange(result.model)
              if (config.reselectBehavior.shouldFire(result.selectionChanged)) {
                result.emptyCategorySelection?.let(onSelection)
              }
            }
            .background(
              if (selected) fkColor(FkColorRole.Surface) else Color.Transparent,
            )
            .padding(
              horizontal = metrics.spacing(FkSpacingToken.S),
              vertical = metrics.spacing(FkSpacingToken.M),
            ),
        )
      }
    }
    VerticalDivider(color = fkColor(FkColorRole.Outline))
    if (sections.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .padding(metrics.spacing(FkSpacingToken.M)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = emptyLabel,
          style = fkTextStyle(FkTextStyle.Body),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
      }
    } else if (gridStyle) {
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .verticalScroll(rememberScrollState())
          .padding(metrics.spacing(FkSpacingToken.S)),
        verticalArrangement = Arrangement.spacedBy(metrics.spacing(FkSpacingToken.M)),
      ) {
        sections.forEach { section ->
          TwoColumnChipSection(
            section = section,
            config = config,
            headerSelected = model.selectedHeaderSectionId == section.id,
            pillStyle = pillStyle,
            onHeaderTap = {
              handleHeaderTap(model, categoryId!!, section, config, onModelChange, onSelection)
            },
            onItemTap = { item ->
              handleItemTap(
                model = model,
                categoryId = categoryId!!,
                section = section,
                item = item,
                allowsMultipleSelection = allowsMultipleSelection,
                config = config,
                onModelChange = onModelChange,
                onSelection = onSelection,
              )
            },
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
      ) {
        sections.forEach { section ->
          item(key = "header-${section.id.raw}") {
            TwoColumnSectionHeader(
              section = section,
              config = config,
              headerSelected = model.selectedHeaderSectionId == section.id,
              onTap = {
                handleHeaderTap(model, categoryId!!, section, config, onModelChange, onSelection)
              },
            )
          }
          if (!section.isCollapsed) {
            items(section.items, key = { "${section.id.raw}-${it.id.raw}" }) { item ->
              FilterListRow(
                item = item,
                selected = item.isSelected,
                onClick = {
                  handleItemTap(
                    model = model,
                    categoryId = categoryId!!,
                    section = section,
                    item = item,
                    allowsMultipleSelection = allowsMultipleSelection,
                    config = config,
                    onModelChange = onModelChange,
                    onSelection = onSelection,
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TwoColumnChipSection(
  section: FilterSection,
  config: TwoColumnPanelConfig,
  headerSelected: Boolean,
  pillStyle: FilterPillStyle,
  onHeaderTap: () -> Unit,
  onItemTap: (FilterOptionItem) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    TwoColumnSectionHeader(
      section = section,
      config = config,
      headerSelected = headerSelected,
      onTap = onHeaderTap,
    )
    if (!section.isCollapsed) {
      FilterChipFlow(
        items = section.items,
        columns = pillStyle.columns.coerceAtLeast(1),
        pillStyle = pillStyle,
        onItemTap = onItemTap,
      )
    }
  }
}

@Composable
private fun TwoColumnSectionHeader(
  section: FilterSection,
  config: TwoColumnPanelConfig,
  headerSelected: Boolean,
  onTap: () -> Unit,
) {
  val title = section.title ?: return
  val interactive = config.rightHeaderBehavior != TwoColumnRightHeaderBehavior.Standard
  val selected = config.rightHeaderBehavior == TwoColumnRightHeaderBehavior.SelectableHeader &&
    headerSelected
  Text(
    text = when (config.rightHeaderBehavior) {
      TwoColumnRightHeaderBehavior.ToggleCollapse ->
        if (section.isCollapsed) "▸ $title" else "▾ $title"
      else -> title
    },
    style = fkTextStyle(FkTextStyle.Subheadline),
    fontWeight = FontWeight.Normal,
    color = if (selected) fkColor(FkColorRole.Primary) else fkColor(FkColorRole.OnSurface),
    modifier = Modifier
      .fillMaxWidth()
      .then(
        if (interactive) {
          Modifier.clickable(onClick = onTap)
        } else {
          Modifier
        },
      )
      .padding(horizontal = 12.dp, vertical = 10.dp),
  )
}

private fun handleHeaderTap(
  model: TwoColumnFilterModel,
  categoryId: FilterId,
  section: FilterSection,
  config: TwoColumnPanelConfig,
  onModelChange: (TwoColumnFilterModel) -> Unit,
  onSelection: (FilterPanelSelection) -> Unit,
) {
  when (config.rightHeaderBehavior) {
    TwoColumnRightHeaderBehavior.Standard -> Unit
    TwoColumnRightHeaderBehavior.ToggleCollapse ->
      onModelChange(FilterSelection.toggleSectionCollapsed(model, categoryId, section.id))
    TwoColumnRightHeaderBehavior.SelectableHeader -> {
      val result = FilterSelection.selectSectionHeader(model, categoryId, section.id)
      onModelChange(result.model)
      if (config.reselectBehavior.shouldFire(result.selectionChanged)) {
        result.selection?.let(onSelection)
      }
    }
  }
}

private fun handleItemTap(
  model: TwoColumnFilterModel,
  categoryId: FilterId,
  section: FilterSection,
  item: FilterOptionItem,
  allowsMultipleSelection: Boolean,
  config: TwoColumnPanelConfig,
  onModelChange: (TwoColumnFilterModel) -> Unit,
  onSelection: (FilterPanelSelection) -> Unit,
) {
  val result = FilterSelection.toggleInTwoColumn(
    model = model,
    categoryId = categoryId,
    sectionId = section.id,
    itemId = item.id,
    allowsMultipleFromTab = allowsMultipleSelection,
    scope = config.singleSelectionScope,
  )
  onModelChange(result.model)
  if (config.reselectBehavior.shouldFire(result.selectionChanged)) {
    result.selection?.let(onSelection)
  }
}

@Composable
private fun TagsPanel(
  sections: List<FilterSection>,
  columns: Int,
  allowsMultipleSelection: Boolean,
  emptyLabel: String,
  onSectionsChange: (List<FilterSection>) -> Unit,
  onSelection: (FilterPanelSelection) -> Unit,
  modifier: Modifier = Modifier,
  pillStyle: FilterPillStyle,
) {
  val metrics = fkMetrics()
  if (sections.isEmpty() || sections.all { it.items.isEmpty() }) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .padding(metrics.spacing(FkSpacingToken.L)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = emptyLabel,
        style = fkTextStyle(FkTextStyle.Body),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
    }
    return
  }
  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(metrics.spacing(FkSpacingToken.M)),
    verticalArrangement = Arrangement.spacedBy(metrics.spacing(FkSpacingToken.M)),
  ) {
    sections.forEachIndexed { index, section ->
      section.title?.let { title ->
        Text(
          text = title,
          style = fkTextStyle(FkTextStyle.Subheadline),
          fontWeight = FontWeight.Normal,
          color = fkColor(FkColorRole.OnSurface),
        )
      }
      FilterChipFlow(
        items = section.items,
        columns = columns,
        pillStyle = pillStyle,
        onItemTap = { item ->
          val result = FilterSelection.toggleInSection(
            section = section,
            itemId = item.id,
            allowsMultipleFromTab = allowsMultipleSelection,
          )
          result.selection?.let { selection ->
            val updated = sections.toMutableList().also { it[index] = result.section }
            onSectionsChange(updated)
            onSelection(selection)
          }
        },
      )
    }
  }
}

@Composable
private fun SingleListPanel(
  section: FilterSection,
  allowsMultipleSelection: Boolean,
  emptyLabel: String,
  onSectionChange: (FilterSection) -> Unit,
  onSelection: (FilterPanelSelection) -> Unit,
  modifier: Modifier = Modifier,
  textCentered: Boolean = true,
) {
  if (section.items.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = emptyLabel,
        style = fkTextStyle(FkTextStyle.Body),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
    }
    return
  }
  LazyColumn(modifier = modifier.fillMaxWidth()) {
    items(section.items, key = { it.id.raw }) { item ->
      FilterListRow(
        item = item,
        selected = item.isSelected,
        textCentered = textCentered,
        onClick = {
          val result = FilterSelection.toggleInSection(
            section = section,
            itemId = item.id,
            allowsMultipleFromTab = allowsMultipleSelection,
          )
          result.selection?.let { selection ->
            onSectionChange(result.section)
            onSelection(selection)
          }
        },
      )
      HorizontalDivider(color = fkColor(FkColorRole.Outline))
    }
  }
}

@Composable
private fun FilterListRow(
  item: FilterOptionItem,
  selected: Boolean,
  onClick: () -> Unit,
  textCentered: Boolean = false,
) {
  val enabled = item.isEnabled
  val textColor = when {
    !enabled -> fkColor(FkColorRole.OnSurfaceSecondary)
    selected -> fkColor(FkColorRole.Primary)
    else -> fkColor(FkColorRole.OnSurface)
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalAlignment = if (textCentered) Alignment.CenterHorizontally else Alignment.Start,
  ) {
    Text(
      text = item.title,
      style = fkTextStyle(FkTextStyle.Body),
      fontWeight = FontWeight.Normal,
      color = textColor,
    )
    item.subtitle?.let { subtitle ->
      Text(
        text = subtitle,
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
        modifier = Modifier.padding(top = 2.dp),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipFlow(
  items: List<FilterOptionItem>,
  columns: Int,
  pillStyle: FilterPillStyle,
  onItemTap: (FilterOptionItem) -> Unit,
) {
  val columnCount = columns.coerceAtLeast(1)
  val spacing = 8.dp
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val chipWidth: Dp = ((maxWidth - spacing * (columnCount - 1)) / columnCount)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(spacing),
      verticalArrangement = Arrangement.spacedBy(spacing),
      maxItemsInEachRow = columnCount,
    ) {
      items.forEach { item ->
        FilterChip(
          item = item,
          width = chipWidth,
          style = pillStyle,
          onClick = { onItemTap(item) },
        )
      }
    }
  }
}

@Composable
private fun FilterChip(
  item: FilterOptionItem,
  width: Dp,
  style: FilterPillStyle,
  onClick: () -> Unit,
) {
  val shape = RoundedCornerShape(style.cornerRadiusDp.dp)
  val enabled = item.isEnabled
  val primary = fkColor(FkColorRole.Primary)
  val onPrimary = fkColor(FkColorRole.OnPrimary)
  val onSurface = fkColor(FkColorRole.OnSurface)
  val mutedBorder = Color(0xFFBBBBBB)

  val bg = when {
    !enabled -> fkColor(FkColorRole.SurfaceElevated)
    item.isSelected && style.selectedFillPrimary -> primary
    item.isSelected && style.selectedTextOnly -> Color.Transparent
    else -> if (style.showNormalBorder) Color.White else Color(0xFFF5F5F5)
  }
  val border = when {
    !enabled -> fkColor(FkColorRole.Outline)
    item.isSelected && style.selectedFillPrimary -> Color.Transparent
    item.isSelected && style.selectedTextOnly -> Color.Transparent
    style.showNormalBorder -> mutedBorder
    else -> Color.Transparent
  }
  val textColor = when {
    !enabled -> fkColor(FkColorRole.OnSurfaceSecondary)
    item.isSelected && style.selectedFillPrimary -> onPrimary
    item.isSelected -> primary
    else -> onSurface
  }
  val borderWidth = if (border == Color.Transparent) 0.dp else 1.dp
  Box(
    modifier = Modifier
      .width(width)
      .clip(shape)
      .then(
        if (borderWidth > 0.dp) {
          Modifier.border(borderWidth, border, shape)
        } else {
          Modifier
        },
      )
      .background(bg)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(
        horizontal = style.horizontalPaddingDp.dp,
        vertical = style.verticalPaddingDp.dp,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = item.title,
      style = fkTextStyle(FkTextStyle.Subheadline),
      color = textColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
