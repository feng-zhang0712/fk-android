package com.fk.ui.toast

/**
 * HUD convenience API over [ToastController].
 *
 * Conceptually aligned with iOS `FKHUD`.
 */
object Hud {
  fun showLoading(
    controller: ToastController,
    message: String = "Loading…",
    timeoutMs: Long = 25_000L,
  ): ToastHandle =
    controller.enqueue(
      ToastRequest(
        message = message,
        configuration = ToastConfiguration(
          kind = ToastKind.Hud,
          style = ToastStyle.Loading,
          position = ToastPosition.Center,
          durationMs = 0L,
          timeoutMs = timeoutMs,
          interceptTouches = true,
        ),
      ),
    )

  fun showSuccess(
    controller: ToastController,
    message: String,
    durationMs: Long = 1_800L,
  ): ToastHandle =
    controller.show(
      message = message,
      style = ToastStyle.Success,
      kind = ToastKind.Hud,
      configuration = ToastConfiguration(
        kind = ToastKind.Hud,
        style = ToastStyle.Success,
        position = ToastPosition.Center,
        durationMs = durationMs,
        interceptTouches = false,
      ),
    )

  fun showFailure(
    controller: ToastController,
    message: String,
    durationMs: Long = 1_800L,
  ): ToastHandle =
    controller.show(
      message = message,
      style = ToastStyle.Error,
      kind = ToastKind.Hud,
      configuration = ToastConfiguration(
        kind = ToastKind.Hud,
        style = ToastStyle.Error,
        position = ToastPosition.Center,
        durationMs = durationMs,
        interceptTouches = false,
      ),
    )

  fun showStatus(
    controller: ToastController,
    message: String,
    style: ToastStyle = ToastStyle.Info,
    title: String? = null,
    durationMs: Long = 2_000L,
  ): ToastHandle =
    controller.show(
      message = message,
      title = title,
      style = style,
      kind = ToastKind.Hud,
      configuration = ToastConfiguration(
        kind = ToastKind.Hud,
        style = style,
        position = ToastPosition.Center,
        durationMs = durationMs,
      ),
    )
}

/**
 * Snackbar convenience API over [ToastController].
 *
 * Conceptually aligned with iOS `FKSnackbar`.
 */
object Snackbar {
  fun show(
    controller: ToastController,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    style: ToastStyle = ToastStyle.Normal,
    durationMs: Long = 4_000L,
  ): ToastHandle =
    controller.enqueue(
      ToastRequest(
        message = message,
        actionLabel = actionLabel,
        onAction = onAction,
        configuration = ToastConfiguration(
          kind = ToastKind.Snackbar,
          style = style,
          position = ToastPosition.Bottom,
          durationMs = durationMs,
          interceptTouches = false,
        ),
      ),
    )
}
