# Flow (`com.fk.ui.flow`)

Lean Compose port of iOS **FKUIKit FlowVisualization**.

## Scope (Phase G5)

| Composable | iOS | Role |
|------------|-----|------|
| `FkStepIndicator` | `FKStepIndicator` | Horizontal wizard / checkout steps |
| `FkTimeline` | `FKTimeline` | Vertical event rail (logistics, audit, activity) |
| `FlowStepItem` / `FlowStepState` | Shared models | Node data + semantics |

## Boundaries

- **Not** a scalar 0…1 progress bar — use Material `LinearProgressIndicator`.
- **Not** tab navigation — use `TabRow` / app navigation.
- Host formats timestamps into `FlowStepItem.timestampText` (no locale engine in this package).
- Optional `FlowStepItem.interactive` overrides default tap policy (disabled/skipped are non-tappable).
- v1 skips: multi layout presets, pulse motion, SF Symbol icon bags, branching DAGs, timeline sections.

## Usage

```kotlin
FkStepIndicator(
  items = checkoutItems,
  currentStepIndex = 1,
  onStepClick = { id -> /* optional */ },
)

FkTimeline(
  items = listOf(
    FlowStepItem("a", "Shipped", timestampText = "Mon 10:00", state = FlowStepState.Completed),
    FlowStepItem("b", "In transit", subtitle = "Hub scan", state = FlowStepState.Current),
    FlowStepItem("c", "Delivered", state = FlowStepState.Upcoming),
  ),
)
```

See `:sample` → UI → Flow.
