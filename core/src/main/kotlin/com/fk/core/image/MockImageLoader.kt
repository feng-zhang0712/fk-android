package com.fk.core.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-memory [ImageLoading] / [ImageCaching] for tests and samples (no network).
 *
 * Conceptually aligned with iOS `FKMockImageLoader`.
 */
class MockImageLoader : ImageLoading, ImageCaching {

  private val memory = ConcurrentHashMap<String, Bitmap>()
  private val cancelledKeys = ConcurrentHashMap.newKeySet<String>()

  /** Bitmap returned on success when [stubError] is null. */
  @Volatile
  var stubImage: Bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).also {
    it.eraseColor(Color.CYAN)
  }

  /** When non-null, every load throws this error. */
  @Volatile
  var stubError: ImageLoadError? = null

  /** Artificial delay before completing a load. */
  @Volatile
  var loadDelayMs: Long = 0L

  private val loadCalls = AtomicInteger(0)

  val loadCallCount: Int get() = loadCalls.get()

  val cancelledUrls: List<String>
    get() = cancelledKeys.map { it.substringBefore("|w=").substringBefore("|h=") }

  fun reset() {
    memory.clear()
    cancelledKeys.clear()
    loadCalls.set(0)
    stubError = null
    loadDelayMs = 0L
  }

  override suspend fun loadImage(
    request: ImageLoadRequest,
    options: ImageLoadOptions,
  ): Bitmap = loadImageResult(request, options).bitmap

  override suspend fun loadImageResult(
    request: ImageLoadRequest,
    options: ImageLoadOptions,
  ): ImageLoadResult {
    loadCalls.incrementAndGet()
    val key = request.resolvedCacheKey()
    if (loadDelayMs > 0L) delay(loadDelayMs)
    if (cancelledKeys.contains(key)) {
      throw ImageLoadError.Cancelled()
    }
    stubError?.let { throw it }

    when (options.cachePolicy) {
      ImageLoadCachePolicy.CacheOnly -> {
        val cached = memory[key] ?: throw ImageLoadError.CacheMiss()
        return ImageLoadResult(cached, wasCached = true, dataSource = "MEMORY")
      }
      ImageLoadCachePolicy.ReloadIgnoringCache -> Unit
      ImageLoadCachePolicy.Default -> {
        memory[key]?.let {
          return ImageLoadResult(it, wasCached = true, dataSource = "MEMORY")
        }
      }
    }

    val image = stubImage
    if (!options.excludesFromDiskCache) {
      memory[key] = image
    }
    return ImageLoadResult(image, wasCached = false, dataSource = "NETWORK")
  }

  override fun cancelLoad(request: ImageLoadRequest) {
    cancelledKeys.add(request.resolvedCacheKey())
  }

  override suspend fun prefetch(
    request: ImageLoadRequest,
    options: ImageLoadOptions,
  ) {
    loadImageResult(request, options)
  }

  override suspend fun prefetch(
    urls: List<String>,
    targetWidth: Int?,
    targetHeight: Int?,
    options: ImageLoadOptions,
  ) {
    urls.forEach { url ->
      prefetch(
        ImageLoadRequest(url = url, targetWidth = targetWidth, targetHeight = targetHeight),
        options,
      )
    }
  }

  override fun cancelPrefetch(request: ImageLoadRequest) {
    cancelLoad(request)
  }

  override fun cachedImage(forKey: String): Bitmap? = memory[forKey]

  override fun store(image: Bitmap, forKey: String) {
    memory[forKey] = image
  }

  override fun removeImage(forKey: String) {
    memory.remove(forKey)
  }

  override fun clearMemoryCache() {
    memory.clear()
  }

  override fun removeAllImages() {
    memory.clear()
  }
}
