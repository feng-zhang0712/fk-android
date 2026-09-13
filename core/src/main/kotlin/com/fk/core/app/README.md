# App (`com.fk.core.app`)

Non-UI app infrastructure: **version / AppInfo**, **deeplink parse + route**,
**ProcessLifecycle → Pluggable lifecycle**, **analytics sink**, **startup tasks**.
Phase **C3**.

Conceptually aligned with iOS `FKCoreKit` BusinessKit (not FKBusinessKit UI Base / CommentKit).

## Layout

| Type | Role |
|------|------|
| `App` | Factory `create` / `mock` → [AppServices] |
| `AppInfo` / `AndroidAppInfo` | Package version + device / channel |
| `VersionChecking` / `DefaultVersionChecker` | Remote version compare (no update UI) |
| `DeeplinkRouter` / `UrlDeeplinkParser` / `PatternRouteHandler` | Parse + handler chain |
| `AnalyticsTracking` / `BufferedAnalyticsTracker` | Page / click / custom + flush |
| `StartupTaskManaging` / `StartupTaskManager` | Ordered launch tasks |
| `ProcessLifecycleAppObserver` | Implements Pluggable `AppLifecycleObserver` |

## Usage

```kotlin
val app = App.create(context)

val version = app.info.versionLabel()

val context = app.deeplinks.parse("https://example.com/product/42?ref=ad")
// context.pathSegments, context.query

app.deeplinks.register(
  PatternRouteHandler(id = "product", pathPattern = "/product/*") { ctx ->
    RouteHandlingResult.Handled
  },
)
app.deeplinks.open("https://example.com/product/42")

app.analytics.trackPageView("home")
app.startup.register(StartupTask(id = "warm-cache") { /* … */ })
app.startup.runAll()
```

## Notes

- Update prompts / Play Store lookup stay in the host app — only the decision API is here.
- Lifecycle reuses Pluggable `AppLifecycleObserver`; do not invent a second lifecycle API.
- Prefer a single process-scoped `ProcessLifecycleAppObserver` (or call `close()` when replacing).
- Deeplink handlers run in **registration order**; path pattern `*` matches any path.
- Analytics is a sink + optional uploader; not a Firebase wrapper.
- Startup task failures are isolated (except cancellation).
- I18n remains in `com.fk.core.i18n` (not duplicated here).
