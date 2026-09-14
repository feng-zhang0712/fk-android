package com.fk.ui.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

/**
 * In-memory toast queue (sequential by default).
 *
 * Conceptually aligned with iOS `FKToast` queue actor semantics (narrow subset).
 *
 * Call [dispose] when the host leaves composition (see [rememberToastController]).
 */
@Stable
class ToastController(
  private val queueConfiguration: ToastQueueConfiguration = ToastQueueConfiguration(),
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
  private val mutex = Mutex()
  private val pending = ArrayDeque<ToastRequest>()
  private val dismissJobs = mutableMapOf<String, Job>()

  /** Currently visible entries (usually 0..[ToastQueueConfiguration.maxConcurrent]). */
  val visible: SnapshotStateList<ToastEntry> = mutableStateListOf()

  /** Pending queue size (not yet on screen). Observable for Compose. */
  var pendingCount by mutableIntStateOf(0)
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
        when (queueConfiguration.presentationStrategy) {
          ToastPresentationStrategy.ReplaceActive -> {
            if (visible.isNotEmpty()) {
              val activeIds = visible.map { it.request.id }
              visible.clear()
              activeIds.forEach { cancelDismissJob(it) }
            }
            pending.clear()
            syncPendingCount()
            presentLocked(request)
          }
          ToastPresentationStrategy.Sequential -> {
            if (visible.size < queueConfiguration.maxConcurrent.coerceAtLeast(1)) {
              presentLocked(request)
            } else {
              pending.addLast(request)
              syncPendingCount()
            }
          }
        }
      }
    }
    return ToastHandle(request.id)
  }

  fun dismiss(id: String) {
    scope.launch {
      mutex.withLock {
        dismissLocked(id)
        pumpLocked()
      }
    }
  }

  fun dismiss(handle: ToastHandle) = dismiss(handle.id)

  fun clearAll() {
    scope.launch {
      mutex.withLock {
        val ids = visible.map { it.request.id } + pending.map { it.id }
        visible.clear()
        pending.clear()
        syncPendingCount()
        ids.forEach { cancelDismissJob(it) }
      }
    }
  }

  fun invokeAction(id: String) {
    scope.launch {
      val action = mutex.withLock {
        val entry = visible.firstOrNull { it.request.id == id }
        val callback = entry?.request?.onAction
        dismissLocked(id)
        pumpLocked()
        callback
      }
      action?.invoke()
    }
  }

  /** Cancels pending work and clears the queue. */
  fun dispose() {
    dismissJobs.values.forEach { it.cancel() }
    dismissJobs.clear()
    visible.clear()
    pending.clear()
    pendingCount = 0
    scope.cancel()
  }

  private fun presentLocked(request: ToastRequest) {
    visible += ToastEntry(request = request)
    scheduleAutoDismiss(request)
  }

  private fun dismissLocked(id: String) {
    cancelDismissJob(id)
    visible.removeAll { it.request.id == id }
    pending.removeAll { it.id == id }
    syncPendingCount()
  }

  private fun pumpLocked() {
    val capacity = queueConfiguration.maxConcurrent.coerceAtLeast(1) - visible.size
    if (capacity <= 0) return
    repeat(capacity) {
      if (pending.isEmpty()) return
      val next = pending.removeFirst()
      syncPendingCount()
      presentLocked(next)
    }
  }

  private fun syncPendingCount() {
    pendingCount = pending.size
  }

  private fun scheduleAutoDismiss(request: ToastRequest) {
    cancelDismissJob(request.id)
    val duration = request.configuration.durationMs
    val timeout = request.configuration.timeoutMs
    val delayMs = when {
      duration > 0L -> duration
      timeout != null && timeout > 0L -> timeout
      else -> return
    }
    dismissJobs[request.id] = scope.launch {
      delay(delayMs)
      mutex.withLock {
        dismissLocked(request.id)
        pumpLocked()
      }
    }
  }

  private fun cancelDismissJob(id: String) {
    dismissJobs.remove(id)?.cancel()
  }
}

/** Remembers a [ToastController] and disposes it when leaving composition / config change. */
@Composable
fun rememberToastController(
  queueConfiguration: ToastQueueConfiguration = ToastQueueConfiguration(),
): ToastController {
  val controller = remember(queueConfiguration) {
    ToastController(queueConfiguration = queueConfiguration)
  }
  DisposableEffect(controller) {
    onDispose { controller.dispose() }
  }
  return controller
}
