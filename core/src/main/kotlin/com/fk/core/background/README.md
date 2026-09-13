# Background (`com.fk.core.background`)

Thin [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) façade for deferred **refresh** / **processing** work, plus best-effort in-process short work. Phase **B5**.

## Layout

| Type | Role |
|------|------|
| `Background` | Package marker + `create` / `mock` factories |
| `BackgroundTaskScheduling` | Pluggable register / schedule / cancel / observe |
| `BackgroundWorkExtending` | `beginBackgroundWork` (in-process; no extra OS budget) |
| `BackgroundTaskManager` | Default WorkManager implementation |
| `MockBackgroundTaskScheduler` | Tests / samples |
| `BackgroundTaskRegistration` / `BackgroundAppRefreshRequest` / `BackgroundProcessingRequest` | Bootstrap + schedule models |
| `BackgroundTaskError` | Unified failure taxonomy |
| `BackgroundWorkInfo` / `BackgroundTaskPendingSummary` | Observation + debug |

## Usage

```kotlin
val background = Background.create(
  context,
  BackgroundTaskConfiguration(logScheduling = true),
)

background.registerAppRefresh("com.example.refresh") { handle ->
  // light sync
  !handle.isExpired
}
background.registerProcessing("com.example.cleanup") { handle ->
  // heavier work
  !handle.isExpired
}

background.installRegistrations(
  listOf(
    BackgroundTaskRegistration("com.example.refresh", BackgroundTaskKind.AppRefresh),
    BackgroundTaskRegistration("com.example.cleanup", BackgroundTaskKind.Processing),
  ),
)

background.scheduleAppRefresh(
  BackgroundAppRefreshRequest(
    identifier = "com.example.refresh",
    earliestBeginEpochMs = System.currentTimeMillis() + 15_000L,
  ),
)

background.observeWorkInfo("com.example.refresh").collect { info ->
  // Enqueued → Running → Succeeded / Failed / Cancelled
}
```

## Notes

- Handlers live in a process-wide registry so WorkManager-constructed workers can resolve them.
  Re-register + `installRegistrations` on every cold start **before** pending work may run.
- Unique work name == task `identifier` (`ExistingWorkPolicy.REPLACE` on reschedule).
- `beginBackgroundWork` does **not** extend process lifetime like iOS `beginBackgroundTask`; use schedule APIs for work that must survive death.
- Apple-only `BGTaskScheduler` / Info.plist permit lists are not ported.
- Large file transfers belong in a future `file` package (not this façade).
