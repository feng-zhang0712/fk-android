package com.fk.core.logging

import android.content.Context
import java.io.File

/**
 * Logging — levels, structured fields, Logcat and optional file sinks.
 *
 * Implements [com.fk.core.pluggable.logging.PluggableLogger] via [FkLogger] so
 * feature modules stay on Pluggable contracts while the host wires concrete sinks.
 *
 * Conceptually aligned with iOS `FKCoreKit` Logger; Android-shaped APIs
 * (Logcat instead of ANSI console, modest default file caps).
 */
object Logging {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.2"

  /** Default Logcat / event tag. */
  const val DEFAULT_TAG: String = "FkLog"

  /** Default subdirectory under app files for [FileLogSink]. */
  const val DEFAULT_LOG_DIR: String = "fk_logs"

  /**
   * Logcat-only [FkLogger] (typical debug wiring).
   */
  fun logcatLogger(
    tag: String = DEFAULT_TAG,
    minimumLevel: LogLevel = LogLevel.Debug,
  ): FkLogger = FkLogger(
    sinks = listOf(LogcatSink()),
    config = LoggingConfig(
      minimumLevel = minimumLevel,
      defaultTag = tag,
    ),
  )

  /**
   * Logcat + file [FkLogger]. File caps stay small by default to limit disk use.
   *
   * @param context Any context; [Context.getApplicationContext] is used for the log directory.
   */
  fun logcatAndFileLogger(
    context: Context,
    tag: String = DEFAULT_TAG,
    minimumLevel: LogLevel = LogLevel.Debug,
    logDirectory: File = File(context.applicationContext.filesDir, DEFAULT_LOG_DIR),
    maxFileBytes: Long = FileLogSink.DEFAULT_MAX_FILE_BYTES,
    maxTotalBytes: Long = FileLogSink.DEFAULT_MAX_TOTAL_BYTES,
  ): FkLogger {
    val fileSink = FileLogSink(
      directory = logDirectory,
      maxFileBytes = maxFileBytes,
      maxTotalBytes = maxTotalBytes,
    )
    return FkLogger(
      sinks = listOf(LogcatSink(), fileSink),
      config = LoggingConfig(
        minimumLevel = minimumLevel,
        defaultTag = tag,
      ),
      fileSink = fileSink,
    )
  }
}
