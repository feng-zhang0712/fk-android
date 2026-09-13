# File (`com.fk.core.file`)

OkHttp transfer façade: **resumable download**, **multipart upload**, **queue**, and **transfer persistence**. Phase **C1**.

Sandbox CRUD / ZIP from iOS `FKFileManager` are intentionally **not** ported in this phase (guide scope: transfers only).

## Layout

| Type | Role |
|------|------|
| `FileTransfers` | Package marker + `create` / `mock` (avoids `java.io.File` name clash) |
| `FileTransferManaging` | Pluggable download / upload / pause / resume / cancel |
| `FileTransferManager` | Default OkHttp implementation |
| `MockFileTransferManager` | Tests / samples |
| `DownloadRequest` / `UploadRequest` / `TransferProgress` | Transfer models |
| `PersistedTransfer` / `TransferSnapshot` | Persistence + observation |
| `FileTransferError` | Unified failure taxonomy |

## Usage

```kotlin
val transfers = FileTransfers.create(context)

val taskId = transfers.download(
  DownloadRequest(
    sourceUrl = "https://httpbin.org/bytes/1048576",
    destinationDirectory = context.cacheDir,
    fileName = "sample.bin",
  ),
) { progress ->
  // progress.progress in 0.0..1.0
}

transfers.observe(taskId).collect { snapshot ->
  // Queued → Running → Completed / Paused / Failed
}

transfers.pauseDownload(taskId)
transfers.resumeDownload(taskId)

// After Completed:
transfers.downloadResult(taskId) // File + source URL
transfers.uploadResult(taskId)   // status + body bytes
```

## Notes

- Downloads write to a `.partial` file under cache; pause keeps bytes and resume sends `Range`.
- Persistence stores **paused downloads only** (for cold-start resume). Auth headers are not persisted; pass them again via `resumeDownload(id, headers)`.
- If the server ignores `Range` and returns 200, the partial file is restarted from scratch.
- Upload pause is not supported (cancel + re-enqueue); matches typical multipart constraints.
- Max concurrency defaults to 2 (`FileTransferConfiguration.maxConcurrentTransfers`).
- Apple background `URLSession` semantics are not ported — use WorkManager / foreground OkHttp.
