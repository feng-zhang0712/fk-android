package com.fk.core.image

import android.graphics.Bitmap

/**
 * Pluggable contract for loading images without binding UI to Coil / Glide.
 *
 * Conceptually aligned with iOS `FKImageLoading`.
 */
interface ImageLoading {
  /** Loads and decodes an image for [request]. */
  suspend fun loadImage(
    request: ImageLoadRequest,
    options: ImageLoadOptions = ImageLoadOptions(),
  ): Bitmap

  /** Loads with metadata ([ImageLoadResult.wasCached], data source). */
  suspend fun loadImageResult(
    request: ImageLoadRequest,
    options: ImageLoadOptions = ImageLoadOptions(),
  ): ImageLoadResult

  /** Cancels an in-flight load for the resolved cache key when supported. */
  fun cancelLoad(request: ImageLoadRequest)

  /** Prefetches into Coil caches without returning a bitmap. */
  suspend fun prefetch(
    request: ImageLoadRequest,
    options: ImageLoadOptions = ImageLoadOptions(),
  )

  /** Prefetches multiple URLs concurrently (same optional target size for each). */
  suspend fun prefetch(
    urls: List<String>,
    targetWidth: Int? = null,
    targetHeight: Int? = null,
    options: ImageLoadOptions = ImageLoadOptions(),
  )

  /** Cancels an in-flight prefetch for the resolved cache key. */
  fun cancelPrefetch(request: ImageLoadRequest)
}

/**
 * Optional cache seam paired with [ImageLoading].
 *
 * Conceptually aligned with iOS `FKImageCaching`.
 */
interface ImageCaching {
  /** Returns a memory-cached bitmap when present. */
  fun cachedImage(forKey: String): Bitmap?

  /** Convenience over [ImageLoadRequest.resolvedCacheKey]. */
  fun cachedImage(request: ImageLoadRequest): Bitmap? =
    cachedImage(forKey = request.resolvedCacheKey())

  /** Stores a bitmap in Coil’s memory cache. */
  fun store(image: Bitmap, forKey: String)

  /** Evicts one entry from memory and disk caches. */
  fun removeImage(forKey: String)

  /** Clears the memory cache only (e.g. onTrimMemory). */
  fun clearMemoryCache()

  /** Clears Coil memory and disk caches. */
  fun removeAllImages()
}
