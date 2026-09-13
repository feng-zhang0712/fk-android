package com.fk.core.logging

import com.fk.core.pluggable.logging.PluggableLogLevel

/**
 * Log severity for the `:core` logging package.
 *
 * Distinct from [PluggableLogLevel]; convert with [toPluggable] / [toLogLevel].
 */
enum class LogLevel {
  Verbose,
  Debug,
  Info,
  Warning,
  Error,
  ;

  /** Short uppercase label for formatted lines. */
  val label: String
    get() = when (this) {
      Verbose -> "VERBOSE"
      Debug -> "DEBUG"
      Info -> "INFO"
      Warning -> "WARNING"
      Error -> "ERROR"
    }

  /** Maps to the Pluggable contract level. */
  fun toPluggable(): PluggableLogLevel = when (this) {
    Verbose -> PluggableLogLevel.Verbose
    Debug -> PluggableLogLevel.Debug
    Info -> PluggableLogLevel.Info
    Warning -> PluggableLogLevel.Warning
    Error -> PluggableLogLevel.Error
  }
}

/** Maps a Pluggable level into [LogLevel]. */
fun PluggableLogLevel.toLogLevel(): LogLevel = when (this) {
  PluggableLogLevel.Verbose -> LogLevel.Verbose
  PluggableLogLevel.Debug -> LogLevel.Debug
  PluggableLogLevel.Info -> LogLevel.Info
  PluggableLogLevel.Warning -> LogLevel.Warning
  PluggableLogLevel.Error -> LogLevel.Error
}
