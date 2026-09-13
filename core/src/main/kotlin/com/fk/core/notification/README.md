# LocalNotification (`com.fk.core.notification`)

Thin [NotificationManager](https://developer.android.com/reference/android/app/NotificationManager) façade with **channels**, **schedule / cancel**, and AlarmManager-backed delayed delivery. Phase **B6**.

## Layout

| Type | Role |
|------|------|
| `LocalNotification` | Package marker + `create` / `mock` factories |
| `LocalNotificationScheduling` | Pluggable schedule / cancel / remove delivered |
| `LocalNotificationManager` | Default NotificationManager + AlarmManager implementation |
| `MockLocalNotificationScheduler` | Tests / samples |
| `LocalNotificationChannel` / `LocalNotificationRequest` / `LocalNotificationTrigger` | Channel + content + trigger models |
| `LocalNotificationError` | Unified failure taxonomy |
| `LocalNotificationCategory` | Optional action buttons applied at post time |

## Usage

```kotlin
val notifications = LocalNotification.create(context)

// Host: request POST_NOTIFICATIONS via Permissions first (API 33+)
notifications.ensureChannel(
  LocalNotificationChannel(
    id = "reminders",
    name = "Reminders",
    importance = LocalNotificationImportance.Default,
  ),
)

if (notifications.canScheduleNotifications()) {
  notifications.schedule(
    LocalNotificationRequest(
      identifier = "com.example.reminder.1",
      channelId = "reminders",
      content = LocalNotificationContent(
        title = "Reminder",
        body = "Time to check in",
      ),
      trigger = LocalNotificationTrigger.TimeInterval(delayMs = 5_000L),
    ),
  )
}
```

## Notes

- This module **never** prompts for permission — use [`permissions`](../permissions/README.md).
- Immediate triggers post via `NotificationManager`; delayed / at-time triggers use `AlarmManager` + persisted pending store (restored on `BOOT_COMPLETED`).
- Exact alarms are used when `AlarmManager.canScheduleExactAlarms()` is true; otherwise inexact `setAndAllowWhileIdle`.
- Same `identifier` replaces a previous pending request.
- Apple-only Critical Alerts / rich push extensions are not ported.
