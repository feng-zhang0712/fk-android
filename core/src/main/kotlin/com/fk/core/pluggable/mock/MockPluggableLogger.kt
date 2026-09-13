package com.fk.core.pluggable.mock

import android.util.Log
import com.fk.core.pluggable.logging.PluggableLogLevel
import com.fk.core.pluggable.logging.PluggableLogger

/**
 * Logcat-backed [PluggableLogger] for samples and debug builds.
 */
class MockPluggableLogger(
  private val tag: String = "FkPluggable",
  override var minimumLevel: PluggableLogLevel = PluggableLogLevel.Debug,
) : PluggableLogger {
  override fun log(
    level: PluggableLogLevel,
    message: () -> String,
    throwable: Throwable?,
  ) {
    if (level.ordinal < minimumLevel.ordinal) return
    val text = message()
    when (level) {
      PluggableLogLevel.Verbose -> Log.v(tag, text, throwable)
      PluggableLogLevel.Debug -> Log.d(tag, text, throwable)
      PluggableLogLevel.Info -> Log.i(tag, text, throwable)
      PluggableLogLevel.Warning -> Log.w(tag, text, throwable)
      PluggableLogLevel.Error -> Log.e(tag, text, throwable)
    }
  }
}
