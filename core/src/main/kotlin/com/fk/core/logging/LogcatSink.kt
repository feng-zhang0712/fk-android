package com.fk.core.logging

import android.util.Log

/**
 * Writes events to Android Logcat.
 *
 * Structured [LogEvent.fields] are appended to the message; the throwable is
 * passed to Logcat when present.
 */
class LogcatSink : LogSink {
  override fun write(event: LogEvent) {
    val fieldsSuffix = formatFieldsSuffix(event.fields)
    val text = if (fieldsSuffix.isEmpty()) {
      event.message
    } else {
      "${event.message} $fieldsSuffix"
    }
    when (event.level) {
      LogLevel.Verbose -> Log.v(event.tag, text, event.throwable)
      LogLevel.Debug -> Log.d(event.tag, text, event.throwable)
      LogLevel.Info -> Log.i(event.tag, text, event.throwable)
      LogLevel.Warning -> Log.w(event.tag, text, event.throwable)
      LogLevel.Error -> Log.e(event.tag, text, event.throwable)
    }
  }
}
