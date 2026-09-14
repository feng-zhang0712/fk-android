# Comment (`com.fk.business.comment`)

Comment **list + composer** kit: flat `replyTo` threads, optimistic like, expand/collapse replies.
Phase **F1**. **No networking.**

Conceptually aligned with iOS `FKCommentKit`.

## Layout

| Type | Role |
|------|------|
| `CommentKit` | Package hub |
| `CommentItem` / page / submit / more | Models |
| `CommentListDataSource` / `CommentListListener` | App contracts |
| `CommentListMutation` / `CommentLikeOptimistic` | Flat-list helpers |
| `CommentListController` | State + load / like / reply / submit |
| `CommentListHost` / `CommentRow` / `CommentComposer` | Compose UI |

## Usage

```kotlin
val controller = rememberCommentListController(dataSource = myDataSource)

CommentListHost(controller) { item ->
  // present more menu for item
}
```

## Notes

- Prefer CommentKit when actions + composer matter; do not merge with display-only thread cells.
- Visual indent clamps to `maxVisualDepth` (default 1); logical depth may be deeper.
- Optimistic like clears `likeCountText`; call `acknowledgeLike` on success and `rollbackLike` on failure.
- Use `insertComment` / `updateComment` / `removeComment` for external or realtime updates.
- Compact preset, keyboard align-to-composer, and ListKit registration are not ported.
