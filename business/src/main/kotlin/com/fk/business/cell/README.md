# Cell (`com.fk.business.cell`)

Selective business **list-row** kit aligned with iOS **CellKit**.

## Scope (Phase F3)

Ship only high-reuse row patterns (item model + Compose):

| Row | iOS source |
|-----|------------|
| `UserListRow` | `FKUserListCell` |
| `NotificationListRow` | `FKNotificationListCell` |
| `SearchResultRow` | `FKSearchResultCell` |
| `OrderListRow` | `FKOrderListCell` |
| `InlineToggleRow` | `FKInlineToggleCell` |

Shared chrome: `CellTag`, `CellStatusPill`, search highlight segments.

## Skip

- Comment thread cells → use `com.fk.business.comment`
- Feed video / cart / checkout / grids / form pickers / ListKit registration / skeletons
- SF Symbols as primary icons; avatar URL loading (host may wire Coil later)

## Usage

```kotlin
UserListRow(
  item = user,
  onClick = { /* open profile */ },
  avatar = { /* optional Coil AsyncImage for user.avatarUrl */ },
)
SearchResultRow(item = SearchResultItem.highlight(id, title, query))
InlineToggleRow(item = toggle, onCheckedChange = { /* persist */ })
```

See `:sample` → Business → Cell.
