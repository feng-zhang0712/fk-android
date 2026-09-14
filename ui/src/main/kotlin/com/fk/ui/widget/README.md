# Widgets (`com.fk.ui.widget`)

Design-system capsules aligned with iOS **FKUIKit Widgets**.

## Scope (Phase G3)

| Composable | iOS | Role |
|------------|-----|------|
| `FkAvatar` / `FkPresenceDot` | FKAvatar / FKPresenceIndicator | Profile / list avatars + presence |
| `FkChip` / `FkTag` / `FkChipGroup` | FKChip / FKTag / FKChipGroup | Thin Material chip façade + Tag + group selection |
| `FkStatusPill` | FKStatusPill | Workflow / order status |

## Chip vs Material 3

Material already ships `FilterChip` / `InputChip` / `SuggestionChip`.

- **One-off screens:** prefer Material chips directly.
- **`FkChip`:** thin façade (Material + FK theme colors) for iOS mode parity — do **not** grow a parallel chip design system.
- **`FkChipGroup`:** keep — Material has no selection orchestration (single / multiple / max).
- **`FkTag`:** keep as a small read-only metadata capsule (Material has no Tag).

## Boundaries

- **StatusPill** = workflow state (`FkStatusSemantic`)
- **Tag** = marketing / role / category metadata
- **Presence** on Avatar ≠ StatusPill dot
- **Chip** ≠ CopyChip (skipped in v1)

## Skip (v1)

AvatarGroup, story ring, presence/status pulse, CopyChip, SF Symbols icon bags, elevated/assist Material chip clones

## Usage

```kotlin
FkAvatar(displayName = "Ada", imageUrl = url, presence = PresenceState.Online, verified = true)
FkStatusPill(title = "Shipped", style = FkStatusSemantic.Success, showsDot = true)
FkTag(title = "VIP", variant = TagVariant.Brand)
FkChipGroup(chips, selectedIds, onSelectionChange, selectionMode = ChipSelectionMode.Single)
```

See `:sample` → UI → Widget.
