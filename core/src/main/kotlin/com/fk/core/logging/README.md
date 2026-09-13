# Logging (`com.fk.core.logging`)

Levels, structured fields, Logcat + optional file sink. Implements Pluggable
[`PluggableLogger`](../pluggable/logging/PluggableLogger.kt). Phase **A4**.

## Layout

| Type | Role |
|------|------|
| `Logging` | Package marker / version / factory helpers |
| `LogLevel` | Severity (`Verbose`…`Error`) + Pluggable mapping |
| `LogEvent` | Payload (message, tag, fields, throwable) |
| `LoggingConfig` | Enable flag, minimum level, default tag |
| `LogSink` / `LogFormatter` | Sink + line formatting contracts |
| `LogcatSink` | Android Logcat destination |
| `FileLogSink` | Daily rotation + size caps (defaults 512 KiB / 2 MiB total) |
| `FkLogger` | `PluggableLogger` + structured-field helpers |

## Usage

```kotlin
val logger = Logging.logcatAndFileLogger(context)
logger.info(fields = mapOf("userId" to "u-1")) { "Signed in" }
logger.error(throwable = e) { "Sync failed" }

// Pluggable wiring
PluggableServices(logger = logger)
```

## Notes

- File caps are intentionally small; raise them only when the product needs larger retention.
- After [FileLogSink.close], further `logFiles` / `clear` / `readRecent` throw; `write` becomes a no-op.
- Crash / signal hooks, ANSI colors, and pretty-print helpers are deferred (not required for A4).
- `MockPluggableLogger` remains for tests that do not need file I/O.
