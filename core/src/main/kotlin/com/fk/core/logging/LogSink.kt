package com.fk.core.logging

import java.util.Calendar
import java.util.Locale

/**
 * Destination that receives filtered [LogEvent]s from [FkLogger].
 */
fun interface LogSink {
  /** Persists or prints [event]. Must not throw to callers of [FkLogger]. */
  fun write(event: LogEvent)
}

/**
 * Formats [LogEvent] into a single line for file / secondary sinks.
 */
fun interface LogFormatter {
  /** Builds one printable line (without trailing newline). */
  fun format(event: LogEvent, config: LoggingConfig): String
}

/**
 * Default line format: `[time] LEVEL tag message key=value …`.
 */
object DefaultLogFormatter : LogFormatter {
  override fun format(event: LogEvent, config: LoggingConfig): String {
    val parts = ArrayList<String>(4 + event.fields.size)
    if (config.includeTimestamp) {
      parts += formatTimestamp(event.timestampMillis)
    }
    parts += event.level.label
    parts += event.tag
    parts += event.message
    appendFields(parts, event.fields)
    val line = parts.joinToString(separator = " ")
    val stack = event.throwable?.stackTraceToString()?.trim()
    return if (stack.isNullOrEmpty()) line else "$line\n$stack"
  }

  private fun formatTimestamp(epochMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return String.format(
      Locale.US,
      "%04d-%02d-%02dT%02d:%02d:%02d.%03d",
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
      cal.get(Calendar.HOUR_OF_DAY),
      cal.get(Calendar.MINUTE),
      cal.get(Calendar.SECOND),
      cal.get(Calendar.MILLISECOND),
    )
  }
}

/** Appends sorted `key=value` tokens for structured fields. */
internal fun appendFields(target: MutableList<String>, fields: Map<String, String>) {
  if (fields.isEmpty()) return
  fields.entries.sortedBy { it.key }.forEach { (key, value) ->
    target += "$key=$value"
  }
}

/** Formats structured fields as a single space-separated suffix (or empty). */
internal fun formatFieldsSuffix(fields: Map<String, String>): String {
  if (fields.isEmpty()) return ""
  val parts = ArrayList<String>(fields.size)
  appendFields(parts, fields)
  return parts.joinToString(separator = " ")
}
