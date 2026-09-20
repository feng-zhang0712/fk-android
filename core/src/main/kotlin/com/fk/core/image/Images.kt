package com.fk.core.image

import android.content.Context
import coil.ImageLoader
import okhttp3.OkHttpClient

/**
 * Image loading — Coil façade (`ImageLoading` / `ImageCaching`).
 *
 * Conceptually aligned with iOS `FKCoreKit` ImageLoader / Pluggable media contracts.
 * Named [Images] (not `Image`) to avoid clashing with Compose / framework `Image` types.
 *
 * Placeholders and Compose `AsyncImage` bindings belong in UI layers; this package is
 * the non-UI load contract.
 */
object Images {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.2"

  /**
   * Builds the default Coil-backed [CoilImageLoader].
   *
   * @param imageLoader Optional shared Coil loader (skips building a new one).
   * @param okHttpClient Optional shared OkHttp client (ignored when [imageLoader] is set).
   */
  fun create(
    context: Context,
    configuration: ImageLoaderConfiguration = ImageLoaderConfiguration(),
    imageLoader: ImageLoader? = null,
    okHttpClient: OkHttpClient? = null,
  ): CoilImageLoader =
    CoilImageLoader(
      context = context,
      configuration = configuration,
      imageLoader = imageLoader,
      okHttpClient = okHttpClient,
    )

  /** Wraps an existing app-wide Coil [ImageLoader]. */
  fun wrap(
    context: Context,
    imageLoader: ImageLoader,
    configuration: ImageLoaderConfiguration = ImageLoaderConfiguration(),
  ): CoilImageLoader =
    CoilImageLoader(
      context = context,
      configuration = configuration,
      imageLoader = imageLoader,
    )

  /** Builds an in-memory mock for tests and samples. */
  fun mock(): MockImageLoader = MockImageLoader()
}
