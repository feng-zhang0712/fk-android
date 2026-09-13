package com.fk.core.logging

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Appends formatted lines under [directory] with daily rotation and size caps.
 *
 * Defaults stay modest on purpose (see [DEFAULT_MAX_FILE_BYTES] /
 * [DEFAULT_MAX_TOTAL_BYTES]) so logging does not exhaust limited device storage.
 *
 * Writes run on a single background thread; [clear] / [logFiles] / [readRecent]
 * synchronize with that queue.
 */
class FileLogSink(
  private val directory: File,
  private val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
  private val maxTotalBytes: Long = DEFAULT_MAX_TOTAL_BYTES,
  private val rotatesDaily: Boolean = true,
  private val formatter: LogFormatter = DefaultLogFormatter,
  private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "fk-file-log").apply { isDaemon = true }
  },
) : LogSink {
  init {
    require(maxFileBytes > 0) { "maxFileBytes must be > 0" }
    require(maxTotalBytes >= maxFileBytes) { "maxTotalBytes must be >= maxFileBytes" }
  }

  @Volatile
  private var latestConfig: LoggingConfig = LoggingConfig()

  /** Updates formatting options used for subsequent lines. */
  fun updateConfig(config: LoggingConfig) {
    latestConfig = config
  }

  override fun write(event: LogEvent) {
    val config = latestConfig
    val line = formatter.format(event, config)
    if (executor.isShutdown) return
    executor.execute {
      runCatching {
        ensureDirectory()
        val target = resolveWritableFile(event.timestampMillis)
        appendLine(target, line)
        enforceStorageLimit()
      }
    }
  }

  /** Log files currently under [directory], oldest first. */
  fun logFiles(): List<File> = runOnWriter { listedFiles() }

  /**
   * Blocks until previously queued writes have finished.
   *
   * Useful for demos / tests that read files immediately after logging.
   */
  fun awaitIdle() {
    runOnWriter { }
  }

  /** Deletes every managed log file. */
  fun clear() {
    runOnWriter {
      listedFiles().forEach { it.delete() }
    }
  }

  /**
   * Reads up to [maxChars] from the newest log file (from the end).
   *
   * Useful for sample / debug screens. Returns an empty string when no file exists.
   * Reads only a tail byte window — does not load the entire file into memory.
   */
  fun readRecent(maxChars: Int = 2_000): String {
    require(maxChars > 0)
    return runOnWriter {
      val newest = listedFiles().lastOrNull() ?: return@runOnWriter ""
      if (!newest.isFile) return@runOnWriter ""
      readTail(newest, maxChars)
    }
  }

  /** Shuts down the background writer. Optional; process exit also clears daemon threads. */
  fun close() {
    executor.shutdown()
  }

  private fun <T> runOnWriter(block: () -> T): T {
    check(!executor.isShutdown) { "FileLogSink is closed" }
    return try {
      executor.submit(block).get()
    } catch (e: ExecutionException) {
      throw e.cause ?: e
    }
  }

  private fun ensureDirectory() {
    if (!directory.exists()) {
      directory.mkdirs()
    }
  }

  private fun resolveWritableFile(timestampMillis: Long): File {
    val dayKey = if (rotatesDaily) dayKey(timestampMillis) else "shared"
    val baseName = "fk-log-$dayKey"
    val primary = File(directory, "$baseName.log")
    if (!primary.exists() || primary.length() < maxFileBytes) {
      return primary
    }
    var index = 2
    while (true) {
      val candidate = File(directory, "$baseName-$index.log")
      if (!candidate.exists() || candidate.length() < maxFileBytes) {
        return candidate
      }
      index += 1
    }
  }

  private fun appendLine(file: File, line: String) {
    file.appendText(line + "\n", StandardCharsets.UTF_8)
  }

  private fun readTail(file: File, maxChars: Int): String {
    val length = file.length()
    if (length <= 0L) return ""
    // Over-read a few bytes so a multi-byte UTF-8 sequence at the cut is rare for log text.
    val bytesToRead = minOf(length, maxChars.toLong() + 4L).toInt()
    val bytes = ByteArray(bytesToRead)
    RandomAccessFile(file, "r").use { raf ->
      raf.seek(length - bytesToRead)
      raf.readFully(bytes)
    }
    val decoded = String(bytes, StandardCharsets.UTF_8)
    return if (decoded.length <= maxChars) decoded else decoded.takeLast(maxChars)
  }

  private fun enforceStorageLimit() {
    var files = listedFiles()
    var total = files.sumOf { it.length() }
    while (total > maxTotalBytes && files.isNotEmpty()) {
      val oldest = files.first()
      val size = oldest.length()
      if (oldest.delete()) {
        total -= size
      }
      files = listedFiles()
      if (files.isEmpty()) break
      // Guard against a non-deletable oldest file looping forever.
      if (files.first() == oldest) break
    }
  }

  private fun listedFiles(): List<File> {
    if (!directory.isDirectory) return emptyList()
    return directory.listFiles { file ->
      file.isFile && file.name.startsWith("fk-log-") && file.name.endsWith(".log")
    }
      ?.sortedBy { it.lastModified() }
      ?: emptyList()
  }

  private fun dayKey(timestampMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    return String.format(
      Locale.US,
      "%04d-%02d-%02d",
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  companion object {
    /** Default max size of a single log file (512 KiB). */
    const val DEFAULT_MAX_FILE_BYTES: Long = 512L * 1024

    /** Default total cap for all log files (2 MiB). */
    const val DEFAULT_MAX_TOTAL_BYTES: Long = 2L * 1024 * 1024
  }
}
