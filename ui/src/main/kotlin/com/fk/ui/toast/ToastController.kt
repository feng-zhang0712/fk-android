package com.fk.ui.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single-slot toast presenter: a new show **replaces** the active item.
 *
 * Conceptually aligned with iOS `FKToast` (lean Compose subset — no multi-item queue).
 *
 * Call [dispose] when the host leaves composition (see [rememberToastController]).
 */
@Stable
class ToastController(
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
  private val mutex = Mutex()
  private var dismissJob: Job? = null

  /** Currently visible entry, or `null` when idle. */
  var current: ToastEntry? by mutableStateOf(null)
    private set

  fun show(
    message: String,
    style: ToastStyle = ToastStyle.Normal,
    kind: ToastKind = ToastKind.Toast,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    configuration: ToastConfiguration = Toast.DefaultConfiguration.copy(
      kind = kind,
      style = style,
    ),
  ): ToastHandle =
    enqueue(
      ToastRequest(
        message = message,
        title = title,
        actionLabel = actionLabel,
        configuration = configuration.copy(kind = kind, style = style),
        onAction = onAction,
      ),
    )

  fun enqueue(request: ToastRequest): ToastHandle {
    scope.launch {
      mutex.withLock {
        presentLocked(request)
      }
    }
    return ToastHandle(request.id)
  }

  fun dismiss(id: String) {
    scope.launch {
      mutex.withLock {
        if (current?.request?.id == id) {
          clearLocked()
        }
      }
    }
  }

  fun dismiss(handle: ToastHandle) = dismiss(handle.id)

  fun clear() {
    scope.launch {
      mutex.withLock {
        clearLocked()
      }
    }
  }

  fun invokeAction(id: String) {
    scope.launch {
      val action = mutex.withLock {
        val entry = current
        if (entry?.request?.id != id) return@withLock null
        val callback = entry.request.onAction
        clearLocked()
        callback
      }
      action?.invoke()
    }
  }

  /** Cancels timers and clears the active toast. */
  fun dispose() {
    dismissJob?.cancel()
    dismissJob = null
    current = null
    scope.cancel()
  }

  private fun presentLocked(request: ToastRequest) {
    cancelDismissJob()
    current = ToastEntry(request = request)
    scheduleAutoDismiss(request)
  }

  private fun clearLocked() {
    cancelDismissJob()
    current = null
  }

  private fun scheduleAutoDismiss(request: ToastRequest) {
    val duration = request.configuration.durationMs
    val timeout = request.configuration.timeoutMs
    val delayMs = when {
      duration > 0L -> duration
      timeout != null && timeout > 0L -> timeout
      else -> return
    }
    dismissJob = scope.launch {
      delay(delayMs)
      mutex.withLock {
        if (current?.request?.id == request.id) {
          clearLocked()
        }
      }
    }
  }

  private fun cancelDismissJob() {
    dismissJob?.cancel()
    dismissJob = null
  }
}

/** Remembers a [ToastController] and disposes it when leaving composition / config change. */
@Composable
fun rememberToastController(): ToastController {
  val controller = remember { ToastController() }
  DisposableEffect(controller) {
    onDispose { controller.dispose() }
  }
  return controller
}
