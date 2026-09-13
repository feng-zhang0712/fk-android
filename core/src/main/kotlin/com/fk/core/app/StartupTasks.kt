package com.fk.core.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * Registers and runs launch-time tasks sequentially.
 *
 * Conceptually aligned with iOS `FKBusinessStartupTaskManaging`.
 */
interface StartupTaskManaging {
  fun register(task: StartupTask)
  fun unregister(taskId: String)
  fun registeredTasks(): List<StartupTask>
  /**
   * Runs registered tasks in priority order.
   *
   * Non-[CancellationException] failures in one task are isolated so later tasks still run.
   */
  suspend fun runAll()
}

/** Default [StartupTaskManaging]: sort by priority then delay, await each work. */
class StartupTaskManager : StartupTaskManaging {
  private val tasks = ConcurrentHashMap<String, StartupTask>()

  override fun register(task: StartupTask) {
    tasks[task.id] = task
  }

  override fun unregister(taskId: String) {
    tasks.remove(taskId)
  }

  override fun registeredTasks(): List<StartupTask> =
    tasks.values.sortedWith(
      compareBy<StartupTask> { it.priority.ordinal }
        .thenBy { it.delayMs }
        .thenBy { it.id },
    )

  override suspend fun runAll() {
    for (task in registeredTasks()) {
      if (task.delayMs > 0L) delay(task.delayMs)
      try {
        task.work()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Throwable) {
        // Isolate task failures so a single bad startup unit does not abort the rest.
      }
    }
  }
}
