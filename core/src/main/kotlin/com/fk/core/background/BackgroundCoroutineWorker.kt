package com.fk.core.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * WorkManager bridge that dispatches to [BackgroundTaskRegistry] handlers.
 */
internal class BackgroundCoroutineWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val identifier = inputData.getString(KEY_IDENTIFIER)
      ?: return Result.failure()

    val entry = BackgroundTaskRegistry.get(identifier)
      ?: return Result.failure(
        workDataOf(KEY_ERROR to "unregistered:$identifier"),
      )

    val handle = BackgroundTaskHandle(
      identifier = identifier,
      expiredProbe = { isStopped },
    )

    return try {
      val returned = entry.handler.invoke(handle)
      if (!handle.isCompleted) {
        handle.complete(returned)
      }
      val ok = (handle.lastSuccess ?: returned) && !handle.isExpired
      if (ok) {
        log("Background task '$identifier' completed success=true")
        Result.success()
      } else {
        log("Background task '$identifier' completed success=false")
        Result.failure()
      }
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
      handle.markExpired()
      log("Background task '$identifier' cancelled")
      throw cancelled
    } catch (t: Throwable) {
      log("Background task '$identifier' failed: ${t.message}")
      Result.failure(workDataOf(KEY_ERROR to (t.message ?: t::class.java.simpleName)))
    }
  }

  private fun log(message: String) {
    if (inputData.getBoolean(KEY_LOG, false)) {
      Log.d(TAG, message)
    }
  }

  companion object {
    const val TAG: String = "FkBackground"
    const val KEY_IDENTIFIER: String = "fk.background.identifier"
    const val KEY_LOG: String = "fk.background.log"
    const val KEY_ERROR: String = "fk.background.error"
  }
}
