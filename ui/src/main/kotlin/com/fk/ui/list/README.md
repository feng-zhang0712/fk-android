# List (`com.fk.ui.list`)

Lazy-list **orchestration** for Compose: pull-to-refresh, load-more chrome, and empty/skeleton wiring. Phase **E1**.

Conceptually aligned with iOS `FKListKit` + `FKRefresh` (scenario port — not Diffable / UITableView APIs).

## Layout

| Type | Role |
|------|------|
| `ListKit` | Package hub + defaults |
| `ListController` | Presentation state, pagination, refresh / load-more |
| `ListHost` | `PullToRefreshBox` + LazyColumn + empty/skeleton |
| `ListLoadMoreFooter` | Footer chrome (loading / retry / no-more) |
| `ListDataProvider` / `ListFetchResult` | Page-fetch contract |
| `ListConfiguration` | PTR / load-more / empty copy flags |

## Usage

```kotlin
val provider = remember {
  object : ListDataProvider<Item> {
    override suspend fun fetchInitial(page: Int) = api.page(page)
    override suspend fun fetchNextPage(page: Int) = api.page(page)
  }
}
val controller = rememberListController(provider = provider)

ListHost(controller, key = { it.id }) { _, item ->
  Text(item.title)
}
```

## Notes

- Pagination is **1-based**; refresh resets; advance only after a successful load-more.
- Refresh and load-more are **mutually exclusive** by default (`cancelLoadMoreOnRefresh`).
- Load-more failures stay on **content** with a footer retry (no full-screen error).
- Initial loading uses `SkeletonListPlaceholder` (override via `ListHost(skeleton = …)`).
- `replaceItems` / `appendItems` update local data without a fetch.
- Empty/error reuse `com.fk.ui.empty`; Diffable snapshots, preset cells, and swipe are not ported.
