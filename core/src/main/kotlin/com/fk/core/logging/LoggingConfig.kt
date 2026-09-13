package com.fk.core.logging

/**
 * Runtime configuration for [FkLogger].
 *
 * @property isEnabled Master switch; when false, all levels are dropped.
 * @property minimumLevel Lowest level that is emitted (inclusive).
 * @property defaultTag Tag used when a call does not override it.
 * @property includeTimestamp When true, formatted sink lines include ISO-like time.
 */
data class LoggingConfig(
  val isEnabled: Boolean = true,
  val minimumLevel: LogLevel = LogLevel.Debug,
  val defaultTag: String = Logging.DEFAULT_TAG,
  val includeTimestamp: Boolean = true,
)
