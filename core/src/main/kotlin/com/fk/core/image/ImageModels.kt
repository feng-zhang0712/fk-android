package com.fk.core.image

/**
 * Request to load a remote or local image.
 *
 * Conceptually aligned with iOS `FKImageLoadRequest`.
 *
 * @property url Absolute `http`/`https`/`file` URI, or a content/`file` path Coil accepts.
 * @property targetWidth Optional decode width in pixels (downsampling hint).
 * @property targetHeight Optional decode height in pixels.
 * @property cacheKey Optional override for Coil / memory cache key.
 * @property headers Extra request headers (e.g. Authorization).
 */
data class ImageLoadRequest(
  val url: String,
  val targetWidth: Int? = null,
  val targetHeight: Int? = null,
  val cacheKey: String? = null,
  val headers: Map<String, String> = emptyMap(),
) {
  /** Resolved cache key used for cancel / coalesce / memory lookups. */
  fun resolvedCacheKey(): String =
    cacheKey?.takeIf { it.isNotBlank() }
      ?: buildString {
        append(url.trim())
        if (targetWidth != null) append("|w=").append(targetWidth)
        if (targetHeight != null) append("|h=").append(targetHeight)
      }
}

/** Cache read policy for a single load. */
enum class ImageLoadCachePolicy {
  /** Memory → disk → network (Coil defaults). */
  Default,

  /** Skip cache reads; still write on success. */
  ReloadIgnoringCache,

  /** Serve from cache only; miss fails with [ImageLoadError.CacheMiss]. */
  CacheOnly,
}

/**
 * Per-load options.
 *
 * @property cachePolicy Cache read/write policy.
 * @property excludesFromDiskCache When true, successful loads are not written to disk.
 */
data class ImageLoadOptions(
  val cachePolicy: ImageLoadCachePolicy = ImageLoadCachePolicy.Default,
  val excludesFromDiskCache: Boolean = false,
)

/**
 * Successful load payload.
 *
 * @property bitmap Decoded bitmap.
 * @property wasCached True when Coil reported a memory or disk hit.
 * @property dataSource Human-readable Coil data source name (`MEMORY`, `DISK`, `NETWORK`, …).
 */
data class ImageLoadResult(
  val bitmap: android.graphics.Bitmap,
  val wasCached: Boolean,
  val dataSource: String,
)

/**
 * Runtime knobs for [CoilImageLoader].
 *
 * Thin mapping onto Coil’s [coil.ImageLoader.Builder]; Coil owns cache limits.
 * When an external [coil.ImageLoader] is passed to [Images.create], [crossfade] and
 * [respectCacheHeaders] are ignored (already baked into that loader).
 *
 * @property maxConcurrentPrefetches Cap on simultaneous prefetch network work.
 */
data class ImageLoaderConfiguration(
  val crossfade: Boolean = false,
  val respectCacheHeaders: Boolean = true,
  val defaultHeaders: Map<String, String> = emptyMap(),
  val maxConcurrentPrefetches: Int = 4,
)

/**
 * Stable error taxonomy for image loads.
 *
 * Conceptually aligned with iOS `FKImageLoaderError` (subset suited to Coil).
 */
sealed class ImageLoadError(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause) {
  class InvalidUrl(value: String) :
    ImageLoadError("Invalid image URL: $value")

  class UnsupportedScheme(scheme: String) :
    ImageLoadError("Unsupported image URL scheme: $scheme")

  class InvalidTargetDimensions :
    ImageLoadError("Invalid target dimensions (must be > 0 when set)")

  class HttpStatus(code: Int) :
    ImageLoadError("Image HTTP status: $code")

  class Network(detail: String, cause: Throwable? = null) :
    ImageLoadError("Image network failure: $detail", cause)

  class DecodeFailed(cause: Throwable? = null) :
    ImageLoadError("Image decode failed", cause)

  class CacheMiss :
    ImageLoadError("Image cache miss under CacheOnly policy")

  class Cancelled :
    ImageLoadError("Image load cancelled")

  class Unknown(detail: String, cause: Throwable? = null) :
    ImageLoadError(detail, cause)
}
