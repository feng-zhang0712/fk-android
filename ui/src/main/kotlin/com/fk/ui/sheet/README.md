# Sheet (`com.fk.ui.sheet`)

Product-level **bottom sheet** (multi-detent mapping) and **center card**. Phase **E3**.

Conceptually aligned with a narrow subset of iOS `FKSheetPresentationController`
(Compose façade over Material3 / Dialog — not a full presentation-engine port).

## Layout

| Type | Role |
|------|------|
| `FkSheet` | Package hub |
| `SheetDetent` / `SheetConfiguration` | Detents, backdrop, dismiss policy |
| `FkBottomSheet` | Material3 `ModalBottomSheet` product wrapper |
| `FkCenterSheet` | Centered floating card |
| `SheetController` | expand / partialExpand / hide |

## Usage

```kotlin
var open by remember { mutableStateOf(false) }
if (open) {
  FkBottomSheet(
    onDismissRequest = { open = false },
    configuration = SheetConfiguration.BottomSheetDefault,
  ) {
    Text("Hello")
  }
}

FkCenterSheet(onDismissRequest = { … }) {
  Text("Card")
}
```

## Notes

- Material3 exposes two visible anchors (partial + expanded). Product detents map onto that.
- Prefer creating `rememberFkBottomSheetState` **inside** the `if (visible)` branch that hosts the sheet.
- Bottom-sheet scrim tap and swipe both end in `onDismissRequest` (Material3 limitation).
  `dismissOnClickOutside` is fully honored by `FkCenterSheet`.
- Center-card dim uses the dialog window dim amount (no stacked Compose scrim).
- Top sheet, anchor dropdowns, container blur, and passthrough overlay hosts are not ported.
- Prefer presets: `BottomSheetDefault`, `BottomSheetLarge`, `BottomSheetFit`, `CenterCard`.
