package com.fk.business.filter

/**
 * Pure selection helpers for filter panels (host-owned immutable models).
 */
object FilterSelection {

  /**
   * Applies an option tap inside a single [FilterSection].
   *
   * @return updated section, the selection payload (if callbacks should fire), and whether selection changed.
   */
  fun toggleInSection(
    section: FilterSection,
    itemId: FilterId,
    allowsMultipleFromTab: Boolean,
  ): SectionToggleResult {
    val index = section.items.indexOfFirst { it.id == itemId }
    if (index < 0) return SectionToggleResult(section, selection = null, selectionChanged = false)
    val tapped = section.items[index]
    if (!tapped.isEnabled) return SectionToggleResult(section, selection = null, selectionChanged = false)

    val effective = FilterSelectionMode.effective(
      requested = section.selectionMode,
      allowsMultipleFromTab = allowsMultipleFromTab,
    )
    val selectionChanged = when (effective) {
      FilterSelectionMode.Single -> !tapped.isSelected
      FilterSelectionMode.Multiple -> true
    }
    val updatedItems = when (effective) {
      FilterSelectionMode.Single ->
        section.items.map { it.copy(isSelected = it.id == itemId) }
      FilterSelectionMode.Multiple ->
        section.items.map {
          if (it.id == itemId) it.copy(isSelected = !it.isSelected) else it
        }
    }
    val updated = section.copy(items = updatedItems)
    val payload = FilterPanelSelection(
      sectionId = section.id,
      item = tapped.copy(isSelected = updatedItems[index].isSelected),
      effectiveMode = effective,
    )
    return SectionToggleResult(updated, payload, selectionChanged)
  }

  /**
   * Selects a category in a two-column model (left column).
   */
  fun selectCategory(
    model: TwoColumnFilterModel,
    categoryId: FilterId,
  ): CategorySelectResult {
    val previous = model.selectedCategoryId
    val updated = model.copy(
      categories = model.categories.map { it.copy(isSelected = it.id == categoryId) },
      selectedHeaderSectionId = null,
    )
    val changed = previous != categoryId
    val category = updated.categories.firstOrNull { it.id == categoryId }
    val emptySections = updated.sectionsByCategoryId[categoryId].orEmpty().isEmpty()
    val emptySelection = if (emptySections && category != null) {
      FilterPanelSelection(
        sectionId = null,
        item = FilterOptionItem(id = category.id, title = category.title, isSelected = true),
        effectiveMode = FilterSelectionMode.Single,
      )
    } else {
      null
    }
    return CategorySelectResult(updated, changed, emptySelection)
  }

  /**
   * Applies an option tap in the right column of a two-column model.
   */
  fun toggleInTwoColumn(
    model: TwoColumnFilterModel,
    categoryId: FilterId,
    sectionId: FilterId,
    itemId: FilterId,
    allowsMultipleFromTab: Boolean,
    scope: TwoColumnSingleSelectionScope,
  ): TwoColumnToggleResult {
    val sections = model.sectionsByCategoryId[categoryId].orEmpty()
    val sectionIndex = sections.indexOfFirst { it.id == sectionId }
    if (sectionIndex < 0) {
      return TwoColumnToggleResult(model, selection = null, selectionChanged = false)
    }
    val section = sections[sectionIndex]
    val itemIndex = section.items.indexOfFirst { it.id == itemId }
    if (itemIndex < 0) {
      return TwoColumnToggleResult(model, selection = null, selectionChanged = false)
    }
    val tapped = section.items[itemIndex]
    if (!tapped.isEnabled) {
      return TwoColumnToggleResult(model, selection = null, selectionChanged = false)
    }

    val effective = FilterSelectionMode.effective(
      requested = section.selectionMode,
      allowsMultipleFromTab = allowsMultipleFromTab,
    )
    val selectionChanged = when (effective) {
      FilterSelectionMode.Single -> !tapped.isSelected
      FilterSelectionMode.Multiple -> true
    }

    val updatedSections = when {
      effective == FilterSelectionMode.Single &&
        scope == TwoColumnSingleSelectionScope.GlobalAcrossSections ->
        sections.map { sec ->
          sec.copy(
            items = sec.items.map { item ->
              item.copy(isSelected = sec.id == sectionId && item.id == itemId)
            },
          )
        }
      else -> {
        val result = toggleInSection(section, itemId, allowsMultipleFromTab)
        sections.toMutableList().also { it[sectionIndex] = result.section }
      }
    }

    val updatedModel = model.copy(
      sectionsByCategoryId = model.sectionsByCategoryId + (categoryId to updatedSections),
      selectedHeaderSectionId = null,
    )
    val updatedItem = updatedSections[sectionIndex].items.first { it.id == itemId }
    val payload = FilterPanelSelection(
      sectionId = section.id,
      item = updatedItem,
      effectiveMode = effective,
    )
    return TwoColumnToggleResult(updatedModel, payload, selectionChanged)
  }

  /**
   * Toggles [FilterSection.isCollapsed] for a section in the selected category.
   */
  fun toggleSectionCollapsed(
    model: TwoColumnFilterModel,
    categoryId: FilterId,
    sectionId: FilterId,
  ): TwoColumnFilterModel {
    val sections = model.sectionsByCategoryId[categoryId].orEmpty()
    val updated = sections.map {
      if (it.id == sectionId) it.copy(isCollapsed = !it.isCollapsed) else it
    }
    return model.copy(sectionsByCategoryId = model.sectionsByCategoryId + (categoryId to updated))
  }

  /**
   * Selects a section header (clears item picks in that section; always single).
   */
  fun selectSectionHeader(
    model: TwoColumnFilterModel,
    categoryId: FilterId,
    sectionId: FilterId,
  ): TwoColumnToggleResult {
    val sections = model.sectionsByCategoryId[categoryId].orEmpty()
    val section = sections.firstOrNull { it.id == sectionId }
      ?: return TwoColumnToggleResult(model, selection = null, selectionChanged = false)
    val title = section.title.orEmpty()
    val updatedSections = sections.map { sec ->
      if (sec.id == sectionId) {
        sec.copy(items = sec.items.map { it.copy(isSelected = false) })
      } else {
        sec
      }
    }
    val selectionChanged = model.selectedHeaderSectionId != sectionId
    val updatedModel = model.copy(
      sectionsByCategoryId = model.sectionsByCategoryId + (categoryId to updatedSections),
      selectedHeaderSectionId = sectionId,
    )
    val headerItem = FilterOptionItem(
      id = section.id,
      title = title.ifEmpty { section.id.raw },
      isSelected = true,
    )
    return TwoColumnToggleResult(
      model = updatedModel,
      selection = FilterPanelSelection(
        sectionId = section.id,
        item = headerItem,
        effectiveMode = FilterSelectionMode.Single,
      ),
      selectionChanged = selectionChanged,
    )
  }

  data class SectionToggleResult(
    val section: FilterSection,
    val selection: FilterPanelSelection?,
    val selectionChanged: Boolean,
  )

  data class CategorySelectResult(
    val model: TwoColumnFilterModel,
    val selectionChanged: Boolean,
    /** Emitted when the category has no right-hand sections. */
    val emptyCategorySelection: FilterPanelSelection?,
  )

  data class TwoColumnToggleResult(
    val model: TwoColumnFilterModel,
    val selection: FilterPanelSelection?,
    val selectionChanged: Boolean,
  )
}
