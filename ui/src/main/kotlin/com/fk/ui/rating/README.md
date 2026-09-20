# Rating (`com.fk.ui.rating`)

Configurable star rating for read-only display and interactive scoring. Supports whole / half / custom step snapping, optional caption, drag selection, and basic accessibility.

## Scope

| API | Role |
|-----|------|
| `FkRating` | Full control (layout / appearance / interaction / label) |
| `FkRatingReadOnly` | Convenience read-only stars (default half-step display) |
| `FkRatingInteractive` | Convenience interactive stars (default half-step input) |
| `RatingConfiguration` | Grouped settings |
| `RatingStep` | `Whole` / `Half` / `Custom` |

## Features

- Read-only and interactive modes
- Half-star (and arbitrary step) snapping on tap / drag
- Partial fill via horizontal **draw clip** over the filled glyph (same silhouette as empty)
- Default star uses `fk_ic_star_fill` for both layers (empty tint + filled tint) so sizes match
- Custom icons: supply `RatingIconStyle.Painters` (same silhouette recommended for clean half fills)

## Usage

```kotlin
var score by remember { mutableDoubleStateOf(3.5) }

FkRatingInteractive(
  value = score,
  onValueChange = { score = it },
  step = RatingStep.Half,
)

FkRatingReadOnly(value = 4.5)

FkRating(
  value = score,
  onValueChange = { score = it },
  configuration = RatingConfiguration(
    layout = RatingLayoutConfiguration(
      itemCount = 5,
      labelPlacement = RatingLabelPlacement.Trailing,
    ),
    interaction = RatingInteractionConfiguration(
      mode = RatingInteractionMode.Interactive,
      step = RatingStep.Half,
    ),
  ),
)
```

See `:sample` → UI → Rating (scenario hub).
