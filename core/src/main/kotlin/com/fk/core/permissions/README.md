# Permissions (`com.fk.core.permissions`)

Unified check / request façade over Android runtime permissions. Phase **B3**.

## Layout

| Type | Role |
|------|------|
| `Permissions` | Package marker + `create` / `manifestPermissions` |
| `PermissionsManager` | Status, request, batch / sequential request, settings, observation |
| `PermissionKind` | High-level domains (camera, mic, location, …) |
| `PermissionStatus` / `PermissionResult` / `PermissionError` | Unified outcomes |
| `PermissionRequest` / `PermissionPrePrompt` | Request input + optional education copy |
| `PermissionPrePromptHandler` | Host-supplied pre-prompt UI (library does not render dialogs) |

## Usage

```kotlin
val permissions = Permissions.create(context)

// Host manifest should declare:
// Permissions.manifestPermissions(PermissionKind.Camera)

val status = permissions.status(PermissionKind.Camera, activity)
val result = permissions.request(
  activity,
  PermissionRequest(
    kind = PermissionKind.Camera,
    prePrompt = PermissionPrePrompt(
      title = "Camera access",
      message = "Needed to scan QR codes.",
    ),
  ),
)

if (result.status == PermissionStatus.PermanentlyDenied) {
  permissions.openAppSettings()
}
```

Declare matching `<uses-permission>` entries in the **host** manifest (the library does not merge them).

## Notes

- Requests use Activity Result Registry (no `onCreate` launcher registration required).
- `NotDetermined` vs `PermanentlyDenied` uses a small SharedPreferences request tracker.
- `LocationAlways` requests foreground location first, then background (API 29+ platform rule).
- `PermanentlyDenied` / `Unavailable` short-circuit without showing another system dialog.
- Notifications / Bluetooth kinds that need no runtime permission on older APIs report `Granted`.
- Skip Apple-only domains (ATT, speech, add-only photos, temporary full accuracy).
