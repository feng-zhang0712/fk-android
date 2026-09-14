# Skeleton (`com.fk.ui.skeleton`)

Shimmer / placeholder patterns for Compose. Phase **D3**.

Conceptually aligned with iOS `FKUIKit` Skeleton **composable + presets**.
UIView Auto tree-scanning and `FKSkeletonManager` are **not** ported.

## Layout

| Type | Role |
|------|------|
| `Skeleton` | Package hub + defaults |
| `SkeletonConfiguration` | Colors, corner, animation mode / direction / duration |
| `SkeletonBlock` / `SkeletonCircle` | Single placeholder |
| `Modifier.skeleton` | Overlay-style fill on existing layout |
| `SkeletonContainer` | Unified shimmer clock + [LocalSkeletonConfiguration] |
| `SkeletonPresets` | `ListRow` / `TextBlock` / `Card` / `GridCell` |
| `SkeletonListPlaceholder` | LazyColumn of N list rows |

## Usage

```kotlin
SkeletonContainer(configuration = SkeletonConfiguration(animationMode = SkeletonAnimationMode.Shimmer)) {
  SkeletonListPlaceholder(count = 6)
  // Children inherit LocalSkeletonConfiguration + unified shimmer phase.
}

SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f), height = 14.dp)

Box(modifier = Modifier.size(80.dp).skeleton(enabled = loading))
```

## Notes

- Colors default from `FkTheme` (`Surface` / `OnSurface`) when not set on configuration.
- Prefer `SkeletonContainer` above a list so all rows share one shimmer phase.
- Individual blocks are `invisibleToUser`; the container announces “Loading”.
- Full ListKit `presetRows` policy belongs in Phase E1 `list`.
