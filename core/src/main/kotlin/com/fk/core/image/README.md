# Image (`com.fk.core.image`)

Coil façade: **`ImageLoading`** + **`ImageCaching`** contracts with a default
[`CoilImageLoader`](CoilImageLoader.kt). Phase **C2**.

Placeholders / Compose `AsyncImage` stay in UI layers — this package is the non-UI
load seam (aligned with iOS `FKImageLoading` / `FKImageLoader`).

## Layout

| Type | Role |
|------|------|
| `Images` | Package marker + `create` / `wrap` / `mock` |
| `ImageLoading` | Load / cancel / prefetch |
| `ImageCaching` | Memory + disk eviction; `clearMemoryCache` |
| `CoilImageLoader` | Default Coil adapter |
| `MockImageLoader` | Tests / samples |
| `ImageLoadRequest` / `ImageLoadOptions` / `ImageLoadResult` | Models |
| `ImageLoadError` | Unified failure taxonomy |

## Usage

```kotlin
val images = Images.create(context)

val result = images.loadImageResult(
  ImageLoadRequest(
    url = "https://picsum.photos/200",
    targetWidth = 200,
    targetHeight = 200,
  ),
)
// result.bitmap, result.wasCached, result.dataSource

images.prefetch(listOf("https://example.com/a.png", "https://example.com/b.png"))
images.cancelLoad(request)
images.clearMemoryCache()
```

## Notes

- Coil owns disk cache, decode, and request coalescing — do not reimplement them here.
- Batch prefetch runs concurrently, capped by `ImageLoaderConfiguration.maxConcurrentPrefetches`.
- `CacheOnly` maps to Coil with network disabled; miss → `ImageLoadError.CacheMiss`.
- Auth headers: set on `ImageLoadRequest.headers` or `ImageLoaderConfiguration.defaultHeaders`.
- Share cookies / TLS with the app network stack via `Images.create(..., okHttpClient = …)` or `wrap`.
- Progressive JPEG / transformers / animated GIF pipelines are out of scope (same as iOS v1).
