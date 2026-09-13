package com.fk.core.file

import kotlinx.coroutines.flow.Flow

/**
 * Pluggable contract for resumable file transfers.
 *
 * Conceptually aligned with iOS `FKTransferManaging`.
 * Engine is OkHttp (Range resume for downloads, multipart for uploads).
 */
interface FileTransferManaging {
  /**
   * Enqueues a download. Returns a task id immediately; work may be queued.
   *
   * @throws FileTransferError.InvalidUrl when [DownloadRequest.sourceUrl] is not http(s).
   */
  suspend fun download(
    request: DownloadRequest,
    onProgress: ((TransferProgress) -> Unit)? = null,
  ): Int

  /**
   * Enqueues a multipart upload. Returns a task id immediately.
   *
   * @throws FileTransferError.FileNotFound when a part file is missing.
   */
  suspend fun upload(
    request: UploadRequest,
    onProgress: ((TransferProgress) -> Unit)? = null,
  ): Int

  /** Pauses a running download (uploads are cancelled; re-enqueue to retry). */
  suspend fun pauseDownload(taskId: Int)

  /** Resumes a paused download from the persisted partial file. */
  suspend fun resumeDownload(
    taskId: Int,
    headers: Map<String, String>? = null,
  )

  /** Cancels one transfer. */
  suspend fun cancel(taskId: Int)

  /** Cancels all active / queued transfers. */
  suspend fun cancelAll()

  /** Observes live state for [taskId]. */
  fun observe(taskId: Int): Flow<TransferSnapshot?>

  /** Persisted snapshots (including paused downloads with partial files). */
  suspend fun persistedTransfers(): List<PersistedTransfer>

  /** In-memory snapshots for active / recently completed tasks. */
  fun activeSnapshots(): List<TransferSnapshot>

  /** Completed download result for [taskId], or `null` if not completed. */
  fun downloadResult(taskId: Int): DownloadResult?

  /** Completed upload result for [taskId], or `null` if not completed. */
  fun uploadResult(taskId: Int): UploadResult?
}
