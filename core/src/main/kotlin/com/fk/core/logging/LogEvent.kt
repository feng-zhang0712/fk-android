package com.fk.core.logging

/**
 * Normalized log payload passed to [LogSink] implementations.
 *
 * @property level Severity.
 * @property message Human-readable body.
 * @property tag Logcat / line tag.
 * @property fields Structured key-value context (stringified at the call site).
 * @property throwable Optional failure cause.
 * @property timestampMillis Event time in epoch millis.
 */
data class LogEvent(
  val level: LogLevel,
  val message: String,
  val tag: String,
  val fields: Map<String, String> = emptyMap(),
  val throwable: Throwable? = null,
  val timestampMillis: Long = System.currentTimeMillis(),
)
