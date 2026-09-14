# Toast (`com.fk.ui.toast`)

Unified **toast / HUD / snackbar** queue for Compose. Phase **D4**.

Conceptually aligned with iOS `FKToast` / `FKHUD` / `FKSnackbar` (narrow Compose port).

## Layout

| Type | Role |
|------|------|
| `Toast` | Package hub + defaults |
| `ToastController` | Sequential / replace-active queue |
| `ToastHost` / `ToastOverlay` | Compose overlay presenter |
| `Hud` / `Snackbar` | Convenience façades |
| `ToastKind` / `ToastStyle` / `ToastPosition` | Presentation model |

## Usage

```kotlin
val controller = rememberToastController()

ToastHost(controller) {
  // app content
}

controller.show("Hello", style = ToastStyle.Info)
Hud.showLoading(controller, "Saving…")
Snackbar.show(controller, "Archived", actionLabel = "Undo")

// Burst queue (verify gate):
repeat(5) { i -> controller.show("Queue #$i", style = ToastStyle.Info) }
```

## Notes

- Default queue is sequential with `maxConcurrent = 1`.
- HUD loading uses `durationMs = 0` + `timeoutMs` and can intercept touches.
- Snackbar defaults to bottom + 4s duration; body tap does not dismiss (action / timeout / `dismiss` do).
- `Snackbar.show(..., onAction = { })` runs after dismiss when the action is pressed.
- `rememberToastController` disposes the controller on leave / config change.
- UIKit window scenes / liquid glass / custom UIView are not ported.
