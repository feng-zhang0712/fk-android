package com.fk.ui.callout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
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
 * Presents anchored callouts. Default policy replaces the active session; set
 * [CalloutPresentationPolicy.AllowConcurrent] to stack multiple sessions.
 *
 * Call [dispose] when the host leaves composition (see [rememberCalloutController]).
 */
@Stable
class CalloutController(
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
  private val mutex = Mutex()
  private val dismissJobs = mutableMapOf<String, Job>()
  private val anchors = mutableMapOf<String, CalloutAnchorState>()

  /** Active sessions (newest last). Empty when idle. */
  var entries: List<CalloutEntry> by mutableStateOf(emptyList())
    private set

  val isPresenting: Boolean
    get() = entries.isNotEmpty()

  /** Registers an anchor so show APIs can resolve bounds by id. */
  fun registerAnchor(state: CalloutAnchorState) {
    anchors[state.id] = state
  }

  fun unregisterAnchor(state: CalloutAnchorState) {
    if (anchors[state.id] === state) {
      anchors.remove(state.id)
    }
  }

  fun anchorBounds(anchorId: String): Rect? {
    val state = anchors[anchorId] ?: return null
    if (!state.isAttached || state.boundsInWindow == Rect.Zero) return null
    return state.boundsInWindow
  }

  /** Returns the registered anchor state so Compose can observe bound updates. */
  fun anchorState(anchorId: String): CalloutAnchorState? = anchors[anchorId]

  fun show(request: CalloutRequest): CalloutHandle {
    scope.launch {
      mutex.withLock {
        presentLocked(request)
      }
    }
    return CalloutHandle(request.id)
  }

  /**
   * Presents [request], or updates an existing session that shares the same [CalloutRequest.anchorId].
   */
  fun showOrUpdate(request: CalloutRequest): CalloutHandle {
    val existing = entries.lastOrNull { it.request.anchorId == request.anchorId }
    if (existing != null) {
      val updated = request.copy(id = existing.request.id)
      scope.launch {
        mutex.withLock {
          updateLocked(existing.request.id, updated.content, updated.configuration, updated)
        }
      }
      return CalloutHandle(existing.request.id)
    }
    return show(request)
  }

  fun update(
    id: String,
    content: CalloutContent,
    configuration: CalloutConfiguration? = null,
  ): Boolean {
    val entry = entries.firstOrNull { it.request.id == id } ?: return false
    scope.launch {
      mutex.withLock {
        updateLocked(id, content, configuration, entry.request)
      }
    }
    return true
  }

  fun update(
    handle: CalloutHandle,
    content: CalloutContent,
    configuration: CalloutConfiguration? = null,
  ): Boolean = update(handle.id, content, configuration)

  fun dismiss(
    id: String,
    reason: CalloutDismissReason = CalloutDismissReason.Manual,
  ) {
    scope.launch {
      mutex.withLock {
        dismissLocked(id, reason)
      }
    }
  }

  fun dismiss(
    handle: CalloutHandle,
    reason: CalloutDismissReason = CalloutDismissReason.Manual,
  ) = dismiss(handle.id, reason)

  fun dismissActive(reason: CalloutDismissReason = CalloutDismissReason.Manual) {
    scope.launch {
      mutex.withLock {
        val ids = entries.map { it.request.id }
        ids.forEach { dismissLocked(it, reason) }
      }
    }
  }

  fun invokeAction(id: String, actionId: String) {
    scope.launch {
      val handler = mutex.withLock {
        val entry = entries.firstOrNull { it.request.id == id } ?: return@withLock null
        val handlers = entry.request.actionHandlers
        val mapped = handlers[actionId]
          ?: entry.request.content.let { content ->
            if (content is CalloutContent.MessageWithActions) {
              content.actions.firstOrNull { it.id == actionId || it.title == actionId }?.let { action ->
                handlers[action.id] ?: handlers[action.title]
              }
            } else if (content is CalloutContent.CoachMark) {
              handlers[content.content.primaryActionTitle]
            } else {
              null
            }
          }
        dismissLocked(id, CalloutDismissReason.ActionTriggered)
        mapped
      }
      handler?.invoke()
    }
  }

  fun invokeClose(id: String) {
    scope.launch {
      val handler = mutex.withLock {
        val entry = entries.firstOrNull { it.request.id == id } ?: return@withLock null
        val close = entry.request.closeHandler
        dismissLocked(id, CalloutDismissReason.CloseButton)
        close
      }
      handler?.invoke()
    }
  }

  fun invokeMenuSelection(id: String, item: CalloutMenuItem) {
    scope.launch {
      val handler = mutex.withLock {
        val entry = entries.firstOrNull { it.request.id == id } ?: return@withLock null
        val select = entry.request.menuSelectionHandler
        dismissLocked(id, CalloutDismissReason.MenuSelection)
        select
      }
      handler?.invoke(item)
    }
  }

  fun dispose() {
    dismissJobs.values.forEach { it.cancel() }
    dismissJobs.clear()
    entries = emptyList()
    scope.cancel()
  }

  private fun presentLocked(request: CalloutRequest) {
    when (request.configuration.presentationPolicy) {
      CalloutPresentationPolicy.ReplaceActive -> {
        val existing = entries.toList()
        existing.forEach { dismissLocked(it.request.id, CalloutDismissReason.Replaced) }
      }
      CalloutPresentationPolicy.AllowConcurrent -> Unit
    }
    request.hooks.willShow?.invoke(request.id)
    entries = entries + CalloutEntry(request)
    request.hooks.didShow?.invoke(request.id)
    scheduleAutoDismiss(request)
  }

  private fun updateLocked(
    id: String,
    content: CalloutContent,
    configuration: CalloutConfiguration?,
    base: CalloutRequest,
  ) {
    val index = entries.indexOfFirst { it.request.id == id }
    if (index < 0) return
    dismissJobs.remove(id)?.cancel()
    val updated = base.copy(
      id = id,
      content = content,
      configuration = configuration ?: base.configuration,
    )
    val mutable = entries.toMutableList()
    mutable[index] = CalloutEntry(updated)
    entries = mutable
    scheduleAutoDismiss(updated)
  }

  private fun dismissLocked(id: String, reason: CalloutDismissReason) {
    val entry = entries.firstOrNull { it.request.id == id } ?: return
    dismissJobs.remove(id)?.cancel()
    entry.request.hooks.willDismiss?.invoke(id, reason)
    entries = entries.filterNot { it.request.id == id }
    entry.request.hooks.didDismiss?.invoke(id, reason)
  }

  private fun scheduleAutoDismiss(request: CalloutRequest) {
    val duration = request.configuration.autoDismissDurationMs ?: return
    if (duration <= 0L) return
    dismissJobs[request.id]?.cancel()
    dismissJobs[request.id] = scope.launch {
      delay(duration)
      mutex.withLock {
        dismissLocked(request.id, CalloutDismissReason.Timeout)
      }
    }
  }
}

@Composable
fun rememberCalloutController(): CalloutController {
  val controller = remember { CalloutController() }
  DisposableEffect(controller) {
    onDispose { controller.dispose() }
  }
  return controller
}
