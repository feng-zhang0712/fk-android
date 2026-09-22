package com.fk.core.file

import android.content.Context
import okhttp3.OkHttpClient

/**
 * File transfers — OkHttp façade (resumable download / multipart upload / persistence).
 *
 * Conceptually aligned with iOS `FKCoreKit` FileManager **transfer** APIs.
 * Sandbox CRUD and ZIP are intentionally out of scope for Phase C1.
 *
 * Named [FileTransfers] (not `File`) to avoid clashing with `java.io.File`.
 */
object FileTransfers {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"

  /** Builds the default [FileTransferManager]. */
  fun create(
    context: Context,
    configuration: FileTransferConfiguration = FileTransferConfiguration(),
    client: OkHttpClient? = null,
  ): FileTransferManager =
    FileTransferManager(
      context = context,
      configuration = configuration,
      client = client,
    )

  /** Builds an in-memory mock for tests and samples. */
  fun mock(): MockFileTransferManager = MockFileTransferManager()
}
