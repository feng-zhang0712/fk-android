package com.fk.core.file

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-memory [FileTransferManaging] for tests and samples.
 *
 * Simulates download progress with pause / resume support (no network).
 */
class MockFileTransferManager : FileTransferManaging {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val nextId = AtomicInteger(1)
  private val snapshots = ConcurrentHashMap<Int, MutableStateFlow<TransferSnapshot?>>()
  private val requests = ConcurrentHashMap<Int, DownloadRequest>()
  private val progressBytes = ConcurrentHashMap<Int, Long>()
  private val jobs = ConcurrentHashMap<Int, Job>()
  private val uploadResults = ConcurrentHashMap<Int, UploadResult>()

  @Volatile
  var simulatedTotalBytes: Long = 100_000L

  @Volatile
  var stepDelayMs: Long = 50L

  override suspend fun download(
    request: DownloadRequest,
    onProgress: ((TransferProgress) -> Unit)?,
  ): Int {
    val id = nextId.getAndIncrement()
    request.destinationDirectory.mkdirs()
    val dest = File(
      request.destinationDirectory,
      request.fileName ?: "mock.bin",
    )
    requests[id] = request
    emit(
      TransferSnapshot(
        id = id,
        kind = TransferKind.Download,
        state = TransferState.Running,
        sourceUrl = request.sourceUrl,
        destinationPath = dest.absolutePath,
        totalBytes = simulatedTotalBytes,
      ),
    )
    jobs[id] = scope.launch {
      simulateDownload(id, request, dest, onProgress, startAt = 0L)
    }
    return id
  }

  override suspend fun upload(
    request: UploadRequest,
    onProgress: ((TransferProgress) -> Unit)?,
  ): Int {
    val id = nextId.getAndIncrement()
    emit(
      TransferSnapshot(
        id = id,
        kind = TransferKind.Upload,
        state = TransferState.Running,
        sourceUrl = request.url,
        totalBytes = simulatedTotalBytes,
      ),
    )
    jobs[id] = scope.launch {
      var completed = 0L
      while (completed < simulatedTotalBytes) {
        delay(stepDelayMs)
        if (snapshots[id]?.value?.state == TransferState.Cancelled) return@launch
        completed = (completed + simulatedTotalBytes / 10).coerceAtMost(simulatedTotalBytes)
        onProgress?.invoke(
          TransferProgress(
            id,
            completed.toDouble() / simulatedTotalBytes,
            completed,
            simulatedTotalBytes,
          ),
        )
        emit(
          TransferSnapshot(
            id = id,
            kind = TransferKind.Upload,
            state = TransferState.Running,
            sourceUrl = request.url,
            completedBytes = completed,
            totalBytes = simulatedTotalBytes,
          ),
        )
      }
      uploadResults[id] = UploadResult(
        taskId = id,
        responseBody = ByteArray(0),
        statusCode = 200,
      )
      emit(
        TransferSnapshot(
          id = id,
          kind = TransferKind.Upload,
          state = TransferState.Completed,
          sourceUrl = request.url,
          completedBytes = simulatedTotalBytes,
          totalBytes = simulatedTotalBytes,
        ),
      )
    }
    return id
  }

  override suspend fun pauseDownload(taskId: Int) {
    val current = snapshots[taskId]?.value ?: return
    if (current.kind != TransferKind.Download) return
    progressBytes[taskId] = current.completedBytes
    emit(current.copy(state = TransferState.Paused))
  }

  override suspend fun resumeDownload(
    taskId: Int,
    headers: Map<String, String>?,
  ) {
    val request = requests[taskId] ?: return
    val startAt = progressBytes[taskId] ?: 0L
    val dest = File(
      request.destinationDirectory,
      request.fileName ?: "mock.bin",
    )
    if (headers != null) {
      requests[taskId] = request.copy(headers = headers)
    }
    emit(
      TransferSnapshot(
        id = taskId,
        kind = TransferKind.Download,
        state = TransferState.Running,
        sourceUrl = request.sourceUrl,
        destinationPath = dest.absolutePath,
        completedBytes = startAt,
        totalBytes = simulatedTotalBytes,
      ),
    )
    jobs[taskId] = scope.launch {
      simulateDownload(taskId, requests[taskId] ?: request, dest, null, startAt = startAt)
    }
  }

  override suspend fun cancel(taskId: Int) {
    jobs[taskId]?.cancel()
    val current = snapshots[taskId]?.value ?: return
    emit(current.copy(state = TransferState.Cancelled))
  }

  override suspend fun cancelAll() {
    snapshots.keys.toList().forEach { cancel(it) }
  }

  override fun observe(taskId: Int): Flow<TransferSnapshot?> =
    snapshots.getOrPut(taskId) { MutableStateFlow(null) }

  override suspend fun persistedTransfers(): List<PersistedTransfer> =
    snapshots.values.mapNotNull { flow ->
      val s = flow.value ?: return@mapNotNull null
      if (s.state != TransferState.Paused) return@mapNotNull null
      PersistedTransfer(
        id = s.id,
        kind = s.kind,
        state = s.state,
        sourceUrl = s.sourceUrl,
        destinationPath = s.destinationPath,
        completedBytes = s.completedBytes,
        totalBytes = s.totalBytes,
        updatedAtEpochMs = System.currentTimeMillis(),
      )
    }

  override fun activeSnapshots(): List<TransferSnapshot> =
    snapshots.values.mapNotNull { it.value }

  override fun downloadResult(taskId: Int): DownloadResult? {
    val snap = snapshots[taskId]?.value ?: return null
    if (snap.state != TransferState.Completed || snap.kind != TransferKind.Download) return null
    val path = snap.destinationPath ?: return null
    return DownloadResult(taskId = taskId, file = File(path), sourceUrl = snap.sourceUrl)
  }

  override fun uploadResult(taskId: Int): UploadResult? = uploadResults[taskId]

  private suspend fun simulateDownload(
    id: Int,
    request: DownloadRequest,
    dest: File,
    onProgress: ((TransferProgress) -> Unit)?,
    startAt: Long,
  ) {
    var completed = startAt
    while (completed < simulatedTotalBytes) {
      delay(stepDelayMs)
      val snap = snapshots[id]?.value
      if (snap?.state == TransferState.Paused) {
        progressBytes[id] = completed
        return
      }
      if (snap?.state == TransferState.Cancelled) return
      completed = (completed + simulatedTotalBytes / 10).coerceAtMost(simulatedTotalBytes)
      progressBytes[id] = completed
      onProgress?.invoke(
        TransferProgress(
          id,
          completed.toDouble() / simulatedTotalBytes,
          completed,
          simulatedTotalBytes,
        ),
      )
      emit(
        TransferSnapshot(
          id = id,
          kind = TransferKind.Download,
          state = TransferState.Running,
          sourceUrl = request.sourceUrl,
          destinationPath = dest.absolutePath,
          completedBytes = completed,
          totalBytes = simulatedTotalBytes,
        ),
      )
    }
    dest.writeBytes(ByteArray(16))
    emit(
      TransferSnapshot(
        id = id,
        kind = TransferKind.Download,
        state = TransferState.Completed,
        sourceUrl = request.sourceUrl,
        destinationPath = dest.absolutePath,
        completedBytes = simulatedTotalBytes,
        totalBytes = simulatedTotalBytes,
      ),
    )
  }

  private fun emit(snapshot: TransferSnapshot) {
    snapshots.getOrPut(snapshot.id) { MutableStateFlow(null) }.value = snapshot
  }
}
