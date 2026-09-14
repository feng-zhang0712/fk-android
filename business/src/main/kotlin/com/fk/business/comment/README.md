# Comment (`com.fk.business.comment`)

Comment **list + composer** kit: flat `replyTo` threads, optimistic like, expand/collapse replies.
**No networking.**

Conceptually aligned with iOS `FKCommentKit` layout presets **Standard** and **Compact**.

## Layout

| Type | Role |
|------|------|
| `CommentKit` | Package hub |
| `CommentLayoutPreset` | Standard / Compact skeletons |
| `CommentItem` / page / submit / more | Models |
| `CommentConfiguration.standard()` / `.compact()` | Preset-matched defaults |
| `CommentComposerConfiguration` / `CommentComposerPresentationMode` | Composer tokens + visibility |
| `CommentComposerSession` | Per-target drafts + blur reset |
| `CommentListDataSource` / `CommentListListener` | App contracts |
| `CommentListMutation` / `CommentLikeOptimistic` | Flat-list helpers |
| `CommentListController` | State + load / like / reply / submit |
| `CommentListHost` / `CommentRow` / `CommentComposer` | Compose UI |

## Presets

| Preset | Row | Composer | Separators | Indent |
|--------|-----|----------|------------|--------|
| Standard | Author + time → reply-to → body → icon action bar → expand | Flat | Yes | 24 |
| Compact | Author ▸ reply-to + trailing time → body → meta rail → expand + chevron | Capsule | No | 42 |

Icons live in `business/src/main/res/drawable/` (`fk_ic_*`), ported from iOS CommentKit assets.

## Composer

`CommentComposer` is a dedicated file (aligned with iOS `FKCommentComposerView`).

| Token | Default | Notes |
|-------|---------|-------|
| `showsReplyTargetBanner` | `true` | Reply stripe above the input |
| `showsCancelReplyButton` | `true` | Cancel on the stripe |
| `showsSendButton` | `true` | Keep `true` for list submit |
| `presentationMode` | `Always` | `OnDemand` / `Automatic` hide idle chrome |
| `clearsCompositionOnBlur` | `true` | Clears stripe + visible text on blur |
| `preservesDrafts` | `true` | Restores text the next time that target is composed |

Blur / scroll / blank-tap dismiss saves the draft, clears the stripe, and resets Send. Drafts are keyed by reply comment id (`""` = top-level).

## Usage

```kotlin
val controller = rememberCommentListController(
  configuration = CommentConfiguration.compact().copy(
    composer = CommentComposerConfiguration(
      presentationMode = CommentComposerPresentationMode.Automatic,
      showsReplyTargetBanner = true,
    ),
  ),
  dataSource = myDataSource,
)

CommentListHost(controller)
```

## Notes

- Prefer CommentKit when actions + composer matter; do not merge with display-only thread cells.
- Visual indent clamps to `maxVisualDepth` (default 1); logical depth may be deeper.
- Beginning a reply focuses the composer (`composerFocusToken`); scrolling the list or tapping blank list space dismisses composition (draft preserved).
- More menu: built-in `FkBottomSheet` when `presentsDefaultMoreMenu` is `true` (default).
- Optimistic like clears `likeCountText`; call `acknowledgeLike` on success and `rollbackLike` on failure.
- Use `insertComment` / `updateComment` / `removeComment` for external or realtime updates.
- Keyboard align-to-composer and ListKit registration are not ported.
