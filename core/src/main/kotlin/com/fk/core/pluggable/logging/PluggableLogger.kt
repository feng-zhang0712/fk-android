package com.fk.core.pluggable.logging

/**
 * Severity for [PluggableLogger] implementations.
 *
 * Named distinctly from any future `:core` `logging` package levels.
 */
enum class PluggableLogLevel {
  Verbose,
  Debug,
  Info,
  Warning,
  Error,
}

/**
 * Pluggable logger used across networking and feature modules.
 *
 * Wire a Logcat logger in debug and a redacted / file-backed logger in production
 * (see future `:core` `logging` package).
 */
interface PluggableLogger {
  /** Minimum level emitted by this logger. */
  var minimumLevel: PluggableLogLevel

  /**
   * Writes a log line when [level] is at or above [minimumLevel].
   *
   * @param message Lazy message to avoid work when filtered out.
   */
  fun log(
    level: PluggableLogLevel,
    message: () -> String,
    throwable: Throwable? = null,
  )
}

/** Logs at [PluggableLogLevel.Debug]. */
fun PluggableLogger.debug(message: () -> String) =
  log(PluggableLogLevel.Debug, message)

/** Logs at [PluggableLogLevel.Info]. */
fun PluggableLogger.info(message: () -> String) =
  log(PluggableLogLevel.Info, message)

/** Logs at [PluggableLogLevel.Error]. */
fun PluggableLogger.error(message: () -> String, throwable: Throwable? = null) =
  log(PluggableLogLevel.Error, message, throwable)
