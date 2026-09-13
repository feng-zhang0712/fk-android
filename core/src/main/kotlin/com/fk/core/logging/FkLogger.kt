package com.fk.core.logging

import com.fk.core.pluggable.logging.PluggableLogLevel
import com.fk.core.pluggable.logging.PluggableLogger

/**
 * Concrete [PluggableLogger] that fans out to one or more [LogSink]s.
 *
 * Prefer injecting this (or the [PluggableLogger] interface) rather than a
 * process-wide singleton.
 *
 * @param sinks Destinations that receive accepted events (e.g. [LogcatSink], [FileLogSink]).
 * @param config Initial filter / tag settings.
 * @param fileSink Optional handle for sample / diagnostics when a file sink is present.
 */
class FkLogger(
  private val sinks: List<LogSink>,
  config: LoggingConfig = LoggingConfig(),
  val fileSink: FileLogSink? = null,
) : PluggableLogger {
  @Volatile
  private var _config: LoggingConfig = config

  /** Current filter and formatting options. */
  var config: LoggingConfig
    get() = _config
    set(value) {
      _config = value
      fileSink?.updateConfig(value)
    }

  init {
    require(sinks.isNotEmpty()) { "At least one LogSink is required" }
    if (fileSink != null) {
      require(sinks.any { it === fileSink }) {
        "fileSink must also be included in sinks"
      }
    }
    fileSink?.updateConfig(config)
  }

  override var minimumLevel: PluggableLogLevel
    get() = _config.minimumLevel.toPluggable()
    set(value) {
      config = _config.copy(minimumLevel = value.toLogLevel())
    }

  override fun log(
    level: PluggableLogLevel,
    message: () -> String,
    throwable: Throwable?,
  ) {
    log(
      level = level.toLogLevel(),
      message = message,
      fields = emptyMap(),
      throwable = throwable,
    )
  }

  /**
   * Logs with optional structured [fields] and [tag] override.
   *
   * [message] is only evaluated when the event passes level filtering.
   */
  fun log(
    level: LogLevel,
    message: () -> String,
    fields: Map<String, String> = emptyMap(),
    throwable: Throwable? = null,
    tag: String? = null,
  ) {
    val snapshot = _config
    if (!snapshot.isEnabled) return
    if (level.ordinal < snapshot.minimumLevel.ordinal) return
    val event = LogEvent(
      level = level,
      message = message(),
      tag = tag?.takeIf { it.isNotBlank() } ?: snapshot.defaultTag,
      fields = fields,
      throwable = throwable,
    )
    for (sink in sinks) {
      runCatching { sink.write(event) }
    }
  }

  /** Logs at [LogLevel.Verbose]. */
  fun verbose(
    fields: Map<String, String> = emptyMap(),
    tag: String? = null,
    message: () -> String,
  ) = log(LogLevel.Verbose, message, fields, tag = tag)

  /** Logs at [LogLevel.Debug]. */
  fun debug(
    fields: Map<String, String> = emptyMap(),
    tag: String? = null,
    message: () -> String,
  ) = log(LogLevel.Debug, message, fields, tag = tag)

  /** Logs at [LogLevel.Info]. */
  fun info(
    fields: Map<String, String> = emptyMap(),
    tag: String? = null,
    message: () -> String,
  ) = log(LogLevel.Info, message, fields, tag = tag)

  /** Logs at [LogLevel.Warning]. */
  fun warning(
    fields: Map<String, String> = emptyMap(),
    tag: String? = null,
    message: () -> String,
  ) = log(LogLevel.Warning, message, fields, tag = tag)

  /** Logs at [LogLevel.Error]. */
  fun error(
    fields: Map<String, String> = emptyMap(),
    throwable: Throwable? = null,
    tag: String? = null,
    message: () -> String,
  ) = log(LogLevel.Error, message, fields, throwable, tag)
}
