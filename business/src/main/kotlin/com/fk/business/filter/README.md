# Filter (`com.fk.business.filter`)

Compose tab-strip filter kit aligned with iOS **TabBarFilter**.

## Scope (Phase F2)

- Models: options, sections, two-column categories, selection payloads
- Effective single/multi (tab ∩ section)
- Controllers: expand / collapse / title override / single-select auto-dismiss
- Panels: hierarchy (two-column list), dual hierarchy (two-column chips), tags, single list
- Host: strip + scrim overlay (pin at screen root), system Back dismiss; panel
  slides with vertical **offset** from the strip bottom (clipped; enter/exit
  symmetric), not height expand/collapse
- Custom panel slot
- Two-column selectable header tracks `selectedHeaderSectionId`
- Tags `columns` sizes chips into an approximate grid

## Skip

- UIKit sheet anchoring / overlay pin APIs
- Attributed titles
- Full height-policy surface (v1 uses max-height fraction; two-column panels fill that height)
- Listing-specific chrome (e.g. price asc/desc toggle) — keep in the host app

## Usage

```kotlin
val controller = rememberFilterController(
  tabs = tabs,
  configuration = FilterConfiguration(
    appearance = FilterAppearance(), // equal-width strip + iOS-aligned heights/pills
  ),
)
FilterHost(
  controller = controller,
  panelContents = panelsByTabId,
  onPanelContentChange = { id, content -> /* update host state */ },
  onSelection = { /* analytics / query refresh */ },
) {
  /* list / page body */
}
```

See `:sample` → Business → Filter.

## Appearance (Phase F2+)

- Tab strip: `FilterTabWidthMode.FillEqually` (default) or `IntrinsicScrollable`
- Chevron: `fk_ic_arrow_triangle_down` / `_up` (override via `FilterHost` painters)
- Tags pills: selected = primary fill + onPrimary text; unselected = `#BBBBBB` border
- Directory pills: selected = primary text only; 2 columns
- Heights: tags/list adaptive ≤55% host; directory fixed ~45–55% host (scroll inside)
- Single-list rows: centered text by default
