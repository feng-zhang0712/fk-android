package com.fk.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * Default [ImageLoading] / [ImageCaching] implementation on Coil.
 *
 * Conceptually aligned with iOS `FKImageLoader` (request / options / cancel / prefetch /
 * memory cache seam). Coil owns disk cache, decode, and in-flight coalescing.
 */
class CoilImageLoader(
  context: Context,
  private val configuration: ImageLoaderConfiguration = ImageLoaderConfiguration(),
  imageLoader: ImageLoader? = null,
  okHttpClient: OkHttpClient? = null,
) : ImageLoading, ImageCaching {

  private val appContext = context.applicationContext
  private val coil: ImageLoader = imageLoader ?: ImageLoader.Builder(appContext)
    .crossfade(configuration.crossfade)
    .respectCacheHeaders(configuration.respectCacheHeaders)
    .apply {
      val client = okHttpClient
      if (client != null) {
        okHttpClient(client)
      }
    }
    .build()

  private val loadJobs = ConcurrentHashMap<String, Job>()
  private val prefetchDisposables = ConcurrentHashMap<String, Disposable>()
  private val prefetchGate =
    Semaphore(configuration.maxConcurrentPrefetches.coerceAtLeast(1))

  override suspend fun loadImage(
    request: ImageLoadRequest,
    options: ImageLoadOptions,
  ): Bitmap = loadImageResult(request, options).bitmap

  override suspend fun loadImageResult(
    request: ImageLoadRequest,
    options: ImageLoadOptions,
  ): ImageLoadResult = coroutineScope {
    validate(request)
    val key = request.resolvedCacheKey()
    val job = coroutineContext[Job]
    if (job != null) {
      loadJobs[key] = job
    }
    try {
      withContext(Dispatchers.IO) {
        when (val result = coil.execute(buildRequest(request, options, prefetch = false))) {
          is SuccessResult -> {
            val bitmap = result.toBitmap()
              ?: throw ImageLoadError.DecodeFailed()
            ImageLoadResult(
              bitmap = bitmap,
              wasCached = result.dataSource.isFromCache(),
              dataSource = result.dataSource.name,
            )
          }
          is ErrorResult -> throw mapError(result, options)
        }
      }
    } finally {
      if (job != null) {
        loadJobs.remove(key, job)
      }
    }
  }

  override fun cancelLoad(request: ImageLoadRequest) {
    val key = request.resolvedCacheKey()
    loadJobs.remove(key)?.cancel()
    prefetchDisposables.remove(key)?.dispose()
  }

  override suspend fun prefetch(
    request: ImageLoadRequest,
    options: ImageLoadOptions,
  ) {
    validate(request)
    val key = request.resolvedCacheKey()
    prefetchGate.withPermit {
      val disposable = coil.enqueue(buildRequest(request, options, prefetch = true))
      prefetchDisposables[key] = disposable
      try {
        when (val result = disposable.job.await()) {
          is SuccessResult -> Unit
          is ErrorResult -> throw mapError(result, options)
        }
      } finally {
        prefetchDisposables.remove(key, disposable)
      }
    }
  }

  override suspend fun prefetch(
    urls: List<String>,
    targetWidth: Int?,
    targetHeight: Int?,
    options: ImageLoadOptions,
  ): Unit = coroutineScope {
    urls.map { url ->
      async {
        prefetch(
          ImageLoadRequest(
            url = url,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
          ),
          options,
        )
      }
    }.awaitAll()
  }

  override fun cancelPrefetch(request: ImageLoadRequest) {
    cancelLoad(request)
  }

  override fun cachedImage(forKey: String): Bitmap? =
    coil.memoryCache?.get(MemoryCache.Key(forKey))?.bitmap

  override fun store(image: Bitmap, forKey: String) {
    coil.memoryCache?.set(MemoryCache.Key(forKey), MemoryCache.Value(image))
  }

  @OptIn(ExperimentalCoilApi::class)
  override fun removeImage(forKey: String) {
    coil.memoryCache?.remove(MemoryCache.Key(forKey))
    coil.diskCache?.remove(forKey)
  }

  override fun clearMemoryCache() {
    coil.memoryCache?.clear()
  }

  @OptIn(ExperimentalCoilApi::class)
  override fun removeAllImages() {
    coil.memoryCache?.clear()
    coil.diskCache?.clear()
  }

  /** Underlying Coil loader for advanced host integration. */
  fun coilImageLoader(): ImageLoader = coil

  private fun buildRequest(
    request: ImageLoadRequest,
    options: ImageLoadOptions,
    prefetch: Boolean,
  ): ImageRequest {
    val memoryPolicy = when (options.cachePolicy) {
      ImageLoadCachePolicy.Default -> CachePolicy.ENABLED
      ImageLoadCachePolicy.ReloadIgnoringCache -> CachePolicy.WRITE_ONLY
      ImageLoadCachePolicy.CacheOnly -> CachePolicy.ENABLED
    }
    val diskPolicy = when {
      options.excludesFromDiskCache -> CachePolicy.DISABLED
      options.cachePolicy == ImageLoadCachePolicy.ReloadIgnoringCache -> CachePolicy.WRITE_ONLY
      options.cachePolicy == ImageLoadCachePolicy.CacheOnly -> CachePolicy.ENABLED
      else -> CachePolicy.ENABLED
    }
    val networkPolicy = when (options.cachePolicy) {
      ImageLoadCachePolicy.CacheOnly -> CachePolicy.DISABLED
      else -> CachePolicy.ENABLED
    }

    val builder = ImageRequest.Builder(appContext)
      .data(request.url.trim())
      .memoryCacheKey(request.resolvedCacheKey())
      .diskCacheKey(request.resolvedCacheKey())
      .memoryCachePolicy(memoryPolicy)
      .diskCachePolicy(diskPolicy)
      .networkCachePolicy(networkPolicy)

    configuration.defaultHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
    request.headers.forEach { (k, v) -> builder.addHeader(k, v) }

    val w = request.targetWidth
    val h = request.targetHeight
    when {
      w != null && h != null -> builder.size(w, h)
      w != null -> builder.size(w, w)
      h != null -> builder.size(h, h)
      prefetch -> builder.size(Size.ORIGINAL)
    }

    return builder.build()
  }

  private fun validate(request: ImageLoadRequest) {
    val raw = request.url.trim()
    if (raw.isEmpty()) throw ImageLoadError.InvalidUrl(request.url)
    val w = request.targetWidth
    val h = request.targetHeight
    if ((w != null && w <= 0) || (h != null && h <= 0)) {
      throw ImageLoadError.InvalidTargetDimensions()
    }
    val scheme = raw.substringBefore(':', missingDelimiterValue = "")
      .lowercase()
      .takeIf { raw.contains(':') }
    when (scheme) {
      null, "", "http", "https", "file", "content", "android.resource", "data" -> Unit
      else -> throw ImageLoadError.UnsupportedScheme(scheme)
    }
  }

  private fun SuccessResult.toBitmap(): Bitmap? {
    val drawable = drawable
    return when (drawable) {
      is BitmapDrawable -> drawable.bitmap
      else -> {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
      }
    }
  }

  private fun DataSource.isFromCache(): Boolean =
    this == DataSource.MEMORY ||
      this == DataSource.MEMORY_CACHE ||
      this == DataSource.DISK

  private fun mapError(result: ErrorResult, options: ImageLoadOptions): ImageLoadError {
    if (options.cachePolicy == ImageLoadCachePolicy.CacheOnly) {
      return ImageLoadError.CacheMiss()
    }
    val throwable = result.throwable
    if (throwable is CancellationException) {
      return ImageLoadError.Cancelled()
    }
    val message = throwable.message.orEmpty()
    val code = Regex("""\b(\d{3})\b""").find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return when {
      code != null && message.contains("HTTP", ignoreCase = true) ->
        ImageLoadError.HttpStatus(code)
      message.contains("decode", ignoreCase = true) ||
        message.contains("drawable", ignoreCase = true) ->
        ImageLoadError.DecodeFailed(throwable)
      else ->
        ImageLoadError.Network(
          message.ifBlank { throwable::class.java.simpleName },
          throwable,
        )
    }
  }
}
