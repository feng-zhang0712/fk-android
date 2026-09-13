package com.fk.core.file

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Default [FileTransferManaging] implementation on OkHttp.
 *
 * Conceptually aligned with iOS `FKFileManager` transfer APIs (download pause/resume,
 * multipart upload, persisted snapshots). Does not port sandbox CRUD / ZIP.
 */
class FileTransferManager(
  context: Context,
  private val configuration: FileTransferConfiguration = FileTransferConfiguration(),
  client: okhttp3.OkHttpClient? = null,
) : FileTransferManaging {

  private val appContext = context.applicationContext
  private val workingDir: File =
    File(appContext.cacheDir, configuration.workingDirectoryName).also { it.mkdirs() }
  private val store = FileTransferStore(appContext, configuration.persistenceKey)
  private val engine = FileTransferEngine(configuration, client)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val semaphore = Semaphore(configuration.maxConcurrentTransfers.coerceAtLeast(1))
  private val nextId = AtomicInteger(1)

  private val tasks = ConcurrentHashMap<Int, TaskRecord>()
  private val snapshots = ConcurrentHashMap<Int, MutableStateFlow<TransferSnapshot?>>()

  init {
    // Only paused downloads are meaningful across process death; drop anything else.
    val paused = store.loadAll()
      .filter { it.kind == TransferKind.Download && it.state == TransferState.Paused }
    store.saveAll(paused)
    paused.forEach { persisted ->
      val record = TaskRecord(
        id = persisted.id,
        kind = TransferKind.Download,
        sourceUrl = persisted.sourceUrl,
        destinationDirectory = persisted.destinationPath?.let { File(it).parentFile },
        fileName = persisted.destinationPath?.let { File(it).name },
        partialFile = persisted.partialPath?.let { File(it) },
        completedBytes = persisted.completedBytes,
        totalBytes = persisted.totalBytes,
      )
      tasks[persisted.id] = record
      emit(
        TransferSnapshot(
          id = persisted.id,
          kind = TransferKind.Download,
          state = TransferState.Paused,
          sourceUrl = persisted.sourceUrl,
          destinationPath = persisted.destinationPath,
          completedBytes = persisted.completedBytes,
          totalBytes = persisted.totalBytes,
        ),
      )
      if (persisted.id >= nextId.get()) {
        nextId.set(persisted.id + 1)
      }
    }
  }

  override suspend fun download(
    request: DownloadRequest,
    onProgress: ((TransferProgress) -> Unit)?,
  ): Int {
    val url = request.sourceUrl.trim()
    if (url.toHttpUrlOrNull() == null) {
      throw FileTransferError.InvalidUrl(request.sourceUrl)
    }
    request.destinationDirectory.mkdirs()
    val id = nextId.getAndIncrement()
    val fileName = request.fileName?.takeIf { it.isNotBlank() }
      ?: url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
    val partial = File(workingDir, "$id.partial")
    val record = TaskRecord(
      id = id,
      kind = TransferKind.Download,
      sourceUrl = url,
      destinationDirectory = request.destinationDirectory,
      fileName = fileName,
      partialFile = partial,
      downloadHeaders = request.headers,
      onProgress = onProgress,
    )
    tasks[id] = record
    emit(
      TransferSnapshot(
        id = id,
        kind = TransferKind.Download,
        state = TransferState.Queued,
        sourceUrl = url,
        destinationPath = File(request.destinationDirectory, fileName).absolutePath,
      ),
    )
    record.job = scope.launch { runDownload(record) }
    return id
  }

  override suspend fun upload(
    request: UploadRequest,
    onProgress: ((TransferProgress) -> Unit)?,
  ): Int {
    if (request.url.toHttpUrlOrNull() == null) {
      throw FileTransferError.InvalidUrl(request.url)
    }
    for (part in request.files) {
      if (!part.file.exists()) throw FileTransferError.FileNotFound(part.file.absolutePath)
    }
    val id = nextId.getAndIncrement()
    val record = TaskRecord(
      id = id,
      kind = TransferKind.Upload,
      sourceUrl = request.url,
      uploadRequest = request,
      onProgress = onProgress,
    )
    tasks[id] = record
    emit(
      TransferSnapshot(
        id = id,
        kind = TransferKind.Upload,
        state = TransferState.Queued,
        sourceUrl = request.url,
      ),
    )
    record.job = scope.launch { runUpload(record) }
    return id
  }

  override suspend fun pauseDownload(taskId: Int) {
    val record = tasks[taskId] ?: return
    if (record.kind != TransferKind.Download) return
    val current = snapshots[taskId]?.value?.state
    if (
      current == TransferState.Paused ||
      current == TransferState.Completed ||
      current == TransferState.Cancelled ||
      current == TransferState.Failed
    ) {
      return
    }
    record.pauseRequested = true
    record.callHolder.cancel()
    // Queued tasks are not on the wire yet — flip to Paused immediately.
    if (current == TransferState.Queued) {
      emit(record.toSnapshot(TransferState.Paused))
      persistPaused(record)
    }
  }

  override suspend fun resumeDownload(
    taskId: Int,
    headers: Map<String, String>?,
  ) {
    val record = tasks[taskId] ?: run {
      val persisted = store.loadAll().firstOrNull { it.id == taskId }
        ?: throw FileTransferError.TaskNotFound(taskId)
      if (persisted.kind != TransferKind.Download || persisted.state != TransferState.Paused) {
        throw FileTransferError.TaskNotFound(taskId)
      }
      TaskRecord(
        id = persisted.id,
        kind = TransferKind.Download,
        sourceUrl = persisted.sourceUrl,
        destinationDirectory = persisted.destinationPath?.let { File(it).parentFile },
        fileName = persisted.destinationPath?.let { File(it).name },
        partialFile = persisted.partialPath?.let { File(it) },
        completedBytes = persisted.completedBytes,
        totalBytes = persisted.totalBytes,
      ).also { tasks[taskId] = it }
    }
    if (record.kind != TransferKind.Download) return
    if (record.job?.isActive == true) return
    if (headers != null) {
      record.downloadHeaders = headers
    }
    record.pauseRequested = false
    record.cancelRequested = false
    record.callHolder = FileTransferEngine.CallHolder()
    emit(record.toSnapshot(TransferState.Queued))
    record.job = scope.launch { runDownload(record) }
  }

  override suspend fun cancel(taskId: Int) {
    val record = tasks[taskId] ?: return
    record.cancelRequested = true
    record.callHolder.cancel()
    record.job?.cancel()
    record.partialFile?.delete()
    emit(record.toSnapshot(TransferState.Cancelled))
    store.remove(taskId)
  }

  override suspend fun cancelAll() {
    tasks.keys.toList().forEach { cancel(it) }
  }

  override fun observe(taskId: Int): Flow<TransferSnapshot?> =
    snapshots.getOrPut(taskId) { MutableStateFlow(null) }

  override suspend fun persistedTransfers(): List<PersistedTransfer> =
    withContext(Dispatchers.IO) { store.loadAll() }

  override fun activeSnapshots(): List<TransferSnapshot> =
    snapshots.values.mapNotNull { it.value }

  override fun downloadResult(taskId: Int): DownloadResult? {
    val record = tasks[taskId] ?: return null
    val file = record.resultFile ?: return null
    return DownloadResult(taskId = taskId, file = file, sourceUrl = record.sourceUrl)
  }

  override fun uploadResult(taskId: Int): UploadResult? =
    tasks[taskId]?.uploadResult

  private suspend fun runDownload(record: TaskRecord) {
    semaphore.acquire()
    try {
      if (record.cancelRequested) {
        emit(record.toSnapshot(TransferState.Cancelled))
        store.remove(record.id)
        return
      }
      if (record.pauseRequested) {
        emit(record.toSnapshot(TransferState.Paused))
        persistPaused(record)
        return
      }
      emit(record.toSnapshot(TransferState.Running))

      val partial = record.partialFile
      val destDir = record.destinationDirectory
      val fileName = record.fileName
      if (partial == null || destDir == null || fileName.isNullOrBlank()) {
        emit(record.toSnapshot(TransferState.Failed, "missing download paths"))
        store.remove(record.id)
        return
      }
      destDir.mkdirs()

      val startOffset = if (partial.exists()) partial.length() else 0L
      record.completedBytes = startOffset
      record.callHolder = FileTransferEngine.CallHolder()

      try {
        val completed = engine.downloadToPartial(
          url = record.sourceUrl,
          partialFile = partial,
          startOffset = startOffset,
          headers = record.downloadHeaders,
          callHolder = record.callHolder,
          onProgress = { done, total ->
            record.completedBytes = done
            record.totalBytes = total
            val progress = if (total > 0L) done.toDouble() / total.toDouble() else 0.0
            record.onProgress?.invoke(
              TransferProgress(record.id, progress.coerceIn(0.0, 1.0), done, total),
            )
            emit(record.toSnapshot(TransferState.Running))
          },
        )
        if (record.pauseRequested) {
          record.completedBytes = if (partial.exists()) partial.length() else completed
          emit(record.toSnapshot(TransferState.Paused))
          persistPaused(record)
          return
        }
        if (record.cancelRequested) {
          partial.delete()
          emit(record.toSnapshot(TransferState.Cancelled))
          store.remove(record.id)
          return
        }

        val destination = File(destDir, fileName)
        if (destination.exists()) destination.delete()
        if (!partial.renameTo(destination)) {
          partial.copyTo(destination, overwrite = true)
          partial.delete()
        }
        record.completedBytes = destination.length()
        record.totalBytes = destination.length()
        record.resultFile = destination
        emit(
          record.toSnapshot(TransferState.Completed).copy(
            destinationPath = destination.absolutePath,
          ),
        )
        store.remove(record.id)
      } catch (cancelled: CancellationException) {
        partial.delete()
        emit(record.toSnapshot(TransferState.Cancelled))
        store.remove(record.id)
        throw cancelled
      } catch (t: Throwable) {
        if (record.pauseRequested) {
          record.completedBytes = if (partial.exists()) partial.length() else record.completedBytes
          emit(record.toSnapshot(TransferState.Paused))
          persistPaused(record)
        } else if (record.cancelRequested) {
          partial.delete()
          emit(record.toSnapshot(TransferState.Cancelled))
          store.remove(record.id)
        } else {
          val message = t.message ?: t::class.java.simpleName
          emit(record.toSnapshot(TransferState.Failed, message))
          store.remove(record.id)
        }
      }
    } finally {
      semaphore.release()
    }
  }

  private suspend fun runUpload(record: TaskRecord) {
    semaphore.acquire()
    try {
      if (record.cancelRequested) {
        emit(record.toSnapshot(TransferState.Cancelled))
        return
      }
      val upload = record.uploadRequest
      if (upload == null) {
        emit(record.toSnapshot(TransferState.Failed, "missing upload request"))
        return
      }
      emit(record.toSnapshot(TransferState.Running))
      record.callHolder = FileTransferEngine.CallHolder()
      try {
        val result = engine.uploadMultipart(
          request = upload,
          callHolder = record.callHolder,
          onProgress = { done, total ->
            record.completedBytes = done
            record.totalBytes = total
            val progress = if (total > 0L) done.toDouble() / total.toDouble() else 0.0
            record.onProgress?.invoke(
              TransferProgress(record.id, progress.coerceIn(0.0, 1.0), done, total),
            )
            emit(record.toSnapshot(TransferState.Running))
          },
        )
        if (record.cancelRequested) {
          emit(record.toSnapshot(TransferState.Cancelled))
          return
        }
        record.uploadResult = result.copy(taskId = record.id)
        emit(record.toSnapshot(TransferState.Completed))
      } catch (cancelled: CancellationException) {
        emit(record.toSnapshot(TransferState.Cancelled))
        throw cancelled
      } catch (t: Throwable) {
        if (record.cancelRequested) {
          emit(record.toSnapshot(TransferState.Cancelled))
        } else {
          val message = t.message ?: t::class.java.simpleName
          emit(record.toSnapshot(TransferState.Failed, message))
        }
      }
    } finally {
      semaphore.release()
    }
  }

  /** Persistence is only required for pause/resume of downloads. */
  private fun persistPaused(record: TaskRecord) {
    val destination = record.destinationDirectory?.let { dir ->
      record.fileName?.let { name -> File(dir, name).absolutePath }
    }
    store.upsert(
      PersistedTransfer(
        id = record.id,
        kind = TransferKind.Download,
        state = TransferState.Paused,
        sourceUrl = record.sourceUrl,
        destinationPath = destination,
        partialPath = record.partialFile?.absolutePath,
        completedBytes = record.completedBytes,
        totalBytes = record.totalBytes,
        updatedAtEpochMs = System.currentTimeMillis(),
      ),
    )
  }

  private fun emit(snapshot: TransferSnapshot) {
    snapshots.getOrPut(snapshot.id) { MutableStateFlow(null) }.value = snapshot
  }

  private fun TaskRecord.toSnapshot(
    state: TransferState,
    errorMessage: String? = null,
  ): TransferSnapshot {
    val destination = destinationDirectory?.let { dir ->
      fileName?.let { name -> File(dir, name).absolutePath }
    }
    return TransferSnapshot(
      id = id,
      kind = kind,
      state = state,
      sourceUrl = sourceUrl,
      destinationPath = destination,
      completedBytes = completedBytes,
      totalBytes = totalBytes,
      errorMessage = errorMessage,
    )
  }

  private class TaskRecord(
    val id: Int,
    val kind: TransferKind,
    val sourceUrl: String,
    var destinationDirectory: File? = null,
    var fileName: String? = null,
    var partialFile: File? = null,
    var downloadHeaders: Map<String, String> = emptyMap(),
    var uploadRequest: UploadRequest? = null,
    var onProgress: ((TransferProgress) -> Unit)? = null,
    var completedBytes: Long = 0L,
    var totalBytes: Long = -1L,
    var job: Job? = null,
    var callHolder: FileTransferEngine.CallHolder = FileTransferEngine.CallHolder(),
    @Volatile var pauseRequested: Boolean = false,
    @Volatile var cancelRequested: Boolean = false,
    var resultFile: File? = null,
    var uploadResult: UploadResult? = null,
  )
}
